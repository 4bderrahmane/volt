/**
 * Pure logic spanning several aggregates. OrderTotalsCalculator lives here
 * (specification §4 names it TotalCalculationService): it turns cart lines plus priced
 * snapshots into excluding VAT / VAT / including VAT, and it is the single most test-worthy class
 * in the project.
 */
package com.volt.order.domain.service;
