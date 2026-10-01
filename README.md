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
- **Fade plugin-created orb overlays**: On by default. Compatible plugins apply the selected transparency to their orb visuals, including Prayer's flick indicator and Regeneration Meter's special-attack ring.
- **Suppress plugin-created orb tooltips**: On by default. Compatible plugins suppress their own tooltip for selected click-through orbs. This never clears the shared tooltip queue, so Mouse Tooltips for NPCs and objects underneath remain visible.
- **Hide World Map tooltip**: Hides the World Map hover tooltip while orb click-through is active.

The two plugin compatibility options work independently of the native hover checkbox and of each other. They require the companion patches; **the current unpatched Plugin Hub/client versions do not yet participate**. Patched plugins behave normally when Orb Clickthrough is disabled or absent.

## Testing companion integrations

The development client includes patches for Prayer, Run Energy, Quick Prayer Preview, Soulreaper Axe QoL, Regeneration Meter, Poison and Poison Ring. LITE Regen Meter and Spec Regen Timer are excluded.

With Git and JDK 11 available, run from this checkout:

```powershell
.\gradlew.bat test -Pcompatibility
.\gradlew.bat run -Pcompatibility
```

The first run fetches pinned sources and applies the checked-in patches under `build/`. It uses development overrides of the affected built-in overlays and loads the three patched external plugins with Orb Clickthrough. It does not replace installed jars. Enable the plugins and select the relevant orbs in Orb Clickthrough; Prayer, Run and Special Attack are not selected by default.

Without `-Pcompatibility`, `run` loads only Orb Clickthrough and the ordinary RuneLite dependencies. The normal plugin jar never includes companion implementations.

See [compatibility/README.md](compatibility/README.md) for the protocol, patch bases and manual test cases.

