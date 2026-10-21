package necesse.entity.projectile;

import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.Buff;
import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.DryadSpiritFollowingMob;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.entity.particle.ProjectileHitStuckParticle;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.item.toolItem.summonToolItem.SummonToolItem;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public class DryadBowProjectile extends Projectile {
   private int dryadHauntedStacksOnHit = 0;

   public DryadBowProjectile() {
   }

   public DryadBowProjectile(
      Mob owner, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int dryadHauntedStacksOnHit, int knockback
   ) {
      this.setOwner(owner);
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.setDamage(damage);
      this.dryadHauntedStacksOnHit = dryadHauntedStacksOnHit;
      this.speed = speed;
      this.setDistance(distance);
      this.knockback = knockback;
   }

   @Override
   public void init() {
      super.init();
      this.height = 18.0F;
      this.trailOffset = -6.0F;
   }

   @Override
   protected int getExtraSpinningParticles() {
      return super.getExtraSpinningParticles() + 2;
   }

   @Override
   protected void modifySpinningParticle(ParticleOption particle) {
      particle.lifeTime(1000);
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), this.getParticleColor(), 12.0F, 200, this.getHeight());
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
      if (!this.removed()) {
         GameLight light = level.getLightLevel(this);
         int drawX = camera.getDrawX(this.x) - this.texture.getWidth() / 2;
         int drawY = camera.getDrawY(this.y) - this.texture.getHeight() / 2;
         final TextureDrawOptions options = this.texture
            .initDraw()
            .light(light)
            .rotate(this.getAngle() + 45.0F, this.texture.getWidth() / 2, this.texture.getHeight() / 2)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         this.addShadowDrawables(tileList, drawX, drawY, light, this.getAngle() + 45.0F, 0);
      }
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (mob != null) {
         Buff dryadHaunted = BuffRegistry.Debuffs.DRYAD_HAUNTED;
         ActiveBuff ab = new ActiveBuff(dryadHaunted, mob, 10000, this.getAttackOwner());
         ab.setStacks(this.dryadHauntedStacksOnHit, 10000, this.getAttackOwner());
         mob.buffManager.addBuff(ab, this.isServer());
         if (mob.buffManager.getStacks(dryadHaunted) >= 10) {
            mob.buffManager.removeBuff(dryadHaunted, this.isServer());
            spawnDryadSpirit(this.getAttackOwner());
         }
      }

      if (this.isClient() && this.bounced == this.getTotalBouncing()) {
         this.getLevel()
            .entityManager
            .addParticle(
               new ProjectileHitStuckParticle(mob, this, x, y, GameRandom.globalRandom.getIntBetween(10, 20), 1000L) {
                  @Override
                  public void addDrawables(
                     Mob target,
                     float xx,
                     float yx,
                     float angle,
                     List<LevelSortedDrawable> list,
                     OrderableDrawables tileList,
                     OrderableDrawables topList,
                     Level level,
                     TickManager tickManager,
                     GameCamera camera,
                     PlayerMob perspective
                  ) {
                     GameLight light = level.getLightLevel(this);
                     int drawX = camera.getDrawX(xx) - 2;
                     int drawY = camera.getDrawY(yx - DryadBowProjectile.this.height) - 2;
                     float alpha = 1.0F;
                     long lifeCycleTime = this.getLifeCycleTime();
                     int fadeTime = 200;
                     if (lifeCycleTime >= this.lifeTime - fadeTime) {
                        alpha = Math.abs((float)(lifeCycleTime - (this.lifeTime - fadeTime)) / fadeTime - 1.0F);
                     }

                     int cut = target == null ? 8 : 0;
                     final TextureDrawOptions options = DryadBowProjectile.this.texture
                        .initDraw()
                        .light(light)
                        .rotate(DryadBowProjectile.this.getAngle() + 45.0F, 2, 2)
                        .alpha(alpha)
                        .pos(drawX, drawY);
                     EntityDrawable drawable = new EntityDrawable(this) {
                        @Override
                        public void draw(TickManager tickManager) {
                           options.draw();
                        }
                     };
                     if (target != null) {
                        topList.add(drawable);
                     } else {
                        list.add(drawable);
                     }
                  }
               },
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   public static void spawnDryadSpirit(Mob owner) {
      if (owner != null && owner.isServer()) {
         int maxSummons = 5;
         DryadSpiritFollowingMob summonedMob = (DryadSpiritFollowingMob)MobRegistry.getMob("dryadspirit", owner.getLevel());
         ((ItemAttackerMob)owner)
            .serverFollowersManager
            .addFollower("summonedmobtemp", summonedMob, FollowPosition.FLYING_CIRCLE_FAST, "summonedmob", 1.0F, maxSummons, null, false);
         Float spawnPoint = SummonToolItem.findSpawnLocation(summonedMob, owner.getLevel(), owner.x, owner.y);
         owner.getLevel().entityManager.addMob(summonedMob, spawnPoint.x, spawnPoint.y);
      }
   }

   @Override
   protected SoundSettings getSpawnSound() {
      return new SoundSettings(GameResources.wind1).volume(0.1F).basePitch(1.3F).pitchVariance(0.1F);
   }
}
