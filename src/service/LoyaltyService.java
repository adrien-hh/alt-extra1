package service;

import domain.Constants;
import domain.Order;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoyaltyService {

  // Calcul points de fidélité (première duplication)
  public static Map<String, Double> computeLoyaltyPoints(List<Order> orders) {
    Map<String, Double> loyaltyPoints = new HashMap<>();
    for (Order o : orders) {
      String cid = o.customerId();
      loyaltyPoints.putIfAbsent(cid, 0.0);
      int qty = o.qty();
      double unitPrice = o.unitPrice();
      loyaltyPoints.put(cid, loyaltyPoints.get(cid) + qty * unitPrice * Constants.LOYALTY_RATIO);
    }
    return loyaltyPoints;
  }
}
