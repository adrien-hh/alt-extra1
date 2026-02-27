package service;

import static org.junit.jupiter.api.Assertions.*;

import domain.Order;
import domain.Product;
import domain.Promotion;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class LinePricingTest {

  @Test
  void computeLineTotal_percentage_promo_should_apply_rate() {
    Product prod = new Product("P1", "name1", "cat1", 10.0, 1.0, true);
    Order order = new Order("O1", "C1", "P1", 2, 999.0, "2025-01-01", "PROMO10", "12:00");

    Map<String, Promotion> promos = new HashMap<>();
    promos.put("PROMO10", new Promotion("PROMO10", "PERCENTAGE", "10", "true"));

    LinePricing.LineResult r = LinePricing.computeLineTotal(order, prod, promos);

    // 2 * 10 * (1 - 0.10) = 18
    assertEquals(18.0, r.lineTotal, 1e-9);
    assertEquals(0.0, r.morningBonus, 1e-9);
  }

  @Test
  void computeLineTotal_fixed_promo_should_apply_bug_per_qty() {
    Product prod = new Product("P1", "name1", "cat1", 10.0, 1.0, true);
    Order order = new Order("O1", "C1", "P1", 2, 999.0, "2025-01-01", "FIX5", "12:00");

    Map<String, Promotion> promos = new HashMap<>();
    promos.put("FIX5", new Promotion("FIX5", "FIXED", "5", "true"));

    LinePricing.LineResult r = LinePricing.computeLineTotal(order, prod, promos);

    // legacy bug: - fixedDiscount * qty => 2*10 - 5*2 = 10
    assertEquals(10.0, r.lineTotal, 1e-9);
  }

  @Test
  void computeLineTotal_morning_bonus_before_10_should_apply_3_percent_reduction() {
    Product prod = new Product("P1", "name1", "cat1", 100.0, 1.0, true);
    Order order = new Order("O1", "C1", "P1", 1, 999.0, "2025-01-01", "", "09:00");

    Map<String, Promotion> promos = Map.of();

    LinePricing.LineResult r = LinePricing.computeLineTotal(order, prod, promos);

    // lineTotal initial = 100 ; morningBonus = 3 ; final = 97
    assertEquals(3.0, r.morningBonus, 1e-9);
    assertEquals(97.0, r.lineTotal, 1e-9);
  }
}
