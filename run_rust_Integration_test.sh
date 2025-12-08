#!/usr/bin/env bash
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

./gradlew -q :ecco-adapter-rust:test --tests "at.jku.isse.ecco.adapter.rust.RustIntegrationTest"

echo "Rust Integration Tests completed."
echo "Report can be found at file://$root/adapter/rust/build/reports/test/index.html"

