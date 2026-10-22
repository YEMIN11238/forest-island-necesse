package localforest.branding;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.state.MainMenu;
import net.bytebuddy.asm.Advice;
@ModMethodPatch(target=MainMenu.class,name="drawHud",arguments={TickManager.class})
public class MosslumeMenuBackdropPatch {
    @Advice.OnMethodEnter public static void enter() { MosslumeBranding.drawBackdrop(); }
}
