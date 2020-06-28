package org.progx.math.simulator;

import org.progx.math.equation.AbstractEquation;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class AbstractSimulator
        extends JComponent
        implements PropertyChangeListener {
    protected AbstractEquation equation;
    protected double time;
    protected int timeScale;

    public AbstractSimulator(AbstractEquation equation) {
        this.equation = equation;
        this.equation.addPropertyChangeListener((PropertyChangeListener) this);
        this.time = 0.0;
        this.timeScale = 1;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        this.repaint();
    }

    public void setTime(double time) {
        this.time = time;
        this.repaint();
    }

    public double getTime() {
        return this.time;
    }

    public void setTimeScale(int timeScale) {
        this.timeScale = timeScale;
        this.repaint();
    }

    public int getTimeScale() {
        return this.timeScale;
    }
}
