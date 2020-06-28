package org.progx.twinkle.setup;

import org.progx.math.equation.AbstractEquation;
import org.progx.math.equation.EquationDisplay;
import org.progx.twinkle.equation.AnimationEquation;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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
        this.gauss.addPropertyChangeListener((PropertyChangeListener) this);
        this.add(this.buildDebugControls(), (Object) "East");
        this.add((Component) this.buildEquationDisplay(), (Object) "Center");
    }

    private Container buildEquationDisplay() {
        JPanel panel = new JPanel(new BorderLayout());
        this.display = new EquationDisplay(0.0, 0.0, -1.1, 1.1, -1.1, 1.1, 0.5, 5, 0.5, 4);
        this.display.addEquation((AbstractEquation) this.gauss, new Color(0.0f, 0.7f, 0.0f, 0.7f));
        panel.add((Component) this.display, "Center");
        return panel;
    }

    private Component buildDebugControls() {
        this.debugPanel = new JPanel(new GridBagLayout());
        this.addEmptySpace(this.debugPanel, 6);
        this.addSeparator(this.debugPanel, "Parameters");
        JSlider slider = this.addDebugSlider(this.debugPanel, "Sigma:", 1, 500, (int) (this.gauss.getSigma() * 10.0));
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int value = ((JSlider) e.getSource()).getValue();
                gauss.setSigma((double) value / 10.0);
            }
        });
        slider = this.addDebugSlider(this.debugPanel, "Phase:", -250, 250, (int) (this.gauss.getPhase() * 100.0));
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int value = ((JSlider) e.getSource()).getValue();
                gauss.setPhase((double) value / 100.0);
            }
        });
        this.sigmaLabel = this.addDebugLabel(this.debugPanel, "Sigma:", Double.toString(this.gauss.getSigma()));
        this.phaseLabel = this.addDebugLabel(this.debugPanel, "Phase:", Double.toString(this.gauss.getPhase()));
        this.addEmptySpace(this.debugPanel, 12);
        this.debugPanel.add(Box.createVerticalGlue(), new GridBagConstraints(0, this.linesCount++, 2, 1, 1.0, 1.0, 21, 0, new Insets(0, 0, 0, 0), 0, 0));
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add((Component) new JSeparator(1), "West");
        wrapper.add(this.debugPanel);
        return wrapper;
    }

    private void addEmptySpace(JPanel panel, int size) {
        panel.add(Box.createVerticalStrut(size), new GridBagConstraints(0, this.linesCount++, 2, 1, 1.0, 0.0, 10, 3, new Insets(6, 0, 0, 0), 0, 0));
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String name = evt.getPropertyName();
        if ("sigma".equals(name)) {
            this.sigmaLabel.setText(evt.getNewValue().toString());
            return;
        }
        if (!"phase".equals(name)) return;
        this.phaseLabel.setText(evt.getNewValue().toString());
    }

    private void addSeparator(JPanel panel, String label) {
        JPanel innerPanel = new JPanel(new GridBagLayout());
        innerPanel.add((Component) new JLabel(label), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 21, 0, new Insets(0, 0, 0, 0), 0, 0));
        innerPanel.add((Component) new JSeparator(), new GridBagConstraints(1, 0, 1, 1, 0.9, 0.0, 21, 2, new Insets(0, 6, 0, 6), 0, 0));
        panel.add((Component) innerPanel, new GridBagConstraints(0, this.linesCount++, 2, 1, 1.0, 0.0, 21, 2, new Insets(6, 6, 6, 0), 0, 0));
    }

    private JLabel addDebugLabel(JPanel panel, String label, String value) {
        JLabel labelComponent = new JLabel(label);
        panel.add((Component) labelComponent, new GridBagConstraints(0, this.linesCount, 1, 1, 0.5, 0.0, 22, 0, new Insets(0, 6, 0, 0), 0, 0));
        labelComponent = new JLabel(value);
        panel.add((Component) labelComponent, new GridBagConstraints(1, this.linesCount++, 1, 1, 0.5, 0.0, 21, 0, new Insets(0, 6, 0, 0), 0, 0));
        return labelComponent;
    }

    private JSlider addDebugSlider(JPanel panel, String label, int min, int max, int value) {
        panel.add((Component) new JLabel(label), new GridBagConstraints(0, this.linesCount++, 2, 1, 1.0, 0.0, 21, 0, new Insets(0, 6, 0, 0), 0, 0));
        JSlider slider = new JSlider(min, max, value);
        panel.add((Component) slider, new GridBagConstraints(0, this.linesCount++, 2, 1, 0.0, 0.0, 21, 2, new Insets(0, 6, 0, 6), 0, 0));
        return slider;
    }

    static /* synthetic */ AnimationEquation access$0(EquationsControlPanel equationsControlPanel) {
        return equationsControlPanel.gauss;
    }
}
