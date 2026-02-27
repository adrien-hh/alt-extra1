package service;

import domain.Product;
import domain.Promotion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerTotalsService {

  // Groupement par client (logique métier mélangée avec aggregation)
  public static Map<String, Map<String, Object>> computeTotalsByCustomer(
          List<Map<String, Object>> orders,
          Map<String, Product> products,
          Map<String, Promotion> promotions) {

    Map<String, Map<String, Object>> totalsByCustomer = new HashMap<>();

    for (Map<String, Object> o : orders) {
      String cid = (String) o.get("customer_id");

      // Récupération produit avec fallback
      Product prod = products.get(o.get("product_id"));

      LinePricing.LineResult priced = LinePricing.computeLineTotal(o, prod, promotions);

      if (!totalsByCustomer.containsKey(cid)) {
        Map<String, Object> totals = new HashMap<>();
        totals.put("subtotal", 0.0);
        totals.put("items", new ArrayList<Map<String, Object>>());
        totals.put("weight", 0.0);
        totals.put("promo_discount", 0.0);
        totals.put("morning_bonus", 0.0);
        totalsByCustomer.put(cid, totals);
      }

      Map<String, Object> totals = totalsByCustomer.get(cid);
      totals.put("subtotal", (Double) totals.get("subtotal") + priced.lineTotal);
      double weight = (prod != null) ? prod.weight() : 1.0;
      totals.put("weight", (Double) totals.get("weight") + weight * priced.qty);
      ((List<Map<String, Object>>) totals.get("items")).add(o);
      totals.put("morning_bonus", (Double) totals.get("morning_bonus") + priced.morningBonus);
    }
    return totalsByCustomer;
  }
}
