package localforest;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.state.MainGame;
import net.bytebuddy.asm.Advice;

/** Native framebuffer QA overlay, absent unless explicitly enabled in a disposable profile. */
@ModMethodPatch(target=MainGame.class,name="drawHud",arguments={TickManager.class})
public class WeaponMatrixHudPatch {
    @Advice.OnMethodExit public static void after(@Advice.This MainGame game,@Advice.Argument(0)TickManager tick){WeaponMatrixProbe.drawHud(game,tick);}
}
