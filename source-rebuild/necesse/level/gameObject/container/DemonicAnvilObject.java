package necesse.level.gameObject.container;

import java.awt.Color;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Tech;

public class DemonicAnvilObject extends IronAnvilObject {
   public DemonicAnvilObject() {
      this.mapColor = new Color(152, 55, 35);
      this.rarity = Item.Rarity.COMMON;
      this.lightLevel = 100;
      this.lightHue = 270.0F;
      this.lightSat = 0.3F;
   }

   @Override
   public CraftingStationUpgrade getStationUpgrade() {
      return new CraftingStationUpgrade(ObjectRegistry.getObject("tungstenanvil"), new Ingredient("tungstenbar", 6), new Ingredient("quartz", 8));
   }

   @Override
   public void loadTextures() {
      this.texture = GameTexture.fromFile("objects/demonicanvil");
   }

   @Override
   public Tech[] getCraftingTechs() {
      return new Tech[]{RecipeTechRegistry.DEMONIC_ANVIL, RecipeTechRegistry.IRON_ANVIL};
   }
}
