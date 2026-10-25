package localforest.visual;

import java.util.HashMap;
import java.util.Map;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.shader.GameShader;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.Level;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

/**
 * GLSL 1.20 port of Maujoe's MIT Simple Wind Shader 2D. Geometry stays in the
 * native sorted draw path; only this sprite is deformed, with a fixed base.
 * See mosslume/third-party/simple-wind-2d for the pinned source and license.
 */
public final class CrownWindRenderer {
    private static final int SEGMENTS = 8;
    private static final Double FIXED_SECONDS = configuredTime();
    private static final boolean PIXEL_COVERAGE = !"false".equalsIgnoreCase(System.getProperty("mosslume.wind.coverage", "true"));
    private static volatile double probeSeconds = Double.NaN;
    private static double frameSeconds=Double.NaN;
    private static int frameDepth;
    private static GameShader shader;
    private static boolean unavailable;
    public static long meshDraws;
    public static long fallbackDraws;
    public static boolean usesVbo() { return !Boolean.getBoolean("mosslume.wind.immediate"); }
    public static long vboDraws() { return WindMeshBuffer.draws; }
    public static void beginFrame() { beginFrame(null); }
    public static void beginFrame(Level level) {
        if(frameDepth==0){VisualClock.frame(level);frameSeconds=timeSeconds();}
        frameDepth++;CrownSpriteBatch.begin();if(usesVbo())WindMeshBuffer.begin();
    }
    public static void endFrame() {
        try{CrownSpriteBatch.end();}
        finally{
            if(usesVbo())WindMeshBuffer.end();
            if(--frameDepth==0)frameSeconds=Double.NaN;
        }
    }

    // The engine GameShader queries locations on each pass call. Plants are
    // numerous, so cache our small fixed uniform set after the initial link.
    private static final class WindShader extends GameShader {
        private final Map<String,Integer> locations = new HashMap<>();
        private final Map<String,float[]> values = new HashMap<>();
        private final Map<String,Integer> integers = new HashMap<>();
        private WindShader() { super("mosslumeWind", "mosslumeWind"); }
        @Override public int getUniform(String name) {
            Integer location=locations.get(name);
            if(location==null){location=super.getUniform(name);locations.put(name,location);}
            return location;
        }
        private boolean changed(String name,float a,float b,float c,float d){
            float[] old=values.get(name);
            if(old!=null&&old[0]==a&&old[1]==b&&old[2]==c&&old[3]==d)return false;
            if(old==null){old=new float[4];values.put(name,old);}
            old[0]=a;old[1]=b;old[2]=c;old[3]=d;return true;
        }
        @Override public void pass1f(String n,float a){if(changed(n,a,0,0,0))super.pass1f(n,a);}
        @Override public void pass2f(String n,float a,float b){if(changed(n,a,b,0,0))super.pass2f(n,a,b);}
        @Override public void pass3f(String n,float a,float b,float c){if(changed(n,a,b,c,0))super.pass3f(n,a,b,c);}
        @Override public void pass4f(String n,float a,float b,float c,float d){if(changed(n,a,b,c,d))super.pass4f(n,a,b,c,d);}
        @Override public void pass1i(String n,int a){Integer old=integers.get(n);if(old==null||old!=a){integers.put(n,a);super.pass1i(n,a);}}
    }

    private static Double configuredTime() {
        String value = System.getProperty("mosslume.wind.time",
                System.getProperty("mosslume.wind.fixedSeconds"));
        if (value == null) return null;
        try { double time = Double.parseDouble(value); return Double.isFinite(time) ? time : null; }
        catch (NumberFormatException ignored) { return null; }
    }

    /** The probe sets this once per render frame, never from a simulation tick. */
    public static void setProbeTimeSeconds(double seconds) { probeSeconds = seconds; }
    public static void clearProbeTime() { probeSeconds = Double.NaN; }
    public static double timeSeconds() {
        if(Double.isFinite(frameSeconds))return frameSeconds;
        if (Double.isFinite(probeSeconds)) return probeSeconds;
        String dynamic = System.getProperty("mosslume.probe.animationSeconds");
        if (dynamic != null) {
            try { double time = Double.parseDouble(dynamic); if (Double.isFinite(time)) return time; }
            catch (NumberFormatException ignored) { }
        }
        return FIXED_SECONDS != null ? FIXED_SECONDS : VisualClock.seconds();
    }

    public static boolean isLegacy() { return Boolean.getBoolean("mosslume.wind.legacy"); }
    public static boolean usesPixelCoverage() { return PIXEL_COVERAGE; }

    private static GameShader shader() {
        if (unavailable) return null;
        if (shader == null) {
            try {
                shader = new WindShader();
                if (GL20.glGetProgrami(shader.getProgram(), GL20.GL_LINK_STATUS) == GL11.GL_FALSE)
                    throw new IllegalStateException(GL20.glGetProgramInfoLog(shader.getProgram()));
                System.out.println("MOSSLUME_WIND_SHADER_READY upstream=5a1ee79571530749b707c8fa3154acbe90aee727 segments=" + SEGMENTS + " pixelCoverage=" + PIXEL_COVERAGE);
            } catch (RuntimeException error) {
                if (shader != null) shader.delete();
                shader = null;
                unavailable = true;
                System.err.println("MOSSLUME_WIND_SHADER_UNAVAILABLE " + error);
            }
        }
        return shader;
    }

    /**
     * Draw a small shared-edge strip mesh. No per-row draw objects, rounded
     * displacement, texture upload or simulation changes are involved.
     */
    public static boolean draw(GameTexture texture, int footX, int footY, float scale,
            GameLight light, float alpha, int tileX, int tileY, boolean crown) {
        return draw(texture,footX,footY,scale,light,alpha,tileX,tileY,crown,null,true,texture.getHeight()*scale);
    }
    public static boolean draw(GameTexture texture, int footX, int footY, float scale,
            GameLight light, float alpha, int tileX, int tileY, boolean crown,Level level,boolean wind,float elevation) {
        if(CrownSpriteBatch.add(texture,footX,footY,scale,light,alpha,tileX,tileY,crown,level,wind,elevation)){
            meshDraws++;return true;
        }
        CrownSpriteBatch.flush();
        GameShader current = shader();
        if (current == null) { fallbackDraws++; return false; }
        float width = Math.round(texture.getWidth() * scale);
        float height = Math.round(texture.getHeight() * scale);
        VisualMaterialDefinition material=VisualMaterials.get(texture);
        float left=material==null?footX-width*.5f:material.left(footX,scale);
        float top=material==null?footY-height:material.top(footY,scale);
        // A shared breeze with a small world-space phase difference. Camera
        // motion never enters the wind field, so it cannot change the bend.
        float phase = WindField.phase(tileX,tileY);
        TextureStateScope.begin();
        try {
            current.use();
            current.pass1f("windTime", (float)timeSeconds());
            current.pass1f("speed",material==null?(crown?WindField.CROWN_SPEED:WindField.PLANT_SPEED):material.windSpeed);
            current.pass1f("strengthScale",wind?width*(material==null?(crown?WindField.CROWN_AMPLITUDE:WindField.PLANT_AMPLITUDE):material.windAmplitude):0);
            current.pass1f("heightOffset",material==null?(crown?.10f:.07f):material.windPivot);
            current.pass1f("heightPower",crown?1.5f:1.9f);
            current.pass1f("windPhase", phase);
            current.pass2f("worldFoot",tileX*32+16,tileY*32+29);
            current.pass1i("pixelCoverage", wind&&PIXEL_COVERAGE ? 1 : 0);
            current.pass2f("texture_pixel_size", 1f / texture.getWidth(), 1f / texture.getHeight());
            current.pass1f("smoothing_factor", 1f);
            current.pass1i("normalizedMesh", usesVbo() ? 1 : 0);
            current.pass4f("meshRect", left, top, width, height);
            DaylightMaterials.bindCrown(current,level,crown);
            if(level!=null&&SceneLighting.enabled())SceneLighting.bind(current,level,tileX*32+16,tileY*32+29,width,height,material==null?elevation:material.effectiveShadowHeight(scale),crown);
            else current.pass1i("surfaceLighting",0);
            GameTexture normals=VisualMaterials.normal(texture);
            current.pass1i("normalTexture",1);current.pass1i("hasNormalTexture",normals==null?0:1);
            if(normals!=null)normals.bindTexture(GL13.GL_TEXTURE1);
            SurfaceOcclusion.bind(current,level);
            texture.bindTexture(GL13.GL_TEXTURE0);
            GL11.glLoadIdentity();
            float intensity = light.getFloatLevel();
            GL11.glColor4f(light.getFloatRed() * intensity, light.getFloatGreen() * intensity,
                    light.getFloatBlue() * intensity, alpha);
            if (usesVbo()) WindMeshBuffer.draw();
            else {
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (int segment = 0; segment <= SEGMENTS; segment++) {
                float v = segment / (float)SEGMENTS;
                float y = top + height * v;
                GL11.glTexCoord2f(0, v); GL11.glVertex3f(left, y, 0);
                GL11.glTexCoord2f(1, v); GL11.glVertex3f(left + width, y, 0);
            }
            GL11.glEnd();
            }
            meshDraws++;
            return true;
        } finally {
            try{current.stop();}finally{TextureStateScope.end();}
        }
    }

    private CrownWindRenderer() { }
}
