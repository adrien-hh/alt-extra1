import java.io.*;
import java.nio.file.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class OrderReportRefactor {

  // Constantes globales mal organisées (mélange styles)
  private static final double TAX = 0.2;
  private static final double SHIPPING_LIMIT = 50;
  private static final double SHIP = 5.0;
  private static final int premium_threshold = 1000;
  private static final double LOYALTY_RATIO = 0.01;
  private static double handling_fee = 2.5;
  public static final double MAX_DISCOUNT = 200;

  // Méthode principale qui fait TOUT (300+ lignes)
  public static String run() throws IOException {
    String base = System.getProperty("user.dir") + "/legacy";
    String custPath = base + "/data/customers.csv";
    String ordPath = base + "/data/orders.csv";
    String prodPath = base + "/data/products.csv";
    String shipPath = base + "/data/shipping_zones.csv";
    String promoPath = base + "/data/promotions.csv";

    // Lecture customers (parsing mélangé avec logique)
    Map<String, Map<String, String>> customers = loadCustomers(custPath);

    // Lecture products (duplication parsing, méthode différente)
    Map<String, Map<String, Object>> products = loadProducts(prodPath);

    // Lecture shipping zones (encore une autre variation avec Scanner)
    Map<String, Map<String, Double>> shippingZones = loadShippingZones(shipPath);

    // Lecture promotions (parsing avec try-catch global)
    Map<String, Map<String, String>> promotions = loadPromotions(promoPath);

    // Lecture orders (mélange parsing et validation)
    List<Map<String, Object>> orders = loadOrders(ordPath);

    // Calcul points de fidélité (première duplication)
    Map<String, Double> loyaltyPoints = new HashMap<>();
    for (Map<String, Object> o : orders) {
      String cid = (String) o.get("customer_id");
      loyaltyPoints.putIfAbsent(cid, 0.0);
      int qty = (Integer) o.get("qty");
      double unitPrice = (Double) o.get("unit_price");
      loyaltyPoints.put(cid, loyaltyPoints.get(cid) + qty * unitPrice * LOYALTY_RATIO);
    }

    // Groupement par client (logique métier mélangée avec aggregation)
    Map<String, Map<String, Object>> totalsByCustomer = new HashMap<>();
    for (Map<String, Object> o : orders) {
      String cid = (String) o.get("customer_id");

      // Récupération produit avec fallback
      Map<String, Object> prod = products.getOrDefault(o.get("product_id"), new HashMap<>());
      double basePrice =
          prod.containsKey("price") ? (Double) prod.get("price") : (Double) o.get("unit_price");

      // Application promo (logique complexe et bugguée)
      String promoCode = (String) o.get("promo_code");
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
      int qty = (Integer) o.get("qty");
      double lineTotal = qty * basePrice * (1 - discountRate) - fixedDiscount * qty;

      // Bonus matin (règle cachée basée sur heure)
      String time = (String) o.get("time");
      int hour = Integer.parseInt(time.split(":")[0]);
      double morningBonus = 0;
      if (hour < 10) {
        morningBonus = lineTotal * 0.03; // 3% réduction supplémentaire
      }
      lineTotal = lineTotal - morningBonus;

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
      totals.put("subtotal", (Double) totals.get("subtotal") + lineTotal);
      double weight = prod.containsKey("weight") ? (Double) prod.get("weight") : 1.0;
      totals.put("weight", (Double) totals.get("weight") + weight * qty);
      ((List<Map<String, Object>>) totals.get("items")).add(o);
      totals.put("morning_bonus", (Double) totals.get("morning_bonus") + morningBonus);
    }

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
        handling = handling_fee;
      }
      if (itemCount > 20) {
        handling = handling_fee * 2; // double pour grosses commandes
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

    // Side effects: print + file write
    System.out.println(result);

    // Export JSON surprise
    String outputPath = base + "/output.json";
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    FileWriter writer = new FileWriter(outputPath);
    gson.toJson(jsonData, writer);
    writer.close();

    return result;
  }

  // Point d'entrée
  public static void main(String[] args) {
    try {
      run();
    } catch (IOException e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
    }
  }

  // Lecture customers (parsing mélangé avec logique)
  private static Map<String, Map<String, String>> loadCustomers(String custPath)
      throws IOException {
    Map<String, Map<String, String>> customers = new HashMap<>();
    BufferedReader custReader = new BufferedReader(new FileReader(custPath));
    String line = custReader.readLine(); // skip header
    while ((line = custReader.readLine()) != null) {
      String[] parts = line.split(",");
      Map<String, String> cust = new HashMap<>();
      cust.put("id", parts[0]);
      cust.put("name", parts[1]);
      cust.put("level", parts.length > 2 ? parts[2] : "BASIC");
      cust.put("shipping_zone", parts.length > 3 ? parts[3] : "ZONE1");
      cust.put("currency", parts.length > 4 ? parts[4] : "EUR");
      customers.put(parts[0], cust);
    }
    custReader.close();
    return customers;
  }

  // Lecture products (duplication parsing, méthode différente)
  private static Map<String, Map<String, Object>> loadProducts(String prodPath) throws IOException {
    Map<String, Map<String, Object>> products = new HashMap<>();
    List<String> prodLines = Files.readAllLines(Paths.get(prodPath));
    for (int i = 1; i < prodLines.size(); i++) {
      try {
        String[] parts = prodLines.get(i).split(",");
        Map<String, Object> prod = new HashMap<>();
        prod.put("id", parts[0]);
        prod.put("name", parts[1]);
        prod.put("category", parts[2]);
        prod.put("price", Double.parseDouble(parts[3]));
        prod.put("weight", parts.length > 4 ? Double.parseDouble(parts[4]) : 1.0);
        prod.put("taxable", parts.length > 5 ? parts[5].equals("true") : true);
        products.put(parts[0], prod);
      } catch (Exception e) {
        // Skip silencieux
        continue;
      }
    }
    return products;
  }

  // Lecture shipping zones (encore une autre variation avec Scanner)
  private static Map<String, Map<String, Double>> loadShippingZones(String shipPath)
      throws IOException {
    Map<String, Map<String, Double>> shippingZones = new HashMap<>();
    Scanner shipScanner = new Scanner(new File(shipPath));
    if (shipScanner.hasNextLine()) {
      shipScanner.nextLine(); // skip header
    }
    while (shipScanner.hasNextLine()) {
      String ln = shipScanner.nextLine();
      String[] p = ln.split(",");
      Map<String, Double> zone = new HashMap<>();
      zone.put("base", Double.parseDouble(p[1]));
      zone.put("per_kg", p.length > 2 ? Double.parseDouble(p[2]) : 0.5);
      shippingZones.put(p[0], zone);
    }
    shipScanner.close();
    return shippingZones;
  }

  // Lecture promotions (parsing avec try-catch global)
  private static Map<String, Map<String, String>> loadPromotions(String promoPath) throws IOException {
    Map<String, Map<String, String>> promotions = new HashMap<>();
    try {
      BufferedReader promoReader = new BufferedReader(new FileReader(promoPath));
      promoReader.readLine(); // header
      String promoLine;
      while ((promoLine = promoReader.readLine()) != null) {
        String[] p = promoLine.split(",");
        Map<String, String> promo = new HashMap<>();
        promo.put("code", p[0]);
        promo.put("type", p[1]);
        promo.put("value", p[2]);
        promo.put("active", p.length > 3 ? p[3] : "true");
        promotions.put(p[0], promo);
      }
      promoReader.close();
    } catch (FileNotFoundException e) {
      // Ignore si fichier promo absent
    }
    return promotions;
  }

  // Lecture orders (mélange parsing et validation)
  private static List<Map<String, Object>> loadOrders(String ordPath) throws IOException {
    List<Map<String, Object>> orders = new ArrayList<>();
    BufferedReader ordReader = new BufferedReader(new FileReader(ordPath));
    ordReader.readLine(); // skip header
    String ordLine;
    while ((ordLine = ordReader.readLine()) != null) {
      try {
        String[] parts = ordLine.split(",");
        int qty = Integer.parseInt(parts[3]);
        double price = Double.parseDouble(parts[4]);

        if (qty <= 0 || price < 0) {
          continue; // validation silencieuse
        }

        Map<String, Object> order = new HashMap<>();
        order.put("id", parts[0]);
        order.put("customer_id", parts[1]);
        order.put("product_id", parts[2]);
        order.put("qty", qty);
        order.put("unit_price", price);
        order.put("date", parts.length > 5 ? parts[5] : "");
        order.put("promo_code", parts.length > 6 ? parts[6] : "");
        order.put("time", parts.length > 7 ? parts[7] : "12:00");
        orders.add(order);
      } catch (Exception e) {
        // Skip silencieux
        continue;
      }
    }
    ordReader.close();
    return orders;
  }
}
