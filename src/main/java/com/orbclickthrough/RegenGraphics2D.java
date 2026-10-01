package com.orbclickthrough;

/** Graphics delegate used only for Regeneration Meter's two independently coloured strokes. */
final class RegenGraphics2D extends DelegatingGraphics2D
{
    private static final java.awt.Color HEALTH_COLOR = ringColor(0x9B0703);
    private static final java.awt.Color SPECIAL_COLOR = ringColor(0x1E95B0);
    private final float health;
    private final float special;

    RegenGraphics2D(java.awt.Graphics2D delegate, float health, float special)
    {
        super(delegate);
        this.health = health;
        this.special = special;
    }

    private static java.awt.Color ringColor(int rgb)
    {
        float[] hsv = java.awt.Color.RGBtoHSB(rgb >>> 16, (rgb >> 8) & 255, rgb & 255, null);
        return java.awt.Color.getHSBColor(hsv[0], 1f, 1f);
    }

    @Override
    public java.awt.Graphics create()
    {
        return new RegenGraphics2D((java.awt.Graphics2D) delegate.create(), health, special);
    }

    @Override
    public void draw(java.awt.Shape p0)
    {
        // The inspected RegenMeterOverlay uses these fixed colours for HP and spec.
        // Identify the stroke, not its screen position, so moved/overlapping orbs stay independent.
        float opacity = delegate.getColor().equals(HEALTH_COLOR) ? health
            : delegate.getColor().equals(SPECIAL_COLOR) ? special : 1f;
        java.awt.Graphics2D stroke = (java.awt.Graphics2D) delegate.create();
        try
        {
            if (stroke.getComposite() instanceof java.awt.AlphaComposite)
            {
                java.awt.AlphaComposite composite = (java.awt.AlphaComposite) stroke.getComposite();
                stroke.setComposite(composite.derive(composite.getAlpha() * opacity));
            }
            stroke.draw(p0);
        }
        finally
        {
            stroke.dispose();
        }
    }

}
