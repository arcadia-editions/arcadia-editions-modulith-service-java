# Contract: Checkout HTTP behavior

The request and success shapes are declared by the generated
`../arcadia-editions-modulith-api/orders-checkout-openapi.yml` (from `orders-checkout.zdl:92-110`) and
are not repeated here:

| Operation | Path | Declared in the OpenAPI |
|---|---|---|
| `startOrderCheckout` | `POST /api/orders/start-checkout` | request `StartOrderCheckoutInput`, `201` → `Order` |
| `getOrder` | `GET /api/orders/{id}` | `200` → `Order` |

This file covers only the responses the OpenAPI does **not** declare (research R-03, R-07). They are
produced by `config/WebExceptionsTranslator` as RFC 9457 problem details
(`Content-Type: application/problem+json`). Documenting them in the OpenAPI is a human generation change.

## `POST /api/orders/start-checkout`

| Status | When | Body |
|---|---|---|
| `201` | every line reserved; order saved as `CREATED` | `Order` (declared). `status` = `CREATED`, `orderId` = `ORD-…`, `totalAmount` = Σ quantity × unitPrice |
| `400` | bean validation fails (no items, quantity < 1, `currency` longer than 3, missing `shippingAddress`, missing required fields): FR-001 | problem details with one property per invalid field, `"<object>.<field>": "<message>"` (generated `fillBindingErrors`) |
| `409` | at least one line cannot be reserved (FR-004, Q-05); nothing reserved, no order saved, `StockUnavailable` published | see below |

`409` body:

```json
{
  "type": "about:blank",
  "title": "Stock unavailable",
  "status": 409,
  "detail": "Some items are not available in the requested quantity",
  "instance": "/api/orders/start-checkout",
  "orderId": "ORD-…",
  "unavailableItems": [
    { "sku": "SKU-001", "requestedQuantity": 3, "availableQuantity": 1 }
  ]
}
```

- `orderId` is the correlation id carried by `StockUnavailable` (Q-05); no order exists under it, and
  `GET /api/orders/{id}` cannot find it.
- `unavailableItems` lists every short line (not only the first) with the fields of
  `StockUnavailable.unavailableItems` (`orders-checkout.zdl:126-136`).

## `GET /api/orders/{id}`

| Status | When | Body |
|---|---|---|
| `200` | order exists | `Order` (declared); `status`, and `cancellationReason` when `CANCELLED` (FR-014) |
| `404` | no order with that id (FR-017) | empty (generated controller behavior) |

## Other mappings in `WebExceptionsTranslator` (FR-017)

| Exception | Status | Title |
|---|---|---|
| `NoSuchElementException` | `404` | `Not found` |
| `*Transitions.InvalidStateTransitionException` (any module) | `409` | `Invalid state transition` |
| `StockUnavailableException` | `409` | `Stock unavailable` (above) |

Event-driven commands have no HTTP caller; for them "not found" or "invalid transition" leaves the event
publication incomplete (logged and resubmitted), which is why the commands short-circuit redeliveries
(data-model, "No-op when").
