package necesse.entity.levelEvent.explosionEvent.splashEvent;

import java.awt.Color;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.NecroticPoisonBuff;

public class NecroPoisonSplashEvent extends BuffSplashEvent {
   public static float poisonDuration = 3.0F;

   public NecroPoisonSplashEvent() {
      this(0.0F, 0.0F, 96, new GameDamage(0.0F), 0.0F, null);
   }

   public NecroPoisonSplashEvent(float x, float y, int range, GameDamage damage, float toolTier, Mob owner) {
      super(x, y, range, damage, toolTier, owner);
      this.knockback = 50;
   }

   @Override
   protected float getDistanceMod(float targetDistance) {
      return 1.0F;
   }

   @Override
   protected ActiveBuff getBuff(Mob buffOwner) {
      float pDamage = this.damage.getBuffedDamage(buffOwner);
      ActiveBuff poisonDebuff = new ActiveBuff(BuffRegistry.Debuffs.NECROTIC_POISON, buffOwner, poisonDuration, this);
      poisonDebuff.getGndData().setFloat("damagepersec", pDamage / 20.0F * poisonDuration);
      return poisonDebuff;
   }

   @Override
   protected Color getInnerSplashColor() {
      return GameRandom.globalRandom.getOneOfWeighted(Color.class, 5, new Color(55, 22, 55), 4, new Color(11, 22, 33), 1, new Color(11, 88, 33));
   }

   @Override
   protected Color getOuterSplashColor() {
      return NecroticPoisonBuff.getNecroticParticleColor();
   }
}
