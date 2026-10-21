package necesse.entity.mobs;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.ability.MobAbility;
import necesse.inventory.container.settlement.events.SettlementSettlerActivityDataEvent;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;

public class ActivityDescription {
   public final Mob mob;
   public static int activityTimeoutMS = 500;
   protected HashMap<String, ActivityDescription.Activity> currentActivities = new HashMap<>();
   protected GameLinkedList<ActivityDescription.Activity> priorities = new GameLinkedList<>();
   protected GameLinkedList<ActivityDescription.Activity> timeouts = new GameLinkedList<>();
   protected ActivityDescription.MobSetActivityAbility setActivityAbility;
   protected GameMessage currentActivity;

   public ActivityDescription(Mob mob) {
      this.mob = mob;
      this.setActivityAbility = mob.registerAbility(new ActivityDescription.MobSetActivityAbility());
   }

   public void writeSpawnPacket(PacketWriter writer) {
      writer.putNextBoolean(this.currentActivity != null);
      if (this.currentActivity != null) {
         this.currentActivity.writePacket(writer);
      }
   }

   public void readSpawnPacket(PacketReader reader) {
      if (reader.getNextBoolean()) {
         this.currentActivity = GameMessage.fromPacket(reader);
      } else {
         this.currentActivity = null;
      }
   }

   public void serverTick() {
      GameMessage last = this.currentActivity;
      this.refreshCurrentActive();
      GameMessage next = this.currentActivity;
      if (!GameMessage.isSame(last, next) && this.mob.isServer()) {
         this.setActivityAbility.updateActivity(next);
         if (this.mob instanceof SettlerMob) {
            SettlerMob settlerMob = (SettlerMob)this.mob;
            if (settlerMob.isSettler()) {
               ServerSettlementData serverData = settlerMob.getSettlerSettlementServerData();
               if (serverData != null) {
                  new SettlementSettlerActivityDataEvent(serverData, Collections.singletonList(this.mob.getUniqueID()))
                     .applyAndSendToAllClients(this.mob.getServer());
               }
            }
         }
      }
   }

   public void refreshCurrentActive() {
      while (!this.timeouts.isEmpty() && this.timeouts.getFirst().shouldTimeout()) {
         this.timeouts.getFirst().remove();
      }

      ActivityDescription.Activity first = this.priorities.getFirst();
      if (first != null) {
         this.currentActivity = first.description;
      } else {
         this.currentActivity = null;
      }
   }

   public void setActivity(String type, int priority, GameMessage description) {
      this.currentActivities.compute(type, (s, last) -> {
         if (last == null) {
            return new ActivityDescription.Activity(type, priority, activityTimeoutMS, description);
         }

         last.refreshTimeoutTime(activityTimeoutMS);
         last.setPriority(priority);
         last.description = description;
         return (ActivityDescription.Activity)last;
      });
   }

   public void clearActivity(String type) {
      ActivityDescription.Activity current = this.currentActivities.get(type);
      if (current != null) {
         current.remove();
      }
   }

   public GameMessage getCurrentActivity() {
      return this.currentActivity;
   }

   private class Activity {
      public final String type;
      public int priority;
      public long timeoutLocalTime;
      public GameLinkedList<ActivityDescription.Activity>.Element priorityElement;
      public GameLinkedList<ActivityDescription.Activity>.Element timeoutElement;
      public GameMessage description;

      public Activity(String type, int priority, int timeoutMS, GameMessage description) {
         this.type = type;
         this.description = description;
         this.setPriority(priority);
         this.refreshTimeoutTime(timeoutMS);
      }

      public void setPriority(int priority) {
         if (this.priority != priority || this.priorityElement == null) {
            if (this.priorityElement != null) {
               this.priorityElement.remove();
            }

            this.priorityElement = GameUtils.insertSortedList(ActivityDescription.this.priorities, this, Comparator.comparingInt(p -> -p.priority));
         }
      }

      public void refreshTimeoutTime(int timeoutMS) {
         long nextTimeoutTime = ActivityDescription.this.mob.getWorldEntity().getLocalTime() + timeoutMS;
         if (this.timeoutLocalTime != nextTimeoutTime || this.timeoutElement == null) {
            this.timeoutLocalTime = nextTimeoutTime;
            if (this.timeoutElement != null) {
               this.timeoutElement.remove();
            }

            this.timeoutElement = GameUtils.insertSortedListReversed(ActivityDescription.this.timeouts, this, Comparator.comparingLong(p -> p.timeoutLocalTime));
         }
      }

      public boolean shouldTimeout() {
         return ActivityDescription.this.mob.getWorldEntity().getLocalTime() >= this.timeoutLocalTime;
      }

      public void remove() {
         if (this.priorityElement != null) {
            this.priorityElement.remove();
            this.priorityElement = null;
         }

         if (this.timeoutElement != null) {
            this.timeoutElement.remove();
            this.timeoutElement = null;
         }

         ActivityDescription.this.currentActivities.remove(this.type);
      }
   }

   private class MobSetActivityAbility extends MobAbility {
      public MobSetActivityAbility() {
      }

      public void updateActivity(GameMessage description) {
         Packet packet = new Packet();
         PacketWriter writer = new PacketWriter(packet);
         writer.putNextBoolean(description != null);
         if (description != null) {
            description.writePacket(writer);
         }

         this.runAndSendAbility(packet);
      }

      @Override
      public void executePacket(PacketReader reader) {
         if (reader.getNextBoolean()) {
            ActivityDescription.this.currentActivity = GameMessage.fromPacket(reader);
         } else {
            ActivityDescription.this.currentActivity = null;
         }
      }
   }
}
