#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROPS="$ROOT/config/airship.local.properties"
EXAMPLE="$ROOT/config/airship.local.properties.example"
OUT="$ROOT/apple/CTVLab/AirshipSecrets.swift"

if [[ ! -f "$PROPS" ]]; then
  PROPS="$EXAMPLE"
fi

app_key="$(awk -F= '/^airship.appKey=/{print $2}' "$PROPS" | tr -d '[:space:]')"
app_secret="$(awk -F= '/^airship.appSecret=/{print $2}' "$PROPS" | tr -d '[:space:]')"
site="$(awk -F= '/^airship.site=/{print $2}' "$PROPS" | tr -d '[:space:]')"
site="${site:-eu}"

mkdir -p "$(dirname "$OUT")"
cat > "$OUT" <<EOF
// Generated from config/airship.local.properties — do not edit.
enum AirshipSecrets {
    static let appKey = "${app_key}"
    static let appSecret = "${app_secret}"
    static let site = "${site}"
    static var isConfigured: Bool {
        !appKey.isEmpty && appKey != "YOUR_APP_KEY"
    }
}
EOF
