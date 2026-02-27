package service;

import domain.Constants;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoyaltyService {

  // Calcul points de fidélité (première duplication)
  public static Map<String, Double> computeLoyaltyPoints(List<Map<String, Object>> orders) {
    Map<String, Double> loyaltyPoints = new HashMap<>();
    for (Map<String, Object> o : orders) {
      String cid = (String) o.get("customer_id");
      loyaltyPoints.putIfAbsent(cid, 0.0);
      int qty = (Integer) o.get("qty");
      double unitPrice = (Double) o.get("unit_price");
      loyaltyPoints.put(cid, loyaltyPoints.get(cid) + qty * unitPrice * Constants.LOYALTY_RATIO);
    }
    return loyaltyPoints;
  }
}
