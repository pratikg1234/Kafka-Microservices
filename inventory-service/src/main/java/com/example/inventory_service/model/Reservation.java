package com.example.inventory_service.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "reservations")
@Data
public class Reservation {

    @Id
    private String id;

    private Long orderId;
    private String productId;
    private int quantity;

    private String status; // RESERVED / RELEASED
}
