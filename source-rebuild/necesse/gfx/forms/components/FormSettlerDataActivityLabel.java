package necesse.gfx.forms.components;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Objects;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.inventory.container.settlement.data.SettlementSettlerData;
import necesse.inventory.container.settlement.data.SettlerActivityData;

public class FormSettlerDataActivityLabel extends FormFairTypeLabel {
   private final Client client;
   private final SettlementSettlerData data;
   private GameMessage lastActivity = null;
   private Color lastColor = null;

   public FormSettlerDataActivityLabel(Client client, SettlementSettlerData data, FontOptions fontOptions, int x, int y, int maxWidth) {
      super(new LocalMessage("activities", "unknownactivity"), x, y);
      this.client = client;
      this.data = data;
      this.setMax(maxWidth, 1, true);
      this.setFontOptions(fontOptions);
      this.setParsers(
         TypeParsers.GAME_COLOR, TypeParsers.ItemIcon(fontOptions.getSize()), TypeParsers.MobIcon(fontOptions.getSize()), TypeParsers.InputIcon(fontOptions)
      );
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      SettlerActivityData activityData = this.data.getSettlerActivityData(this.client.getLevel());
      GameMessage currentActivity;
      Color color;
      if (activityData != null && activityData.currentActivity != null) {
         currentActivity = activityData.currentActivity;
         if (activityData.hasCommands) {
            color = this.getInterfaceStyle().errorTextColor;
         } else {
            color = this.getInterfaceStyle().activeTextColor;
         }
      } else {
         currentActivity = new LocalMessage("activities", "unknownactivity");
         color = this.getInterfaceStyle().warningTextColor;
      }

      if (!GameMessage.isSame(this.lastActivity, currentActivity) || !Objects.equals(this.lastColor, color)) {
         this.lastActivity = currentActivity;
         this.lastColor = color;
         String next = currentActivity.translate();
         this.setColor(color);
         this.setText(next.isEmpty() ? " " : next);
      }

      super.draw(tickManager, perspective, renderBox);
      if (this.isHovering()) {
         StringTooltips tooltips = new StringTooltips();
         if (!this.displaysFullText()) {
            tooltips.add(currentActivity.translate(), 300);
         }

         if (activityData == null) {
            tooltips.add(Localization.translate("settlement", "movecloser"));
         }

         if (tooltips.getSize() != 0) {
            GameTooltipManager.addTooltip(tooltips, TooltipLocation.FORM_FOCUS);
         }
      }
   }
}
