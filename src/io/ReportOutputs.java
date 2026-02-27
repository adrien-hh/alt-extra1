package io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class ReportOutputs {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  public static void printReport(String report) {
    System.out.println(report);
  }

  public static void writeJson(List<Map<String, Object>> jsonData) throws IOException {
    Path outputDir = Path.of(domain.Paths.OUTPUT_DIR);
    Files.createDirectories(outputDir);
    String outputPath = domain.Paths.OUTPUT_DIR + domain.Paths.JSON_FILENAME;
    try (FileWriter writer = new FileWriter(outputPath)) {
      GSON.toJson(jsonData, writer);
    }
  }
}
