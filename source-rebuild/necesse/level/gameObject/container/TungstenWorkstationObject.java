package necesse.level.gameObject.container;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.HashSet;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Tech;

public class TungstenWorkstationObject extends WorkstationDuoObject {
   public static int[] registerWorkstation() {
      TungstenWorkstationObject o1 = new TungstenWorkstationObject();
      TungstenWorkstationObject.TungstenWorkstation2Object o2 = new TungstenWorkstationObject.TungstenWorkstation2Object();
      int i1 = ObjectRegistry.registerObject("tungstenworkstation", o1, 140.0F, true);
      int i2 = ObjectRegistry.registerObject("tungstenworkstation2", o2, 0.0F, false);
      o1.counterID = i2;
      o2.counterID = i1;
      return new int[]{i1, i2};
   }

   protected TungstenWorkstationObject() {
      this.mapColor = new Color(65, 69, 89);
      this.rarity = Item.Rarity.UNCOMMON;
   }

   @Override
   public GameMessage getNewLocalization() {
      return new LocalMessage("object", "tungstenworkstation");
   }

   @Override
   public CraftingStationUpgrade getStationUpgrade() {
      return new CraftingStationUpgrade(ObjectRegistry.getObject("fallenworkstation"), new Ingredient("upgradeshard", 15), new Ingredient("alchemyshard", 15));
   }

   @Override
   public HashSet<ItemCategory> getForcedSoloCraftingCategories() {
      HashSet<ItemCategory> depths = super.getForcedSoloCraftingCategories();
      depths.add(ItemCategory.craftingManager.getCategory("equipment", "trinkets"));
      return depths;
   }

   @Override
   public void loadTextures() {
      this.texture = GameTexture.fromFile("objects/tungstenworkstation");
   }

   @Override
   public Tech[] getCraftingTechs() {
      return new Tech[]{RecipeTechRegistry.TUNGSTEN_WORKSTATION, RecipeTechRegistry.DEMONIC_WORKSTATION, RecipeTechRegistry.WORKSTATION};
   }

   public static class TungstenWorkstation2Object extends WorkstationDuoObject.WorkstationDuo2Object {
      protected TungstenWorkstation2Object() {
         this.mapColor = new Color(65, 69, 89);
         this.toolType = ToolType.ALL;
         this.isLightTransparent = true;
         this.hoverHitbox = new Rectangle(0, -16, 32, 48);
      }

      @Override
      public void loadTextures() {
         this.texture = GameTexture.fromFile("objects/tungstenworkstation");
      }
   }
}
