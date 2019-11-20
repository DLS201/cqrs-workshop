package fr.soat.cqrs.service.backoffice;

import fr.soat.cqrs.dao.OrderEventDAO;
import fr.soat.cqrs.dao.ProductDAO;
import fr.soat.cqrs.dao.ProductMarginDAO;
import fr.soat.cqrs.event.OrderDeletedEvent;
import fr.soat.cqrs.event.OrderEvent;
import fr.soat.cqrs.event.OrderSavedEvent;
import fr.soat.cqrs.model.Order;
import fr.soat.cqrs.model.OrderLine;
import fr.soat.cqrs.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class ProductMarginUpdater {

    private final ProductDAO productDAO;
    private final ProductMarginDAO productMarginDAO;
    private final OrderEventDAO orderEventDAO;

    /** polls and consumes events only if enabled */
    private final AtomicBoolean enabled = new AtomicBoolean(false);

    public ProductMarginUpdater(ProductDAO productDAO, ProductMarginDAO productMarginDAO, OrderEventDAO orderEventDAO) {
        this.productDAO = productDAO;
        this.productMarginDAO = productMarginDAO;
        this.orderEventDAO = orderEventDAO;
        disable();
    }

    public void disable() {
        this.enabled.set(false);
        log.info(this.getClass().getSimpleName() + " is disabled (stop consuming events)");
    }

    public void enable() {
        this.enabled.set(true);
        log.info(this.getClass().getSimpleName() + " is enabled (start consuming events)");
    }

    @Transactional
    @Scheduled(fixedDelay = 100)
    public void consumePendingOrderEvents() {
        if (enabled.get()) {
            log.info(this.getClass().getSimpleName() + " is polling for pending events...");

            Optional<OrderEvent> maybeOrder = orderEventDAO.pop();

            if (maybeOrder.isPresent()) {
                OrderEvent order = maybeOrder.get();

                if (order instanceof OrderSavedEvent) {
                    onOrderSavedEvent(((OrderSavedEvent) order));
                }
                else {
                    onOrderDeletedEvent(((OrderDeletedEvent) order));
                }
            }
        }
    }


    private void onOrderSavedEvent(OrderSavedEvent orderSavedEvent) {
        log.info("Consuming " + orderSavedEvent);
        Order order  = orderSavedEvent.getOrder();

        for (OrderLine orderLine : order.getLines()) {
            // 2. for each product, compute the margin to add
            Long productReference = orderLine.getProductReference();
            int quantity = orderLine.getQuantity();
            Product product = productDAO.getByReference(productReference);
            float productMargin = Math.round((product.getPrice() - product.getSupplyPrice()) * quantity);

            // 3. update total_margin in product_margin table
            productMarginDAO.incrementProductMargin(productReference, product.getName(), productMargin);
        }
    }

    private void onOrderDeletedEvent(OrderDeletedEvent orderDeletedEvent) {
        log.info("Consuming " + orderDeletedEvent);
        Order order  = orderDeletedEvent.getOrder();

        for (OrderLine orderLine : order.getLines()) {
            // 2. for each product, compute the margin to remove
            Long productReference = orderLine.getProductReference();
            int quantity = orderLine.getQuantity();
            Product product = productDAO.getByReference(productReference);
            float productMargin = Math.round((product.getPrice() - product.getSupplyPrice()) * quantity);

            // 3. update total_margin in product_margin table
            productMarginDAO.decrementProductMargin(productReference, product.getName(), productMargin);
        }
    }

}
