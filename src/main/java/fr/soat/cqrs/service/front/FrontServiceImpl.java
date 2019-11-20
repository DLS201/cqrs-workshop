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

    private final OrderDAO orderDAO;
    private final ProductInventoryDAO productInventoryDAO;

    @Autowired
    public FrontServiceImpl(OrderDAO orderDAO, ProductInventoryDAO productInventoryDAO) {
        this.orderDAO = orderDAO;
        this.productInventoryDAO = productInventoryDAO;
    }

    @Override
    @Transactional
    public Long order(Order order) {
        // save order in product_order
        Long orderId = orderDAO.insert(order);

        // update inventory
        for(OrderLine line : order.getLines()) {
            productInventoryDAO.decreaseProductInventory(line.getProductReference(), line.getQuantity());
        }

        return orderId;
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        // delete order from product_order / order_line
        Order order = orderDAO.getById(orderId);
        orderDAO.delete(orderId);

        // update inventory
        for (OrderLine line : order.getLines()) {
            productInventoryDAO.increaseProductInventory(
                    line.getProductReference(),
                    line.getQuantity());
        }
    }

}
