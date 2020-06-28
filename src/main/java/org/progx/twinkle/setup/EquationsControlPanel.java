package org.progx.twinkle.setup;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.progx.math.equation.EquationDisplay;
import org.progx.twinkle.equation.AnimationEquation;

public class EquationsControlPanel extends JPanel implements PropertyChangeListener {
    private EquationDisplay display;

    private AnimationEquation gauss;

    private JLabel sigmaLabel;
    private JLabel phaseLabel;

    private JPanel debugPanel;
    
    private int linesCount = 0;
    
    EquationsControlPanel(AnimationEquation gauss) {
        super(new BorderLayout());
        
        this.gauss = gauss;
        this.gauss.addPropertyChangeListener(this);

        add(buildDebugControls(), BorderLayout.EAST);
        add(buildEquationDisplay(), BorderLayout.CENTER);
    }
    
    private Container buildEquationDisplay() {
        JPanel panel = new JPanel(new BorderLayout());
        
        display = new EquationDisplay(0.0, 0.0,
                                      -1.1, 1.1, -1.1, 1.1,
                                      0.5, 5,
                                      0.5, 4);
        display.addEquation(gauss, new Color(0.0f, 0.7f, 0.0f, 0.7f));

        panel.add(display, BorderLayout.CENTER);
        
        return panel;
    }

    private Component buildDebugControls() {
        debugPanel = new JPanel(new GridBagLayout());

        JSlider slider;
        
        addEmptySpace(debugPanel, 6);
        addSeparator(debugPanel, "Parameters");

        slider = addDebugSlider(debugPanel, "Sigma:", 1, 500, (int) (gauss.getSigma() * 10.0));
        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                final int value = ((JSlider) e.getSource()).getValue();
                gauss.setSigma(value / 10.0);
            }
        });
        
        slider = addDebugSlider(debugPanel, "Phase:", -250, 250, (int) (gauss.getPhase() * 100.0));
        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                final int value = ((JSlider) e.getSource()).getValue();
                gauss.setPhase(value / 100.0);
            }
        });
        
        sigmaLabel = addDebugLabel(debugPanel, "Sigma:", Double.toString(gauss.getSigma()));
        phaseLabel = addDebugLabel(debugPanel, "Phase:", Double.toString(gauss.getPhase()));

        addEmptySpace(debugPanel, 12);
        
        debugPanel.add(Box.createVerticalGlue(),
                  new GridBagConstraints(0, linesCount++,
                                         2, 1,
                                         1.0, 1.0,
                                         GridBagConstraints.LINE_START,
                                         GridBagConstraints.NONE, 
                                         new Insets(0, 0, 0, 0),
                                         0, 0));
        
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(new JSeparator(JSeparator.VERTICAL), BorderLayout.WEST);
        wrapper.add(debugPanel);
        return wrapper;
    }

    private void addEmptySpace(JPanel panel, int size) {
        panel.add(Box.createVerticalStrut(size),
                   new GridBagConstraints(0, linesCount++,
                                          2, 1,
                                          1.0, 0.0,
                                          GridBagConstraints.CENTER,
                                          GridBagConstraints.VERTICAL, 
                                          new Insets(6, 0, 0, 0),
                                          0, 0));
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String name = evt.getPropertyName();
        if (AnimationEquation.PROPERTY_SIGMA.equals(name)) {
            sigmaLabel.setText(evt.getNewValue().toString());
        } else if (AnimationEquation.PROPERTY_PHASE.equals(name)) {
            phaseLabel.setText(evt.getNewValue().toString());
        }
    }
    
    private void addSeparator(JPanel panel, String label) {
        JPanel innerPanel = new JPanel(new GridBagLayout());
        innerPanel.add(new JLabel(label),
                  new GridBagConstraints(0, 0,
                                         1, 1,
                                         0.0, 0.0,
                                         GridBagConstraints.LINE_START,
                                         GridBagConstraints.NONE, 
                                         new Insets(0, 0, 0, 0),
                                         0, 0));
        innerPanel.add(new JSeparator(),
                  new GridBagConstraints(1, 0,
                                         1, 1,
                                         0.9, 0.0,
                                         GridBagConstraints.LINE_START,
                                         GridBagConstraints.HORIZONTAL, 
                                         new Insets(0, 6, 0, 6),
                                         0, 0));
        panel.add(innerPanel,
                  new GridBagConstraints(0, linesCount++,
                                         2, 1,
                                         1.0, 0.0,
                                         GridBagConstraints.LINE_START,
                                         GridBagConstraints.HORIZONTAL, 
                                         new Insets(6, 6, 6, 0),
                                         0, 0));
    }

    private JLabel addDebugLabel(JPanel panel, String label, String value) {
        JLabel labelComponent = new JLabel(label);
        panel.add(labelComponent,
                  new GridBagConstraints(0, linesCount,
                                         1, 1,
                                         0.5, 0.0,
                                         GridBagConstraints.LINE_END,
                                         GridBagConstraints.NONE, 
                                         new Insets(0, 6, 0, 0),
                                         0, 0));
        labelComponent = new JLabel(value);
        panel.add(labelComponent,
                  new GridBagConstraints(1, linesCount++,
                                         1, 1,
                                         0.5, 0.0,
                                         GridBagConstraints.LINE_START,
                                         GridBagConstraints.NONE, 
                                         new Insets(0, 6, 0, 0),
                                         0, 0));
        return labelComponent;
    }
    
    private JSlider addDebugSlider(JPanel panel, String label,
                                    int min, int max, int value) {
        JSlider slider;
        panel.add(new JLabel(label),
                  new GridBagConstraints(0, linesCount++,
                                         2, 1,
                                         1.0, 0.0,
                                         GridBagConstraints.LINE_START,
                                         GridBagConstraints.NONE, 
                                         new Insets(0, 6, 0, 0),
                                         0, 0));
        panel.add(slider = new JSlider(min, max, value),
                  new GridBagConstraints(0, linesCount++,
                                         2, 1,
                                         0.0, 0.0,
                                         GridBagConstraints.LINE_START,
                                         GridBagConstraints.HORIZONTAL, 
                                         new Insets(0, 6, 0, 6),
                                         0, 0));
        return slider;
    }
}
