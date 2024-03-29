#!/usr/bin/env bash

set -euo pipefail

sudo modprobe vcan can_raw can_bcm can_j1939

interface_name="${1:-vcan0}"

sudo ip link add dev "$interface_name" type vcan
sudo ip link set up "$interface_name"

if ! sudo modprobe can_isotp
then
    echo "Failed to load the ISOTP module, related unit tests will probably fail"
fi
