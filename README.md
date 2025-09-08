# Bongo Cat for IntelliJ

This is an IntelliJ plugin that displays a cute Bongo Cat animation in a tool window. The cat plays the bongos as you type, adding a bit of fun to your coding sessions.

## Features

*   **Bongo Cat Animation:** A Bongo Cat animation that reacts to your typing.
*   **Dynamic Resizing:** The Bongo Cat animation automatically resizes to fit the tool window.
*   **Idle State:** The cat returns to a resting state when you stop typing.

## How to Use

1.  Open the Bongo Cat tool window by navigating to `View -> Tool Windows -> Bongo Cat`.
2.  Start typing in any editor, and you will see the Bongo Cat playing the bongos.

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
│   │       └── META-INF
│   │           └── plugin.xml
├── .gitignore
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── settings.gradle.kts
```

*   **`BongoCatToolWindow.kt`**: This file contains the core logic for the plugin, including the creation of the tool window and the handling of user input to drive the animation.
*   **`resources/BongoCat_img`**: This directory contains the images used for the Bongo Cat animation.
*   **`resources/META-INF/plugin.xml`**: This is the plugin descriptor file, which defines the plugin's name, description, and other metadata.
