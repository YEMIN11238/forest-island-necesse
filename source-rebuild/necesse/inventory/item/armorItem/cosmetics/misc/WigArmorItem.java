package necesse.inventory.item.armorItem.cosmetics.misc;

import java.awt.Color;
import java.util.List;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameHair;
import necesse.gfx.HumanLook;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.MergeFunction;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.armorItem.HelmetArmorItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class WigArmorItem extends HelmetArmorItem {
   public WigArmorItem(int armor) {
      super(armor, null, 0, null, null);
      this.facialFeatureDrawOptions = ArmorItem.FacialFeatureDrawMode.OVER_FACIAL_FEATURE;
   }

   @Override
   public void loadItemTextures() {
   }

   @Override
   public GameTexture getArmorTexture(
      InventoryItem item, Level level, PlayerMob player, InventoryItem headItem, InventoryItem chestItem, InventoryItem feetItem
   ) {
      return item != null
         ? GameHair.getHair(getHair(item.getGndData())).getHairTexture()
         : super.getArmorTexture(item, level, player, headItem, chestItem, feetItem);
   }

   @Override
   public GameTexture getBackArmorTexture(InventoryItem item, PlayerMob player) {
      return item != null ? GameHair.getHair(getHair(item.getGndData())).getBackHairTexture() : super.getBackArmorTexture(item, player);
   }

   @Override
   public GameSprite getItemSprite(InventoryItem item, PlayerMob perspective) {
      return item != null ? new GameSprite(GameHair.getHair(getHair(item.getGndData())).getWigTexture()) : super.getItemSprite(item, perspective);
   }

   @Override
   public boolean isSameGNDData(Level level, InventoryItem me, InventoryItem them, String purpose) {
      return super.isSameGNDData(level, me, them, purpose) && me.getGndData().sameKeys(them.getGndData(), "hair", "haircol");
   }

   @Override
   public void addDefaultItems(List<InventoryItem> list, PlayerMob player) {
      for (int i = 0; i < GameHair.getTotalHair(); i++) {
         HumanLook look = player == null ? new HumanLook() : new HumanLook(player.look);
         look.setHair(i);
         list.add(addWigData(new InventoryItem(this), look));
      }

      super.addDefaultItems(list, player);
   }

   @Override
   public InventoryItem getDefaultItem(PlayerMob player, int amount) {
      return addWigData(super.getDefaultItem(player, amount), player == null ? new HumanLook() : player.look);
   }

   public static int getHair(GNDItemMap gndData) {
      return gndData.getByte("hair") & 0xFF;
   }

   public static int getHairCol(GNDItemMap gndData) {
      return gndData.getByte("haircol") & 0xFF;
   }

   public static GNDItemMap getWigData(HumanLook look) {
      return addWigData((GNDItemMap)null, look);
   }

   public static GNDItemMap addWigData(GNDItemMap data, HumanLook look) {
      if (data == null) {
         data = new GNDItemMap();
      }

      data.setByte("hair", (byte)look.getHair());
      data.setByte("haircol", (byte)look.getHairColor());
      return data;
   }

   public static InventoryItem addWigData(InventoryItem item, HumanLook look) {
      addWigData(item.getGndData(), look);
      return item;
   }

   @Override
   public DrawOptions getArmorDrawOptions(
      InventoryItem item,
      Level level,
      PlayerMob player,
      InventoryItem headItem,
      InventoryItem chestItem,
      InventoryItem feetItem,
      int spriteX,
      int spriteY,
      int spriteRes,
      int drawX,
      int drawY,
      int width,
      int height,
      boolean mirrorX,
      boolean mirrorY,
      GameLight light,
      boolean hasGlowEffect,
      int glowHash,
      float alpha,
      MaskShaderOptions mask
   ) {
      GameTexture armorTexture = this.getArmorTexture(item, level, player, headItem, chestItem, feetItem);
      Color col = this.getDrawColor(item, player);
      return armorTexture != null
         ? armorTexture.initDraw()
            .sprite(spriteX, spriteY, spriteRes)
            .startGlowOptions(level, glowHash)
            .colorLight(col, light)
            .apply(hasGlowEffect)
            .alpha(alpha)
            .size(width, height)
            .mirror(mirrorX, mirrorY)
            .addMaskShader(mask)
            .recolor(GameHair.recolorLookupTexture, getHairCol(item.getGndData()) + 1)
            .pos(drawX, drawY)
         : () -> {};
   }

   @Override
   public DrawOptions getFrontArmorDrawOptions(
      InventoryItem item,
      Level level,
      PlayerMob player,
      int spriteX,
      int spriteY,
      int spriteRes,
      int drawX,
      int drawY,
      int width,
      int height,
      boolean mirrorX,
      boolean mirrorY,
      GameLight light,
      boolean hasGlowEffect,
      int glowHash,
      float alpha,
      MaskShaderOptions mask
   ) {
      GameTexture armorTexture = this.getFrontArmorTexture(item, player);
      if (armorTexture != null) {
         Color col = this.getDrawColor(item, player);
         return armorTexture.initDraw()
            .sprite(spriteX, spriteY, spriteRes)
            .startGlowOptions(level, glowHash)
            .colorLight(col, light)
            .apply(hasGlowEffect)
            .alpha(alpha)
            .size(width, height)
            .mirror(mirrorX, mirrorY)
            .addMaskShader(mask)
            .recolor(GameHair.recolorLookupTexture, getHairCol(item.getGndData()) + 1)
            .pos(drawX, drawY);
      } else {
         return null;
      }
   }

   @Override
   public DrawOptions getBackArmorDrawOptions(
      InventoryItem item,
      Level level,
      PlayerMob player,
      int spriteX,
      int spriteY,
      int spriteRes,
      int drawX,
      int drawY,
      int width,
      int height,
      boolean mirrorX,
      boolean mirrorY,
      GameLight light,
      boolean hasGlowEffect,
      int glowHash,
      float alpha,
      MaskShaderOptions mask
   ) {
      GameTexture armorTexture = this.getBackArmorTexture(item, player);
      if (armorTexture != null) {
         Color col = this.getDrawColor(item, player);
         return armorTexture.initDraw()
            .sprite(spriteX, spriteY, spriteRes)
            .startGlowOptions(level, glowHash)
            .colorLight(col, light)
            .apply(hasGlowEffect)
            .alpha(alpha)
            .size(width, height)
            .mirror(mirrorX, mirrorY)
            .addMaskShader(mask)
            .recolor(GameHair.recolorLookupTexture, getHairCol(item.getGndData()) + 1)
            .pos(drawX, drawY);
      } else {
         return null;
      }
   }

   @Override
   public void drawIcon(InventoryItem item, PlayerMob perspective, int x, int y, int size, Color color) {
      if (getHair(item.getGndData()) <= 0) {
         super.drawIcon(item, perspective, x, y, size, color);
      } else {
         if (color != null) {
            color = MergeFunction.MULTIPLY.merge(color, this.getDrawColor(item, perspective));
         } else {
            color = this.getDrawColor(item, perspective);
         }

         this.getItemSprite(item, perspective)
            .initDraw()
            .color(color)
            .recolor(GameHair.recolorLookupTexture, getHairCol(item.getGndData()) + 1)
            .size(size)
            .draw(x, y);
      }
   }
}
