package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.gfx.GameColor;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.SpacerGameTooltip;
import necesse.inventory.InventoryItem;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class PotionSellerSettlerPersonality extends SettlerPersonality {
   public static int MAX_POTIONS_FOR_SALE = 10;

   public PotionSellerSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void init() {
      super.init();
      if (this.mob instanceof HumanShop) {
         HumanShop humanShop = (HumanShop)this.mob;

         for (int i = 0; i < MAX_POTIONS_FOR_SALE; i++) {
            int finalI = i;
            humanShop.shop
               .addSellingItem(this.getStringID() + "personalitypotions" + i, new SellingShopItem(25, 5))
               .setItem((random, client, mob, blackboard) -> {
                  TicketSystemList<String> potionLottery = blackboard.get(TicketSystemList.class, this.getStringID() + "personalitypotions");
                  if (potionLottery == null) {
                     potionLottery = this.getPotionSelectionStringIDs();
                     blackboard.set(this.getStringID() + "personalitypotions", potionLottery);
                  }

                  return potionLottery.isEmpty() ? null : new InventoryItem(potionLottery.getAndRemoveRandomObject(random));
               })
               .addRequirement((random, client, mob, blackboard) -> finalI < this.getCurrentPotionCount())
               .setStaticBrokerPriceBasedOnHappiness(3.0F, 6.0F, 2.0F);
         }
      }
   }

   public TicketSystemList<String> getPotionSelectionStringIDs() {
      TicketSystemList<String> list = new TicketSystemList<>();
      list.addObject(100, "manaregenpotion");
      list.addObject(100, "speedpotion");
      list.addObject(100, "healthregenpotion");
      list.addObject(100, "resistancepotion");
      list.addObject(100, "battlepotion");
      list.addObject(100, "attackspeedpotion");
      list.addObject(100, "accuracypotion");
      list.addObject(100, "rapidpotion");
      list.addObject(100, "knockbackpotion");
      list.addObject(100, "thornspotion");
      list.addObject(100, "fishingpotion");
      list.addObject(100, "miningpotion");
      list.addObject(100, "spelunkerpotion");
      list.addObject(100, "treasurepotion");
      list.addObject(100, "trackerpotion");
      list.addObject(100, "passivepotion");
      list.addObject(100, "buildingpotion");
      ServerSettlementData data = this.mob.getSettlementServerData();
      if (data != null && data.storyObjectives.hasCompletedStoryObjectiveOrHigher("defeatpiratecaptain")) {
         list.addObject(50, "strengthpotion");
         list.addObject(50, "rangerpotion");
         list.addObject(50, "wisdompotion");
         list.addObject(50, "minionpotion");
         list.addObject(50, "webpotion");
      }

      return list;
   }

   public int getCurrentPotionCount() {
      return Math.min(this.mob.getSettlerQualityOfLife(), MAX_POTIONS_FOR_SALE);
   }

   @Override
   public GameTooltips getTooltip() {
      ListGameTooltips tooltips = new ListGameTooltips();
      tooltips.add(super.getTooltip());
      tooltips.add(new SpacerGameTooltip(10));
      tooltips.add(new LocalMessage("personalities", "currentbonus"));
      int currentPotionCount = this.getCurrentPotionCount();
      GameColor color = currentPotionCount <= 0 ? GameColor.YELLOW : GameColor.GREEN;
      GameMessage currentMessage = new GameMessageBuilder()
         .append(color.getColorCode())
         .append(new LocalMessage("personalities", "potionsellercurrent", "number", currentPotionCount))
         .append(GameColor.NO_COLOR.getColorCode());
      tooltips.add(currentMessage);
      return tooltips;
   }
}
