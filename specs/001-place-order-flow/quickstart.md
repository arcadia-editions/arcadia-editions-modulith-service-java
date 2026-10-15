# Quickstart: validate the Place Order Flow

How to prove the feature works once the tasks are implemented. Behavior rules are in
[data-model.md](data-model.md); responses beyond the OpenAPI in [contracts/checkout-http.md](contracts/checkout-http.md);
outcome triggers in [contracts/stand-in-adapters.md](contracts/stand-in-adapters.md).

## Prerequisites

- Java 25 and Maven. In Git Bash: `export JAVA_HOME=~/.sdkman/candidates/java/25.0.2-tem` (gap report T-02).
- Docker (PostgreSQL and Kafka from `docker-compose.yml`; tests start them through Testcontainers when not running).
- Models unchanged since planning (staleness check in `docs/zenwave-sdd-overlay.md`). If it prints `STALE`, stop: regenerate first (human).

## 1. Automated checks (the definition of done)

```bash
mvn -DskipTests clean compile 2>&1 | tee ../compile.log   # log outside target/ (T-03)
grep -c "Unmapped target propert" ../compile.log           # only the expected ones remain (X-08)
mvn test
```

Expected:

| Check | Proves |
|---|---|
| `*ServiceTest` (5 modules) green | each command: happy path, every alternative, invalid transition, not found, redelivery = no second event (G-FT-02) |
| `flows/PlaceOrderFlowTest` green, 6 scenarios | FR-018 / SC-001: `completed`, `stockGone`, `orderCancelled` via declined, retry exhausted, fulfillment failed, capture failed |
| `flows/InventoryConcurrencyTest` green | SC-002: concurrent orders for the last units never oversell |
| `OrdersCheckoutApiIntegrationTest` green | 201 / 400 / 409 / 200 / 404 (G-FT-03) |
| `ArchitectureTest`, `JMoleculesArchitectureTest` (and `ModularityTest` if added) green | module and layer rules intact |

Then re-run `/zenwave-gap-report`: no blocker or major finding left for `PlaceOrderFlow` (constitution, "Done").

## 2. Manual run against the app

```bash
docker compose up -d postgresql kafka
mvn spring-boot:run          # profile "default": ddl-auto update, localhost datasource
```

The API uses HTTP basic `admin:password`. Inventory positions are created through backoffice code, which
has no REST controller yet (G-CI-12), so seed one directly:

```bash
docker compose exec postgresql psql -U postgres -d app -c \
 "INSERT INTO inventory_position (id, version, sku, available_quantity, reserved_quantity, sold_quantity)
  VALUES (nextval('inventory_position_seq'), 0, 'SKU-001', 2, 0, 0);"
```

Request body used below (`body.json`); change `paymentMethodId`, `quantity`, `postalCode` per scenario:

```json
{
  "customerId": "CUST-001", "customerEmail": "reader@example.com", "paymentMethodId": "pm_ok",
  "currency": "EUR",
  "items": [ { "sku": "SKU-001", "quantity": 1, "unitPrice": 49.90 } ],
  "shippingAddress": { "name": "Ada Reader", "line1": "1 Main St", "city": "Madrid", "postalCode": "28001", "country": "ES" }
}
```

```bash
curl -s -u admin:password -H 'Content-Type: application/json' -d @body.json http://localhost:8080/api/orders/start-checkout
curl -s -u admin:password http://localhost:8080/api/orders/<id>
```

| Scenario | Change in `body.json` | Expect |
|---|---|---|
| US1 completed | as above | `201`, `status` `CREATED`; shortly after, `GET` shows `CONFIRMED`; log shows one order-confirmation e-mail |
| US1 with one retry | `paymentMethodId` `pm_fail_once` | same end state; payment `attempt_count` = 2 |
| US2 stock gone | `quantity` greater than available | `409` with `unavailableItems`; no new `checkout_order` row; one stock-unavailable e-mail in the log |
| US3 declined | `pm_decline` | `GET` ends `CANCELLED`, reason `PAYMENT_DECLINED`; stock back in `available_quantity` |
| US3 retry exhausted | `pm_fail_always` | `CANCELLED`, reason `PAYMENT_RETRY_EXHAUSTED`; payment `RETRY_EXHAUSTED`, `attempt_count` = 3 |
| US4 fulfillment failed | `postalCode` `00000` | `CANCELLED`, reason `PAYMENT_VOIDED`; payment `VOIDED`, never `CAPTURED` |
| US4 capture failed | `pm_capture_fail` | `CANCELLED`, reason `PAYMENT_VOIDED`; payment `VOIDED` |
| Validation | `"currency": "EURO"` or `"items": []` | `400` problem details, inventory unchanged |
| Unknown order | `GET /api/orders/999999` | `404` |

Checks after each cancelled scenario (SC-003, SC-006):

```sql
SELECT sku, available_quantity, reserved_quantity, sold_quantity FROM inventory_position;
SELECT order_id, status, attempt_count FROM payment;
SELECT order_id, type, status FROM notification ORDER BY created_at;   -- one row per order and type
SELECT count(*) FROM event_publication WHERE completion_date IS NULL;  -- 0 once the flow has settled
```

Reservation expiry is out of scope: an order whose payment never resolves keeps its stock held.
