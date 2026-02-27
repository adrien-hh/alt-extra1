package io;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class CsvLoaders {
    // Lecture customers (parsing mélangé avec logique)
    public static Map<String, Map<String, String>> loadCustomers(String custPath)
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
    public static Map<String, Map<String, Object>> loadProducts(String prodPath) throws IOException {
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
    public static Map<String, Map<String, Double>> loadShippingZones(String shipPath)
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
    public static Map<String, Map<String, String>> loadPromotions(String promoPath) throws IOException {
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
    public static List<Map<String, Object>> loadOrders(String ordPath) throws IOException {
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
