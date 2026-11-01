# Green Apple — سیب سلامت

برنامهٔ آفلاین اندروید برای ثبت تغذیه، آب، نشستن و فعالیت روزانه. نام برنامه روی گوشی «سیب سلامت» است.

An offline Android nutrition and daily habits tracker with Persian and English interfaces.

## Download / دانلود

Download **Green-Apple-1.4.0.apk** from [Releases](https://github.com/zianazari/Green-Apple/releases/latest). Android 11 or newer is required. Install over your existing Green Apple / Sabz installation to retain data; do not uninstall first.

نسخهٔ نصب‌شدنی در بخش Releases قرار دارد. برای حفظ اطلاعات، نسخهٔ جدید را روی نسخهٔ قبلی نصب کنید.

## Features

- Persian RTL interface with bundled Vazirmatn font; English interface.
- Persian and Gregorian calendars; editable daily history.
- Immediate food and drink logging, personal frequent foods, custom foods.
- Automatic three-hour intervals; empty periods merge forward within the same day.
- Each newly recorded interval defaults to one hour of sitting; users can adjust or explicitly set zero. Empty intervals do not add sitting time.
- Daily calories, protein, fiber, plain water and activity summaries.
- Walking, moderate exercise, stretching, other activities and a separate Kegel session counter.
- Light and charcoal-blue dark themes; visible language, theme and calendar choice buttons.
- Green apple companion and seven-day habit score.
- Native reminders, nightly reviews, PDF printing and JSON backup/restore.

## Privacy and data

Data stays on the device. There is no account, server, analytics or INTERNET permission. Export a JSON backup before uninstalling or changing devices. The app is for general habit tracking, not medical diagnosis. Nutrition values are approximate; goals are configurable.

The application ID remains `com.sabz.companion` to preserve upgrades from previous releases.

## Build

Requires JDK 17, Android SDK platform 35 and Build Tools 35.0.0.

Open the project in Android Studio, or use Gradle 8.9 with Android Gradle Plugin 8.7.3:

```sh
gradle :app:assembleDebug
```

A Gradle wrapper is not bundled. Alternatively, with `ANDROID_HOME` and `JAVA_HOME` set:

```sh
./build-sdk.sh
```

The SDK-only script writes `build-local/Green-Apple-1.4.0.apk` and generates a local development signing key. That key is different from the key used for the published APK, so locally signed builds cannot replace the published installation. The original release signing key is deliberately excluded from this repository. Preserve it separately for future release updates.

## Tests

```sh
node tests/core.cjs
node tests/core-v2.cjs
node tests/automatic.cjs
```

Pure Java checks:

```sh
mkdir -p build-local/tests
javac -d build-local/tests app/src/main/java/com/sabz/companion/NightReview.java app/src/main/java/com/sabz/companion/ReminderSchedule.java tests/NightReviewTest.java tests/ReminderScheduleTest.java
java -cp build-local/tests com.sabz.companion.NightReviewTest
java -cp build-local/tests com.sabz.companion.ReminderScheduleTest
```

Version 1.4.0's settings controls, persistence, RTL/LTR layout and theme colors were also checked in a local browser. This release has not been tested on a physical phone.

## Implementation and assets

Java Android shell with an offline WebView interface; minSdk 30, targetSdk 35. Native notifications require the relevant Android permissions and can be delayed by system power settings.

Nutrition data uses USDA SR Legacy (2018), with source information embedded in the food catalogue. Icon attribution: Twemoji © Twitter / jdecked contributors, CC BY 4.0. Vazirmatn is distributed under SIL OFL. Bundled attribution and font license files are under `app/src/main/assets/`.
