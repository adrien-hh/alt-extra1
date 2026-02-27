package service;

import static org.junit.jupiter.api.Assertions.*;

import domain.Constants;
import domain.Order;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class LoyaltyServiceTest {

  @Test
  void computeLoyaltyPoints_should_accumulate_by_customer_with_ratio() {
    Order o1 = new Order("O1", "C1", "P1", 2, 10.0, "2025-01-04", "12:00", "");
    Order o2 = new Order("O2", "C1", "P2", 1, 20.0, "2025-01-04", "12:00", "");
    double o1Points = o1.qty() * o1.unitPrice() * Constants.LOYALTY_RATIO;
    double o2Points = o2.qty() * o2.unitPrice() * Constants.LOYALTY_RATIO;

    Map<String, Double> pts = LoyaltyService.computeLoyaltyPoints(List.of(o1, o2));

    // Assertion pas fixée en cas de changement de LOYALTY_RATIO
    assertEquals(o1Points + o2Points, pts.get("C1"));
  }
}
