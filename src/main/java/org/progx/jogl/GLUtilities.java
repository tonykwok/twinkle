package org.progx.jogl;
import java.util.HashMap;

import com.jogamp.opengl.GL2;
import org.jogamp.vecmath.Matrix4f;
import org.jogamp.vecmath.Point2d;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3f;

import org.progx.jogl.rendering.Renderable;

public class GLUtilities {
    private static HashMap<Integer, Point2d[]> jitterMap = null;

    public static void drawLocalAxis(GL2 gl, float axisLength) {
        Vector3f x = new Vector3f(1.0f, 0.0f, 0.0f);
        Vector3f y = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f z = new Vector3f(0.0f, 0.0f, 1.0f);
        
        gl.glLineWidth(2);
        gl.glBegin(GL2.GL_LINES);
        gl.glColor3f(1, 0, 0);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(x.x * axisLength, x.y * axisLength, x.z * axisLength);
    
        gl.glColor3f(0, 1, 0);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(y.x * axisLength, y.y * axisLength, y.z * axisLength);
    
        gl.glColor3f(0, 0, 1);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(z.x * axisLength, z.y * axisLength, z.z * axisLength);
        gl.glEnd();
    
        gl.glPopAttrib();
    }

    public static void renderBillboard(GL2 gl,  Vector3f camPos, Vector3f camUp, Renderable item) {
        Point3f pos = item.getPosition();
        
        Vector3f look = new Vector3f();
        Vector3f right = new Vector3f();
        Vector3f up = new Vector3f();

        look.sub(camPos, pos);
        look.normalize();

        right.cross(camUp, look);
        up.cross(look, right);

        gl.glMultMatrixf(new float[] { right.x, right.y, right.z, 0.0f,
                                       up.x, up.y, up.z, 0.0f,
                                       look.x, look.y, look.z, 0.0f,
                                       pos.x, pos.y, pos.z, 1 }, 0);
        
        item.render(gl);
    }

    public static void getCameraVectors(GL2 gl, Vector3f camPos, Vector3f camUp) {
        float[] matrix = new float[16];
        gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, matrix, 0);
    
        camPos.set(new float[] { -matrix[12], -matrix[13], -matrix[14] });
        camUp.set(new float[] { matrix[1], matrix[5], matrix[9] });
        
        matrix[12] = matrix[13] = matrix[14] = 0;
        Matrix4f view = new Matrix4f(matrix);
        view.transform(camPos);
    }

    public static void renderAntiAliased(GL2 gl, Renderable scene, int aa) {
        if (aa <= 1) {
            scene.render(gl);
            return;
        }
        
        int[] viewport = new int[4];
        gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);
        gl.glClear(GL2.GL_ACCUM_BUFFER_BIT);
        
        Point2d[] samples = GLUtilities.getJitterSamples(aa);

        for (int jitter = 0; jitter < samples.length; jitter++) {
           gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
           GLUtilities.setPerspective(gl,
                                      45.0, (double) viewport[2] / (double) viewport[3], 1.0, 250.0,
                                      samples[jitter].x, samples[jitter].y,
                                      0.0, 0.0, 1.0);
           scene.render(gl);
           gl.glAccum(GL2.GL_ACCUM, 1.0f / samples.length);
        }

        gl.glAccum(GL2.GL_RETURN, 1.0f);
        gl.glFlush();
    }

    public static void setFrustum(GL2 gl,
                                  double left, double right, double bottom, 
                                  double top, double near, double far, double pixdx, 
                                  double pixdy, double eyedx, double eyedy, double focus) {
        int[] viewport = new int[4];
        gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);
        
        double xwsize = right - left;
        double ywsize = top - bottom;
        
        double dx = -(pixdx * xwsize / (double) viewport[2] + eyedx * near / focus);
        double dy = -(pixdy * ywsize / (double) viewport[3] + eyedy * near / focus);
        
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glFrustum(left + dx, right + dx, bottom + dy, top + dy, near, far);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glTranslatef((float) -eyedx, (float) -eyedy, 0.0f); 
    }

    public static void setPerspective(GL2 gl,
                                      double fovy, double aspect, 
                                      double near, double far, double pixdx, double pixdy, 
                                      double eyedx, double eyedy, double focus) {
        double fov2 = ((fovy * Math.PI) / 180.0) / 2.0;

        double top = near / (Math.cos(fov2) / Math.sin(fov2));
        double bottom = -top;

        double right = top * aspect;
        double left = -right;

        setFrustum(gl,
                   left, right, bottom, top, near, far,
                   pixdx, pixdy, eyedx, eyedy, focus);
    } 

    public static Point2d[] getJitterSamples(int sampleAmount) {
        if (jitterMap == null) {
            initJitterMap();
        }

        // fail-safe when sampleAmount does not exist
        if (!jitterMap.containsKey(sampleAmount)) {
            int difference = Integer.MAX_VALUE;
            int newSampleAmount = 0;
            
            for (int value: jitterMap.keySet()) {
                if (Math.abs(value - sampleAmount) < difference) {
                    difference = Math.abs(value - sampleAmount);
                    newSampleAmount = value;
                }
            }
            sampleAmount = newSampleAmount;
        }

        return jitterMap.get(sampleAmount);
    }

    // these values come from the example jitter.h in the Red Book
    private static void initJitterMap() {
        jitterMap = new HashMap<Integer, Point2d[]>(7);
        jitterMap.put(2, new Point2d[] { new Point2d(0.246490,  0.249999),
                                         new Point2d(-0.246490, -0.249999) });
        jitterMap.put(3, new Point2d[] { new Point2d(-0.373411, -0.250550),
                                         new Point2d(0.256263,  0.368119),
                                         new Point2d(0.117148, -0.117570) });
        jitterMap.put(4, new Point2d[] { new Point2d(-0.208147,  0.353730),
                                         new Point2d(0.203849, -0.353780),
                                         new Point2d(-0.292626, -0.149945),
                                         new Point2d(0.296924,  0.149994) });
        jitterMap.put(8, new Point2d[] { new Point2d(-0.334818,  0.435331),
                                         new Point2d(0.286438, -0.393495),
                                         new Point2d(0.459462,  0.141540),
                                         new Point2d(-0.414498, -0.192829),
                                         new Point2d(-0.183790,  0.082102),
                                         new Point2d(-0.079263, -0.317383),
                                         new Point2d(0.102254,  0.299133),
                                         new Point2d(0.164216, -0.054399) });
        jitterMap.put(15, new Point2d[] { new Point2d(0.285561,  0.188437),
                                          new Point2d(0.360176, -0.065688),
                                          new Point2d(-0.111751,  0.275019),
                                          new Point2d(-0.055918, -0.215197),
                                          new Point2d(-0.080231, -0.470965),
                                          new Point2d(0.138721,  0.409168),
                                          new Point2d(0.384120,  0.458500),
                                          new Point2d(-0.454968,  0.134088),
                                          new Point2d(0.179271, -0.331196),
                                          new Point2d(-0.307049, -0.364927),
                                          new Point2d(0.105354, -0.010099),
                                          new Point2d(-0.154180,  0.021794),
                                          new Point2d(-0.370135, -0.116425),
                                          new Point2d(0.451636, -0.300013),
                                          new Point2d(-0.370610,  0.387504) });
        jitterMap.put(24, new Point2d[] { new Point2d(0.030245,  0.136384),
                                          new Point2d(0.018865, -0.348867),
                                          new Point2d(-0.350114, -0.472309),
                                          new Point2d(0.222181,  0.149524),
                                          new Point2d(-0.393670, -0.266873),
                                          new Point2d(0.404568,  0.230436),
                                          new Point2d(0.098381,  0.465337),
                                          new Point2d(0.462671,  0.442116),
                                          new Point2d(0.400373, -0.212720),
                                          new Point2d(-0.409988,  0.263345),
                                          new Point2d(-0.115878, -0.001981),
                                          new Point2d(0.348425, -0.009237),
                                          new Point2d(-0.464016,  0.066467),
                                          new Point2d(-0.138674, -0.468006),
                                          new Point2d(0.144932, -0.022780),
                                          new Point2d(-0.250195,  0.150161),
                                          new Point2d(-0.181400, -0.264219),
                                          new Point2d(0.196097, -0.234139),
                                          new Point2d(-0.311082, -0.078815),
                                          new Point2d(0.268379,  0.366778),
                                          new Point2d(-0.040601,  0.327109),
                                          new Point2d(-0.234392,  0.354659),
                                          new Point2d(-0.003102, -0.154402),
                                          new Point2d(0.297997, -0.417965) });
        jitterMap.put(66, new Point2d[] { new Point2d(0.266377, -0.218171),
                                          new Point2d(-0.170919, -0.429368),
                                          new Point2d(0.047356, -0.387135),
                                          new Point2d(-0.430063,  0.363413),
                                          new Point2d(-0.221638, -0.313768),
                                          new Point2d(0.124758, -0.197109),
                                          new Point2d(-0.400021,  0.482195),
                                          new Point2d(0.247882,  0.152010),
                                          new Point2d(-0.286709, -0.470214),
                                          new Point2d(-0.426790,  0.004977),
                                          new Point2d(-0.361249, -0.104549),
                                          new Point2d(-0.040643,  0.123453),
                                          new Point2d(-0.189296,  0.438963),
                                          new Point2d(-0.453521, -0.299889),
                                          new Point2d(0.408216, -0.457699),
                                          new Point2d(0.328973, -0.101914),
                                          new Point2d(-0.055540, -0.477952),
                                          new Point2d(0.194421,  0.453510),
                                          new Point2d(0.404051,  0.224974),
                                          new Point2d(0.310136,  0.419700),
                                          new Point2d(-0.021743,  0.403898),
                                          new Point2d(-0.466210,  0.248839),
                                          new Point2d(0.341369,  0.081490),
                                          new Point2d(0.124156, -0.016859),
                                          new Point2d(-0.461321, -0.176661),
                                          new Point2d(0.013210,  0.234401),
                                          new Point2d(0.174258, -0.311854),
                                          new Point2d(0.294061,  0.263364),
                                          new Point2d(-0.114836,  0.328189),
                                          new Point2d(0.041206, -0.106205),
                                          new Point2d(0.079227,  0.345021),
                                          new Point2d(-0.109319, -0.242380),
                                          new Point2d(0.425005, -0.332397),
                                          new Point2d(0.009146,  0.015098),
                                          new Point2d(-0.339084, -0.355707),
                                          new Point2d(-0.224596, -0.189548),
                                          new Point2d(0.083475,  0.117028),
                                          new Point2d(0.295962, -0.334699),
                                          new Point2d(0.452998,  0.025397),
                                          new Point2d(0.206511, -0.104668),
                                          new Point2d(0.447544, -0.096004),
                                          new Point2d(-0.108006, -0.002471),
                                          new Point2d(-0.380810,  0.130036),
                                          new Point2d(-0.242440,  0.186934),
                                          new Point2d(-0.200363,  0.070863),
                                          new Point2d(-0.344844, -0.230814),
                                          new Point2d(0.408660,  0.345826),
                                          new Point2d(-0.233016,  0.305203),
                                          new Point2d(0.158475, -0.430762),
                                          new Point2d(0.486972,  0.139163),
                                          new Point2d(-0.301610,  0.009319),
                                          new Point2d(0.282245, -0.458671),
                                          new Point2d(0.482046,  0.443890),
                                          new Point2d(-0.121527,  0.210223),
                                          new Point2d(-0.477606, -0.424878),
                                          new Point2d(-0.083941, -0.121440),
                                          new Point2d(-0.345773,  0.253779),
                                          new Point2d(0.234646,  0.034549),
                                          new Point2d(0.394102, -0.210901),
                                          new Point2d(-0.312571,  0.397656),
                                          new Point2d(0.200906,  0.333293),
                                          new Point2d(0.018703, -0.261792),
                                          new Point2d(-0.209349, -0.065383),
                                          new Point2d(0.076248,  0.478538),
                                          new Point2d(-0.073036, -0.355064),
                                          new Point2d(0.145087,  0.221726) });
    }
}
