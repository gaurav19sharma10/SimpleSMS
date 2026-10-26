# Simple SMS — Native Android Telephony Application

**Simple SMS** is a modern, high-performance native Android application built with **Jetpack Compose**, **Material 3**, and the Android Telephony stack (`android-sim-sms` skill). It enables users to select one or multiple contacts from their phonebook and sequentially dispatch SMS notifications directly through the device's physical SIM card baseband radio.

---

## 📱 Features

1. **One-Time Default SIM Setup (Multi-SIM Resolution)**:
   - On the very first launch, the app inspects active SIM cards via `SubscriptionManager`.
   - If 2 SIM cards are detected, it prompts the user to choose their preferred default SIM (showing carrier name and slot index).
   - Once chosen, the preference is persisted in `SharedPreferences`. The user is **never prompted to choose a SIM again** when sending messages.
   - The default SIM can be changed at any time in **Settings** or by tapping the SIM badge in the Top App Bar.
   - If only 1 SIM card is present, it is configured automatically without interrupting the user.

2. **In-App Contact Picker & Multi-Selection**:
   - Queries `ContactsContract` with name & phone search.
   - Quick "Select All" and "Deselect All" actions.
   - Multi-select checkboxes for custom contact groups.

3. **Sequential SMS Dispatch with Live Green Tick (✔)**:
   - Sends messages **one-by-one** using carrier-specific `SmsManager` resolved by `subscriptionId`.
   - Multi-part message division (`smsManager.divideMessage`) handles GSM-7 (160 char) and Unicode (70 char) carrier limits.
   - Uses dynamic `BroadcastReceiver` tracking baseband `Activity.RESULT_OK` callback with `FLAG_IMMUTABLE` and `RECEIVER_NOT_EXPORTED`.
   - When each message is confirmed sent by the baseband radio, the contact row dynamically displays a **prominent vibrant Green Tick (✔)**.

4. **Background Foreground Service**:
   - Dispatches via `SmsDispatchService` so mass sending is not terminated if the user switches apps or locks the screen.
   - Shows live progress in notification ("Sending 3 of 10 SMS...").
   - Built-in pacing throttle (~1.5s delay between dispatches) to protect against carrier spam rate-limiting.

5. **Message Composer & Personalization**:
   - Live character and multipart SMS count indicator (`45 / 160 chars (1 SMS)`).
   - `{name}` template tag dynamically replaced with each recipient's name.

---

## 🚀 Project Structure

```
SimpleSMS/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/simplesms/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── SimpleSmsApp.kt
│   │   │   ├── data/
│   │   │   │   ├── model/
│   │   │   │   │   ├── ContactItem.kt       # Contact entity with live SendStatus
│   │   │   │   │   ├── SimCardInfo.kt       # SIM slot & subscription entity
│   │   │   │   │   └── SmsResult.kt         # Sealed telephony result types
│   │   │   │   └── repository/
│   │   │   │       ├── ContactRepository.kt # Fast phonebook resolver
│   │   │   │       ├── PreferenceRepository.kt # Default SIM & throttle persistence
│   │   │   │       └── SimManagerHelper.kt  # SubscriptionManager & permission helper
│   │   │   ├── service/
│   │   │   │   ├── SmsDispatcher.kt         # Baseband multipart sender & BroadcastReceiver
│   │   │   │   └── SmsDispatchService.kt    # Foreground service with progress notification
│   │   │   └── ui/
│   │   │       ├── screens/
│   │   │       │   ├── MainScreen.kt        # Contact list, composer, live ticks
│   │   │       │   ├── SettingsDialog.kt    # SIM switcher & delay throttle
│   │   │       │   └── SimSelectionDialog.kt# First-launch dual-SIM selector
│   │   │       ├── theme/                   # Material 3 colors, typography, theme
│   │   │       └── viewmodel/
│   │   │           └── MainViewModel.kt     # Reactive UI state management
│   │   └── res/                             # Vector icons, colors, themes, strings
│   └── build.gradle.kts
├── install_and_grant.ps1                    # One-click ADB install script
├── build.gradle.kts
├── settings.gradle.kts
└── local.properties
```

---

## 🛠️ How to Build and Install

### 1. Build Debug APK:
```powershell
cd C:\Users\dell\.gemini\antigravity\scratch\SimpleSMS
.\gradlew assembleDebug
```
Output APK is located at:
`app\build\outputs\apk\debug\app-debug.apk`

### 2. Install to Connected Device (Recommended):
Connect your Android phone via USB with USB Debugging enabled, then run:
```powershell
.\install_and_grant.ps1
```
*(The script uses `adb install -r -g` to bypass Google Play Protect sideloading blocks and automatically grants `SEND_SMS`, `READ_PHONE_STATE`, and `READ_CONTACTS` at installation time).*
