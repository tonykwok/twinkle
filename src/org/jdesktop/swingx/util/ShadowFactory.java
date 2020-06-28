package org.jdesktop.swingx.util;

import java.awt.*;
import java.awt.image.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;

public class ShadowFactory {
    public static final String KEY_BLUR_QUALITY = "blur_quality";
    public static final String VALUE_BLUR_QUALITY_FAST = "fast";
    public static final String VALUE_BLUR_QUALITY_HIGH = "high";
    public static final String SIZE_CHANGED_PROPERTY = "shadow_size";
    public static final String OPACITY_CHANGED_PROPERTY = "shadow_opacity";
    public static final String COLOR_CHANGED_PROPERTY = "shadow_color";
    private int size = 5;
    private float opacity = 0.5f;
    private Color color = Color.BLACK;
    private HashMap<Object, Object> hints = new HashMap();
    private PropertyChangeSupport changeSupport;

    public ShadowFactory() {
        this(5, 0.5f, Color.BLACK);
    }

    public ShadowFactory(int size, float opacity, Color color) {
        this.hints.put(KEY_BLUR_QUALITY, VALUE_BLUR_QUALITY_FAST);
        this.changeSupport = new PropertyChangeSupport((Object) this);
        this.setSize(size);
        this.setOpacity(opacity);
        this.setColor(color);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.changeSupport.removePropertyChangeListener(listener);
    }

    public void setRenderingHint(Object key, Object value) {
        this.hints.put(key, value);
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color shadowColor) {
        if (shadowColor == null) return;
        Color oldColor = this.color;
        this.color = shadowColor;
        this.changeSupport.firePropertyChange(COLOR_CHANGED_PROPERTY, oldColor, this.color);
    }

    public float getOpacity() {
        return this.opacity;
    }

    public void setOpacity(float shadowOpacity) {
        float oldOpacity = this.opacity;
        this.opacity = (double) shadowOpacity < 0.0 ? 0.0f : (shadowOpacity > 1.0f ? 1.0f : shadowOpacity);
        this.changeSupport.firePropertyChange(OPACITY_CHANGED_PROPERTY, new Float(oldOpacity), new Float(this.opacity));
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int shadowSize) {
        int oldSize = this.size;
        this.size = shadowSize < 0 ? 0 : shadowSize;
        this.changeSupport.firePropertyChange(SIZE_CHANGED_PROPERTY, new Integer(oldSize), new Integer(this.size));
    }

    public BufferedImage createShadow(BufferedImage image) {
        if (this.hints.get(KEY_BLUR_QUALITY) != VALUE_BLUR_QUALITY_HIGH) return this.createShadowFast(image);
        BufferedImage subject = this.prepareImage(image);
        BufferedImage shadow = new BufferedImage(subject.getWidth(), subject.getHeight(), 2);
        BufferedImage shadowMask = this.createShadowMask(subject);
        this.getLinearBlurOp(this.size).filter(shadowMask, shadow);
        return shadow;
    }

    private BufferedImage prepareImage(BufferedImage image) {
        BufferedImage subject = new BufferedImage(image.getWidth() + this.size * 2, image.getHeight() + this.size * 2, 2);
        Graphics2D g2 = subject.createGraphics();
        g2.drawImage(image, null, this.size, this.size);
        g2.dispose();
        return subject;
    }

    private BufferedImage createShadowFast(BufferedImage src) {
        int aSum;
        int a;
        int historyIdx;
        int shadowSize = this.size;
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        int dstWidth = srcWidth + this.size;
        int dstHeight = srcHeight + this.size;
        int left = shadowSize - 1 >> 1;
        int right = shadowSize - left;
        int yStop = dstHeight - right;
        BufferedImage dst = new BufferedImage(dstWidth, dstHeight, 2);
        int shadowRgb = this.color.getRGB() & 16777215;
        int[] aHistory = new int[shadowSize];
        ColorModel srcColorModel = src.getColorModel();
        WritableRaster srcRaster = src.getRaster();
        int[] dstBuffer = ((DataBufferInt) dst.getRaster().getDataBuffer()).getData();
        int lastPixelOffset = right * dstWidth;
        float hSumDivider = 1.0f / (float) this.size;
        float vSumDivider = this.opacity / (float) this.size;
        int srcY = 0;
        int dstOffset = left * dstWidth;
        while (srcY < srcHeight) {
            historyIdx = 0;
            while (historyIdx < shadowSize) {
                aHistory[historyIdx++] = 0;
            }
            aSum = 0;
            historyIdx = 0;
            int srcX = 0;
            while (srcX < srcWidth) {
                a = (int) ((float) aSum * hSumDivider);
                dstBuffer[dstOffset++] = a << 24;
                aSum -= aHistory[historyIdx];
                aHistory[historyIdx] = a = srcColorModel.getAlpha(srcRaster.getDataElements(srcX, srcY, null));
                aSum += a;
                if (++historyIdx >= shadowSize) {
                    historyIdx -= shadowSize;
                }
                ++srcX;
            }
            int i = 0;
            while (i < shadowSize) {
                a = (int) ((float) aSum * hSumDivider);
                dstBuffer[dstOffset++] = a << 24;
                aSum -= aHistory[historyIdx];
                if (++historyIdx >= shadowSize) {
                    historyIdx -= shadowSize;
                }
                ++i;
            }
            ++srcY;
        }
        int x = 0;
        int bufferOffset = 0;
        while (x < dstWidth) {
            aSum = 0;
            historyIdx = 0;
            while (historyIdx < left) {
                aHistory[historyIdx++] = 0;
            }
            int y = 0;
            while (y < right) {
                a = dstBuffer[bufferOffset] >>> 24;
                aHistory[historyIdx++] = a;
                aSum += a;
                ++y;
                bufferOffset += dstWidth;
            }
            bufferOffset = x;
            historyIdx = 0;
            y = 0;
            while (y < yStop) {
                a = (int) ((float) aSum * vSumDivider);
                dstBuffer[bufferOffset] = a << 24 | shadowRgb;
                aSum -= aHistory[historyIdx];
                aHistory[historyIdx] = a = dstBuffer[bufferOffset + lastPixelOffset] >>> 24;
                aSum += a;
                if (++historyIdx >= shadowSize) {
                    historyIdx -= shadowSize;
                }
                ++y;
                bufferOffset += dstWidth;
            }
            y = yStop;
            while (y < dstHeight) {
                a = (int) ((float) aSum * vSumDivider);
                dstBuffer[bufferOffset] = a << 24 | shadowRgb;
                aSum -= aHistory[historyIdx];
                if (++historyIdx >= shadowSize) {
                    historyIdx -= shadowSize;
                }
                ++y;
                bufferOffset += dstWidth;
            }
            bufferOffset = ++x;
        }
        return dst;
    }

    private BufferedImage createShadowMask(BufferedImage image) {
        BufferedImage mask = new BufferedImage(image.getWidth(), image.getHeight(), 2);
        Graphics2D g2d = mask.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.setComposite(AlphaComposite.getInstance(5, this.opacity));
        g2d.setColor(this.color);
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2d.dispose();
        return mask;
    }

    private ConvolveOp getLinearBlurOp(int size) {
        float[] data = new float[size * size];
        float value = 1.0f / (float) (size * size);
        int i = 0;
        while (i < data.length) {
            data[i] = value;
            ++i;
        }
        return new ConvolveOp(new Kernel(size, size, data));
    }
}
