package com.volt.catalog;

import com.volt.catalog.application.port.in.ManageProductUseCase;
import com.volt.catalog.application.port.in.SearchProductsUseCase;
import com.volt.catalog.domain.model.PagedResult;
import com.volt.catalog.domain.model.Product;
import com.volt.catalog.domain.model.ProductSearchCriteria;
import com.volt.catalog.domain.model.Unit;
import com.volt.catalog.infrastructure.adapter.out.persistence.entity.BrandJpaEntity;
import com.volt.catalog.infrastructure.adapter.out.persistence.entity.CategoryJpaEntity;
import com.volt.catalog.infrastructure.adapter.out.persistence.repository.BrandJpaRepository;
import com.volt.catalog.infrastructure.adapter.out.persistence.repository.CategoryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Specification §F2 — filtering by category and brand, against real SQL.
 *
 * <p>These predicates are built with the JPA Criteria API in
 * {@code ProductPersistenceAdapter}, so an in-memory fake cannot prove them: a
 * join to the wrong column, a filter silently dropped from the predicate list,
 * or an OR where an AND was meant all behave perfectly in a stream and wrongly
 * in a database. Every assertion here therefore runs against PostgreSQL.
 *
 * <p>Each product's label carries a token unique to this class. Other tests
 * share the container and some of them commit, so an assertion on a bare word
 * like "cable" would eventually start counting someone else's rows.
 */
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ProductSearchFilterIntegrationTest {

    private static final String TOKEN = "fltprobe";

    @Autowired private CategoryJpaRepository categories;
    @Autowired private BrandJpaRepository brands;
    @Autowired private ManageProductUseCase manageProducts;
    @Autowired private SearchProductsUseCase searchProducts;

    private long cables;
    private long lighting;
    private long acme;
    private long bolt;

    private long cableAcme;
    private long cableBolt;
    private long lightingAcme;
    private long lightingBolt;
    private long retiredCableAcme;

    @BeforeEach
    void seedTwoCategoriesAndTwoBrands() {
        cables = categories.save(CategoryJpaEntity.builder()
                .code("FLT-CAB").label("Filter cables").build()).getId();
        lighting = categories.save(CategoryJpaEntity.builder()
                .code("FLT-LGT").label("Filter lighting").build()).getId();
        acme = brands.save(BrandJpaEntity.builder().name("Filter Acme").build()).getId();
        bolt = brands.save(BrandJpaEntity.builder().name("Filter Bolt").build()).getId();

        // The search is a single contiguous LIKE over label and reference, not a
        // per-word match, so the shared token has to sit directly before the word
        // each test searches for.
        cableAcme = create("FLT-1", TOKEN + " copper drum", cables, acme);
        cableBolt = create("FLT-2", TOKEN + " copper reel", cables, bolt);
        lightingAcme = create("FLT-3", TOKEN + " lamp warm", lighting, acme);
        lightingBolt = create("FLT-4", TOKEN + " lamp cool", lighting, bolt);

        retiredCableAcme = create("FLT-5", TOKEN + " copper offcut", cables, acme);
        manageProducts.deactivate(retiredCableAcme);
    }

    @Test
    void filtersByCategory() {
        assertThat(idsOf(criteria(null, cables, null, true)))
                .containsExactlyInAnyOrder(cableAcme, cableBolt);
    }

    @Test
    void filtersByBrand() {
        assertThat(idsOf(criteria(null, null, acme, true)))
                .containsExactlyInAnyOrder(cableAcme, lightingAcme);
    }

    /** Two filters must intersect. An OR here would return four rows, not one. */
    @Test
    void combinesCategoryAndBrandWithAnd() {
        assertThat(idsOf(criteria(null, cables, acme, true)))
                .containsExactly(cableAcme);
    }

    @Test
    void combinesAFilterWithTheTextSearch() {
        // "copper" matches two active products; the brand narrows it to one.
        assertThat(idsOf(criteria("copper", null, bolt, true)))
                .containsExactly(cableBolt);

        assertThat(idsOf(criteria("lamp", lighting, null, true)))
                .containsExactlyInAnyOrder(lightingAcme, lightingBolt);
    }

    /** The public listing hides deactivated products; §F4's soft delete depends on it. */
    @Test
    void excludesDeactivatedProductsUnlessAskedFor() {
        assertThat(idsOf(criteria(null, cables, acme, true)))
                .doesNotContain(retiredCableAcme);

        assertThat(idsOf(criteria(null, cables, acme, false)))
                .containsExactlyInAnyOrder(cableAcme, retiredCableAcme);
    }

    @Test
    void reportsAnEmptyPageWhenNothingMatchesTheCombination() {
        // "lamp" exists, and the cables category exists, but never together.
        PagedResult<Product> result = searchProducts.search(criteria(TOKEN + " lamp", cables, null, true));

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
    }

    /** Paging must count the filtered set, not the whole catalog. */
    @Test
    void pagesTheFilteredResultRatherThanTheWholeCatalog() {
        PagedResult<Product> firstPage = searchProducts.search(
                new ProductSearchCriteria(null, cables, null, true, 0, 1));

        assertThat(firstPage.content()).hasSize(1);
        assertThat(firstPage.totalElements()).isEqualTo(2);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.hasNext()).isTrue();

        PagedResult<Product> secondPage = searchProducts.search(
                new ProductSearchCriteria(null, cables, null, true, 1, 1));

        assertThat(secondPage.hasNext()).isFalse();
        assertThat(idsOf(firstPage)).doesNotContainAnyElementsOf(idsOf(secondPage));
    }

    @Test
    void anUnknownCategoryOrBrandMatchesNothingRatherThanEverything() {
        long unusedCategory = categories.save(CategoryJpaEntity.builder()
                .code("FLT-NONE").label("Filter empty").build()).getId();

        assertThat(idsOf(criteria(null, unusedCategory, null, true))).isEmpty();
    }

    // ------------------------------------------------------------------ helpers

    private long create(String reference, String label, long categoryId, long brandId) {
        return manageProducts.create(new ManageProductUseCase.CreateProductCommand(
                reference, label, null, new BigDecimal("10.00"),
                Unit.ITEM, 5, categoryId, brandId)).getId();
    }

    private static ProductSearchCriteria criteria(
            String query, Long categoryId, Long brandId, boolean activeOnly) {
        String scoped = query == null ? TOKEN : (query.contains(TOKEN) ? query : TOKEN + " " + query);
        return new ProductSearchCriteria(scoped, categoryId, brandId, activeOnly, 0, 20);
    }

    private List<Long> idsOf(ProductSearchCriteria criteria) {
        return idsOf(searchProducts.search(criteria));
    }

    private static List<Long> idsOf(PagedResult<Product> result) {
        return result.content().stream().map(Product::getId).toList();
    }
}
