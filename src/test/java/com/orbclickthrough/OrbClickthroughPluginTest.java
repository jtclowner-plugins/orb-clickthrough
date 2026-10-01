package com.orbclickthrough;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.Plugin;
import java.util.ArrayList;
import java.util.List;

/** Development launcher; this class is not part of the published plugin. */
public class OrbClickthroughPluginTest
{
    public static void main(String[] args) throws Exception
    {
        List<Class<? extends Plugin>> plugins = new ArrayList<>();
        plugins.add(OrbClickthroughPlugin.class);
        if (Boolean.getBoolean("orbclickthrough.compatibility"))
        {
            plugins.add(Class.forName("com.soulreaperaxeqol.SoulreaperAxeQoLPlugin").asSubclass(Plugin.class));
            plugins.add(Class.forName("io.hydrox.quickprayerpreview.QuickPrayerPreviewPlugin").asSubclass(Plugin.class));
            plugins.add(Class.forName("com.github.corhen.poisonring.PoisonRingPlugin").asSubclass(Plugin.class));
        }
        @SuppressWarnings("unchecked")
        Class<? extends Plugin>[] pluginClasses = plugins.toArray(new Class[0]);
        ExternalPluginManager.loadBuiltin(pluginClasses);
        RuneLite.main(args);
    }
}
