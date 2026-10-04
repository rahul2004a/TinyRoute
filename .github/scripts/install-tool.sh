#!/usr/bin/env bash
# Download pinned official releases and verify their SHA-256 before execution.
set -euo pipefail

tool="${1:?Usage: install-tool.sh gitleaks|trivy|actionlint destination}"
destination="${2:?Missing destination directory}"
system="$(uname -s)"
architecture="$(uname -m)"
case "$system/$architecture" in
  Linux/x86_64) gitleaks_platform=linux_x64; trivy_platform=Linux-64bit; actionlint_platform=linux_amd64 ;;
  Darwin/arm64) gitleaks_platform=darwin_arm64; trivy_platform=macOS-ARM64; actionlint_platform=darwin_arm64 ;;
  *) echo "Unsupported platform: $system/$architecture" >&2; exit 1 ;;
esac
case "$tool" in
  gitleaks)
    version=8.30.1
    repository=gitleaks/gitleaks
    archive="gitleaks_${version}_${gitleaks_platform}.tar.gz"
    checksums="gitleaks_${version}_checksums.txt"
    ;;
  trivy)
    version=0.75.0
    repository=aquasecurity/trivy
    archive="trivy_${version}_${trivy_platform}.tar.gz"
    checksums="trivy_${version}_checksums.txt"
    ;;
  actionlint)
    version=1.7.12
    repository=rhysd/actionlint
    archive="actionlint_${version}_${actionlint_platform}.tar.gz"
    checksums="actionlint_${version}_checksums.txt"
    ;;
  *) echo "Unknown tool: $tool" >&2; exit 1 ;;
esac

scratch="$(mktemp -d)"
trap 'rm -rf "$scratch"' EXIT
base="https://github.com/${repository}/releases/download/v${version}"
curl --fail --silent --show-error --location --retry 3 "$base/$archive" -o "$scratch/$archive"
curl --fail --silent --show-error --location --retry 3 "$base/$checksums" -o "$scratch/$checksums"
expected="$(awk -v archive="$archive" '$2 == archive { print $1 }' "$scratch/$checksums")"
actual="$(shasum -a 256 "$scratch/$archive" | awk '{ print $1 }')"
if [[ ! "$expected" =~ ^[0-9a-f]{64}$ || "$actual" != "$expected" ]]; then
  echo "Checksum verification failed for $archive" >&2
  exit 1
fi
mkdir -p "$destination"
tar -xzf "$scratch/$archive" -C "$scratch" "$tool"
install -m 0755 "$scratch/$tool" "$destination/$tool"
if [[ -n "${GITHUB_PATH:-}" ]]; then
  echo "$destination" >> "$GITHUB_PATH"
fi
"$destination/$tool" --version
