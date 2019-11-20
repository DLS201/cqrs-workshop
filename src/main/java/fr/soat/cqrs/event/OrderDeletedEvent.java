package fr.soat.cqrs.event;

import fr.soat.cqrs.model.Order;
import lombok.Getter;

public class OrderDeletedEvent {
    @Getter
    private final Order order;

    public OrderDeletedEvent(Order order) {
        this.order = order;
    }
}
