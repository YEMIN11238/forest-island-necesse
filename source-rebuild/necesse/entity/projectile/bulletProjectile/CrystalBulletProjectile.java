package necesse.entity.projectile.bulletProjectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.mobAbilityLevelEvent.CrystallizeShatterEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.Buff;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ProjectileHitStuckParticle;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.ThemeColorRegistry;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public class CrystalBulletProjectile extends BulletProjectile {
   protected int spriteX;

   public CrystalBulletProjectile() {
   }

   public CrystalBulletProjectile(float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback, Mob owner) {
      this();
      this.setLevel(owner.getLevel());
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.speed = speed;
      this.setDistance(distance);
      this.setDamage(damage);
      this.knockback = knockback;
      this.setOwner(owner);
      this.spriteX = GameRandom.globalRandom.getIntBetween(0, 2);
   }

   @Override
   public void init() {
      super.init();
      this.particleSpeedMod = 0.03F;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.spriteX);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.spriteX = reader.getNextInt();
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (this.isServer() && mob != null) {
         Mob attackOwner = this.getAttackOwner();
         if (attackOwner != null) {
            GameDamage damage = this.getDamage();
            float thresholdMod = attackOwner.getCritChance() + damage.type.getTypeCritChanceModifier(attackOwner);
            float crystallizeMod = attackOwner.getCritDamageModifier() + damage.type.getTypeCritDamageModifier(attackOwner);
            int stackThreshold = (int)GameMath.limit(10.0F - 7.0F * thresholdMod, 3.0F, 10.0F);
            float crystallizeDamageMultiplier = GameMath.limit(crystallizeMod, 2.0F, stackThreshold);
            Buff crystallizeBuff = BuffRegistry.Debuffs.CRYSTALLIZE_BUFF;
            ActiveBuff ab = new ActiveBuff(crystallizeBuff, mob, 10000, attackOwner);
            mob.buffManager.addBuff(ab, true);
            ActiveBuff buff = mob.buffManager.getBuff(crystallizeBuff);
            if (buff != null && buff.getStacks() >= stackThreshold) {
               this.getLevel().entityManager.events.add(new CrystallizeShatterEvent(mob, CrystallizeShatterEvent.ParticleType.SAPPHIRE));
               mob.buffManager.removeBuff(crystallizeBuff, true);
               GameDamage finalDamage = damage.modDamage(crystallizeDamageMultiplier);
               mob.isServerHit(finalDamage, 0.0F, 0.0F, 0.0F, this);
            }
         }
      }

      if (this.isClient() && this.bounced == this.getTotalBouncing()) {
         this.getLevel()
            .entityManager
            .addParticle(
               new ProjectileHitStuckParticle(mob, this, x, y, GameRandom.globalRandom.getIntBetween(10, 20), 5000L) {
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
                     int drawY = camera.getDrawY(yx - CrystalBulletProjectile.this.height) - 2;
                     float alpha = 1.0F;
                     long lifeCycleTime = this.getLifeCycleTime();
                     int fadeTime = 1000;
                     if (lifeCycleTime >= this.lifeTime - fadeTime) {
                        alpha = Math.abs((float)(lifeCycleTime - (this.lifeTime - fadeTime)) / fadeTime - 1.0F);
                     }

                     final TextureDrawOptions options = CrystalBulletProjectile.this.texture
                        .initDraw()
                        .sprite(CrystalBulletProjectile.this.spriteX, 0, 18, 32)
                        .light(light)
                        .rotate(CrystalBulletProjectile.this.getAngle(), 8, 0)
                        .pos(drawX, drawY)
                        .alpha(alpha);
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

   @Override
   public void onMaxMoveTick() {
      if (this.isClient()) {
         this.spawnSpinningParticle();
      }
   }

   @Override
   public Trail getTrail() {
      Trail trail = new Trail(this, this.getLevel(), ThemeColorRegistry.SAPPHIRE.getRandomColor(), 22.0F, 100, this.height);
      trail.sprite = new GameSprite(GameResources.chains, 7, 0, 32);
      return trail;
   }

   @Override
   protected Color getWallHitColor() {
      return ThemeColorRegistry.SAPPHIRE.getRandomColor();
   }

   @Override
   public void refreshParticleLight() {
      this.getLevel().lightManager.refreshParticleLightFloat(this.x, this.y, this.getWallHitColor(), this.lightSaturation);
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
         int drawX = camera.getDrawX(this.x) - 8;
         int drawY = camera.getDrawY(this.y) - 18;
         final TextureDrawOptions options = this.texture
            .initDraw()
            .sprite(this.spriteX, 0, 18, 32)
            .light(light)
            .rotate(this.getAngle(), 8, 18)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         this.addShadowDrawables(tileList, drawX, drawY, light, this.getAngle(), 18);
      }
   }

   @Override
   public void playHitSound(float x, float y) {
      SoundManager.playSound(GameResources.crystalHit1, SoundEffect.effect(this).volume(2.0F).pitch(GameRandom.globalRandom.getFloatBetween(0.9F, 1.1F)));
   }
}
