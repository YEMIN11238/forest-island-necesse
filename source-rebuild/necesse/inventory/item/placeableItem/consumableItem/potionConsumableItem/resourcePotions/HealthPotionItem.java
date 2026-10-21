package necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.resourcePotions;

import java.awt.geom.Line2D;
import necesse.engine.input.Control;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.ComparableSequence;
import necesse.engine.util.GameBlackboard;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MobHealthChangeEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.ItemUsed;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.PotionConsumableItem;
import necesse.level.maps.Level;

public class HealthPotionItem extends PotionConsumableItem {
   public int restoreHealthFlat;
   public float restoreHealthPercent;

   public HealthPotionItem(Item.Rarity rarity, int restoreHealthFlat) {
      super(250, "healthpotionfatigue", 30, 1);
      this.rarity = rarity;
      this.restoreHealthFlat = restoreHealthFlat;
      this.obeysBuffPotionPolicy = false;
      this.setItemCategory(ItemCategory.craftingManager, "consumable", "resourcepotions");
   }

   public HealthPotionItem(Item.Rarity rarity, int restoreHealthFlat, float restoreHealthPercent) {
      super(250, "healthpotionfatigue", 30, 1);
      this.rarity = rarity;
      this.restoreHealthFlat = restoreHealthFlat;
      this.restoreHealthPercent = restoreHealthPercent;
      this.obeysBuffPotionPolicy = false;
      this.setItemCategory(ItemCategory.craftingManager, "consumable", "resourcepotions");
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      int healthRestored = this.getHealthRestored(perspective);
      if (this.restoreHealthPercent > 0.0F) {
         String healthString = this.restoreHealthFlat + " + " + (int)(this.restoreHealthPercent * 100.0F) + "%";
         tooltips.add(Localization.translate("itemtooltip", "healthpotflatpercent", "health", healthString));
         tooltips.add(Localization.translate("itemtooltip", "healthpotcurrent", "health", healthRestored));
      } else {
         tooltips.add(Localization.translate("itemtooltip", "healthpot", "health", healthRestored));
      }

      tooltips.add(Localization.translate("itemtooltip", "healthpottip", "key", TypeParsers.getInputParseString(Control.HEALTH_POT)));
      return tooltips;
   }

   public int getHealthRestored(Mob mob) {
      int maxHealth = mob == null ? 0 : mob.getMaxHealthFlat() + mob.getMaxHealthUpgrade();
      return this.restoreHealthFlat + (int)(maxHealth * this.restoreHealthPercent);
   }

   @Override
   public InventoryItem onPlace(Level level, int x, int y, PlayerMob player, int seed, InventoryItem item, GNDItemMap mapContent) {
      InventoryItem out = super.onPlace(level, x, y, player, seed, item, mapContent);
      if (level.isServer()) {
         int healthToRestore = this.getHealthRestored(player);
         if (player.buffManager.hasBuff("clockworkhearttrinket")) {
            int missingHealth = player.getMaxHealth() - player.getHealth();
            int restoredHealth = Math.min(healthToRestore, missingHealth);
            float resGain = healthToRestore - restoredHealth;
            if (resGain > 0.0F) {
               float finalResilience = player.getResilience() + resGain;
               player.setResilienceHidden(finalResilience);
               player.sendResiliencePacket(false);
            }
         }

         LevelEvent event = new MobHealthChangeEvent(player, healthToRestore);
         level.entityManager.events.add(event);
      }

      return out;
   }

   @Override
   public String canPlace(Level level, int x, int y, PlayerMob player, Line2D playerPositionLine, InventoryItem item, GNDItemMap mapContent) {
      if (player.buffManager.hasBuff("clockworkhearttrinket") && player.getResilience() != player.getMaxResilience()) {
         return super.canPlace(level, x, y, player, playerPositionLine, item, mapContent);
      } else {
         return player.getHealth() >= player.getMaxHealth() ? "fullhealth" : super.canPlace(level, x, y, player, playerPositionLine, item, mapContent);
      }
   }

   @Override
   public boolean canAndShouldPartyConsume(Level level, HumanMob mob, ServerClient partyClient, InventoryItem item, String purpose) {
      return !super.canAndShouldPartyConsume(level, mob, partyClient, item, purpose)
         ? false
         : mob.getHealth() <= mob.getMaxHealth() - this.getHealthRestored(mob) && mob.getHealthPercent() <= 0.75F || mob.getHealthPercent() <= 0.5F;
   }

   @Override
   public InventoryItem onPartyConsume(Level level, HumanMob mob, ServerClient partyClient, InventoryItem item, String purpose) {
      InventoryItem out = super.onPartyConsume(level, mob, partyClient, item, purpose);
      if (level.isServer()) {
         LevelEvent event = new MobHealthChangeEvent(mob, this.getHealthRestored(mob));
         level.entityManager.events.add(event);
      }

      return out;
   }

   @Override
   public ComparableSequence<Integer> getPartyPriority(
      Level level, HumanMob mob, ServerClient partyClient, Inventory inventory, int inventorySlot, InventoryItem item, String purpose
   ) {
      return super.getPartyPriority(level, mob, partyClient, inventory, inventorySlot, item, purpose).beforeBy(-this.getHealthRestored(mob));
   }

   @Override
   public ComparableSequence<Integer> getInventoryPriority(
      Level level, PlayerMob player, Inventory inventory, int inventorySlot, InventoryItem item, String purpose
   ) {
      return purpose.equals("usehealthpotion")
         ? super.getInventoryPriority(level, player, inventory, inventorySlot, item, purpose).beforeBy(-this.getHealthRestored(player))
         : super.getInventoryPriority(level, player, inventory, inventorySlot, item, purpose);
   }

   @Override
   public ItemUsed useHealthPotion(Level level, PlayerMob player, int seed, InventoryItem item) {
      String error = this.canPlace(level, 0, 0, player, null, item, null);
      return error == null
         ? new ItemUsed(true, this.onPlace(level, 0, 0, player, seed, item, null))
         : new ItemUsed(false, this.onAttemptPlace(level, 0, 0, player, item, null, error));
   }

   @Override
   public ItemUsed useBuffPotion(Level level, PlayerMob player, int seed, InventoryItem item) {
      return new ItemUsed(false, item);
   }
}
