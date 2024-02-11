#!/usr/bin/env bash

set -euo pipefail

here="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
dockcross_image="${4?no arch given}"
"$here/../compile-native.sh" "$@" && podman image rm "$dockcross_image"
