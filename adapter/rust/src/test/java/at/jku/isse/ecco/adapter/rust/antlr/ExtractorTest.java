package at.jku.isse.ecco.adapter.rust.antlr;

import at.jku.isse.ecco.service.EccoService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static at.jku.isse.ecco.adapter.rust.antlr.Utils.*;


class ExtractorTest {
    @Test
    void commitVariant() throws Exception {
        Path testDir = Paths.get("src/test/resources/extractor/commit_test").toAbsolutePath();
        Files.createDirectories(testDir);
        // Initialize ECCO service
        EccoService service = setupEccoService(testDir);
        Path outputBase = Paths.get("src/test/resources/extractor/output");
        Files.createDirectories(outputBase);

        List<String> files = getOutputFiles(outputBase);
        String lastFolder = files.removeLast();
        Assertions.assertNotNull(service);
        for (String folder : files) {
            service.setBaseDir(outputBase.resolve(folder));
            service.commit(folder);
        }
        service.setBaseDir(testDir);
        service.checkout(service.getConfigStringFromFile(outputBase.resolve(lastFolder).resolve(".config")));

        // Verify files
        assertFoldersEqual(outputBase.resolve(lastFolder), testDir);
    }

    /**
     * Commits all permutations of totalVariantsToCommit variants, checking out the remaining one each time.
     * @param variantLocation
     * @param eccoLocation
     * @param totalVariantsToCommit
     * @throws IOException
     */
    private Stream<DynamicTest> CommitAllPermutations(Path variantLocation, Path eccoLocation, int totalVariantsToCommit) {
        List<String> files = getOutputFiles(variantLocation);
        if (files.size() < totalVariantsToCommit) {
            // If there are not enough files, commit all but one
            totalVariantsToCommit = files.size() - 1;
        }
        List<List<String>> permutations = Permutations.permutations(files, totalVariantsToCommit);
        // For each permutation, commit the first totalVariantsToCommit and checkout the remaining one
        return permutations.stream().map(perm -> DynamicTest.dynamicTest("Commit variants " + String.join(", ", perm.subList(0, perm.size() - 1)) + " checkout " + perm.getLast(), () -> {
            // Create a unique test directory for each permutation
            String currentTestDirName = String.join("_", perm.subList(0, perm.size() - 1) + "_checkout_" + perm.getLast()).replace(" ", "");
            Path currentTestDir = Files.createDirectories(eccoLocation.resolve(currentTestDirName));
            try (EccoService service = setupEccoService(currentTestDir)) {
                int permutationsToCommit = perm.size() - 1;
                for (int i = 0; i < permutationsToCommit - 1; i++) {
                    String folder = perm.get(i);
                    service.setBaseDir(variantLocation.resolve(folder));
                    service.commit("commited" + folder);
                }
                Path checkoutLocation = Files.createDirectories(currentTestDir.resolve("checkout"));
                service.setBaseDir(checkoutLocation);
                String checkoutFolder = perm.getLast();
                service.checkout(service.getConfigStringFromFile(variantLocation.resolve(checkoutFolder)));
                // Verify files
                assertFoldersEqual(variantLocation.resolve(checkoutFolder), checkoutLocation);
            }
        }));
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

    // Commit two variants and checkout a third one, check results
    @TestFactory
    Stream<DynamicTest> commitTwoVariantsCheckoutThird() throws Exception {
        final Path outputBase = Paths.get("src/test/resources/extractor/output");
        final Path testDir = Paths.get("src/test/resources/extractor/commit_two_checkout_third").toAbsolutePath();
        // Clean up test directory if it exists
        deleteDirectoryRecursively(testDir);
        return CommitAllPermutations(outputBase, testDir, 3);
    }

    @TestFactory
    Stream<DynamicTest> commitFourVariantsCheckoutFifth() throws Exception {
        final Path outputBase = Paths.get("src/test/resources/extractor/output");
        final Path testDir = Paths.get("src/test/resources/extractor/commit_four_checkout_fifth").toAbsolutePath();
        // Clean up test directory if it exists
        deleteDirectoryRecursively(testDir);
        return CommitAllPermutations(outputBase, testDir, 5);
    }

    @TestFactory
    Stream<DynamicTest> commitThreeVariantsCheckoutFourth() throws Exception {
        final Path outputBase = Paths.get("src/test/resources/extractor/output");
        final Path testDir = Paths.get("src/test/resources/extractor/commit_three_checkout_fourth").toAbsolutePath();
        // Clean up test directory if it exists
        deleteDirectoryRecursively(testDir);
        return CommitAllPermutations(outputBase, testDir, 4);
    }


}
