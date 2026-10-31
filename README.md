# LUNA KIC – Interactive Launcher

An Android HOME launcher prototype with an Ice Blue visual direction.

## Current prototype
- Android launcher entry point (HOME intent)
- Installed-app dashboard (up to 48 visible entries)
- Swipe up to reveal the dashboard; swipe down to close
- Finger-position-responsive abstract ice glow
- GitHub Actions workflow to build a debug APK

## Build APK
1. Open the repository's **Actions** tab.
2. Select **Build Luna KIC APK**.
3. Press **Run workflow** on the `main` branch.
4. Wait for the run to finish successfully.
5. Open the run and download the `Luna-KIC-debug-apk` artifact.
6. Extract the ZIP and install `app-debug.apk` on the phone. Android may ask you to allow installation from that browser/file manager.

## Important status
This is an early functional shell, not the finished Luna KIC experience. It currently uses an abstract ice-blue background; the supplied Luna KIC character artwork, layered/touch-reactive character animation, realistic crystal/glass tear transition, and refined app drawer are still pending. Do not treat this build as the final design.

## Technical
- Java / Android native custom View
- minSdk 23, targetSdk 28
- compileSdk 35, Android Gradle Plugin 8.7.3
- GitHub Actions builds with JDK 17 and Gradle 8.9
