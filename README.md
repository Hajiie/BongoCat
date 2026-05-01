# Bongo Cat for IntelliJ

This is an IntelliJ plugin that displays a cute Bongo Cat animation in a tool window. The cat reacts as you type, with optional bongo or keyboard sound effects.

## Features

*   **Bongo Cat Animation:** A Bongo Cat animation that reacts to your typing.
*   **Dynamic Resizing:** The Bongo Cat animation automatically resizes to fit the tool window.
*   **Idle State:** The cat returns to a resting state when you stop typing.
*   **Sound Modes:** Choose between `Off`, `Bongo`, and `Keyboard` sound modes.
*   **Persistent Settings:** The selected sound mode is saved and restored when the IDE restarts.
*   **IME-Friendly Playback:** Sound playback is coalesced for Korean IME input to avoid duplicate sound effects.

## How to Use

1.  Open the Bongo Cat tool window by navigating to `View -> Tool Windows -> Bongo Cat`.
2.  Select a sound mode from the top-right control: `Off`, `Bongo`, or `Keyboard`.
3.  Start typing in any editor, and you will see the Bongo Cat react to your input.

## Build

Run the Gradle build from the project root:

```bash
./gradlew build
```

## Project Structure

The project is a standard IntelliJ Platform Plugin with the following structure:

```
.
├── .idea
├── build
├── gradle
├── src
│   ├── main
│   │   ├── kotlin
│   │   │   └── com
│   │   │       └── example
│   │   │           └── bongocat
│   │   │               └── BongoCatToolWindow.kt  // Main plugin logic
│   │   └── resources
│   │       ├── BongoCat_img
│   │       │   ├── bongo_left.png
│   │       │   ├── bongo_middle.png
│   │       │   └── bongo_right.png
│   │       ├── BongoCat_sound
│   │       │   ├── BongoSound
│   │       │   │   ├── BongoSoundL.wav
│   │       │   │   └── BongoSoundR.wav
│   │       │   └── KeyboardSound
│   │       │       ├── Keyboard_1.wav
│   │       │       ├── Keyboard_*.wav
│   │       │       ├── Keyboard_BackSpace.wav
│   │       │       ├── Keyboard_Enter.wav
│   │       │       └── Keyboard_Space.wav
│   │       └── META-INF
│   │           └── plugin.xml
├── .gitignore
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── settings.gradle.kts
```

*   **`BongoCatToolWindow.kt`**: This file contains the Tool Window factory and content logic. The content class manages images, timers, resize handling, editor document listeners, and sound mode controls.
*   **`BongoCatSettings.kt`**: This file stores persistent user settings, including the selected sound mode.
*   **`BongoCatSoundPlayer.kt`**: This file loads WAV files into reusable `Clip` pools for low-latency sound playback.
*   **`resources/BongoCat_img`**: This directory contains the images used for the Bongo Cat animation.
*   **`resources/BongoCat_sound`**: This directory contains the bongo and keyboard WAV files used by the sound modes.
*   **`resources/META-INF/plugin.xml`**: This is the plugin descriptor file, which defines the plugin's name, description, and other metadata.
