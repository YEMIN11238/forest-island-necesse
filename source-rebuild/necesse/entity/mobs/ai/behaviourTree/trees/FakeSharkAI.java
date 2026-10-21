package necesse.entity.mobs.ai.behaviourTree.trees;

import java.awt.geom.Point2D.Float;
import java.util.function.Supplier;
import necesse.engine.network.packet.PacketMobAttack;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.event.ConfuseWanderAIEvent;
import necesse.entity.mobs.hostile.SharkMob;

public class FakeSharkAI<T extends SharkMob> extends SharkAI<T> {
   public FakeSharkAI(int searchDistance) {
      super(searchDistance);
      this.critterAttackCooldown = 15000L;
   }

   @Override
   protected boolean sharkAttack(T mob, Mob target, Supplier<Boolean> superAttackTarget) {
      if (!target.canBeHit(mob)) {
         return false;
      }

      mob.getServer().network.sendToAllClients(new PacketMobAttack(mob, mob.getX(), mob.getY(), true));
      Float attackDir = GameMath.normalize(target.x - mob.x, target.y - mob.y);
      float attackAngle = GameMath.getAngle(attackDir);
      float runAwayAngle = GameMath.fixAngle(attackAngle + GameRandom.globalRandom.getFloatBetween(-20.0F, 20.0F));
      Float runAwayDir = GameMath.getAngleDir(runAwayAngle);
      int confuseTime = GameRandom.globalRandom.getIntBetween(3000, 5000);
      this.getBlackboard().submitEvent("confuseWander", new ConfuseWanderAIEvent(confuseTime, runAwayDir));
      return true;
   }
}
