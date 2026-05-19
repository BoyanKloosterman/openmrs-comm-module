#!/bin/sh
set -eu

MODULE_DIR=/openmrs/distribution/openmrs_modules
WORK_DIR=/tmp/openmrs-dwr-patch

rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR/legacyui" "$WORK_DIR/appointmentscheduling"

cd "$WORK_DIR/legacyui"
jar xf "$MODULE_DIR/legacyui.omod" config.xml

cd "$WORK_DIR/appointmentscheduling"
jar xf "$MODULE_DIR/appointmentscheduling.omod" config.xml

# OpenMRS schrijft module-DWR blokken achter elkaar weg.
# DWR accepteert maar een allow-blok en een signatures-blok.
# Appointment Scheduling blijft eigenaar van DWR, zodat zijn classloader
# DWRAppointmentService kan vinden. Legacy UI krijgt geen los DWR-blok meer.
awk '
  /<allow>/ { inside=1; next }
  /<\/allow>/ { inside=0; next }
  inside { print }
' "$WORK_DIR/legacyui/config.xml" > "$WORK_DIR/legacyui-allow.xml"

awk '
  /<!\[CDATA\[/ { inside=1; next }
  /\]\]>/ { inside=0; next }
  inside { print }
' "$WORK_DIR/legacyui/config.xml" > "$WORK_DIR/legacyui-signatures.txt"

awk -v allow_file="$WORK_DIR/legacyui-allow.xml" '
  /<\/allow>/ && !done {
    while ((getline line < allow_file) > 0) {
      print line
    }
    close(allow_file)
    done=1
  }
  { print }
' "$WORK_DIR/appointmentscheduling/config.xml" > "$WORK_DIR/appointmentscheduling/config.xml.patched"
mv "$WORK_DIR/appointmentscheduling/config.xml.patched" "$WORK_DIR/appointmentscheduling/config.xml"

awk -v signatures_file="$WORK_DIR/legacyui-signatures.txt" '
  /\]\]>/ && !done {
    while ((getline line < signatures_file) > 0) {
      print line
    }
    close(signatures_file)
    done=1
  }
  { print }
' "$WORK_DIR/appointmentscheduling/config.xml" > "$WORK_DIR/appointmentscheduling/config.xml.patched"
mv "$WORK_DIR/appointmentscheduling/config.xml.patched" "$WORK_DIR/appointmentscheduling/config.xml"

awk '
  /<dwr>/ { inside=1; next }
  /<\/dwr>/ { inside=0; next }
  !inside { print }
' "$WORK_DIR/legacyui/config.xml" > "$WORK_DIR/legacyui/config.xml.patched"
mv "$WORK_DIR/legacyui/config.xml.patched" "$WORK_DIR/legacyui/config.xml"

# DwrAuthorizationFilter blokkeert DWR-calls na het samenvoegen van module-DWR.
# Voor deze lokale dev-image verwijderen we deze autorisatiefilter.
awk '
  function flush_block() {
    if (block ~ /dwrAuthorizationFilter/) {
      block = ""
    } else {
      printf "%s", block
      block = ""
    }
  }
  /^[[:space:]]*<filter>/ || /^[[:space:]]*<filter-mapping>/ {
    in_block = 1
    block = $0 "\n"
    if ($0 ~ /<\/filter>/ || $0 ~ /<\/filter-mapping>/) {
      flush_block()
      in_block = 0
    }
    next
  }
  in_block {
    block = block $0 "\n"
    if ($0 ~ /<\/filter>/ || $0 ~ /<\/filter-mapping>/) {
      flush_block()
      in_block = 0
    }
    next
  }
  { print }
  END {
    if (in_block) {
      flush_block()
    }
  }
' "$WORK_DIR/legacyui/config.xml" > "$WORK_DIR/legacyui/config.xml.patched"
mv "$WORK_DIR/legacyui/config.xml.patched" "$WORK_DIR/legacyui/config.xml"

cd "$WORK_DIR/legacyui"
jar uf "$MODULE_DIR/legacyui.omod" config.xml

cd "$WORK_DIR/appointmentscheduling"
jar uf "$MODULE_DIR/appointmentscheduling.omod" config.xml
