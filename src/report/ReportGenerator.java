package report;

import service.PricingService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static domain.Constants.*;
import static domain.Constants.HANDLING_FEE;

public class ReportGenerator {

  public static ReportResult generate(
      Map<String, Map<String, String>> customers,
      Map<String, Map<String, Object>> totalsByCustomer,
      Map<String, Double> loyaltyPoints,
      Map<String, Map<String, Object>> products,
      Map<String, Map<String, Double>> shippingZones,
      double TAX,
      double SHIPPING_LIMIT,
      double HANDLING_FEE,
      double MAX_DISCOUNT) {
    // Génération rapport (mélange calculs + formatage + I/O)
    List<String> outputLines = new ArrayList<>();
    List<Map<String, Object>> jsonData = new ArrayList<>();
    double grandTotal = 0.0;
    double totalTaxCollected = 0.0;

    // Tri par ID client (comportement à préserver)
    List<String> sortedCustomerIds = new ArrayList<>(totalsByCustomer.keySet());
    Collections.sort(sortedCustomerIds);

    for (String cid : sortedCustomerIds) {
      Map<String, String> cust = customers.getOrDefault(cid, new HashMap<>());
      String name = cust.getOrDefault("name", "Unknown");
      String level = cust.getOrDefault("level", "BASIC");
      String zone = cust.getOrDefault("shipping_zone", "ZONE1");
      String currency = cust.getOrDefault("currency", "EUR");

      Map<String, Object> totals = totalsByCustomer.get(cid);
      double sub = (Double) totals.get("subtotal");

      // Remise par paliers (duplication + magic numbers)
      double disc = 0.0;
      if (sub > 50) {
        disc = sub * 0.05;
      }
      if (sub > 100) {
        disc = sub * 0.10; // écrase la précédente (bug intentionnel)
      }
      if (sub > 500) {
        disc = sub * 0.15;
      }
      if (sub > 1000 && level.equals("PREMIUM")) {
        disc = sub * 0.20;
      }

      // Bonus weekend (règle cachée basée sur date)
      List<Map<String, Object>> items = (List<Map<String, Object>>) totals.get("items");
      String firstOrderDate = items.size() > 0 ? (String) items.get(0).get("date") : "";
      int dayOfWeek = 0;
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
      // Calendar: 1=Sunday, 7=Saturday
      if (dayOfWeek == 1 || dayOfWeek == 7) {
        disc = disc * 1.05; // 5% bonus sur remise
      }

      // Calcul remise fidélité (duplication)
      double loyaltyDiscount = 0.0;
      double pts = loyaltyPoints.getOrDefault(cid, 0.0);
      if (pts > 100) {
        loyaltyDiscount = Math.min(pts * 0.1, 50.0);
      }
      if (pts > 500) {
        loyaltyDiscount = Math.min(pts * 0.15, 100.0); // écrase précédent
      }

      // Plafond remise global (règle cachée)
      double totalDiscount = disc + loyaltyDiscount;
      if (totalDiscount > MAX_DISCOUNT) {
        totalDiscount = MAX_DISCOUNT;
        // Ajustement proportionnel (logique complexe)
        double ratio = (disc + loyaltyDiscount) > 0 ? MAX_DISCOUNT / (disc + loyaltyDiscount) : 1;
        disc = disc * ratio;
        loyaltyDiscount = loyaltyDiscount * ratio;
      }

      // Calcul taxe (gestion spéciale par produit)
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

      // Frais de gestion (magic number + condition cachée)
      double handling = 0.0;
      int itemCount = items.size();
      if (itemCount > 10) {
        handling = HANDLING_FEE;
      }
      if (itemCount > 20) {
        handling = HANDLING_FEE * 2; // double pour grosses commandes
      }

      // Conversion devise (règle cachée pour non-EUR)
      double currencyRate = 1.0;
      if (currency.equals("USD")) {
        currencyRate = 1.1;
      } else if (currency.equals("GBP")) {
        currencyRate = 0.85;
      }

      double total = Math.round((taxable + tax + ship + handling) * currencyRate * 100.0) / 100.0;
      grandTotal += total;
      totalTaxCollected += tax * currencyRate;

      // Formatage texte (dispersé, pas de méthode dédiée)
      outputLines.add(String.format("Customer: %s (%s)", name, cid));
      outputLines.add(String.format("Level: %s | Zone: %s | Currency: %s", level, zone, currency));
      outputLines.add(String.format("Subtotal: %.2f", sub));
      outputLines.add(String.format("Discount: %.2f", totalDiscount));
      outputLines.add(String.format("  - Volume discount: %.2f", disc));
      outputLines.add(String.format("  - Loyalty discount: %.2f", loyaltyDiscount));
      double morningBonus = (Double) totals.get("morning_bonus");
      if (morningBonus > 0) {
        outputLines.add(String.format("  - Morning bonus: %.2f", morningBonus));
      }
      outputLines.add(String.format("Tax: %.2f", tax * currencyRate));
      outputLines.add(String.format("Shipping (%s, %.1fkg): %.2f", zone, weight, ship));
      if (handling > 0) {
        outputLines.add(String.format("Handling (%d items): %.2f", itemCount, handling));
      }
      outputLines.add(String.format("Total: %.2f %s", total, currency));
      outputLines.add(String.format("Loyalty Points: %d", (int) Math.floor(pts)));
      outputLines.add("");

      // Export JSON en parallèle (side effect)
      Map<String, Object> jsonEntry = new HashMap<>();
      jsonEntry.put("customer_id", cid);
      jsonEntry.put("name", name);
      jsonEntry.put("total", total);
      jsonEntry.put("currency", currency);
      jsonEntry.put("loyalty_points", (int) Math.floor(pts));
      jsonData.add(jsonEntry);
    }

    outputLines.add(String.format("Grand Total: %.2f EUR", grandTotal));
    outputLines.add(String.format("Total Tax Collected: %.2f EUR", totalTaxCollected));

    String result = String.join("\n", outputLines);
    return new ReportResult(result, jsonData);
    }

    public static final class ReportResult {
        public final String report;
        public final List<Map<String, Object>> jsonData;

        public ReportResult(String report, List<Map<String, Object>> jsonData) {
            this.report = report;
            this.jsonData = jsonData;
        }
    }
}
