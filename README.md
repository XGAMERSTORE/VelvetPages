# BookBond

Book-themed Android dating/community app with real server-side users, likes, mutual matches, private messages and a shared reading feed.

## What is real
- Email/password accounts are stored on PocketBase.
- Profiles, likes, posts and messages are server records, not hard-coded demo content.
- Mutual likes are detected client-side and unlock the chat screen.
- Chat polls the server every 3 seconds, so two phones see the same conversation.
- The server URL is editable inside the APK, so switching from a test server to a public Czech VPS does not require rebuilding the app.

## Backend on a Czech VPS
1. Create an Ubuntu/Debian VPS with Docker.
2. Point a domain/subdomain such as api.example.cz to the VPS public IP.
3. Copy the backend/ directory to the VPS.
4. In backend/, create .env from .env.example and set DOMAIN=api.example.cz.
5. Run docker compose up -d --build.
6. Open https://api.example.cz/_/ to create the first PocketBase superuser.
7. In the Android app set https://api.example.cz as the BookBond server.

For LAN testing without a domain, use http://SERVER_LAN_IP:8090 in the app.

## Android
Android Gradle Plugin 8.7.3, compile SDK 35, Java 17, min SDK 26.

Build: `gradle :app:assembleDebug`
APK: `app/build/outputs/apk/debug/app-debug.apk`
