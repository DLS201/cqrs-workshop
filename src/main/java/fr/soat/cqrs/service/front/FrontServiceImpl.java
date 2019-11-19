package fr.soat.cqrs.service.front;

import fr.soat.cqrs.dao.OrderDAO;
import fr.soat.cqrs.dao.ProductInventoryDAO;
import fr.soat.cqrs.model.Order;
import fr.soat.cqrs.model.OrderLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FrontServiceImpl implements FrontService {

    private final ProductInventoryDAO productInventoryDAO;
    private final OrderDAO orderDAO;

    @Autowired
    public FrontServiceImpl(ProductInventoryDAO productInventoryDAO, OrderDAO orderDAO) {
        this.productInventoryDAO = productInventoryDAO;
        this.orderDAO = orderDAO;
    }

    @Override
    @Transactional
    public Long order(Order order) {
        // save order in product_order
        Long orderId = orderDAO.insert(order);

        // update inventory
        for (OrderLine line : order.getLines()) {
            productInventoryDAO.decreaseProductInventory(line.getProductReference(), line.getQuantity());
        }

        return orderId;
    }

}
