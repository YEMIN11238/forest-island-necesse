package necesse.level.gameObject.container;

import java.awt.Color;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.recipe.Tech;

public class FallenAlchemyTableObject extends AlchemyTableObject {
   public FallenAlchemyTableObject() {
      this.mapColor = new Color(90, 89, 97);
      this.rarity = Item.Rarity.RARE;
   }

   @Override
   public void loadTextures() {
      this.texture = GameTexture.fromFile("objects/fallenalchemytable");
   }

   @Override
   public CraftingStationUpgrade getStationUpgrade() {
      return null;
   }

   @Override
   public Tech[] getCraftingTechs() {
      return new Tech[]{RecipeTechRegistry.FALLEN_ALCHEMY, RecipeTechRegistry.CAVEGLOW_ALCHEMY, RecipeTechRegistry.VOID_ALCHEMY, RecipeTechRegistry.ALCHEMY};
   }
}
