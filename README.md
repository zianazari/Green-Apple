# Green Apple — سیب سلامت

برنامهٔ آفلاین اندروید برای ثبت تغذیه، آب، نشستن و فعالیت روزانه؛ نوشته‌شده با Kotlin و رابط کاملاً بومی Jetpack Compose.

An offline Android nutrition and daily habits tracker, written in Kotlin with a fully native Jetpack Compose interface.

## Download / دانلود

Get **Green-Apple-2.0.1.apk** from [Releases](https://github.com/zianazari/Green-Apple/releases/latest). Android 11 or newer is required. Install over the existing Green Apple / Sabz app to retain data; do not uninstall first.

نسخهٔ جدید را روی نسخهٔ قبلی نصب کنید. قالب پشتیبان و اطلاعات روزهای قبلی حفظ شده است؛ نیازی به حذف برنامه یا ورود دوبارهٔ اطلاعات نیست.

## Native version 2.0

- All production Android code is Kotlin. Every app screen is Jetpack Compose.
- PDF reports use Android Canvas, text shaping and PdfDocument.
- No WebView, JavaScript interface, HTML, CSS or JavaScript runtime.
- Food illustrations are bundled native VectorDrawables, adapted from Twemoji SVG assets.
- Persian RTL with Vazirmatn; English; Persian and Gregorian calendars.
- Light and charcoal-blue dark themes; visible language, theme and calendar choices.
- Immediate food/drink logging, frequent foods, custom recipes and editable history.
- Finish a day with one tap, even if some or all fields are unrecorded; completed days remain editable and can be reopened.
- Automatic three-hour intervals; empty periods merge forward within the day.
- A newly recorded interval defaults to one hour of sitting, adjustable to actual time or zero.
- Walking, moderate activity, stretching, other activities and separate Kegel session counts.
- Daily nutrition, water and activity summaries; animated green apple and seven-day habit score.
- Native reminders, nightly reviews, PDF export and JSON backup/restore.

## Data compatibility and privacy

The application ID remains `com.sabz.companion`. The published APK uses the original signing key.

The native version reads the existing version-1 JSON from `SharedPreferences("data")["state"]`. It preserves the original record IDs, embedded food data, custom foods, dates and goals. On the first successful native save, it also retains the original state as `preNativeState`. Invalid existing data disables writes and can be exported for recovery; invalid imports do not replace the current state.

Data stays on the device. There is no account, backend, analytics or INTERNET permission. Back up before uninstalling or changing phones. Values are approximate and goals are configurable; this is general habit tracking, not medical diagnosis.

## Build

Requires JDK 17, Android SDK platform 35 and Build Tools 35.0.0. Open in Android Studio, or set `ANDROID_HOME` and run:

```sh
./gradlew :app:assembleDebug
```

The checked-in Gradle 8.9 wrapper pins the official distribution checksum. Kotlin/Compose compiler 2.0.21, Compose BOM 2024.12.01 and AGP 8.7.3 are pinned to a toolchain verified with SDK 35.

Release compilation:

```sh
./gradlew :app:assembleRelease
```

This produces an unsigned APK under `app/build/outputs/apk/release/`. Sign it with your own protected signing key. The original release signing key is deliberately excluded from the repository. Builds signed with another key cannot replace the published installation.

## Verification

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

Tests include numerical parity with 25 legacy data fixtures, legacy backup validation, automatic interval merging, explicit zeros, Kegel units and local-time/DST reminders. Native device tests cover immediate food registration, settings persistence, custom-food drafts, sitting controls, Persian calendar names and native PDF generation.

The in-place upgrade is additionally tested by installing the native build over an older app in an emulator and comparing the stored legacy data.

## Architecture

- `Domain.kt`: deterministic calculations, intervals, validation and score.
- `Store.kt`: atomic persisted state and backwards-compatible migration.
- `AppUi.kt`, `LogScreens.kt`, `HistoryScreens.kt`, `SettingsScreen.kt`: native screens and navigation.
- `CalendarTools.kt`: ICU Persian/Gregorian calendar and localized formatting.
- `NativeReport.kt`: shaped Persian/English native PDF export.
- `ReminderReceiver.kt`, `ReminderSchedule.kt`: alarms and nightly feedback.

## Assets and attribution

Nutrition data: USDA SR Legacy (2018), with source identifiers in the bundled catalogue. Font: Vazirmatn, SIL OFL. Food art: Twemoji © Twitter / jdecked contributors, CC BY 4.0. Original SVGs and license text are retained under `app/src/main/assets/art/`; the displayed artwork is converted to native Android vectors. Green apple launcher artwork is maintained as native vector source.
