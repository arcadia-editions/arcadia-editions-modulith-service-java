package com.arcadiaeditions.catalog.inventory.service.impl;

import static org.mockito.Mockito.*;

import com.arcadiaeditions.catalog.inventory.config.*;
import com.arcadiaeditions.catalog.inventory.domain.*;
import com.arcadiaeditions.catalog.inventory.repository.jpa.*;
import com.arcadiaeditions.catalog.inventory.repository.jpa.inmemory.*;
import com.arcadiaeditions.catalog.inventory.service.*;
import com.arcadiaeditions.catalog.inventory.service.dtos.*;
import com.arcadiaeditions.catalog.inventory.service.impl.mappers.*;
import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Acceptance Test for InventoryService.
 */
class InventoryServiceTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    ServicesInMemoryConfig context = new ServicesInMemoryConfig();
    InventoryServiceImpl inventoryService = context.inventoryService();

    StockReservationRepositoryInMemory stockReservationRepository = context.stockReservationRepository();

    @BeforeEach
    void setUp() {
        context.reloadTestData();
    }

    @Test
    void reserveStockTest() {
        var input = new ReserveStockInput()
                .setOrderId("ORD-NEW")
                .setItems(java.util.List.of(
                        new ReserveStockItemInput().setSku("SKU-002").setQuantity(2),
                        new ReserveStockItemInput().setSku("SKU-003").setQuantity(1)));

        var result = inventoryService.reserveStock(input);

        var now = context.clock.instant();
        org.junit.jupiter.api.Assertions.assertEquals(ReserveStockOutcome.RESERVED, result.getOutcome());
        org.junit.jupiter.api.Assertions.assertNull(result.getUnavailable());
        var reserved = result.getReserved();
        org.junit.jupiter.api.Assertions.assertEquals("ORD-NEW", reserved.getOrderId());
        org.junit.jupiter.api.Assertions.assertEquals(now.plus(Duration.ofMinutes(10)), reserved.getExpiresAt());
        org.junit.jupiter.api.Assertions.assertEquals(2, reserved.getReservations().size());
        reserved.getReservations().forEach(line -> {
            org.junit.jupiter.api.Assertions.assertTrue(line.getReservationId().startsWith("RES-"));
            org.junit.jupiter.api.Assertions.assertEquals(now, line.getReservedAt());
        });

        var saved = stockReservationRepository.findAll().stream()
                .filter(r -> "ORD-NEW".equals(r.getOrderId()))
                .toList();
        org.junit.jupiter.api.Assertions.assertEquals(2, saved.size());
        saved.forEach(r -> {
            org.junit.jupiter.api.Assertions.assertEquals(ReservationStatus.RESERVED, r.getStatus());
            org.junit.jupiter.api.Assertions.assertEquals(now, r.getReservedAt());
            org.junit.jupiter.api.Assertions.assertEquals(now.plus(Duration.ofMinutes(10)), r.getExpiresAt());
        });

        // SKU-002 20/7 -> 18/9 ; SKU-003 2/1 -> 1/2
        assertPosition("SKU-002", 18, 9);
        assertPosition("SKU-003", 1, 2);
        assertPosition("SKU-001", 9, 0);
    }

    @Test
    void reserveStockUnavailableReservesNothingTest() {
        var input = new ReserveStockInput()
                .setOrderId("ORD-NEW")
                .setItems(java.util.List.of(
                        new ReserveStockItemInput().setSku("SKU-002").setQuantity(2),
                        new ReserveStockItemInput().setSku("SKU-003").setQuantity(3),
                        new ReserveStockItemInput().setSku("SKU-004").setQuantity(1),
                        new ReserveStockItemInput().setSku("SKU-UNKNOWN").setQuantity(1)));
        var before = stockReservationRepository.findAll().size();

        var result = inventoryService.reserveStock(input);

        org.junit.jupiter.api.Assertions.assertEquals(ReserveStockOutcome.UNAVAILABLE, result.getOutcome());
        org.junit.jupiter.api.Assertions.assertNull(result.getReserved());
        var unavailable = result.getUnavailable();
        org.junit.jupiter.api.Assertions.assertEquals("ORD-NEW", unavailable.getOrderId());
        var lines = unavailable.getUnavailableItems().stream()
                .collect(java.util.stream.Collectors.toMap(StockUnavailableLine::getSku, l -> l));
        org.junit.jupiter.api.Assertions.assertEquals(3, lines.size());
        org.junit.jupiter.api.Assertions.assertEquals(3, lines.get("SKU-003").getRequestedQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(2, lines.get("SKU-003").getAvailableQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(1, lines.get("SKU-004").getRequestedQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(0, lines.get("SKU-004").getAvailableQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(0, lines.get("SKU-UNKNOWN").getAvailableQuantity());

        org.junit.jupiter.api.Assertions.assertEquals(before, stockReservationRepository.findAll().size());
        assertPosition("SKU-002", 20, 7);
        assertPosition("SKU-003", 2, 1);
        assertPosition("SKU-004", 0, 0);
    }

    private void assertPosition(String sku, int available, int reservedQty) {
        var position = context.inventoryPositionRepository().findAll().stream()
                .map(p -> (InventoryPosition) p)
                .filter(p -> sku.equals(p.getSku()))
                .findFirst()
                .orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(available, position.getAvailableQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(reservedQty, position.getReservedQuantity());
    }

    @Test
    void releaseStockTest() {
        var result = inventoryService.releaseStock(
                new ReleaseStockInput().setOrderId("ORD-010").setReason(StockReleaseReason.PAYMENT_DECLINED));

        var now = context.clock.instant();
        org.junit.jupiter.api.Assertions.assertEquals(1, result.size());
        var saved = stockReservationRepository.findAll().stream()
                .filter(r -> "ORD-010".equals(r.getOrderId()))
                .toList();
        org.junit.jupiter.api.Assertions.assertEquals(1, saved.size());
        org.junit.jupiter.api.Assertions.assertEquals(ReservationStatus.RELEASED, saved.get(0).getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(StockReleaseReason.PAYMENT_DECLINED, saved.get(0).getReleaseReason());
        org.junit.jupiter.api.Assertions.assertEquals(now, saved.get(0).getReleasedAt());
        // SKU-002 20/7 -> 21/6
        assertPosition("SKU-002", 21, 6);

        var events = context.eventPublisher().getEvents();
        org.junit.jupiter.api.Assertions.assertEquals(1, events.size());
        var event = (com.arcadiaeditions.catalog.inventory.domain.events.StockReleased) events.get(0);
        org.junit.jupiter.api.Assertions.assertEquals("ORD-010", event.getOrderId());
        org.junit.jupiter.api.Assertions.assertEquals(StockReleaseReason.PAYMENT_DECLINED, event.getReason());
        org.junit.jupiter.api.Assertions.assertEquals(now, event.getReleasedAt());
        org.junit.jupiter.api.Assertions.assertEquals(1, event.getItems().size());
        org.junit.jupiter.api.Assertions.assertEquals("RES-012", event.getItems().get(0).getReservationId());
        org.junit.jupiter.api.Assertions.assertEquals("SKU-002", event.getItems().get(0).getSku());
        org.junit.jupiter.api.Assertions.assertEquals(1, event.getItems().get(0).getQuantity());
    }

    @Test
    void releaseStockOnPaymentRetryExhaustedTest() {
        var retryExhausted = new com.arcadiaeditions.payments.processing.domain.event.PaymentRetryExhausted()
                .setOrderId("ORD-011")
                .setReason("processor_unavailable");
        var input = com.arcadiaeditions.catalog.inventory.events.processing.mappers.PaymentsProcessingEventsListenerMapper.INSTANCE
                .asReleaseStockInput(retryExhausted);
        org.junit.jupiter.api.Assertions.assertEquals(StockReleaseReason.PAYMENT_RETRY_EXHAUSTED, input.getReason());

        var result = inventoryService.releaseStock(input);

        org.junit.jupiter.api.Assertions.assertEquals(1, result.size());
        var saved = stockReservationRepository.findAll().stream()
                .filter(r -> "ORD-011".equals(r.getOrderId()))
                .toList();
        org.junit.jupiter.api.Assertions.assertEquals(ReservationStatus.RELEASED, saved.get(0).getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(
                StockReleaseReason.PAYMENT_RETRY_EXHAUSTED, saved.get(0).getReleaseReason());
        var events = context.eventPublisher().getEvents();
        org.junit.jupiter.api.Assertions.assertEquals(1, events.size());
        var event = (com.arcadiaeditions.catalog.inventory.domain.events.StockReleased) events.get(0);
        org.junit.jupiter.api.Assertions.assertEquals("ORD-011", event.getOrderId());
        org.junit.jupiter.api.Assertions.assertEquals(StockReleaseReason.PAYMENT_RETRY_EXHAUSTED, event.getReason());
    }

    @Test
    void releaseStockOnPaymentVoidedSoldBackToAvailableTest() {
        var voided = new com.arcadiaeditions.payments.processing.domain.event.PaymentVoided()
                .setOrderId("ORD-016")
                .setReason("carrier_unavailable");
        var input = com.arcadiaeditions.catalog.inventory.events.processing.mappers.PaymentsProcessingEventsListenerMapper.INSTANCE
                .asReleaseStockInput(voided);
        org.junit.jupiter.api.Assertions.assertEquals(StockReleaseReason.PAYMENT_VOIDED, input.getReason());
        var before = context.inventoryPositionRepository().findAll().stream()
                .map(p -> (InventoryPosition) p)
                .filter(p -> "SKU-002".equals(p.getSku()))
                .findFirst()
                .orElseThrow();
        int available = before.getAvailableQuantity();
        int sold = before.getSoldQuantity();
        int reserved = before.getReservedQuantity();

        var result = inventoryService.releaseStock(input);

        org.junit.jupiter.api.Assertions.assertEquals(1, result.size());
        org.junit.jupiter.api.Assertions.assertEquals(ReservationStatus.RELEASED, result.get(0).getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(StockReleaseReason.PAYMENT_VOIDED, result.get(0).getReleaseReason());
        var after = context.inventoryPositionRepository().findAll().stream()
                .map(p -> (InventoryPosition) p)
                .filter(p -> "SKU-002".equals(p.getSku()))
                .findFirst()
                .orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(available + 1, after.getAvailableQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(sold - 1, after.getSoldQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(reserved, after.getReservedQuantity());
        var events = context.eventPublisher().getEvents();
        org.junit.jupiter.api.Assertions.assertEquals(1, events.size());
        var event = (com.arcadiaeditions.catalog.inventory.domain.events.StockReleased) events.get(0);
        org.junit.jupiter.api.Assertions.assertEquals("ORD-016", event.getOrderId());
        org.junit.jupiter.api.Assertions.assertEquals(StockReleaseReason.PAYMENT_VOIDED, event.getReason());
    }

    @Test
    void releaseStockOnPaymentVoidFailedSoldBackToAvailableTest() {
        var voidFailed = new com.arcadiaeditions.payments.processing.domain.event.PaymentVoidFailed()
                .setOrderId("ORD-025")
                .setReason("carrier_unavailable");
        var input = com.arcadiaeditions.catalog.inventory.events.processing.mappers.PaymentsProcessingEventsListenerMapper.INSTANCE
                .asReleaseStockInput(voidFailed);
        org.junit.jupiter.api.Assertions.assertEquals(StockReleaseReason.PAYMENT_VOID_FAILED, input.getReason());
        var before = skuPosition("SKU-005");
        int available = before.getAvailableQuantity();
        int sold = before.getSoldQuantity();
        int reserved = before.getReservedQuantity();

        var result = inventoryService.releaseStock(input);

        var now = context.clock.instant();
        org.junit.jupiter.api.Assertions.assertEquals(1, result.size());
        org.junit.jupiter.api.Assertions.assertEquals(ReservationStatus.RELEASED, result.get(0).getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(StockReleaseReason.PAYMENT_VOID_FAILED, result.get(0).getReleaseReason());
        org.junit.jupiter.api.Assertions.assertEquals(now, result.get(0).getReleasedAt());
        var after = skuPosition("SKU-005");
        org.junit.jupiter.api.Assertions.assertEquals(available + 1, after.getAvailableQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(sold - 1, after.getSoldQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(reserved, after.getReservedQuantity());
        var events = context.eventPublisher().getEvents();
        org.junit.jupiter.api.Assertions.assertEquals(1, events.size());
        var event = (com.arcadiaeditions.catalog.inventory.domain.events.StockReleased) events.get(0);
        org.junit.jupiter.api.Assertions.assertEquals("ORD-025", event.getOrderId());
        org.junit.jupiter.api.Assertions.assertEquals(StockReleaseReason.PAYMENT_VOID_FAILED, event.getReason());
    }

    @Test
    void releaseStockOnPaymentVoidFailedRedeliveryEmitsNothingTest() {
        var input = new ReleaseStockInput().setOrderId("ORD-026").setReason(StockReleaseReason.PAYMENT_VOID_FAILED);
        var before = skuPosition("SKU-002");
        int available = before.getAvailableQuantity();
        int sold = before.getSoldQuantity();

        var result = inventoryService.releaseStock(input);

        org.junit.jupiter.api.Assertions.assertTrue(result.isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(context.eventPublisher().getEvents().isEmpty());
        var after = skuPosition("SKU-002");
        org.junit.jupiter.api.Assertions.assertEquals(available, after.getAvailableQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(sold, after.getSoldQuantity());
    }

    private InventoryPosition skuPosition(String sku) {
        return context.inventoryPositionRepository().findAll().stream()
                .map(p -> (InventoryPosition) p)
                .filter(p -> sku.equals(p.getSku()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void releaseStockConfirmedReturnsSoldTest() {
        inventoryService.confirmStockReservation(new ConfirmStockReservationInput().setOrderId("ORD-003"));
        context.eventPublisher().getEvents().clear();

        var result = inventoryService.releaseStock(
                new ReleaseStockInput().setOrderId("ORD-003").setReason(StockReleaseReason.PAYMENT_DECLINED));

        org.junit.jupiter.api.Assertions.assertEquals(1, result.size());
        org.junit.jupiter.api.Assertions.assertEquals(ReservationStatus.RELEASED, result.get(0).getStatus());
        // SKU-002 20/7/6 -> confirm 20/6/7 -> release 21/6/6
        var position = context.inventoryPositionRepository().findAll().stream()
                .map(p -> (InventoryPosition) p)
                .filter(p -> "SKU-002".equals(p.getSku()))
                .findFirst()
                .orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(21, position.getAvailableQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(6, position.getReservedQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(6, position.getSoldQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(1, context.eventPublisher().getEvents().size());
    }

    @Test
    void releaseStockRedeliveryEmitsNothingTest() {
        var input = new ReleaseStockInput().setOrderId("ORD-018").setReason(StockReleaseReason.PAYMENT_DECLINED);
        var before = stockReservationRepository.findAll().stream()
                .filter(r -> "ORD-018".equals(r.getOrderId()))
                .findFirst()
                .orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(ReservationStatus.RELEASED, before.getStatus());

        var result = inventoryService.releaseStock(input);

        org.junit.jupiter.api.Assertions.assertTrue(result.isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(context.eventPublisher().getEvents().isEmpty());
        assertPosition("SKU-002", 20, 7);
    }

    @Test
    void confirmStockReservationTest() {
        var result = inventoryService.confirmStockReservation(new ConfirmStockReservationInput().setOrderId("ORD-003"));

        var now = context.clock.instant();
        org.junit.jupiter.api.Assertions.assertEquals(ConfirmStockReservationOutcome.CONFIRMED, result.getOutcome());
        var saved = stockReservationRepository.findAll().stream()
                .filter(r -> "ORD-003".equals(r.getOrderId()))
                .toList();
        org.junit.jupiter.api.Assertions.assertEquals(1, saved.size());
        org.junit.jupiter.api.Assertions.assertEquals(ReservationStatus.CONFIRMED, saved.get(0).getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(now, saved.get(0).getConfirmedAt());

        // SKU-002 20/7/6 -> 20/6/7
        var position = context.inventoryPositionRepository().findAll().stream()
                .map(p -> (InventoryPosition) p)
                .filter(p -> "SKU-002".equals(p.getSku()))
                .findFirst()
                .orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(20, position.getAvailableQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(6, position.getReservedQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(7, position.getSoldQuantity());

        var events = context.eventPublisher().getEvents();
        org.junit.jupiter.api.Assertions.assertEquals(1, events.size());
        var event = (com.arcadiaeditions.catalog.inventory.domain.events.StockReservationConfirmed) events.get(0);
        org.junit.jupiter.api.Assertions.assertEquals("ORD-003", event.getOrderId());
        org.junit.jupiter.api.Assertions.assertEquals(now, event.getConfirmedAt());
        org.junit.jupiter.api.Assertions.assertEquals(1, event.getItems().size());
        org.junit.jupiter.api.Assertions.assertEquals("RES-004", event.getItems().get(0).getReservationId());
        org.junit.jupiter.api.Assertions.assertEquals("SKU-002", event.getItems().get(0).getSku());
        org.junit.jupiter.api.Assertions.assertEquals(1, event.getItems().get(0).getQuantity());
    }

    @Test
    void confirmStockReservationRedeliveryEmitsNothingTest() {
        var input = new ConfirmStockReservationInput().setOrderId("ORD-003");
        inventoryService.confirmStockReservation(input);
        context.eventPublisher().getEvents().clear();

        var result = inventoryService.confirmStockReservation(input);

        org.junit.jupiter.api.Assertions.assertEquals(ConfirmStockReservationOutcome.CONFIRMED, result.getOutcome());
        org.junit.jupiter.api.Assertions.assertTrue(context.eventPublisher().getEvents().isEmpty());
        assertPosition("SKU-002", 20, 6);
    }

    @Test
    void confirmStockReservationNotHeldWhenReleasedTest() {
        var result = inventoryService.confirmStockReservation(new ConfirmStockReservationInput().setOrderId("ORD-022"));

        org.junit.jupiter.api.Assertions.assertEquals(ConfirmStockReservationOutcome.NOT_HELD, result.getOutcome());
        org.junit.jupiter.api.Assertions.assertNotNull(result.getNotHeld());
        org.junit.jupiter.api.Assertions.assertEquals("ORD-022", result.getNotHeld().getOrderId());
        org.junit.jupiter.api.Assertions.assertNull(result.getConfirmed());
        org.junit.jupiter.api.Assertions.assertTrue(context.eventPublisher().getEvents().isEmpty());
        var saved = stockReservationRepository.findAll().stream()
                .filter(r -> "ORD-022".equals(r.getOrderId()))
                .toList();
        org.junit.jupiter.api.Assertions.assertEquals(1, saved.size());
        org.junit.jupiter.api.Assertions.assertEquals(ReservationStatus.RELEASED, saved.get(0).getStatus());
    }

    @Test
    void confirmStockReservationAlreadyConfirmedAnswersConfirmedTest() {
        var result = inventoryService.confirmStockReservation(new ConfirmStockReservationInput().setOrderId("ORD-004"));

        org.junit.jupiter.api.Assertions.assertEquals(ConfirmStockReservationOutcome.CONFIRMED, result.getOutcome());
        org.junit.jupiter.api.Assertions.assertEquals("ORD-004", result.getConfirmed().getOrderId());
        org.junit.jupiter.api.Assertions.assertNull(result.getNotHeld());
        org.junit.jupiter.api.Assertions.assertTrue(context.eventPublisher().getEvents().isEmpty());
    }

    @Test
    void expireReservationNoPaymentReleasesReservedLinesTest() {
        var before = skuPosition("SKU-005");
        int available = before.getAvailableQuantity();
        int reserved = before.getReservedQuantity();
        int sold = before.getSoldQuantity();

        var result = inventoryService.expireReservation(new ExpireReservationInput().setOrderId("ORD-027"));

        var now = context.clock.instant();
        org.junit.jupiter.api.Assertions.assertEquals(1, result.size());
        var saved = stockReservationRepository.findAll().stream()
                .filter(r -> "ORD-027".equals(r.getOrderId()))
                .toList();
        org.junit.jupiter.api.Assertions.assertEquals(1, saved.size());
        org.junit.jupiter.api.Assertions.assertEquals(ReservationStatus.RELEASED, saved.get(0).getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(
                StockReleaseReason.RESERVATION_EXPIRED, saved.get(0).getReleaseReason());
        org.junit.jupiter.api.Assertions.assertEquals(now, saved.get(0).getReleasedAt());
        var after = skuPosition("SKU-005");
        org.junit.jupiter.api.Assertions.assertEquals(available + 1, after.getAvailableQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(reserved - 1, after.getReservedQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(sold, after.getSoldQuantity());

        var events = context.eventPublisher().getEvents();
        org.junit.jupiter.api.Assertions.assertEquals(1, events.size());
        var event = (com.arcadiaeditions.catalog.inventory.domain.events.StockReleased) events.get(0);
        org.junit.jupiter.api.Assertions.assertEquals("ORD-027", event.getOrderId());
        org.junit.jupiter.api.Assertions.assertEquals(StockReleaseReason.RESERVATION_EXPIRED, event.getReason());
        org.junit.jupiter.api.Assertions.assertEquals(now, event.getReleasedAt());
        org.junit.jupiter.api.Assertions.assertEquals(1, event.getItems().size());
        org.junit.jupiter.api.Assertions.assertEquals("RES-028", event.getItems().get(0).getReservationId());
        org.junit.jupiter.api.Assertions.assertEquals("SKU-005", event.getItems().get(0).getSku());
        org.junit.jupiter.api.Assertions.assertEquals(1, event.getItems().get(0).getQuantity());
    }

    @Test
    void expireReservationPaymentStartedReleasesNothingTest() {
        var before = skuPosition("SKU-005");
        int available = before.getAvailableQuantity();
        int reserved = before.getReservedQuantity();

        var result = inventoryService.expireReservation(new ExpireReservationInput().setOrderId("ORD-028"));

        org.junit.jupiter.api.Assertions.assertTrue(result.isEmpty());
        var saved = stockReservationRepository.findAll().stream()
                .filter(r -> "ORD-028".equals(r.getOrderId()))
                .toList();
        org.junit.jupiter.api.Assertions.assertEquals(1, saved.size());
        org.junit.jupiter.api.Assertions.assertEquals(ReservationStatus.RESERVED, saved.get(0).getStatus());
        org.junit.jupiter.api.Assertions.assertNull(saved.get(0).getReleasedAt());
        org.junit.jupiter.api.Assertions.assertTrue(context.eventPublisher().getEvents().isEmpty());
        var after = skuPosition("SKU-005");
        org.junit.jupiter.api.Assertions.assertEquals(available, after.getAvailableQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(reserved, after.getReservedQuantity());
    }

    @Test
    void expireReservationNothingReservedIsNoOpTest() {
        var before = skuPosition("SKU-002");
        int available = before.getAvailableQuantity();
        int reserved = before.getReservedQuantity();

        var result = inventoryService.expireReservation(new ExpireReservationInput().setOrderId("ORD-021"));

        org.junit.jupiter.api.Assertions.assertTrue(result.isEmpty());
        var saved = stockReservationRepository.findAll().stream()
                .filter(r -> "ORD-021".equals(r.getOrderId()))
                .toList();
        org.junit.jupiter.api.Assertions.assertEquals(1, saved.size());
        org.junit.jupiter.api.Assertions.assertEquals(ReservationStatus.RELEASED, saved.get(0).getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(
                StockReleaseReason.RESERVATION_EXPIRED, saved.get(0).getReleaseReason());
        org.junit.jupiter.api.Assertions.assertTrue(context.eventPublisher().getEvents().isEmpty());
        var after = skuPosition("SKU-002");
        org.junit.jupiter.api.Assertions.assertEquals(available, after.getAvailableQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(reserved, after.getReservedQuantity());
    }

    @Test
    void expireReservationRedeliveryAfterReleaseIsNoOpTest() {
        var input = new ExpireReservationInput().setOrderId("ORD-027");
        inventoryService.expireReservation(input);
        context.eventPublisher().getEvents().clear();
        var before = skuPosition("SKU-005");

        var result = inventoryService.expireReservation(input);

        org.junit.jupiter.api.Assertions.assertTrue(result.isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(context.eventPublisher().getEvents().isEmpty());
        var after = skuPosition("SKU-005");
        org.junit.jupiter.api.Assertions.assertEquals(before.getAvailableQuantity(), after.getAvailableQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(before.getReservedQuantity(), after.getReservedQuantity());
    }

    // ReservationExpired timer (reservation-expired-event-v1)

    private com.arcadiaeditions.catalog.inventory.scheduling.ReservationExpiredScheduler scheduler(
            InventoryService service, Clock clock) {
        return new com.arcadiaeditions.catalog.inventory.scheduling.ReservationExpiredScheduler(
                stockReservationRepository, service, clock);
    }

    @Test
    void reservationExpiredTimerExpiresOnlyOrdersPastDeadlineTest() {
        var result = scheduler(inventoryService, context.clock).expireDueReservations();

        // RESERVED and past the deadline at the fixed now: ORD-027 (no payment), ORD-028 (payment started)
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("ORD-027", "ORD-028"), result);
        var byOrder = stockReservationRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(StockReservation::getOrderId));
        org.junit.jupiter.api.Assertions.assertEquals(
                ReservationStatus.RELEASED, byOrder.get("ORD-027").get(0).getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(
                StockReleaseReason.RESERVATION_EXPIRED,
                byOrder.get("ORD-027").get(0).getReleaseReason());
        org.junit.jupiter.api.Assertions.assertEquals(
                ReservationStatus.RESERVED, byOrder.get("ORD-028").get(0).getStatus());
        // before the deadline (10:00:02): untouched
        byOrder.get("ORD-002")
                .forEach(r -> org.junit.jupiter.api.Assertions.assertEquals(ReservationStatus.RESERVED, r.getStatus()));

        var events = context.eventPublisher().getEvents();
        org.junit.jupiter.api.Assertions.assertEquals(1, events.size());
        var event = (com.arcadiaeditions.catalog.inventory.domain.events.StockReleased) events.get(0);
        org.junit.jupiter.api.Assertions.assertEquals("ORD-027", event.getOrderId());
        org.junit.jupiter.api.Assertions.assertEquals(StockReleaseReason.RESERVATION_EXPIRED, event.getReason());
    }

    @Test
    void reservationExpiredTimerRunsExpireReservationOncePerOrderTest() {
        // at ORD-002's deadline, its two RESERVED lines are due: one expireReservation for the order
        var atOrd002Deadline = Clock.fixed(Instant.parse("2026-10-14T10:00:02Z"), ZoneOffset.UTC);
        var service = spy(inventoryService);

        var result = scheduler(service, atOrd002Deadline).expireDueReservations();

        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("ORD-002", "ORD-027", "ORD-028"), result);
        verify(service, times(1)).expireReservation(argThat(i -> "ORD-002".equals(i.getOrderId())));
        verify(service, times(3)).expireReservation(any());
        // ORD-002 payment started: nothing released for it
        stockReservationRepository.findByOrderId("ORD-002")
                .forEach(r -> org.junit.jupiter.api.Assertions.assertEquals(ReservationStatus.RESERVED, r.getStatus()));
    }

    @Test
    void reservationExpiredTimerNextRunEmitsNothingNewTest() {
        var timer = scheduler(inventoryService, context.clock);
        timer.expireDueReservations();
        context.eventPublisher().getEvents().clear();

        var result = timer.expireDueReservations();

        // ORD-027 is RELEASED now; ORD-028 still RESERVED (payment owns the end) and asked again, no release
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("ORD-028"), result);
        org.junit.jupiter.api.Assertions.assertTrue(context.eventPublisher().getEvents().isEmpty());
    }

    @Test
    void reservationExpiredTimerFailureOfOneOrderDoesNotStopTheOthersTest() {
        var service = spy(inventoryService);
        doThrow(new IllegalStateException("boom"))
                .when(service)
                .expireReservation(argThat(i -> "ORD-027".equals(i.getOrderId())));

        var result = scheduler(service, context.clock).expireDueReservations();

        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("ORD-027", "ORD-028"), result);
        verify(service, times(1)).expireReservation(argThat(i -> "ORD-028".equals(i.getOrderId())));
        org.junit.jupiter.api.Assertions.assertTrue(context.eventPublisher().getEvents().isEmpty());
    }
}
