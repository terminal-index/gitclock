# Contributing to GitClock

Thank you for your interest in improving `GitClock`! 🤎

Given the nature of this project (Always-on display, background services, device longevity), we prioritize **battery efficiency** and **screen safety**.

## ⚠️ Safety First

*   **Test on your own hardware first.** Do not submit anything that you haven't verified.
*   **Watch the Battery.** Crucial step, do not kill the battery life because of a tight loop.
*   **Be explicit.** If a change involves a risk (e.g. keeping wake locks), document it clearly in the comments and the PR description.

## Reporting Bugs

I appreciate bug reports! However, please understand that Android devices vary wildly. What works on a Pixel might not work on a Samsung.

### What to Include in a Bug Report
1.  **Device Specifications**: Model, Screen Resolution, Android Version.
2.  **App Version**: The version code or commit hash you are running.
3.  **Logs**: `adb logcat` (relevant parts or something that caught your eye).
4.  **Screenshots**: If it's a UI issue, show us.

## 🛠️ Development Guidelines

### Kotlin & Compose
This project relies mainly on Kotlin and Jetpack Compose. Please adhere to the following:

*   **Indentation**: Use **tabs**. Please do not use spaces.
*   **Architecture**: MVVM. Keep logic out of the UI.
*   **Permissions**: If a feature requires new permissions, handle the "User Denied" state gracefully.
*   **Error Handling**: Check for network availability before making calls.
    ```kotlin
    if (isConnected) {
        fetchData()
    } else {
        showOfflineState()
    }
    ```
*   **Comments**: Comment liberally. Drawing logic on the Canvas can be obscure; explain *why* you are doing something, not just *what*.

### Tools
*   **Lint**: I recommend running lint on your changes to catch common mistakes.
    ```bash
    ./gradlew lintDebug
    ```

## 📥 Pull Request Process

1.  **Fork** the repository.
2.  **Create a branch** for your feature or fix: `git checkout -b fix/overlapping-clock`.
3.  **Test** your changes thoroughly.
4.  **Commit** with clear messages.
5.  **Push** and open a **Pull Request**.

### PR Description
In your PR, describing:
*   **The Problem**: What are you fixing or adding?
*   **The Solution**: How did you implement it?
*   **Verification**: **Crucial.** Describe the hardware/emulator you tested this on and the results.

## 📄 License

It's MIT! You can do whatever you want with it, just don't blame me if something breaks. I am not responsible for any damage you may cause to your device. You are on your own. Sorry :C
