# Public API and real SMS setup

The code and installable development build are published on GitHub. No hosting account, paid service or real SMS has been activated yet.

## Proposed costs (checked 15 October 2026)

Render's 0.5 CPU / 512 MB web service is $7/month; 1 GB persistent disk adds $0.25/month. Estimated hosting base: **$7.25/month**, before taxes, bandwidth overages or paid workspace upgrades. Use the free Hobby workspace. Confirm the actual checkout quote before paying.

Twilio Verify charges **$0.05 per successful verification plus destination SMS delivery charges**. Its India SMS page lists $0.0832 per outbound segment, before additional carrier fees. These published rates suggest about $0.1332 for one successful verification with one SMS; Verify's account-specific India rate must be confirmed in Console. Retries can increase delivery costs. Do not use the US SMS price for Indian numbers. Start with a small prepaid balance and spending alerts; do not enable automatic recharge without an agreed budget.

Sources: [Render compute comparison](https://render.com/articles/render-vs-railway), [Render pricing](https://render.com/pricing), [Twilio Verify pricing](https://www.twilio.com/en-us/verify/pricing), [India SMS pricing](https://www.twilio.com/en-us/sms/pricing/in).

## Account owner steps

1. Create your [Twilio account](https://www.twilio.com/try-twilio), verify your email and phone, and complete its requested business/account information. A trial is for testing approved recipient numbers; it is not unrestricted public OTP service.
2. In Twilio Console, create a Verify service named Chukta with SMS enabled and a six-digit code. Enable fraud protection and allow the intended destination countries. Review the India routing/onboarding requirements shown for your account. Keep the Auth Token private.
3. Create your [Render account](https://dashboard.render.com/) and review the proposed monthly budget. The repository is public; select only this repository if granting a GitHub integration access.
4. Open [Deploy Chukta](https://render.com/deploy?repo=https://github.com/vishal7069/chukta). Review `render.yaml`: Singapore, one Node 24 web instance, paid 512 MB compute, 1 GB disk. Enter the three Twilio secrets directly into Render's environment fields. Do not put them in chat, source files, GitHub issues or screenshots.
5. Set WEB_ORIGIN to your final HTTPS API origin. If its generated hostname is not known during setup, replace the temporary value with the service's actual `https://…onrender.com` origin after creation.
6. Review the checkout total before creating the paid service. Once deployed, `/health` must return `{"status":"ok","mode":"production"}`. Development OTP must remain disabled.

The Blueprint uses Render's native Node runtime and mounts SQLite at `/var/data`; the alternative Docker deployment uses `/data`. Keep one API instance. Render's free web service cannot attach the disk this app requires. The Blueprint runs API tests before starting and deploys later commits only after GitHub checks pass.

## Connect and verify the app

In the debug APK, tap **API setup** on login and save the final HTTPS origin. For a release build use `-PapiUrl=https://YOUR-SERVICE.onrender.com`; set CHUKTA_API_URL in the iPhone target to the same origin. Android release signing and Apple device/TestFlight signing still need the owner's credentials.

With the owner's test number, request one real OTP and confirm delivery, login and profile creation. Create a synthetic trip/expense, restart the API service, and confirm the trip still exists. Test an invite between two approved accounts, offline expense replay and a manually recorded settlement. Do not move real money just to test the app. UPI records remain user-confirmed rather than bank-verified.

Before opening public registration, publish the owner's privacy policy/support contact, confirm provider onboarding, set usage alerts, and keep database-consistent backups. Use SQLite's backup API or `VACUUM INTO` rather than copying a live WAL database file. Store backups outside the same volume and check restoration. [Render disk guidance](https://render.com/docs/disks).

An iPhone simulator ZIP is for an Apple Silicon Mac simulator; it cannot be installed on a physical iPhone. TestFlight requires the owner's Apple Developer account.
