#!/usr/bin/env bash

interface_name="${1:-vcan0}"

sudo ip link add dev "$interface_name" type vcan
sudo ip link set up "$interface_name"
