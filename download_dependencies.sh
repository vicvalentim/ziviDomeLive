#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
libs_dir="$repo_root/src/main/libs"
deps_temp_dir="$(mktemp -d "${TMPDIR:-/tmp}/zividomelive-dependencies.XXXXXX")"

cleanup() {
  if [[ -n "${deps_temp_dir:-}" && -d "$deps_temp_dir" \
      && "$deps_temp_dir" == *"/zividomelive-dependencies."* ]]; then
    rm -rf -- "$deps_temp_dir"
  fi
}
trap cleanup EXIT

sha256_file() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  else
    echo "No SHA-256 tool found (expected sha256sum or shasum)." >&2
    return 1
  fi
}

download_and_extract() {
  local name="$1"
  local url="$2"
  local archive_sha256="$3"
  local jar_path="$4"
  local jar_sha256="$5"
  local use_github_api="${6:-false}"
  local archive="$deps_temp_dir/$name.zip"
  local extract_dir="$deps_temp_dir/$name"
  local destination="$libs_dir/$(basename -- "$jar_path")"
  local actual_sha256

  echo "Downloading pinned $name dependency..."
  if [[ "$use_github_api" == "true" ]]; then
    curl --fail --location --silent --show-error --retry 3 --retry-all-errors \
      -H "Accept: application/octet-stream" "$url" -o "$archive"
  else
    curl --fail --location --silent --show-error --retry 3 --retry-all-errors \
      "$url" -o "$archive"
  fi

  actual_sha256="$(sha256_file "$archive")"
  if [[ "$actual_sha256" != "$archive_sha256" ]]; then
    echo "$name archive checksum mismatch: expected $archive_sha256, got $actual_sha256" >&2
    return 1
  fi

  mkdir -p "$extract_dir"
  unzip -q "$archive" "$jar_path" -d "$extract_dir"
  install -m 0644 "$extract_dir/$jar_path" "$destination"

  actual_sha256="$(sha256_file "$destination")"
  if [[ "$actual_sha256" != "$jar_sha256" ]]; then
    echo "$name JAR checksum mismatch: expected $jar_sha256, got $actual_sha256" >&2
    return 1
  fi

  echo "Installed $(basename -- "$destination") ($actual_sha256)."
}

mkdir -p "$libs_dir"

# Syphon for Processing 4.0, immutable GitHub release asset 59352362.
download_and_extract \
  "Syphon-4.0" \
  "https://api.github.com/repos/Syphon/Processing/releases/assets/59352362" \
  "0842c04d2332e3bfc0b601ae6dafb467b9ba8157934d584df378789750648798" \
  "Syphon/library/Syphon.jar" \
  "546af773807bb0329bc53cc9a9df44a9ed521eb839045fd2077a58625f4150c6" \
  "true"

# ControlP5 2.2.6, versioned release asset.
download_and_extract \
  "controlP5-2.2.6" \
  "https://github.com/sojamo/controlp5/releases/download/v2.2.6/controlP5-2.2.6.zip" \
  "88bd4bdbb4f3d5cb77211004ac4796742eeae86d0a74726f3194cc316ad23fb3" \
  "controlP5/library/controlP5.jar" \
  "69e160f9cee979d631a4a9674f3d3b2016b66eb3e6f353918cda2b325ef3cc75"

# Spout for Processing 2.0.8.0, immutable GitHub release asset 188539046.
download_and_extract \
  "Spout-2.0.8.0" \
  "https://api.github.com/repos/leadedge/SpoutProcessing/releases/assets/188539046" \
  "65fff0a2779833073dbcd54fbbe862f43b2505cb04dd2a4fcb52b0d80c95b4ca" \
  "spout/library/spout.jar" \
  "74c6a8422590dc00ffc7a207c3c408f4b09f6da9390aae7718452095ea974cef" \
  "true"

echo "Pinned Processing dependencies installed successfully."
