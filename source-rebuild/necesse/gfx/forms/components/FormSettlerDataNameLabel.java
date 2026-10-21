package necesse.gfx.forms.components;

import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.network.client.Client;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.fairType.FairType;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.forms.controller.ControllerNavigationHandler;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.position.FormPosition;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameFont.GameFontHandler;
import necesse.inventory.container.settlement.data.SettlementSettlerData;

public class FormSettlerDataNameLabel extends FormComponent implements FormPositionContainer {
   private FormPosition position;
   private final Client client;
   private final SettlementSettlerData data;
   private final int maxWidth;
   private final FairType.TextAlign align;
   private final FontOptions fontOptions;
   private String lastName;
   private String limitedName;

   public FormSettlerDataNameLabel(Client client, SettlementSettlerData data, FontOptions fontOptions, FairType.TextAlign align, int x, int y, int maxWidth) {
      this.client = client;
      this.data = data;
      this.fontOptions = fontOptions;
      this.align = align;
      this.position = new FormFixedPosition(x, y);
      this.maxWidth = maxWidth;
   }

   @Override
   public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
   }

   @Override
   public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
   }

   @Override
   public void addNextControllerFocus(
      List<ControllerFocus> list, int currentXOffset, int currentYOffset, ControllerNavigationHandler customNavigationHandler, Rectangle area, boolean draw
   ) {
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      String nextName = this.data.getSettlerName(this.client.getLevel(), "N/A");
      this.fontOptions.defaultColor(this.getInterfaceStyle().activeTextColor);
      GameFontHandler font = FontManager.bit;
      if (this.lastName == null || !this.lastName.equals(nextName)) {
         this.lastName = nextName;
         this.limitedName = this.maxWidth > 0 ? GameUtils.maxString(this.lastName, this.fontOptions, this.maxWidth) : this.lastName;
      }

      int drawX = this.getX();
      if (this.align == FairType.TextAlign.CENTER) {
         drawX = this.getX() - font.getWidthCeil(this.limitedName, this.fontOptions) / 2;
      } else if (this.align == FairType.TextAlign.RIGHT) {
         drawX = this.getX() - font.getWidthCeil(this.limitedName, this.fontOptions);
      }

      font.drawString(drawX, this.getY(), this.limitedName, this.fontOptions);
   }

   @Override
   public List<Rectangle> getHitboxes() {
      int startX = this.getX();
      if (this.align == FairType.TextAlign.CENTER) {
         startX = this.getX() - this.maxWidth / 2;
      } else if (this.align == FairType.TextAlign.RIGHT) {
         startX = this.getX() - this.maxWidth;
      }

      return singleBox(new Rectangle(startX, this.getY(), this.maxWidth, this.fontOptions.getSize()));
   }

   @Override
   public FormPosition getPosition() {
      return this.position;
   }

   @Override
   public void setPosition(FormPosition position) {
      this.position = position;
   }
}
