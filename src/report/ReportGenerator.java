package report;

import domain.*;
import java.util.*;
import service.PricingService;

public class ReportGenerator {

  public static ReportResult generate(
      Map<String, Customer> customers,
      Map<String, Map<String, Object>> totalsByCustomer,
      Map<String, Double> loyaltyPoints,
      Map<String, Product> products,
      Map<String, ShippingZone> shippingZones) {
    // Génération rapport (mélange calculs + formatage + I/O)
    List<String> outputLines = new ArrayList<>();
    List<Map<String, Object>> jsonData = new ArrayList<>();
    double grandTotal = 0.0;
    double totalTaxCollected = 0.0;

    // Tri par ID client (comportement à préserver)
    List<String> sortedCustomerIds = new ArrayList<>(totalsByCustomer.keySet());
    Collections.sort(sortedCustomerIds);

    for (String cid : sortedCustomerIds) {
      Customer cust =
          customers.getOrDefault(cid, new Customer(cid, "Unknown", "BASIC", "ZONE1", "EUR"));
      String name = cust.name();
      String level = cust.level();
      String zone = cust.shippingZone();
      String currency = cust.currency();

      Map<String, Object> totals = totalsByCustomer.get(cid);
      double sub = (Double) totals.get("subtotal");

      // Calcul remises
      List<Order> items = (List<Order>) totals.get("items");
      double pts = loyaltyPoints.getOrDefault(cid, 0.0);

      PricingService.Discounts discounts =
          PricingService.computeDiscounts(sub, level, items, pts, Constants.MAX_DISCOUNT);

      double totalDiscount = discounts.totalDiscount;

      // Calcul taxe (gestion spéciale par produit)
      double taxable = sub - totalDiscount;
      double tax = PricingService.computeTax(taxable, items, products, Constants.TAX);

      // Frais de port complexes (duplication)
      double ship =
          PricingService.computeShipping(
              totals, sub, shippingZones, Constants.SHIPPING_LIMIT, zone);

      // Frais de gestion
      double handling = PricingService.computeHandling(items.size(), Constants.HANDLING_FEE);

      // Conversion devise
      double currencyRate = PricingService.computeCurrencyRate(currency);

      double total = Math.round((taxable + tax + ship + handling) * currencyRate * 100.0) / 100.0;
      grandTotal += total;
      totalTaxCollected += tax * currencyRate;

      // Formatage texte (dispersé, pas de méthode dédiée)
      outputLines.add(String.format("Customer: %s (%s)", name, cid));
      outputLines.add(String.format("Level: %s | Zone: %s | Currency: %s", level, zone, currency));
      outputLines.add(String.format("Subtotal: %.2f", sub));
      outputLines.add(String.format("Discount: %.2f", totalDiscount));
      outputLines.add(String.format("  - Volume discount: %.2f", discounts.volumeDiscount));
      outputLines.add(String.format("  - Loyalty discount: %.2f", discounts.loyaltyDiscount));
      double morningBonus = (Double) totals.get("morning_bonus");
      if (morningBonus > 0) {
        outputLines.add(String.format("  - Morning bonus: %.2f", morningBonus));
      }
      outputLines.add(String.format("Tax: %.2f", tax * currencyRate));
      outputLines.add(
          String.format("Shipping (%s, %.1fkg): %.2f", zone, (Double) totals.get("weight"), ship));
      if (handling > 0) {
        outputLines.add(String.format("Handling (%d items): %.2f", items.size(), handling));
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
