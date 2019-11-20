package fr.soat.cqrs.dao;

import fr.soat.cqrs.event.OrderEvent;
import fr.soat.cqrs.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.Optional;

@Repository
public class OrderEventDAOImpl implements OrderEventDAO {

    public static final OrderEventMapper ORDER_EVENT_MAPPER = new OrderEventMapper();
    public static final OrderJsonMapper ORDER_JSON_MAPPER = new OrderJsonMapper();
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public OrderEventDAOImpl(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void push(OrderEvent event) {
        Order order = event.getOrder();
        String sql =
            "INSERT INTO order_event (event_type, product_order)\n" +
            "VALUES (?,?)";

        String eventType = event.getClass().getSimpleName();
        String jsonOrder = ORDER_JSON_MAPPER.toJson(event.getOrder());

        jdbcTemplate.update(sql, eventType, jsonOrder);
    }

    @Override
    public Optional<OrderEvent> pop() {
        String sql =
            "DELETE FROM order_event\n" +
            "WHERE event_id = (SELECT MIN(event_id) FROM order_event)\n" +
            "RETURNING event_id, event_type, product_order;";

        OrderEvent event = jdbcTemplate.queryForObject(sql, ORDER_EVENT_MAPPER);

        return Optional.of(event);
    }
}
