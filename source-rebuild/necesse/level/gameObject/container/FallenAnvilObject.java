package necesse.level.gameObject.container;

import java.awt.Color;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.recipe.Tech;

public class FallenAnvilObject extends IronAnvilObject {
   public FallenAnvilObject() {
      this.mapColor = new Color(51, 53, 56);
      this.rarity = Item.Rarity.RARE;
   }

   @Override
   public CraftingStationUpgrade getStationUpgrade() {
      return null;
   }

   @Override
   public void loadTextures() {
      this.texture = GameTexture.fromFile("objects/fallenanvil");
   }

   @Override
   public Tech[] getCraftingTechs() {
      return new Tech[]{RecipeTechRegistry.FALLEN_ANVIL, RecipeTechRegistry.TUNGSTEN_ANVIL, RecipeTechRegistry.DEMONIC_ANVIL, RecipeTechRegistry.IRON_ANVIL};
   }
}
