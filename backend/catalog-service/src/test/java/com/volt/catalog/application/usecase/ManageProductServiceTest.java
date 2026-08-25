package com.volt.catalog.application.usecase;

import com.volt.catalog.application.port.in.ManageProductUseCase;
import com.volt.catalog.domain.exception.DuplicateProductReferenceException;
import com.volt.catalog.domain.model.Product;
import com.volt.catalog.domain.model.Unit;
import com.volt.catalog.infrastructure.adapter.out.memory.InMemoryProductRepositoryAdapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests application orchestration with a replaceable output adapter.
 *
 * <p>No web server or database is used. This is the main testing benefit of
 * ports: the use case can run against a lightweight adapter in milliseconds.
 */
class ManageProductServiceTest {

    private InMemoryProductRepositoryAdapter repository;
    private ManageProductService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProductRepositoryAdapter();
        Clock clock = Clock.fixed(Instant.parse("2026-08-14T10:00:00Z"), ZoneOffset.UTC);
        service = new ManageProductService(repository, clock);
    }

    @Test
    void createsAndAssignsAnIdThroughTheOutputPort() {
        Product created = service.create(createCommand());

        assertEquals(2L, created.getId());
        assertEquals("BOX-001", created.getReference());
    }

    @Test
    void updatesAndDeactivatesTheDomainEntity() {
        Product created = service.create(createCommand());
        Product updated = service.update(created.getId(), new ManageProductUseCase.UpdateProductCommand(
                "Updated box",
                null,
                new BigDecimal("39.90"),
                Unit.BOX,
                2L,
                3L));
        service.deactivate(updated.getId());

        assertEquals("Updated box", repository.findById(updated.getId()).orElseThrow().getLabel());
        assertFalse(repository.findById(updated.getId()).orElseThrow().isActive());
    }

    @Test
    void rejectsADuplicateReference() {
        service.create(createCommand());

        assertThrows(DuplicateProductReferenceException.class, () -> service.create(createCommand()));
    }

    private static ManageProductUseCase.CreateProductCommand createCommand() {
        return new ManageProductUseCase.CreateProductCommand(
                "BOX-001",
                "Cable box",
                null,
                new BigDecimal("29.90"),
                Unit.BOX,
                5,
                1L,
                1L);
    }
}
