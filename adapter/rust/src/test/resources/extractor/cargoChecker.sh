#!/usr/bin/env bash
# Used in run_rust_extractor_tests.sh

# check if cargo is installed
if ! command -v cargo &> /dev/null; then
    echo "cargo could not be found, please install Rust and Cargo."
    exit 1
fi

# Check if a path argument was provided
if [ $# -lt 1 ]; then
    echo "Usage: $0 <path>"
    exit 1
fi

base_path="$1"

# Loop over all directories in the current folder
for dir in "$base_path"/*/; do
    checkout_dir="$dir/checkout"
    if [ -d "$checkout_dir" ]; then
        echo ">>> Entering $checkout_dir"
        if cargo check --manifest-path "$checkout_dir/Cargo.toml"; then
            echo "cargo check succeeded for $checkout_dir"
            cargo clean --manifest-path "$checkout_dir/Cargo.toml"
            echo "<<< Finished $checkout_dir"
        else
            echo "cargo check failed for $checkout_dir" >&2
        fi
    else
        echo "Skipping $dir (no checkout subdir)"
    fi
done