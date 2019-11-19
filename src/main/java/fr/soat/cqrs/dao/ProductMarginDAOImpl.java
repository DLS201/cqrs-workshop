package fr.soat.cqrs.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;

@Repository
public class ProductMarginDAOImpl implements ProductMarginDAO {

    JdbcTemplate jdbcTemplate;

    private final String SQL_UPDATE_MARGIN =
            "INSERT INTO product_margin (product_reference, product_name, total_margin)\n" +
            "VALUES (?, ?, ?)\n" +
            "ON CONFLICT (product_reference) DO UPDATE\n" +
            "SET total_margin = product_margin.total_margin + ?\n" +
            "WHERE product_margin.product_reference = ?";

    @Autowired
    public ProductMarginDAOImpl(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void incrementProductMargin(Long productReference, String productName, float marginToAdd) {
        jdbcTemplate.update(
                SQL_UPDATE_MARGIN,
                productReference, productName, marginToAdd, marginToAdd, productReference);
    }
}
