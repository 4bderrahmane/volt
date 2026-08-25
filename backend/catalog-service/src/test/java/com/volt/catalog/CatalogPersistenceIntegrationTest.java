package com.volt.catalog;

import com.volt.catalog.application.port.in.ManageProductUseCase;
import com.volt.catalog.application.port.in.ManageReservationUseCase;
import com.volt.catalog.application.port.in.ReserveStockUseCase;
import com.volt.catalog.application.port.in.SearchProductsUseCase;
import com.volt.catalog.application.port.out.ProductRepositoryPort;
import com.volt.catalog.domain.model.ProductSearchCriteria;
import com.volt.catalog.domain.model.Unit;
import com.volt.catalog.infrastructure.adapter.out.persistence.entity.BrandJpaEntity;
import com.volt.catalog.infrastructure.adapter.out.persistence.repository.BrandJpaRepository;
import com.volt.catalog.infrastructure.adapter.out.persistence.entity.CategoryJpaEntity;
import com.volt.catalog.infrastructure.adapter.out.persistence.repository.CategoryJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class CatalogPersistenceIntegrationTest {

    @Autowired
    private CategoryJpaRepository categories;

    @Autowired
    private BrandJpaRepository brands;

    @Autowired
    private ManageProductUseCase manageProducts;

    @Autowired
    private SearchProductsUseCase searchProducts;

    @Autowired
    private ReserveStockUseCase reserveStock;

    @Autowired
    private ManageReservationUseCase manageReservations;

    @Autowired
    private ProductRepositoryPort productRepository;

    @Test
    void persistsSearchesReservesAndConfirmsStock() {
        CategoryJpaEntity category = categories.save(CategoryJpaEntity.builder()
                .code("TST")
                .label("Test category")
                .build());
        BrandJpaEntity brand = brands.save(BrandJpaEntity.builder().name("Test brand").build());
        var product = manageProducts.create(new ManageProductUseCase.CreateProductCommand(
                "TST-001",
                "Persistent product",
                null,
                new BigDecimal("12.50"),
                Unit.ITEM,
                10,
                category.getId(),
                brand.getId()));

        assertEquals(1, searchProducts.search(
                new ProductSearchCriteria("persistent", null, null, true, 0, 20)).totalElements());

        var reservation = reserveStock.reserve(new ReserveStockUseCase.ReserveStockCommand(
                "ORD-INTEGRATION-1",
                List.of(new ReserveStockUseCase.RequestedLine(product.getId(), 3))));
        manageReservations.confirm(reservation.reservationId());

        assertEquals(7, productRepository.findById(product.getId()).orElseThrow().getStockQuantity());

        manageReservations.restock("ORD-INTEGRATION-1");
        manageReservations.restock("ORD-INTEGRATION-1");

        assertEquals(10, productRepository.findById(product.getId()).orElseThrow().getStockQuantity());
    }
}
