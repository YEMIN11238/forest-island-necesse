package necesse.engine.registries;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.settler.personalities.SettlerPersonalityFilter;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.AspiringHeroRomancePersonality;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.HonestFarmerRomancePersonality;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.QuietCompassionateRomancePersonality;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.SettlerRomancePersonality;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.SettlerRomancePersonalityConstructor;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.WealthIsAllRomancePersonality;

public class SettlerRomancePersonalityRegistry extends GameRegistry<SettlerRomancePersonalityRegistry.SettlerRomancePersonalityRegistryElement> {
   public static final SettlerRomancePersonalityRegistry instance = new SettlerRomancePersonalityRegistry();

   private SettlerRomancePersonalityRegistry() {
      super("SettlerRomancePersonality", 32767);
   }

   @Override
   public void registerCore() {
      registerSettlerPersonality("honestfarmer", HonestFarmerRomancePersonality.class);
      registerSettlerPersonality("aspiringhero", AspiringHeroRomancePersonality.class);
      registerSettlerPersonality("quietcompassionate", QuietCompassionateRomancePersonality.class);
      registerSettlerPersonality("wealthisall", WealthIsAllRomancePersonality.class);
   }

   protected void onRegister(SettlerRomancePersonalityRegistry.SettlerRomancePersonalityRegistryElement object, int id, String stringID, boolean isReplace) {
      try {
         object.newInstance(null).onRegistered();
      } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
         throw new RuntimeException(
            "Error registering settler personality \"" + stringID + "\". Make sure we can construct a personality with null HumanMob parameter", e
         );
      }
   }

   @Override
   protected void onRegistryClose() {
   }

   public static int registerSettlerPersonality(String stringID, Class<? extends SettlerRomancePersonality> personalityClass) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register settler personalities");
      }

      try {
         return instance.register(stringID, new SettlerRomancePersonalityRegistry.SettlerRomancePersonalityRegistryElement(personalityClass));
      } catch (NoSuchMethodException e) {
         System.err.println("Could not register SettlerPersonality " + personalityClass.getSimpleName() + ": Missing constructor with HumanMob parameter");
         return -1;
      }
   }

   public static int registerSettlerPersonality(
      String stringID, SettlerRomancePersonalityConstructor personalityConstructor, SettlerPersonalityFilter filter, boolean isBonusPerk
   ) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot settler personalities");
      } else {
         return instance.register(stringID, new SettlerRomancePersonalityRegistry.SettlerRomancePersonalityRegistryElement(personalityConstructor));
      }
   }

   public static SettlerRomancePersonality getNewSettlerRomancePersonality(int id, HumanMob mob) {
      if (id >= 0 && id < instance.size()) {
         try {
            return instance.getElement(id).newInstance(mob);
         } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
         }
      } else {
         return null;
      }
   }

   public static SettlerRomancePersonality getNewSettlerRomancePersonality(String stringID, HumanMob mob) {
      return getNewSettlerRomancePersonality(getSettlerRomancePersonalityID(stringID), mob);
   }

   public static SettlerRomancePersonality getNewRandomSettlerRomancePersonality(HumanMob mob, GameRandom random) {
      TicketSystemList<SettlerRomancePersonalityRegistry.SettlerRomancePersonalityRegistryElement> allRomancePersonalities = new TicketSystemList<>();

      for (SettlerRomancePersonalityRegistry.SettlerRomancePersonalityRegistryElement element : instance.getElements()) {
         allRomancePersonalities.addObject(100, element);
      }

      int romancePersonalityChosenID = allRomancePersonalities.getRandomObject(random).getID();
      return getNewSettlerRomancePersonality(romancePersonalityChosenID, mob);
   }

   public static int getSettlerRomancePersonalityID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static String getSettlerPersonalityStringID(int id) {
      return instance.getElementStringID(id);
   }

   protected static class SettlerRomancePersonalityRegistryElement implements IDDataContainer {
      public final IDData idData = new IDData();
      private final SettlerRomancePersonalityConstructor constructor;

      public SettlerRomancePersonalityRegistryElement(Class<? extends SettlerRomancePersonality> romancePersonalityClass) throws NoSuchMethodException {
         Constructor<? extends SettlerRomancePersonality> constructor = romancePersonalityClass.getConstructor(HumanMob.class);
         this.constructor = xva$0 -> constructor.newInstance(xva$0);
      }

      public SettlerRomancePersonalityRegistryElement(SettlerRomancePersonalityConstructor constructor) {
         this.constructor = constructor;
      }

      @Override
      public IDData getIDData() {
         return this.idData;
      }

      public SettlerRomancePersonality newInstance(HumanMob mob) throws IllegalAccessException, InvocationTargetException, InstantiationException {
         SettlerRomancePersonality personality = this.constructor.construct(mob);
         personality.getIDData().setData(this.idData.getID(), this.idData.getStringID());
         return personality;
      }
   }
}
