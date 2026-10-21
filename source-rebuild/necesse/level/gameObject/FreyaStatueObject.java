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

public class FreyaStatueObject extends StaticMultiObject {
   protected FreyaStatueObject(String texturePath, int multiX, int multiY, int multiWidth, int multiHeight, int[] multiIDs, Rectangle fullCollision) {
      super(multiX, multiY, multiWidth, multiHeight, multiIDs, fullCollision, "statues/" + texturePath);
      this.stackSize = 1;
      this.rarity = Item.Rarity.EPIC;
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
      itemTooltips.add(GameUtils.getDevQuoteTooltip("itemtooltip", "freyastatuequote", null));
      return itemTooltips;
   }

   @Override
   public void onMouseHover(Level level, int x, int y, GameCamera camera, PlayerMob perspective, boolean debug) {
      super.onMouseHover(level, x, y, camera, perspective, debug);
      String tip = Localization.translate("ui", "freyastatueplaque1")
         + "\n"
         + Localization.translate("ui", "freyastatueplaque2")
         + "\n"
         + Localization.translate("ui", "freyastatueplaque3");
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
      GameSprite sprite = new GameSprite(this.texture, 0, 0, 64, 86, 64, 86);
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
      GameSprite sprite = new GameSprite(this.texture, 0, 0, 64, 86, 64, 86);
      this.drawMultiTexturePreview(sprite, tileX, tileY, alpha, camera);
   }

   public static int[] registerFreyaStatue(String texturePath, boolean isObtainable) {
      int[] ids = new int[4];
      Rectangle collision = new Rectangle(8, 10, 48, 54);
      ids[0] = ObjectRegistry.registerObject(texturePath, new FreyaStatueObject(texturePath, 0, 0, 2, 2, ids, collision), 600.0F, isObtainable);
      ids[1] = ObjectRegistry.registerObject(texturePath + "2", new FreyaStatueObject(texturePath, 1, 0, 2, 2, ids, collision), 0.0F, false);
      ids[2] = ObjectRegistry.registerObject(texturePath + "3", new FreyaStatueObject(texturePath, 0, 1, 2, 2, ids, collision), 0.0F, false);
      ids[3] = ObjectRegistry.registerObject(texturePath + "4", new FreyaStatueObject(texturePath, 1, 1, 2, 2, ids, collision), 0.0F, false);
      return ids;
   }
}
