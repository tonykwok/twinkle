package org.progx.jogl.rendering;

import com.jogamp.opengl.GL2;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Point3i;
import org.jogamp.vecmath.Vector3f;
import org.progx.jogl.GLUtilities;

public class Billboard extends Renderable {
    private Renderable item;

    public Billboard(Renderable item) {
        this.item = item;
        setPosition(0.0f, 0.0f, 0.0f);
        setScale(1.0f, 1.0f, 1.0f);
        setRotation(0, 0, 0);
    }

    @Override
    public void init(GL2 gl) {
        item.init(gl);
    }

    public Renderable getItem() {
        return item;
    }

    public void setItem(Renderable item) {
        this.item = item;
    }

    @Override
    public void render(GL2 gl, boolean antiAliased) {
        Vector3f camPos = new Vector3f();
        Vector3f camUp = new Vector3f();
        GLUtilities.getCameraVectors(gl, camPos, camUp);
        gl.glPushMatrix();
        GLUtilities.renderBillboard(gl, camPos, camUp, item, antiAliased);
        gl.glPopMatrix();
    }

    @Override
    public Point3f getPosition() {
        return item.getPosition();
    }

    @Override
    public void setPosition(Point3f position) {
        if (item == null) {
            return;
        }
        item.setPosition(position);
    }

    @Override
    public void setPosition(float x, float y, float z) {
        if (item == null) {
            return;
        }
        item.setPosition(x, y, z);
    }

    @Override
    public void setPosition(float[] coordinates) {
        if (item == null) {
            return;
        }
        item.setPosition(coordinates);
    }

    @Override
    public Point3i getRotation() {
        return new Point3i(0, 0, 0);
    }

    @Override
    public Point3f getScale() {
        return item.getScale();
    }

    @Override
    public void setRotation(int x, int y, int z) {
    }

    @Override
    public void setRotation(int[] coordinates) {
    }

    @Override
    public void setRotation(Point3i rot) {
    }

    @Override
    public void setScale(float x, float y, float z) {
        if (item == null) {
            return;
        }
        item.setScale(x, y, z);
    }

    @Override
    public void setScale(float[] coordinates) {
        if (item == null) {
            return;
        }
        item.setScale(coordinates);
    }

    @Override
    public void setScale(Point3f scale) {
        if (item == null) {
            return;
        }
        item.setScale(scale);
    }
}
