package localforest.branding;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.platforms.sharedOnPC.window.GLFWGameWindow;
import net.bytebuddy.asm.Advice;
@ModMethodPatch(target=GLFWGameWindow.class,name="createWindow",arguments={GLFWGameWindow.class,boolean.class})
public class MosslumeWindowPatch {
    @Advice.OnMethodExit public static void exit(@Advice.This GLFWGameWindow window) {
        MosslumeBranding.setWindowTitle(window);
    }
}
