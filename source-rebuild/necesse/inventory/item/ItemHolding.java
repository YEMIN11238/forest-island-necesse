package necesse.inventory.item;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.gameNetworkData.GNDItem;
import necesse.engine.network.gameNetworkData.GNDItemInventoryItem;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class ItemHolding extends Item {
   public ItemHolding() {
      super(1);
   }

   public static InventoryItem setGNDData(InventoryItem item, InventoryItem holdItem) {
      item.getGndData().setItem("holdItem", new GNDItemInventoryItem(holdItem));
      return item;
   }

   public InventoryItem getHoldItem(InventoryItem item) {
      GNDItem gndItem = item.getGndData().getItem("holdItem");
      return gndItem instanceof GNDItemInventoryItem ? ((GNDItemInventoryItem)gndItem).invItem : null;
   }

   @Override
   public boolean holdsItem(InventoryItem item, PlayerMob player) {
      return true;
   }

   @Override
   public DrawOptions getHoldItemDrawOptions(
      InventoryItem item,
      Level level,
      PlayerMob player,
      int spriteX,
      int spriteY,
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
      InventoryItem holdItem = this.getHoldItem(item);
      if (holdItem == null) {
         holdItem = item;
      }

      GameSprite sprite = holdItem.item.getItemSprite(item, player);
      int size = 20;
      float widthPercent = width / 64.0F;
      float heightPercent = height / 64.0F;
      TextureDrawOptionsEnd options = sprite.initDraw();
      options = options.size(size);
      options = options.size((int)(options.getWidth() * widthPercent), (int)(options.getHeight() * heightPercent));
      int xOffset = 0;
      int yOffset = 0;
      switch (spriteY) {
         case 0:
            xOffset += 24;
            yOffset += 32;
            break;
         case 1:
            xOffset += 28;
            yOffset += 36;
            break;
         case 2:
            xOffset += 22;
            yOffset += 36;
            break;
         case 3:
            xOffset += 16;
            yOffset += 36;
      }

      if (HumanDrawOptions.isSpriteXOffset(spriteX)) {
         yOffset -= 2;
      }

      if (mask != null) {
         mask.addMaskOffset(-xOffset, -yOffset);
         xOffset += mask.drawXOffset;
         yOffset += mask.drawYOffset;
      }

      return options.startGlowOptions(level, glowHash)
         .light(light)
         .apply(hasGlowEffect)
         .alpha(alpha)
         .mirror(mirrorX, mirrorY)
         .pos(drawX + xOffset, drawY + yOffset);
   }

   @Override
   public GameMessage getNewLocalization() {
      return new StaticMessage(this.getStringID());
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      tooltips.add("NOT OBTAINABLE");
      return tooltips;
   }

   @Override
   public InventoryItem getDefaultItem(PlayerMob player, int amount) {
      return setGNDData(super.getDefaultItem(player, amount), new InventoryItem("apple"));
   }
}
