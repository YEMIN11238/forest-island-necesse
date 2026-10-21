package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class CoolingTowerObject extends StaticMultiObject {
   protected final GameRandom drawRandom;
   protected ParticleTypeSwitcher particleTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC
   );

   public CoolingTowerObject(int multiX, int multiY, int multiWidth, int multiHeight, int[] multiIDs, Rectangle fullCollision) {
      super(multiX, multiY, multiWidth, multiHeight, multiIDs, fullCollision, "coolingtower");
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
   public void tickEffect(Level level, int layerID, int tileX, int tileY) {
      if (this.multiX == 0 && this.multiY == 0) {
         level.entityManager
            .addTopParticle(tileX * 32 + 64 + GameRandom.globalRandom.getIntBetween(-15, 15), tileY * 32 - 32, this.particleTypeSwitcher.next())
            .sprite(GameResources.smokePuff.sprite(GameRandom.globalRandom.getIntBetween(0, 4), 0, 32))
            .color(new Color(205, 210, 218))
            .sizeFades(16, 32)
            .movesFriction(0.0F, -10.0F, 0.7F)
            .moves(
               (pos, delta, lifeTime, timeAlive, lifePercent) -> {
                  pos.x = pos.x
                     + (
                        GameRandom.globalRandom.nextBoolean()
                           ? (float)(Math.sin(timeAlive / 100.0F) * delta) * 0.1F
                           : (float)(Math.cos(timeAlive / 100.0F) * delta) * 0.1F
                     );
                  pos.y -= (2.0F - lifePercent) * delta * 0.05F;
               }
            )
            .fadesAlphaTime(100, 200)
            .lifeTime(1000 + this.drawRandom.nextInt(1000));
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
         sectionX = this.drawRandom.seeded(getTileSeed(tileX - this.multiX, tileY - this.multiY)).nextInt(texture.getWidth() / 128) * 128;
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
         sectionX = this.drawRandom.seeded(getTileSeed(tileX - this.multiX, tileY - this.multiY)).nextInt(this.texture.getWidth() / 128) * 128;
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

   public static int[] registerCoolingTower() {
      int[] ids = new int[8];
      Rectangle collision = new Rectangle(16, 0, 96, 48);
      ids[0] = ObjectRegistry.registerObject("coolingtower", new CoolingTowerObject(0, 0, 4, 2, ids, collision), 0.0F, true);
      ids[1] = ObjectRegistry.registerObject("coolingtower2", new CoolingTowerObject(1, 0, 4, 2, ids, collision), 0.0F, false);
      ids[2] = ObjectRegistry.registerObject("coolingtower3", new CoolingTowerObject(2, 0, 4, 2, ids, collision), 0.0F, false);
      ids[3] = ObjectRegistry.registerObject("coolingtower4", new CoolingTowerObject(3, 0, 4, 2, ids, collision), 0.0F, false);
      ids[4] = ObjectRegistry.registerObject("coolingtower5", new CoolingTowerObject(0, 1, 4, 2, ids, collision), 0.0F, false);
      ids[5] = ObjectRegistry.registerObject("coolingtower6", new CoolingTowerObject(1, 1, 4, 2, ids, collision), 0.0F, false);
      ids[6] = ObjectRegistry.registerObject("coolingtower7", new CoolingTowerObject(2, 1, 4, 2, ids, collision), 0.0F, false);
      ids[7] = ObjectRegistry.registerObject("coolingtower8", new CoolingTowerObject(3, 1, 4, 2, ids, collision), 0.0F, false);
      return ids;
   }
}
