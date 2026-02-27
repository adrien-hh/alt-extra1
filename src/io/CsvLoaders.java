package io;

import domain.Customer;
import domain.Product;
import domain.Promotion;
import domain.ShippingZone;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class CsvLoaders {
  // Lecture customers (parsing mélangé avec logique)
  public static Map<String, Customer> loadCustomers() throws IOException {
    Map<String, Customer> customers = new HashMap<>();
    BufferedReader custReader = new BufferedReader(new FileReader(domain.Paths.CUST_PATH));
    String line;
    while ((line = custReader.readLine()) != null) {
      String[] parts = line.split(",");
      String id = parts[0];
      String name = parts[1];
      String level = parts.length > 2 ? parts[2] : "BASIC";
      String shippingZone = parts.length > 3 ? parts[3] : "ZONE1";
      String currency = parts.length > 4 ? parts[4] : "EUR";

      customers.put(id, new Customer(id, name, level, shippingZone, currency));
    }
    custReader.close();
    return customers;
  }

  // Lecture products (duplication parsing, méthode différente)
  public static Map<String, Product> loadProducts() throws IOException {
    Map<String, Product> products = new HashMap<>();
    List<String> prodLines = Files.readAllLines(Paths.get(domain.Paths.PROD_PATH));
    for (int i = 1; i < prodLines.size(); i++) {
      try {
        String[] parts = prodLines.get(i).split(",");
        String id = parts[0];
        String name = parts[1];
        String category = parts[2];
        double price = Double.parseDouble(parts[3]);
        double weight = parts.length > 4 ? Double.parseDouble(parts[4]) : 1.0;
        boolean taxable = parts.length > 5 ? parts[5].equals("true") : true;

        products.put(id, new Product(id, name, category, price, weight, taxable));
      } catch (Exception e) {
        // Skip silencieux
        continue;
      }
    }
    return products;
  }

  // Lecture shipping zones (encore une autre variation avec Scanner)
  public static Map<String, ShippingZone> loadShippingZones() throws IOException {
    Map<String, ShippingZone> shippingZones = new HashMap<>();
    Scanner shipScanner = new Scanner(new File(domain.Paths.SHIP_PATH));
    if (shipScanner.hasNextLine()) {
      shipScanner.nextLine(); // skip header
    }
    while (shipScanner.hasNextLine()) {
      String ln = shipScanner.nextLine();
      String[] p = ln.split(",");
      double base = Double.parseDouble(p[1]);
      double perKg = p.length > 2 ? Double.parseDouble(p[2]) : 0.5;
      shippingZones.put(p[0], new ShippingZone(base, perKg));
    }
    shipScanner.close();
    return shippingZones;
  }

  // Lecture promotions (parsing avec try-catch global)
  public static Map<String, Promotion> loadPromotions() throws IOException {
    Map<String, Promotion> promotions = new HashMap<>();
    try {
      BufferedReader promoReader = new BufferedReader(new FileReader(domain.Paths.PROMO_PATH));
      promoReader.readLine(); // header
      String promoLine;
      while ((promoLine = promoReader.readLine()) != null) {
        String[] p = promoLine.split(",");
        String code = p[0];
        String type = p[1];
        String value = p[2];
        String active = p.length > 3 ? p[3] : "true";
        promotions.put(p[0], new Promotion(code, type, value, active));
      }
      promoReader.close();
    } catch (FileNotFoundException e) {
      // Ignore si fichier promo absent
    }
    return promotions;
  }

  // Lecture orders (mélange parsing et validation)
  public static List<Map<String, Object>> loadOrders() throws IOException {
    List<Map<String, Object>> orders = new ArrayList<>();
    BufferedReader ordReader = new BufferedReader(new FileReader(domain.Paths.ORD_PATH));
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
