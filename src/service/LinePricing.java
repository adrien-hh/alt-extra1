package service;

import java.util.Map;

public class LinePricing {

  public static LineResult computeLineTotal(
      Map<String, Object> order,
      Map<String, Object> productFallback,
      Map<String, Map<String, String>> promotions) {
    // Récupération produit avec fallback
    Map<String, Object> prod = productFallback;
    double basePrice =
        prod.containsKey("price") ? (Double) prod.get("price") : (Double) order.get("unit_price");

    // Application promo (logique complexe et bugguée)
    String promoCode = (String) order.get("promo_code");
    double discountRate = 0;
    double fixedDiscount = 0;

    if (promoCode != null && !promoCode.isEmpty() && promotions.containsKey(promoCode)) {
      Map<String, String> promo = promotions.get(promoCode);
      if (!promo.get("active").equals("false")) {
        if (promo.get("type").equals("PERCENTAGE")) {
          discountRate = Double.parseDouble(promo.get("value")) / 100;
        } else if (promo.get("type").equals("FIXED")) {
          // Bug: appliqué par ligne au lieu de global
          fixedDiscount = Double.parseDouble(promo.get("value"));
        }
      }
    }

    // Calcul ligne avec réduction promo
    int qty = (Integer) order.get("qty");
    double lineTotal = qty * basePrice * (1 - discountRate) - fixedDiscount * qty;

    // Bonus matin (règle cachée basée sur heure)
    String time = (String) order.get("time");
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
    public final Map<String, Object> product;

    public LineResult(double lineTotal, double morningBonus, int qty, Map<String, Object> product) {
      this.lineTotal = lineTotal;
      this.morningBonus = morningBonus;
      this.qty = qty;
      this.product = product;
    }
  }
}
