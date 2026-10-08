# Oplus Bypass Charging ⚡

A clean, open-source root utility for controlling Oplus bypass charging from the app and Quick Settings.

> **Lead app developer:** [Robinop](https://github.com/robinop)
> **Project / original concept:** [Thor](https://github.com/almightygodthor)

## What it does

- Root-powered Oplus bypass charging control.
- Quick Settings tile with live ON/OFF state.
- Live battery percentage, current, voltage and calculated power.
- Automatically restores normal charging when the charger is disconnected.
- Modern vivid-blue glass-style AMOLED UI.
- No dependency on the original APK's package, classes or branding.

## Oplus control interface

The initial implementation targets the Oplus charging interface discovered from the original working application:

`/sys/devices/virtual/oplus_chg/battery/mmi_charging_enable`

- `0` → bypass enabled
- `1` → normal charging

Root is required to write this node.

## Live power telemetry

The app reads standard Android power-supply interfaces when available:

- `/sys/class/power_supply/battery/capacity`
- `/sys/class/power_supply/battery/current_now`
- `/sys/class/power_supply/battery/voltage_now`
- USB/AC online state

Power is calculated from current × voltage. Hardware/vendor implementations can expose different nodes, so unsupported readings are shown as unavailable rather than fabricated.

## Build

The repository uses Android Gradle Plugin **9.4.0**, Gradle **9.6.0**, JDK 17 and Android API 37.

GitHub Actions builds the debug APK automatically on pushes to `main` and can also be started manually.

## Credits

- **Robinop** — lead application developer and original application implementation.
- **Thor / Thundergod Thor** — project owner, original concept and Oplus charging research.

This repository is a clean source reimplementation; the original APK is used only as a behavioral reference.

## Safety

This utility directly controls a vendor charging interface and requires root. Use only on compatible Oplus hardware where the interface is known to be valid. Do not assume that a sysfs interface is safe or equivalent across different devices or kernels.
