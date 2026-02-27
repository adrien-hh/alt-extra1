package service;

import static org.junit.jupiter.api.Assertions.*;

import domain.Order;
import domain.Product;
import domain.ShippingZone;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class PricingServiceTest {

  @Test
  void computeCurrencyRate_should_return_expected_rates() {
    assertEquals(1.0, PricingService.computeCurrencyRate("EUR"), 1e-9);
    assertEquals(1.1, PricingService.computeCurrencyRate("USD"), 1e-9);
    assertEquals(0.85, PricingService.computeCurrencyRate("GBP"), 1e-9);
  }

  @Test
  void computeHandling_should_apply_thresholds() {
    assertEquals(0.0, PricingService.computeHandling(10, 2.5), 1e-9);
    assertEquals(2.5, PricingService.computeHandling(11, 2.5), 1e-9);
    assertEquals(5.0, PricingService.computeHandling(21, 2.5), 1e-9);
  }

  @Test
  void computeShipping_sub_below_limit_weight_thresholds_and_zone_multiplier() {
    Map<String, Object> totals = new HashMap<>();
    totals.put("weight", 4.0);

    Map<String, ShippingZone> zones = new HashMap<>();
    zones.put("ZONE1", new ShippingZone(5.0, 0.5));
    zones.put("ZONE3", new ShippingZone(5.0, 0.5));

    // weight <= 5 => base
    assertEquals(5.0, PricingService.computeShipping(totals, 10.0, zones, 50.0, "ZONE1"), 1e-9);

    // 5 < weight <= 10 => base + (weight-5)*0.3
    totals.put("weight", 7.0);
    assertEquals(
        5.0 + (7.0 - 5.0) * 0.3,
        PricingService.computeShipping(totals, 10.0, zones, 50.0, "ZONE1"),
        1e-9);

    // weight > 10 => base + (weight-10)*perKg
    totals.put("weight", 12.0);
    assertEquals(
        5.0 + (12.0 - 10.0) * 0.5,
        PricingService.computeShipping(totals, 10.0, zones, 50.0, "ZONE1"),
        1e-9);

    // ZONE3 multiplier *1.2
    totals.put("weight", 4.0);
    assertEquals(
        5.0 * 1.2, PricingService.computeShipping(totals, 10.0, zones, 50.0, "ZONE3"), 1e-9);
  }

  @Test
  void computeShipping_sub_above_limit_heavy_weight_fee_only_over_20kg() {
    Map<String, Object> totals = new HashMap<>();
    Map<String, ShippingZone> zones = new HashMap<>();

    totals.put("weight", 20.0);
    assertEquals(0.0, PricingService.computeShipping(totals, 100.0, zones, 50.0, "ZONE1"), 1e-9);

    totals.put("weight", 24.0);
    assertEquals(
        (24.0 - 20.0) * 0.25,
        PricingService.computeShipping(totals, 100.0, zones, 50.0, "ZONE1"),
        1e-9);
  }

  @Test
  void computeTax_all_taxable_should_use_taxable_base_and_round_2_decimals() {
    Product p1 = new Product("P1", "name1", "cat1", 10.00, 1.0, true);
    Product p2 = new Product("P2", "name2", "cat2", 5.00, 1.0, true);

    Map<String, Product> products = new HashMap<>();
    products.put("P1", p1);
    products.put("P2", p2);

    Order o1 = new Order("O1", "C1", "P1", 3, 10.00, "2025-01-01", "12:00", "");
    Order o2 = new Order("O2", "C1", "P2", 2, 5.00, "2025-01-01", "12:00", "");

    double taxable = 12.345; // pour tester l'arrondi
    double tax = PricingService.computeTax(taxable, List.of(o1, o2), products, 0.2);

    assertEquals(Math.round(taxable * 0.2 * 100.0) / 100.0, tax, 1e-9);
  }

  @Test
  void computeTax_with_non_taxable_product_should_sum_only_taxable_lines_and_round() {
    Product taxable = new Product("P1", "name1", "cat1", 10.00, 1.0, true);
    Product nonTaxable = new Product("P2", "name2", "cat2", 100.00, 1.0, false);

    Map<String, Product> products = new HashMap<>();
    products.put("P1", taxable);
    products.put("P2", nonTaxable);

    Order o1 = new Order("O1", "C1", "P1", 2, 10.00, "2025-01-01", "12:00", "");
    Order o2 = new Order("O2", "C1", "P2", 1, 100.00, "2025-01-01", "12:00", "");

    // Dans cette branche, ton code utilise prod.price(), pas unitPrice.
    double expected = Math.round((2 * 10.0 * 0.2) * 100.0) / 100.0;
    double tax = PricingService.computeTax(999.0, List.of(o1, o2), products, 0.2);

    assertEquals(expected, tax, 1e-9);
  }

  @Test
  void computeDiscounts_should_apply_volume_threshold_overwrite() {
    List<Order> items =
        List.of(new Order("O1", "C1", "P1", 1, 1.0, "2025-01-06", "12:00", "")); // Monday
    PricingService.Discounts d60 = PricingService.computeDiscounts(60, "BASIC", items, 0, 200);
    assertEquals(60 * 0.05, d60.volumeDiscount, 1e-9);

    PricingService.Discounts d120 = PricingService.computeDiscounts(120, "BASIC", items, 0, 200);
    assertEquals(120 * 0.10, d120.volumeDiscount, 1e-9);

    PricingService.Discounts d600 = PricingService.computeDiscounts(600, "BASIC", items, 0, 200);
    assertEquals(600 * 0.15, d600.volumeDiscount, 1e-9);

    PricingService.Discounts d1200Premium =
        PricingService.computeDiscounts(1200, "PREMIUM", items, 0, 300);
    assertEquals(1200 * 0.20, d1200Premium.volumeDiscount, 1e-9);
  }

  @Test
  void computeDiscounts_weekend_bonus_should_multiply_volume_discount() {
    List<Order> saturday =
        List.of(new Order("O1", "C1", "P1", 1, 1.0, "2025-01-04", "12:00", "")); // Saturday
    PricingService.Discounts d = PricingService.computeDiscounts(120, "BASIC", saturday, 0, 10_000);

    double base = 120 * 0.10;
    assertEquals(base * 1.05, d.volumeDiscount, 1e-9);
  }

  @Test
  void computeDiscounts_should_apply_loyalty_overwrite_and_cap_and_global_max_discount_ratio() {
    List<Order> items =
        List.of(new Order("O1", "C1", "P1", 1, 1.0, "2025-01-06", "12:00", "")); // Monday

    // pts > 500 => 0.15 capped 100
    double pts = 1000;
    PricingService.Discounts d = PricingService.computeDiscounts(1200, "PREMIUM", items, pts, 50);

    // Avant cap : volume = 240, loyalty = 100 => total 340 > 50
    assertEquals(50.0, d.totalDiscount, 1e-9);

    double ratio = 50.0 / (240.0 + 100.0);
    assertEquals(240.0 * ratio, d.volumeDiscount, 1e-9);
    assertEquals(100.0 * ratio, d.loyaltyDiscount, 1e-9);
  }
}
