package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class CrystalClusterSmallObject extends GameObject {
   protected String dropItem;
   protected int minDropAmount;
   protected int maxDropAmount;
   protected int placedDropAmount;
   protected String textureName;
   public GameTexture texture;
   protected final GameRandom drawRandom;

   public CrystalClusterSmallObject(
      String textureName, Color mapColor, float glowHue, String dropItem, int minDropAmount, int maxDropAmount, int placedDropAmount, String... category
   ) {
      super(new Rectangle(4, 16, 24, 12));
      this.textureName = textureName;
      this.mapColor = mapColor;
      this.dropItem = dropItem;
      this.minDropAmount = minDropAmount;
      this.maxDropAmount = maxDropAmount;
      this.placedDropAmount = placedDropAmount;
      this.displayMapTooltip = true;
      this.isLightTransparent = true;
      this.lightLevel = 75;
      this.lightSat = 1.0F;
      this.lightHue = glowHue;
      this.drawRandom = new GameRandom();
      this.canPlaceOnLiquid = false;
      this.attackThrough = true;
      this.objectHealth = 10;
      if (category.length > 0) {
         this.setItemCategory(category);
         this.setCraftingCategory(category);
      } else {
         this.setItemCategory("objects", "landscaping", "rocksandores");
         this.setCraftingCategory("objects", "landscaping", "rocksandores");
      }
   }

   @Override
   public GameMessage getNewLocalization() {
      return new LocalMessage("object", this.textureName);
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/" + this.textureName);
   }

   @Override
   public void attackThrough(Level level, int x, int y, GameDamage damage, Attacker attacker) {
      if (!level.objectLayer.isPlayerPlaced(x, y)) {
         super.attackThrough(level, x, y, damage, attacker);
         this.playDamageSound(level, x, y, true);
      }
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      if (level.objectLayer.isPlayerPlaced(tileX, tileY)) {
         return super.getLootTable(level, layerID, tileX, tileY);
      } else {
         return this.dropItem != null && this.maxDropAmount > 0
            ? new LootTable(
               LootItem.between(this.dropItem, this.minDropAmount, GameRandom.globalRandom.getChance(0.5F) ? this.maxDropAmount : this.minDropAmount)
                  .splitItems(5)
            )
            : new LootTable();
      }
   }

   public int getRandomYOffset(int tileX, int tileY) {
      synchronized (this.drawRandom) {
         return (int)((this.drawRandom.seeded(getTileSeed(tileX, tileY, 1)).nextFloat() * 2.0F - 1.0F) * 8.0F) - 4;
      }
   }

   @Override
   protected Rectangle getCollision(Level level, int x, int y, int rotation) {
      Rectangle collision = super.getCollision(level, x, y, rotation);
      collision.y = collision.y + this.getRandomYOffset(x, y);
      return collision;
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
      final int randomYOffset = this.getRandomYOffset(tileX, tileY);
      int sprite;
      synchronized (this.drawRandom) {
         sprite = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(this.texture.getWidth() / 32);
      }

      drawY += randomYOffset;
      final TextureDrawOptions options = this.texture
         .initDraw()
         .sprite(sprite, 0, 32, this.texture.getHeight())
         .light(light.minLevelCopy(100.0F))
         .pos(drawX, drawY - this.texture.getHeight() + 32);
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16 + randomYOffset;
         }

         @Override
         public void draw(TickManager tickManager) {
            options.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int randomYOffset = this.getRandomYOffset(tileX, tileY);
      int sprite;
      synchronized (this.drawRandom) {
         sprite = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(this.texture.getWidth() / 32);
      }

      drawY += randomYOffset;
      this.texture
         .initDraw()
         .sprite(sprite, 0, 32, this.texture.getHeight())
         .light(light.minLevelCopy(100.0F))
         .alpha(alpha)
         .draw(drawX, drawY - this.texture.getHeight() + 32);
   }

   @Override
   public boolean isValid(Level level, int layerID, int x, int y) {
      return !level.isShore(x, y);
   }

   @Override
   public void playDamageSound(Level level, int x, int y, boolean damageDone) {
      SoundManager.playSound(GameResources.crystalHit1, SoundEffect.effect(x * 32 + 16, y * 32 + 16).pitch(GameRandom.globalRandom.getFloatBetween(0.9F, 1.1F)));
   }
}
