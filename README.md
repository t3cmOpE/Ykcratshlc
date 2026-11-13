# NotifLog
Backup pribadi notifikasi (terutama WhatsApp) + media, semua lokal.

## Build via GitHub Actions
1. Buat repo GitHub, upload seluruh isi folder ini (termasuk `.github/`), push ke branch `main`.
2. Tab **Actions** → workflow "Build APK Debug" → setelah hijau, download artifact `NotifLog-debug-apk`.
3. Install APK, buka NotifLog, ikuti halaman Izin.

## Build lokal
Android Studio (JDK 17) → Open → Run. Atau: `gradle assembleDebug`.
