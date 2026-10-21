package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class ScrapheapBigObject extends StaticMultiObject {
   protected final GameRandom drawRandom;

   public ScrapheapBigObject(int multiX, int multiY, int multiWidth, int multiHeight, int[] multiIDs, Rectangle fullCollision) {
      super(multiX, multiY, multiWidth, multiHeight, multiIDs, fullCollision, "scrapheap_big");
      this.stackSize = 200;
      this.rarity = Item.Rarity.COMMON;
      this.mapColor = new Color(91, 92, 105);
      this.objectHealth = 200;
      this.toolType = ToolType.PICKAXE;
      this.toolTier = 10.0F;
      this.isLightTransparent = true;
      this.drawRandom = new GameRandom();
      this.hoverHitbox = new Rectangle(0, -32, 32, 64);
      this.setItemCategory("objects", "landscaping", "incursionrocksandores");
      this.setCraftingCategory("objects", "landscaping", "incursionrocksandores");
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      return level.objectLayer.isPlayerPlaced(tileX, tileY)
         ? super.getLootTable(level, layerID, tileX, tileY)
         : new LootTable(new LootItem("junk", GameRandom.globalRandom.getIntBetween(10, 20)));
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
      final DrawOptions options = this.getMultiLightTextureDrawOptions(this.texture, level, tileX, tileY, camera);
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

   private DrawOptions getMultiLightTextureDrawOptions(GameTexture texture, Level level, int tileX, int tileY, GameCamera camera) {
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int sectionX;
      synchronized (this.drawRandom) {
         sectionX = this.drawRandom.seeded(getTileSeed(tileX - this.multiX, tileY - this.multiY)).nextInt(texture.getWidth() / 96) * 96;
      }

      int yOffset = texture.getHeight() - this.multiHeight * 32;
      if (this.multiY == 0) {
         return texture.initDraw()
            .section(sectionX + this.multiX * 32, sectionX + 32 + this.multiX * 32, 0, 32 + yOffset)
            .size(32, 32 + yOffset)
            .light(light)
            .addObjectDamageOverlay(this, level, tileX, tileY)
            .pos(drawX, drawY - yOffset);
      }

      int startY = this.multiY * 32 + yOffset;
      return texture.initDraw()
         .section(sectionX + this.multiX * 32, sectionX + 32 + this.multiX * 32, startY, startY + 32)
         .size(32, 32)
         .light(light)
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .pos(drawX, drawY);
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int sectionX;
      synchronized (this.drawRandom) {
         sectionX = this.drawRandom.seeded(getTileSeed(tileX - this.multiX, tileY - this.multiY)).nextInt(this.texture.getWidth() / 96) * 96;
      }

      int yOffset = this.texture.getHeight() - this.multiHeight * 32;
      if (this.multiY == 0) {
         this.texture
            .initDraw()
            .section(sectionX + this.multiX * 32, sectionX + 32 + this.multiX * 32, 0, 32 + yOffset)
            .size(32, 32 + yOffset)
            .alpha(alpha)
            .addObjectDamageOverlay(this, level, tileX, tileY)
            .draw(drawX, drawY - yOffset);
      } else {
         int startY = this.multiY * 32 + yOffset;
         this.texture
            .initDraw()
            .section(sectionX + this.multiX * 32, sectionX + 32 + this.multiX * 32, startY, startY + 32)
            .size(32, 32)
            .alpha(alpha)
            .addObjectDamageOverlay(this, level, tileX, tileY)
            .draw(drawX, drawY);
      }
   }

   public static int[] registerScrapHeapBig() {
      int[] ids = new int[6];
      Rectangle collision = new Rectangle(16, 0, 64, 48);
      ids[0] = ObjectRegistry.registerObject("scrapheapbig", new ScrapheapBigObject(0, 0, 3, 2, ids, collision), 0.0F, true);
      ids[1] = ObjectRegistry.registerObject("scrapheapbig2", new ScrapheapBigObject(1, 0, 3, 2, ids, collision), 0.0F, false);
      ids[2] = ObjectRegistry.registerObject("scrapheapbig3", new ScrapheapBigObject(2, 0, 3, 2, ids, collision), 0.0F, false);
      ids[3] = ObjectRegistry.registerObject("scrapheapbig4", new ScrapheapBigObject(0, 1, 3, 2, ids, collision), 0.0F, false);
      ids[4] = ObjectRegistry.registerObject("scrapheapbig5", new ScrapheapBigObject(1, 1, 3, 2, ids, collision), 0.0F, false);
      ids[5] = ObjectRegistry.registerObject("scrapheapbig6", new ScrapheapBigObject(2, 1, 3, 2, ids, collision), 0.0F, false);
      return ids;
   }
}
