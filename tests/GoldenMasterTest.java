import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GoldenMasterTest {

  private static final String EXPECTED_FILE = "legacy/expected/report.txt";
  private static final String EXPECTED_JSON = "legacy/output.json";
  private static final String REFACTOR_OUTPUT_DIR = "src/output";

  @BeforeAll
  static void generateGoldenMaster() throws Exception {
    // Texte
    Path expectedPath = Path.of(EXPECTED_FILE);
    Files.createDirectories(expectedPath.getParent());
    if (!Files.exists(expectedPath)) {
      String legacyOutput = runLegacy();
      Files.writeString(expectedPath, legacyOutput);
      System.out.println("✅ Golden Master texte généré : " + EXPECTED_FILE);
    }

    // JSON
    Path expectedJson = Path.of(EXPECTED_JSON);
    if (!Files.exists(expectedJson)) {
      // Le legacy a déjà écrit legacy/output.json en side effect
      Files.copy(Path.of("legacy/output.json"), expectedJson);
      System.out.println("✅ Golden Master JSON généré : " + EXPECTED_JSON);
    }
  }

  @Test
  void refactoredOutputMatchesLegacy() throws Exception {
    String expected = Files.readString(Path.of(EXPECTED_FILE));
    String actual = runRefactored();

    assertEquals(expected, actual, "❌ La sortie texte diffère du Golden Master !");
    System.out.println("✅ Sortie texte identique → Test PASSE");
  }

  @Test
  void refactoredJsonMatchesLegacy() throws Exception {
    // Relit le JSON produit par le refactoré (déjà écrit en side effect)
    String expectedJson = Files.readString(Path.of(EXPECTED_JSON));
    String actualJson = Files.readString(Path.of(REFACTOR_OUTPUT_DIR, "/output.json"));

    assertEquals(expectedJson, actualJson, "❌ Le JSON produit diffère du Golden Master !");
    System.out.println("✅ JSON identique → Test PASSE");
  }

  private static String runLegacy() throws Exception {
    return captureOutput(() -> legacy.OrderReportLegacy.main(new String[] {}));
  }

  private static String runRefactored() throws Exception {
    return captureOutput(() -> OrderReportApp.main(new String[] {}));
  }

  private static String captureOutput(RunnableWithException code) throws Exception {
    PrintStream originalOut = System.out;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    System.setOut(new PrintStream(baos, true, StandardCharsets.UTF_8));
    try {
      code.run();
    } finally {
      System.setOut(originalOut);
    }

    return baos.toString(StandardCharsets.UTF_8);
  }

  @FunctionalInterface
  interface RunnableWithException {
    void run() throws Exception;
  }
}
