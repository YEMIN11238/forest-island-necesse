package necesse.entity.mobs.friendly.critters.croppler;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.entity.Entity;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobTexture;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.critters.CritterMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public abstract class CaveCroppler extends CritterMob {
   public CaveCroppler() {
      this.setSpeed(30.0F);
      this.setFriction(3.0F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-12, -14, 24, 24);
      this.selectBox = new Rectangle(-16, -28, 32, 34);
      this.swimMaskMove = 8;
      this.swimMaskOffset = 8;
      this.swimSinkOffset = 0;
   }

   protected abstract MobTexture getCropplerTexture();

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.beetCroppler.body, 12, i, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   public int getRockSpeed() {
      return 10;
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return new SoundSettings(GameResources.caveCropplerAmbient);
   }

   @Override
   protected SoundSettings getDeathSound() {
      return new SoundSettings(GameResources.caveCropplerDeath).volume(0.4F);
   }

   @Override
   public void addDrawables(
      List<MobDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      Level level,
      int x,
      int y,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
      GameLight light = level.getLightLevel(Entity.getTileCoordinate(x), Entity.getTileCoordinate(y));
      int drawX = camera.getDrawX(x) - 22 - 10;
      int drawY = camera.getDrawY(y) - 44 - 7 - 6;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(Entity.getTileCoordinate(x), Entity.getTileCoordinate(y)).getMobSinkingAmount(this);
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions drawOptions = this.getCropplerTexture()
         .body
         .initDraw()
         .sprite(sprite.x, sprite.y, 64)
         .addMaskShader(swimMask)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            swimMask.use();
            drawOptions.draw();
            swimMask.stop();
         }
      });
      this.addShadowDrawables(tileList, level, x, y - 5, light, camera);
   }
}
