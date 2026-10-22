package localforest.visual;

import java.util.function.Consumer;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import net.bytebuddy.asm.Advice;

/** Bed/chair owners draw the same original resident in their native depth layer. */
@ModMethodPatch(target=HumanMob.class,name="getUserDrawOptions",arguments={Level.class,int.class,int.class,TickManager.class,GameCamera.class,PlayerMob.class,Consumer.class})
public class MosslumeResidentObjectUserPatch {
    @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class)
    public static DrawOptions enter(@Advice.This HumanMob mob,@Advice.Argument(0)Level level,@Advice.Argument(1)int x,@Advice.Argument(2)int y,
        @Advice.Argument(4)GameCamera camera,@Advice.Argument(5)PlayerMob player,
        @Advice.FieldValue(value="attackingItem",declaringType=ItemAttackerMob.class)InventoryItem attacking,@Advice.FieldValue("customHoldItem")InventoryItem held,@Advice.FieldValue("customHoldItemTimeout")long heldUntil){
        return CompanionSprites.objectUser(mob,level,x,y,camera,player,attacking,held,heldUntil);
    }
    @Advice.OnMethodExit public static void exit(@Advice.Enter DrawOptions replacement,@Advice.Return(readOnly=false)DrawOptions result){if(replacement!=null)result=replacement;}
}
