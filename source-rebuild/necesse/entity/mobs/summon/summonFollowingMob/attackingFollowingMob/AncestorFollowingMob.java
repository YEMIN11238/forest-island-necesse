package necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob;

import java.awt.Color;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class AncestorFollowingMob extends AttackingFollowingMob {
   public int lifeTime = 15000;

   public AncestorFollowingMob() {
      super(10);
   }

   @Override
   public void clientTick() {
      super.clientTick();
      Level level = this.getLevel();
      if (level.tickManager().getTotalTicks() % 2L == 0L) {
         level.entityManager
            .addParticle(
               this.x + (float)(GameRandom.globalRandom.nextGaussian() * 6.0),
               this.y + (float)(GameRandom.globalRandom.nextGaussian() * 8.0),
               Particle.GType.COSMETIC
            )
            .movesConstant(this.dx / 10.0F, this.dy / 10.0F)
            .color(new Color(0, 222, 218))
            .alpha(0.5F)
            .sizeFades(6, 10)
            .givesLight(200.0F, 0.6F)
            .height(16.0F);
      }

      level.lightManager.refreshParticleLightFloat(this.x, this.y, 200.0F, 0.6F, 10);
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.lifeTime -= 50;
      if (this.lifeTime <= 0) {
         this.remove(0.0F, 0.0F, null, true);
      }
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      GameRandom random = GameRandom.globalRandom;
      float anglePerParticle = 36.0F;

      for (int i = 0; i < 10; i++) {
         int angle = (int)(i * anglePerParticle + random.nextFloat() * anglePerParticle);
         float dx = (float)Math.sin(Math.toRadians(angle)) * 20.0F;
         float dy = (float)Math.cos(Math.toRadians(angle)) * 20.0F;
         this.getLevel()
            .entityManager
            .addParticle(this, Particle.GType.IMPORTANT_COSMETIC)
            .sprite(GameResources.puffParticles.sprite(random.nextInt(5), 0, 12))
            .sizeFades(12, 24)
            .movesFriction(dx * 2.0F, dy * 2.0F, 0.8F)
            .color(new Color(0, 222, 218))
            .alpha(0.5F)
            .heightMoves(0.0F, 30.0F)
            .lifeTime(1500);
      }
   }

   @Override
   protected SoundSettings getHitDeathSound() {
      return new SoundSettings(GameResources.fadedeath3).volume(0.5F).basePitch(1.5F);
   }

   protected TextureDrawOptions getShadowDrawOptions(int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.human_baby_shadow;
      int res = shadowTexture.getHeight();
      int drawX = camera.getDrawX(x) - res / 2;
      int drawY = camera.getDrawY(y) - res / 2;
      drawY += this.getBobbing(x, y);
      int dir = this.getDir();
      return shadowTexture.initDraw().sprite(dir, 0, res).light(light).pos(drawX, drawY);
   }

   @Override
   public void showAttack(int x, int y, int seed, boolean showAllDirections) {
      super.showAttack(x, y, seed, showAllDirections);
      if (this.isClient()) {
         SoundManager.playSound(GameResources.swing1, SoundEffect.effect(this).volume(0.3F));
      }
   }
}
