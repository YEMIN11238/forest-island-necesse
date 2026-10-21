package necesse.entity.particle;

import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.engine.util.tween.Easings;
import necesse.engine.util.tween.FloatTween;
import necesse.engine.util.tween.PlayableSequence;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class ScrapBallParticle extends Particle {
   private final int startHeight = -100;
   protected PlayableSequence sequence = new PlayableSequence();
   protected FloatTween heightTween = new FloatTween(0.0F);
   private final Mob target;
   private final long startTime;
   private final int timeToAvoid;
   protected ParticleTypeSwitcher extraParticlesTypes = new ParticleTypeSwitcher(
      Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC
   );

   public ScrapBallParticle(Level level, Mob target, long lifeTime, int timeToAvoid) {
      super(level, target.getX(), target.getY(), lifeTime);
      this.target = target;
      this.startTime = level.getWorldEntity().getTime();
      this.timeToAvoid = timeToAvoid;
   }

   @Override
   public void init() {
      super.init();
      this.sequence.addAfterPrevious(this.heightTween.newTween(500.0, 100.0F).setEase(Easings.BackIn));
   }

   @Override
   public void clientTick() {
      super.clientTick();
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      if (this.target != null && this.getLevel().getWorldEntity().getTime() <= this.startTime + this.lifeTime - this.timeToAvoid) {
         this.x = (int)this.target.x;
         this.y = (int)this.target.y;
      }

      if (this.sequence.isRunning()) {
         this.sequence.update(this.getLocalTime());
      } else if (this.getLifeCyclePercent() > 0.75F) {
         this.sequence.play(this.getLocalTime(), this.getLocalTime());
      }
   }

   public int getParticleHeight() {
      return this.getLifeCyclePercent() > 0.75F ? (int)(-100.0F + this.heightTween.getValue()) : -100;
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
      GameLight light = level.getLightLevel(this.getTileX(), this.getTileY());
      int drawX = camera.getDrawX(this.getX()) - 32;
      int drawY = camera.getDrawY(this.getY()) - 32;
      int spriteSize = 64;
      float shadowSizeMod = Math.max(0.75F, this.getLifeCyclePercent());
      int shadowPosOffset = (int)((spriteSize - spriteSize * shadowSizeMod) / 2.0F);
      float rotation = this.getLifeCyclePercent() * 360.0F;
      float fadeIn = Math.min(this.getLifeCyclePercent() * 2.0F, 1.0F);
      DrawOptions options = GameResources.scrapBallParticles
         .initDraw()
         .sprite(0, 0, spriteSize)
         .rotate(rotation)
         .light(light)
         .pos(drawX, drawY + this.getParticleHeight())
         .color(fadeIn, fadeIn, fadeIn, fadeIn);
      DrawOptions shadowOptions = GameResources.scrapBallParticles
         .initDraw()
         .sprite(1, 0, spriteSize)
         .size((int)(spriteSize * shadowSizeMod), (int)(spriteSize * shadowSizeMod))
         .rotate(rotation)
         .light(light)
         .pos(drawX + shadowPosOffset, drawY + shadowPosOffset);
      if (options != null) {
         tileList.add(tm -> shadowOptions.draw());
         topList.add(tm -> options.draw());
      }
   }

   public void despawnNow() {
      for (int i = 0; i < 20; i++) {
         float xMove = GameRandom.globalRandom.getFloatBetween(-0.5F, 0.5F);
         float yStart = this.y + GameRandom.globalRandom.getFloatBetween(-30.0F, 30.0F) + this.getParticleHeight();
         this.getLevel()
            .entityManager
            .addParticle(this.x, this.y, this.extraParticlesTypes.next())
            .sprite(GameResources.scrapBallDebrisParticles.sprite(GameRandom.globalRandom.nextInt(4), 0, 16, 16))
            .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
               pos.x += xMove * (1.0F - lifePercent);
               pos.y = (float)(yStart - 25.0 * Math.abs(1.3F * Math.cos(4.0F * lifePercent)));
            })
            .rotates(360.0F, 720.0F)
            .size((options, lifeTime, timeAlive, lifePercent) -> options.size(16, 16))
            .fadesAlpha(0.0F, 0.2F)
            .lifeTimeBetween(500, 1000);
      }

      float variation = GameRandom.globalRandom.getFloatBetween(-0.1F, 0.1F);
      SoundManager.playSound(GameResources.explosionLight, SoundEffect.effect(this.x, this.y).falloffDistance(1000).volume(1.5F).pitch(0.5F + variation));
      SoundManager.playSound(GameResources.iceBreak, SoundEffect.effect(this.x, this.y).falloffDistance(1000).volume(1.5F).pitch(0.5F + variation));
      this.lifeTime = 0L;
      this.spawnTime = this.getWorldEntity().getLocalTime();
   }
}
