package necesse.entity.mobs.jumping;

import necesse.entity.mobs.Mob;

public class JumpingMobStats {
   public final Mob mob;
   private int jumpAnimationTime = 500;
   private int jumpCooldown = 250;
   private float jumpStrength = 60.0F;
   public boolean jumpAnimationUseSpeedMod = true;
   public boolean jumpCooldownUseSpeedMod = true;
   public boolean jumpStrengthUseSpeedMod = true;
   long lastJumpTime;
   int lastJumpAnimationTime;

   public JumpingMobStats(Mob mob) {
      if (!(mob instanceof JumpingMobInterface)) {
         throw new IllegalArgumentException("Mob must implement JumpingMobInterface");
      }

      this.mob = mob;
   }

   public void setJumpAnimationTime(int milliseconds) {
      this.jumpAnimationTime = milliseconds;
   }

   public void setJumpCooldown(int milliseconds) {
      this.jumpCooldown = milliseconds;
   }

   public void setJumpStrength(float strength) {
      this.jumpStrength = strength;
   }

   private float getTimeModifier() {
      return 1.0F / this.mob.getSpeedModifier();
   }

   public int getJumpAnimationTime() {
      return this.jumpAnimationUseSpeedMod ? (int)(this.jumpAnimationTime * this.getTimeModifier()) : this.jumpAnimationTime;
   }

   public int getJumpCooldown() {
      return this.jumpCooldownUseSpeedMod ? (int)(this.jumpCooldown * this.getTimeModifier()) : this.jumpCooldown;
   }

   public float getJumpStrength() {
      return this.jumpStrengthUseSpeedMod ? (int)(this.jumpStrength * this.mob.getSpeedModifier()) : this.jumpStrength;
   }
}
