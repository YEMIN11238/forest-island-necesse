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
import necesse.inventory.recipe.Tech;

public class FallenWorkstationObject extends WorkstationDuoObject {
   public static int[] registerWorkstation() {
      FallenWorkstationObject o1 = new FallenWorkstationObject();
      FallenWorkstationObject.FallenWorkstation2Object o2 = new FallenWorkstationObject.FallenWorkstation2Object();
      int i1 = ObjectRegistry.registerObject("fallenworkstation", o1, 200.0F, true);
      int i2 = ObjectRegistry.registerObject("fallenworkstation2", o2, 0.0F, false);
      o1.counterID = i2;
      o2.counterID = i1;
      return new int[]{i1, i2};
   }

   protected FallenWorkstationObject() {
      this.mapColor = new Color(0, 107, 109);
      this.rarity = Item.Rarity.RARE;
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.lightLevel = 100;
      this.lightHue = 220.0F;
      this.lightSat = 0.2F;
      this.hoverHitbox = new Rectangle(0, -16, 32, 48);
   }

   @Override
   public CraftingStationUpgrade getStationUpgrade() {
      return null;
   }

   @Override
   public GameMessage getNewLocalization() {
      return new LocalMessage("object", "fallenworkstation");
   }

   @Override
   public HashSet<ItemCategory> getForcedSoloCraftingCategories() {
      HashSet<ItemCategory> depths = super.getForcedSoloCraftingCategories();
      depths.add(ItemCategory.craftingManager.getCategory("equipment", "trinkets"));
      return depths;
   }

   @Override
   public void loadTextures() {
      this.texture = GameTexture.fromFile("objects/fallenworkstation");
   }

   @Override
   public Tech[] getCraftingTechs() {
      return new Tech[]{
         RecipeTechRegistry.FALLEN_WORKSTATION, RecipeTechRegistry.ADVANCED_WORKSTATION, RecipeTechRegistry.DEMONIC_WORKSTATION, RecipeTechRegistry.WORKSTATION
      };
   }

   public static class FallenWorkstation2Object extends WorkstationDuoObject.WorkstationDuo2Object {
      protected FallenWorkstation2Object() {
         this.mapColor = new Color(0, 107, 109);
         this.lightLevel = 100;
         this.lightHue = 220.0F;
         this.lightSat = 0.2F;
      }

      @Override
      public void loadTextures() {
         this.texture = GameTexture.fromFile("objects/fallenworkstation");
      }
   }
}
