package necesse.level.gameObject.container;

import java.awt.Color;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Tech;

public class VoidAlchemyTableObject extends AlchemyTableObject {
   public VoidAlchemyTableObject() {
      this.mapColor = new Color(148, 99, 25);
      this.rarity = Item.Rarity.COMMON;
   }

   @Override
   public CraftingStationUpgrade getStationUpgrade() {
      return new CraftingStationUpgrade(ObjectRegistry.getObject("caveglowalchemytable"), new Ingredient("glassbottle", 6), new Ingredient("caveglow", 12));
   }

   @Override
   public void loadTextures() {
      this.texture = GameTexture.fromFile("objects/voidalchemytable");
   }

   @Override
   public Tech[] getCraftingTechs() {
      return new Tech[]{RecipeTechRegistry.VOID_ALCHEMY, RecipeTechRegistry.ALCHEMY};
   }
}
