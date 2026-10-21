package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.HuginStatueObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;
import necesse.level.maps.multiTile.MultiTile;

public class HuginStatueObject extends StaticMultiObject {
   public HuginStatueObject(int multiX, int multiY, int multiWidth, int multiHeight, int[] multiIDs, Rectangle fullCollision) {
      super(multiX, multiY, multiWidth, multiHeight, multiIDs, fullCollision, "statues/hugin");
      this.stackSize = 1;
      this.rarity = Item.Rarity.UNIQUE;
      this.mapColor = new Color(0, 27, 64);
      this.displayMapTooltip = true;
      this.objectHealth = 1000;
      this.toolType = ToolType.NONE;
      this.isLightTransparent = true;
      this.hoverHitbox = new Rectangle(0, -48, 32, 80);
      this.setItemCategory("objects", "misc");
      this.setCraftingCategory("objects", "landscaping", "misc");
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
         HuginStatueObjectEntity objectEntity = this.getCurrentObjectEntity(level, x, y, HuginStatueObjectEntity.class);
         if (objectEntity != null) {
            objectEntity.activateStatue();
         }
      }
   }

   @Override
   public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
      return new HuginStatueObjectEntity(level, x, y);
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
      int spriteX = GameUtils.getAnim(level.getTime(), 4, 1000);
      GameSprite sprite = new GameSprite(this.texture, spriteX, 0, 64, 92, 64, 92);
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
      int spriteX = GameUtils.getAnim(level.getTime(), 4, 1000);
      GameSprite sprite = new GameSprite(this.texture, spriteX, 0, 64, 92, 64, 92);
      this.drawMultiTexturePreview(sprite, tileX, tileY, alpha, camera);
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(new StringTooltips(Localization.translate("ui", "valheimhuginspeak1"), new Color(243, 134, 25)));
      tooltips.add(GameUtils.getDevQuoteTooltip("A tribute to our friends from the Valheim team", null));
      return tooltips;
   }

   public static int[] registerHuginStatue(String stringID, boolean isObtainable) {
      int[] ids = new int[2];
      Rectangle collision = new Rectangle(0, 0, 64, 32);
      ids[0] = ObjectRegistry.registerObject(stringID, new HuginStatueObject(0, 0, 2, 1, ids, collision), 0.0F, isObtainable);
      ids[1] = ObjectRegistry.registerObject(stringID + "2", new HuginStatueObject(1, 0, 2, 1, ids, collision), 0.0F, false);
      return ids;
   }
}
