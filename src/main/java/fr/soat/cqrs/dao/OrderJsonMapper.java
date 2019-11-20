package fr.soat.cqrs.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.soat.cqrs.model.Order;

import java.io.IOException;

public class OrderJsonMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String toJson(Order order) {
        try {
            return objectMapper.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize order; " + order);
        }
    }

    public static Order fromJson(String orderJson) {
        try {
            return objectMapper.readValue(orderJson, Order.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize order json; " + orderJson);
        }
    }



}
