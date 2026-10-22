package localforest.branding;
import java.util.function.Supplier;
import necesse.engine.GameLoadingScreen;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.window.GameWindow;
import net.bytebuddy.asm.Advice;
@ModMethodPatch(target=GameLoadingScreen.class,name="drawLogo",arguments={GameWindow.class,Supplier.class})
public class MosslumeLogoPatch {
    @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class) public static boolean enter() {
        return MosslumeBranding.enabled();
    }
}
