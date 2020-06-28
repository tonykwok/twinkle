package org.progx.twinkle.setup;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class HeaderPanel
        extends JPanel {
    private ImageIcon icon;

    public HeaderPanel(ImageIcon icon, String title, String help1, String help2) {
        super(new BorderLayout());
        this.icon = icon;
        JPanel titlesPanel = new JPanel(new GridLayout(3, 1));
        titlesPanel.setOpaque(false);
        titlesPanel.setBorder(new EmptyBorder(12, 0, 12, 0));
        JLabel headerTitle = new JLabel(title);
        Font police = headerTitle.getFont().deriveFont(1);
        headerTitle.setFont(police);
        headerTitle.setBorder(new EmptyBorder(0, 12, 0, 0));
        titlesPanel.add(headerTitle);
        JLabel message = new JLabel(help1);
        titlesPanel.add(message);
        police = headerTitle.getFont().deriveFont(0);
        message.setFont(police);
        message.setBorder(new EmptyBorder(0, 24, 0, 0));
        message = new JLabel(help2);
        titlesPanel.add(message);
        police = headerTitle.getFont().deriveFont(0);
        message.setFont(police);
        message.setBorder(new EmptyBorder(0, 24, 0, 0));
        message = new JLabel(this.icon);
        message.setBorder(new EmptyBorder(0, 0, 0, 12));
        this.add("West", (Component) titlesPanel);
        this.add("East", (Component) message);
        this.add("South", (Component) new JSeparator(0));
        this.setPreferredSize(new Dimension(500, this.icon.getIconHeight() + 24));
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!this.isOpaque()) {
            return;
        }
        Color control = UIManager.getColor("control");
        int width = this.getWidth();
        int height = this.getHeight();
        Graphics2D g2 = (Graphics2D) g;
        Paint storedPaint = g2.getPaint();
        g2.setPaint(new GradientPaint(this.icon.getIconWidth(), 0.0f, Color.white, width, 0.0f, control));
        g2.fillRect(0, 0, width, height);
        g2.setPaint(storedPaint);
    }
}
