package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.MergeFunction;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SingleOreRockSmall extends GameObject {
   public String droppedStone;
   public long oreHash;
   public String rockLocalizationKey;
   public String oreLocalizationKey;
   public String rockTextureName;
   public String oreTextureName;
   public GameTexture rockTexture;
   public GameTexture oreTexture;
   public String droppedOre;
   public int droppedOreMin;
   public int droppedOreMax;
   public int placedDroppedOre;
   protected final GameRandom drawRandom;

   public SingleOreRockSmall(
      String droppedStone,
      float toolTier,
      String rockLocalizationKey,
      String oreLocalizationKey,
      String rockTextureName,
      String oreTextureName,
      Color oreColor,
      String droppedOre,
      int droppedOreMin,
      int droppedOreMax,
      int placedDroppedOre,
      boolean isIncursionExtractionObject,
      String... category
   ) {
      super(new Rectangle(2, 10, 28, 20));
      this.droppedStone = droppedStone;
      this.toolTier = toolTier;
      this.rockLocalizationKey = rockLocalizationKey;
      this.oreLocalizationKey = oreLocalizationKey;
      this.rockTextureName = rockTextureName;
      this.oreTextureName = oreTextureName;
      this.mapColor = oreColor;
      this.droppedOre = droppedOre;
      this.droppedOreMin = droppedOreMin;
      this.droppedOreMax = droppedOreMax;
      this.placedDroppedOre = placedDroppedOre;
      this.isIncursionExtractionObject = isIncursionExtractionObject;
      this.oreHash = oreColor.hashCode();
      this.isOre = true;
      this.displayMapTooltip = true;
      this.isLightTransparent = true;
      this.drawRandom = new GameRandom();
      this.canPlaceOnLiquid = true;
      this.hoverHitbox = new Rectangle(0, -32, 32, 64);
      if (category.length > 0) {
         this.setItemCategory(category);
         this.setCraftingCategory(category);
      } else {
         this.setItemCategory("objects", "landscaping", "rocksandores");
         this.setCraftingCategory("objects", "landscaping", "rocksandores");
      }
   }

   public SingleOreRockSmall(
      String droppedStone,
      float toolTier,
      String rockLocalizationKey,
      String oreLocalizationKey,
      String rockTextureName,
      String oreTextureName,
      Color oreColor,
      String droppedOre,
      int droppedOreMin,
      int droppedOreMax,
      int placedDroppedOre,
      String... category
   ) {
      this(
         droppedStone,
         toolTier,
         rockLocalizationKey,
         oreLocalizationKey,
         rockTextureName,
         oreTextureName,
         oreColor,
         droppedOre,
         droppedOreMin,
         droppedOreMax,
         placedDroppedOre,
         true,
         category
      );
   }

   @Override
   public GameMessage getNewLocalization() {
      return new LocalMessage(
         "object", "rockoreformat", "rock", new LocalMessage("object", this.rockLocalizationKey), "ore", new LocalMessage("object", this.oreLocalizationKey)
      );
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.rockTexture = GameTexture.fromFile("objects/" + this.rockTextureName);
      this.oreTexture = GameTexture.fromFile("objects/" + this.oreTextureName);
   }

   @Override
   public GameTexture generateItemTexture() {
      GameTexture rockTexture = new GameTexture(GameTexture.fromFile("items/" + this.rockTextureName));
      GameTexture oreTexture = GameTexture.fromFile("items/" + this.oreTextureName);
      rockTexture.merge(oreTexture, 0, 0, MergeFunction.NORMAL);
      rockTexture.makeFinal();
      return rockTexture;
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      if (level.objectLayer.isPlayerPlaced(tileX, tileY)) {
         return super.getLootTable(level, layerID, tileX, tileY);
      }

      LootTable lootTable = new LootTable(LootItem.between(this.droppedOre, this.droppedOreMin, this.droppedOreMax).splitItems(5));
      if (this.droppedStone != null) {
         lootTable.items.add(LootItem.between(this.droppedStone, 15, 25).splitItems(5));
      }

      return lootTable;
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
      GameLight oreLight = light;
      if (this.isIncursionExtractionObject && level.isIncursionLevel) {
         float anim = GameUtils.getAnimFloatContinuous(Math.abs(level.getTime() + 2500L * this.oreHash), 2500);
         oreLight = light.minLevelCopy(GameMath.lerp(anim, 80, 100));
      }

      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      final int randomYOffset = this.getRandomYOffset(tileX, tileY);
      int sprite;
      boolean mirror;
      synchronized (this.drawRandom) {
         sprite = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(this.rockTexture.getWidth() / 32);
         mirror = this.drawRandom.nextBoolean();
      }

      drawY += randomYOffset;
      final DrawOptionsList drawOptions = new DrawOptionsList();
      drawOptions.add(
         this.rockTexture
            .initDraw()
            .sprite(sprite, 0, 32, this.rockTexture.getHeight())
            .addObjectDamageOverlay(this, level, tileX, tileY)
            .mirror(mirror, false)
            .light(light)
            .pos(drawX, drawY - this.rockTexture.getHeight() + 32)
      );
      drawOptions.add(
         this.oreTexture
            .initDraw()
            .sprite(sprite, 0, 32, this.oreTexture.getHeight())
            .addObjectDamageOverlay(this, level, tileX, tileY)
            .mirror(mirror, false)
            .startGlowOptions(level, this.oreHash)
            .light(oreLight)
            .applySpelunker(perspective)
            .pos(drawX, drawY - this.oreTexture.getHeight() + 32)
      );
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16 + randomYOffset;
         }

         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      GameLight light = level.getLightLevel(tileX, tileY);
      GameLight oreLight = light;
      if (this.isIncursionExtractionObject && level.isIncursionLevel) {
         float anim = GameUtils.getAnimFloatContinuous(Math.abs(level.getTime() + 2500L * this.oreHash), 2500);
         oreLight = light.minLevelCopy(GameMath.lerp(anim, 80, 100));
      }

      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int randomYOffset = this.getRandomYOffset(tileX, tileY);
      int sprite;
      boolean mirror;
      synchronized (this.drawRandom) {
         sprite = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(this.rockTexture.getWidth() / 32);
         mirror = this.drawRandom.nextBoolean();
      }

      drawY += randomYOffset;
      this.rockTexture
         .initDraw()
         .sprite(sprite, 0, 32, this.rockTexture.getHeight())
         .mirror(mirror, false)
         .light(light)
         .alpha(alpha)
         .draw(drawX, drawY - this.rockTexture.getHeight() + 32);
      this.oreTexture
         .initDraw()
         .sprite(sprite, 0, 32, this.oreTexture.getHeight())
         .mirror(mirror, false)
         .startGlowOptions(level, this.oreHash)
         .light(oreLight)
         .applySpelunker(player)
         .alpha(alpha)
         .draw(drawX, drawY - this.oreTexture.getHeight() + 32);
   }

   @Override
   public boolean isValid(Level level, int layerID, int x, int y) {
      return !level.isShore(x, y);
   }
}
