# OMX-27 Bitwig Extension

Bitwig controller extension for Okyeron's OMX-27.
This is an unofficial community extension (separate from the official OMX-27 firmware project).

## About OMX-27

- Product page (kit): [OMX-27 v3 - MIDI Keyboard Kit](https://denki-oto.com/products/omx-27-v3-midi-keyboard-kit)
- Official OMX-27 firmware/code repository: [okyeron/OMX-27](https://github.com/okyeron/OMX-27)

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

## Default knob bank behavior

- **Bank 1**: Knobs 1-4 -> selected track Remotes 1-4, Knob 5 -> selected track Volume
- **Bank 2**: Knobs 1-4 -> selected track Remotes 5-8, Knob 5 -> selected track Volume
- **Bank 5**: Knobs 1-3 -> selected track Sends 1-3, Knob 4 -> selected track Pan, Knob 5 -> selected track Volume

By default, Knob 5 (`CC 7`) controls selected track volume in all knob banks.

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
