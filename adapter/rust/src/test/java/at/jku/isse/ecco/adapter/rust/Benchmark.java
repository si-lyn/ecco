package at.jku.isse.ecco.adapter.rust;

import at.jku.isse.ecco.service.EccoService;
import org.junit.jupiter.api.Assertions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static at.jku.isse.ecco.adapter.rust.Utils.deleteDirectoryRecursively;
import static at.jku.isse.ecco.adapter.rust.Utils.getOutputFiles;

public class Benchmark {
    static long commitAllVariantsAndCheckoutOne() throws Exception {
        final Path outputBase = Paths.get("adapter/rust/src/test/resources/extractor/output");
        final Path testDir = Paths.get("adapter/rust/src/test/resources/extractor/commit_all_checkout_one").toAbsolutePath();
        // Clean up test directory if it exists
        deleteDirectoryRecursively(testDir);
        Files.createDirectories(testDir);
        List<String> files = getOutputFiles(outputBase);
        Path eccoDir = Files.createDirectories(testDir.resolve("ecco_repo"));
        long startTime;
        try (EccoService service = new EccoService(eccoDir)) {
            service.init();
            Assertions.assertNotNull(service);
            startTime = System.currentTimeMillis();
            // Commit all variants
            for (String folder : files) {
                service.setBaseDir(outputBase.resolve(folder));
                service.commit("commited" + folder);
            }
            // Checkout one variant
            Path checkoutLocation = Files.createDirectories(testDir.resolve("checkout_" + files.getFirst()));
            service.setBaseDir(checkoutLocation);
            service.checkout(service.getConfigStringFromFile(outputBase.resolve(files.getFirst())));
            return System.currentTimeMillis() - startTime;
        }
    }

    public static void main(String[] args) throws Exception {
        // do a warmup run to help with JIT optimizations
        commitAllVariantsAndCheckoutOne();
        List<Long> times = new ArrayList<>(5);
        // to test 5 times and take the average
        for (int i = 0; i < 5; i++) {
            times.add(commitAllVariantsAndCheckoutOne());
           System.out.println("Run " + (i + 1) + " took " + times.get(i) + " ms");
        }
        System.out.println("Benchmark Commit All Variants and Checkout One: Average time over " + times.size() + " runs: " +
                times.stream().mapToLong(Long::longValue).average().orElse(0) + " ms");
    }

}
