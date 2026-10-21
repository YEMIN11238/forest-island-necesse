package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D.Float;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.entity.ParticleBeamHandler;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidHornMob;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameSprite;

public class TheVoidHornBeamLevelEvent extends GenericBeamLevelEvent {
   private boolean isLeftHorn;

   public TheVoidHornBeamLevelEvent() {
   }

   public TheVoidHornBeamLevelEvent(
      Mob owner,
      boolean isLeftHorn,
      float startAngle,
      float endAngle,
      long startTime,
      int sweepTime,
      int seed,
      float distance,
      GameDamage damage,
      int knockback,
      int hitCooldown,
      int bounces
   ) {
      super(owner, startAngle, endAngle, startTime, sweepTime, seed, distance, damage, knockback, hitCooldown, bounces);
      this.isLeftHorn = isLeftHorn;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextBoolean(this.isLeftHorn);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.isLeftHorn = reader.getNextBoolean();
   }

   @Override
   public void init() {
      super.init();
      if (this.owner != null && this.isClient()) {
         SoundManager.playSound(GameResources.magicbolt4, SoundEffect.effect(this).falloffDistance(1250).volume(0.8F).pitch(0.7F));
      }
   }

   @Override
   public int getHitboxWidth() {
      return this.getAlpha() < 0.5F ? 0 : 10;
   }

   @Override
   public Float getBeamStartPosition(float currentAngle, Float dir) {
      if (this.owner instanceof TheVoidHornMob) {
         TheVoidHornMob hornMob = (TheVoidHornMob)this.owner;
         Point offset = hornMob.getHornOffset();
         return new Float(hornMob.x + offset.x, hornMob.y + offset.y);
      } else {
         return super.getBeamStartPosition(currentAngle, dir);
      }
   }

   @Override
   public SoundSettings getSound() {
      return super.getSound().fallOffDistance(3000);
   }

   @Override
   public void modifyTrail(ParticleBeamHandler handler) {
      handler.sprite(new GameSprite(GameResources.chains, 11, 0, 32))
         .color(() -> new Color(1.0F, 1.0F, 1.0F, this.getAlpha()))
         .particleColor(() -> new Color(0.0F, 0.0F, 0.0F, this.getAlpha()))
         .particleThicknessMod(4.0F)
         .thickness(25, 20)
         .height(16.0F)
         .drawOnTop(50);
   }

   public float getAlpha() {
      if (this.owner instanceof TheVoidHornMob && ((TheVoidHornMob)this.owner).isBroken) {
         return 0.0F;
      } else {
         int fadeTime = Math.min(500, this.sweepTime / 5);
         int timeSinceStart = this.getTimeSinceStart();
         if (timeSinceStart < fadeTime) {
            float progress = (float)timeSinceStart / fadeTime;
            return Math.max(0.0F, progress);
         } else {
            int timeLeft = this.getTimeLeft();
            if (timeLeft < fadeTime) {
               float progress = (float)timeLeft / fadeTime;
               return Math.max(0.0F, progress);
            } else {
               return 1.0F;
            }
         }
      }
   }
}
