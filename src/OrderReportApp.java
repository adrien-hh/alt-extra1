import java.io.*;
import service.OrderReportService;

public class OrderReportApp {
  // Point d'entrée
  public static void main(String[] args) {
    try {
      OrderReportService.run();
    } catch (IOException e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
