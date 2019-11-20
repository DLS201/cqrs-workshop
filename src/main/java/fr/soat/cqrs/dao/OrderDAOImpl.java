package fr.soat.cqrs.dao;

import fr.soat.cqrs.event.OrderDeletedEvent;
import fr.soat.cqrs.event.OrderSavedEvent;
import fr.soat.cqrs.model.Order;
import fr.soat.cqrs.model.OrderLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;

@Repository
public class OrderDAOImpl implements OrderDAO {

    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public OrderDAOImpl(DataSource dataSource, ApplicationEventPublisher eventPublisher) {
        jdbcTemplate = new JdbcTemplate(dataSource);
        this.eventPublisher = eventPublisher;
    }

    public List<Order> getOrders() {
        return jdbcTemplate.query(
                "select product_order.id as order_id, " +
                        "order_line.id as line_id, " +
                        "order_line.reference, " +
                        "order_line.quantity " +
                        "from product_order inner join order_line " +
                        "on order_line.order_id = product_order.id",
                new Object[0],
                new OrderMapper());
    }

    public Order getById(Long orderId) {
        String sql = "select product_order.id as order_id, " +
                "order_line.id as line_id, " +
                "order_line.reference, " +
                "order_line.quantity " +
                "from product_order inner join order_line " +
                "on order_line.order_id = product_order.id " +
                "where product_order.id = ?";

        return jdbcTemplate.queryForObject(
                sql,
                new Object[] { orderId },
                new OrderMapper());
    }

    @Override
    public Long insert(Order order) {
        // save
        Long orderId = insertOrder(order);
        order.setId(orderId);
        insertLines(orderId, order.getLines());

        // notiffy
        eventPublisher.publishEvent(new OrderSavedEvent(order));
        return orderId;
    }

    private Long insertOrder(Order order) {
        String sql = "INSERT INTO product_order(id) VALUES (DEFAULT) RETURNING id";
        Long id = jdbcTemplate.queryForObject(sql, Long.class);
        return id;
    }

    private void insertLines(Long orderId, List<OrderLine> lines) {
        String sql = "INSERT INTO order_line(id, order_id, reference, quantity) " +
                "VALUES (DEFAULT, ?, ?, ?)";
        for (OrderLine line : lines) {
            jdbcTemplate.update(sql, orderId, line.getProductReference(), line.getQuantity());
        }
    }

    @Override
    public void delete(Long orderId) {
        Order order = getById(orderId);

        String sql = "DELETE FROM order_line WHERE order_id = ?";
        jdbcTemplate.update(sql, orderId);

        sql = "DELETE FROM product_order WHERE id = ?";
        jdbcTemplate.update(sql, orderId);

        // publish an OrderDeletedEvent
        // hint: reload the order from DB to wrap it into the event
        eventPublisher.publishEvent(new OrderDeletedEvent(order));
    }
}
