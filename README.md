# LinkShort

A simple Android URL shortener built with Kotlin and Jetpack Compose. It uses Bitly to create short links and stores each successful long/short URL pair in a local SQLite database.

## Open in Android Studio

1. Open Android Studio and choose **Open**.
2. Select this project folder.
3. Allow Gradle to sync and install Android SDK 35 if prompted.
4. Configure your Bitly access token as described below.
5. Start an emulator or connect an Android device, then click **Run**.

## Configure Bitly

Create a Bitly access token, then add it to the ignored `local.properties` file in the project root:

```properties
BITLY_ACCESS_TOKEN=your_bitly_access_token
```

Alternatively, set the `BITLY_ACCESS_TOKEN` environment variable or pass it to Gradle as `-PBITLY_ACCESS_TOKEN=...`. The app needs internet access to shorten links. Enter a complete `https://` or `http://` URL, then use **Copy**, **Open**, or **Share** on the result. The saved URL count is stored on the device and remains after restarting the app.

The token is embedded in the Android app when it is built, so this setup is suitable for local development only. For a production release, call Bitly from a backend service rather than shipping an access token in the app.
