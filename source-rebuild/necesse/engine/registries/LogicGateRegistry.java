package necesse.engine.registries;

import java.util.stream.Collectors;
import necesse.engine.modLoader.LoadedMod;
import necesse.inventory.item.Item;
import necesse.level.gameLogicGate.GameLogicGate;
import necesse.level.gameLogicGate.SimpleEntityLogicGate;
import necesse.level.gameLogicGate.entities.AndLogicGateEntity;
import necesse.level.gameLogicGate.entities.BufferLogicGateEntity;
import necesse.level.gameLogicGate.entities.CountdownLogicGateEntity;
import necesse.level.gameLogicGate.entities.CountdownRelayLogicGateEntity;
import necesse.level.gameLogicGate.entities.CounterLogicGateEntity;
import necesse.level.gameLogicGate.entities.DelayLogicGateEntity;
import necesse.level.gameLogicGate.entities.NAndLogicGateEntity;
import necesse.level.gameLogicGate.entities.NOrLogicGateEntity;
import necesse.level.gameLogicGate.entities.OrLogicGateEntity;
import necesse.level.gameLogicGate.entities.SRLatchLogicGateEntity;
import necesse.level.gameLogicGate.entities.SensorLogicGateEntity;
import necesse.level.gameLogicGate.entities.SoundLogicGateEntity;
import necesse.level.gameLogicGate.entities.TFlipFlopLogicGateEntity;
import necesse.level.gameLogicGate.entities.TimerLogicGateEntity;
import necesse.level.gameLogicGate.entities.XOrLogicGateEntity;

public class LogicGateRegistry extends GameRegistry<LogicGateRegistry.LogicGateRegistryElement> {
   public static final LogicGateRegistry instance = new LogicGateRegistry();
   private static String[] stringIDs = null;

   private LogicGateRegistry() {
      super("LogicGate", 32762);
   }

   @Override
   public void registerCore() {
      registerLogicGate("andgate", new SimpleEntityLogicGate(AndLogicGateEntity::new), 2.0F, true);
      registerLogicGate("nandgate", new SimpleEntityLogicGate(NAndLogicGateEntity::new), 2.0F, true);
      registerLogicGate("orgate", new SimpleEntityLogicGate(OrLogicGateEntity::new), 2.0F, true);
      registerLogicGate("norgate", new SimpleEntityLogicGate(NOrLogicGateEntity::new), 2.0F, true);
      registerLogicGate("xorgate", new SimpleEntityLogicGate(XOrLogicGateEntity::new), 2.0F, true);
      registerLogicGate("tflipflopgate", new SimpleEntityLogicGate(TFlipFlopLogicGateEntity::new), 2.0F, true);
      registerLogicGate("srlatchgate", new SimpleEntityLogicGate(SRLatchLogicGateEntity::new), 2.0F, true);
      registerLogicGate("countergate", new SimpleEntityLogicGate(CounterLogicGateEntity::new), 10.0F, true);
      registerLogicGate("timergate", new SimpleEntityLogicGate(TimerLogicGateEntity::new), 10.0F, true);
      registerLogicGate("buffergate", new SimpleEntityLogicGate(BufferLogicGateEntity::new), 10.0F, true);
      registerLogicGate("delaygate", new SimpleEntityLogicGate(DelayLogicGateEntity::new), 10.0F, true);
      registerLogicGate("sensorgate", new SimpleEntityLogicGate(SensorLogicGateEntity::new), 10.0F, true);
      registerLogicGate("soundgate", new SimpleEntityLogicGate(SoundLogicGateEntity::new), 10.0F, true);
      registerLogicGate("countdowngate", new SimpleEntityLogicGate(CountdownLogicGateEntity::new), 10.0F, false);
      registerLogicGate("countdownrelay", new SimpleEntityLogicGate(CountdownRelayLogicGateEntity::new), 2.0F, false);
   }

   protected void onRegister(LogicGateRegistry.LogicGateRegistryElement element, int id, String stringID, boolean isReplace) {
      Item item = element.gate.generateNewItem();
      if (item != null) {
         ItemRegistry.registerItem(element.gate.getStringID(), item, element.itemBrokerValue, element.itemObtainable);
      }
   }

   @Override
   protected void onRegistryClose() {
      this.streamElements().map(e -> e.gate).forEach(GameLogicGate::updateLocalDisplayName);

      for (LogicGateRegistry.LogicGateRegistryElement element : this.getElements()) {
         element.gate.onLogicGateRegistryClosed();
      }

      stringIDs = instance.streamElements().map(e -> e.gate.getStringID()).toArray(String[]::new);
   }

   public static int registerLogicGate(String stringID, GameLogicGate logicGate, float itemBrokerValue, boolean itemObtainable) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register logic gates");
      } else {
         return instance.register(stringID, new LogicGateRegistry.LogicGateRegistryElement(logicGate, itemBrokerValue, itemObtainable));
      }
   }

   public static GameLogicGate getLogicGate(int id) {
      LogicGateRegistry.LogicGateRegistryElement element = instance.getElement(id);
      return element == null ? null : element.gate;
   }

   public static int getLogicGateID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static String getLogicGateStringID(int id) {
      return instance.getElementStringID(id);
   }

   public static Iterable<GameLogicGate> getLogicGates() {
      return instance.streamElements().map(e -> e.gate).collect(Collectors.toList());
   }

   public static String[] getGateStringIDs() {
      if (stringIDs == null) {
         throw new IllegalStateException("LogicGateRegistry not yet closed");
      } else {
         return stringIDs;
      }
   }

   protected static class LogicGateRegistryElement implements IDDataContainer {
      public final GameLogicGate gate;
      public final float itemBrokerValue;
      public final boolean itemObtainable;

      public LogicGateRegistryElement(GameLogicGate gate, float itemBrokerValue, boolean itemObtainable) {
         this.gate = gate;
         this.itemBrokerValue = itemBrokerValue;
         this.itemObtainable = itemObtainable;
      }

      @Override
      public IDData getIDData() {
         return this.gate.idData;
      }
   }
}
