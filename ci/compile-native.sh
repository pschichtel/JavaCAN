#!/usr/bin/env bash

set -euo pipefail

dockcross_image="${4?no arch given}"
./compile-native.sh "$@" && podman image rm "$dockcross_image"
