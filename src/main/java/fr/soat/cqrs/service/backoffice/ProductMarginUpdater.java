package fr.soat.cqrs.service.backoffice;

import fr.soat.cqrs.dao.EventDAO;
import fr.soat.cqrs.dao.ProductDAO;
import fr.soat.cqrs.dao.ProductMarginDAO;
import fr.soat.cqrs.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ProductMarginUpdater {

    private final ProductDAO productDAO;
    private final EventDAO eventDAO;
    private final ProductMarginDAO productMarginDAO;
    private final DatabaseChangeEventListener databaseChangeEventListener;

    public ProductMarginUpdater(ProductDAO productDAO, EventDAO eventDAO, ProductMarginDAO productMarginDAO, DatabaseChangeEventListener databaseChangeEventListener) {
        this.productDAO = productDAO;
        this.eventDAO = eventDAO;
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
                if ("c".equals(op) || "r".equals(op) ) {
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
        // 0. check event not already processed
        Struct rowBefore = (Struct) ((Struct) record.value()).get("before");
        //FIXME check if event is consumed for the first time !
        String hash = hash("del", rowBefore, null);
        eventDAO.insert(hash);

        // 1. compute margin for order line
        long reference = Long.valueOf((int)rowBefore.get("reference"));
        int quantity = (int) rowBefore.get("quantity");
        Product product = productDAO.getByReference(reference);
        float productMargin = Math.round((product.getPrice() - product.getSupplyPrice()) * quantity);

        // 2. update product_margin
        productMarginDAO.decrementProductMargin(reference, product.getName(), productMargin);
        log.info("(-) decreasing margin on {} of {}", product.getName(), productMargin);
    }

    private void onCreateOrderLine(SourceRecord record) {
        // 0. check event not already processed
        Struct rowAfter = (Struct) ((Struct) record.value()).get("after");
        //FIXME check if event is consumed for the first time !
        String hash = hash("ins", null, rowAfter);
        eventDAO.insert(hash);

        // 1. compute margin for order line
        long reference = Long.valueOf((int)rowAfter.get("reference"));
        int quantity = (int) rowAfter.get("quantity");
        Product product = productDAO.getByReference(reference);
        float productMargin = Math.round((product.getPrice() - product.getSupplyPrice()) * quantity);

        // 2. update product_margin
        productMarginDAO.incrementProductMargin(reference, product.getName(), productMargin);
        log.info("(+) increasing margin on {} of {}", product.getName(), productMargin);
    }

    private String hash(String operation, Struct rowBefore, Struct rowAfter) {
        return operation + "-" +
                rowBefore + "-" +
                rowAfter;
    }
}
