package necesse.entity.mobs;

import java.util.HashSet;
import necesse.engine.util.GameBlackboard;

public class MobWasKilledEvent {
   public final Mob target;
   public final Attacker attacker;
   public final HashSet<Attacker> attackers;
   public final GameBlackboard blackboard = new GameBlackboard();

   public MobWasKilledEvent(Mob target, Attacker attacker, HashSet<Attacker> attackers) {
      this.target = target;
      this.attacker = attacker;
      this.attackers = attackers;
   }

   public boolean didKillingBlow(Mob attacker) {
      return this.attacker != null && this.attacker.getAttackOwner() == attacker;
   }
}
