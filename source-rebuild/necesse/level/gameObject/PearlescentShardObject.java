package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;

public class PearlescentShardObject extends GameObject {
   protected String textureName;
   public GameTexture texture;
   protected final GameRandom drawRandom;

   public PearlescentShardObject(String textureName, Color mapColor) {
      super(new Rectangle(0, -14, 32, 46));
      this.textureName = textureName;
      this.mapColor = mapColor;
      this.isIncursionExtractionObject = true;
      this.isOre = true;
      this.displayMapTooltip = true;
      this.isLightTransparent = true;
      this.drawRandom = new GameRandom();
      this.canPlaceOnLiquid = true;
      this.setItemCategory("objects", "landscaping", "incursionrocksandores");
      this.setCraftingCategory("objects", "landscaping", "incursionrocksandores");
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
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      return level.objectLayer.isPlayerPlaced(tileX, tileY)
         ? super.getLootTable(level, layerID, tileX, tileY)
         : new LootTable(LootItem.between("pearlescentdiamond", 1, 2));
   }

   @Override
   public void tickEffect(Level level, int layerID, int tileX, int tileY) {
      super.tickEffect(level, layerID, tileX, tileY);
      this.spawnShardParticles(level, GameMath.getLevelCoordinate(tileX) + 16, GameMath.getLevelCoordinate(tileY) - 32);
      float hueMod = (float)level.getWorldEntity().getLocalTime() / 50.0F % 240.0F;
      float glowHue = hueMod < 120.0F ? hueMod + 200.0F : 440.0F - hueMod;
      level.lightManager.refreshParticleLight(tileX, tileY, glowHue, 1.0F, 200);
   }

   public float getDesiredHeight(Level level, int tileX, int tileY) {
      int seededOffset = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(3000);
      float perc = GameUtils.getAnimFloat(level.getWorldEntity().getTime() + seededOffset, 3000);
      return GameMath.sin(perc * 360.0F) * 5.0F;
   }

   public void spawnShardParticles(Level level, int x, int y) {
      GameRandom random = GameRandom.globalRandom;
      AtomicReference<Float> currentAngle = new AtomicReference<>(random.nextFloat() * 360.0F);
      float distance = 30.0F;
      if (random.getChance(0.05F)) {
         level.entityManager
            .addParticle(x + GameMath.sin(currentAngle.get()) * distance, y + GameMath.cos(currentAngle.get()) * distance, Particle.GType.CRITICAL)
            .sprite(GameResources.pearlescentShardParticles.sprite(random.nextInt(4), 0, 18, 24))
            .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
               float angle = currentAngle.accumulateAndGet(delta * 5.0F / 250.0F, Float::sum);
               pos.x = x + GameMath.sin(angle) * distance;
               pos.y = y + 65 + (x - pos.x) - angle / 36.0F + GameMath.cos(angle) / 2.0F * distance;
            })
            .lifeTime(5000)
            .height(15.0F)
            .ignoreLight(true)
            .sizeFadesInAndOut(18, 24, 500, 500);
         level.entityManager
            .addParticle(x + GameMath.sin(currentAngle.get()) * distance, y + GameMath.cos(currentAngle.get()) * distance, Particle.GType.CRITICAL)
            .sprite(GameResources.pearlescentShardParticles.sprite(random.nextInt(4), 0, 18, 24))
            .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
               float angle = currentAngle.accumulateAndGet(delta * 5.0F / 250.0F, Float::sum);
               pos.x = x + GameMath.sin(angle) * distance;
               pos.y = y + 65 - (x - pos.x) - angle / 36.0F + GameMath.cos(angle) / 2.0F * distance;
            })
            .lifeTime(5000)
            .height(15.0F)
            .ignoreLight(true)
            .sizeFadesInAndOut(18, 24, 1000, 1000);
      }
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
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int sprite;
      int height;
      synchronized (this.drawRandom) {
         sprite = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(this.texture.getWidth() / 32);
         height = (int)this.getDesiredHeight(level, tileX, tileY);
      }

      final TextureDrawOptions options = this.texture
         .initDraw()
         .sprite(sprite, 0, 32, this.texture.getHeight())
         .pos(drawX, drawY - this.texture.getHeight() + 32 + height);
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
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int sprite;
      int height;
      synchronized (this.drawRandom) {
         sprite = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(this.texture.getWidth() / 32);
         height = (int)this.getDesiredHeight(level, tileX, tileY);
      }

      this.texture.initDraw().sprite(sprite, 0, 32, this.texture.getHeight()).alpha(alpha).draw(drawX, drawY - this.texture.getHeight() + 32 + height);
   }
}
