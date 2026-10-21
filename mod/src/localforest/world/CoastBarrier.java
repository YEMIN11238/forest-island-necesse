package localforest.world;

import java.awt.Color;
import java.awt.Rectangle;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.GameObject;

/** Invisible collision keeps player and native pathfinding on the playable shore. */
public final class CoastBarrier extends GameObject {
 public CoastBarrier(){
  super(new Rectangle(32,32));
  isSolid=true;isLightTransparent=true;canPlaceOnLiquid=true;canPlaceOnShore=true;
  toolType=ToolType.UNBREAKABLE;objectHealth=10000;mapColor=new Color(0,95,150);
 }
 @Override public GameMessage getNewLocalization(){return new StaticMessage("海岸边界");}
}
