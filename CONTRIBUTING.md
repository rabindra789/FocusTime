# Contributing to FocusTime

FocusTime is a small, offline Android app: a Pomodoro timer with a landscape watch face. Bug
reports and focused pull requests are welcome. Please keep the app account-free, network-free and
dependency-light.

## Before you start

- **JDK 21** and the **Android SDK (API 36)**. Gradle is bundled through the wrapper; you do not
  need a separate install.
- A device or emulator running Android 8.0 (API 26) or newer for the Compose tests.
- Open an issue first for anything larger than a bug fix or a small UI change, so the approach is
  agreed before you spend time on it.

## Build and test

```bash
./gradlew assembleDebug               # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest           # JVM tests, no device needed
./gradlew connectedDebugAndroidTest   # Compose/UI tests, needs a connected device
```

On Windows use `gradlew.bat`. For every push to `main` and every pull request, CI runs
`testDebugUnitTest`, `assembleDebug`, `lintDebug`, and the Compose suite on an API 36 emulator.

## Making a change

1. Branch from `main` and keep the change focused.
2. Match the existing Kotlin/Compose style: four-space indents, quiet composables, pure logic
   extracted into testable functions, and no new dependency unless it is clearly justified.
3. Add or update tests with the change:
   - pure timer, policy, formatting and growth logic in `app/src/test`;
   - Compose behaviour in `app/src/androidTest`.
4. Run the commands above. If you changed a landscape face, attach a screenshot from a real
   device to the pull request and update the README screenshots when the look changes.
5. Commit with a short, specific subject and a body that explains *why*. Sign off your commits:

   ```bash
   git commit -s -m "Describe the change"
   ```

   The sign-off certifies that you wrote the change or have the right to submit it under the MIT
   License.

## Pull requests

- Describe the problem, the fix and anything you verified (tests, device, screenshots).
- Link the issue it closes, if there is one.
- Keep unrelated refactors out of the same pull request.
- By contributing you agree that your work is released under the project's
  [MIT License](LICENSE).

## Reporting bugs

Include the app version (Settings → About), Android version and phone model, and the steps that
reproduce the problem. Timer bugs are much easier to fix with the session duration, whether the
screen was off, and whether the app had been force-stopped.

## Supporting the project

If FocusTime is useful to you, you can sponsor its development through the **Sponsor** button on
the repository or at [github.com/sponsors/rabindra789](https://github.com/sponsors/rabindra789).
Sponsoring is optional and does not change the licence or the roadmap.
