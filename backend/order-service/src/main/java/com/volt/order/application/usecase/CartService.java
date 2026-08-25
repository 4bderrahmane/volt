package com.volt.order.application.usecase;

import com.volt.order.application.port.in.ManageCartUseCase;
import com.volt.order.application.port.in.ViewCartUseCase;
import com.volt.order.application.port.out.CartRepositoryPort;
import com.volt.order.application.port.out.CatalogClientPort;
import com.volt.order.domain.exception.CartLineNotFoundException;
import com.volt.order.domain.exception.ProductUnavailableException;
import com.volt.order.domain.model.Cart;
import com.volt.order.domain.model.OrderLine;
import com.volt.order.domain.model.OrderTotals;
import com.volt.order.domain.model.ProductSnapshot;
import com.volt.order.domain.model.VatRate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CartService implements ManageCartUseCase, ViewCartUseCase {
    private final CartRepositoryPort carts;
    private final CatalogClientPort catalog;
    private final Clock clock;

    public CartService(CartRepositoryPort carts, CatalogClientPort catalog, Clock clock) {
        this.carts = carts;
        this.catalog = catalog;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Cart addLine(UUID customerId, long productId, int quantity) {
        ProductSnapshot product = catalog.findProducts(List.of(productId)).stream()
                .filter(candidate -> candidate.productId().equals(productId) && candidate.active())
                .findFirst()
                .orElseThrow(() -> new ProductUnavailableException(productId));
        Cart cart = carts.findByCustomerId(customerId).orElseGet(() -> Cart.empty(customerId, clock.instant()));
        cart.addLine(product.productId(), quantity, clock.instant());
        return carts.save(cart);
    }

    @Override
    @Transactional
    public Cart updateLineQuantity(UUID customerId, long cartLineId, int quantity) {
        Cart cart = existing(customerId, cartLineId);
        cart.changeLineQuantity(cartLineId, quantity, clock.instant());
        return carts.save(cart);
    }

    @Override
    @Transactional
    public Cart removeLine(UUID customerId, long cartLineId) {
        Cart cart = existing(customerId, cartLineId);
        cart.removeLine(cartLineId, clock.instant());
        return carts.save(cart);
    }

    @Override
    public CartView view(UUID customerId) {
        Cart cart = carts.findByCustomerId(customerId).orElseGet(() -> Cart.empty(customerId, clock.instant()));
        if (cart.isEmpty()) return new CartView(List.of(), OrderTotals.zero());

        Map<Long, ProductSnapshot> products = catalog.findProducts(
                        cart.getLines().stream().map(line -> line.getProductId()).toList()).stream()
                .collect(Collectors.toMap(ProductSnapshot::productId, Function.identity()));

        List<OrderLine> pricedLines = cart.getLines().stream().map(line -> {
            ProductSnapshot product = product(products, line.getProductId());
            return OrderLine.of(product.productId(), product.reference(), product.label(),
                    product.unitPriceExclVat(), line.getQuantity());
        }).toList();

        List<Item> items = java.util.stream.IntStream.range(0, cart.getLines().size())
                .mapToObj(index -> new Item(
                        cart.getLines().get(index),
                        product(products, cart.getLines().get(index).getProductId()),
                        pricedLines.get(index).getLineTotalExclVat()))
                .toList();
        return new CartView(items, OrderTotals.calculate(pricedLines, VatRate.STANDARD));
    }

    private Cart existing(UUID customerId, long lineId) {
        return carts.findByCustomerId(customerId).orElseThrow(() -> new CartLineNotFoundException(lineId));
    }

    private static ProductSnapshot product(Map<Long, ProductSnapshot> products, Long productId) {
        ProductSnapshot product = products.get(productId);
        if (product == null) throw new ProductUnavailableException(productId);
        return product;
    }
}
