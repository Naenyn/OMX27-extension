# OMX-27 Bitwig Extension

Bitwig controller extension for Okyeron's OMX-27.

## What it does

- Passes OMX note data into Bitwig (instrument playing / MIDI recording).
- Maps transport controls (stop/play/record) and mirrors state back to OMX LEDs.
- Supports metronome toggle, count-in control, and visual metronome flash.
- Supports separate visible-metronome mode (LED flash can be independent from Bitwig metronome audio).
- Maps knob CCs to Bitwig remotes plus volume, pan, and sends.

## Default CC mappings

- **Transport**: Stop `102`, Play `103`, Record `104`
- **Metronome**: Toggle `105`, Count-in `106`, Flash output `107`, Visible toggle `108`
- **Remotes**: `110-117` (Remote 1-8)
- **Mix**: Volume `7`, Pan `101`, Send 1 `126`, Send 2 `127`, Send 3 `100`

All mappings are configurable in Bitwig controller settings.

## Build (macOS)

These instructions are intended for building on macOS.

## Build environment requirements

- macOS with `zsh`
- Bitwig Studio installed (default app path: `/Applications/Bitwig Studio.app`)
- Java toolchain available in shell (`javac`/`jar`)
- `jenv` configured in your shell startup (`.zshrc`) if you use version-managed Java

## Build steps

This project uses a simple `javac` build script and compiles with `--release 8` (Java 8 bytecode target).

By default it expects Bitwig's API JAR at:

`/Applications/Bitwig Studio.app/Contents/Java/bitwig.jar`

If your Bitwig app is somewhere else, override the path:

```zsh
BITWIG_APP_PATH="/path/to/Bitwig Studio.app" ./build.zsh
```

Or point directly at the JAR:

```zsh
BITWIG_JAR="/path/to/bitwig.jar" ./build.zsh
```

Run from a login zsh shell so `jenv`/`.zshrc` Java selection is applied:

```zsh
zsh -lic 'cd "/path/to/OMX27-extension" && ./build.zsh'
```

Build output:

`build/OMX-27.bwextension`

## Install in Bitwig

Copy `build/OMX-27.bwextension` to your Bitwig user extensions folder:

`~/Documents/Bitwig Studio/Extensions`

Then restart Bitwig or reload controller scripts.

## Notes

- This README currently documents macOS paths only.
- Windows/Linux users can still build, but will need to adapt Bitwig and user-library paths.
