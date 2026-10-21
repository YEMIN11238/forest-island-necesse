package necesse.inventory.item.armorItem.runestone;

import java.awt.Color;
import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.ChestArmorItem;
import necesse.inventory.lootTable.presets.BodyArmorLootTable;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class RunicChestplateArmorItem extends ChestArmorItem {
   public GameTexture lightTexture;

   public RunicChestplateArmorItem() {
      super(11, 750, Item.Rarity.UNCOMMON, "runicchestplate", "runicarms", BodyArmorLootTable.bodyArmor);
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.05F), new ModifierValue<>(BuffModifiers.SPEED, 0.05F));
   }

   @Override
   protected void loadArmorTexture() {
      super.loadArmorTexture();
      this.lightTexture = GameTexture.fromFile("player/armor/" + this.textureName + "_light");
   }

   @Override
   public GameSprite getAttackArmSprite(
      InventoryItem item, Level level, PlayerMob player, InventoryItem headItem, InventoryItem chestItem, InventoryItem feetItem
   ) {
      if (player != null && player.getDir() == 3) {
         GameTexture armorTexture = this.getArmorTexture(item, level, player, headItem, chestItem, feetItem);
         return armorTexture == null ? null : new GameSprite(armorTexture, 0, 9, 32);
      } else {
         return super.getAttackArmSprite(item, level, player, headItem, chestItem, feetItem);
      }
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
      DrawOptionsList options = new DrawOptionsList();
      options.add(
         super.getArmorDrawOptions(
            item,
            level,
            player,
            headItem,
            chestItem,
            feetItem,
            spriteX,
            spriteY,
            spriteRes,
            drawX,
            drawY,
            width,
            height,
            mirrorX,
            mirrorY,
            light,
            hasGlowEffect,
            glowHash,
            alpha,
            mask
         )
      );
      Color col = this.getDrawColor(item, player);
      options.add(
         this.lightTexture
            .initDraw()
            .sprite(spriteX, spriteY, spriteRes)
            .startGlowOptions(level, glowHash)
            .colorLight(col, light.minLevelCopy(150.0F))
            .apply(hasGlowEffect)
            .alpha(alpha)
            .size(width, height)
            .mirror(mirrorX, mirrorY)
            .addMaskShader(mask)
            .pos(drawX, drawY)
      );
      return options;
   }
}
