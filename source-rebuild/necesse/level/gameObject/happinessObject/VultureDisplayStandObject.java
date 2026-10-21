package necesse.level.gameObject.happinessObject;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.interfaces.OEInventory;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.HappinessObject;
import necesse.level.gameObject.container.DisplayStandObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class VultureDisplayStandObject extends DisplayStandObject implements HappinessObject {
   public VultureDisplayStandObject(String textureName, ToolType toolType, Color mapColor, int itemHeight, String... category) {
      super(textureName, toolType, mapColor, itemHeight, category);
      this.rarity = Item.Rarity.EPIC;
   }

   public VultureDisplayStandObject(String textureName, Color mapColor, int itemHeight, String... category) {
      this(textureName, ToolType.ALL, mapColor, itemHeight, category);
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      Level level,
      int tileX,
      int tileY,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      final TextureDrawOptions base = this.texture
         .initDraw()
         .sprite(0, 0, 32, this.texture.getHeight())
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX, drawY - (this.texture.getHeight() - 32));
      ObjectEntity ent = level.entityManager.getObjectEntity(tileX, tileY);
      final DrawOptions item;
      if (ent != null && ent.implementsOEInventory()) {
         InventoryItem invItem = ((OEInventory)ent).getInventory().getItem(0);
         item = invItem != null ? invItem.getWorldDrawOptions(perspective, drawX + 16, drawY + 32 - this.itemHeight, light, 0.0F, 32) : () -> {};
      } else {
         item = () -> {};
      }

      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            base.draw();
            item.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      this.texture.initDraw().sprite(0, 0, 32, this.texture.getHeight()).alpha(alpha).draw(drawX, drawY - (this.texture.getHeight() - 32));
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(this.getHappinessObjectTooltip());
      return tooltips;
   }
}
