package com.github.corhen.poisonring;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("poisonring")
public interface PoisonRingConfig extends Config {

    @ConfigItem(
        keyName = "shiftX",
        name = "Shift X",
        description = "Shifts the ring left (negative) or right (positive)",
        position = 1
    )
    default int shiftX() {
        return 1;
    }

    @ConfigItem(
        keyName = "shiftY",
        name = "Shift Y",
        description = "Shifts the ring up (negative) or down (positive)",
        position = 2
    )
    default int shiftY() {
        return 1;
    }

    @ConfigItem(
        keyName = "diameter",
        name = "Diameter",
        description = "Shrinks (negative) or expands (positive) the circle diameter",
        position = 3
    )
    default int diameter() {
        return -1;
    }

    @ConfigItem(
        keyName = "lineThickness",
        name = "Line Thickness",
        description = "Changes the thickness of the ring",
        position = 4
    )
    default int lineThickness() {
        return 2;
    }

    @ConfigItem(
        keyName = "poisonColor",
        name = "Poison Color",
        description = "The color of the ring when poisoned",
        position = 5
    )
    default Color poisonColor() {
        return new Color(0, 255, 0, 255);
    }

    @ConfigItem(
        keyName = "testMode",
        name = "Test Mode",
        description = "Displays the ring for alignment purposes without being poisoned",
        position = 0 // Put it at the very top
    )
    default boolean testMode() {
        return false;
    }

    @ConfigItem(
        keyName = "venomColor",
        name = "Venom Color",
        description = "The color of the ring when venomed",
        position = 6
    )
    default Color venomColor() {
        return new Color(0, 255, 0, 255);
    }
}
