package necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.TrainingDummyMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.StationaryPlayerShooterAI;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.JunkMechMachineGunProjectile;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class MakeshiftTurretMob extends AttackingFollowingMob {
   private static final int makeshiftTurretRange = 500;
   protected int deathTime = 8000;
   protected int lifeTime = 0;

   public MakeshiftTurretMob() {
      super(10);
      this.setSpeed(0.0F);
      this.setFriction(3.0F);
      this.attackAnimTime = 200;
      this.attackCooldown = 100;
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-12, -14, 24, 24);
      this.selectBox = new Rectangle(-13, -30, 26, 40);
      this.swimMaskMove = 16;
      this.swimMaskOffset = 0;
      this.swimSinkOffset = -4;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(
         this,
         new StationaryPlayerShooterAI<MakeshiftTurretMob>(500) {
            public void shootTarget(MakeshiftTurretMob mob, Mob target) {
               JunkMechMachineGunProjectile projectile = new JunkMechMachineGunProjectile(
                  mob.x, mob.y, target.x, target.y, 500.0F, 640, MakeshiftTurretMob.this.summonDamage, 10, mob
               );
               projectile.setTargetPrediction(target);
               MakeshiftTurretMob.this.attack((int)(mob.x + projectile.dx * 100.0F), (int)(mob.y + projectile.dy * 100.0F), true);
               projectile.x = projectile.x + Math.signum(MakeshiftTurretMob.this.attackDir.x) * 10.0F;
               projectile.y = projectile.y + MakeshiftTurretMob.this.attackDir.y * 6.0F;
               MakeshiftTurretMob.this.getLevel().entityManager.projectiles.add(projectile);
            }

            public Stream<Mob> streamTargets(MakeshiftTurretMob mob, int shootDistance) {
               return GameUtils.streamTargets(mob, GameUtils.rangeBounds(MakeshiftTurretMob.this.x, MakeshiftTurretMob.this.y, 500))
                  .filter(m -> mob.isHostile || m.isHostile || m instanceof TrainingDummyMob)
                  .filter(m -> m.getDistance(mob) <= 500.0F);
            }
         }
      );
      if (this.isClient()) {
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
               .color(new Color(30, 177, 143))
               .heightMoves(0.0F, 30.0F)
               .lifeTime(1500);
         }

         SoundManager.playSound(GameResources.magicbolt4, SoundEffect.effect(this).volume(0.3F).pitch(GameRandom.globalRandom.getFloatBetween(1.4F, 1.5F)));
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.lifeTime += 50;
      if (this.lifeTime >= this.deathTime) {
         this.remove(0.0F, 0.0F, null, true);
      }
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.makeshiftTurret, i, 12, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
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
      GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
      int drawX = camera.getDrawX(x) - 48;
      int drawY = camera.getDrawY(y) - 48 - 12;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions body = MobRegistry.Textures.makeshiftTurret
         .initDraw()
         .sprite(sprite.x, sprite.y, 96)
         .addMaskShader(swimMask)
         .light(light)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            body.draw();
         }
      });
   }

   @Override
   public void showAttack(int x, int y, int seed, boolean showAllDirections) {
      super.showAttack(x, y, seed, showAllDirections);
      if (this.isClient()) {
         SoundManager.playSound(GameResources.swing1, SoundEffect.effect(this).volume(0.3F));
      }
   }
}
