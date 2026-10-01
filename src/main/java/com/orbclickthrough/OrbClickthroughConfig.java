package com.orbclickthrough;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("orbclickthrough")
public interface OrbClickthroughConfig extends Config
{
	@ConfigSection(
			name = "Hotkey activation",
			description = "Controls if selected orb buttons become click-through via hotkey.",
			position = 0
	)
	String activationSection = "activationSection";

	@ConfigSection(
			name = "Click-through orbs",
			description = "Choose which minimap orb buttons should be made click-through.",
			position = 1
	)
	String orbsSection = "orbsSection";

	@ConfigSection(
			name = "Miscellaneous",
			description = "Extra behaviour and compatibility options.",
			position = 2,
			closedByDefault = true
	)
	String miscSection = "miscSection";

	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
			keyName = "clickThroughTransparency",
			name = "Click-through transparency",
			description = "Transparency of selected orbs while click-through is active. 0% keeps their normal appearance; 100% is transparent.",
			section = miscSection,
			position = 1
	)
	default int clickThroughTransparency()
	{
		return 50;
	}

	@ConfigItem(
			keyName = "disableHoverEffects",
			name = "Disable click-through hover effects",
			description = "Hide orb hover highlights and tooltips while hovering a selected click-through orb.",
			section = miscSection,
			position = 2
	)
	default boolean disableHoverEffects()
	{
		return false;
	}

	@ConfigItem(
			keyName = "fadePluginOverlays",
			name = "Fade plugin-created orb overlays",
			description = "Apply click-through transparency to supported plugin visuals, such as the Prayer flick indicator and Regeneration Meter's special-attack ring. Requires compatible plugin versions.",
			section = miscSection,
			position = 3
	)
	default boolean fadePluginOverlays()
	{
		return true;
	}

	@ConfigItem(
			keyName = "suppressPluginTooltips",
			name = "Suppress plugin-created orb tooltips",
			description = "Hide supported plugins' tooltips for selected click-through orbs. Mouse tooltips for NPCs and objects underneath remain visible. Requires compatible plugin versions.",
			section = miscSection,
			position = 4
	)
	default boolean suppressPluginTooltips()
	{
		return true;
	}

	@ConfigItem(
			keyName = "hotkey",
			name = "Hotkey",
			description = "Hotkey used to activate or restore orb clicks. If no hotkey is set, orbs are always click-through.",
			section = activationSection,
			position = 0
	)
	default Keybind hotkey()
	{
		return Keybind.SHIFT;
	}

	@ConfigItem(
			keyName = "activationMode",
			name = "Mode",
			description = "Controls how the hotkey changes orb click-through behaviour.",
			section = activationSection,
			position = 1
	)
	default OrbClickthroughActivationMode activationMode()
	{
		return OrbClickthroughActivationMode.HOLD_TO_RESTORE_CLICKS;
	}

	@ConfigItem(
			keyName = "manageHealthOrb",
			name = "Hitpoints orb",
			description = "Make the Hitpoints orb click-through when active.",
			section = orbsSection,
			position = 10
	)
	default boolean manageHealthOrb()
	{
		return true;
	}

	@ConfigItem(
			keyName = "managePrayerOrb",
			name = "Prayer orb",
			description = "Make the Prayer orb click-through when active.",
			section = orbsSection,
			position = 11
	)
	default boolean managePrayerOrb()
	{
		return false;
	}

	@ConfigItem(
			keyName = "manageRunOrb",
			name = "Run orb",
			description = "Make the Run orb click-through when active.",
			section = orbsSection,
			position = 12
	)
	default boolean manageRunOrb()
	{
		return false;
	}

	@ConfigItem(
			keyName = "manageSpecialAttackOrb",
			name = "Special Attack orb",
			description = "Make the Special Attack orb click-through when active.",
			section = orbsSection,
			position = 13
	)
	default boolean manageSpecialAttackOrb()
	{
		return false;
	}

	@ConfigItem(
			keyName = "manageWorldMapOrb",
			name = "World Map orb",
			description = "Make the World Map orb click-through when active.",
			section = orbsSection,
			position = 14
	)
	default boolean manageWorldMapOrb()
	{
		return true;
	}

	@ConfigItem(
			keyName = "manageXpOrb",
			name = "XP orb",
			description = "Make the XP orb click-through when active.",
			section = orbsSection,
			position = 15
	)
	default boolean manageXpOrb()
	{
		return true;
	}

	@ConfigItem(
			keyName = "manageActivityOrb",
			name = "Activity orb",
			description = "Make the Activity orb click-through when active.",
			section = orbsSection,
			position = 16
	)
	default boolean manageActivityOrb()
	{
		return true;
	}

	@ConfigItem(
			keyName = "manageWikiOrb",
			name = "Wiki orb",
			description = "Make the Wiki orb click-through when active.",
			section = orbsSection,
			position = 17
	)
	default boolean manageWikiOrb()
	{
		return true;
	}

	@ConfigItem(
			keyName = "manageStoreOrb",
			name = "Store orb",
			description = "Make the Store orb click-through when active.",
			section = orbsSection,
			position = 18
	)
	default boolean manageStoreOrb()
	{
		return true;
	}

	@ConfigItem(
			keyName = "manageCompassOrb",
			name = "Compass orb",
			description = "Make the Compass orb click-through when active.",
			section = orbsSection,
			position = 19
	)
	default boolean manageCompassOrb()
	{
		return false;
	}

	@ConfigItem(
			keyName = "manageLogoutOrb",
			name = "Logout orb",
			description = "Make the Logout orb click-through when active.",
			section = orbsSection,
			position = 20
	)
	default boolean manageLogoutOrb()
	{
		return false;
	}

	@ConfigItem(
			keyName = "hideWorldMapTooltip",
			name = "Hide World Map tooltip",
			description = "Hide the World Map hover tooltip while orb click-through is active.",
			section = miscSection,
			position = 0
	)
	default boolean hideWorldMapTooltip()
	{
		return true;
	}
}
