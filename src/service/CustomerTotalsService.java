package service;

import domain.Order;
import domain.Product;
import domain.Promotion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerTotalsService {

  // Groupement par client (logique métier mélangée avec aggregation)
  public static Map<String, Map<String, Object>> computeTotalsByCustomer(
          List<Order> orders,
          Map<String, Product> products,
          Map<String, Promotion> promotions) {

    Map<String, Map<String, Object>> totalsByCustomer = new HashMap<>();

    for (Order o : orders) {
      String cid = o.customerId();

      // Récupération produit avec fallback
      Product prod = products.get(o.productId());

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
      ((List<Order>) totals.get("items")).add(o);
      totals.put("morning_bonus", (Double) totals.get("morning_bonus") + priced.morningBonus);
    }
    return totalsByCustomer;
  }
}
