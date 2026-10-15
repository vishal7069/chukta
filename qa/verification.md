# Verification

Android APK compiled successfully with JDK 17 / SDK 36. Android lint completed with zero errors; warnings concern dependency updates and generated template resources.

Backend: 5 tests passed. Shared Kotlin: 3 tests passed.

The backend and shared tests verify exact paise allocation, weighted shares, kitty accounting and the Goa Figma example. API tests cover one-time OTP, rejected unauthenticated/non-member access, owner-approved join requests, expense edits, idempotent expenses/payments, partial payment validation and development OTP restrictions in production.

## Android device evidence

Tested on a local Android emulator, with synthetic development accounts and local API data:

1. Entered mobile number using the custom keypad, requested a development OTP and verified it.
2. Saved a name; reached the first-use Home screen.
3. Created a Goa trip and added a named friend.
4. Entered a ₹1,240 expense paid by the friend; each share was ₹620.
5. Opened the Pay sheet, recorded a synthetic ₹620 cash payment and saw Hisaab chukta.
6. Opened Summary and verified the same trip total and category values.
7. Stopped the local API, added a ₹900 expense and saw it saved to the offline outbox.
8. Restarted the emulator and API. The queued expense survived the restart and synced; SQLite contained exactly two distinct expenses, with no duplicate.

Screenshots are in design/android-login.png, design/android-home.png, design/android-paid.png and design/android-offline.png. The UI uses native status bars, safe areas and actual inputs, so platform chrome and displayed data differ from the static Figma examples.

## Release setup still required

- Public HTTPS API host with a persistent database volume.
- SMS provider secrets; current local OTP is a development code.
- Apple signing and physical iPhone/TestFlight verification. The shared framework and SwiftUI host compiled successfully in GitHub Actions on macOS / Xcode 16.4 for the ARM64 iPhone simulator (run 38729692208).
- App-store policy links, bank/provider verification if required, and external notification/reminder providers.

Real UPI transfers and real SMS were not executed in the test. Settlement records are explicitly user_recorded.
