#!/usr/bin/env bash
# Переименование пакета com.instagram.android -> com.insta.mod.
# Заменяет собой patch 04: имена файлов вида X/Foo.1.smali, которые apktool
# генерирует при коллизиях на case-insensitive ФС, различаются между macOS и
# Linux, поэтому переименование сделано правилами, а не патчем.
#
# Правила:
#   1. com.instagram.android -> com.insta.mod (в smali, xml, json)
#   2. authorities провайдеров из манифеста: com.instagram.X -> com.insta.mod.X
#      (и в манифесте, и в строковых литералах smali)
# Реальные классы (android:name, targetActivity) не трогаются — в манифесте
# они не матчатся правилом 1, а правило 2 бьёт только по authority-строкам.
set -euo pipefail
SRC=${1:?usage: pkg-rename.sh <decompiled-src-dir>}

find "$SRC" -type f \( -name '*.smali' -o -name '*.xml' -o -name '*.json' \) -print0 \
  | xargs -0 perl -pi -e 's/com\.instagram\.android/com.insta.mod/g'

# 2. Authorities провайдеров: com.instagram.X -> com.insta.mod.X
#    Список — scripts/pkg-rename-auths.txt (суффиксы после com.instagram.).
#    Манифест: только android:authorities (android:name с реальными классами
#    не трогаем). smali/json: префиксная замена (ловит и content://authority,
#    и строки вида "authority$Inner", "authority (file.kt:16)").
ALT=$(perl -pe 's/\./\\./g' "$(dirname "$0")/pkg-rename-auths.txt" | paste -sd'|' -)
perl -pi -e 's/android:authorities="com\.instagram\./android:authorities="com.insta.mod./g' \
  "$SRC/AndroidManifest.xml"
find "$SRC" -type f \( -name '*.smali' -o -name '*.json' \) -print0 \
  | xargs -0 perl -pi -e "s/com\.instagram\.($ALT)/com.insta.mod.\$1/g"
echo "pkg-rename: done"
