package localforest.visual;

import java.util.List;
import localforest.IslandMod;
import necesse.engine.postProcessing.PostProcessShaderStage;
import necesse.engine.postProcessing.PostProcessStage;
import necesse.engine.window.GameWindow;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameFrameBuffer;
import necesse.gfx.shader.GameShader;
import org.lwjgl.opengl.GL20;

/** Replaces, rather than stacks on, the engine's final scene scaler.
 * Upstream: zadpos, Sharp-Shimmerless-Shader, CC0 (see packaged notice).
 */
public final class MosslumePixelPipeline {
    public static long frames;
    public static int inputWidth, inputHeight, outputWidth, outputHeight;
    public static boolean installed;

    public static void install(GameWindow window, List<PostProcessStage> stages) {
        if (!IslandMod.enabled() || Boolean.getBoolean("mosslume.pixel.legacy")) return;
        if (stages.isEmpty()) return;
        int index = stages.size() - 1;
        PostProcessStage previous = stages.get(index);
        if (!(previous instanceof PostProcessShaderStage)
                || ((PostProcessShaderStage) previous).shader != GameResources.pixelScalingShader) {
            System.err.println("MOSSLUME_PIXEL_SCALER_SKIPPED final stage is not the native pixel scaler");
            return;
        }
        GameShader shader = new GameShader("vertScene", "mosslumeSharpShimmerless");
        if (GL20.glGetProgrami(shader.getProgram(), GL20.GL_LINK_STATUS) == GL20.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(shader.getProgram());
            shader.delete();
            throw new IllegalStateException("Mosslume pixel shader failed to link: " + log);
        }
        Stage replacement = new Stage(window, shader);
        stages.set(index, replacement);
        previous.dispose();
        installed = true;
        System.out.println("MOSSLUME_PIXEL_SCALER sharp-shimmerless source=2a74e7af599335c2ce5e493f823a757925c80db3 final-stage-replacement=true");
    }

    private static final class Stage extends PostProcessShaderStage {
        private boolean disposed;
        Stage(GameWindow window, GameShader shader) { super(window, shader); }

        @Override public void updateFrameBufferSize() {
            int width = window.getFrameWidth(), height = window.getFrameHeight();
            if (frameBuffer == null || frameBuffer.getWidth() != width || frameBuffer.getHeight() != height) {
                if (frameBuffer != null) frameBuffer.dispose();
                frameBuffer = window.getNewFrameBuffer(width, height);
            }
        }

        @Override public GameFrameBuffer doPostProcessing(GameFrameBuffer input) {
            // Read the actual preceding FBO; window scene dimensions are not assumed.
            inputWidth = input.getWidth(); inputHeight = input.getHeight();
            outputWidth = frameBuffer.getWidth(); outputHeight = frameBuffer.getHeight();
            frames++;
            return super.doPostProcessing(input);
        }

        @Override protected void setShaderVariables() {
            shader.pass2f("InputSize", inputWidth, inputHeight);
            shader.pass2f("OutputSize", outputWidth, outputHeight);
        }

        @Override public void dispose() {
            if (!disposed) { super.dispose(); shader.delete(); disposed = true; }
        }
    }
    private MosslumePixelPipeline() { }
}
