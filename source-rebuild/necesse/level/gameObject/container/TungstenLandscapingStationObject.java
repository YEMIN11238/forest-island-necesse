package necesse.level.gameObject.container;

import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Tech;

public class TungstenLandscapingStationObject extends LandscapingStationObject {
   public static int[] registerTungstenLandscapingStation() {
      TungstenLandscapingStationObject o1 = new TungstenLandscapingStationObject();
      TungstenLandscapingStationObject.TungstenLandscapingStation2Object o2 = new TungstenLandscapingStationObject.TungstenLandscapingStation2Object();
      int cb1i = ObjectRegistry.registerObject("tungstenlandscapingstation", o1, 20.0F, true);
      int cb2i = ObjectRegistry.registerObject("tungstenlandscapingstation2", o2, 0.0F, false);
      o1.counterID = cb2i;
      o2.counterID = cb1i;
      return new int[]{cb1i, cb2i};
   }

   protected TungstenLandscapingStationObject() {
      this.rarity = Item.Rarity.UNCOMMON;
   }

   @Override
   public CraftingStationUpgrade getStationUpgrade() {
      return new CraftingStationUpgrade(
         ObjectRegistry.getObject("fallenlandscapingstation"), new Ingredient("basalt", 12), new Ingredient("emerald", 6), new Ingredient("alchemyshard", 5)
      );
   }

   @Override
   public void loadTextures() {
      this.texture = GameTexture.fromFile("objects/tungstenlandscapingstation");
   }

   @Override
   public Tech[] getCraftingTechs() {
      return new Tech[]{RecipeTechRegistry.TUNGSTEN_LANDSCAPING, RecipeTechRegistry.LANDSCAPING};
   }

   public static class TungstenLandscapingStation2Object extends LandscapingStationObject.LandscapingStation2Object {
      protected TungstenLandscapingStation2Object() {
      }

      @Override
      public void loadTextures() {
         this.texture = GameTexture.fromFile("objects/tungstenlandscapingstation");
      }
   }
}
