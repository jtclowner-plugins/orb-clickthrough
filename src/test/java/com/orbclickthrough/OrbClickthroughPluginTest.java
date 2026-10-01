package com.orbclickthrough;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Development launcher; this class is not part of the published plugin. */
public class OrbClickthroughPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(OrbClickthroughPlugin.class);
        RuneLite.main(args);
    }
}
