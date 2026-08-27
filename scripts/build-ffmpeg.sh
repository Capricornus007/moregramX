#!/bin/bash
set -e
# shellcheck source=set-env.sh
source "$(dirname "$0")"/set-env.sh

scripts/private/build-ffmpeg-impl.sh || (echo "ffmpeg build failed" && exit 1)