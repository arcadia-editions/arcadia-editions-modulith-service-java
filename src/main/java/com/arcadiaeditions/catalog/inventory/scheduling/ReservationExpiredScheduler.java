package com.arcadiaeditions.catalog.inventory.scheduling;

import com.arcadiaeditions.catalog.inventory.domain.ReservationStatus;
import com.arcadiaeditions.catalog.inventory.domain.StockReservation;
import com.arcadiaeditions.catalog.inventory.repository.jpa.StockReservationRepository;
import com.arcadiaeditions.catalog.inventory.service.InventoryService;
import com.arcadiaeditions.catalog.inventory.service.dtos.ExpireReservationInput;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ReservationExpired timer (SchedulerApi, channel {@value #CHANNEL}): runs expireReservation once per order
 * whose RESERVED reservations have reached their expiresAt, read from the injected Clock.
 *
 * Properties: {@code arcadia.timers.reservation-expired-event-v1.enabled} (default true) and
 * {@code .fixed-delay} (default PT30S).
 */
@Component
@org.jmolecules.architecture.layered.InterfaceLayer
public class ReservationExpiredScheduler {

    public static final String CHANNEL = "reservation-expired-event-v1";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final StockReservationRepository stockReservationRepository;
    private final InventoryService inventoryService;
    private final Clock clock;

    @Value("${arcadia.timers." + CHANNEL + ".enabled:true}")
    private boolean enabled = true;

    public ReservationExpiredScheduler(
            StockReservationRepository stockReservationRepository, InventoryService inventoryService, Clock clock) {
        this.stockReservationRepository = stockReservationRepository;
        this.inventoryService = inventoryService;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString = "${arcadia.timers." + CHANNEL + ".fixed-delay:PT30S}",
            initialDelayString = "${arcadia.timers." + CHANNEL + ".fixed-delay:PT30S}")
    void onTimer() {
        if (enabled) {
            expireDueReservations();
        }
    }

    /**
     * Fires ReservationExpired for every order with a RESERVED reservation past its deadline.
     * A failure on one order is logged and the next run retries it; the others go on.
     *
     * @return the orderIds expireReservation ran for, sorted
     */
    public List<String> expireDueReservations() {
        var now = clock.instant();
        var orderIds = stockReservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReservationStatus.RESERVED)
                .filter(r -> r.getExpiresAt() != null && !r.getExpiresAt().isAfter(now))
                .map(StockReservation::getOrderId)
                .distinct()
                .sorted()
                .toList();
        orderIds.forEach(orderId -> {
            log.debug("{}: expireReservation {}", CHANNEL, orderId);
            try {
                inventoryService.expireReservation(new ExpireReservationInput().setOrderId(orderId));
            } catch (RuntimeException e) {
                log.error("{}: expireReservation {} failed, retried next run", CHANNEL, orderId, e);
            }
        });
        return orderIds;
    }
}
