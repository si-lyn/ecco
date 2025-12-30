#!/usr/bin/env bash

# fail on errors
set -euo pipefail

# make sure we are in the dir of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1
root="$SCRIPT_DIR"

# cd .. until dir = ecco
while [ "$(basename "$root")" != "ecco" ] && [ "$root" != "/" ]; do
  root="$(dirname "$root")"
done
if [ "$PWD" = "/" ]; then
  echo "Error: Could not find ecco root directory."
  exit 1
fi

extractor_dir="$root/adapter/rust/src/test/resources/extractor"

# process all directories starting with "commit_"
for target_dir in "$extractor_dir"/commit_*/; do
  if [ ! -d "$target_dir" ]; then
    continue
  fi

  # Copy serdeTomls contents only into existing checkout/ folders
  for dir in "$target_dir"*/; do
    if [[ "$(basename "$target_dir")" == "commit_all_checkout_all" ]]; then
      checkout_dir="${dir}"
    else
      checkout_dir="${dir}checkout"
    fi
    if [ -d "$checkout_dir" ]; then
      if ! rsync -av --update "$extractor_dir/serdeTomls/" "$checkout_dir" >/dev/null 2>&1; then
        echo "Failed to copy serdeTomls contents into $checkout_dir"
        exit 1
      fi
    else
      echo "Skipping $dir (no checkout subdir)"
    fi
  done
  cargo_checker="$extractor_dir/cargoChecker.sh"
  "$cargo_checker" "$target_dir" "$SCRIPT_DIR/cargo_checker_output.txt"
done

echo "All tests completed. Check $SCRIPT_DIR/cargo_checker_output.txt for details."

