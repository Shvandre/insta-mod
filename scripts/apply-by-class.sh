#!/usr/bin/env bash
# Применяет однофайловый smali-патч к файлу, найденному по имени КЛАССА
# (.class-директива), а не по имени файла. Имя файла нестабильно для классов,
# чьи имена различаются только регистром (X/9Dn vs X/9DN, X/Qza*): apktool
# разводит коллизии суффиксами .N.smali, и кто получит «чистое» имя —
# зависит от ФС и запуска.
# usage: apply-by-class.sh <build-root> <class, напр. X/9Dn> <patch>
set -euo pipefail
WORK=$1
CLASS=$2
PATCH=$3

F=$(grep -rl "^\.class.*L${CLASS};" "$WORK"/src/smali*/ 2>/dev/null | head -1)
[ -n "${F:-}" ] || { echo "apply-by-class: класс L${CLASS}; не найден"; exit 1; }
REL=${F#"$WORK"/}
sed "s|a/src/[^ ]*\.smali|a/$REL|g; s|b/src/[^ ]*\.smali|b/$REL|g" "$PATCH" > "$WORK/.abc.patch"
(cd "$WORK" && git apply .abc.patch)
rm -f "$WORK/.abc.patch"
echo "apply-by-class: $(basename "$PATCH") -> $REL"
