package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameBackground;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTooltips.FairTypeTooltip;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;

public class FreyaAndZithaStatueObject extends StaticMultiObject {
   protected FreyaAndZithaStatueObject(String texturePath, int multiX, int multiY, int multiWidth, int multiHeight, int[] multiIDs, Rectangle fullCollision) {
      super(multiX, multiY, multiWidth, multiHeight, multiIDs, fullCollision, "statues/" + texturePath);
      this.stackSize = 1;
      this.rarity = Item.Rarity.LEGENDARY;
      this.mapColor = new Color(143, 143, 143);
      this.objectHealth = 100;
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.hoverHitbox = new Rectangle(0, 0, 32, 32);
      this.setItemCategory("objects", "landscaping", "masonry");
      this.setCraftingCategory("objects", "landscaping", "masonry");
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips itemTooltips = super.getItemTooltips(item, perspective);
      itemTooltips.add(GameUtils.getDevQuoteTooltip("itemtooltip", "freyaandzithastatuequote", null));
      return itemTooltips;
   }

   @Override
   public void onMouseHover(Level level, int x, int y, GameCamera camera, PlayerMob perspective, boolean debug) {
      super.onMouseHover(level, x, y, camera, perspective, debug);
      String tip = Localization.translate("ui", "freyaandzithastatueplaque1")
         + "\n"
         + Localization.translate("ui", "freyaandzithastatueplaque2")
         + "\n"
         + Localization.translate("ui", "freyaandzithastatueplaque3")
         + "\n"
         + Localization.translate("ui", "freyaandzithastatueplaque4");
      FairType fairType = new FairType();
      fairType.append(new FontOptions(Settings.tooltipTextSize).outline(), tip);
      FairTypeTooltip fairTypeTooltip = new FairTypeTooltip(fairType.getDrawOptions(FairType.TextAlign.CENTER));
      GameTooltipManager.addTooltip(fairTypeTooltip, GameBackground.itemTooltip, TooltipLocation.INTERACT_FOCUS);
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
      GameSprite sprite = new GameSprite(this.texture, 0, 0, 96, 86, 96, 86);
      final DrawOptions options = this.getMultiTextureDrawOptions(sprite, level, tileX, tileY, camera);
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            options.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      GameSprite sprite = new GameSprite(this.texture, 0, 0, 96, 86, 96, 86);
      this.drawMultiTexturePreview(sprite, tileX, tileY, alpha, camera);
   }

   public static int[] registerFreyaAndZithaStatue(String texturePath, boolean isObtainable) {
      int[] ids = new int[6];
      Rectangle collision = new Rectangle(8, 10, 80, 54);
      ids[0] = ObjectRegistry.registerObject(texturePath, new FreyaAndZithaStatueObject(texturePath, 0, 0, 3, 2, ids, collision), 1000.0F, isObtainable);
      ids[1] = ObjectRegistry.registerObject(texturePath + "2", new FreyaAndZithaStatueObject(texturePath, 1, 0, 3, 2, ids, collision), 0.0F, false);
      ids[2] = ObjectRegistry.registerObject(texturePath + "3", new FreyaAndZithaStatueObject(texturePath, 2, 0, 3, 2, ids, collision), 0.0F, false);
      ids[3] = ObjectRegistry.registerObject(texturePath + "4", new FreyaAndZithaStatueObject(texturePath, 0, 1, 3, 2, ids, collision), 0.0F, false);
      ids[4] = ObjectRegistry.registerObject(texturePath + "5", new FreyaAndZithaStatueObject(texturePath, 1, 1, 3, 2, ids, collision), 0.0F, false);
      ids[5] = ObjectRegistry.registerObject(texturePath + "6", new FreyaAndZithaStatueObject(texturePath, 2, 1, 3, 2, ids, collision), 0.0F, false);
      return ids;
   }
}
