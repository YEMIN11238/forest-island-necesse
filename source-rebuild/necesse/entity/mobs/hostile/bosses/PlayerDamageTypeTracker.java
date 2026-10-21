package necesse.entity.mobs.hostile.bosses;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.gameDamageType.DamageType;

public class PlayerDamageTypeTracker {
   protected HashMap<Long, PlayerDamageTypeTracker.Tracker> authToTracker = new HashMap<>();

   public void addDamage(ServerClient client, DamageType type, int damage) {
      if (damage > 0) {
         PlayerDamageTypeTracker.Tracker tracker = this.authToTracker
            .compute(client.authentication, (k, v) -> v == null ? new PlayerDamageTypeTracker.Tracker() : v);
         tracker.typeToDamage.compute(type, (k, v) -> v == null ? damage : v + damage);
      }
   }

   public DamageType getMostDamageTypeDealt(ServerClient client) {
      PlayerDamageTypeTracker.Tracker tracker = this.authToTracker.get(client.authentication);
      return tracker == null ? null : tracker.typeToDamage.entrySet().stream().max(Comparator.comparingInt(Entry::getValue)).map(Entry::getKey).orElse(null);
   }

   public void addOtherStats(PlayerDamageTypeTracker other) {
      for (Entry<Long, PlayerDamageTypeTracker.Tracker> entry : other.authToTracker.entrySet()) {
         PlayerDamageTypeTracker.Tracker tracker = this.authToTracker.compute(entry.getKey(), (k, v) -> v == null ? new PlayerDamageTypeTracker.Tracker() : v);

         for (Entry<DamageType, Integer> typeEntry : entry.getValue().typeToDamage.entrySet()) {
            tracker.typeToDamage.compute(typeEntry.getKey(), (k, v) -> v == null ? typeEntry.getValue() : v + typeEntry.getValue());
         }
      }
   }

   public MobWasHitEvent runIsServerHit(MobWasHitEvent event) {
      if (event == null) {
         return null;
      }

      PlayerMob firstPlayer = event.attacker == null ? null : event.attacker.getFirstPlayerOwner();
      if (firstPlayer != null && firstPlayer.isServerClient()) {
         ServerClient client = firstPlayer.getServerClient();
         int healthDamage = Math.min(event.damage, event.beforeHealth - event.target.getHealth());
         if (healthDamage > 0) {
            this.addDamage(client, event.damageType, healthDamage);
         }
      }

      return event;
   }

   protected static class Tracker {
      public HashMap<DamageType, Integer> typeToDamage = new HashMap<>();
   }
}
