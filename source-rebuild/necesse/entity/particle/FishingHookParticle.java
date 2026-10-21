package necesse.entity.particle;

import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.inventory.item.placeableItem.fishingRodItem.FishingRodItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class FishingHookParticle extends Particle {
   private static final float BLOC_DEC_PER_TICK = 0.025F;
   private final float initY;
   private float blob;
   private final GameSprite projectileSprite;
   private final GameSprite waterSprite;

   public FishingHookParticle(Level level, float x, float y, FishingRodItem fishingRod) {
      super(level, x, y, 5000L);
      this.initY = y;
      this.projectileSprite = fishingRod.getHookProjectileSprite();
      this.waterSprite = fishingRod.getHookParticleSprite();
   }

   @Override
   public void clientTick() {
      if (this.blob > 0.0F) {
         this.blob -= 0.025F;
      }

      if (this.blob < 0.0F) {
         this.blob = 0.0F;
      }

      int offset = this.getLevel().getLevelTile(this.getTileY(), getTileCoordinate(this.initY)).getLiquidBobbing();
      this.y = this.initY + this.blob / 4.0F + offset;
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      OrderableDrawables overlayList,
      Level level,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      GameLight light = level.getLightLevel(this);
      int drawX = this.getX() - camera.getX() - 16;
      int drawY = this.getY() - camera.getY() - 26;
      final TextureDrawOptions options;
      if (level.isLiquidTile(this.getTileX(), getTileCoordinate(this.initY))) {
         int blob = (int)(this.blob * 10.0F);
         options = this.waterSprite.initDrawSection(0, 32, 0, 32 - blob).light(light).size(32, 32 - blob).pos(drawX, drawY);
      } else {
         options = this.projectileSprite.initDraw().light(light).pos(drawX, drawY + 10);
      }

      list.add(new EntityDrawable(this) {
         @Override
         public void draw(TickManager tickManager) {
            options.draw();
         }
      });
   }

   public void blob() {
      this.blob = 1.0F;
   }
}
