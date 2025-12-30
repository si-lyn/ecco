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

# Check if path arguments were provided
if [ $# -lt 2 ]; then
    echo "Usage: $0 <path> <output_file>"
    exit 1
fi

base_path="${1%/}"
output_file="$2"

# Arrays to track successes and failures
successful_tests=()
failed_tests=()

# Loop over all directories in the current folder
for dir in "$base_path"/*/; do
    if [ ! -d "$dir" ]; then
        continue
    fi
    # Remove trailing slash from dir for cleaner path construction
    dir="${dir%/}"

    if [[ "$(basename "$base_path")" == "commit_all_checkout_all" ]]; then
        checkout_dir="$dir"
    else
        checkout_dir="${dir}/checkout"
    fi
    if [ -d "$checkout_dir" ]; then
        echo ">>> Entering $checkout_dir"
        if cargo test --manifest-path "$checkout_dir/Cargo.toml"; then
            echo "cargo test succeeded for $checkout_dir"
            successful_tests+=("$checkout_dir")
            cargo clean --manifest-path "$checkout_dir/Cargo.toml"
            echo "<<< Finished $checkout_dir"
        else
            echo "cargo check failed for $checkout_dir" >&2
            failed_tests+=("$checkout_dir")
            cargo clean --manifest-path "$checkout_dir/Cargo.toml"
        fi
    else
        echo "Skipping $dir (no checkout subdir)"
    fi
done

# Output summary
{
    echo ""
    echo "================================"
    echo "Test Summary for $base_path"
    echo "================================"
    echo "Successful tests (${#successful_tests[@]}):"
    for test in "${successful_tests[@]}"; do
        echo "  ✓ $test"
    done

    echo ""
    echo "Failed tests (${#failed_tests[@]}):"
    for test in "${failed_tests[@]}"; do
        echo "  x $test"
    done
} | tee -a "$output_file"