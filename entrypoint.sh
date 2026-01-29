#!/usr/bin/env bash

set -e pipefail

java -jar backend.jar --provision

java -jar backend.jar