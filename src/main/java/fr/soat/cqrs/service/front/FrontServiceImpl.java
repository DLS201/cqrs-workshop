package fr.soat.cqrs.service.front;

import fr.soat.cqrs.dao.OrderDAO;
import fr.soat.cqrs.dao.ProductDAO;
import fr.soat.cqrs.dao.ProductMarginDAO;
import fr.soat.cqrs.model.Order;
import fr.soat.cqrs.model.OrderLine;
import fr.soat.cqrs.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FrontServiceImpl implements FrontService {

    private final ProductDAO productDAO;
    private final ProductMarginDAO productMarginDAO;
    private final OrderDAO orderDAO;

    @Autowired
    public FrontServiceImpl(OrderDAO orderDAO, ProductDAO productDAO, ProductMarginDAO productMarginDAO) {
        this.productDAO = productDAO;
        this.productMarginDAO = productMarginDAO;
        this.orderDAO = orderDAO;
    }

    @Override
    @Transactional
    public Long order(Order order) {
        Long orderId = orderDAO.insert(order);

        for (OrderLine line : order.getLines())
        {
            Product product = productDAO.getByReference(line.getProductReference());
            float margin = (product.getPrice() - product.getSupplyPrice()) * line.getQuantity();

            productMarginDAO.incrementProductMargin(product.getReference(), product.getName(), margin);
        }

        return orderId;
    }
}
