package necesse.gfx.drawables;

import necesse.engine.util.GameMath;

public class WallShadowVariables {
   public float lightLevel;
   public float angle;
   public float range;
   public double dirX;
   public double dirY;
   public float dirXOffset;
   public float dirYOffset;
   public boolean east;
   public boolean south;
   public boolean west;
   public boolean north;
   public float startAlpha;
   public float endAlpha;

   public WallShadowVariables(float lightLevel, float angle, float range) {
      this.lightLevel = lightLevel;
      this.angle = angle;
      this.range = range;
   }

   public static WallShadowVariables fromProgress(float lightLevel, float progress, float minRange, float maxRange) {
      float angle = GameMath.fixAngle(180.0F - progress * 180.0F);
      float sunHeight = Math.abs((float)Math.pow(progress * 2.0F - 1.0F, 4.0) - 1.0F);
      float rangeDelta = maxRange - minRange;
      float range = maxRange - sunHeight * rangeDelta;
      return new WallShadowVariables(lightLevel, angle, range);
   }

   public void calculate() {
      this.dirX = Math.cos(Math.toRadians(this.angle));
      this.dirY = Math.sin(Math.toRadians(this.angle));
      this.dirXOffset = (float)(this.dirX * this.range);
      this.dirYOffset = (float)(this.dirY * this.range);
      this.east = this.angle < 90.0F || this.angle > 270.0F;
      this.south = this.angle > 0.0F && this.angle < 180.0F;
      this.west = this.angle > 90.0F && this.angle < 270.0F;
      this.north = this.angle > 180.0F && this.angle < 360.0F;
      this.startAlpha = this.lightLevel * 0.3F;
      this.endAlpha = this.lightLevel * 0.3F;
   }
}
