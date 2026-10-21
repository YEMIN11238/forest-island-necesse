package necesse.entity.manager;

import java.util.Iterator;
import java.util.LinkedList;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameUtils;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MobAbilityLevelEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.projectile.Projectile;
import necesse.level.maps.Level;

public class ClientSubmittedHits {
   private static final int INVALIDATE_TIMEOUT = 2000;
   private final EntityManager manager;
   private final Level level;
   private final LinkedList<ClientSubmittedHits.SubmittedHit<Projectile>> projectileHits = new LinkedList<>();
   private final LinkedList<ClientSubmittedHits.SubmittedHit<MobAbilityLevelEvent>> mobAbilityLevelEventHits = new LinkedList<>();

   public ClientSubmittedHits(EntityManager manager) {
      this.manager = manager;
      this.level = manager.level;
   }

   public void tick() {
      while (!this.projectileHits.isEmpty()) {
         ClientSubmittedHits.SubmittedHit<Projectile> first = this.projectileHits.getFirst();
         if (!first.isStillValid()) {
            this.projectileHits.removeFirst();
            if (first.removedHandler != null) {
               first.removedHandler.onRemoved(first.client, first.attackerUniqueID, first.attacker, first.targetUniqueID, first.target);
            }
            continue;
         }
         break;
      }
   }

   public void submitProjectileHit(
      ServerClient client, int projectileUniqueID, int targetUniqueID, SubmittedHitHandler<Projectile> hitHandler, RemovedHitHandler<Projectile> removedHandler
   ) {
      ClientSubmittedHits.SubmittedHit<Projectile> submittedHit = new ClientSubmittedHits.SubmittedHit<>(
         projectileUniqueID, this.manager.projectiles.get(projectileUniqueID, true), targetUniqueID, client, hitHandler, removedHandler
      );
      if (!submittedHit.handleHit()) {
         this.projectileHits.add(submittedHit);
      }
   }

   public void submitNewProjectile(Projectile projectile) {
      this.handleNewAttacker(projectile, projectile.getUniqueID(), this.projectileHits.iterator());
   }

   public void submitMobAbilityLevelEventHit(
      ServerClient client,
      int eventUniqueID,
      int targetUniqueID,
      SubmittedHitHandler<MobAbilityLevelEvent> hitHandler,
      RemovedHitHandler<MobAbilityLevelEvent> removedHandler
   ) {
      LevelEvent levelEvent = this.manager.events.get(eventUniqueID, true);
      MobAbilityLevelEvent toolItemEvent = levelEvent instanceof MobAbilityLevelEvent ? (MobAbilityLevelEvent)levelEvent : null;
      ClientSubmittedHits.SubmittedHit<MobAbilityLevelEvent> submittedHit = new ClientSubmittedHits.SubmittedHit<>(
         eventUniqueID, toolItemEvent, targetUniqueID, client, hitHandler, removedHandler
      );
      if (!submittedHit.handleHit()) {
         this.mobAbilityLevelEventHits.add(submittedHit);
      }
   }

   public void submitNewMobAbilityLevelEvent(MobAbilityLevelEvent event) {
      this.handleNewAttacker(event, event.getUniqueID(), this.mobAbilityLevelEventHits.iterator());
   }

   public void submitNewMob(Mob mob) {
      this.handleNewMob(mob, this.projectileHits.iterator());
   }

   private <T> void handleNewAttacker(T attacker, int attackerUniqueID, Iterator<ClientSubmittedHits.SubmittedHit<T>> iterator) {
      while (iterator.hasNext()) {
         ClientSubmittedHits.SubmittedHit<T> next = iterator.next();
         if (next.attackerUniqueID == attackerUniqueID) {
            next.attacker = attacker;
            if (next.handleHit()) {
               iterator.remove();
            }
         }
      }
   }

   private <T> void handleNewMob(Mob mob, Iterator<ClientSubmittedHits.SubmittedHit<T>> iterator) {
      while (iterator.hasNext()) {
         ClientSubmittedHits.SubmittedHit<T> next = iterator.next();
         if (next.targetUniqueID == mob.getUniqueID()) {
            next.target = mob;
            if (next.handleHit()) {
               iterator.remove();
            }
         }
      }
   }

   private class SubmittedHit<T> {
      public final int attackerUniqueID;
      public final int targetUniqueID;
      public final ServerClient client;
      public final long submitTime;
      private final SubmittedHitHandler<T> hitHandler;
      private final RemovedHitHandler<T> removedHandler;
      public T attacker;
      public Mob target;

      public SubmittedHit(
         int attackerUniqueID, T attacker, int targetUniqueID, ServerClient client, SubmittedHitHandler<T> hitHandler, RemovedHitHandler<T> removedHandler
      ) {
         this.attackerUniqueID = attackerUniqueID;
         this.attacker = attacker;
         this.targetUniqueID = targetUniqueID;
         this.client = client;
         this.submitTime = ClientSubmittedHits.this.level.getWorldEntity().getTime();
         this.hitHandler = hitHandler;
         this.removedHandler = removedHandler;
         this.target = GameUtils.getLevelMob(targetUniqueID, ClientSubmittedHits.this.level);
      }

      public boolean isStillValid() {
         return this.client.isDisposed() ? false : this.submitTime + 2000L >= ClientSubmittedHits.this.level.getWorldEntity().getTime();
      }

      public boolean handleHit() {
         if (this.attacker != null && this.target != null) {
            this.hitHandler.handleHit(this.client, this.attacker, this.target);
            return true;
         } else {
            return false;
         }
      }
   }
}
