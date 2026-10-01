package com.orbclickthrough;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.config.Keybind;
import org.junit.Test;

public class OrbClickthroughTest
{
	@Test
	public void defaultConfigValuesAreExpected()
	{
		OrbClickthroughConfig config = new OrbClickthroughConfig() {};

		assertEquals(Keybind.SHIFT, config.hotkey());
		assertEquals(OrbClickthroughActivationMode.HOLD_TO_RESTORE_CLICKS, config.activationMode());

		assertTrue(config.manageHealthOrb());
		assertFalse(config.managePrayerOrb());
		assertFalse(config.manageRunOrb());
		assertFalse(config.manageSpecialAttackOrb());

		assertTrue(config.manageWorldMapOrb());
		assertTrue(config.manageXpOrb());
		assertTrue(config.manageActivityOrb());
		assertTrue(config.manageWikiOrb());
		assertTrue(config.manageStoreOrb());

		assertTrue(config.suppressOrbTooltips());
	}

	@Test
	public void fixedModeHasNoActiveResizableNoClickLayout()
	{
		assertEquals(-1, OrbClickthroughPlugin.selectActiveMapNoClick0(
				false, true, true, true, true));
	}

	@Test
	public void visibleModernLayoutWins()
	{
		assertEquals(InterfaceID.ToplevelPreEoc.MAP_NOCLICK_0,
				OrbClickthroughPlugin.selectActiveMapNoClick0(
						true, true, false, true, true));
	}

	@Test
	public void visibleClassicLayoutWins()
	{
		assertEquals(InterfaceID.ToplevelOsrsStretch.MAP_NOCLICK_0,
				OrbClickthroughPlugin.selectActiveMapNoClick0(
						true, false, true, true, true));
	}

	@Test
	public void existingLayoutIsUsedWhileVisibilitySettles()
	{
		assertEquals(InterfaceID.ToplevelPreEoc.MAP_NOCLICK_0,
				OrbClickthroughPlugin.selectActiveMapNoClick0(
						true, false, false, true, false));

		assertEquals(InterfaceID.ToplevelOsrsStretch.MAP_NOCLICK_0,
				OrbClickthroughPlugin.selectActiveMapNoClick0(
						true, false, false, false, true));
	}

	@Test
	public void missingLayoutsReturnNoActiveLayout()
	{
		assertEquals(-1, OrbClickthroughPlugin.selectActiveMapNoClick0(
				true, false, false, false, false));
	}
}