package localforest.branding;
import necesse.engine.modLoader.annotations.ModConstructorPatch;
import necesse.engine.state.MainMenu;
import necesse.gfx.forms.presets.MainMenuForm;
import net.bytebuddy.asm.Advice;
@ModConstructorPatch(target=MainMenuForm.class, arguments={MainMenu.class})
public class MosslumeMenuConstructorPatch {
    @Advice.OnMethodExit public static void exit(@Advice.This MainMenuForm menu) {
        MosslumeBranding.install(menu);
    }
}
