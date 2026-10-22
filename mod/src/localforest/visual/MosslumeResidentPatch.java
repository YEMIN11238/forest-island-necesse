package localforest.visual;
import java.util.List;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.*;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import net.bytebuddy.asm.Advice;
@ModMethodPatch(target=HumanMob.class,name="addDrawables",arguments={List.class,OrderableDrawables.class,OrderableDrawables.class,Level.class,int.class,int.class,TickManager.class,GameCamera.class,PlayerMob.class})
public class MosslumeResidentPatch{
 @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class)
 public static boolean enter(@Advice.This HumanMob mob,@Advice.Argument(0)List<MobDrawable> list,@Advice.Argument(3)Level level,@Advice.Argument(4)int x,@Advice.Argument(5)int y,@Advice.Argument(7)GameCamera camera,@Advice.Argument(8)PlayerMob player){
  return MosslumeArt.drawResident(mob,list,level,x,y,camera,player);
 }
}
