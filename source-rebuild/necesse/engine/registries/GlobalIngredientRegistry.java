package necesse.engine.registries;

import java.util.NoSuchElementException;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.GameUtils;
import necesse.inventory.recipe.GlobalIngredient;

public class GlobalIngredientRegistry extends GameRegistry<GlobalIngredientRegistry.GlobalIngredientRegistryElement> {
   public static final GlobalIngredientRegistry instance = new GlobalIngredientRegistry();

   private GlobalIngredientRegistry() {
      super("GlobalIngredient", 32767);
   }

   @Override
   public void registerCore() {
      registerGlobalIngredient("anycoolingfuel", new LocalMessage("item", "anycoolingfuel"), new LocalMessage("itemtooltip", "coolingfueltip"));

      for (int i = 0; i < 10; i++) {
         registerGlobalIngredient("anytier" + i + "essence", new LocalMessage("item", "anytieressence", "tier", i), null);
      }
   }

   protected void onRegister(GlobalIngredientRegistry.GlobalIngredientRegistryElement object, int id, String stringID, boolean isReplace) {
   }

   @Override
   protected void onRegistryClose() {
      for (GlobalIngredientRegistry.GlobalIngredientRegistryElement element : this.getElements()) {
         element.ingredient.onGlobalIngredientRegistryClosed();
      }
   }

   public static int registerGlobalIngredient(String stringID, GameMessage displayName, GameMessage itemTooltip) {
      GlobalIngredient newIngredient = new GlobalIngredient(displayName, itemTooltip);
      return instance.register(stringID, new GlobalIngredientRegistry.GlobalIngredientRegistryElement(newIngredient));
   }

   public static GlobalIngredient getGlobalIngredient(String stringID) {
      try {
         int id = instance.getElementIDRaw(stringID);
         return instance.getElement(id).ingredient;
      } catch (NoSuchElementException e) {
         int id = registerGlobalIngredient(stringID, new LocalMessage("item", stringID), null);
         return instance.getElement(id).ingredient;
      }
   }

   public static int getGlobalIngredientID(String stringID) {
      return getGlobalIngredient(stringID).getID();
   }

   public static GlobalIngredient getGlobalIngredient(int id) {
      return instance.getElement(id).ingredient;
   }

   public static Iterable<GlobalIngredient> getGlobalIngredients() {
      return GameUtils.mapIterable(instance.getElements().iterator(), e -> e.ingredient);
   }

   protected static class GlobalIngredientRegistryElement implements IDDataContainer {
      public final GlobalIngredient ingredient;

      public GlobalIngredientRegistryElement(GlobalIngredient ingredient) {
         this.ingredient = ingredient;
      }

      @Override
      public IDData getIDData() {
         return this.ingredient.idData;
      }
   }
}
