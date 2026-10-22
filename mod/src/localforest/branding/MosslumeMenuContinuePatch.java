package localforest.branding;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.gfx.forms.presets.MainMenuForm;
import net.bytebuddy.asm.Advice;
@ModMethodPatch(target=MainMenuForm.class,name="updateContinueButton",arguments={})
public class MosslumeMenuContinuePatch {
    @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class) public static boolean enter() {
        // Our fixed-island entry deliberately does not follow an absolute-path continue cache.
        return MosslumeBranding.enabled();
    }
}
