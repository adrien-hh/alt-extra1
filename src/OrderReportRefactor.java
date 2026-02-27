import java.io.*;
import java.nio.file.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.CsvLoaders;
import service.LoyaltyService;
import service.CustomerTotalsService;

import static domain.Constants.*;
import static domain.Paths.*;

public class OrderReportRefactor {

  public static String run() throws IOException {

    // Chargement des données depuis CSV
    Map<String, Map<String, String>> customers = CsvLoaders.loadCustomers(CUST_PATH);
    Map<String, Map<String, Object>> products = CsvLoaders.loadProducts(PROD_PATH);
    Map<String, Map<String, Double>> shippingZones = CsvLoaders.loadShippingZones(SHIP_PATH);
    Map<String, Map<String, String>> promotions = CsvLoaders.loadPromotions(PROMO_PATH);
    List<Map<String, Object>> orders = CsvLoaders.loadOrders(ORD_PATH);

    // Calcul points de fidélité
    Map<String, Double> loyaltyPoints = LoyaltyService.computeLoyaltyPoints(orders, LOYALTY_RATIO);

    // Groupement par client (logique métier mélangée avec aggregation)
    Map<String, Map<String, Object>> totalsByCustomer =
            CustomerTotalsService.computeTotalsByCustomer(orders, products, promotions);

    // Génération rapport (mélange calculs + formatage + I/O)
    report.ReportGenerator.ReportResult res =
            report.ReportGenerator.generate(
                    customers, totalsByCustomer, loyaltyPoints, products, shippingZones,
                    TAX, SHIPPING_LIMIT, HANDLING_FEE, MAX_DISCOUNT
            );

    String result = res.report;
    List<Map<String, Object>> jsonData = res.jsonData;

    // Side effects: print + file write
    System.out.println(result);

    // Export JSON surprise
    Files.createDirectories(Paths.get(OUTPUT_DIR));
    String outputPath = OUTPUT_DIR + "/output.json";
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
}
