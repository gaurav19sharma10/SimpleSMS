# Firebase Web Remote Gateway Setup Guide

With this feature, Simple SMS gives each phone its own secure, unique Web Portal URL. You can open this URL from any computer, tablet, or browser to send SMS messages directly through your phone's local SIM card, even when the app is minimized or closed.

---

## 🚀 Quick 3-Step Setup

### Step 1: Create a Free Firebase Project
1. Go to the [Firebase Console](https://console.firebase.google.com/) and click **"Add project"**.
2. Give it any name (e.g., `MySimpleSmsGateway`) and click **Continue** (Google Analytics can be disabled).
3. Once created, click the **Android icon (➕)** to register your Android app:
   - **Android package name**: `com.simplesms.app`
   - **App nickname**: `Simple SMS`
4. Click **"Register app"**, then download your `google-services.json` file.
5. Copy the downloaded `google-services.json` file into:
   ```
   SimpleSMS/app/google-services.json
   ```

---

### Step 2: Enable Firebase Realtime Database
1. In your Firebase Console left sidebar, click **Build** ➔ **Realtime Database**.
2. Click **"Create Database"**, choose your region, and click Next.
3. Select **"Start in test mode"** (or use the security rules below) and click **Enable**.
4. *(Optional)* Realtime Database Security Rules:
   ```json
   {
     "rules": {
       "devices": {
         "$deviceId": {
           ".read": true,
           ".write": true
         }
       }
     }
   }
   ```

---

### Step 3: Open the Web Portal
1. Open the **"Web Portal"** tab inside the Simple SMS app on your phone.
2. It shows your unique, cryptographically bound URL:
   ```
   https://simplesms-gateway.web.app/?device=device_xxxxxx&key=key_yyyyyy
   ```
3. Open `SimpleSMS/web-dashboard/index.html` in your computer's browser (double click to open directly or host on Firebase Hosting / GitHub Pages).
4. Click **Config** in the top right of the web dashboard and paste your Firebase Database URL (found on your Firebase Realtime Database page).
5. Done! You can now send single or bulk SMS directly through your phone's SIM card from your laptop or PC!
