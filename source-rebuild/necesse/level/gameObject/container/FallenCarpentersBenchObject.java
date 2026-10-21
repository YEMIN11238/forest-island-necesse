package necesse.level.gameObject.container;

import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.recipe.Tech;

public class FallenCarpentersBenchObject extends CarpentersBenchObject {
   public static int[] registerFallenCarpentersBench() {
      FallenCarpentersBenchObject cb1o = new FallenCarpentersBenchObject();
      FallenCarpentersBenchObject.FallenCarpentersBench2Object cb2o = new FallenCarpentersBenchObject.FallenCarpentersBench2Object();
      int cb1i = ObjectRegistry.registerObject("fallencarpentersbench", cb1o, 20.0F, true);
      int cb2i = ObjectRegistry.registerObject("fallencarpentersbench2", cb2o, 0.0F, false);
      cb1o.counterID = cb2i;
      cb2o.counterID = cb1i;
      return new int[]{cb1i, cb2i};
   }

   protected FallenCarpentersBenchObject() {
      this.rarity = Item.Rarity.RARE;
   }

   @Override
   public CraftingStationUpgrade getStationUpgrade() {
      return null;
   }

   @Override
   public void loadTextures() {
      this.texture = GameTexture.fromFile("objects/fallencarpentersbench");
   }

   @Override
   public Tech[] getCraftingTechs() {
      return new Tech[]{RecipeTechRegistry.FALLEN_CARPENTER, RecipeTechRegistry.TUNGSTEN_CARPENTER, RecipeTechRegistry.CARPENTER};
   }

   public static class FallenCarpentersBench2Object extends CarpentersBenchObject.CarpentersBench2Object {
      protected FallenCarpentersBench2Object() {
      }

      @Override
      public void loadTextures() {
         super.loadTextures();
         this.texture = GameTexture.fromFile("objects/fallencarpentersbench");
      }
   }
}
