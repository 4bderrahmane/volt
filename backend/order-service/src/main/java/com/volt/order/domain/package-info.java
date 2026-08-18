/**
 * Pure business model. No Spring, no JPA, no Jackson, no jakarta.*. Enforced
 * by HexagonalArchitectureTest. This service holds the only interesting
 * arithmetic in the project (totals, VAT, rounding) and it must be testable
 * without booting anything.
 */
package com.volt.order.domain;
