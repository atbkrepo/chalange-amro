package com.xyz.orders.repository;

import com.xyz.orders.dto.ProductCatalogRow;
import com.xyz.orders.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCase(String name);

    @Query("""
            select new com.xyz.orders.dto.ProductCatalogRow(
                p.name,
                (i.quantity - i.reservedQuantity),
                p.price,
                case when (i.quantity - i.reservedQuantity) < 10 then true else false end
            )
            from Inventory i
            join i.product p
            order by p.name
            """)
    List<ProductCatalogRow> findAllProductCatalogRows();
}
