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
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.PraiseTheMoonStatueObjectEntity;
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
import necesse.level.maps.LevelObject;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.multiTile.MultiTile;

public class PraiseTheMoonStatueObject extends StaticMultiObject {
   public PraiseTheMoonStatueObject(int multiX, int multiY, int multiWidth, int multiHeight, int[] multiIDs, Rectangle fullCollision) {
      super(multiX, multiY, multiWidth, multiHeight, multiIDs, fullCollision, "statues/praisethemoonstatue");
      this.stackSize = 1;
      this.rarity = Item.Rarity.UNCOMMON;
      this.mapColor = new Color(99, 159, 153);
      this.displayMapTooltip = true;
      this.objectHealth = 1000;
      this.toolType = ToolType.PICKAXE;
      this.isLightTransparent = true;
      this.hoverHitbox = new Rectangle(0, -48, 32, 80);
      this.setItemCategory("objects", "misc");
      this.setCraftingCategory("objects", "landscaping", "misc");
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips itemTooltips = super.getItemTooltips(item, perspective);
      itemTooltips.add(GameUtils.getDevQuoteTooltip("itemtooltip", "praisethemoonstatuequote", "Ollie"));
      return itemTooltips;
   }

   @Override
   public boolean canInteract(Level level, int x, int y, PlayerMob player) {
      return true;
   }

   @Override
   public void interact(Level level, int x, int y, PlayerMob player) {
      super.interact(level, x, y, player);
      MultiTile multiTile = this.getMultiTile(level, 0, x, y);
      if (!multiTile.isMaster) {
         multiTile.getMasterLevelObject(level, 0, x, y).ifPresent(e -> e.interact(player));
      } else if (player != null && player.isServerClient()) {
         if (!player.isOnGenericCooldown("praisethemooninteract")) {
            player.startGenericCooldown("praisethemooninteract", 2000L);
            PraiseTheMoonStatueObjectEntity objectEntity = this.getCurrentObjectEntity(level, x, y, PraiseTheMoonStatueObjectEntity.class);
            if (objectEntity != null) {
               objectEntity.activateStatue(player.getServerClient());
            }
         }
      }
   }

   @Override
   public void onMouseHover(Level level, int x, int y, GameCamera camera, PlayerMob perspective, boolean debug) {
      super.onMouseHover(level, x, y, camera, perspective, debug);
      String tip = Localization.translate("ui", "praisethemooninteracttip1")
         + "\n"
         + Localization.translate("ui", "praisethemooninteracttip2")
         + "\n"
         + Localization.translate("ui", "praisethemooninteracttip3")
         + "\n\n"
         + Localization.translate("ui", "praisethemooninteracttip4")
         + "\n"
         + Localization.translate("ui", "praisethemooninteracttip5");
      FairType fairType = new FairType();
      fairType.append(new FontOptions(Settings.tooltipTextSize).outline(), tip);
      FairTypeTooltip fairTypeTooltip = new FairTypeTooltip(fairType.getDrawOptions(FairType.TextAlign.CENTER));
      GameTooltipManager.addTooltip(fairTypeTooltip, GameBackground.itemTooltip, TooltipLocation.INTERACT_FOCUS);
   }

   @Override
   public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
      return new PraiseTheMoonStatueObjectEntity(level, x, y);
   }

   public float getAlpha(Level level, int tileX, int tileY, long currentTime) {
      LevelObject master = this.getMultiTile(level.getObjectRotation(tileX, tileY)).getMasterLevelObject(level, 0, tileX, tileY).orElse(null);
      if (master != null) {
         PraiseTheMoonStatueObjectEntity objectEntity = this.getCurrentObjectEntity(level, master.tileX, master.tileY, PraiseTheMoonStatueObjectEntity.class);
         if (objectEntity != null) {
            return objectEntity.getGlowAlpha(currentTime);
         }
      }

      return 0.0F;
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
      float alpha = this.getAlpha(level, tileX, tileY, level.getTime());
      GameSprite sprite = new GameSprite(this.texture, 0, 0, 96, 160, 96, 160);
      GameSprite overlaySprite = new GameSprite(this.texture, 1, 0, 96, 160, 96, 160);
      final DrawOptions options = this.getMultiTextureDrawOptions(sprite, level, tileX, tileY, camera);
      final DrawOptions overlayOptions = this.getMultiTextureDrawOptions(
         overlaySprite, level, tileX, tileY, camera, drawOptions -> drawOptions.colorLight(Color.WHITE, new GameLight(alpha * 255.0F)).alpha(alpha)
      );
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 32;
         }

         @Override
         public void draw(TickManager tickManager) {
            options.draw();
            overlayOptions.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      GameSprite sprite = new GameSprite(this.texture, 0, 0, 96, 160, 96, 160);
      this.drawMultiTexturePreview(sprite, tileX, tileY, alpha, camera);
   }

   public static int[] registerPraiseTheMoonStatue(String stringID, boolean isObtainable, boolean countInStats) {
      int[] ids = new int[6];
      Rectangle collision = new Rectangle(16, 16, 64, 48);
      ids[0] = ObjectRegistry.registerObject(stringID, new PraiseTheMoonStatueObject(0, 0, 3, 2, ids, collision), 825.0F, isObtainable, countInStats);
      ids[1] = ObjectRegistry.registerObject(stringID + "2", new PraiseTheMoonStatueObject(1, 0, 3, 2, ids, collision), 0.0F, false, false);
      ids[2] = ObjectRegistry.registerObject(stringID + "3", new PraiseTheMoonStatueObject(2, 0, 3, 2, ids, collision), 0.0F, false, false);
      ids[3] = ObjectRegistry.registerObject(stringID + "4", new PraiseTheMoonStatueObject(0, 1, 3, 2, ids, collision), 0.0F, false, false);
      ids[4] = ObjectRegistry.registerObject(stringID + "5", new PraiseTheMoonStatueObject(1, 1, 3, 2, ids, collision), 0.0F, false, false);
      ids[5] = ObjectRegistry.registerObject(stringID + "6", new PraiseTheMoonStatueObject(2, 1, 3, 2, ids, collision), 0.0F, false, false);
      return ids;
   }
}
