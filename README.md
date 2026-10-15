# Chukta

Native Android and iPhone expense-sharing app built from the **Chukta • App flow** and **Chukta • Case study** pages in the supplied Figma file.

Android and iOS share the Kotlin Multiplatform / Compose UI, navigation, data models and ledger. Android uses a Kotlin activity; iOS embeds the shared UI in a SwiftUI host. The backend is Node 24 and persistent SQLite.

## Run the API

Install Node 24+, then:

```sh
cd server
cp .env.example .env
npm start
```

The development server runs at port 8787. DEV_AUTH=true returns a random test OTP to the app; it **does not send an SMS**. Production refuses development OTP. Set DEV_AUTH=false, NODE_ENV=production and the three Twilio Verify secrets on the host to send real SMS. Do not commit .env.

## Android

Install JDK 17 and Android SDK 36. Open the root project in Android Studio, or:

```sh
./gradlew :androidApp:assembleDebug
```

Windows: `gradlew.bat :androidApp:assembleDebug`.
APK: `androidApp/app/build/outputs/apk/debug/androidApp-debug.apk`.

The default API URL is `http://10.0.2.2:8787`, for an Android emulator. On a physical phone, tap **API setup** on login and enter your deployed HTTPS URL or your computer's LAN URL, e.g. `http://192.168.1.10:8787` while on the same Wi-Fi. Development HTTP is permitted only in the debug manifest. The computer's firewall must allow the test service. Release builds require HTTPS:

```sh
./gradlew :androidApp:assembleRelease -PapiUrl=https://api.your-domain.com
```

The release APK is unsigned until you configure your own signing key.

## iPhone

Use macOS with Xcode and JDK 17. Install XcodeGen, then:

```sh
cd iosApp
xcodegen generate
open Chukta.xcodeproj
```

Select your Apple development team and set CHUKTA_API_URL to your HTTPS API URL. The Xcode prebuild script compiles the shared Kotlin framework. A simulator build needs no App Store signing; TestFlight and device distribution require your Apple Developer account. iOS source is prepared but **has not been compiled on this Windows machine**. The GitHub workflow includes a Mac simulator build so platform errors will be visible before distribution.

## Implemented flows

- Stamp splash, phone login, six-digit automatic submit, verified hand-off and name/photo setup.
- Home totals, trip cards, empty state, profile and UPI ID.
- Trip creation, emoji, optional dates, contact picker, manual members, invite sharing and guest preview.
- Owner approval of invited friends claiming named trip members.
- Expense entry, categories, payer, dates, equal/weighted shares, excluded members, compressed receipt images.
- Expense detail, edit, delete and session-level undo before settlements.
- Offline expense outbox, cached trips and manual sync with stable idempotency keys.
- Kitty contributions and kitty-funded expenses using a virtual ledger member.
- Net balance explanation, debt simplification, partial UPI/cash settlement, QR and copy actions.
- Real UPI deep link hand-off. Confirmation requires a UPI reference or cash acknowledgement; records are marked user_recorded, **not bank verified**.
- Paid receipt, category summary, close-trip validation, text sharing and backup export.
- Native contacts/photo/share/clipboard integration and encrypted Android storage / iOS Keychain sessions.

## Deployment

GitHub stores source and APK build artifacts. It does not host this persistent API. Deploy `server/Dockerfile` to a container host with HTTPS, a persistent volume mounted at /data, and the SMS environment secrets. SQLite is intended for one service instance. Back up the data volume. To scale to multiple API instances, migrate the database to PostgreSQL.

Do not expose DEV_AUTH to the internet. Add your published legal policies and support details before a public launch. App-sent scheduled WhatsApp reminders, SMS reading/autofill, push notifications, app-store releases and bank-side payment verification require separate provider/platform configuration; current reminders open the user's native share sheet.

## Verification

```sh
cd server && npm test
cd ..
./gradlew :shared:testDebugUnitTest :androidApp:assembleDebug
```

The backend tests exercise OTP reuse, access control, approved membership, duplicate requests, partial payments, weighted splits, kitty accounting, edit validation, and production OTP restrictions. Kotlin tests check the shared money calculations against the Figma Goa example. See `qa/verification.md` for device evidence and remaining release setup.

Design colors: Haldi #F7B500, Kaajal #1C1A17, Kagaz #FBF7F0; Bricolage Grotesque and Plus Jakarta Sans. Original Figma SVG assets are local. Screens adapt to system safe areas and actual data.
