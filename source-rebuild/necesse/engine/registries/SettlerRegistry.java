package necesse.engine.registries;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import necesse.engine.modLoader.LoadedMod;
import necesse.level.maps.levelData.settlementData.settler.AlchemistSettler;
import necesse.level.maps.levelData.settlementData.settler.AnglerSettler;
import necesse.level.maps.levelData.settlementData.settler.AnimalKeeperSettler;
import necesse.level.maps.levelData.settlementData.settler.BlacksmithSettler;
import necesse.level.maps.levelData.settlementData.settler.ElderSettler;
import necesse.level.maps.levelData.settlementData.settler.ExoticMerchantSettler;
import necesse.level.maps.levelData.settlementData.settler.ExplorerSettler;
import necesse.level.maps.levelData.settlementData.settler.FarmerSettler;
import necesse.level.maps.levelData.settlementData.settler.GenericSettler;
import necesse.level.maps.levelData.settlementData.settler.GuardSettler;
import necesse.level.maps.levelData.settlementData.settler.GunsmithSettler;
import necesse.level.maps.levelData.settlementData.settler.HunterSettler;
import necesse.level.maps.levelData.settlementData.settler.LockedNoSettler;
import necesse.level.maps.levelData.settlementData.settler.MageSettler;
import necesse.level.maps.levelData.settlementData.settler.MinerSettler;
import necesse.level.maps.levelData.settlementData.settler.PawnBrokerSettler;
import necesse.level.maps.levelData.settlementData.settler.PirateSettler;
import necesse.level.maps.levelData.settlementData.settler.Settler;
import necesse.level.maps.levelData.settlementData.settler.StylistSettler;
import necesse.level.maps.levelData.settlementData.settler.TraderSettler;

public class SettlerRegistry extends GameRegistry<SettlerRegistry.SettlerRegistryElement> {
   public static final SettlerRegistry instance = new SettlerRegistry();
   public static final LockedNoSettler SETTLER_LOCKED = new LockedNoSettler();

   private SettlerRegistry() {
      super("Settler", 32767);
   }

   @Override
   public void registerCore() {
      registerSettler("elder", new ElderSettler());
      registerSettler("farmer", new FarmerSettler());
      registerSettler("blacksmith", new BlacksmithSettler());
      registerSettler("mage", new MageSettler());
      registerSettler("hunter", new HunterSettler());
      registerSettler("gunsmith", new GunsmithSettler());
      registerSettler("alchemist", new AlchemistSettler());
      registerSettler("angler", new AnglerSettler());
      registerSettler("pawnbroker", new PawnBrokerSettler());
      registerSettler("trader", new TraderSettler());
      registerSettler("exoticmerchant", new ExoticMerchantSettler());
      registerSettler("explorer", new ExplorerSettler());
      registerSettler("miner", new MinerSettler());
      registerSettler("animalkeeper", new AnimalKeeperSettler());
      registerSettler("stylist", new StylistSettler());
      registerSettler("pirate", new PirateSettler());
      registerSettler("guard", new GuardSettler());
      registerSettler("generic", new GenericSettler());
   }

   protected void onRegister(SettlerRegistry.SettlerRegistryElement object, int id, String stringID, boolean isReplace) {
   }

   @Override
   protected void onRegistryClose() {
      for (SettlerRegistry.SettlerRegistryElement element : this.getElements()) {
         element.settler.onSettlerRegistryClosed();
      }
   }

   public static void loadSettlerIconTextures() {
      instance.streamElements().map(e -> e.settler).forEach(Settler::loadTextures);
   }

   public static int registerSettler(String stringID, Settler settler) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register settlers");
      } else {
         return instance.register(stringID, new SettlerRegistry.SettlerRegistryElement(settler));
      }
   }

   public static int replaceSettler(String stringID, Settler settler) {
      return instance.replace(stringID, new SettlerRegistry.SettlerRegistryElement(settler));
   }

   public static Settler getSettler(int id) {
      if (id >= 0 && id < instance.size()) {
         SettlerRegistry.SettlerRegistryElement e = instance.getElement(id);
         return e == null ? null : e.settler;
      } else {
         return null;
      }
   }

   public static Settler getSettler(String stringID) {
      SettlerRegistry.SettlerRegistryElement e = instance.getElement(stringID);
      return e == null ? null : e.settler;
   }

   public static int getSettlerID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static String getSettlerStringID(int id) {
      return instance.getElementStringID(id);
   }

   public static Stream<Settler> streamSettlers() {
      return instance.streamElements().map(s -> s.settler);
   }

   public static List<Settler> getSettlers() {
      return streamSettlers().collect(Collectors.toList());
   }

   protected static class SettlerRegistryElement implements IDDataContainer {
      public final Settler settler;

      public SettlerRegistryElement(Settler settler) {
         this.settler = settler;
      }

      @Override
      public IDData getIDData() {
         return this.settler.idData;
      }
   }
}
