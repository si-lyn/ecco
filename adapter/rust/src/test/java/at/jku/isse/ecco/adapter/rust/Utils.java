package at.jku.isse.ecco.adapter.rust;

import at.jku.isse.ecco.adapter.rust.antlr.RustLexer;
import at.jku.isse.ecco.adapter.rust.antlr.RustParser;
import at.jku.isse.ecco.service.EccoService;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Assertions;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class Utils {
    /**
     * Get a sorted list of output files (directories) in the given output base path.
     * @param outputBase
     * @return
     */
    public static List<String> getOutputFiles(Path outputBase) {
        List<String> files;
        try (Stream<Path> filesStream = Files.list(outputBase)) {
            files = filesStream.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return files;
    }

    /** Assert that all .rs files in two folders are equal, ignoring comments and empty lines.
     * @param expectedDir expected folder
     * @param actualDir actual folder
     */
    public static void assertFoldersEqual(Path expectedDir, Path actualDir) {
        try (Stream<Path> expectedFiles = Files.walk(expectedDir)) {
            expectedFiles.filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".rs"))
                    .forEach(expectedFile -> {
                        Path relativePath = expectedDir.relativize(expectedFile);
                        Path actualFile = actualDir.resolve(relativePath);
                        assertFilesEqual(expectedFile, actualFile);
            });
        } catch (IOException e) {
            Assertions.fail("Failed to compare folders: " + e.getMessage(), e);
        }
    }

    /** Assert that two files are equal, ignoring comments and empty lines.
     * If they differ, try to parse both files with ANTLR and compare the parse trees.
     * @param excepted what we expect the file to be
     * @param actual what the file actually is
     */
    public static void assertFilesEqual(Path excepted, Path actual) {
        Assertions.assertTrue(Files.exists(actual), "Actual file does not exist: " + actual);
        Assertions.assertTrue(Files.exists(excepted), "Excepted file does not exist: " + excepted);

        try {
            Assertions.assertNotEquals(0, Files.size(actual), "Actual file was empty: " + actual);
        } catch (IOException e) {
            Assertions.fail("Could not get size of actual file: " + actual);
        }
        StringBuilder diffReport = new StringBuilder();
        boolean filesAreEqual = true;
        try (BufferedReader expectedLines = Files.newBufferedReader(excepted);
             BufferedReader actualLines = Files.newBufferedReader(actual)) {
            String expectedLine;
            String actualLine;
            int lineNumber = 0;

            while (filesAreEqual) {
                // Skip empty lines in actual file
                do {
                    actualLine = actualLines.readLine();
                } while (actualLine != null && (actualLine.trim().startsWith("///") || actualLine.trim().startsWith("//") || actualLine.trim().isEmpty()));
                // Skip empty lines in expected file
                do {
                    expectedLine = expectedLines.readLine();
                } while (expectedLine != null && (expectedLine.trim().startsWith("///") || expectedLine.trim().startsWith("//") || expectedLine.trim().isEmpty()));

                if (expectedLine == null && actualLine == null) {
                    break; // End of both files
                }
                lineNumber++;
                if (actualLine == null) {
                    filesAreEqual = false;
                    diffReport.append(String.format("Line %d differs:%nExpected: %s%nActual:   <no line>%n%n", lineNumber, expectedLine.trim()));
                    continue;
                }

                String expectedTrimmed = expectedLine != null ? expectedLine.trim() : "<no line>";
                String actualTrimmed = actualLine.trim();

                if (!expectedTrimmed.equals(actualTrimmed)) {
                    filesAreEqual = false;
                    diffReport.append(String.format("Line %d differs:%nExpected: %s%nActual:   %s%n%n", lineNumber, expectedTrimmed, actualTrimmed));
                }
            }
        } catch (IOException e) {
            Assertions.fail("Could not read files for comparison: " + e.getMessage());
        }
        if (!filesAreEqual) {
            // try using antlr to parse both files and compare the parse trees
            try {
                RustLexer expectedLexer = new RustLexer(CharStreams.fromPath(excepted));
                expectedLexer.removeErrorListeners();
                RustParser expectedParser = new RustParser(new CommonTokenStream(expectedLexer));
                expectedParser.removeErrorListeners();
                RustLexer actualLexer = new RustLexer(CharStreams.fromPath(actual));
                actualLexer.removeErrorListeners();
                RustParser actualParser = new RustParser(new CommonTokenStream(actualLexer));
                actualParser.removeErrorListeners();
                ParseTree expectedTree = expectedParser.crate();
                ParseTree actualTree = actualParser.crate();
                assertParseTreesEqual(expectedTree, actualTree);
            } catch (IOException e) {
                Assertions.fail("failed to parse file with ANTLR: " + e.getMessage());
            } catch (AssertionError e) {
                String errorReport = "File " + actual + " differs from expected " + excepted + ":\n" +
                        "Parse tree comparison failed: " + e.getMessage() + "\n" +
                        "Line by line comparison failed:\n" + diffReport;
                Assertions.fail(errorReport);
            }
        }
    }

    /** Recursively assert two antlr parse trees are equal */
    private static void assertParseTreesEqual(ParseTree expected, ParseTree actual) {
        if (expected == null || actual == null) {
            return;
        }
        // base case: both are leaves
        if (expected.getChildCount() == 0 && actual.getChildCount() == 0) {
            if (!expected.getText().equals(actual.getText())) {
                Assertions.fail("Parse trees differ at leaf nodes:\nExpected: " + expected.getText() + "\nActual: " + actual.getText());
            }
            return;
        }
        // recursively check children
        for (int i = 0; i < expected.getChildCount(); i++) {
            assertParseTreesEqual(expected.getChild(i), actual.getChild(i));
        }

    }

    /** Recursively delete a directory and all its contents */
    public static void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder()) // delete children before parents
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

    /** Setup an ECCO service for testing
     * @param testDir directory to use for the ECCO service
     * @return initialized ECCO service
     */
    public static EccoService setupEccoService(Path testDir) {
        EccoService service = null;
        try {
            service = new EccoService(testDir);
            service.init();
        } catch (Exception e) {
            Assertions.fail("Failed to setup EccoService: " + e.getMessage(), e);
        }
        return service;
    }

    /** Run a cargo command in the given directory
     *
     * @param directory The directory to run the command in
     * @param command   The cargo command to run (e.g., "check", "build", "clean")
     * @return The exit code of the command
     * @throws Exception If an error occurs while running the command
     */
    public static int runCargoCommand(Path directory, String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("cargo", command);
        pb.directory(directory.toFile());
        Process process = pb.start();
        return process.waitFor();
    }
}
