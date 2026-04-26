package com.example.inventory_service.repository;

import com.example.inventory_service.model.Product;
import com.example.inventory_service.model.Reservation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReservationRepository extends MongoRepository<Reservation, Long> {
}
