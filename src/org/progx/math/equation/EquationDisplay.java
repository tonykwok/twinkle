package org.progx.math.equation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.GeneralPath;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;

public class EquationDisplay
        extends JComponent
        implements PropertyChangeListener {
    private static final Color COLOR_BACKGROUND = Color.WHITE;
    private static final Color COLOR_MAJOR_GRID = Color.GRAY.brighter();
    private static final Color COLOR_MINOR_GRID = new Color(220, 220, 220);
    private static final Color COLOR_AXIS = Color.BLACK;
    private static final float STROKE_AXIS = 1.2f;
    private static final float STROKE_GRID = 1.0f;
    private static final float COEFF_ZOOM = 1.1f;
    private List<DrawableEquation> equations;
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private double originX;
    private double originY;
    private double majorX;
    private int minorX;
    private double majorY;
    private int minorY;
    private Point dragStart;
    private NumberFormat formatter;

    public EquationDisplay(double originX, double originY, double minX, double maxX, double minY, double maxY, double majorX, int minorX, double majorY, int minorY) {
        if (minX >= maxX) {
            throw new IllegalArgumentException("minX must be < to maxX");
        }
        if (originX < minX) throw new IllegalArgumentException("originX must be between minX and maxX");
        if (originX > maxX) {
            throw new IllegalArgumentException("originX must be between minX and maxX");
        }
        if (minY >= maxY) {
            throw new IllegalArgumentException("minY must be < to maxY");
        }
        if (originY < minY) throw new IllegalArgumentException("originY must be between minY and maxY");
        if (originY > maxY) {
            throw new IllegalArgumentException("originY must be between minY and maxY");
        }
        if (minorX <= 0) {
            throw new IllegalArgumentException("minorX must be > 0");
        }
        if (minorY <= 0) {
            throw new IllegalArgumentException("minorY must be > 0");
        }
        if (majorX <= 0.0) {
            throw new IllegalArgumentException("majorX must be > 0.0");
        }
        if (majorY <= 0.0) {
            throw new IllegalArgumentException("majorY must be > 0.0");
        }
        this.originX = originX;
        this.originY = originY;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.majorX = majorX;
        this.minorX = minorX;
        this.majorY = majorY;
        this.minorY = minorY;
        this.equations = new LinkedList();
        this.formatter = NumberFormat.getInstance();
        this.formatter.setMaximumFractionDigits(2);
        this.addMouseListener(new PanHandler());
        this.addMouseMotionListener(new PanMotionHandler());
        this.addMouseWheelListener(new ZoomHandler());
    }

    public void addEquation(AbstractEquation equation, Color color) {
        if (equation == null) return;
        if (this.equations.contains((Object) equation)) return;
        equation.addPropertyChangeListener(this);
        this.equations.add(new DrawableEquation(equation, color));
        this.repaint();
    }

    public void removeEquation(AbstractEquation equation) {
        if (equation == null) return;
        DrawableEquation toRemove = null;
        for (DrawableEquation drawable : this.equations) {
            if (drawable.getEquation() != equation) continue;
            toRemove = drawable;
            break;
        }
        if (toRemove == null) return;
        equation.removePropertyChangeListener((PropertyChangeListener) this);
        this.equations.remove((Object) toRemove);
        this.repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 300);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        this.repaint();
    }

    private double yPositionToPixel(double position) {
        double height = this.getHeight();
        return height - (position - this.minY) * height / (this.maxY - this.minY);
    }

    private double xPositionToPixel(double position) {
        return (position - this.minX) * (double) this.getWidth() / (this.maxX - this.minX);
    }

    private double xPixelToPosition(double pixel) {
        double axisV = this.xPositionToPixel(this.originX);
        return (pixel - axisV) * (this.maxX - this.minX) / (double) this.getWidth();
    }

    private double yPixelToPosition(double pixel) {
        double axisH = this.yPositionToPixel(this.originY);
        return ((double) this.getHeight() - pixel - axisH) * (this.maxY - this.minY) / (double) this.getHeight();
    }

    protected void paintComponent(Graphics g) {
        if (!this.isVisible()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g;
        this.setupGraphics(g2);
        this.drawBackground(g2);
        this.drawGrid(g2);
        this.drawAxis(g2);
        this.drawEquations(g2);
    }

    private void drawEquations(Graphics2D g2) {
        for (DrawableEquation drawable : this.equations) {
            g2.setColor(drawable.getColor());
            this.drawEquation(g2, drawable.getEquation());
        }
    }

    private void drawEquation(Graphics2D g2, AbstractEquation equation) {
        float x = 0.0f;
        float y = (float) this.yPositionToPixel(equation.compute(this.xPixelToPosition(0.0)));
        GeneralPath path = new GeneralPath();
        path.moveTo(x, y);
        x = 0.0f;
        while (x < (float) this.getWidth()) {
            double position = this.xPixelToPosition((double) x);
            y = (float) this.yPositionToPixel(equation.compute(position));
            path.lineTo(x, y);
            x += 1.0f;
        }
        g2.draw(path);
    }

    private void drawGrid(Graphics2D g2) {
        Stroke stroke = g2.getStroke();
        this.drawVerticalGrid(g2);
        this.drawHorizontalGrid(g2);
        g2.setStroke(stroke);
    }

    private void drawHorizontalGrid(Graphics2D g2) {
        int i;
        int position;
        int position2;
        double minorSpacing = this.majorY / (double) this.minorY;
        double axisV = this.xPositionToPixel(this.originX);
        BasicStroke gridStroke = new BasicStroke(1.0f);
        BasicStroke axisStroke = new BasicStroke(1.2f);
        double y = this.originY + this.majorY;
        while (y < this.maxY + this.majorY) {
            g2.setStroke(gridStroke);
            g2.setColor(COLOR_MINOR_GRID);
            i = 0;
            while (i < this.minorY) {
                position2 = (int) this.yPositionToPixel(y - (double) i * minorSpacing);
                g2.drawLine(0, position2, this.getWidth(), position2);
                ++i;
            }
            position = (int) this.yPositionToPixel(y);
            g2.setColor(COLOR_MAJOR_GRID);
            g2.drawLine(0, position, this.getWidth(), position);
            g2.setStroke(axisStroke);
            g2.setColor(COLOR_AXIS);
            g2.drawLine((int) axisV - 3, position, (int) axisV + 3, position);
            g2.drawString(this.formatter.format(y), (int) axisV + 5, position);
            y += this.majorY;
        }
        y = this.originY - this.majorY;
        while (y > this.minY - this.majorY) {
            g2.setStroke(gridStroke);
            g2.setColor(COLOR_MINOR_GRID);
            i = 0;
            while (i < this.minorY) {
                position2 = (int) this.yPositionToPixel(y + (double) i * minorSpacing);
                g2.drawLine(0, position2, this.getWidth(), position2);
                ++i;
            }
            position = (int) this.yPositionToPixel(y);
            g2.setColor(COLOR_MAJOR_GRID);
            g2.drawLine(0, position, this.getWidth(), position);
            g2.setStroke(axisStroke);
            g2.setColor(COLOR_AXIS);
            g2.drawLine((int) axisV - 3, position, (int) axisV + 3, position);
            g2.drawString(this.formatter.format(y), (int) axisV + 5, position);
            y -= this.majorY;
        }
    }

    private void drawVerticalGrid(Graphics2D g2) {
        int i;
        int position;
        int position2;
        double minorSpacing = this.majorX / (double) this.minorX;
        double axisH = this.yPositionToPixel(this.originY);
        BasicStroke gridStroke = new BasicStroke(1.0f);
        BasicStroke axisStroke = new BasicStroke(1.2f);
        FontMetrics metrics = g2.getFontMetrics();
        double x = this.originX + this.majorX;
        while (x < this.maxX + this.majorX) {
            g2.setStroke(gridStroke);
            g2.setColor(COLOR_MINOR_GRID);
            i = 0;
            while (i < this.minorX) {
                position2 = (int) this.xPositionToPixel(x - (double) i * minorSpacing);
                g2.drawLine(position2, 0, position2, this.getHeight());
                ++i;
            }
            position = (int) this.xPositionToPixel(x);
            g2.setColor(COLOR_MAJOR_GRID);
            g2.drawLine(position, 0, position, this.getHeight());
            g2.setStroke(axisStroke);
            g2.setColor(COLOR_AXIS);
            g2.drawLine(position, (int) axisH - 3, position, (int) axisH + 3);
            g2.drawString(this.formatter.format(x), position, (int) axisH + metrics.getHeight());
            x += this.majorX;
        }
        x = this.originX - this.majorX;
        while (x > this.minX - this.majorX) {
            g2.setStroke(gridStroke);
            g2.setColor(COLOR_MINOR_GRID);
            i = 0;
            while (i < this.minorX) {
                position2 = (int) this.xPositionToPixel(x + (double) i * minorSpacing);
                g2.drawLine(position2, 0, position2, this.getHeight());
                ++i;
            }
            position = (int) this.xPositionToPixel(x);
            g2.setColor(COLOR_MAJOR_GRID);
            g2.drawLine(position, 0, position, this.getHeight());
            g2.setStroke(axisStroke);
            g2.setColor(COLOR_AXIS);
            g2.drawLine(position, (int) axisH - 3, position, (int) axisH + 3);
            g2.drawString(this.formatter.format(x), position, (int) axisH + metrics.getHeight());
            x -= this.majorX;
        }
    }

    private void drawAxis(Graphics2D g2) {
        double axisH = this.yPositionToPixel(this.originY);
        double axisV = this.xPositionToPixel(this.originX);
        g2.setColor(COLOR_AXIS);
        Stroke stroke = g2.getStroke();
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawLine(0, (int) axisH, this.getWidth(), (int) axisH);
        g2.drawLine((int) axisV, 0, (int) axisV, this.getHeight());
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(this.formatter.format(0.0), (int) axisV + 5, (int) axisH + metrics.getHeight());
        g2.setStroke(stroke);
    }

    private void setupGraphics(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private void drawBackground(Graphics2D g2) {
        g2.setColor(COLOR_BACKGROUND);
        g2.fill(g2.getClipBounds());
    }

    static /* synthetic */ double access$0(EquationDisplay equationDisplay) {
        return equationDisplay.maxX;
    }

    static /* synthetic */ double access$1(EquationDisplay equationDisplay) {
        return equationDisplay.minX;
    }

    static /* synthetic */ double access$2(EquationDisplay equationDisplay) {
        return equationDisplay.maxY;
    }

    static /* synthetic */ double access$3(EquationDisplay equationDisplay) {
        return equationDisplay.minY;
    }

    static /* synthetic */ void access$4(EquationDisplay equationDisplay, double d) {
        equationDisplay.minX = d;
    }

    static /* synthetic */ void access$5(EquationDisplay equationDisplay, double d) {
        equationDisplay.maxX = d;
    }

    static /* synthetic */ void access$6(EquationDisplay equationDisplay, double d) {
        equationDisplay.minY = d;
    }

    static /* synthetic */ void access$7(EquationDisplay equationDisplay, double d) {
        equationDisplay.maxY = d;
    }

    static /* synthetic */ void access$8(EquationDisplay equationDisplay, Point point) {
        equationDisplay.dragStart = point;
    }

    static /* synthetic */ double access$9(EquationDisplay equationDisplay, double d) {
        return equationDisplay.xPixelToPosition(d);
    }

    static /* synthetic */ Point access$10(EquationDisplay equationDisplay) {
        return equationDisplay.dragStart;
    }

    static /* synthetic */ double access$11(EquationDisplay equationDisplay, double d) {
        return equationDisplay.yPixelToPosition(d);
    }

    private class DrawableEquation {
        private AbstractEquation equation;
        private Color color;

        DrawableEquation(AbstractEquation equation, Color color) {
            this.equation = equation;
            this.color = color;
        }

        AbstractEquation getEquation() {
            return this.equation;
        }

        Color getColor() {
            return this.color;
        }
    }

    private class PanHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            dragStart = e.getPoint();
        }
    }

    private class PanMotionHandler extends MouseMotionAdapter {
        @Override
        public void mouseDragged(MouseEvent e) {
            Point dragEnd = e.getPoint();
            double distance = xPixelToPosition(dragEnd.getX()) - xPixelToPosition(dragStart.getX());
            minX = minX - distance;
            maxX = maxX - distance;
            distance = yPixelToPosition(dragEnd.getY()) - yPixelToPosition(dragStart.getY());
            minY = minY - distance;
            maxY = maxY - distance;
            repaint();
            dragStart = dragEnd;
        }
    }

    private class ZoomHandler implements MouseWheelListener {
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            double distanceX = maxX - minX;
            double distanceY = maxY - minY;
            double cursorX = minX + distanceX / 2.0;
            double cursorY = minY + distanceY / 2.0;
            int rotation = e.getWheelRotation();
            if (rotation < 0) {
                distanceX /= 1.100000023841858;
                distanceY /= 1.100000023841858;
            } else {
                distanceX *= 1.100000023841858;
                distanceY *= 1.100000023841858;
            }
            minX = cursorX - distanceX / 2.0;
            maxX = cursorX + distanceX / 2.0;
            minY = cursorY - distanceY / 2.0;
            maxY = cursorY + distanceY / 2.0;
            repaint();
        }
    }
}
