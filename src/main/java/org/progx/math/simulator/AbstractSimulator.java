package org.progx.math.simulator;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;

import org.progx.math.equation.AbstractEquation;

public class AbstractSimulator extends JComponent implements PropertyChangeListener {
    protected AbstractEquation equation;
    protected double time;
    protected int timeScale;    
    
    
    public AbstractSimulator(AbstractEquation equation) {
        this.equation = equation;
        this.equation.addPropertyChangeListener(this);
        this.time = 0.0f;
        this.timeScale = 1;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        repaint();
    }

    public void setTime(double time) {
        this.time = time;
        repaint();
    }
    
    public double getTime() {
        return this.time;
    }
    
    public void setTimeScale(int timeScale) {
        this.timeScale = timeScale;
        repaint();
    }
    
    public int getTimeScale() {
        return this.timeScale;
    }
}
