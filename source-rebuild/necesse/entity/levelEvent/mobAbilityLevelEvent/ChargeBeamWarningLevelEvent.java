package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import java.util.HashMap;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.RayLinkedList;
import necesse.entity.ParticleBeamHandler;
import necesse.entity.mobs.AttackAnimMob;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.level.maps.LevelObjectHit;

public class ChargeBeamWarningLevelEvent extends MouseBeamLevelEvent {
   private long startTime;
   private int fullChargeTime;
   private SoundPlayer chargeSound;

   public ChargeBeamWarningLevelEvent() {
   }

   public ChargeBeamWarningLevelEvent(
      Mob owner,
      int startTargetX,
      int startTargetY,
      int seed,
      float speed,
      float distance,
      float appendAttackSpeedModifier,
      int bounces,
      Color color,
      int fullChargeTime
   ) {
      super(
         owner,
         startTargetX,
         startTargetY,
         seed,
         speed,
         distance,
         new GameDamage(0.0F),
         0,
         new HashMap<>(),
         1000,
         appendAttackSpeedModifier,
         bounces,
         0.0F,
         color
      );
      this.fullChargeTime = fullChargeTime;
   }

   @Override
   public void init() {
      super.init();
      this.startTime = this.getTime();
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.fullChargeTime);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.fullChargeTime = reader.getNextInt();
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.chargeSound == null || this.chargeSound.isDone()) {
         this.chargeSound = SoundManager.playSound(GameResources.laserBeam1, SoundEffect.effect(this).volume(0.0F));
         if (this.chargeSound != null) {
            this.chargeSound.fadeIn(1.0F);
         }
      }

      if (this.chargeSound != null) {
         long timeProgress = this.getTime() - this.startTime;
         float progress = GameMath.limit((float)timeProgress / this.fullChargeTime, 0.0F, 1.0F);
         float pitch = GameMath.lerp(progress, 0.5F, 1.0F);
         float volume = GameMath.lerp(progress, 0.05F, 0.1F);
         this.chargeSound.effect = SoundEffect.effect(this).pitch(pitch).volume(volume).falloffDistance(1400);
         this.chargeSound.refreshLooping(1.0F);
      }

      float ownerX = this.owner.x;
      float ownerY = this.owner.y;
      if (this.owner instanceof AttackAnimMob) {
         Float attackDir = ((AttackAnimMob)this.owner).attackDir;
         if (attackDir != null) {
            ownerX += attackDir.x * 10.0F;
            ownerY += attackDir.y * 10.0F;
         }
      }

      for (int i = 0; i < 2; i++) {
         int angle = GameRandom.globalRandom.nextInt(360);
         Float dir = GameMath.getAngleDir(angle);
         float range = GameRandom.globalRandom.getFloatBetween(15.0F, 22.0F);
         float startX = ownerX + dir.x * range;
         float startY = ownerY + 4.0F;
         float endHeight = 18.0F;
         float startHeight = endHeight + dir.y * range;
         int lifeTime = GameRandom.globalRandom.getIntBetween(200, 500);
         float speed = dir.x * range * 250.0F / lifeTime;
         Color color1 = new Color(199, 40, 34);
         Color color2 = new Color(222, 42, 75);
         Color color3 = new Color(220, 37, 92);
         Color color = GameRandom.globalRandom.getOneOf(color1, color2, color3);
         this.getLevel()
            .entityManager
            .addParticle(startX, startY, Particle.GType.IMPORTANT_COSMETIC)
            .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.nextInt(5), 0, 12))
            .sizeFades(12, 14)
            .rotates()
            .heightMoves(startHeight, endHeight)
            .movesConstant(-speed, 0.0F)
            .color(color)
            .fadesAlphaTime(100, 50)
            .lifeTime(lifeTime);
      }
   }

   @Override
   public boolean canHit(LevelObjectHit hit) {
      return false;
   }

   @Override
   public boolean canHit(Mob mob) {
      return false;
   }

   @Override
   protected ParticleBeamHandler constructBeam() {
      return super.constructBeam().particleSize(10, 12).particleThicknessMod(0.2F).endParticleSize(0, 0).distPerParticle(20.0F).thickness(8, 2).speed(50.0F);
   }

   @Override
   protected void updateTrail(RayLinkedList<LevelObjectHit> rays, float delta) {
      super.updateTrail(rays, delta);
      long timeProgress = this.getTime() - this.startTime;
      float progress = GameMath.limit((float)timeProgress / this.fullChargeTime, 0.0F, 1.0F);
      this.beamHandler.color(new Color(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), (int)(progress * 255.0F)));
   }

   @Override
   public void onDispose() {
      super.onDispose();
      if (this.chargeSound != null) {
         this.chargeSound.stop();
      }
   }
}
