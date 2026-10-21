package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.LootItemList;
import necesse.inventory.lootTable.lootItem.LootItemMultiplierIgnored;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class KatanaStoneObject extends GameObject {
   protected GameTexture texture;
   protected final GameRandom drawRandom;

   public KatanaStoneObject() {
      super(new Rectangle(4, 8, 24, 16));
      this.mapColor = new Color(192, 216, 226);
      this.toolType = ToolType.ALL;
      this.setItemCategory("objects", "landscaping", "masonry");
      this.setCraftingCategory("objects", "landscaping", "masonry");
      this.displayMapTooltip = true;
      this.objectHealth = 100;
      this.isLightTransparent = true;
      this.drawRandom = new GameRandom();
      this.hoverHitbox = new Rectangle(0, -16, 32, 48);
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      return new LootTable(new LootItemMultiplierIgnored(new LootItemList(new LootItem("stone", 20).splitItems(5), new LootItem("katana"))));
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/katanastone");
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
      int sprites = Math.max(1, this.texture.getWidth() / 64);
      int animTime = 200;
      int timeOffset;
      boolean mirrored;
      synchronized (this.drawRandom) {
         timeOffset = this.drawRandom.seeded(getTileSeed(tileX, tileY, 10)).nextInt(animTime * sprites);
         mirrored = this.drawRandom.seeded(getTileSeed(tileX, tileY, 5)).nextBoolean();
      }

      int sprite = GameUtils.getAnim(level.getTime() + timeOffset, sprites, animTime * sprites);
      final TextureDrawOptionsEnd drawOptions = this.texture
         .initDraw()
         .sprite(sprite, 0, 64)
         .mirror(mirrored, false)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyTreasure(perspective)
         .pos(drawX - (mirrored ? 32 : 0), drawY - (this.texture.getHeight() - 32));
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int sprites = this.texture.getWidth() / 64;
      int animTime = 200;
      int timeOffset;
      boolean mirrored;
      synchronized (this.drawRandom) {
         timeOffset = this.drawRandom.seeded(getTileSeed(tileX, tileY, 10)).nextInt(animTime * sprites);
         mirrored = this.drawRandom.seeded(getTileSeed(tileX, tileY, 5)).nextBoolean();
      }

      int sprite = GameUtils.getAnim(level.getTime() + timeOffset, sprites, animTime * sprites);
      this.texture
         .initDraw()
         .sprite(sprite, 0, 64)
         .mirror(mirrored, false)
         .alpha(alpha)
         .draw(drawX - (mirrored ? 32 : 0), drawY - (this.texture.getHeight() - 32));
   }

   @Override
   public boolean shouldSnapSmartMining(Level level, int x, int y) {
      return true;
   }

   @Override
   public boolean isValid(Level level, int layerID, int x, int y) {
      return !level.isShore(x, y);
   }
}
