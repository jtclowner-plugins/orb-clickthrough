package com.orbclickthrough;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ClientTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class OrbAppearanceTest
{
    private final Client client = mock(Client.class);
    private final OrbWidgetTransformer transformer = new OrbWidgetTransformer(client);

    private Widget visual(int opacity)
    {
        Widget widget = mock(Widget.class);
        AtomicInteger value = new AtomicInteger(opacity);
        when(widget.getType()).thenReturn(WidgetType.GRAPHIC);
        when(widget.getOpacity()).thenAnswer(invocation -> value.get());
        when(widget.setOpacity(anyInt())).thenAnswer(invocation -> {
            value.set(invocation.getArgument(0));
            return widget;
        });
        return widget;
    }

    @Test
    public void fadesAllChildKindsWithoutAccumulatingAndRestoresOriginalAlpha()
    {
        Widget root = mock(Widget.class);
        Widget background = visual(0);
        Widget icon = visual(100);
        Widget nested = visual(0);
        Widget unrelated = visual(0);
        when(client.getWidget(1)).thenReturn(root);
        when(root.getStaticChildren()).thenReturn(new Widget[]{background, null});
        when(background.getDynamicChildren()).thenReturn(new Widget[]{icon});
        when(root.getNestedChildren()).thenReturn(new Widget[]{nested});
        // Shared references and even cycles must not cause duplicate work.
        when(icon.getNestedChildren()).thenReturn(new Widget[]{root});
        transformer.allowClickThroughTree(1);
        transformer.applyTransparency(50);
        transformer.applyTransparency(50);
        assertEquals(128, background.getOpacity());
        assertEquals(178, icon.getOpacity());
        assertEquals(128, nested.getOpacity());
        assertEquals(0, unrelated.getOpacity());
        transformer.restoreOrbWidgetsChangedByUs();
        assertEquals(0, background.getOpacity());
        assertEquals(100, icon.getOpacity());
        assertEquals(0, nested.getOpacity());
    }

    @Test
    public void nextFrameUsesUpdatedGameOpacityAndZeroDisablesFading()
    {
        Widget widget = visual(0);
        transformer.allowClickThrough(widget);
        transformer.applyTransparency(50);
        transformer.restoreTransparency();
        widget.setOpacity(100);
        transformer.applyTransparency(50);
        assertEquals(178, widget.getOpacity());
        transformer.applyTransparency(0);
        assertEquals(100, widget.getOpacity());
        transformer.applyTransparency(150);
        assertEquals(255, widget.getOpacity());
        transformer.restoreEverythingChangedByUs();
        assertEquals(100, widget.getOpacity());
    }

    @Test
    public void renderingFollowsEveryActivationMode() throws Exception
    {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        for (OrbClickthroughActivationMode mode : OrbClickthroughActivationMode.values())
        {
            for (boolean active : new boolean[]{false, true})
            {
                OrbClickthroughConfig config = mock(OrbClickthroughConfig.class);
                when(config.activationMode()).thenReturn(mode);
                when(config.clickThroughTransparency()).thenReturn(65);
                OrbWidgetTransformer mockedTransformer = mock(OrbWidgetTransformer.class);
                OrbClickthroughPlugin plugin = new OrbClickthroughPlugin();
                set(plugin, "client", client);
                set(plugin, "config", config);
                set(plugin, "widgetTransformer", mockedTransformer);
                set(plugin, "hotkeyHeld", mode == OrbClickthroughActivationMode.HOLD_TO_RESTORE_CLICKS ? !active : active);
                set(plugin, "toggleActive", active);
                plugin.onBeforeRender(new BeforeRender());
                verify(mockedTransformer, times(active ? 1 : 0)).applyTransparency(65);
                plugin.onClientTick(new ClientTick());
                verify(mockedTransformer).restoreTransparency();
            }
        }
    }

    static void set(Object target, String name, Object value) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
