package fr.soat.cqrs.dao;

public interface ProductMarginDAO {
    void incrementProductMargin(Long productReference, String productName, float marginToAdd);
}
