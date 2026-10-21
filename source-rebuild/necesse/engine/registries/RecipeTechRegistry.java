package necesse.engine.registries;

import java.util.NoSuchElementException;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.inventory.recipe.Tech;

public class RecipeTechRegistry extends GameRegistry<RecipeTechRegistry.TechRegistryElement> {
   @Deprecated
   public static Tech DEMONIC;
   @Deprecated
   public static Tech ADVANCED_WORKSTATION;
   public static Tech ALL;
   public static Tech NONE;
   public static Tech WORKSTATION;
   public static Tech COOKING_POT;
   public static Tech ROASTING_STATION;
   public static Tech FORGE;
   public static Tech CARPENTER;
   public static Tech LANDSCAPING;
   public static Tech IRON_ANVIL;
   public static Tech ALCHEMY;
   public static Tech DEMONIC_WORKSTATION;
   public static Tech COOKING_STATION;
   public static Tech DEMONIC_ANVIL;
   public static Tech VOID_ALCHEMY;
   public static Tech TUNGSTEN_WORKSTATION;
   public static Tech TUNGSTEN_ANVIL;
   public static Tech CAVEGLOW_ALCHEMY;
   public static Tech TUNGSTEN_CARPENTER;
   public static Tech TUNGSTEN_LANDSCAPING;
   public static Tech FALLEN_WORKSTATION;
   public static Tech FALLEN_ANVIL;
   public static Tech FALLEN_ALCHEMY;
   public static Tech FALLEN_CARPENTER;
   public static Tech FALLEN_LANDSCAPING;
   public static Tech TRANSMUTATION_STATION;
   public static Tech COMPOST_BIN;
   public static Tech GRAIN_MILL;
   public static Tech CHEESE_PRESS;
   public static final RecipeTechRegistry instance = new RecipeTechRegistry();

   private RecipeTechRegistry() {
      super("Recipe Tech", 65535);
   }

   @Override
   public void registerCore() {
      ALL = registerTech("all", "all");
      NONE = registerTech("none", "inventory", new LocalMessage("tech", "inventory"));
      WORKSTATION = registerTech("workstation", "workstation");
      COOKING_POT = registerTech("cookingpot", "cookingpot");
      ROASTING_STATION = registerTech("roastingstation", "roastingstation");
      FORGE = registerTech("forge", "forge");
      CARPENTER = registerTech("carpenter", "carpentersbench");
      LANDSCAPING = registerTech("landscaping", "landscapingstation");
      IRON_ANVIL = registerTech("ironanvil", "ironanvil");
      ALCHEMY = registerTech("alchemy", "alchemytable");
      DEMONIC_WORKSTATION = DEMONIC = registerTech("demonic", "demonicworkstation");
      COOKING_STATION = registerTech("cookingstation", "cookingstation");
      DEMONIC_ANVIL = registerTech("demonicanvil", "demonicanvil");
      VOID_ALCHEMY = registerTech("voidalchemy", "voidalchemytable");
      TUNGSTEN_WORKSTATION = ADVANCED_WORKSTATION = registerTech("tungstenworkstation", "tungstenworkstation");
      TUNGSTEN_ANVIL = registerTech("tungstenanvil", "tungstenanvil");
      TUNGSTEN_CARPENTER = registerTech("tungstencarpenter", "tungstencarpentersbench");
      TUNGSTEN_LANDSCAPING = registerTech("tungstenlandscaping", "tungstenlandscapingstation");
      CAVEGLOW_ALCHEMY = registerTech("caveglowalchemy", "caveglowalchemytable");
      FALLEN_WORKSTATION = registerTech("fallen", "fallenworkstation");
      FALLEN_ANVIL = registerTech("fallenanvil", "fallenanvil");
      FALLEN_ALCHEMY = registerTech("fallenalchemy", "fallenalchemytable");
      FALLEN_CARPENTER = registerTech("fallencarpenter", "fallencarpentersbench");
      FALLEN_LANDSCAPING = registerTech("fallenlandscaping", "fallenlandscapingstation");
      TRANSMUTATION_STATION = registerTech("transmutation", "transmutationstation");
      COMPOST_BIN = registerTech("compostbin", "compostbin", new LocalMessage("object", "compostbin"), null);
      GRAIN_MILL = registerTech("grainmill", "grainmill", new LocalMessage("object", "grainmill"), new LocalMessage("itemtooltip", "milltip"));
      CHEESE_PRESS = registerTech("cheesepress", "cheesepress", new LocalMessage("object", "cheesepress"), null);
   }

   protected void onRegister(RecipeTechRegistry.TechRegistryElement object, int id, String stringID, boolean isReplace) {
   }

   @Override
   protected void onRegistryClose() {
   }

   public static Tech registerTech(String stringID, String itemStringID, GameMessage displayName, GameMessage craftingMatTip) {
      Tech tech = new Tech(itemStringID, displayName, craftingMatTip);
      instance.register(stringID, new RecipeTechRegistry.TechRegistryElement(tech));
      return tech;
   }

   public static Tech registerTech(String stringID, String itemStringID, GameMessage displayName) {
      return registerTech(stringID, itemStringID, displayName, new LocalMessage("itemtooltip", "craftingmat"));
   }

   public static Tech registerTech(String stringID, String itemStringID) {
      return registerTech(stringID, itemStringID, new LocalMessage("tech", stringID));
   }

   public static Tech getTech(int ID) {
      return instance.getElement(ID).tech;
   }

   public static Tech getTech(String stringID) throws NoSuchElementException {
      int id = instance.getElementIDRaw(stringID);
      return getTech(id);
   }

   protected static class TechRegistryElement implements IDDataContainer {
      public final Tech tech;

      public TechRegistryElement(Tech tech) {
         this.tech = tech;
      }

      @Override
      public IDData getIDData() {
         return this.tech.data;
      }
   }
}
