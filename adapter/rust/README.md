# ECCO Rust Adapter

Artifact adapter plugin for ECCO that provides reader and writer for Rust source files.

## Overview

The Rust adapter enables ECCO to process Rust source code files (.rs) by parsing them into a ECCO treee. It supports reading Rust files with feature annotations and writing them back with the appropriate conditional compilation attributes.

## Componentts

### Reader (RustReader)
located at `adapter/rust/src/main/java/at/jku/isse/ecco/adapter/rust/RustReader.java`
Reads .rs files from the filesystem. Then parses Rust source code into AST using ANTLR4 with the Rust grammar located at `adapter/rust/src/main/antlr4/`.
`RustEccoVisitor` then visits the AST and constructs a generic ECCO tree structure representing the Rust code using various `ArtifactData` classes located in `adapter/rust/src/main/java/at/jku/isse/ecco/adapter/rust/data/`.


### Writer (RustWriter)
located at `adapter/rust/src/main/java/at/jku/isse/ecco/adapter/rust/RustWriter.java`
Reconstructs Rust source files from ECCO's node structure
Writes files to the filesystem with proper formatting


## Data Types

All the different Artifacts used in the ECCO tree is in `adapter/rust/src/main/java/at/jku/isse/ecco/adapter/rust/data`. 
Some of these artifact classes implments the `adapter/rust/src/main/java/at/jku/isse/ecco/adapter/rust/data/RustWritable.java` interface to support writing the artifacts back to Rust source files. This interface is implemented by the following artifact classes:

- `FunctionArtifactData` - Function signatures
- `ModuleArtifactData` - Module declarations
- `VisibilityArtifactData` - Visibility modifiers (pub, pub(crate), etc.)
- `LineArtifactData` - Individual lines of code

The other artifact classes are used to represent specific Rust constructs where the content is `LineArtifactData`.
### Testing

#### RustIntegrationTest

The adapter includes comprehensive tests in `RustIntegrationTest` covering:

- All Rust item types (functions, structs, enums, traits, etc.)
- Feature-based variants
- Application scenarios with multiple feature combinations
- Merge operations

This test is located at `adapter/rust/src/test/java/at/jku/isse/ecco/adapter/rust/RustIntegrationTest.java` and can be run through an IDE or Gradle.

To run the tests via Gradle, use:
#### ExtractorTest

This tests commit varying amounts of variants of serde.rs from in the folder `adapter/rust/src/test/resources/extractor/output`. Beware this test takes a long time to run. It also requires that you have cargo installed and available in your system path.

To run this test, run the script `run_rust_extractor_tests.sh` in the folder `adapter/rust/src/test/resources/extractor/`