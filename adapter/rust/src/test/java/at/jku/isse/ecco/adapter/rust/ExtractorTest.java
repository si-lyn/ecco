package at.jku.isse.ecco.adapter.rust;

import at.jku.isse.ecco.service.EccoService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static at.jku.isse.ecco.adapter.rust.Utils.*;


class ExtractorTest {
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
        List<List<String>> permutations = Picks.generatePicks(files, totalVariantsToCommit);
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
            }
        }));
    }

    private Stream<DynamicTest> commitAndCheckoutTest(int variantsToCommit, String testDirName) throws Exception {
        final Path outputBase = Paths.get("src/test/resources/extractor/output");
        final Path testDir = Paths.get("src/test/resources/extractor/" + testDirName).toAbsolutePath();
        // Clean up test directory if it exists
        deleteDirectoryRecursively(testDir);
        return CommitAllPermutations(outputBase, testDir, variantsToCommit);
    }

    // Commit two variants and checkout a third one, check results
    @TestFactory
    Stream<DynamicTest> commitTwoVariantsCheckoutThird() throws Exception {
        return commitAndCheckoutTest(2, "commit_two_checkout_third");
    }

    @TestFactory
    Stream<DynamicTest> commitThreeVariantsCheckoutFourth() throws Exception {
        return commitAndCheckoutTest(3, "commit_three_checkout_fourth");
    }

    @TestFactory
    Stream<DynamicTest> commitFourVariantsCheckoutFifth() throws Exception {
        return commitAndCheckoutTest(4, "commit_four_checkout_fifth");
    }

    @Test
    void commitAllVariantsAndCheckoutAll() throws Exception {
        final Path outputBase = Paths.get("src/test/resources/extractor/output");
        final Path testDir = Paths.get("src/test/resources/extractor/commit_all_checkout_all").toAbsolutePath();
        // Clean up test directory if it exists
        deleteDirectoryRecursively(testDir);
        Files.createDirectories(testDir);
        List<String> files = getOutputFiles(outputBase);
        Path eccoDir = Files.createDirectories(testDir.resolve("ecco_repo"));
        try (EccoService service = new EccoService(eccoDir)) {
            service.init();
            Assertions.assertNotNull(service);
            // Commit all variants
            for (String folder : files) {
                service.setBaseDir(outputBase.resolve(folder));
                service.commit("commited" + folder);
            }
            // Checkout each variant and verify
            for (String folder : files) {
                Path checkoutLocation = Files.createDirectories(testDir.resolve("checkout_" + folder));
                service.setBaseDir(checkoutLocation);
                service.checkout(service.getConfigStringFromFile(outputBase.resolve(folder)));
            }
        }
    }

    @TestFactory
    @Execution(ExecutionMode.CONCURRENT)
    Stream<DynamicNode> assertCommmitVariantTests() throws Exception {
        final Path outputBase = Paths.get("src/test/resources/extractor/output").toAbsolutePath();
        // test dirs containing all folders that start with commit_
        List<Path> testDirs = new ArrayList<>();
        try (Stream<Path> paths = Files.list(Paths.get("src/test/resources/extractor"))) {
            paths.filter(Files::isDirectory)
                .filter(path -> path.getFileName().toString().startsWith("commit_"))
                .map(Path::toAbsolutePath)
                .forEach(testDirs::add);
        }

        return testDirs.stream().map(testDir -> {
            List<String> files = getOutputFiles(testDir);
            return DynamicContainer.dynamicContainer("Commit test for " + testDir.getFileName(),
                    files.stream()
                    .map(Path::of)
                    .map(testDir::resolve)
                    .map(folder -> DynamicTest.dynamicTest("Verify checkout of " + folder.getFileName(), () -> {
                        Path checkoutLocation = folder.resolve("checkout");
                        String folderName = folder.getFileName().toString();
                        int checkoutIndex = folderName.indexOf("checkout_");
                        if (checkoutIndex == -1) {
                            throw new IllegalArgumentException("Folder name does not contain 'checkout_': " + folderName);
                        }
                        folderName = folderName.substring(checkoutIndex + "checkout_".length());
                        assertFoldersEqual(outputBase.resolve(folderName), checkoutLocation);
                    }))
            );
        });
    }
}
