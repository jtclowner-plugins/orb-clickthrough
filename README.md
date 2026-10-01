# Orb Clickthrough
Orb Clickthrough makes selected minimap orbs click-through while a configurable hotkey mode is active.

It is intended for players who want to avoid accidentally clicking minimap orbs such as Quick-Prayers, Special Attack, Run, World Map, XP, or Wiki while playing.

![Example - Stop misclicking the Run Energy orb](images/clickthrough.png)

## Features
- Make selected minimap orbs click-through
- Choose which orbs are affected
- Configure hotkey behaviour:
  - Hold hotkey to click-through orbs
  - Click-through orbs by default, and hold hotkey to click orbs as normal
  - Toggle click-through mode with the hotkey
- Handles the base Wiki orb and the replacement orb used by the Wiki plugin

## Configuration
### Hotkey activation

- **Hotkey**: The key used to activate or restore orb clicks.
- **Mode**: Controls how the hotkey affects click-through behaviour.

### Click-through orbs
Enable or disable click-through handling for each supported minimap orb.

### Miscellaneous
- **Click-through transparency**: Fade selected orbs by 0–100% (default 50%) whenever click-through is active, in every hotkey mode. Normal appearance returns when clicks are restored. Changes to this setting preserve the current held/toggled state.
- **Disable click-through hover effects**: Optional checkbox (off by default) for native orb hover highlights and tooltips. Stat values and active prayer/run/XP indicators continue updating normally.
- **Suppress plugin-created orb tooltips**: On by default. Suppresses Prayer statistics, Run Energy, Poison and both Quick Prayer Preview display modes for selected click-through orbs. Independent of the native hover checkbox. All other mouse tooltips remain visible.
- **Hide World Map tooltip**: Hides the World Map hover tooltip while orb click-through is active.

## Plugin tooltip compatibility

Support is implemented entirely inside Orb Clickthrough and works with the unmodified companion plugins and the normal developer launcher. No companion patches, API changes or special client are needed.

Exact overlay class names identify the supported tooltip producers. Their original instances stay registered for their owners to remove on shutdown; temporary render adapters preserve their drawings while discarding only tooltip objects added by that specific render call. Quick Prayer Preview's dedicated tooltip overlay is skipped while suppression applies, covering its directly drawn panel too. Original rendering is restored when Orb Clickthrough stops. Unknown overlays are untouched.

Soulreaper Axe QoL, Regeneration Meter and Poison Ring do not generate orb tooltips in the inspected sources. LITE Regen Meter is excluded.

Transparency currently affects native widgets only. The previous companion-based overlay fading option and test harness have been removed; independent overlay fading is not implemented in this version.

## Testing

Build using the normal developer build/launcher script, or run `./gradlew build` with JDK 11. Select Prayer, Run and/or Health in Orb Clickthrough and enable the relevant other plugins.

Check each hotkey mode, the suppression checkbox, both Quick Prayer Preview display modes, and stopping/restarting either plugin. Prayer's dose indicator should remain visible. Mouse Tooltips for NPCs, ground items and objects should remain visible. Native hover colouring has its own checkbox.
