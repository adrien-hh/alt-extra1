package service;

import domain.Customer;
import domain.Product;
import domain.Promotion;
import domain.ShippingZone;
import io.CsvLoaders;
import io.ReportOutputs;
import java.io.IOException;
import java.util.*;
import report.ReportGenerator;

public class OrderReportService {

  public static String run() throws IOException {

    Map<String, Customer> customers = CsvLoaders.loadCustomers();
    Map<String, Product> products = CsvLoaders.loadProducts();
    Map<String, ShippingZone> shippingZones = CsvLoaders.loadShippingZones();
    Map<String, Promotion> promotions = CsvLoaders.loadPromotions();
    List<Map<String, Object>> orders = CsvLoaders.loadOrders();

    Map<String, Double> loyaltyPoints = LoyaltyService.computeLoyaltyPoints(orders);

    Map<String, Map<String, Object>> totalsByCustomer =
        CustomerTotalsService.computeTotalsByCustomer(orders, products, promotions);

    ReportGenerator.ReportResult res =
        ReportGenerator.generate(
            customers, totalsByCustomer, loyaltyPoints, products, shippingZones);

    ReportOutputs.printReport(res.report);
    ReportOutputs.writeJson(res.jsonData);

    return res.report;
  }
}
