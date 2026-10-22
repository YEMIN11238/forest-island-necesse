package localforest.visual;
import java.util.function.Consumer;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import net.bytebuddy.asm.Advice;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.PlayerSprite;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.level.maps.light.GameLight;
@ModMethodPatch(target=PlayerSprite.class,name="getDrawOptions",arguments={PlayerMob.class,int.class,int.class,GameLight.class,GameCamera.class,Consumer.class})
public class MosslumePlayerPatch{
 @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class)
 public static DrawOptions enter(@Advice.Argument(0)PlayerMob p,@Advice.Argument(1)int x,@Advice.Argument(2)int y,@Advice.Argument(3)GameLight light,@Advice.Argument(4)GameCamera camera){return MosslumeArt.player(p,x,y,light,camera);}
 @Advice.OnMethodExit public static void exit(@Advice.Enter DrawOptions replacement,@Advice.Return(readOnly=false)DrawOptions result){if(replacement!=null)result=replacement;}
}
