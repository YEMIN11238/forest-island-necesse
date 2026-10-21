package localforest.visual;
import java.util.List;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import net.bytebuddy.asm.Advice;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.*;
import necesse.level.maps.Level;
import necesse.level.gameObject.GameObject;
@ModMethodPatch(target=necesse.level.gameObject.container.WorkstationLegacyObject.class,name="addDrawables",arguments={List.class,OrderableDrawables.class,Level.class,int.class,int.class,TickManager.class,GameCamera.class,PlayerMob.class})
public class WorkbenchVisualPatch {
 @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class)
 public static boolean enter(@Advice.This GameObject object,@Advice.Argument(0) List<LevelSortedDrawable> sorted,
  @Advice.Argument(1) OrderableDrawables tiles,@Advice.Argument(2) Level level,@Advice.Argument(3) int x,@Advice.Argument(4) int y,
  @Advice.Argument(5) TickManager tick,@Advice.Argument(6) GameCamera camera,@Advice.Argument(7) PlayerMob player) {
   return IslandVisuals.object(object,sorted,tiles,level,x,y,tick,camera,player,"bench");
 }
}
