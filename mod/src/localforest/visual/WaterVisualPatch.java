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
@ModMethodPatch(target=necesse.level.gameTile.LiquidTile.class,name="addDrawables",arguments={LevelTileTerrainDrawOptions.class,LevelTileLiquidDrawOptions.class,LevelTileTerrainDrawOptions.class,OrderableDrawables.class,List.class,Level.class,int.class,int.class,GameCamera.class,TickManager.class})
public class WaterVisualPatch {
 @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class)
 public static boolean enter(@Advice.This necesse.level.gameTile.GameTile tile,@Advice.Argument(0) LevelTileTerrainDrawOptions under,
  @Advice.Argument(1) LevelTileLiquidDrawOptions liquid,@Advice.Argument(2) LevelTileTerrainDrawOptions over,
  @Advice.Argument(3) OrderableDrawables objects,@Advice.Argument(4) List<LevelSortedDrawable> sorted,@Advice.Argument(5) Level level,
  @Advice.Argument(6) int x,@Advice.Argument(7) int y,@Advice.Argument(8) GameCamera camera,@Advice.Argument(9) TickManager tick) {
   return IslandVisuals.terrain(tile,under,liquid,over,objects,sorted,level,x,y,camera,tick);
 }
}
