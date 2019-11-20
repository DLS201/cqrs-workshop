package fr.soat.cqrs.dao;

import fr.soat.cqrs.model.Order;
import fr.soat.cqrs.model.order.OrderFixtures;
import org.junit.Test;

import static fr.soat.cqrs.model.order.OrderFixtures.ProductEnum.ROBE_REINE_DES_NEIGES;
import static fr.soat.cqrs.model.order.OrderFixtures.ProductEnum.TSHIRT_BOB_LEPONGE;
import static fr.soat.cqrs.model.order.OrderFixtures.one;
import static fr.soat.cqrs.model.order.OrderFixtures.two;
import static org.junit.Assert.*;

public class OrderJsonMapperTest {

    @Test
    public void should_ser_des() {
        Order order = OrderFixtures.buildOrder(
                one(TSHIRT_BOB_LEPONGE),
                two(ROBE_REINE_DES_NEIGES)
        );
        Order serdesOrder = OrderJsonMapper.fromJson(OrderJsonMapper.toJson(order));

        assertEquals(serdesOrder, order);
    }
}