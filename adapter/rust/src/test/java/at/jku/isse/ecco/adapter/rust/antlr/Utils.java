package at.jku.isse.ecco.adapter.rust.antlr;

import at.jku.isse.ecco.service.EccoService;
import org.junit.jupiter.api.Assertions;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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

        StringBuilder diffReport = new StringBuilder();
        boolean filesAreEqual = true;

        try (BufferedReader expectedLines = Files.newBufferedReader(excepted);
             BufferedReader actualLines = Files.newBufferedReader(actual)) {
            String expectedLine;
            String actualLine;
            int lineNumber = 0;

            while (true) {
                expectedLine = expectedLines.readLine();
                actualLine = actualLines.readLine();
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
        if (!filesAreEqual)  Assertions.fail("Files are not equal:\n" + diffReport);
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

    /**
     * Returns all unordered combinations of size 3 from the provided list.
     *
     * @param <T>  element type
     * @param items input list
     * @return list of combinations; each combination is a List<T> of size 3
     * @throws NullPointerException if items is null
     */
    public static <T> List<List<T>> combinationsOfThree(List<T> items) {
        Objects.requireNonNull(items, "items must not be null");
        int n = items.size();
        if (n < 3) {
            return Collections.emptyList();
        }
        List<List<T>> result = new ArrayList<>((n * (n - 1) * (n - 2)) / 6);
        for (int i = 0; i < n - 2; i++) {
            for (int j = i + 1; j < n - 1; j++) {
                for (int k = j + 1; k < n; k++) {
                    List<T> combo = new ArrayList<>(3);
                    combo.add(items.get(i));
                    combo.add(items.get(j));
                    combo.add(items.get(k));
                    result.add(combo);
                }
            }
        }
        return result;
    }
}
