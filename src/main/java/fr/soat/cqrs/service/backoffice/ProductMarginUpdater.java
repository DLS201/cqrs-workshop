package fr.soat.cqrs.service.backoffice;

import fr.soat.cqrs.dao.ProductDAO;
import fr.soat.cqrs.dao.ProductMarginDAO;
import fr.soat.cqrs.event.OrderSavedEvent;
import fr.soat.cqrs.model.Order;
import fr.soat.cqrs.model.OrderLine;
import fr.soat.cqrs.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Service
@Slf4j
public class ProductMarginUpdater {

    private final ProductDAO productDAO;
    private final ProductMarginDAO productMarginDAO;

    public ProductMarginUpdater(ProductDAO productDAO, ProductMarginDAO productMarginDAO) {
        this.productDAO = productDAO;
        this.productMarginDAO = productMarginDAO;
    }

    @Async
    @Transactional
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void onOrderSavedEvent(OrderSavedEvent orderSavedEvent) {
        log.info("Received " + orderSavedEvent);
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
}
