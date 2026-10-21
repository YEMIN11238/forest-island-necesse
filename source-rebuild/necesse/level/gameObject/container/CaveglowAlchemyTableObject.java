package necesse.level.gameObject.container;

import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Tech;

public class CaveglowAlchemyTableObject extends AlchemyTableObject {
   public CaveglowAlchemyTableObject() {
      this.rarity = Item.Rarity.UNCOMMON;
   }

   @Override
   public CraftingStationUpgrade getStationUpgrade() {
      return new CraftingStationUpgrade(
         ObjectRegistry.getObject("fallenalchemytable"), new Ingredient("anytier1essence", 8), new Ingredient("alchemyshard", 15)
      );
   }

   @Override
   public void loadTextures() {
      this.texture = GameTexture.fromFile("objects/caveglowalchemytable");
   }

   @Override
   public Tech[] getCraftingTechs() {
      return new Tech[]{RecipeTechRegistry.CAVEGLOW_ALCHEMY, RecipeTechRegistry.VOID_ALCHEMY, RecipeTechRegistry.ALCHEMY};
   }
}
