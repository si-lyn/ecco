#!/usr/bin/env bash
set -euo pipefail

echo "This test can take minutes, do you wish to continue (y/n)"
read -r response
if [[ "$response" != "y" && "$response" != "Y" ]]; then
    echo "Aborting test."
    exit 0
fi

# start from current dir
root="$PWD"

# go up until we find the ecco root or reach /
while [ "$(basename "$root")" != "ecco" ] && [ "$root" != "/" ]; do
  root="$(dirname "$root")"
done
if [ "$PWD" = "/" ]; then
  echo "Error: Could not find ecco root directory."
  exit 1
fi
cd "$root" || exit 1

./gradlew :ecco-adapter-rust:test --tests "at.jku.isse.ecco.adapter.rust.ExtractorTest"

echo "Do you wish to also check with Cargo Checker? (y/n)"
read -r answer
if [[ "$answer" == "y" || "$answer" == "Y" ]]; then
    ./adapter/rust/src/test/resources/extractor/run_rust_extractor_tests.sh
    echo "Cargo Checker tests completed. Check ./adapter/rust/src/test/resources/extractor/cargo_checker_output.txt for details."
else
    echo "Skipping Cargo Checker."
fi

