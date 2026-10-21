package necesse.level.maps.levelData.settlementData.notifications;

import necesse.engine.localization.Localization;
import necesse.engine.network.client.Client;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.GameBackground;
import necesse.gfx.GameColor;
import necesse.gfx.forms.presets.HelpForms;
import necesse.gfx.gameTooltips.BackgroundedGameTooltips;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.SpacerGameTooltip;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageFoodQualityIndex;

public class LowFoodSettlementNotification extends SettlementNotification {
   @Override
   public GameTooltips getTooltip(SettlementNotificationManager.ActiveNotification notification) {
      ListGameTooltips tooltips = new ListGameTooltips();
      tooltips.add(Localization.translate("ui", "notificationlowfood"), 400);
      tooltips.add(
         new StringTooltips(Localization.translate("ui", "notificationnutrition", "value", notification.gndData.getInt("nutrition")), GameColor.GRAY, 400)
      );
      tooltips.add(new SpacerGameTooltip(10));
      tooltips.add(new StringTooltips(Localization.translate("ui", "notificationclickhelp"), GameColor.GRAY, 400));
      return new BackgroundedGameTooltips(tooltips, GameBackground.getItemTooltipBackground());
   }

   @Override
   public void onClicked(Client client, SettlementNotificationManager.ActiveNotification data) {
      HelpForms.openHelpForm("settlementlowfood");
   }

   @Override
   public boolean isStillValid(SettlerMob settlerMob) {
      return false;
   }

   @Override
   public boolean isStillValid(ServerSettlementData data) {
      return getTotalNutrition(data) < getExpectedFood(data);
   }

   public static int getExpectedFood(ServerSettlementData data) {
      double nutritionUsagePerDay = (double)data.getWorldEntity().getDayTimeMax() / HumanMob.secondsToPassAtFullHunger * 100.0;
      return (int)((data.countTotalSettlers() - 1) * nutritionUsagePerDay);
   }

   public static void tickShow(ServerSettlementData data) {
      int totalNutrition = getTotalNutrition(data);
      if (totalNutrition < getExpectedFood(data)) {
         SettlementNotificationManager manager = data.networkData.notifications;
         SettlementNotificationManager.ActiveNotification n = manager.submitNotification("lowfood", data, SettlementNotificationSeverity.NOTE);
         n.getGndData().setInt("nutrition", totalNutrition);
      }
   }

   public static int getTotalNutrition(ServerSettlementData data) {
      return data.storageRecords.getIndex(SettlementStorageFoodQualityIndex.class).getTotalNutrition();
   }

   public static int getTotalItems(ServerSettlementData data) {
      return data.storageRecords.getIndex(SettlementStorageFoodQualityIndex.class).getTotalItems();
   }
}
