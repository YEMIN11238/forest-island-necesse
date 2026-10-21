package necesse.level.gameObject.happinessObject;

import java.awt.Color;
import java.awt.Rectangle;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.HappinessObject;
import necesse.level.gameObject.StaticMultiObject;
import necesse.level.maps.Level;

public class RuneboundDrumObject extends StaticMultiObject implements HappinessObject {
   public RuneboundDrumObject(int multiX, int multiY, int multiWidth, int multiHeight, int[] multiIDs, Rectangle fullCollision) {
      super(multiX, multiY, multiWidth, multiHeight, multiIDs, fullCollision, "runebounddrum");
      this.rarity = Item.Rarity.EPIC;
      this.mapColor = new Color(180, 160, 124);
      this.objectHealth = 50;
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.hoverHitbox = new Rectangle(0, -32, 32, 64);
      this.setItemCategory("objects", "decorations");
      this.setCraftingCategory("objects", "decorations");
   }

   @Override
   public boolean canInteract(Level level, int x, int y, PlayerMob player) {
      return true;
   }

   @Override
   public void interact(Level level, int x, int y, PlayerMob player) {
      super.interact(level, x, y, player);
      if (player.isClient()) {
         SoundManager.playSound(
            GameResources.blunthit, SoundEffect.effect(x * 32, y * 32).volume(0.8F).pitch(GameRandom.globalRandom.getFloatBetween(1.9F, 2.1F))
         );
      }
   }

   public static int[] registerRuneboundDrum() {
      int[] ids = new int[2];
      Rectangle collision = new Rectangle(0, 0, 64, 32);
      ids[0] = ObjectRegistry.registerObject("runebounddrum", new RuneboundDrumObject(0, 0, 2, 1, ids, collision), 750.0F, true, false);
      ids[1] = ObjectRegistry.registerObject("runebounddrum2", new RuneboundDrumObject(1, 0, 2, 1, ids, collision), 0.0F, false);
      return ids;
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(this.getHappinessObjectTooltip());
      return tooltips;
   }
}
