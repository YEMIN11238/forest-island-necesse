package necesse.level.gameObject;

import java.awt.Color;
import necesse.inventory.item.toolItem.ToolType;

public class PotTableDecorationObject extends TableDecorationObject {
   public PotTableDecorationObject(
      String textureName,
      ToolType toolType,
      Color mapColor,
      int decorationWidth,
      int decorationHeight,
      int holderDrawXOffset,
      int holderDrawYOffset,
      String customLocalizationKey,
      boolean isFlower
   ) {
      super(textureName, toolType, mapColor, decorationWidth, decorationHeight, holderDrawXOffset, holderDrawYOffset);
      if (customLocalizationKey != null) {
         this.setCustomLocalization(customLocalizationKey);
      }

      if (isFlower) {
         this.roomProperties.add("flower");
         this.roomProperties.add("plant");
      }

      this.setCustomLocalization("pottedflower");
      this.setItemCategory("objects", "decorations", "pots");
      this.setCraftingCategory("objects", "decorations", "pots");
   }

   public PotTableDecorationObject(
      String textureName,
      Color mapColor,
      int decorationWidth,
      int decorationHeight,
      int holderDrawXOffset,
      int holderDrawYOffset,
      String customLocalizationKey,
      boolean isFlower
   ) {
      super(textureName, mapColor, decorationWidth, decorationHeight, holderDrawXOffset, holderDrawYOffset);
      if (customLocalizationKey != null) {
         this.setCustomLocalization(customLocalizationKey);
      }

      if (isFlower) {
         this.roomProperties.add("flower");
         this.roomProperties.add("plant");
      }

      this.setItemCategory("objects", "decorations", "pots");
      this.setCraftingCategory("objects", "decorations", "pots");
   }

   public PotTableDecorationObject(
      String textureName, Color mapColor, int decorationWidth, int decorationHeight, String customLocalizationKey, boolean isFlower
   ) {
      super(textureName, mapColor, decorationWidth, decorationHeight);
      if (customLocalizationKey != null) {
         this.setCustomLocalization(customLocalizationKey);
      }

      if (isFlower) {
         this.roomProperties.add("flower");
         this.roomProperties.add("plant");
      }

      this.setItemCategory("objects", "decorations", "pots");
      this.setCraftingCategory("objects", "decorations", "pots");
   }
}
