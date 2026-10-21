package necesse.level.gameObject.container;

import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Tech;

public class TungstenCarpentersBenchObject extends CarpentersBenchObject {
   public static int[] registerTungstenCarpentersBench() {
      TungstenCarpentersBenchObject cb1o = new TungstenCarpentersBenchObject();
      TungstenCarpentersBenchObject.TungstenCarpentersBench2Object cb2o = new TungstenCarpentersBenchObject.TungstenCarpentersBench2Object();
      int cb1i = ObjectRegistry.registerObject("tungstencarpentersbench", cb1o, 20.0F, true);
      int cb2i = ObjectRegistry.registerObject("tungstencarpentersbench2", cb2o, 0.0F, false);
      cb1o.counterID = cb2i;
      cb2o.counterID = cb1i;
      return new int[]{cb1i, cb2i};
   }

   protected TungstenCarpentersBenchObject() {
      this.rarity = Item.Rarity.UNCOMMON;
   }

   @Override
   public CraftingStationUpgrade getStationUpgrade() {
      return new CraftingStationUpgrade(
         ObjectRegistry.getObject("fallencarpentersbench"),
         new Ingredient("dryadlog", 16),
         new Ingredient("bone", 6),
         new Ingredient("bamboo", 4),
         new Ingredient("upgradeshard", 5)
      );
   }

   @Override
   public void loadTextures() {
      this.texture = GameTexture.fromFile("objects/tungstencarpentersbench");
   }

   @Override
   public Tech[] getCraftingTechs() {
      return new Tech[]{RecipeTechRegistry.TUNGSTEN_CARPENTER, RecipeTechRegistry.CARPENTER};
   }

   public static class TungstenCarpentersBench2Object extends CarpentersBenchObject.CarpentersBench2Object {
      protected TungstenCarpentersBench2Object() {
      }

      @Override
      public void loadTextures() {
         super.loadTextures();
         this.texture = GameTexture.fromFile("objects/tungstencarpentersbench");
      }
   }
}
