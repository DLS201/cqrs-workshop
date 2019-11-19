package fr.soat.cqrs.dao;

import fr.soat.cqrs.model.BestSales;
import fr.soat.cqrs.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

@Repository
public class ProductDAOImpl implements ProductDAO {

    JdbcTemplate jdbcTemplate;

    private final String SQL_FIND_BY_REF = "select * from product where reference = ?";
    private final String SQL_BEST_SALES =
            "SELECT product_name, total_margin as product_margin\n" +
            "FROM product_margin\n" +
            "ORDER BY total_margin DESC\n" +
            "LIMIT 3";

    @Autowired
    public ProductDAOImpl(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public Product getByReference(Long reference) {
        return jdbcTemplate.queryForObject(
                "select * " +
                        "from product " +
                        "where reference = ?",
                new Object[] { reference }, new ProductMapper());
    }

    @Override
    public BestSales getBestSales() {
        return jdbcTemplate.queryForObject(SQL_BEST_SALES, new BestSalesMapper());
    }
}
