package com.arcadiaeditions.flows;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;

import com.arcadiaeditions.catalog.inventory.domain.InventoryPosition;
import com.arcadiaeditions.catalog.inventory.repository.jpa.InventoryPositionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

/**
 * Reservation concurrency during a drop (SC-002, R-04, X-04): more concurrent checkouts of one unit than units in
 * stock, against PostgreSQL (in-memory repositories cannot lock). Exactly N checkouts succeed (201), the rest get 409
 * (StockUnavailableException), the counters stay consistent, and no request fails with a lock error (5xx).
 *
 * <p>Needs PostgreSQL on localhost:5432 ({@code docker compose up -d postgresql}). Uses a dedicated random SKU, so it
 * does not disturb the fixtures; downstream listeners may already confirm some reservations (reserved to sold), so
 * reserved and sold are asserted together.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class InventoryConcurrencyTest {

    private static final int STOCK = 5;
    private static final int THREADS = 20;

    @Autowired
    WebApplicationContext context;

    @Autowired
    InventoryPositionRepository inventoryRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        this.webTestClient =
                MockMvcWebTestClient.bindToApplicationContext(context).build();
    }

    @Test
    void concurrentCheckoutsNeverOversell() throws Exception {
        String sku = "SKU-CONC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(s -> inventoryRepository.save(new InventoryPosition()
                .setSku(sku)
                .setAvailableQuantity(STOCK)
                .setReservedQuantity(0)
                .setSoldQuantity(0)));

        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(THREADS);
        List<Future<Integer>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < THREADS; i++) {
                final int n = i;
                futures.add(pool.submit(() -> {
                    start.await();
                    return checkout(sku, n);
                }));
            }
            start.countDown();
            int created = 0;
            int conflicts = 0;
            List<Integer> others = new ArrayList<>();
            for (var f : futures) {
                int status = f.get(120, TimeUnit.SECONDS);
                if (status == 201) created++;
                else if (status == 409) conflicts++;
                else others.add(status);
            }
            assertThat(others)
                    .as("unexpected statuses (deadlock / optimistic lock would be 5xx)")
                    .isEmpty();
            assertThat(created).as("successful checkouts").isEqualTo(STOCK);
            assertThat(conflicts).as("StockUnavailable (409) checkouts").isEqualTo(THREADS - STOCK);
        } finally {
            pool.shutdownNow();
        }

        InventoryPosition p = tx.execute(
                s -> inventoryRepository.findAllBySkuInOrderBySku(List.of(sku)).get(0));
        assertThat(p.getAvailableQuantity()).as("available").isZero();
        assertThat(p.getReservedQuantity() + p.getSoldQuantity())
                .as("reserved + sold")
                .isEqualTo(STOCK);
        assertThat(p.getReservedQuantity()).as("reserved").isBetween(0, STOCK);
        assertThat(p.getAvailableQuantity() + p.getReservedQuantity() + p.getSoldQuantity())
                .as("total units unchanged")
                .isEqualTo(STOCK);
    }

    /** POSTs a 1-unit checkout and returns the HTTP status. */
    int checkout(String sku, int n) {
        String body = """
                {
                  "customerId": "CUST-CONC-%d", "customerEmail": "conc%d@example.com", "paymentMethodId": "pm_ok",
                  "currency": "EUR",
                  "items": [ { "sku": "%s", "quantity": 1, "unitPrice": 10.00 } ],
                  "shippingAddress": { "name": "Ada Reader", "line1": "1 Main St", "city": "Madrid", "postalCode": "28001", "country": "ES" }
                }""".formatted(n, n, sku);
        return webTestClient
                .method(POST)
                .uri("/api/orders/start-checkout")
                .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_PROBLEM_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .returnResult(String.class)
                .getStatus()
                .value();
    }
}
