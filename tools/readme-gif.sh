#!/bin/sh
# Convert the README tour recording (assets/readme/nexora-tour.webm, produced
# by apps/web/scripts/readme-capture.mjs) into a bounded-size, paletted GIF.
# Prerequisite: ffmpeg on PATH. Output: assets/readme/nexora-tour.gif.
set -eu

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
out="$repo_root/assets/readme"

ffmpeg -y -i "$out/nexora-tour.webm" \
  -vf "fps=10,scale=800:-1:flags=lanczos,split[s0][s1];[s0]palettegen=max_colors=96[p];[s1][p]paletteuse=dither=bayer:bayer_scale=3" \
  -loop 0 \
  "$out/nexora-tour.gif"
