package localforest.visual;
import java.awt.Color;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import net.bytebuddy.asm.Advice;
import necesse.entity.mobs.PlayerMob;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
@ModMethodPatch(target=Item.class,name="drawIcon",arguments={InventoryItem.class,PlayerMob.class,int.class,int.class,int.class,Color.class})
public class MosslumeItemPatch{
 @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class)
 public static boolean enter(@Advice.This Item item,@Advice.Argument(0)InventoryItem stack,@Advice.Argument(1)PlayerMob player,@Advice.Argument(2)int x,@Advice.Argument(3)int y,@Advice.Argument(4)int size,@Advice.Argument(5)Color color){return MosslumeArt.icon(item,stack,player,x,y,size,color);}
}
