#!/bin/bash
set -e
# shellcheck source=set-env.sh
source "$(dirname "$0")"/set-env.sh

scripts/private/build-vpx-impl.sh || (echo "vpx build failed" && exit 1)