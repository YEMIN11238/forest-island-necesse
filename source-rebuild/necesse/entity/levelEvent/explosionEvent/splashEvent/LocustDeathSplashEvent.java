package necesse.entity.levelEvent.explosionEvent.splashEvent;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.gfx.GameResources;
import necesse.gfx.ThemeColorRange;
import necesse.gfx.ThemeColorRegistry;

public class LocustDeathSplashEvent extends SplashEvent {
   protected ThemeColorRange splashColorInner;
   protected Color locustColor;

   public LocustDeathSplashEvent() {
      this(0.0F, 0.0F, 96, new GameDamage(0.0F), 0.0F, null, null);
   }

   public LocustDeathSplashEvent(float x, float y, int range, GameDamage damage, float toolTier, Mob owner, Color locustColor) {
      super(x, y, range, damage, false, toolTier, owner);
      this.knockback = 20;
      this.locustColor = locustColor;
      this.splashColorInner = ThemeColorRegistry.combine(ThemeColorRegistry.PURPLE, ThemeColorRegistry.BLUE);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.locustColor.getRGB());
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.locustColor = new Color(reader.getNextInt());
      ThemeColorRange magicrange = ThemeColorRegistry.combine(ThemeColorRegistry.PURPLE, ThemeColorRegistry.BLUE);
      this.splashColorInner = new ThemeColorRange(magicrange, this.locustColor);
   }

   @Override
   protected boolean canHitMob(Mob target) {
      return super.canHitMob(target) && target != this.ownerMob;
   }

   @Override
   protected void onMobWasHit(Mob target, float distance) {
      Float normalDir = GameMath.normalize(target.x - this.x, target.y - this.y);
      target.isServerHit(this.damage, normalDir.x, normalDir.y, this.knockback, this);
   }

   @Override
   protected Color getInnerSplashColor() {
      return this.splashColorInner.getRandomColor();
   }

   @Override
   protected Color getOuterSplashColor() {
      return this.locustColor;
   }

   @Override
   protected void playExplosionEffects() {
      SoundManager.playSound(
         GameResources.explosionLight,
         SoundEffect.effect(this.x, this.y).volume(0.7F).pitch(GameRandom.globalRandom.getFloatBetween(1.15F, 1.25F)).falloffDistance(1200)
      );
   }
}
