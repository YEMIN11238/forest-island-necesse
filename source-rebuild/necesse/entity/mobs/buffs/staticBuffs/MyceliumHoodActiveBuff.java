package necesse.entity.mobs.buffs.staticBuffs;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;

public class MyceliumHoodActiveBuff extends Buff {
   public MyceliumHoodActiveBuff() {
      this.shouldSave = false;
      this.isVisible = false;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setMinModifier(BuffModifiers.SLOW, 1.0F, 1000000);
      buff.setModifier(BuffModifiers.RANGED_ATTACK_SPEED, 1.0F);
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      if (buff.owner.getLevel().tickManager().getTotalTicks() % 2L == 0L) {
         float[] angles = new float[]{40.0F, 140.0F, 220.0F, 320.0F};

         for (float angle : angles) {
            Float dir = GameMath.getAngleDir(angle);
            buff.owner
               .getLevel()
               .entityManager
               .addParticle(buff.owner.x, buff.owner.y, Particle.GType.IMPORTANT_COSMETIC)
               .color(new Color(158, 82, 8))
               .sizeFadesInAndOut(5, 10, 50, 200)
               .movesConstant(dir.x * 10.0F, dir.y * 10.0F)
               .lifeTime(500)
               .heightMoves(8.0F, 0.0F);
         }
      }
   }
}
