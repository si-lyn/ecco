package at.jku.isse.ecco.adapter.rust.antlr;

import at.jku.isse.ecco.adapter.rust.extractor.Extractor;
import at.jku.isse.ecco.service.EccoService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static at.jku.isse.ecco.adapter.rust.antlr.Utils.*;
import static at.jku.isse.ecco.adapter.rust.extractor.Extractor.getFeaturesFromFile;

class ExtractorTest {
    @Test
    void createVariants() {
        Path input = Paths.get("src/test/resources/extractor/serde");
        Path featureDir = Paths.get("src/test/resources/extractor/featureLists");
        try (Stream<Path> files = Files.list(featureDir)) {
            files.filter(Files::isRegularFile).forEach(featureFile -> {
                String featureFileName = featureFile.getFileName().toString();
                String lastFolder = input.getFileName().toString();
                Path output = Paths.get("src/test/resources/extractor/output").resolve(lastFolder + "-" + featureFileName.replace(".csvconf", ""));
                Set<String> features = getFeaturesFromFile(input, featureFile);
                Extractor extractor = new Extractor(features, Paths.get("."));
                extractor.extractFromDirectory(input, output);
                extractor.createConfigFile(features, output.resolve(".config"));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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

    // Try to commit all variants except one and checkout the remaining one, and verify. Then repeat for all variants.
    @TestFactory
    Stream<DynamicTest> commitAllButOneVariants() throws Exception {
        final Path outputBase = Paths.get("src/test/resources/extractor/output");
        List<String> files;
        files = getOutputFiles(outputBase);
        final Path testDir = Paths.get("src/test/resources/extractor/commit_test_all_but_one").toAbsolutePath();
        // Clean up test directory if it exists
        deleteDirectoryRecursively(testDir);

        return files.stream().map(folder -> DynamicTest.dynamicTest("Commit all but " + folder, () -> {
            Files.createDirectories(testDir);

            // Initialize ECCO service
            Path baseDir = testDir.resolve("not_" + folder);
            Files.createDirectories(baseDir);
            try (EccoService service = setupEccoService(baseDir)) {
                // Commit all but one
                for (String f : files) {
                    if (!f.equals(folder)) {
                        service.setBaseDir(outputBase.resolve(f));
                        service.commit("commited" + f);
                    }
                }
                Path checkout = Files.createDirectories(baseDir.resolve("checkout"));
                service.setBaseDir(checkout);
                service.checkout(service.getConfigStringFromFile(outputBase.resolve(folder).resolve(".config")));
                // Verify files
                assertFoldersEqual(outputBase.resolve(folder), checkout);
            }
        }));
    }

    // Commit two variants and checkout a third one, check results
    @TestFactory
    Collection<DynamicTest> commitTwoVariantsCheckoutThird() throws Exception {
        final Path outputBase = Paths.get("src/test/resources/extractor/output");
        List<String>files = getOutputFiles(outputBase);

        final Path testDir = Paths.get("src/test/resources/extractor/commit_two_checkout_third").toAbsolutePath();
        // Clean up test directory if it exists
        deleteDirectoryRecursively(testDir);
        Collection<DynamicTest> tests = new java.util.ArrayList<>();
        int i = 0;
        while (i < files.size()) {
            Path checkout = outputBase.resolve(files.get(i));
            Path commit1 = outputBase.resolve(files.get((i + 1) % files.size()));
            Path commit2 = outputBase.resolve(files.get((i + 2) % files.size()));
            i++;
            DynamicTest dynamicTest = DynamicTest.dynamicTest("Commit " + commit1.getFileName() + " and " + commit2.getFileName() + " checkout " + checkout.getFileName(), () -> {
                Files.createDirectories(testDir);

                // Initialize ECCO service
                Path baseDir = testDir.resolve("commit_" + commit1.getFileName() + "_" + commit2.getFileName() + "_checkout_" + checkout.getFileName());
                Files.createDirectories(baseDir);
                try (EccoService service = setupEccoService(baseDir)) {
                    String configuration = service.getConfigStringFromFile(checkout);
                    // Commit two variants
                    service.setBaseDir(commit1);
                    service.commit("commited" + commit1.getFileName());
                    service.setBaseDir(commit2);
                    service.commit("commited" + commit2.getFileName());
                    Path checkoutDir = Files.createDirectories(baseDir.resolve("checkout"));
                    service.setBaseDir(checkoutDir);
                    service.checkout(configuration);
                    // Verify files
                    assertFoldersEqual(checkout, checkoutDir);
                }
            });
            tests.add(dynamicTest);
        }
        return tests;
    }


}
