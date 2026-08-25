package com.volt.catalog.application.port.in;

import com.volt.catalog.domain.model.Product;

import java.util.Collection;
import java.util.List;

/**
 * Incoming port describing the product-reading operations offered by the core.
 *
 * <p>An incoming port is an interface used by input adapters. The web controller
 * calls this contract without knowing which concrete use-case class implements
 * it. Another input adapter, such as a command-line tool, could call it too.
 */
public interface GetProductUseCase {

    Product getById(long productId);

    List<Product> getAll(Collection<Long> productIds);
}
