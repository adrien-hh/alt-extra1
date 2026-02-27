package service;

import domain.Order;
import domain.Product;
import domain.Promotion;
import java.util.Map;

public class LinePricing {

  public static LineResult computeLineTotal(
      Order order, Product productFallback, Map<String, Promotion> promotions) {
    // Récupération produit avec fallback
    Product prod = productFallback;
    double basePrice = prod.price();

    // Application promo (logique complexe et bugguée)
    String promoCode = order.promoCode();
    double discountRate = 0;
    double fixedDiscount = 0;

    if (promoCode != null && !promoCode.isEmpty() && promotions.containsKey(promoCode)) {
      Promotion promo = promotions.get(promoCode);
      if (!promo.active().equals("false")) {
        if (promo.type().equals("PERCENTAGE")) {
          discountRate = Double.parseDouble(promo.value()) / 100;
        } else if (promo.type().equals("FIXED")) {
          // Bug: appliqué par ligne au lieu de global
          fixedDiscount = Double.parseDouble(promo.value());
        }
      }
    }

    // Calcul ligne avec réduction promo
    int qty = (Integer) order.qty();
    double lineTotal = qty * basePrice * (1 - discountRate) - fixedDiscount * qty;

    // Bonus matin (règle cachée basée sur heure)
    String time = order.time();
    int hour = Integer.parseInt(time.split(":")[0]);
    double morningBonus = 0;
    if (hour < 10) {
      morningBonus = lineTotal * 0.03; // 3% réduction supplémentaire
    }
    lineTotal = lineTotal - morningBonus;
    return new LineResult(lineTotal, morningBonus, qty, prod);
  }

  public static final class LineResult {
    public final double lineTotal;
    public final double morningBonus;
    public final int qty;
    public final Product product;

    public LineResult(double lineTotal, double morningBonus, int qty, Product product) {
      this.lineTotal = lineTotal;
      this.morningBonus = morningBonus;
      this.qty = qty;
      this.product = product;
    }
  }
}
