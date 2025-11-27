package at.jku.isse.ecco.adapter.rust;

import at.jku.isse.ecco.service.EccoService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static at.jku.isse.ecco.adapter.rust.Utils.assertFilesEqual;
import static at.jku.isse.ecco.adapter.rust.Utils.deleteDirectoryRecursively;

class RustIntegrationTest {
    final Path testDir = Paths.get("src/test/resources/rust_examples/test_output").toAbsolutePath();
    EccoService service;

    @BeforeEach
    void setUpEcco() {
        // Clean up test directory if it exists
        if (Files.exists(testDir)) {
            try {
                deleteDirectoryRecursively(testDir);
            } catch (IOException e) {
                System.err.println("Failed to clean up test directory: " + e.getMessage());
            }
        }
        // Initialize ECCO service
        try {
            Files.createDirectories(testDir);
            service = new EccoService(testDir);
            service.init();
        } catch (Exception e) {
            System.out.println("Exception during ECCO setup: " + e.getMessage());
        }
    }

    @Test
    void testAllItems() {
        Path testFolder = Paths.get("src/test/resources/rust_examples/allItemsTest/");
        commitSingleDir(testFolder, service);
        service.checkout("all_items.1");
        Path actual = Paths.get("src/test/resources/rust_examples/allItemsTest/main.rs");
        Path testOutput = Paths.get("src/test/resources/rust_examples/test_output/main.rs");
        assertFilesEqual(actual, testOutput);
    }

    // Disabled since comments are not supported by antlr parser
    @Disabled
    @Test
    void comments() {
            Path testFolder = Paths.get("src/test/resources/rust_examples/commentTest/");
            commitSingleDir(testFolder, service);
        try {
            service.checkout("comments.1");
        } catch (Exception e) {
            System.out.println("Exception during checkout: " + e.getMessage());
        }
        Path actual = Paths.get("src/test/resources/rust_examples/commentTest/main.rs");
        Path testOutput = Paths.get("src/test/resources/rust_examples/test_output/main.rs");
        assertFilesEqual(actual, testOutput);
    }

    @Test
    void functionWithOuterAttribute() throws Exception {
        String[] folders = { "v1", "v2"};
        Path base = Paths.get("src/test/resources/rust_examples/functionTest/");
        commit(folders, base.toString(), service);

        Path actualFile = base.resolve("actual/main.rs");
        deleteDirectoryRecursively(base.resolve("actual"));
        Files.createDirectories(base.resolve("actual"));

        service.setBaseDir(base.resolve("actual").toAbsolutePath());
        service.checkout("hello.1,farewell.1");

        Path expectedFile = base.resolve("expected/main.rs");
        assertFilesEqual(expectedFile, actualFile);
    }

    @Test
    void struct() {
        Path testFolder = Paths.get("src/test/resources/rust_examples/structTest/");
        commitSingleDir(testFolder, service);
        service.checkout("struct.1");
        Path actual = Paths.get("src/test/resources/rust_examples/structTest/main.rs");
        Path testOutput = Paths.get("src/test/resources/rust_examples/test_output/main.rs");
        assertFilesEqual(actual, testOutput);
    }

    @Test
    void MergeEnumTest() {
        String[] folders = { "v1", "v2"};
        String testFolderStr = "src/test/resources/rust_examples/MergeEnumTest/";
        commit(folders, testFolderStr, service);
        service.checkout("create.1,get.1,getAll.1,change.1");
        Path actual = Paths.get("src/test/resources/rust_examples/MergeEnumTest/result/main.rs");
        Path testOutput = Paths.get("src/test/resources/rust_examples/test_output/main.rs");
        assertFilesEqual(actual, testOutput);
    }


    //  @TODO create v1, v3 results for application variants
    private static Stream<Arguments> provideApplicationVariants() {
        return Stream.of(
                Arguments.of(new String[] { "v1" }, "base.1,create.1,get.1"),
                Arguments.of(new String[] { "v2" }, "base.1,create.1,getAll.1"),
                Arguments.of(new String[] { "v3" }, "base.1,create.1,get.1,updatePassword.1"),
                Arguments.of(new String[] { "v1", "v2" }, "base.1,create.1,get.1,getAll.1"),
                Arguments.of(new String[] { "v2", "v3" }, "create.1,getAll.1,updatePassword.1,base.1"),
                Arguments.of(new String[] { "v1", "v3" }, "base.1,create.1,get.1,updatePassword.1"),
                Arguments.of(new String[] { "v1", "v2", "v3" }, "create.1,get.1,getAll.1,change.1,base.1")
        );
    }

    /** Test various combinations of committing and checking out application variants
     * @param folders Folders to commit
     * @param checkoutConfig Configuration string for checkout
     * @throws IOException
     */
    @ParameterizedTest
    @MethodSource("provideApplicationVariants")
    @DisplayName("Application Variants Commit and Checkout Test")
    void applicationVariants(String[] folders,  String checkoutConfig) throws IOException {
        String testFolderStr = "src/test/resources/rust_examples/application/";
        commit(folders, testFolderStr, service);
        String combinedFolders = String.join(",", folders);
        Path actualLocation = Paths.get("src/test/resources/rust_examples/application/actual_checkout/"+combinedFolders);
        deleteDirectoryRecursively(actualLocation);
        Files.createDirectories(actualLocation);

        service.setBaseDir(actualLocation.toAbsolutePath());
        service.checkout(checkoutConfig);
        Path expected;
        if (folders.length == 1) {
            expected = Paths.get("src/test/resources/rust_examples/application/" + folders[0] + "/main.rs");
        } else {
            String resultFolderName = "result" + Arrays.stream(folders).map(String::toUpperCase).collect(Collectors.joining(""));
            expected = Paths.get("src/test/resources/rust_examples/application/results/" + resultFolderName + "/main.rs");
        }

        assertFilesEqual(actualLocation.resolve("main.rs"), expected);
    }


    //commit v1,v2,v3 and checkout v1,v2,v3 individually
    @TestFactory
    Stream<DynamicTest> commitAllVariantsOfApplicationAndCheckoutOneByOne() throws Exception {
        String[] folders = { "v1", "v2", "v3"};
        Path testFolder = Path.of("src/test/resources/rust_examples/application/");
        Path actualLocation = testFolder.resolve("actual_checkout");
        deleteDirectoryRecursively(actualLocation);
        Files.createDirectories(actualLocation);
        commit(folders, testFolder.toString(), service);
        List<String> configs = new ArrayList<>();
        for (String folder : folders) {
            List<String> lines = Files.readAllLines(testFolder.resolve(folder).resolve(".config"));
            String combinedLines = String.join("", lines);
            configs.add(combinedLines);
        }

        return Arrays.stream(folders).map(folder -> DynamicTest.dynamicTest("Checkout " + folder, () -> {
            String checkoutConfig = switch (folder) {
                case "v1" -> configs.get(0);
                case "v2" -> configs.get(1);
                case "v3" -> configs.get(2);
                default -> throw new IllegalStateException("Unexpected value: " + folder);
            };
            Path dir = actualLocation.resolve(folder);
            Files.createDirectories(dir);
            service.setBaseDir(actualLocation.resolve(folder).toAbsolutePath());
            service.checkout(checkoutConfig);
            Path actual = actualLocation.resolve(folder).resolve("main.rs");
            Path expected = testFolder.resolve(folder).resolve("main.rs");
            assertFilesEqual(actual, expected);
        }));

    }

    // Test for commiting a windows logger and a unix logger, and then extract the shared logic
    @Test
    void loggerTest() throws  Exception {
        Path testLocation = Paths.get("src/test/resources/rust_examples/trait-imp-test/").toAbsolutePath();
        String[] folders = { "windows", "unix"};
        String testFolderStr = "src/test/resources/rust_examples/trait-imp-test/";
        commit(folders, testFolderStr, service);

        Path actualDir = testLocation.resolve("actual");
        deleteDirectoryRecursively(actualDir);
        Files.createDirectories(actualDir);

        service.setBaseDir(testLocation.resolve("actual"));
        Files.deleteIfExists(actualDir.resolve("main.rs"));
        service.checkout("unix_logger.1, windows_logger.1");
        Path actual = actualDir.resolve("main.rs");
        Path testOutput = testLocation.resolve("expected/main.rs");
        assertFilesEqual(actual, testOutput);
    }


    private void commit(String[] folders, String testFolderStr, EccoService service) {
        Path testFolder = Paths.get(testFolderStr);
        for (String folder : folders) {
            commitSingleDir(testFolder.resolve(folder), service);
        }
    }

    private void commitSingleDir(Path dir, EccoService service) {
        service.setBaseDir(dir);
        service.commit();
        service.setBaseDir(this.testDir);
    }


}
