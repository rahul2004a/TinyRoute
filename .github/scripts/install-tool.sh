#!/usr/bin/env bash
# Download official releases and verify repository-pinned SHA-256 before execution.
set -euo pipefail

tool="${1:?Usage: install-tool.sh gitleaks|trivy|actionlint destination}"
destination="${2:?Missing destination directory}"
system="$(uname -s)"
architecture="$(uname -m)"
case "$system/$architecture" in
  Linux/x86_64)
    gitleaks_platform=linux_x64
    gitleaks_sha256=551f6fc83ea457d62a0d98237cbad105af8d557003051f41f3e7ca7b3f2470eb
    trivy_platform=Linux-64bit
    trivy_sha256=c6e65abddb348e25f10549df887045629cf28cc72453cd1c63acb717316b3f3f
    actionlint_platform=linux_amd64
    actionlint_sha256=8aca8db96f1b94770f1b0d72b6dddcb1ebb8123cb3712530b08cc387b349a3d8
    ;;
  Darwin/arm64)
    gitleaks_platform=darwin_arm64
    gitleaks_sha256=b40ab0ae55c505963e365f271a8d3846efbc170aa17f2607f13df610a9aeb6a5
    trivy_platform=macOS-ARM64
    trivy_sha256=4a77108cccf8e55c8d6823e1e759939a622277e66cd0daa3c1fc621ed69e4568
    actionlint_platform=darwin_arm64
    actionlint_sha256=aba9ced2dee8d27fecca3dc7feb1a7f9a52caefa1eb46f3271ea66b6e0e6953f
    ;;
  *) echo "Unsupported platform: $system/$architecture" >&2; exit 1 ;;
esac
case "$tool" in
  gitleaks)
    version=8.30.1
    repository=gitleaks/gitleaks
    archive="gitleaks_${version}_${gitleaks_platform}.tar.gz"
    expected="$gitleaks_sha256"
    ;;
  trivy)
    version=0.75.0
    repository=aquasecurity/trivy
    archive="trivy_${version}_${trivy_platform}.tar.gz"
    expected="$trivy_sha256"
    ;;
  actionlint)
    version=1.7.12
    repository=rhysd/actionlint
    archive="actionlint_${version}_${actionlint_platform}.tar.gz"
    expected="$actionlint_sha256"
    ;;
  *) echo "Unknown tool: $tool" >&2; exit 1 ;;
esac

scratch="$(mktemp -d)"
trap 'rm -rf "$scratch"' EXIT
base="https://github.com/${repository}/releases/download/v${version}"
curl --fail --silent --show-error --location --retry 3 "$base/$archive" -o "$scratch/$archive"
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
