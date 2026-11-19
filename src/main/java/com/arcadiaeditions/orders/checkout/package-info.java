/**
 * Every cross-module type orders.checkout uses must be listed here; anything else is a Modulith violation. Event
 * listeners count too: a listener parameter is a type dependency on the producer's event class.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Arcadia Editions - Orders Checkout",
        allowedDependencies = {
                // direct, synchronous call: reserve / confirm stock
                "catalog.inventory :: InventoryService",
                // events consumed
                "catalog.inventory :: events.StockReleased", "payments.processing :: events.PaymentAuthorized",
                "common" })
package com.arcadiaeditions.orders.checkout;
