import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GoldenMasterTest {

  private static final String EXPECTED_FILE = "legacy/expected/report.txt";

  // ─── Étape 1 & 2 & 3 : génère la référence si elle n'existe pas ───────────
  @BeforeAll
  static void generateGoldenMaster() throws Exception {
    Path expectedPath = Path.of(EXPECTED_FILE);
    Files.createDirectories(expectedPath.getParent());

    if (!Files.exists(expectedPath)) {
      String legacyOutput = runLegacy();
      Files.writeString(expectedPath, legacyOutput);
      System.out.println("✅ Golden Master généré : " + EXPECTED_FILE);
    } else {
      System.out.println("ℹ️  Golden Master existant conservé.");
    }
  }

  // ─── Étape 4, 5 & 6 : compare refactoré vs référence ─────────────────────
  @Test
  void refactoredOutputMatchesLegacy() throws Exception {
    String expected = Files.readString(Path.of(EXPECTED_FILE));
    String actual = runRefactored();

    assertEquals(expected, actual, "❌ La sortie refactorisée diffère du Golden Master !");
    System.out.println("✅ Les sorties sont identiques → Test PASSE");
  }

  // ──────────────────────────────────────────────────────────────────────────

  private static String runLegacy() throws Exception {
    return captureOutput(() -> legacy.OrderReportLegacy.main(new String[] {}));
  }

  private static String runRefactored() throws Exception {
    return captureOutput(() -> OrderReportRefactor.main(new String[] {}));
  }

  private static String captureOutput(RunnableWithException code) throws Exception {
    PrintStream originalOut = System.out;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    System.setOut(new PrintStream(baos));
    try {
      code.run();
    } finally {
      System.setOut(originalOut); // toujours restaurer !
    }

    return baos.toString();
  }

  @FunctionalInterface
  interface RunnableWithException {
    void run() throws Exception;
  }
}
