package com.volt.catalog.infrastructure.adapter.in.web.controller;

import com.volt.catalog.application.port.in.GetProductUseCase;
import com.volt.catalog.application.port.in.ManageProductUseCase;
import com.volt.catalog.application.port.in.SearchProductsUseCase;
import com.volt.catalog.domain.model.PagedResult;
import com.volt.catalog.domain.model.Product;
import com.volt.catalog.domain.model.ProductSearchCriteria;
import com.volt.catalog.infrastructure.adapter.in.web.dto.request.CreateProductRequest;
import com.volt.catalog.infrastructure.adapter.in.web.dto.request.UpdateProductRequest;
import com.volt.catalog.infrastructure.adapter.in.web.dto.response.PagedProductResponse;
import com.volt.catalog.infrastructure.adapter.in.web.dto.response.ProductResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final GetProductUseCase getProduct;
    private final ManageProductUseCase manageProduct;
    private final SearchProductsUseCase searchProducts;

    @GetMapping(params = "!ids")
    public PagedProductResponse search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId) {

        PagedResult<Product> result = searchProducts.search(new ProductSearchCriteria(query, categoryId, brandId, true, page, size));
        return PagedProductResponse.from(result);
    }

    @GetMapping(params = "ids")
    public List<ProductResponse> getBatch(@RequestParam List<Long> ids) {
        if (ids.isEmpty() || ids.stream().anyMatch(id -> id == null || id < 1)) {
            throw new IllegalArgumentException("ids must contain positive product IDs");
        }
        Map<Long, Product> found = new LinkedHashMap<>();
        getProduct.getAll(ids).forEach(product -> found.put(product.getId(), product));
        return ids.stream().distinct().filter(found::containsKey).map(found::get).map(ProductResponse::from).toList();
    }

    @GetMapping("/{productId}")
    public ProductResponse getById(@PathVariable @Positive long productId) {
        return ProductResponse.from(getProduct.getById(productId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        Product created = manageProduct.create(request.toCommand());
        return ResponseEntity
                .created(URI.create("/api/v1/products/" + created.getId()))
                .body(ProductResponse.from(created));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse update(@PathVariable @Positive long productId, @Valid @RequestBody UpdateProductRequest request) {
        return ProductResponse.from(manageProduct.update(productId, request.toCommand()));
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable @Positive long productId) {
        manageProduct.deactivate(productId);
        return ResponseEntity.noContent().build();
    }
}
