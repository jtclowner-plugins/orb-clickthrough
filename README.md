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
- **Suppress orb tooltips**: On by default. Suppresses native orb tooltips/highlights, the World Map tooltip, Prayer statistics, Run Energy, Poison, Spec Regen Timer and both Quick Prayer Preview display modes for selected click-through orbs. All unrelated mouse tooltips remain visible.
- **Fade plugin-created orb overlays**: On by default. Applies the transparency percentage to supported plugin orb visuals while their associated orb is click-through. Independent of tooltip suppression.

## Plugin compatibility

Support is implemented entirely inside Orb Clickthrough and works with unmodified plugins and the normal developer launcher. No companion patches, API changes or special client are needed.

| Plugin visuals | Associated orb |
| --- | --- |
| Prayer flick indicator and dose indicator | Prayer |
| Soulreaper Axe QoL replacement orb, extra orb, text, icons, borders and timers | Special Attack |
| Regeneration Meter HP ring | Hitpoints |
| Regeneration Meter special-attack ring | Special Attack |
| Poison Ring | Hitpoints |

Select the associated orb to apply fading. Prayer and Special Attack are not selected by default. Soulreaper's offset extra orb follows the Special Attack selection. Spec Regen Timer's tooltip is suppressed while the selected Special Attack orb is click-through. LITE Regen Meter is excluded.

Exact overlay classes identify the supported producers. Original instances remain registered so their owners can remove them on shutdown. Temporary adapters preserve the original rendering layer and restore normal rendering when Orb Clickthrough stops. Unknown overlays are untouched.

Replacement orbs are drawn into a reusable small offscreen buffer and faded once as a group, including their text and overlapping fills. Regeneration Meter's two strokes are identified by the fixed HP/spec colours in its source, so their transparency remains independent even when their positions overlap. This adapter affects only Regeneration Meter, not other drawings with the same colours.

Tooltip suppression removes only tooltip objects added during a known producer's synchronous render call; it never matches tooltip text or clears the shared queue. Quick Prayer Preview's dedicated tooltip overlay is skipped to cover its directly drawn panel too.

## Testing

Build using the normal developer build/launcher script, or run `./gradlew build` with JDK 11. No extra launch flag is required.

Check all hotkey modes and 0%, 50%, and 100% transparency; native and replacement orbs should restore immediately when clicks are restored. Toggle the two checkboxes separately. Check Soulreaper's extra-orb offsets, both text layouts and its display-layer setting; check HP/spec regeneration rings with only one orb selected. Test both Quick Prayer Preview modes and stopping/restarting either plugin. Mouse Tooltips for NPCs, ground items and objects should remain visible.

Test-only upstream source snapshots and assets are documented in `src/test/FIXTURES.md`; they are not included in the production plugin jar.
