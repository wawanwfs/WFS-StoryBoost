#!/usr/bin/env bash
set -euo pipefail
# Qualcomm DnCNN, MIT-licensed original KAIR weights.
# The build downloads and embeds the model so Android processing runs offline.
MODEL_URL="https://qaihub-public-assets.s3.us-west-2.amazonaws.com/qai-hub-models/models/dncnn/releases/v0.65.0/dncnn-tflite-float.zip"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODEL="$ROOT/app/src/main/assets/models/dncnn_25.tflite"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
curl -fL --retry 3 --retry-delay 2 --connect-timeout 20 --max-time 180 "$MODEL_URL" -o "$TMP/model.zip"
unzip -t "$TMP/model.zip" >/dev/null
mapfile -t FOUND < <(unzip -Z1 "$TMP/model.zip" | grep -Ei '\.tflite$' || true)
if [[ ${#FOUND[@]} -ne 1 ]]; then
  printf 'Expected exactly one TFLite model, got %s\n' "${#FOUND[@]}" >&2
  unzip -Z1 "$TMP/model.zip" >&2
  exit 1
fi
mkdir -p "$(dirname "$MODEL")"
unzip -p "$TMP/model.zip" "${FOUND[0]}" > "$MODEL"
SIZE=$(stat -c %s "$MODEL")
if (( SIZE < 500000 || SIZE > 12000000 )); then
  echo "Unexpected DnCNN model size $SIZE" >&2
  exit 1
fi
HASH="$(sha256sum "$MODEL" | cut -d' ' -f1)"
echo "DNCNN_ASSET_SHA256=$HASH"
echo "DNCNN_ASSET_BYTES=$SIZE"
if [[ -n "${DNCNN_TFLITE_SHA256:-}" && "$HASH" != "$DNCNN_TFLITE_SHA256" ]]; then
  echo "Denoiser model SHA256 mismatch" >&2
  exit 1
fi
echo "Model binary: ${FOUND[0]}"
