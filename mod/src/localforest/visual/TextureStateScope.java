package localforest.visual;

import org.lwjgl.opengl.GL11;

/**
 * Isolates auxiliary samplers in the GL 2.1 compatibility renderer. The native
 * SceneShader owns units 1 (recolor) and 2 (overlay); changing the program alone
 * does not restore those bindings. GL_TEXTURE_BIT also saves the active server
 * texture unit and each unit's binding, without synchronous glGet calls per
 * sprite or resetting the native shader's sampler uniforms.
 *
 * Keep begin/end paired in finally, outside shader use/stop. This is a server
 * attribute scope, separate from the vertex-array client attribute scope.
 */
final class TextureStateScope {
    static void begin() { GL11.glPushAttrib(GL11.GL_TEXTURE_BIT); }
    static void end() { GL11.glPopAttrib(); }
    private TextureStateScope() {}
}
