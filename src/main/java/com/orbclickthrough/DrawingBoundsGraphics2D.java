package com.orbclickthrough;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImageObserver;

/** Records actual drawing operations, including those on graphics.create() children. */
final class DrawingBoundsGraphics2D extends DelegatingGraphics2D
{
    static final class Drawing
    {
        final Rectangle canvas;
        Rectangle bounds;
        Drawing(Rectangle canvas) { this.canvas = canvas; }
        void add(Rectangle rectangle)
        {
            rectangle = rectangle.intersection(canvas);
            if (rectangle.isEmpty()) return;
            if (bounds == null) bounds = rectangle;
            else bounds.add(rectangle);
        }
    }

    private final Drawing drawing;

    DrawingBoundsGraphics2D(Graphics2D delegate, Drawing drawing)
    {
        super(delegate);
        this.drawing = drawing;
    }

    private void mark(Shape shape, boolean stroke)
    {
        if (stroke) shape = delegate.getStroke().createStrokedShape(shape);
        Rectangle bounds = delegate.getTransform().createTransformedShape(shape).getBounds();
        bounds.grow(2, 2); // Include rasterization/antialiasing at the edges.
        drawing.add(bounds);
    }

    private void box(double x, double y, double width, double height, boolean stroke)
    {
        mark(new Rectangle2D.Double(Math.min(x, x + width), Math.min(y, y + height),
            Math.abs(width), Math.abs(height)), stroke);
    }

    private void image(Image image, double x, double y, ImageObserver observer)
    {
        int width = image.getWidth(observer), height = image.getHeight(observer);
        if (width < 0 || height < 0) drawing.add(new Rectangle(drawing.canvas));
        else box(x, y, width, height, false);
    }

    @Override public Graphics create()
    {
        return new DrawingBoundsGraphics2D((Graphics2D) delegate.create(), drawing);
    }

    @Override public void draw(Shape shape) { mark(shape, true); delegate.draw(shape); }
    @Override public void fill(Shape shape) { mark(shape, false); delegate.fill(shape); }
    @Override public void drawLine(int x1, int y1, int x2, int y2)
    { mark(new Line2D.Double(x1, y1, x2, y2), true); delegate.drawLine(x1, y1, x2, y2); }
    @Override public void fillRect(int x, int y, int width, int height)
    { box(x, y, width, height, false); delegate.fillRect(x, y, width, height); }
    @Override public void clearRect(int x, int y, int width, int height)
    { box(x, y, width, height, false); delegate.clearRect(x, y, width, height); }
    @Override public void drawOval(int x, int y, int width, int height)
    { box(x, y, width, height, true); delegate.drawOval(x, y, width, height); }
    @Override public void fillOval(int x, int y, int width, int height)
    { box(x, y, width, height, false); delegate.fillOval(x, y, width, height); }
    @Override public void drawArc(int x, int y, int width, int height, int start, int arc)
    { box(x, y, width, height, true); delegate.drawArc(x, y, width, height, start, arc); }
    @Override public void fillArc(int x, int y, int width, int height, int start, int arc)
    { box(x, y, width, height, false); delegate.fillArc(x, y, width, height, start, arc); }
    @Override public void drawRoundRect(int x, int y, int width, int height, int aw, int ah)
    { box(x, y, width, height, true); delegate.drawRoundRect(x, y, width, height, aw, ah); }
    @Override public void fillRoundRect(int x, int y, int width, int height, int aw, int ah)
    { box(x, y, width, height, false); delegate.fillRoundRect(x, y, width, height, aw, ah); }
    @Override public void drawPolygon(int[] x, int[] y, int n)
    { mark(new java.awt.Polygon(x, y, n), true); delegate.drawPolygon(x, y, n); }
    @Override public void fillPolygon(int[] x, int[] y, int n)
    { mark(new java.awt.Polygon(x, y, n), false); delegate.fillPolygon(x, y, n); }
    @Override public void drawPolyline(int[] x, int[] y, int n)
    { mark(new java.awt.Polygon(x, y, n), true); delegate.drawPolyline(x, y, n); }
    @Override public void copyArea(int x, int y, int width, int height, int dx, int dy)
    { drawing.add(new Rectangle(drawing.canvas)); delegate.copyArea(x, y, width, height, dx, dy); }

    @Override public void drawString(String text, float x, float y)
    {
        if (!text.isEmpty())
        {
            Rectangle2D bounds = new java.awt.font.TextLayout(text, delegate.getFont(), delegate.getFontRenderContext()).getBounds();
            box(x + bounds.getX(), y + bounds.getY(), bounds.getWidth(), bounds.getHeight(), false);
        }
        delegate.drawString(text, x, y);
    }
    @Override public void drawString(String text, int x, int y) { drawString(text, (float) x, (float) y); }
    @Override public void drawGlyphVector(java.awt.font.GlyphVector glyphs, float x, float y)
    { mark(glyphs.getOutline(x, y), false); delegate.drawGlyphVector(glyphs, x, y); }
    @Override public void drawString(java.text.AttributedCharacterIterator text, float x, float y)
    { drawing.add(new Rectangle(drawing.canvas)); delegate.drawString(text, x, y); }
    @Override public void drawString(java.text.AttributedCharacterIterator text, int x, int y)
    { drawing.add(new Rectangle(drawing.canvas)); delegate.drawString(text, x, y); }

    @Override public boolean drawImage(Image image, int x, int y, ImageObserver observer)
    { image(image, x, y, observer); return delegate.drawImage(image, x, y, observer); }
    @Override public boolean drawImage(Image image, int x, int y, Color bg, ImageObserver observer)
    { image(image, x, y, observer); return delegate.drawImage(image, x, y, bg, observer); }
    @Override public boolean drawImage(Image image, int x, int y, int width, int height, ImageObserver observer)
    { box(x, y, width, height, false); return delegate.drawImage(image, x, y, width, height, observer); }
    @Override public boolean drawImage(Image image, int x, int y, int width, int height, Color bg, ImageObserver observer)
    { box(x, y, width, height, false); return delegate.drawImage(image, x, y, width, height, bg, observer); }
    @Override public boolean drawImage(Image image, int x1, int y1, int x2, int y2,
        int sx1, int sy1, int sx2, int sy2, ImageObserver observer)
    { box(x1, y1, x2 - x1, y2 - y1, false); return delegate.drawImage(image, x1, y1, x2, y2, sx1, sy1, sx2, sy2, observer); }
    @Override public boolean drawImage(Image image, int x1, int y1, int x2, int y2,
        int sx1, int sy1, int sx2, int sy2, Color bg, ImageObserver observer)
    { box(x1, y1, x2 - x1, y2 - y1, false); return delegate.drawImage(image, x1, y1, x2, y2, sx1, sy1, sx2, sy2, bg, observer); }
    @Override public boolean drawImage(Image image, AffineTransform transform, ImageObserver observer)
    {
        int width = image.getWidth(observer), height = image.getHeight(observer);
        if (width < 0 || height < 0) drawing.add(new Rectangle(drawing.canvas));
        else mark(transform.createTransformedShape(new Rectangle(0, 0, width, height)), false);
        return delegate.drawImage(image, transform, observer);
    }
    // Unusual filter/renderable paths conservatively capture the whole canvas.
    @Override public void drawImage(java.awt.image.BufferedImage image, java.awt.image.BufferedImageOp op, int x, int y)
    { drawing.add(new Rectangle(drawing.canvas)); delegate.drawImage(image, op, x, y); }
    @Override public void drawRenderedImage(java.awt.image.RenderedImage image, AffineTransform transform)
    { drawing.add(new Rectangle(drawing.canvas)); delegate.drawRenderedImage(image, transform); }
    @Override public void drawRenderableImage(java.awt.image.renderable.RenderableImage image, AffineTransform transform)
    { drawing.add(new Rectangle(drawing.canvas)); delegate.drawRenderableImage(image, transform); }
}
