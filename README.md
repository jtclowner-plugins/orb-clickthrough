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

At 100%, native visual widgets are hidden for the drawing frame and restored before game scripts update them again. This avoids the native sprite renderer's remaining 1/256 opacity. The modern logout orb includes both its button/backing and its separate sibling icon.
- **Suppress orb tooltips**: On by default. Suppresses native orb tooltips/highlights, the World Map tooltip, Prayer statistics, Run Energy, Poison, Spec Regen Timer and both Quick Prayer Preview display modes for selected click-through orbs. All unrelated mouse tooltips remain visible.
- **Fade plugin-created orb overlays**: On by default. Applies the transparency percentage to supported plugin orb visuals while their associated orb is click-through. Independent of tooltip suppression.
- **Additional orb overlays**: Optional mappings for other plugins. Leave blank to use built-in support. See below for the format.

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

Exact overlay classes identify the supported producers. Original instances remain registered so their owners can remove them on shutdown. Temporary adapters preserve the original rendering layer and restore normal rendering when Orb Clickthrough stops. Changes to an overlay's layer, position, priority or mapping are resynchronized; overlays with manual draw hooks are left to their owners to avoid duplicate rendering. Unknown overlays are untouched unless explicitly mapped.

Replacement orbs are drawn into a reusable canvas-sized offscreen buffer and faded once as a group, including their text and overlapping fills. A graphics wrapper records actual drawing bounds, including child graphics contexts, so only painted regions are cleared and copied. No plugin-specific drawing rectangles, offset configuration keys or text-width guesses are needed. Regeneration Meter's two strokes are identified by the fixed HP/spec colours in its source, so their transparency remains independent even when their positions overlap. This adapter affects only Regeneration Meter, not other drawings with the same colours.

Tooltip suppression removes only tooltip objects added during a known producer's synchronous render call; it never matches tooltip text or clears the shared queue. Quick Prayer Preview's dedicated tooltip overlay is skipped to cover its directly drawn panel too.

### Additional orb overlays

Enter a fully qualified overlay class and its associated orb, one mapping per line (semicolons also work):

```text
example.plugin.ExtraPrayerOverlay=prayer
example.plugin.CompassTooltipOverlay=compass:tooltip
```

Supported orb names: `health`, `prayer`, `run`, `special`, `compass`, `worldMap`, `xp`, `activity`, `wiki`, `store`, `logout`. Mappings follow the associated orb's selection, hotkey state, transparency and the two existing checkboxes. Removing a mapping restores the original overlay. Invalid mappings are ignored and logged.

Use `:tooltip` only for an entire overlay dedicated to displaying a tooltip panel: suppression skips its rendering. Without that suffix, normal drawing is preserved and only newly queued tooltip objects are suppressed. Map only overlays dedicated to an orb; a mixed overlay containing unrelated information cannot be separated automatically. Mouse Tooltips is always excluded, including from custom mappings. The class name is available in the plugin's source. Future plugins can be mapped without changing their source or Orb Clickthrough, provided they use ordinary dynamic, detached or tooltip overlays without manual draw hooks. Tooltips added outside the overlay's render call cannot be attributed this way.

### Native compass

The complete compass click tree is managed in fixed, classic resizable and modern resizable layouts. Its needle uses content type 1339, and its frame shares a sprite with the minimap. Consequently, widget opacity is insufficient. A small native-canvas compositor fades the circular compass area after the minimap layer draws, preserving the actual rotating needle and themed frame while leaving the surrounding minimap unchanged. This native fading follows the transparency slider independently of the plugin-overlay checkbox.

GPU pixels are blended with premultiplied alpha; software resizable mode uses the current scene captured before widget drawing. Fixed mode fades against its black background (or transparent GPU surface). Previous changes are restored before each frame and on shutdown, preventing accumulation when native drawings are cached. If the software scene capture is unavailable, fading is skipped instead of using stale pixels. In-game verification is needed for compass placement and appearance with each layout and theme.

## Testing

Build using the normal developer build/launcher script, or run `./gradlew build` with JDK 11. No extra launch flag is required.

Check all hotkey modes and 0%, 50%, and 100% transparency; native and replacement orbs should restore immediately when clicks are restored. Test both the compass needle and frame in all three layouts, with GPU on/off and your Interface Styles/Resource Packs settings. The minimap and menus over the compass should stay unchanged. Toggle the two checkboxes separately. Check Soulreaper's extra-orb offsets, both text layouts and its display-layer setting; check HP/spec regeneration rings with only one orb selected. Test both Quick Prayer Preview modes and stopping/restarting either plugin. Mouse Tooltips for NPCs, ground items and objects should remain visible.

Test-only upstream source snapshots and assets are documented in `src/test/FIXTURES.md`; they are not included in the production plugin jar.
