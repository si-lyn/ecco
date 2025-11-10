package at.jku.isse.ecco.adapter.rust.antlr;

import at.jku.isse.ecco.service.EccoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static at.jku.isse.ecco.adapter.rust.antlr.Utils.assertFilesEqual;
import static at.jku.isse.ecco.adapter.rust.antlr.Utils.deleteDirectoryRecursively;

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

    @AfterEach
    void cleanUp() {
        // Clean up test directory if it exists
        if (Files.exists(testDir)) {
            try {
                deleteDirectoryRecursively(testDir);
            } catch (IOException e) {
                System.err.println("Failed to clean up test directory: " + e.getMessage());
            }
        }
        service.close();
        service = null;
    }

    // Commented out since comments are not supported in parser
//    @Test
//    void comments() throws Exception {
//            Path testFolder = Paths.get("src/test/resources/rust_examples/commentTest/");
//            commitSingleDir(testFolder, service);
//        try {
//            service.checkout("comments.1");
//        } catch (Exception e) {
//            System.out.println("Exception during checkout: " + e.getMessage());
//        }
//        Path actual = Paths.get("src/test/resources/rust_examples/commentTest/main.rs");
//        Path testOutput = Paths.get("src/test/resources/rust_examples/test_output/main.rs");
//        assertFilesEqual(actual, testOutput);
//    }

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
    void struct() throws Exception {
        Path testFolder = Paths.get("src/test/resources/rust_examples/structTest/");
        commitSingleDir(testFolder, service);
        service.checkout("struct.1");
        Path actual = Paths.get("src/test/resources/rust_examples/structTest/main.rs");
        Path testOutput = Paths.get("src/test/resources/rust_examples/test_output/main.rs");
        assertFilesEqual(actual, testOutput);
    }

    @Test
    void MergeEnumTest() throws Exception {
        String[] folders = { "v1", "v2"};
        String testFolderStr = "src/test/resources/rust_examples/MergeEnumTest/";
        commit(folders, testFolderStr, service);
        service.checkout("create.1,get.1,getAll.1,change.1");
        Path actual = Paths.get("src/test/resources/rust_examples/MergeEnumTest/result/main.rs");
        Path testOutput = Paths.get("src/test/resources/rust_examples/test_output/main.rs");
        assertFilesEqual(actual, testOutput);
    }

    @Test
    void applicationV1() throws  Exception {
        String[] folders = { "v1"};
        String testFolderStr = "src/test/resources/rust_examples/application/";
        commit(folders, testFolderStr, service);

        service.checkout("base.1,create.1,get.1");
        Path actual = Paths.get("src/test/resources/rust_examples/application/v1/main.rs");
        Path testOutput = Paths.get("src/test/resources/rust_examples/test_output/main.rs");
        assertFilesEqual(actual, testOutput);
    }

    @Test
    void applicationV2() throws  Exception {
        String[] folders = {"v2"};
        String testFolderStr = "src/test/resources/rust_examples/application/";
        commit(folders, testFolderStr, service);

        service.checkout("base.1,create.1,getAll.1");
        Path actual = Paths.get("src/test/resources/rust_examples/application/v2/main.rs");
        Path testOutput = Paths.get("src/test/resources/rust_examples/test_output/main.rs");
        assertFilesEqual(actual, testOutput);
    }

    @Test
    void applicationV1V2() throws  Exception {
        String[] folders = { "v1", "v2"};
        String testFolderStr = "src/test/resources/rust_examples/application/";
        commit(folders, testFolderStr, service);

        service.checkout("base.1,create.1,get.1,getAll.1");
        Path actual = Paths.get("src/test/resources/rust_examples/application/results/resultV1V2/main.rs");
        Path testOutput = Paths.get("src/test/resources/rust_examples/test_output/main.rs");
        assertFilesEqual(actual, testOutput);
    }

    @Test
    void applicationV1V2V3() throws  Exception {
        String[] folders = { "v1", "v2", "v3"};
        String testFolderStr = "src/test/resources/rust_examples/application/";
        commit(folders, testFolderStr, service);

        service.checkout("create.1,get.1,getAll.1,change.1,base.1");
        Path actual = Paths.get("src/test/resources/rust_examples/application/results/resultV1V2V3/main.rs");
        Path testOutput = Paths.get("src/test/resources/rust_examples/test_output/main.rs");
        assertFilesEqual(actual, testOutput);
    }

    // Test for commiting a windows logger and a unix logger, and then extract the shared logic
    @Test
    void loggerTest() throws  Exception {
        Path testLocation = Paths.get("src/test/resources/rust_examples/trait-imp-test/");
        String[] folders = { "windows", "unix"};
        String testFolderStr = "src/test/resources/rust_examples/trait-imp-test/";
        commit(folders, testFolderStr, service);
        service.setBaseDir(testLocation.resolve("actual"));
        Files.deleteIfExists(testLocation.resolve("actual/main.rs"));
        service.checkout("base.1, unix_logger.1, windows_logger.1");
        Path actual = testLocation.resolve("actual/main.rs");
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
