package localforest.branding;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.window.GameWindow;
import necesse.gfx.forms.presets.MainMenuForm;
import net.bytebuddy.asm.Advice;
@ModMethodPatch(target=MainMenuForm.class,name="onWindowResized",arguments={GameWindow.class})
public class MosslumeMenuResizePatch {
    @Advice.OnMethodExit public static void exit(@Advice.This MainMenuForm menu,@Advice.Argument(0) GameWindow window) {
        MosslumeBranding.position(menu,window);
    }
}
