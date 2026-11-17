#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p out
# Selain lib/, gunakan lokasi Connector/J yang sudah dipilih di IntelliJ pada laptop ini.
AV_DRIVER_CP="lib/*"
AV_DRIVER_FOUND=false
for AV_JAR in lib/*.jar; do
    if [ -f "$AV_JAR" ]; then AV_DRIVER_FOUND=true; fi
done
for AV_JAR in "$HOME"/Downloads/mysql-connector-j-*/*.jar; do
    if [ -f "$AV_JAR" ]; then AV_DRIVER_CP="$AV_DRIVER_CP:$AV_JAR"; AV_DRIVER_FOUND=true; fi
done
if [ "$AV_DRIVER_FOUND" = false ]; then
    echo "Driver MySQL belum ditemukan. Letakkan MySQL Connector/J .jar di folder lib/." >&2
    exit 1
fi
javac -encoding UTF-8 -d out src/model/*.java src/security/*.java src/database/*.java src/service/*.java src/web/*.java src/view/*.java
exec java -cp "out:$AV_DRIVER_CP" web.WebServer
