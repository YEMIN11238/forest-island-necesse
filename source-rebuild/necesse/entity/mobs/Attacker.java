package necesse.entity.mobs;

import java.util.HashSet;
import java.util.LinkedList;
import necesse.engine.localization.message.GameMessage;

public interface Attacker {
   int DEFAULT_RESPAWN_TIME = 5000;

   GameMessage getAttackerName();

   DeathMessageTable getDeathMessages();

   Mob getFirstAttackOwner();

   default Mob getAttackOwner() {
      LinkedList<Mob> ownerChain = this.getAttackOwnerChain();
      return ownerChain.isEmpty() ? this.getFirstAttackOwner() : ownerChain.getLast();
   }

   default LinkedList<Mob> getAttackOwnerChain() {
      LinkedList<Mob> out = new LinkedList<>();
      HashSet<Mob> ownersFound = new HashSet<>();
      Mob last = this.getFirstAttackOwner();

      while (last != null) {
         out.addLast(last);
         ownersFound.add(last);
         Mob next = last.getFirstAttackOwner();
         if (next == null) {
            return out;
         }

         if (last == next || ownersFound.contains(next)) {
            return out;
         }

         last = next;
      }

      return out;
   }

   default PlayerMob getFirstPlayerOwner() {
      HashSet<Mob> ownersFound = new HashSet<>();
      Mob last = this.getFirstAttackOwner();

      while (last != null) {
         if (last.isPlayer) {
            return (PlayerMob)last;
         }

         ownersFound.add(last);
         Mob next = last.getFirstAttackOwner();
         if (next == null) {
            return null;
         }

         if (last == next || ownersFound.contains(next)) {
            return null;
         }

         last = next;
      }

      return null;
   }

   default HashSet<Mob> getAttackOwners() {
      HashSet<Mob> ownersFound = new HashSet<>();
      Mob last = this.getFirstAttackOwner();

      while (last != null) {
         ownersFound.add(last);
         Mob next = last.getFirstAttackOwner();
         if (next == null) {
            return ownersFound;
         }

         if (last == next || ownersFound.contains(next)) {
            return ownersFound;
         }

         last = next;
      }

      return ownersFound;
   }

   default boolean isInAttackOwnerChain(Mob target) {
      return this.getAttackOwners().contains(target);
   }

   default boolean removed() {
      Mob attackOwner = this.getAttackOwner();
      return attackOwner != null ? attackOwner.removed() : false;
   }

   default int getAttackerUniqueID() {
      Mob attackOwner = this.getAttackOwner();
      return attackOwner != null ? attackOwner.getUniqueID() : -1;
   }

   default int getRespawnTime() {
      return 5000;
   }

   default DeathMessageTable getDeathMessages(String key, int count) {
      return DeathMessageTable.fromRange(key, count);
   }

   default boolean isTrapAttacker() {
      return false;
   }

   default void addAttackersToSet(HashSet<Attacker> attackers) {
      attackers.add(this);

      for (Mob attackOwner : this.getAttackOwners()) {
         if (attackOwner != this) {
            attackOwner.addAttackersToSet(attackers);
         }
      }
   }

   default Attacker getAttackerDamageProxy() {
      return this;
   }

   default void startTargetHitCooldown(Mob target) {
      target.startHitCooldown();
   }

   default boolean isTargetOnHitCooldown(Mob target) {
      return target.isOnHitCooldown();
   }
}
