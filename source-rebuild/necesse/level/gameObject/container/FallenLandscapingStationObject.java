package necesse.level.gameObject.container;

import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.recipe.Tech;

public class FallenLandscapingStationObject extends LandscapingStationObject {
   public static int[] registerFallenLandscapingStation() {
      FallenLandscapingStationObject o1 = new FallenLandscapingStationObject();
      FallenLandscapingStationObject.FallenLandscapingStation2Object o2 = new FallenLandscapingStationObject.FallenLandscapingStation2Object();
      int cb1i = ObjectRegistry.registerObject("fallenlandscapingstation", o1, 20.0F, true);
      int cb2i = ObjectRegistry.registerObject("fallenlandscapingstation2", o2, 0.0F, false);
      o1.counterID = cb2i;
      o2.counterID = cb1i;
      return new int[]{cb1i, cb2i};
   }

   protected FallenLandscapingStationObject() {
      this.rarity = Item.Rarity.RARE;
   }

   @Override
   public CraftingStationUpgrade getStationUpgrade() {
      return null;
   }

   @Override
   public void loadTextures() {
      this.texture = GameTexture.fromFile("objects/fallenlandscapingstation");
   }

   @Override
   public Tech[] getCraftingTechs() {
      return new Tech[]{RecipeTechRegistry.FALLEN_LANDSCAPING, RecipeTechRegistry.TUNGSTEN_LANDSCAPING, RecipeTechRegistry.LANDSCAPING};
   }

   public static class FallenLandscapingStation2Object extends LandscapingStationObject.LandscapingStation2Object {
      protected FallenLandscapingStation2Object() {
      }

      @Override
      public void loadTextures() {
         this.texture = GameTexture.fromFile("objects/fallenlandscapingstation");
      }
   }
}
