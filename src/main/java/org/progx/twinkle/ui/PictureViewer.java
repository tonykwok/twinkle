package org.progx.twinkle.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.glu.GLU;
import org.jdesktop.swingx.util.ShadowFactory;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Point3i;
import org.progx.jogl.CompositeGLPanel;
import org.progx.jogl.GLUtilities;
import org.progx.jogl.rendering.ReflectedQuad;
import org.progx.jogl.rendering.Renderable;
import org.progx.jogl.rendering.RenderableFactory;
import org.progx.math.equation.Equation;
import org.progx.twinkle.Debug;
import org.progx.twinkle.equation.AnimationEquation;

public class PictureViewer extends CompositeGLPanel {
    public static final String KEY_ACTION_NEXT_PICTURE = "next";
    public static final String KEY_ACTION_PREVIOUS_PICTURE = "previous";
    public static final String KEY_ACTION_SHOW_PICTURE = "show";
    
    private static boolean envAntiAliasing = false;
    static {
        envAntiAliasing = System.getProperty("twinkle.aa") != null;
    }
    
    private static final float QUAD_WIDTH = 60.0f;

    private static final int THUMB_SPACING = 5;
    private static final int THUMB_WIDTH = 48;
    private static final double SELECTED_THUMB_RATIO = 0.35;
    private static final double SELECTED_THUMB_EXTRA_WIDTH = THUMB_WIDTH * SELECTED_THUMB_RATIO;
    
    private static final int INDEX_LEFT_PICTURE = 0;
    private static final int INDEX_SELECTED_PICTURE = 1;
    private static final int INDEX_NEXT_PICTURE = 2;
    private static final int INDEX_RIGHT_PICTURE = 3;

    private List<Picture> pictures = Collections.synchronizedList(new ArrayList<Picture>());
    private Renderable[] renderables = new Renderable[4];
    
    private Queue<Renderable> initQuadsQueue = new ConcurrentLinkedQueue<Renderable>();
    private Queue<Renderable> disposeQuadsQueue = new ConcurrentLinkedQueue<Renderable>();

    private float camPosX = 0.0f;
    private float camPosY = 0.0f;
    private float camPosZ = 100.0f;
    
    private int picturesStripHeight = 0;
    
    private BufferedImage textImage = null;
    private BufferedImage nextTextImage = null;
    private BufferedImage alphaMask;
    private ShadowFactory shadowFactory = new ShadowFactory(11, 1.0f, Color.BLACK);
    private Font textFont;
    private float textAlpha = 1.0f;
    private double animFactor = 0.0;
    private Color grayColor = new Color(0xE1E1E1);

    private boolean next;
    private int selectedPicture = -1;
    private int nextPicture = -1;
    
    private boolean pictureIsShowing = false;
    private Equation curve = new AnimationEquation(2.8, -0.98);//3.6, -1.0);
    private Timer animator;
    
    private boolean antiAliasing = envAntiAliasing;
    
    public PictureViewer() {
        super(false, true);
        setPreferredSize(new Dimension(640, 480));
        
        addMouseWheelListener(new MouseWheelDriver());
        
        setFocusable(true);
        registerActions();

        textFont = getFont().deriveFont(Font.BOLD, 32.0f);
        alphaMask = createGradientMask(THUMB_WIDTH);
        
        createButtons();
    }

    public boolean isAntiAliasing() {
        return antiAliasing;
    }

    public void setAntiAliasing(boolean antiAliasing) {
        this.antiAliasing = antiAliasing && envAntiAliasing;
        repaint();
    }

    public void addPicture(String name, BufferedImage image) {
        if (name == null) {
            name = "";
        }

        int size = -1;
        Picture picture = new Picture(name, image);
        
        pictures.add(picture);
        size = pictures.size();

        if (size == 1) {
            initQuadsQueue.add(createQuad(INDEX_SELECTED_PICTURE, 0));
        } else if (size - 1 == selectedPicture + 1) {
            initQuadsQueue.add(createQuad(INDEX_NEXT_PICTURE, 1));
        } else if (size - 1 == nextPicture + 1) {
            initQuadsQueue.add(createQuad(INDEX_RIGHT_PICTURE, 2));
        }
        
        float ratio = picture.getRatio();
        picturesStripHeight = Math.max(picturesStripHeight,
                                      (int) (THUMB_WIDTH + SELECTED_THUMB_EXTRA_WIDTH / ratio));

        getActionMap().get(KEY_ACTION_SHOW_PICTURE).setEnabled(selectedPicture >= 0);
        getActionMap().get(KEY_ACTION_NEXT_PICTURE).setEnabled(selectedPicture < size - 1);
        getActionMap().get(KEY_ACTION_PREVIOUS_PICTURE).setEnabled(selectedPicture > 0);
        
        repaint();
    }
    
    public void showSelectedPicture() {
        if (animator != null && animator.isRunning()) {
            return;
        }
        
        pictureIsShowing = !pictureIsShowing;
        ((ShowPictureAction) getActionMap().get(KEY_ACTION_SHOW_PICTURE)).toggleName();

        animator = new Timer(1000 / 60, new ZoomAnimation());
        animator.start();
    }

    public void nextPicture() {
        int size = -1;
        size = pictures.size() - 1;

        if (selectedPicture < size) {
            showPicture(true);
        }
    }
    
    public void previousPicture() {
        if (selectedPicture > 0) {
            showPicture(false);
        }
    }
    
    @Override
    public void init(GLAutoDrawable drawable) {
        super.init(drawable);
        GL2 gl = drawable.getGL().getGL2();

        initQuads(gl);
    }
    
    private void registerActions() {
        KeyStroke stroke;
        Action action;
        
        InputMap inputMap = getInputMap();
        ActionMap actionMap = getActionMap();

        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);
        inputMap.put(stroke, KEY_ACTION_NEXT_PICTURE);
        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
        inputMap.put(stroke, KEY_ACTION_NEXT_PICTURE);

        action = new NextPictureAction();
        actionMap.put(KEY_ACTION_NEXT_PICTURE, action);
        
        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0);
        inputMap.put(stroke, KEY_ACTION_PREVIOUS_PICTURE);
        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
        inputMap.put(stroke, KEY_ACTION_PREVIOUS_PICTURE);

        action = new PreviousPictureAction();
        actionMap.put(KEY_ACTION_PREVIOUS_PICTURE, action);
        
        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
        inputMap.put(stroke, KEY_ACTION_SHOW_PICTURE);
        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        inputMap.put(stroke, KEY_ACTION_SHOW_PICTURE);

        action = new ShowPictureAction();
        actionMap.put(KEY_ACTION_SHOW_PICTURE, action);
    }
    
    private void createButtons() {
        ControlButton button;
        ControlPanel buttonsPanel = new ControlPanel();
        
        button = new ControlButton(getActionMap().get(KEY_ACTION_PREVIOUS_PICTURE));
        buttonsPanel.add(button);
        button = new ControlButton(getActionMap().get(KEY_ACTION_SHOW_PICTURE));
        buttonsPanel.add(button);
        button = new ControlButton(getActionMap().get(KEY_ACTION_NEXT_PICTURE));
        buttonsPanel.add(button);

        setLayout(new GridBagLayout());
        add(Box.createGlue(), new GridBagConstraints(0, 0,
                                                     2, 1,
                                                     0.0, 1.0,
                                                     GridBagConstraints.LINE_START,
                                                     GridBagConstraints.VERTICAL, 
                                                     new Insets(0, 0, 0, 0),
                                                     0, 0));
        add(buttonsPanel, new GridBagConstraints(0, 1,
                                                 1, 1,
                                                 0.0, 0.0,
                                                 GridBagConstraints.LINE_START,
                                                 GridBagConstraints.NONE, 
                                                 new Insets(0, 13, 13, 0),
                                                 0, 0));
        add(Box.createHorizontalGlue(), new GridBagConstraints(1, 1,
                                                               1, 1,
                                                               1.0, 0.0,
                                                               GridBagConstraints.LINE_START,
                                                               GridBagConstraints.HORIZONTAL, 
                                                               new Insets(0, 0, 0, 0),
                                                               0, 0));
    }
    
    private void showPicture(final boolean next) {
        if (animator != null && animator.isRunning()) {
            return;
        }
        
        if (pictureIsShowing) {
            new Thread(new Runnable() {
                public void run() {
                    showSelectedPicture();
                    while (animator.isRunning()) {
                        Thread.yield();
                    }
                    showPicture(next);
                    while (animator.isRunning()) {
                        Thread.yield();
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            showSelectedPicture();
                        }
                    });
                }
            }).start();
            return;
        }
        
        this.next = next;

        animator = new Timer(1000 / 60, new SlideAnimation(next));
        animator.start();
    }
    
    private Renderable createQuad(int index, int pictureNumber) {
        Picture picture = null;
        picture = pictures.get(pictureNumber);
        
        if (picture == null || index > renderables.length) {
            return null;
        }

        float ratio = picture.getRatio();
        int height = (int) (QUAD_WIDTH / ratio);
        
        Renderable quad = RenderableFactory.createReflectedQuad(0.0f, 0.0f, 0.0f,
                                                                QUAD_WIDTH, height,
                                                                picture.getImage(), null,
                                                                picture.getName());
        renderables[index] = quad;
        
        if (index == INDEX_SELECTED_PICTURE) {
            selectedPicture = pictureNumber;
            
            quad.setPosition(-7.0f, 0.0f, 0.0f);
            quad.setRotation(0, 30, 0);
            
            textImage = generateTextImage(picture);
        } else if (index == INDEX_NEXT_PICTURE) {
            nextPicture = pictureNumber;
            
            quad.setScale(0.5f, 0.5f, 0.5f);
            quad.setPosition(36.0f, -height / 2.0f, 30.0f);
            quad.setRotation(0, -20, 0);
        } else if (index == INDEX_RIGHT_PICTURE) {
            quad.setScale(0.5f, 0.5f, 0.5f);
            quad.setPosition(196.0f, -height / 2.0f, 30.0f);
            quad.setRotation(0, -20, 0);
        } else if (index == INDEX_LEFT_PICTURE) {
            quad.setPosition(-7.0f - QUAD_WIDTH * 2.0f, 0.0f, 0.0f);
            quad.setRotation(0, 30, 0);
        }
        
        return quad;
    }

    private BufferedImage generateTextImage(Picture picture) {
        FontRenderContext context = getFontMetrics(textFont).getFontRenderContext();
        GlyphVector vector = textFont.createGlyphVector(context, picture.getName());
        Rectangle bounds = vector.getPixelBounds(context, 0.0f, 0.0f);
        TextLayout layout = new TextLayout(picture.getName(), textFont, context);
        
        BufferedImage image = new BufferedImage((int) (bounds.getWidth()),
                                                (int) (layout.getAscent() + layout.getDescent() + layout.getLeading()),
                                                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        layout.draw(g2, 0, layout.getAscent());
        g2.dispose();
        
        BufferedImage shadow = shadowFactory.createShadow(image);
        BufferedImage composite = new BufferedImage(shadow.getWidth(),
                                                    shadow.getHeight(),
                                                    BufferedImage.TYPE_INT_ARGB);
        g2 = composite.createGraphics();
        g2.drawImage(shadow, null,
                     -1 - (shadow.getWidth() - image.getWidth()) / 2,
                     2 - (shadow.getHeight() - image.getHeight()) / 2);
        g2.drawImage(image, null, 0, 0);
        g2.dispose();
        
        shadow.flush();
        image.flush();
        
        return composite;
    }

    @Override
    protected void render2DBackground(Graphics g) {
        // NOTE: with antialiasing on the accum buffer creates a black backround
        if (!antiAliasing) {
            float h = getHeight() * 0.55f;

            GradientPaint paint = new GradientPaint(0.0f, h, Color.BLACK,
                                                    0.0f, getHeight(), new Color(0x4C4C4C));
            Graphics2D g2 = (Graphics2D) g;
            Paint oldPaint = g2.getPaint();
            g2.setPaint(paint);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setPaint(oldPaint);
        }
    }

    @Override
    protected void render2DForeground(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        setupForegroundGraphics(g2);

        paintPicturesStrip(g2);
        paintInfo(g2);
    }
    
    private BufferedImage createGradientMask(int width) {
        BufferedImage gradient = new BufferedImage(width, 2,
                                                   BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = gradient.createGraphics();
        GradientPaint painter = new GradientPaint(0.0f, 0.0f,
                                                  new Color(1.0f, 1.0f, 1.0f, 1.0f),
                                                  width / 2.0f, 0.0f,
                                                  new Color(1.0f, 1.0f, 1.0f, 0.0f));
        g.setPaint(painter);
        g.fill(new Rectangle2D.Double(0, 0, width, 2));
        
        g.dispose();

        return gradient;
    }

    private void paintPicturesStrip(Graphics2D g2) {
        Rectangle clip = g2.getClipBounds();
        
        int x = (int) (getWidth() / 2 - (selectedPicture + (next ? 1 : -1) * animFactor) * (THUMB_WIDTH + THUMB_SPACING));
        int y = picturesStripHeight / 2;
        int i = 0;

        int width = (int) (THUMB_WIDTH + SELECTED_THUMB_EXTRA_WIDTH * (1.0 - animFactor));
        int width2 = (int) (THUMB_WIDTH + SELECTED_THUMB_EXTRA_WIDTH * animFactor);
        
        x -= width / 2;
        x -= (width2 - THUMB_WIDTH) / 2;

        Picture[] picturesArray = new Picture[pictures.size()];
        picturesArray = pictures.toArray(picturesArray);
        
        for (Picture picture: picturesArray) {
            int picWidth = THUMB_WIDTH;
            if (i == selectedPicture) {
                picWidth = width;
            } else if ((next && i == nextPicture) ||
                       (!next && i == selectedPicture - 1)) {
                picWidth = width2;
            }

            if (x > clip.x + clip.width) {
                return;
            }

            if (x + picWidth >= clip.x) {
                BufferedImage thumb = picture.getThumbnail(THUMB_WIDTH * 2);
                float ratio = picture.getRatio();
                int height = (int) (picWidth / ratio);
                int y1 = y - height / 2;
    
                g2.setColor(grayColor);
                if (i == selectedPicture) {
                    Graphics2D g2d = (Graphics2D) g2.create();
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                                1.0f - (float) animFactor));
                    g2d.drawRect(x - 1, y1 - 1, picWidth + 1, height + 1);
                } else if ((next && i == nextPicture) ||
                           (!next && i == selectedPicture - 1)) {
                    Graphics2D g2d = (Graphics2D) g2.create();
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                                (float) animFactor));
                    g2d.drawRect(x - 1, y1 - 1, picWidth + 1, height + 1);
                }
                
                g2.drawImage(thumb, x, y1, picWidth, height, null);
                
                if (x < picWidth || x + picWidth > getWidth() - picWidth) {
                    int x1 = 0;
                    
                    Graphics2D g2d = (Graphics2D) g2.create();
                    g2d.setComposite(AlphaComposite.DstOut);
                    
                    if (x + picWidth > getWidth() - picWidth) {
                        double scaleY = (double) height / (double) alphaMask.getHeight();
                        AffineTransform transform = AffineTransform.getScaleInstance(-1.0, scaleY);
                        x1 = getWidth();
                        g2d.translate(x1, y1);
                        g2d.drawImage(alphaMask, transform, null);
                    } else {
                        g2d.drawImage(alphaMask, x1, y1 - 1, picWidth, height + 1, null);
                    }
                }
            }
            
            x += picWidth + THUMB_SPACING;
            i++;
        }
    }

    private void paintInfo(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        
        if (Debug.isDebug()) {
            g2.drawString("X: " + camPosX, 5, 15);
            g2.drawString("Y: " + camPosY, 5, 30);
            g2.drawString("Z: " + camPosZ, 5, 45);
        }
        
        if (textImage != null) {
            Composite composite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, textAlpha));
            g2.drawImage(textImage, null,
                         (int) (getWidth() - textImage.getWidth()) / 2,
                         (int) (getHeight() - textFont.getSize() * 1.7));
            g2.setComposite(composite);
        }
    }
    
    private void setupForegroundGraphics(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    @Override
    protected void render3DScene(final GL2 gl, final GLU glu) {
        initScene(gl);
        initAndDisposeQuads(gl);
        
        Renderable scene = new Renderable() {
            public Point3f getPosition() {
                return null;
            }
        
            public void render(GL2 gl) {
                setupCamera(gl, glu);
                renderItems(gl);
            }
        
            public void init(GL gl) {
            }
        };

        if (antiAliasing) {
            GLUtilities.renderAntiAliased(gl, scene, 4);
        } else {
            scene.render(gl);
        }
    }

    private void initQuads(GL2 gl) {
        for (Renderable item: renderables) {
            if (item != null) {
                item.init(gl);
            }
        }
    }
    
    private void initAndDisposeQuads(final GL2 gl) {
        while (!initQuadsQueue.isEmpty()) {
            Renderable quad = initQuadsQueue.poll();
            if (quad != null) {
                quad.init(gl);
            }
        }
        
        while (!disposeQuadsQueue.isEmpty()) {
            Renderable quad = disposeQuadsQueue.poll();
            if (quad != null) {
                quad.dispose(gl);
            }
        }
    }
    
    private void initScene(GL2 gl) {
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    private void setupCamera(GL2 gl, GLU glu) {
        glu.gluLookAt(camPosX, camPosY, camPosZ,
                      0.0f, 0.0f, 0.0f,
                      0.0f, 1.0f, 0.0f);
        gl.glTranslatef(0.0f, -1.0f, 0.0f);
    }

    private void renderItems(GL2 gl) {
        for (Renderable renderable: renderables) {
            setAndRender(gl, renderable);
        }
    }
    
    private void setAndRender(GL2 gl, Renderable renderable) {
        if (renderable == null) {
            return;
        }
        
        Point3f pos = renderable.getPosition();
        Point3i rot = renderable.getRotation();
        Point3f scale = renderable.getScale();

        gl.glPushMatrix();
        gl.glScalef(scale.x, scale.y, scale.z);
        gl.glTranslatef(pos.x, pos.y, pos.z);
        gl.glRotatef(rot.x, 1.0f, 0.0f, 0.0f);
        gl.glRotatef(rot.y, 0.0f, 1.0f, 0.0f);
        gl.glRotatef(rot.z, 0.0f, 0.0f, 1.0f);
        
        renderable.render(gl);
        gl.glPopMatrix();
    }

    private final class ZoomAnimation implements ActionListener {
        private final int ANIM_DELAY = 400;
        private long start;

        private ZoomAnimation() {
            start = System.currentTimeMillis();
        }

        public void actionPerformed(ActionEvent e) {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed >= ANIM_DELAY) {
                Timer timer = (Timer) e.getSource();
                timer.stop();
            } else {
                double factor = (double) elapsed / (double) ANIM_DELAY;
                animateQuads(curve.compute(factor));
            }
            repaint();
        }

        private void animateQuads(double factor) {
            if (!pictureIsShowing) {
                factor = 1.0 - factor;
            }
            
            Renderable quad = renderables[INDEX_SELECTED_PICTURE];
            Point3f position = quad.getPosition();
            
            quad.setRotation(0, (int) (30.0 * (1.0 - factor)), 0);
            quad.setPosition((float) (-7.0f * (1.0 - factor)),
                             position.y,
                             (float) (30.0 * factor));
      
            quad = renderables[INDEX_NEXT_PICTURE];
            if (quad != null) {
                position = quad.getPosition();
                quad.setPosition(36.0f + (float) (120.0f * factor),
                                 position.y,
                                 position.z);
            }
        }
    }

    private final class SlideAnimation implements ActionListener {
        private final int ANIM_DELAY = 800;
        
        private final boolean next;
        private long start;

        private SlideAnimation(boolean next) {
            this.next = next;
            start =  System.currentTimeMillis();
            
            if (next) {
                if (nextPicture < pictures.size()) {
                    nextTextImage = generateTextImage(pictures.get(nextPicture));
                } else {
                    nextTextImage = null;
                }
            } else {
                if (selectedPicture > 0) {
                    nextTextImage = generateTextImage(pictures.get(selectedPicture - 1));
                } else {
                    nextTextImage = null;
                }
            }
        }

        public void actionPerformed(ActionEvent e) {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed >= ANIM_DELAY) {
                Timer timer = (Timer) e.getSource();
                timer.stop();
                
                if (next) {
                    selectNextPicture();
                } else {
                    selectPreviousPicture();
                }
                
                Action action = getActionMap().get(KEY_ACTION_NEXT_PICTURE);
                action.setEnabled(selectedPicture < pictures.size() - 1);
                
                action = getActionMap().get(KEY_ACTION_PREVIOUS_PICTURE);
                action.setEnabled(selectedPicture > 0 && pictures.size() > 1);
                
                animFactor = 0.0;
            } else {
                double factor = (double) elapsed / (double) ANIM_DELAY;
                double curvedFactor = curve.compute(factor);
                animFactor = curvedFactor;
                
                if (next) {
                    animateQuadsNext(curvedFactor);
                } else {
                    animateQuadsPrevious(1.0 - curvedFactor);
                }
                
                setTextAlpha(elapsed, factor);
            }
            
            repaint();
        }

        private void animateQuadsNext(double factor) {
            Renderable quad = renderables[INDEX_SELECTED_PICTURE];
            Point3f position = quad.getPosition();
            quad.setPosition(-7.0f - QUAD_WIDTH * 2.0f * (float) factor, position.y, position.z);
            
            ReflectedQuad reflected = (ReflectedQuad) renderables[INDEX_NEXT_PICTURE];
            if (reflected != null) {
                float scale = 0.5f + 0.5f * (float) factor;
      
                reflected.setScale(scale, scale, scale);
                reflected.setRotation(0, (int) (-20.0 + 50.0 * factor), 0);
                reflected.setPosition((float) (36.0f - 43.0f * factor),
                                      (float) (-reflected.getHeight() * (1.0f - scale)),
                                      (float) (30.0 * (1.0 - factor)));
            }
            
            quad = renderables[INDEX_RIGHT_PICTURE];
            if (quad != null) {
                position = quad.getPosition();
                quad.setPosition(36.0f + 160.0f * (float) (1.0 - factor), position.y, position.z);
            }
        }
        
        private void animateQuadsPrevious(double factor) {
            ReflectedQuad reflected = (ReflectedQuad) renderables[INDEX_SELECTED_PICTURE];
            float scale = 0.5f + 0.5f * (float) factor;
  
            reflected.setScale(scale, scale, scale);
            reflected.setRotation(0, (int) (-20.0 + 50.0 * factor), 0);
            reflected.setPosition((float) (36.0f - 43.0f * factor),
                                  (float) (-reflected.getHeight() * (1.0f - scale)),
                                  (float) (30.0 * (1.0 - factor)));
            
            Renderable quad = renderables[INDEX_NEXT_PICTURE];
            if (quad != null) {
                Point3f position = quad.getPosition();
                quad.setPosition(36.0f + 160.0f * (float) (1.0 - factor), position.y, position.z);
            }

            quad = renderables[INDEX_LEFT_PICTURE];
            if (quad != null) {
                Point3f position = quad.getPosition();
                quad.setPosition(-7.0f - QUAD_WIDTH * 2.0f * (float) factor, position.y, position.z);
            }
        }

        private void setTextAlpha(long elapsed, double factor) {
            if (elapsed < ANIM_DELAY / 2.0) {
                textAlpha = (float) (1.0 - 2.0 * factor);
            } else {
                textAlpha = (float) ((factor - 0.5) * 2.0);
                if (textAlpha > 1.0f) {
                    textAlpha = 1.0f;
                }
            }
            if (textAlpha < 0.1f) {
                textAlpha = 0.1f;
                textImage = nextTextImage;
            }
        }

        private void selectPreviousPicture() {
            selectedPicture--;
            nextPicture--;

            if (renderables[INDEX_RIGHT_PICTURE] != null) {
                disposeQuadsQueue.add(renderables[INDEX_RIGHT_PICTURE]);
            }
            
            Renderable quad = renderables[INDEX_NEXT_PICTURE];
            if (quad != null) { 
                renderables[INDEX_RIGHT_PICTURE] = quad;
                quad.setScale(0.5f, 0.5f, 0.5f);
                quad.setPosition(196.0f, -((ReflectedQuad) quad).getHeight() / 2.0f, 30.0f);
                quad.setRotation(0, -20, 0);
            }
            
            quad = renderables[INDEX_SELECTED_PICTURE];
            renderables[INDEX_NEXT_PICTURE] = quad;
            
            nextTextImage = generateTextImage(pictures.get(nextPicture));
            
            quad = renderables[INDEX_LEFT_PICTURE];
            renderables[INDEX_SELECTED_PICTURE] = quad;
            
            textImage = generateTextImage(pictures.get(selectedPicture));
            
            if (selectedPicture > 0) {
                initQuadsQueue.add(createQuad(INDEX_LEFT_PICTURE, selectedPicture - 1));
            } else {
                renderables[INDEX_LEFT_PICTURE] = null;
            }
        }

        private void selectNextPicture() {
            selectedPicture++;
            nextPicture++;
            
            if (renderables[INDEX_LEFT_PICTURE] != null) {
                disposeQuadsQueue.add(renderables[INDEX_LEFT_PICTURE]);
            }
            
            Renderable quad = renderables[INDEX_SELECTED_PICTURE];
            renderables[INDEX_LEFT_PICTURE] = quad;
            quad.setPosition(-7.0f - QUAD_WIDTH * 2.0f, 0.0f, 0.0f);
            quad.setRotation(0, 30, 0);
            
            quad = renderables[INDEX_NEXT_PICTURE];
            renderables[INDEX_SELECTED_PICTURE] = quad;
            
            textImage = generateTextImage(pictures.get(selectedPicture));
            
            if (nextPicture < pictures.size()) {
                quad = renderables[INDEX_RIGHT_PICTURE];
                renderables[INDEX_NEXT_PICTURE] = quad;
                nextTextImage = generateTextImage(pictures.get(nextPicture));
            } else {
                renderables[INDEX_NEXT_PICTURE] = null;
            }
            
            if (nextPicture < pictures.size() - 1) {
                initQuadsQueue.add(createQuad(INDEX_RIGHT_PICTURE, nextPicture + 1));
            } else {
                renderables[INDEX_RIGHT_PICTURE] = null;
            }
        }
    }
    
    private final class NextPictureAction extends AbstractAction {
        public NextPictureAction() {
            super("Next");
            ImageIcon nextIconActive = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-next-button.png"));
            ImageIcon nextIconPressed = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-next-button-pressed.png"));
            ImageIcon disabledIcon = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-next-disabled-button.png"));
            
            setEnabled(false);
            
            putValue("disabledIcon", disabledIcon);
            putValue("pressedIcon", nextIconPressed);
            putValue(Action.LARGE_ICON_KEY, nextIconActive);
            putValue(Action.ACTION_COMMAND_KEY, "next");
            putValue(Action.SHORT_DESCRIPTION, "Show next picture");
        }

        public void actionPerformed(ActionEvent e) {
            nextPicture();
        }
    }
    
    private final class PreviousPictureAction extends AbstractAction {
        public PreviousPictureAction() {
            super("Previous");
            ImageIcon previousIconActive = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-previous-button.png"));
            ImageIcon previousIconPressed = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-previous-button-pressed.png"));
            ImageIcon disabledIcon = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-previous-disabled-button.png"));

            setEnabled(false);
            
            putValue("disabledIcon", disabledIcon);
            putValue("pressedIcon", previousIconPressed);
            putValue(Action.LARGE_ICON_KEY, previousIconActive);
            putValue(Action.ACTION_COMMAND_KEY, "previous");
            putValue(Action.SHORT_DESCRIPTION, "Show previous picture");
        }

        public void actionPerformed(ActionEvent e) {
            previousPicture();
        }
    }
    
    private final class ShowPictureAction extends AbstractAction {
        private ImageIcon showIconActive;
        private ImageIcon showIconAll;
        private ImageIcon showIconPressed;
        private ImageIcon showIconAllPressed;
        
        public ShowPictureAction() {
            super(pictureIsShowing ? "Show All" : "Show Picture");
            
            showIconActive = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-show-button.png"));
            showIconPressed = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-show-button-pressed.png"));
            showIconAll = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-show-all-button.png"));
            showIconAllPressed = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-show-all-button-pressed.png"));
            
            ImageIcon disabledIcon = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-show-disabled-button.png"));
            
            setEnabled(false);

            putValue("disabledIcon", disabledIcon);
            putValue("pressedIcon", pictureIsShowing ? showIconAllPressed : showIconPressed);
            putValue(Action.LARGE_ICON_KEY, pictureIsShowing ? showIconAll : showIconActive);
            putValue(Action.ACTION_COMMAND_KEY, "show");
            putValue(Action.SHORT_DESCRIPTION, "Show selected picture");
        }

        public void actionPerformed(ActionEvent e) {
            showSelectedPicture();
        }
        
        public void toggleName() {
            putValue(Action.NAME, pictureIsShowing ? "Show All" : "Show Picture");
            putValue(Action.LARGE_ICON_KEY, pictureIsShowing ? showIconAll : showIconActive);
            putValue("pressedIcon", pictureIsShowing ? showIconAllPressed : showIconPressed);
        }
    }
    
    private final class ControlButton extends JButton implements PropertyChangeListener {
        public ControlButton(Action action) {
            super(action);

            getAction().addPropertyChangeListener(this);
            
            setPressedIcon((Icon) getAction().getValue("pressedIcon"));
            setDisabledIcon((Icon) getAction().getValue("disabledIcon"));
            
            setForeground(grayColor);
            setFocusable(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setMargin(new Insets(0, 0, 0, 0));
            setHideActionText(true);
        }
        
        @Override
        public void setToolTipText(String text) {
        }

        public void propertyChange(PropertyChangeEvent evt) {
            if ("pressedIcon".equals(evt.getPropertyName())) {
                setPressedIcon((Icon) evt.getNewValue());
            }
        }
    }
    
    private final class ControlPanel extends JPanel {
        private BufferedImage background;
        
        public ControlPanel() {
            super(new FlowLayout(FlowLayout.CENTER, 2, 2));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (background == null) {
                createBackground();
            }

            g.drawImage(background, 0, 0, null);
        }

        private void createBackground() {
            background = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = background.createGraphics();
            
            g2.setColor(Color.WHITE);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            
            Insets insets = getInsets();
            RoundRectangle2D rect = new RoundRectangle2D.Double(insets.left, insets.top,
                                                                getWidth() - insets.right - insets.left,
                                                                getHeight() - insets.bottom - insets.top,
                                                                14, 14);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.fill(rect);
            
            g2.dispose();
        }
    }
    
    private final class MouseWheelDriver implements MouseWheelListener {
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.getWheelRotation() > 0) {
                nextPicture();
            } else {
                previousPicture();
            }
        }
    }
}