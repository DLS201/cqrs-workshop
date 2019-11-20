package fr.soat.cqrs.service.backoffice;

import fr.soat.cqrs.dao.ProductDAO;
import fr.soat.cqrs.dao.ProductMarginDAO;
import fr.soat.cqrs.event.OrderDeletedEvent;
import fr.soat.cqrs.event.OrderSavedEvent;
import fr.soat.cqrs.model.Order;
import fr.soat.cqrs.model.OrderLine;
import fr.soat.cqrs.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ProductMarginUpdater {

    private final ProductDAO productDAO;
    private final ProductMarginDAO productMarginDAO;
    private final DatabaseChangeEventListener databaseChangeEventListener;

    public ProductMarginUpdater(ProductDAO productDAO, ProductMarginDAO productMarginDAO, DatabaseChangeEventListener databaseChangeEventListener) {
        this.productDAO = productDAO;
        this.productMarginDAO = productMarginDAO;
        this.databaseChangeEventListener = databaseChangeEventListener;
    }

    public void start() {
        databaseChangeEventListener.startListener("public.order_line", this::onOrderLineRecord);
        log.info(this.getClass().getSimpleName() + " is started (start consuming events)");
    }

    public void stop() {
        databaseChangeEventListener.stopListeners();
        log.info(this.getClass().getSimpleName() + " is stopped (stop consuming events)");
    }

    @Transactional
    public void onOrderLineRecord(SourceRecord record) {
        log.info("SourceRecord received: {}", record);
        try {
            Struct recordValue = (Struct) record.value();
            if (recordValue != null) {
                String op = recordValue.getString("op");
                if ("c".equals(op) || "r".equals(op)) {
                    onCreateOrderLine(record);
                } else if ("d".equals(op)) { // delete
                    onDeleteOrderLine(record);
                } else {
                    log.warn("Received unsupported record: {}", record);
                }
            }
        } catch (Exception e) {
            log.error("Failed to consume SourceRecord", e);
        }
    }

    private void onDeleteOrderLine(SourceRecord record) {
        // 1. compute margin for order line
        Struct row = (Struct) ((Struct) record.value()).get("before");
        long reference = Long.valueOf((int) row.get("reference"));
        int quantity = (int) row.get("quantity");
        Product product = productDAO.getByReference(reference);
        //FIXME compute product total margin
        float productMargin = getProductMargin(product, quantity);

        // 2. update product_margin
        productMarginDAO.decrementProductMargin(reference, product.getName(), productMargin);
        log.info("(-) decreasing margin on {} of {}", product.getName(), productMargin);
    }

    private void onCreateOrderLine(SourceRecord record) {
        // 1. compute margin for order line
        Struct row = (Struct) ((Struct) record.value()).get("after");
        long reference = Long.valueOf((int) row.get("reference"));
        int quantity = (int) row.get("quantity");
        Product product = productDAO.getByReference(reference);
        //FIXME compute product total margin
        float productMargin = getProductMargin(product, quantity);

        // 2. update product_margin
        productMarginDAO.incrementProductMargin(reference, product.getName(), productMargin);
        log.info("(+) increasing margin on {} of {}", product.getName(), productMargin);
    }

    private float getProductMargin(Product product, int quantity) {
        return Math.round((product.getPrice() - product.getSupplyPrice()) * quantity);
    }
}
