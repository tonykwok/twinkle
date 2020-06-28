package org.progx.twinkle;

import org.progx.twinkle.ui.PictureViewer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Properties;

public class Twinkle extends JFrame {

    private PictureViewer viewer = new PictureViewer();

    public Twinkle() {
        super("Twinkle Photo Viewer");
        this.add("Center", viewer);
        this.pack();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
    }

    private void loadPictures() {
        Thread loader = new Thread(new PicturesLoader(), "Pictures Loader");
        loader.start();
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                org.progx.twinkle.Twinkle f = new org.progx.twinkle.Twinkle();
                f.setVisible(true);
                f.loadPictures();
            }
        });
    }

    private final class PicturesLoader implements Runnable {

        @Override
        public void run() {
            Properties props = new Properties();
            try {
                props.load(this.getClass().getResourceAsStream("/resources/photos.properties"));
            } catch (IOException e) {
                return;
            }
            String value = props.getProperty("photos.count");
            int count = Integer.parseInt(value);
            int i = 0;
            while (i < count) {
                String name = props.getProperty("photo." + i + ".name");
                String path = props.getProperty("photo." + i + ".path");
                try {
                    viewer.addPicture(name, ImageIO.read(this.getClass().getResourceAsStream(path)));
                } catch (IOException iOException) {
                    // empty catch block
                }
                ++i;
            }
        }
    }
}
