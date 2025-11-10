package at.jku.isse.ecco.adapter.rust.antlr;

import at.jku.isse.ecco.service.EccoService;
import org.junit.jupiter.api.Assertions;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
    public static List<String> getOutputFiles(Path outputBase) {
        List<String> files;
        try (Stream<Path> filesStream = Files.list(outputBase)) {
            files = filesStream.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return files;
    }

    public static void assertFoldersEqual(Path expectedDir, Path actualDir) throws Exception {
        try (Stream<Path> expectedFiles = Files.walk(expectedDir)) {
            expectedFiles.filter(Files::isRegularFile).filter(file -> file.toString().endsWith(".rs")).forEach(expectedFile -> {
                Path relativePath = expectedDir.relativize(expectedFile);
                Path actualFile = actualDir.resolve(relativePath);
                try {
                    assertFilesEqual(expectedFile, actualFile);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public static void assertFilesEqual(Path excepted, Path actual) throws Exception {
        if (!Files.exists(excepted)) throw new IllegalArgumentException("File does not exist: " + excepted);
        if (!Files.exists(actual)) throw new IllegalArgumentException("File does not exist: " + actual);

        Assertions.assertNotEquals(0, Files.size(actual), "Expected file is empty: " + actual);
        StringBuilder diffReport = new StringBuilder();
        boolean filesAreEqual = true;

        try (BufferedReader expectedLines = Files.newBufferedReader(excepted);
             BufferedReader actualLines = Files.newBufferedReader(actual)) {
            String expectedLine;
            String actualLine;
            int lineNumber = 0;

            while (true) {
                // Skip empty lines in actual file
                do {
                    actualLine = actualLines.readLine();
                } while (actualLine != null && (actualLine.trim().startsWith("///") || actualLine.trim().startsWith("//") || actualLine.trim().isEmpty()));
                // Skip empty lines in expected file
                do {
                    expectedLine = expectedLines.readLine();
                } while (expectedLine != null && (expectedLine.trim().startsWith("///") || expectedLine.trim().startsWith("//") || expectedLine.trim().isEmpty()));

                lineNumber++;
                if (expectedLine == null && actualLine == null) {
                    break; // End of both files
                }
                String expectedTrimmed = expectedLine != null ? expectedLine.trim() : "<no line>";
                String actualTrimmed = actualLine != null ? actualLine.trim() : "<no line>";

                if (!expectedTrimmed.equals(actualTrimmed)) {
                    filesAreEqual = false;
                    diffReport.append(String.format("Line %d differs:%nExpected: %s%nActual:   %s%n%n", lineNumber, expectedTrimmed, actualTrimmed));
                }
            }
        }
        if (!filesAreEqual) {
            Assertions.fail("File at Path" + actual + " has failed: \n" + diffReport);
        }
    }

    public static void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                walk.sorted((a, b) -> b.compareTo(a)) // delete children before parents
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException e) {
                                System.err.println("Failed to delete: " + p);
                            }
                        });
            }
        }
    }

    public static EccoService setupEccoService(Path testDir) {
        EccoService service = null;
        try {
            service = new EccoService(testDir);
            service.init();
        } catch (Exception e) {
            System.out.println("Exception during ECCO setup: " + e.getMessage());
        }
        return service;
    }
}
