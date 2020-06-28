package org.progx.twinkle;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.*;

import org.progx.twinkle.ui.PictureViewer;

public class Twinkle extends JFrame {
    private PictureViewer viewer = new PictureViewer();
    
    public Twinkle() {
        super("Twinkle Photo Viewer");
        
        add(BorderLayout.CENTER, viewer);
        pack();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }
    
    private void loadPictures() {
        Thread loader = new Thread(new PicturesLoader(), "Pictures Loader");
        loader.start();
    }
    
    private final class PicturesLoader implements Runnable {
        public void run() {
            Properties props = new Properties();
        
            try {
                props.load(getClass().getResourceAsStream("images/photos.properties"));
            } catch (IOException e) {
                return;
            }
            
            String value = props.getProperty("photos.count");
            int count = Integer.parseInt(value);
            
            for (int i = 0; i < count; i++) {
                String name = props.getProperty("photo." + i + ".name");
                String path = props.getProperty("photo." + i + ".path");
                
                try {
                    viewer.addPicture(name,
                                      ImageIO.read(getClass().getResourceAsStream(path)));
                } catch (IOException e) {
                }
            }                
        }
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                Twinkle f = new Twinkle();
                f.setVisible(true);
                f.loadPictures();
            }
        });
    }
}
