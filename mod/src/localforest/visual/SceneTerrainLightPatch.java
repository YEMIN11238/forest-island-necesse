package localforest.visual;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.gfx.drawables.LevelTileLightDrawOptions;
import necesse.level.maps.Level;
import net.bytebuddy.asm.Advice;
@ModMethodPatch(target=LevelTileLightDrawOptions.class,name="getSmoothLight",arguments={TickManager.class,Level.class,int.class,int.class})
public class SceneTerrainLightPatch {
 @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class)
 public static float[] enter(@Advice.Argument(1) Level l,@Advice.Argument(2) int x,@Advice.Argument(3) int y){return SceneLighting.enabled()&&IslandVisuals.active(l)?SceneLighting.terrain(l,x,y,true):null;}
 @Advice.OnMethodExit public static void exit(@Advice.Enter float[] replacement,@Advice.Return(readOnly=false) float[] result){if(replacement!=null)result=replacement;}
}
