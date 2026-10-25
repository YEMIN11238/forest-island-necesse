package localforest.visual;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;

/**
 * One immutable shared mesh, uploaded once instead of glBegin/vertex JNI calls
 * for each plant. Buffer ownership/upload/draw separation follows libGDX's
 * Apache-2.0 SpriteBatch/Mesh path; native sorting and shader inputs stay here.
 * See third-party/libgdx-render for source, license and adaptation boundaries.
 */
final class WindMeshBuffer {
    private static int buffer;
    private static int scopes, previousBuffer, previousClientTexture;
    static long draws;

    static void begin() {
        if(scopes++!=0)return;
        previousBuffer=GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        previousClientTexture=GL11.glGetInteger(GL13.GL_CLIENT_ACTIVE_TEXTURE);
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
    }
    static void end() {
        if(scopes==0||--scopes!=0)return;
        GL11.glPopClientAttrib();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,previousBuffer);
        GL13.glClientActiveTexture(previousClientTexture);
    }

    static void draw() {
        boolean owned=scopes==0;
        if(owned)begin();
        try {
            GL13.glClientActiveTexture(GL13.GL_TEXTURE0);
            if (buffer == 0) {
                FloatBuffer vertices = BufferUtils.createFloatBuffer(18 * 5);
                for (int row = 0; row <= 8; row++) {
                    float v = row / 8f;
                    vertices.put(0).put(v).put(0).put(v).put(0);
                    vertices.put(1).put(v).put(1).put(v).put(0);
                }
                vertices.flip();
                buffer = GL15.glGenBuffers();
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
            } else GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            GL11.glInterleavedArrays(GL11.GL_T2F_V3F, 0, 0L);
            GL11.glDrawArrays(GL11.GL_QUAD_STRIP, 0, 18);
            draws++;
        } finally {
            // Native immediate drawing ignores disabled arrays; native array
            // passes configure their own pointers. Restore the caller's exact
            // client state once at the enclosing level-draw boundary, rather
            // than querying/stalling the driver hundreds of times per frame.
            GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,0);
            GL13.glClientActiveTexture(previousClientTexture);
            if(owned)end();
        }
    }

    private WindMeshBuffer() { }
}
