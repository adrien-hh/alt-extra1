package service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class PricingService {

  public static double computeCurrencyRate(String currency) {
    double currencyRate = 1.0;
    if (currency.equals("USD")) {
      currencyRate = 1.1;
    } else if (currency.equals("GBP")) {
      currencyRate = 0.85;
    }
    return currencyRate;
  }

  public static double computeHandling(int itemCount, double handlingFee) {
    double handling = 0.0;
    if (itemCount > 10) {
      handling = handlingFee;
    }
    if (itemCount > 20) {
      handling = handlingFee * 2;
    }
    return handling;
  }

  public static double computeShipping(
      Map<String, Object> totals,
      double sub,
      Map<String, Map<String, Double>> shippingZones,
      double SHIPPING_LIMIT,
      String zone) {
    // Frais de port complexes (duplication)
    double ship = 0.0;
    double weight = (Double) totals.get("weight");

    if (sub < SHIPPING_LIMIT) {
      Map<String, Double> shipZone =
          shippingZones.getOrDefault(
              zone,
              new HashMap<String, Double>() {
                {
                  put("base", 5.0);
                  put("per_kg", 0.5);
                }
              });
      double baseShip = shipZone.get("base");

      if (weight > 10) {
        ship = baseShip + (weight - 10) * shipZone.get("per_kg");
      } else if (weight > 5) {
        // Palier intermédiaire (règle cachée)
        ship = baseShip + (weight - 5) * 0.3;
      } else {
        ship = baseShip;
      }

      // Majoration zones éloignées
      if (zone.equals("ZONE3") || zone.equals("ZONE4")) {
        ship = ship * 1.2;
      }
    } else {
      // Livraison gratuite mais frais manutention poids élevé
      if (weight > 20) {
        ship = (weight - 20) * 0.25;
      }
    }
    return ship;
  }

  public static double computeTax(
      double sub,
      double totalDiscount,
      List<Map<String, Object>> items,
      Map<String, Map<String, Object>> products,
      double TAX) {
    double taxable = sub - totalDiscount;
    double tax = 0.0;

    // Vérifier si tous produits taxables
    boolean allTaxable = true;
    for (Map<String, Object> item : items) {
      Map<String, Object> prod = products.get(item.get("product_id"));
      if (prod != null && prod.containsKey("taxable") && !(Boolean) prod.get("taxable")) {
        allTaxable = false;
        break;
      }
    }

    if (allTaxable) {
      tax = Math.round(taxable * TAX * 100.0) / 100.0; // Arrondi 2 décimales
    } else {
      // Calcul taxe par ligne (plus complexe)
      for (Map<String, Object> item : items) {
        Map<String, Object> prod = products.get(item.get("product_id"));
        if (prod != null && (!(prod.containsKey("taxable")) || (Boolean) prod.get("taxable"))) {
          double itemPrice =
              prod.containsKey("price")
                  ? (Double) prod.get("price")
                  : (Double) item.get("unit_price");
          int itemQty = (Integer) item.get("qty");
          tax += itemQty * itemPrice * TAX;
        }
      }
      tax = Math.round(tax * 100.0) / 100.0;
    }
    return tax;
  }

  public static Discounts computeDiscounts(
      double sub, String level, List<Map<String, Object>> items, double pts, double maxDiscount) {
    double disc = 0.0;
    if (sub > 50) disc = sub * 0.05;
    if (sub > 100) disc = sub * 0.10;
    if (sub > 500) disc = sub * 0.15;
    if (sub > 1000 && level.equals("PREMIUM")) disc = sub * 0.20;

    int dayOfWeek = 0;
    String firstOrderDate = items.size() > 0 ? (String) items.get(0).get("date") : "";
    if (!firstOrderDate.isEmpty()) {
      try {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date date = sdf.parse(firstOrderDate);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
      } catch (ParseException e) {
        // Ignore
      }
    }
    if (dayOfWeek == 1 || dayOfWeek == 7) {
      disc = disc * 1.05;
    }

    double loyaltyDiscount = 0.0;
    if (pts > 100) loyaltyDiscount = Math.min(pts * 0.1, 50.0);
    if (pts > 500) loyaltyDiscount = Math.min(pts * 0.15, 100.0);

    double totalDiscount = disc + loyaltyDiscount;
    if (totalDiscount > maxDiscount) {
      totalDiscount = maxDiscount;
      double ratio = (disc + loyaltyDiscount) > 0 ? maxDiscount / (disc + loyaltyDiscount) : 1;
      disc = disc * ratio;
      loyaltyDiscount = loyaltyDiscount * ratio;
    }

    return new Discounts(disc, loyaltyDiscount, totalDiscount);
  }

  public static final class Discounts {
    public final double volumeDiscount;
    public final double loyaltyDiscount;
    public final double totalDiscount;

    public Discounts(double volumeDiscount, double loyaltyDiscount, double totalDiscount) {
      this.volumeDiscount = volumeDiscount;
      this.loyaltyDiscount = loyaltyDiscount;
      this.totalDiscount = totalDiscount;
    }
  }
}
