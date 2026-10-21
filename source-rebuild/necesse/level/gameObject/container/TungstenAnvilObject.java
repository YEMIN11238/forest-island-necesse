package necesse.level.gameObject.container;

import java.awt.Color;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Tech;

public class TungstenAnvilObject extends IronAnvilObject {
   public TungstenAnvilObject() {
      this.mapColor = new Color(51, 53, 56);
      this.rarity = Item.Rarity.UNCOMMON;
   }

   @Override
   public CraftingStationUpgrade getStationUpgrade() {
      return new CraftingStationUpgrade(ObjectRegistry.getObject("fallenanvil"), new Ingredient("anytier1essence", 10), new Ingredient("upgradeshard", 10));
   }

   @Override
   public void loadTextures() {
      this.texture = GameTexture.fromFile("objects/tungstenanvil");
   }

   @Override
   public Tech[] getCraftingTechs() {
      return new Tech[]{RecipeTechRegistry.TUNGSTEN_ANVIL, RecipeTechRegistry.DEMONIC_ANVIL, RecipeTechRegistry.IRON_ANVIL};
   }
}
