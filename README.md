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
- **Click-through transparency**: Fade selected orbs by 0-100% while click-through is active (default 50%). Normal appearance returns when clicks are restored.
- **Suppress orb tooltips**: On by default. Suppresses native orb tooltips/highlights and supported plugin tooltips from Prayer, Run Energy, Poison, Spec Regen Timer and Quick Prayer Preview. Unrelated mouse tooltips remain visible.
- **Fade plugin-created orb overlays**: On by default. Applies the transparency setting to supported orb visuals from Prayer, Soulreaper Axe QoL, Regeneration Meter and Poison Ring.
