package localforest.branding;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.state.MainMenu;
import necesse.engine.window.GameWindow;
import net.bytebuddy.asm.Advice;
@ModMethodPatch(target=MainMenu.class,name="frameTick",arguments={TickManager.class,GameWindow.class})
public class MosslumeMenuQaPatch {
    @Advice.OnMethodExit public static void exit(@Advice.This MainMenu menu) {
        MosslumeBranding.qaTick(menu);
    }
}
