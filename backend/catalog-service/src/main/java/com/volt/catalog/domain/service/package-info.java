/**
 * Pure logic that spans several aggregates and therefore belongs to no single
 * one. Stateless, constructor-injected with nothing, testable with plain
 * JUnit. Most logic should live on the aggregates; reach for this package only
 * when it genuinely does not fit on one.
 */
package com.volt.catalog.domain.service;
