package necesse.level.maps.levelData.settlementData.notifications;

import necesse.engine.modLoader.LoadedMod;
import necesse.engine.registries.GameRegistry;
import necesse.engine.registries.IDData;
import necesse.engine.registries.IDDataContainer;

public class SettlementNotificationRegistry extends GameRegistry<SettlementNotificationRegistry.NotificationRegistryElement> {
   public static final SettlementNotificationRegistry instance = new SettlementNotificationRegistry();

   private SettlementNotificationRegistry() {
      super("SettlementNotification", 32762);
   }

   @Override
   public void registerCore() {
      registerNotification("lowfood", new LowFoodSettlementNotification());
      registerNotification("hungry", new HungrySettlementNotification());
      registerNotification("fullinventory", new FullInventorySettlementNotification());
      registerNotification("nobed", new NoBedSettlementNotification());
      registerNotification("noflag", new SettlementNoFlagNotification());
      registerNotification("lowqol", new LowQoLSettlementNotification());
   }

   protected void onRegister(SettlementNotificationRegistry.NotificationRegistryElement object, int id, String stringID, boolean isReplace) {
   }

   @Override
   protected void onRegistryClose() {
   }

   public static int registerNotification(String stringID, SettlementNotification notification) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register settlement notifications");
      } else {
         return instance.register(stringID, new SettlementNotificationRegistry.NotificationRegistryElement(notification));
      }
   }

   public static int replaceNotification(String stringID, SettlementNotification notification) {
      return instance.register(stringID, new SettlementNotificationRegistry.NotificationRegistryElement(notification));
   }

   public static SettlementNotification getNotification(int id) {
      if (id >= instance.size()) {
         id = 0;
      }

      SettlementNotificationRegistry.NotificationRegistryElement element = instance.getElement(id);
      if (element == null) {
         throw new NullPointerException(instance.objectCallName + " ID " + id + " seems to be missing or corrupted");
      } else {
         return element.notification;
      }
   }

   public static int getNotificationID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static SettlementNotification getNotification(String stringID) {
      SettlementNotificationRegistry.NotificationRegistryElement element = instance.getElement(stringID);
      if (element == null) {
         throw new NullPointerException(instance.objectCallName + " stringID " + stringID + " seems to be missing or corrupted");
      } else {
         return element.notification;
      }
   }

   protected static class NotificationRegistryElement implements IDDataContainer {
      public final SettlementNotification notification;

      public NotificationRegistryElement(SettlementNotification notification) {
         this.notification = notification;
      }

      @Override
      public IDData getIDData() {
         return this.notification.idData;
      }
   }
}
