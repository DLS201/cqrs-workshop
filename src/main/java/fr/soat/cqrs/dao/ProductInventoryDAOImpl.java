package fr.soat.cqrs.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class ProductInventoryDAOImpl implements ProductInventoryDAO {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ProductInventoryDAOImpl(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void increaseProductInventory(Long productReference, int provisionedQuantity) {
        // try to insert initial quantity. If already exists, increment quantity
        String upsertSQL = "INSERT INTO product_inventory (product_reference, quantity)" +
                " VALUES (?, ?) " +
                "ON CONFLICT (product_reference) DO UPDATE " +
                "SET quantity = product_inventory.quantity + ? " +
                "WHERE product_inventory.product_reference = ?";
        jdbcTemplate.update(upsertSQL, productReference, provisionedQuantity, provisionedQuantity, productReference);
    }

    @Override
    public void decreaseProductInventory(Long productReference, int removedQuantity) {
        // 3 cases:
        // * decrease succeed
        // * decrease failed because new quantity is < 0 (DB constraint violation)
        // * decrease failed because no stock (no row for the product reference
        try {
            Integer stock = getStock(productReference);

            if (stock < removedQuantity) {
                throw new InventoryException("Stock too low");
            }
        }
        catch (EmptyResultDataAccessException e) {
            throw new InventoryException("Empty stock");
        }

        String upsertSQL = "UPDATE product_inventory " +
                "SET quantity = quantity - ? " +
                "WHERE product_reference = ?";
        jdbcTemplate.update(upsertSQL, removedQuantity, productReference);
    }

    @Override
    public Integer getStock(Long productReference) {
        return jdbcTemplate.queryForObject(
                "select quantity " +
                        "from product_inventory " +
                        "where product_reference = ?",
                new Object[] { productReference }, (rs, rowNum) -> rs.getInt("quantity"));
    }
}
