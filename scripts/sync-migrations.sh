#!/usr/bin/env bash
# Node repo'sundaki Drizzle migration'larını test fixture'ı olarak kopyalar.
#
# NEDEN: Şemanın tek kaynağı Node tarafındaki Drizzle migration'larıdır — Java bu şemaya
# dokunmaz, yalnızca eşler. Entity mapping testi gerçek şemaya karşı koşsun diye
# migration'ların bir kopyası test resource'larında tutulur.
#
# NE ZAMAN: Node tarafında yeni bir migration eklendiğinde bu script çalıştırılmalı.
# Çalıştırılmazsa entity testi ESKİ şemaya karşı geçmeye devam eder ve şema kayması
# fark edilmez — bilinçli kabul edilmiş bir risktir.

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC="${1:-$HERE/../../backend/src/db/postgres/migrations}"
DST="$HERE/../appinsight-admin/appinsight-admin-service/src/test/resources/db/migrations"

if [ ! -d "$SRC" ]; then
  echo "✗ Kaynak bulunamadı: $SRC"
  echo "  Kullanım: $0 [node-migrations-dizini]"
  exit 1
fi

mkdir -p "$DST"
rm -f "$DST"/*.sql
cp "$SRC"/*.sql "$DST"/

echo "✓ $(ls -1 "$DST"/*.sql | wc -l | tr -d ' ') migration kopyalandı → $DST"
