package com.example.inventory_service.service;

import com.example.inventory_service.model.Product;
import com.example.inventory_service.model.Reservation;
import com.example.inventory_service.repository.ReservationRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InventoryService {

    private final MongoTemplate mongoTemplate;
    private final ReservationRepository reservationRepository;
    private final Counter reservationSuccessCounter;
    private final Counter reservationFailedCounter;

    public InventoryService(MongoTemplate mongoTemplate,
                            ReservationRepository reservationRepository,
                            MeterRegistry meterRegistry) {
        this.mongoTemplate = mongoTemplate;
        this.reservationRepository = reservationRepository;
        this.reservationSuccessCounter = Counter.builder("inventory.reservation.success")
                .description("Successful stock reservations")
                .register(meterRegistry);
        this.reservationFailedCounter = Counter.builder("inventory.reservation.failed")
                .description("Failed stock reservations (insufficient stock)")
                .register(meterRegistry);
    }

    @Observed(name = "inventory.reserve", contextualName = "reserve-stock")
    public boolean reserveStock(Long orderId, String productId, int quantity) {

        log.info("Reserving stock: orderId={}, productId={}, quantity={}", orderId, productId, quantity);

        Query query = new Query();
        query.addCriteria(
                Criteria.where("id").is(productId)
                        .and("availableStock").gte(quantity)
        );

        Update update = new Update()
                .inc("availableStock", -quantity)
                .inc("reservedStock", quantity);

        Product updated = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Product.class
        );

        if (updated != null) {
            reservationSuccessCounter.increment();
            log.info("Stock reserved successfully: productId={}, remainingStock={}", productId, updated.getAvailableStock());
            return true;
        } else {
            reservationFailedCounter.increment();
            log.warn("Stock reservation failed: productId={}, requestedQuantity={}", productId, quantity);
            return false;
        }
    }
}