package org.progx.jogl;

import java.awt.Graphics;

import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.awt.GLJPanel;

/**
 * @author campbelc
 */
public class CompositeGLPanel extends GLJPanel implements GLEventListener {
    private static GLU glu = new GLU();

    private boolean hasDepth;

    public CompositeGLPanel(boolean isOpaque, boolean hasDepth) {
        super(getCaps(isOpaque));
        setOpaque(isOpaque);
        this.hasDepth = hasDepth;
        addGLEventListener(this);
    }

    private static GLCapabilities getCaps(boolean opaque) {
        //getting the capabilities object of GL2 profile
        final GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities caps = new GLCapabilities(profile);

        if (!opaque) {
            caps.setAlphaBits(8);
        }

        return caps;
    }

    @Override
    public void paintComponent(Graphics g) {
        render2DBackground(g);
        super.paintComponent(g);
        render2DForeground(g);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        if (hasDepth) {
            gl.glEnable(GL.GL_DEPTH_TEST);
        }
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    }

    protected void render2DBackground(Graphics g) {
    }

    protected void render3DScene(GL2 gl, GLU glu) {
    }

    protected void render2DForeground(Graphics g) {
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        int clearBits = 0;
        if (hasDepth) {
            clearBits |= GL.GL_DEPTH_BUFFER_BIT;
        }
        if (!shouldPreserveColorBufferIfTranslucent()) {
            clearBits |= GL.GL_COLOR_BUFFER_BIT;
        }
        if (clearBits != 0) {
            gl.glClear(clearBits);
        }
        render3DScene(gl, glu);
    }

    @Override
    public void reshape(GLAutoDrawable drawable,
                        int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        if (hasDepth) {
            double aspectRatio = (double) width / (double) height;
            glu.gluPerspective(45.0, aspectRatio, 1.0, 400.0);
        } else {
            gl.glOrtho(0.0, width, height, 0.0, -100.0, 100.0);
        }

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
    }
}
