package necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ProjectileRegistry;
import necesse.engine.sound.PositionSoundEffect;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.CustomMobAbility;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.PlayerFollowerChaserAI;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.friendly.GrizzboltMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.Projectile;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;

public class GrizzboltSummonMob extends AttackingFollowingMob {
   public static int MINIGUN_CHARGE_TIME = 750;
   public static int MINIGUN_DISCHARGE_TIME = 750;
   public float minigunProgress;
   public GrizzboltSummonMob.ShowGrizzboltSummonAttackAbility showMinigunAbility;
   public SoundPlayer chargeSoundPlayer;
   public float lastMinigunSoundProgress;

   public GrizzboltSummonMob() {
      super(10);
      this.setSpeed(60.0F);
      this.setFriction(3.0F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-20, -16, 40, 32);
      this.selectBox = new Rectangle(-30, -66, 60, 70);
      this.swimMaskMove = 26;
      this.swimMaskOffset = -48;
      this.swimSinkOffset = -8;
      this.attackAnimTime = 500;
      this.attackCooldown = 100;
      this.showMinigunAbility = this.registerAbility(new GrizzboltSummonMob.ShowGrizzboltSummonAttackAbility(this));
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextFloat(this.minigunProgress);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.minigunProgress = reader.getNextFloat();
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(
         this,
         new PlayerFollowerChaserAI<GrizzboltSummonMob>(448, 512, false, false, 640, 64) {
            public boolean attackTarget(GrizzboltSummonMob mob, Mob target) {
               if (mob.canAttack()) {
                  int attackCooldownMs = Math.min(mob.attackCooldown, mob.attackAnimTime);
                  float increase = 1.0F / ((float)GrizzboltSummonMob.MINIGUN_CHARGE_TIME / attackCooldownMs);
                  mob.minigunProgress = Math.min(mob.minigunProgress + increase, 1.0F);
                  int targetX = target.getX();
                  int targetY = target.getY();
                  Point offset = GrizzboltMob.pivotOffsets[mob.getDir()];
                  if (mob.minigunProgress >= 1.0F) {
                     GameDamage damage = GrizzboltSummonMob.this.summonDamage;
                     Mob followingMob = GrizzboltSummonMob.this.getFollowingMob();
                     if (followingMob != null) {
                        damage = damage.modDamage(followingMob.buffManager.getModifier(BuffModifiers.MAX_SUMMONS).intValue());
                     }

                     Projectile projectile = ProjectileRegistry.getProjectile(
                        "grizzboltbullet", mob.getLevel(), mob.x + offset.x, mob.y + offset.y + 22.0F, target.x, target.y, 200.0F, 1024, damage, mob
                     );
                     projectile.setTargetPrediction(target, -70.0F);
                     projectile.moveDist(70.0);
                     targetX = (int)(mob.x + projectile.dx * 100.0F);
                     targetY = (int)(mob.y + projectile.dy * 100.0F);
                     mob.getLevel().entityManager.projectiles.add(projectile);
                  }

                  mob.showMinigunAbility.run(targetX, targetY);
                  return true;
               } else {
                  return false;
               }
            }
         }
      );
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      if (!this.isAttacking && this.minigunProgress > 0.0F) {
         float decrease = delta / MINIGUN_DISCHARGE_TIME;
         this.minigunProgress = Math.max(this.minigunProgress - decrease, 0.0F);
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.minigunProgress > 0.0F) {
         if (this.chargeSoundPlayer == null
            || this.chargeSoundPlayer.isDone()
            || Math.abs(this.minigunProgress - this.lastMinigunSoundProgress) > 0.02F
            || this.minigunProgress >= 1.0F && this.lastMinigunSoundProgress < 1.0F) {
            SoundPlayer lastPlayer = this.chargeSoundPlayer;
            float pitch = GameMath.lerp(this.minigunProgress, 1.0F, 1.5F);
            float volume = GameMath.lerp(this.minigunProgress, 0.25F, 0.2F);
            PositionSoundEffect effect = SoundEffect.effect(this).volume(volume).pitch(pitch).falloffDistance(1500);
            this.chargeSoundPlayer = SoundManager.playSound(GameResources.minigunCharge, effect, player -> {
               if (lastPlayer == null) {
                  player.fadeIn(1.0F);
               } else {
                  player.copyFadeInProgress(lastPlayer);
                  player.setPosition(lastPlayer.getPositionSeconds());
               }
            });
            this.lastMinigunSoundProgress = this.minigunProgress;
         }

         if (this.chargeSoundPlayer != null && !this.chargeSoundPlayer.isDone()) {
            this.chargeSoundPlayer.refreshLooping(0.5F);
         }
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
      GrizzboltMob.addDrawables(this, this.minigunProgress, list, tileList, topList, level, x, y, tickManager, camera, perspective);
   }

   @Override
   public int getRockSpeed() {
      return 10;
   }

   public static class ShowGrizzboltSummonAttackAbility extends CustomMobAbility {
      public final GrizzboltSummonMob mob;

      public ShowGrizzboltSummonAttackAbility(GrizzboltSummonMob mob) {
         this.mob = mob;
      }

      public void run(int targetX, int targetY) {
         Packet content = new Packet();
         PacketWriter writer = new PacketWriter(content);
         writer.putNextInt(targetX);
         writer.putNextInt(targetY);
         writer.putNextFloat(this.mob.minigunProgress);
         this.runAndSend(content);
      }

      @Override
      protected void run(Packet content) {
         PacketReader reader = new PacketReader(content);
         int targetX = reader.getNextInt();
         int targetY = reader.getNextInt();
         this.mob.minigunProgress = reader.getNextFloat();
         this.mob.showAttack(targetX, targetY, 0, true);
         if (this.mob.minigunProgress >= 1.0F && !this.mob.isServer()) {
            SoundManager.playSound(GameResources.minigunShoot, SoundEffect.effect(this.mob).volume(0.3F).falloffDistance(1500));
            int yOffset = this.mob.getDir() == 0 ? -10 : 20;
            this.mob
               .getLevel()
               .entityManager
               .addParticle(
                  new FleshParticle(this.mob.getLevel(), MobRegistry.Textures.grizzbolt_body, 5, 16, 32, this.mob.x, this.mob.y + yOffset, 20.0F, 0.0F, 0.0F),
                  Particle.GType.IMPORTANT_COSMETIC
               );
         }
      }
   }
}
