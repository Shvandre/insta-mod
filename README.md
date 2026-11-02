# Insta Mod

Патч-набор для Instagram (Android, arm64), убирающий рекламу и добавляющий приватность-фичи.
Распространяется **только как патчи** — оригинальный код Meta здесь не публикуется,
APK каждый собирает сам из легально полученного оригинала.

## Фичи

- Без рекламы (лента, сториз, reels) — включено по умолчанию
- Название «Insta» вместо «Instagram» (иконка приложения + вордмарк в шапке)
- Без сжатия фото/видео при загрузке (JPEG ~100%, max dimension, видео без даунскейла)
- Пакет `com.insta.mod` — ставится рядом с оригиналом
- Отдельное меню «Insta Mod» (английский интерфейс, тёмная тема) с тумблерами
  (открывается иконкой «Insta Mod» в списке приложений или долгим тапом по вордмарку «Insta» в шапке ленты):
  - Hide story views — не отмечать сториз просмотренными
  - Disable screenshot detection — отключить детект скриншотов (Direct/сториз)
  - Block ads — блокировать рекламу
  - Block analytics — блокировать аналитику
- Автообновление из меню (GitHub Releases) с прогрессом скачивания и отменой
- Автопроверка обновлений при запуске приложения (не чаще раза в 12 часов) — при наличии новой версии показывается диалог

## Сборка

Нужны: JDK 17+, Android SDK (build-tools 36.1.0, platform android-36.1), apktool 3.0.3.

```bash
scripts/build.sh                      # сам скачает базовый APK (Aptoide, arm64)
scripts/build.sh instagram.apk        # или из локального файла
```

Результат: `dist/insta-mod-<версия>-mod.<rev>-signed.apk`.

При первой сборке генерируется `mod.keystore` — **сохрани его**: автообновления
ставятся поверх только при совпадающей подписи.

## Как устроено

- `patches/*.patch` — smali/манифест-патчи, накатываются на `apktool d`-дерево (`git apply`)
- `patches/by-class/*.patch` — патчи к классам, чьи имена файлов нестабильны
  (X/05nH, X/0ZkC, X/0Xqx, X/0CeX — коллизии регистра, apktool разводит их суффиксами
  `.N.smali` по-разному на macOS/Linux); `scripts/apply-by-class.sh` находит файл по
  `.class`-директиве и подставляет его в патч
- `scripts/pkg-rename.sh` — переименование пакета `com.instagram.android` → `com.insta.mod`
  скриптом (та же проблема нестабильных имён у 185 файлов);
  список authorities — `scripts/pkg-rename-auths.txt`
- `scripts/hook-screenshot.sh` — хук обеих реализаций детектора скриншотов
  (ищутся по методу `Abh(JLjava/lang/String;Ljava/util/List;)V`)
- `assets/wordmark/` — PNG вордмарка «Insta» (бинарники в патчах не живут)
- `modsrc/` — исходник меню настроек + автообновлялки, компилируется в отдельный dex
- `version.txt` — базовая версия Instagram, `modrev.txt` — ревизия патчей
- `.github/workflows/build.yml` — CI: ежедневно проверяет новую версию Instagram,
  пересобирает и (при `vars.PUBLISH_APK = true`) публикует релиз

Порядок применения: 01-ads → 05nH-ads-gate → 02-quality (0F6S + 0ZkC + 0Xqx) →
03-rename → 06a-manifest (до pkg-rename — контекст ханка захватывает package=) →
pkg-rename.sh → 05a/b/c-wordmark-force → 06b/c-modsettings →
07a/b-wordmark-longclick → 0CeX-seen-flush → hook-screenshot.sh.

### Якоря для переноса патчей на новую версию Instagram

Обфусцированные имена классов плывут от версии к версии, искать по маркерам:

- ads: строка `"Is ad pod"` в boolean-методе; `TigonServiceLayer.startRequest`
- rename: `>Instagram<` в res/values/strings.xml + drawable `instagram_wordmark_ig_wordmark_*`
- quality: JPEG-рампа (float `0x445c0000`), поля `ShareType`+`UserSession` у гейта качества,
  рампа битрейта (`0x438`/`0x2d0`/`0x7d9`)
- pkg: глобальная замена пакета в манифесте/smali (НЕ трогать `android:name` с реальными классами)
- modsettings: `media/seen/?reel=` (seen-state + флаш); детекторы скриншотов — метод
  `Abh(JLjava/lang/String;Ljava/util/List;)V` (в 435 назывался `AN7`) + строка `ScreenshotDetectorThread`
- wordmark force: `ActionBarTitleViewSwitcher`, `MergedFeedsActionBar`, гейт в MainFeedActionBarHelper
  (строка `"MainFeedActionBarHelper.onCreateView"`)

Соответствия 435 → 455 (для сверки при следующем переносе):

| роль | 435 | 455 |
|---|---|---|
| static Context | `X/2zD.A00` | `X/04g9.A00` (сетится в `InstagramAppShell.attachBaseContext`) |
| ad-pod гейт | `X/1vT.A02(1vT;08X;4od)Z` | `X/05nH.A02(05nH;07es;04uz)Z` |
| качество: гейты | `X/Wd4.A06/A09()Z` | `X/0ZkC.A09/A0C()Z` |
| качество: JPEG/размер | `X/Geh.A00(I)I`, `A03(UserSession;Z)I` | `X/0F6S.A01(I)I`, `A03(UserSession;Z)I` |
| качество: битрейт | `X/MmN.A00(Context;IZZ)I` | `X/0Xqx.A00(Context;IZZZ)I` |
| reel-модель (лок. отметка) | `X/4gv.A0b(UserSession;J)V` | `X/04ht.A0m(UserSession;J)V` |
| накопление seen | `X/9Cp.A09(ReelItem;Gsn)V` | `X/0CdU.A08(ReelItem;02Na)V` |
| флаш seen | `X/9Dn.A00()V` | `X/0CeX.A00()V` |
| детекторы скриншотов | `X/Qza.AN7`, `X/3dU.AN7` | `X/0Cya.Abh`, `X/02uZ.Abh` |
| гейт вордмарка | `X/2mV` (config BHQ) | `X/06dT` (config Bcj, v10) |
| wordmark drawable | `0x7f08285a` | `0x7f082b34` |
| Tigon-запрос | `LX/4qo;` URI `A08`, `failRequest(...4 пары)` | `LX/02dI;` URI `A08`, `failRequest(...+I)` (int=0) |

## Юридическая заметка

Instagram — проприетарный продукт Meta. Модификация и распространение
модифицированного APK нарушают условия использования Meta. Этот репозиторий
содержит только оригинальные патчи автора (лицензия MIT на них) и не содержит
кода Meta. Используй на свой страх и риск, только для личного использования.
