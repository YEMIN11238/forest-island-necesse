package necesse.gfx.forms.components.lists;

import java.awt.Color;
import java.awt.Rectangle;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.network.client.Client;
import necesse.engine.network.client.ClientClient;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.Renderer;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;

public class FormScoreboardList extends FormGeneralList<FormScoreboardList.ScoreElement> {
   private Client client;

   public FormScoreboardList(int x, int y, int width, int height, Client client) {
      super(x, y, width, height, 30);
      this.client = client;
      this.reset();
   }

   @Override
   public void reset() {
      super.reset();
      if (this.client != null) {
         for (int i = 0; i < this.client.getSlots(); i++) {
            ClientClient player = this.client.getClient(i);
            if (player != null) {
               this.elements.add(new FormScoreboardList.ScoreElement(player));
            }
         }
      }
   }

   public void slotChanged(int slot, ClientClient player) {
      this.elements.removeIf(ex -> ex.player.slot == slot);
      if (player != null) {
         for (int i = 0; i < this.elements.size(); i++) {
            FormScoreboardList.ScoreElement e = this.elements.get(i);
            if (e.player.slot > slot) {
               this.elements.add(i, new FormScoreboardList.ScoreElement(player));
               return;
            }
         }

         this.elements.add(new FormScoreboardList.ScoreElement(player));
      }
   }

   public static void drawLatencyBars(int latency, int barWidth, int height, int drawX, int drawY) {
      Color c;
      int bars;
      if (latency <= 50) {
         c = new Color(50, 200, 50);
         bars = 4;
      } else if (latency <= 125) {
         c = new Color(200, 200, 50);
         bars = 3;
      } else if (latency <= 250) {
         c = new Color(250, 150, 50);
         bars = 2;
      } else {
         c = new Color(250, 50, 50);
         bars = 1;
      }

      for (int i = 0; i < 4; i++) {
         if (i <= bars - 1) {
            int startHeight = height / 10;
            int barHeight = (height - startHeight) / 4 * (i + 1) + startHeight;
            Renderer.initQuadDraw(barWidth, barHeight).color(Color.BLACK).draw(drawX + (barWidth + 2) * i + 2, drawY + height - barHeight);
            Renderer.initQuadDraw(barWidth - 2, barHeight - 2).color(c).draw(drawX + (barWidth + 2) * i + 3, drawY + height - barHeight + 1);
         }
      }
   }

   public static int getLatencyBarsWidth(int barWidth) {
      return (barWidth + 2) * 4;
   }

   public static boolean isMouseOverLatencyBar(int x, int y, InputEvent event) {
      return event == null ? false : new Rectangle(x, y, 32, 16).contains(event.pos.hudX, event.pos.hudY);
   }

   public class ScoreElement extends FormListElement<FormScoreboardList> {
      private ClientClient player;

      public ScoreElement(ClientClient player) {
         this.player = player;
      }

      protected void draw(FormScoreboardList parent, TickManager tickManager, PlayerMob perspective, int elementIndex) {
         float grey = this.isHovering() ? 0.1F : 0.0F;
         Renderer.initQuadDraw(parent.width, parent.elementHeight - 4).color(grey, grey, grey, 0.5F).draw(0, 0);
         FontOptions options = new FontOptions(20);
         FontManager.bit.drawString(4.0F, 2.0F, GameUtils.maxString(this.player.getName(), options, parent.width - 40), options);
         FormScoreboardList.drawLatencyBars(this.player.latency, 5, 20, parent.width - 32, 4);
         if (this.isHovering() && FormScoreboardList.isMouseOverLatencyBar(parent.width - 32, 6, this.getMoveEvent())) {
            GameTooltipManager.addTooltip(
               new StringTooltips(Localization.translate("ui", "latencytip", "latency", this.player.latency)), TooltipLocation.FORM_FOCUS
            );
         }
      }

      protected void onClick(FormScoreboardList parent, int elementIndex, InputEvent event, PlayerMob perspective) {
         if (event.getID() == -100) {
            event.use();
            FormScoreboardList.this.playTickSound();
         }
      }

      protected void onControllerEvent(FormScoreboardList parent, int elementIndex, ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
         if (event.getState() == ControllerInput.MENU_SELECT) {
            event.use();
            FormScoreboardList.this.playTickSound();
         }
      }
   }
}
