package org.progx.twinkle.setup;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.HeadlessException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.progx.twinkle.equation.AnimationEquation;

public class AnimationSetupFrame extends JFrame {
    private AnimationEquation gauss;

    public AnimationSetupFrame(AnimationEquation gauss) throws HeadlessException {
        super("Twinkle - Setup");
        this.gauss = gauss;

        add(buildHeader(), BorderLayout.NORTH);
        add(buildControlPanel(), BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private Component buildHeader() {
        ImageIcon icon = new ImageIcon(getClass().getResource("images/simulator.png"));
        HeaderPanel header = new HeaderPanel(icon,
                                             "Animation Setup",
                                             "Fine-tune animation curve using the controls on the right.",
                                             "The reading of the parameter sigma is to be used in Twinkle.");
        return header;
    }

    private Component buildControlPanel() {
        return new EquationsControlPanel(gauss);
    }
    
    public static void main(String[] args) {
        AnimationEquation gauss = new AnimationEquation(3.6, -1.0);
        AnimationSetupFrame frame = new AnimationSetupFrame(gauss);
        frame.setVisible(true);
    }
}
