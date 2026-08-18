package com.volt.catalog.domain.model;

/**
 * Domain value describing how a product is sold. Keeping it in the domain
 * prevents controllers or databases from inventing their own values.
 */
public enum Unit {
    ITEM,
    METER,
    BOX
}
