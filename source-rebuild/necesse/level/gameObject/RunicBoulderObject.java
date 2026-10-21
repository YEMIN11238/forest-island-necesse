package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
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

public class RunicBoulderObject extends StaticMultiObject {
   private GameTexture lightTexture;
   private GameTexture shadowTexture;
   protected final GameRandom drawRandom;

   public RunicBoulderObject(int multiX, int multiY, int multiWidth, int multiHeight, int[] multiIDs, Rectangle fullCollision) {
      super(multiX, multiY, multiWidth, multiHeight, multiIDs, fullCollision, "runicboulder");
      this.stackSize = 200;
      this.rarity = Item.Rarity.UNCOMMON;
      this.mapColor = new Color(136, 214, 255);
      this.objectHealth = 300;
      this.toolType = ToolType.PICKAXE;
      this.toolTier = 1.0F;
      this.isLightTransparent = true;
      this.drawRandom = new GameRandom();
      this.hoverHitbox = new Rectangle(0, -32, 32, 64);
      this.setItemCategory("objects", "landscaping", "plainsrocksandores");
      this.setCraftingCategory("objects", "landscaping", "plainsrocksandores");
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.lightTexture = GameTexture.fromFile("objects/" + this.texturePath + "_light");
      this.shadowTexture = GameTexture.fromFile("objects/" + this.texturePath + "_shadow");
   }

   public float getDesiredHeight(Level level, int tileX, int tileY) {
      int seededOffset = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(3000);
      float perc = GameUtils.getAnimFloat(level.getWorldEntity().getTime() + seededOffset, 3000);
      return GameMath.sin(perc * 360.0F) * 5.0F;
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      return level.objectLayer.isPlayerPlaced(tileX, tileY)
         ? super.getLootTable(level, layerID, tileX, tileY)
         : new LootTable(new LootItem("runestone", GameRandom.globalRandom.getIntBetween(1, 2)));
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
      final DrawOptions shadowOptions = this.getMultiLightTextureDrawOptions(this.shadowTexture, level, tileX, tileY, camera, false, true);
      final DrawOptions options = this.getMultiLightTextureDrawOptions(this.texture, level, tileX, tileY, camera, false, false);
      final DrawOptions lightOptions = this.getMultiLightTextureDrawOptions(this.lightTexture, level, tileX, tileY, camera, true, false);
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            shadowOptions.draw();
            options.draw();
            lightOptions.draw();
         }
      });
   }

   private DrawOptions getMultiLightTextureDrawOptions(
      GameTexture texture, Level level, int tileX, int tileY, GameCamera camera, boolean isLight, boolean isShadow
   ) {
      GameLight light = isLight ? level.getLightLevel(tileX, tileY).minLevelCopy(150.0F) : level.getLightLevel(tileX, tileY);
      int height = isShadow ? 0 : (int)this.getDesiredHeight(level, tileX - this.multiX, tileY - this.multiY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int sectionX;
      synchronized (this.drawRandom) {
         sectionX = this.drawRandom.seeded(getTileSeed(tileX - this.multiX, tileY - this.multiY)).nextInt(texture.getWidth() / 64) * 64;
      }

      int yOffset = texture.getHeight() - this.multiHeight * 32 + height;
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
      int height = (int)this.getDesiredHeight(level, tileX - this.multiX, tileY - this.multiY);
      int sectionX;
      synchronized (this.drawRandom) {
         sectionX = this.drawRandom.seeded(getTileSeed(tileX - this.multiX, tileY - this.multiY)).nextInt(this.texture.getWidth() / 64) * 64;
      }

      int yOffset = this.texture.getHeight() - this.multiHeight * 32 + height;
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

   public static int[] registerRunicBoulder() {
      int[] ids = new int[4];
      Rectangle collision = new Rectangle(12, 16, 40, 48);
      ids[0] = ObjectRegistry.registerObject("runicboulder", new RunicBoulderObject(0, 0, 2, 2, ids, collision), 0.0F, true);
      ids[1] = ObjectRegistry.registerObject("runicboulder2", new RunicBoulderObject(1, 0, 2, 2, ids, collision), 0.0F, false);
      ids[2] = ObjectRegistry.registerObject("runicboulder3", new RunicBoulderObject(0, 1, 2, 2, ids, collision), 0.0F, false);
      ids[3] = ObjectRegistry.registerObject("runicboulder4", new RunicBoulderObject(1, 1, 2, 2, ids, collision), 0.0F, false);
      return ids;
   }
}
