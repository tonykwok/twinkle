package org.progx.twinkle.setup;

import org.progx.twinkle.equation.AnimationEquation;

import javax.swing.*;
import java.awt.*;

public class AnimationSetupFrame extends JFrame {

    private AnimationEquation gauss;

    public AnimationSetupFrame(AnimationEquation gauss) throws HeadlessException {
        super("Twinkle - Setup");
        this.gauss = gauss;
        this.add(buildHeader(), "North");
        this.add(buildControlPanel(), "Center");
        this.pack();
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private Component buildHeader() {
        ImageIcon icon = new ImageIcon(this.getClass().getResource("images/simulator.png"));
        return new HeaderPanel(icon, "Animation Setup", "Fine-tune animation curve using the controls on the right.", "The reading of the parameter sigma is to be used in Twinkle.");
    }

    private Component buildControlPanel() {
        return new EquationsControlPanel(this.gauss);
    }

    public static void main(String[] args) {
        AnimationEquation gauss = new AnimationEquation(3.6, -1.0);
        AnimationSetupFrame frame = new AnimationSetupFrame(gauss);
        frame.setVisible(true);
    }
}
