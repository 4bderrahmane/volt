package com.volt.catalog.application.port.in;

import com.volt.catalog.domain.model.Product;

import java.util.Collection;
import java.util.List;

public interface GetProductUseCase {

    Product getById(long productId);

    List<Product> getAll(Collection<Long> productIds);
}
