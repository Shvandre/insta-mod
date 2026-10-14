#!/usr/bin/env bash
# Хук тумблера no_screenshot_detect во вторую реализацию детектора скриншотов
# (класс LX/Qza;, метод AN7). Имя файла (Qza.smali / Qza.N.smali) зависит от
# ФС, поэтому файл ищется по директиве .class, а не по имени.
set -euo pipefail
SRC=${1:?usage: hook-qza.sh <decompiled-src-dir>}

F=$(grep -rl '^\.class.*LX/Qza;$' "$SRC"/smali*/X/ | head -1)
[ -n "${F:-}" ] || { echo "hook-qza: класс LX/Qza; не найден"; exit 1; }

export BLOCK_FILE=$(mktemp)
cat > "$BLOCK_FILE" <<'EOF'

    sget-object v0, LX/2zD;->A00:Landroid/content/Context;

    if-nez v0, :instamod_ss_pref_off

    const-string v1, "instamod"

    const/4 v2, 0x0

    invoke-virtual {v0, v1, v2}, Landroid/content/Context;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;

    move-result-object v0

    const-string v1, "no_screenshot_detect"

    invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result v0

    if-eqz v0, :instamod_ss_pref_off

    return-void

    :instamod_ss_pref_off
EOF

perl -0777 -pi -e '
  BEGIN { local $/; open my $bh, "<", $ENV{BLOCK_FILE} or die $!; $b = <$bh>; }
  s/(\.method public final AN7\(JLjava\/lang\/String;Ljava\/util\/List;\)V\n    \.locals \d+\n)\n/$1$b/
' "$F"
rm -f "$BLOCK_FILE"
grep -q 'instamod_ss_pref_off' "$F" || { echo "hook-qza: вставка не удалась в $F"; exit 1; }
echo "hook-qza: patched $F"
