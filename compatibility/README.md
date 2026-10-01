# Companion plugin compatibility (development)

These patches use the existing RuneLite `PluginMessage` event. They do not modify
RuneLite's API, renderer, tooltip manager, or other plugins' configuration. They
are not upstream releases: stock installations will need the companion changes
before the two new controls affect their independent overlays.

## Included integrations

| Source | Effect | Orb policy |
| --- | --- | --- |
| Prayer | Flick/dose graphics; prayer statistics tooltip | Prayer |
| Run Energy | Orb hover tooltip | Run |
| Quick Prayer Preview | Both the direct panel and shared-tooltip modes | Prayer |
| Soulreaper Axe QoL | Native overlay, additional orb, text and regeneration graphics | Special Attack |
| Regeneration Meter | HP and special-attack rings, handled independently | HP / Special Attack |
| Poison | HP-orb hover tooltip | HP |
| Poison Ring | Poison/venom ring, including configured offsets | HP |

LITE Regen Meter and Spec Regen Timer are intentionally excluded. Mouse Tooltips
is a regression-test target, not a consumer of the suppression policy.

## Reproducible local test client

From the repository root, with Git and JDK 11 on the path:

```powershell
.\gradlew.bat test -Pcompatibility
.\gradlew.bat run -Pcompatibility
```

`manifest.json` records exact source revisions and patch paths. Gradle fetches
those revisions, applies the patches, and compiles only the selected built-in
overlays plus the external plugins into the **test** output. The production jar
contains only Orb Clickthrough. No companion changes are installed into your
normal RuneLite installation or the Gradle dependency cache.

For the local test classpath only, Gradle creates an unsigned copy of the RuneLite
client jar under `build/`: Java cannot load unsigned development classes alongside
signed release classes in the same package. The original cached jar is untouched.
The companion sources and test dependencies are fixed to RuneLite 1.13.1 for this
test harness. The `shadowJar` task is disabled in compatibility mode to avoid
mistaking the modified development client for a plugin release.

The development launcher registers all four external plugin classes together.
RuneLite excludes installed hub jars with the same plugin class names from that
development launch. Enable the desired plugins in its configuration panel.

To apply a patch to a separate source checkout at the recorded revision:

```powershell
git apply --check C:\path\to\orb-clickthrough\compatibility\patches\runelite.patch
git apply C:\path\to\orb-clickthrough\compatibility\patches\runelite.patch
```

Substitute the matching patch for each repository. The RuneLite patch changes
only the participating built-in overlay implementations and adds their helper;
it contains no API or renderer changes. Independent maintainer adoption is still
needed to provide these integrations in ordinary client/hub releases.

## Protocol: `orbclickthrough / appearance-v1`

An overlay queries the current policy synchronously during rendering:

```java
BiConsumer<Float, Boolean> reply = (opacity, suppressTooltip) -> {
    // Apply only to this overlay's orb graphics and its own hover output.
};
eventBus.post(new PluginMessage("orbclickthrough", "appearance-v1",
    Map.of("orb", "prayer", "reply", reply)));
```

`reply` uses the JVM's standard `BiConsumer`, avoiding a dependency on Orb
Clickthrough's classloader or jar. It is a synchronous in-process callback, not a
serialized message. Query on the client rendering thread; do not cache policies
across frames. If nobody replies, retain normal opacity (1.0) and show tooltips.

Supported identifiers: `health`, `prayer`, `run`, `special`, `worldMap`, `xp`,
`activity`, `wiki`, `store`, `compass`, `logout`. Unknown identifiers receive normal
behavior. Replies use opacity 0–1, where 0 is invisible, plus a Boolean indicating
whether **that orb's** tooltip should be suppressed. The two config switches are
independent, and both are gated by login state, orb selection and active hotkey
mode. No saved cross-plugin state or startup ordering is required.

`src/main/java/com/orbclickthrough/compat/OrbAppearance.java` is the helper copied
into each participating repository's own package. Each renderer uses a disposable
graphics copy, multiplying existing alpha and leaving shared graphics untouched.
Tooltip producers omit only their own output. No matching of text, proximity,
overlay class names, or contents of the global tooltip queue is involved.

## Validation

The compatibility tests execute the actual patched overlays. They cover 0%, 50%
and 100% transparency; both Quick Prayer Preview modes; Soulreaper's native,
duplicate and text variants; separate HP/spec selection; and the actual Mouse
Tooltips overlay producing an Attack Goblin tooltip alongside suppressed orb
tooltips. Core tests cover all activation modes, independent controls, provider
absence/disable/logout and graphics-state preservation.

Before merging, verify in a logged-in client:

1. All three hotkey modes and changing settings while held/toggled.
2. Both new switches independently, with the native hover checkbox both on/off.
3. An attackable NPC and an interactable object underneath an orb: underlying
   mouse tooltip, left-click action and right-click menu should remain available.
4. The supported overlays while values change: prayer drain/flicking, running,
   HP/spec regeneration, poison/venom and Soulreaper stack changes.
5. Enable/disable Orb Clickthrough and each companion in either order.
6. Fixed/resizable layouts, minimap visibility changes and orb relocation plugins.
   The companions use their existing widget bounds; this does not claim new
   clickthrough support for every alternate layout created by other plugins.
7. GPU and software rendering, including overlays moved via their own offsets.

Keep testing fixes on `codex/clickthrough-appearance`. Once accepted, squash-merge
the branch so main receives the final implementation. Reverts are for undoing
behavior, not for removing intermediate bugs from an unmerged test branch.
