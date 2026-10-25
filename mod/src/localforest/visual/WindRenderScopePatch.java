package localforest.visual;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelDrawUtils;
import necesse.level.maps.Level;
import net.bytebuddy.asm.Advice;
import java.util.List;
import necesse.gfx.drawables.LevelSortedDrawable;

/** Borrow client array state once, across native-order sprite draws. */
@ModMethodPatch(target=LevelDrawUtils.class,name="draw",
    arguments={GameCamera.class,PlayerMob.class,TickManager.class,boolean.class})
public class WindRenderScopePatch {
    @Advice.OnMethodEnter
    public static boolean before(@Advice.FieldValue("level") Level level,
            @Advice.FieldValue("drawPrepared") boolean prepared,
            @Advice.FieldValue(value="sortedDrawables",readOnly=false) List<LevelSortedDrawable> sorted) {
        if(!IslandVisuals.active(level))return false;
        if(!prepared)return false;
        sorted=CrownSpriteBatch.boundaries(sorted);
        CrownWindRenderer.beginFrame(level);return true;
    }
    @Advice.OnMethodExit(onThrowable=Throwable.class)
    public static void after(@Advice.Enter boolean owned) {
        if(owned)CrownWindRenderer.endFrame();
    }
}
