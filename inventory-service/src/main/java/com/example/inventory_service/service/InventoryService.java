package com.example.inventory_service.service;

import com.example.inventory_service.model.Product;
import com.example.inventory_service.model.Reservation;
import com.example.inventory_service.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final MongoTemplate mongoTemplate;
    private final ReservationRepository reservationRepository;

public boolean reserveStock(Long orderId, String productId, int quantity) {

    log.info("Trying to reserve productId={}, quantity={}", productId, quantity);
    log.info("Mongo DB Name: {}", mongoTemplate.getDb().getName());
    log.info("All products: {}", mongoTemplate.findAll(Product.class));
    Query query = new Query();
    query.addCriteria(
            Criteria.where("id").is(productId)
                    .and("availableStock").gte(quantity)
    );

    Product productBefore = mongoTemplate.findOne(query, Product.class);
    log.info("Product BEFORE update: {}", productBefore);

    Update update = new Update()
            .inc("availableStock", -quantity)
            .inc("reservedStock", quantity);

    Product updated = mongoTemplate.findAndModify(
            query,
            update,
            FindAndModifyOptions.options().returnNew(true),
            Product.class
    );

    log.info("Product AFTER update: {}", updated);

    return updated != null;
}
}