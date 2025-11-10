#!/usr/bin/env bash

./gradlew :ecco-adapter-rust:test --tests "at.jku.isse.ecco.adapter.rust.antlr.ExtractorTest.commitThreeVariantsCheckoutFourth"

cd adapter/rust/src/test/resources/extractor || exit 1

# Copy cargo.tomls to checkout folders
ls commit_three_checkout_fourth | xargs -I {} rsync -av --update adapter/rust/src/test/resources/extractor/serdeTomls/ '{}/checkout/'

./cargoChecker.sh "$(pwd)"