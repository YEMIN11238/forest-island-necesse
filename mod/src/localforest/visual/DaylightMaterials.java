package localforest.visual;

import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.shader.GameShader;
import necesse.level.maps.Level;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/** Daytime source-material separation, before the existing scene light pass.
 * Grass and road verges share one palette transform. Props, water, paths,
 * emissive pixels and the accepted evening/night lighting stay unchanged.
 */
public final class DaylightMaterials {
    private static final boolean ENABLED = !"false".equalsIgnoreCase(System.getProperty("mosslume.daycolor", "true"));
    public static final float GROUND_SATURATION = parameter("groundSaturation", .70f, 0, 1.5f);
    public static final float GROUND_BRIGHTNESS = parameter("groundBrightness", .84f, 0, 1.5f);
    public static final float CROWN_SATURATION = parameter("crownSaturation", .88f, 0, 1.5f);
    public static final float CROWN_BRIGHTNESS = parameter("crownBrightness", 1f, 0, 1.5f);
    private static GameShader groundShader;
    private static boolean unavailable;
    public static long groundBatchDraws;

    private static float parameter(String key, float fallback, float min, float max) {
        try {
            float value = Float.parseFloat(System.getProperty("mosslume.daycolor." + key, Float.toString(fallback)));
            return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
        } catch (NumberFormatException ignored) { return fallback; }
    }

    /** Smooth only within full daylight. At 18:00 the accepted dusk/night
     * renderer receives the exact original source colors, not an inverse tint.
     */
    static float daylightAtHour(float hour) {
        if (!Float.isFinite(hour) || hour <= 7f || hour >= 18f) return 0;
        float value = Math.min(1f, Math.min(hour - 7f, 18f - hour));
        return value * value * (3f - 2f * value);
    }

    public static float amount(Level level) {
        return !ENABLED || level == null || level.isCave || level.getWorldEntity() == null
            ? 0 : daylightAtHour(level.getWorldEntity().getDayTimeHourFloat());
    }

    /** Called for every wind-shader draw so a preceding crown cannot tint a
     * following house, person, trunk, flower or other static object.
     */
    public static void bindCrown(GameShader shader, Level level, boolean crown) {
        shader.pass3f("dayMaterial", crown ? amount(level) : 0, CROWN_SATURATION, CROWN_BRIGHTNESS);
    }

    public static SharedTextureDrawOptions groundBatch(GameTexture texture, Level level) {
        return new GroundBatch(texture, level);
    }

    private static GameShader groundShader() {
        if (unavailable) return null;
        if (groundShader == null) {
            try {
                groundShader = new GameShader("mosslumeDayGround", "mosslumeDayGround");
                if (GL20.glGetProgrami(groundShader.getProgram(), GL20.GL_LINK_STATUS) == GL11.GL_FALSE)
                    throw new IllegalStateException(GL20.glGetProgramInfoLog(groundShader.getProgram()));
                System.out.println("MOSSLUME_DAY_MATERIAL_READY ground=" + GROUND_SATURATION + "/" + GROUND_BRIGHTNESS
                    + " crown=" + CROWN_SATURATION + "/" + CROWN_BRIGHTNESS + " hours=7-18 night=unchanged");
            } catch (RuntimeException error) {
                if (groundShader != null) groundShader.delete();
                groundShader = null;
                unavailable = true;
                System.err.println("MOSSLUME_DAY_MATERIAL_UNAVAILABLE " + error);
            }
        }
        return groundShader;
    }

    private static final class GroundBatch extends SharedTextureDrawOptions {
        private final Level level;
        private GroundBatch(GameTexture texture, Level level) { super(texture); this.level = level; }
        @Override public void draw(int maxDrawsPerCall) {
            float amount = amount(level);
            // The original native shader and batching path remains exact at night.
            GameShader shader = amount > 0 ? groundShader() : null;
            if (shader == null) { super.draw(maxDrawsPerCall); return; }
            shader.use();
            try {
                shader.pass3f("dayMaterial", amount, GROUND_SATURATION, GROUND_BRIGHTNESS);
                super.draw(maxDrawsPerCall);
                groundBatchDraws++;
            } finally { shader.stop(); }
        }
    }

    private DaylightMaterials() { }
}
