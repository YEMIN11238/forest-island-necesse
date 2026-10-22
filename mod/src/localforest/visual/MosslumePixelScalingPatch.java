package localforest.visual;

import java.util.ArrayList;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.postProcessing.PostProcessStage;
import necesse.engine.window.GameWindow;
import net.bytebuddy.asm.Advice;

@ModMethodPatch(target=GameWindow.class, name="setupPostProcessing", arguments={})
public class MosslumePixelScalingPatch {
    @Advice.OnMethodExit public static void after(@Advice.This GameWindow window,
            @Advice.FieldValue("scenePostProcessing") ArrayList<PostProcessStage> stages) {
        MosslumePixelPipeline.install(window, stages);
    }
}
