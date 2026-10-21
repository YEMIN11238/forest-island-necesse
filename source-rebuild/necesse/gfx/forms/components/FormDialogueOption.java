package necesse.gfx.forms.components;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import java.util.function.Supplier;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.fairType.FairType;
import necesse.gfx.fairType.FairTypeDrawOptions;
import necesse.gfx.fairType.FairTypeDrawOptionsContainer;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.fairType.parsers.TypeParser;
import necesse.gfx.forms.FormClickHandler;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.forms.controller.ControllerNavigationHandler;
import necesse.gfx.forms.events.FormEventListener;
import necesse.gfx.forms.events.FormEventsHandler;
import necesse.gfx.forms.events.FormInputEvent;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.position.FormPosition;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameFont.GameFontHandler;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;

public class FormDialogueOption extends FormComponent implements FormPositionContainer {
   private static final int[] toolbarHotkeys = new int[]{49, 50, 51, 52, 53, 54, 55, 56, 57, 48};
   private final GameFontHandler font = FontManager.bit;
   private final int optionNumber;
   private final boolean isInsideChatBubble;
   public final FontOptions fontOptions;
   private FormPosition position;
   private GameMessage text;
   private final int numberWidth;
   private int maxWidth;
   public final FairTypeDrawOptionsContainer drawOptions;
   protected TypeParser[] parsers = new TypeParser[]{TypeParsers.GAME_COLOR, TypeParsers.URL_OPEN, TypeParsers.MARKDOWN_URL};
   private boolean isHovering;
   private final FormClickHandler clickHandler;
   private boolean active = true;
   public boolean mouseOverCoversEntireWidth = true;
   protected FormEventsHandler<FormInputEvent<FormDialogueOption>> clickedEvents;
   public Supplier<GameTooltips> tooltipsSupplier;

   public FormDialogueOption(int optionNumber, boolean isInsideChatBubble, GameMessage text, FontOptions fontOptions, int x, int y, int maxWidth) {
      this.optionNumber = optionNumber;
      this.isInsideChatBubble = isInsideChatBubble;
      this.fontOptions = fontOptions;
      this.position = new FormFixedPosition(x, y);
      this.numberWidth = optionNumber > 0 ? this.font.getWidthCeil(optionNumber + ". ", fontOptions) : 0;
      this.clickedEvents = new FormEventsHandler<>();
      this.clickHandler = new FormClickHandler(e -> this.acceptsEvents() && this.isMouseOver(e), -100, e -> {
         FormInputEvent<FormDialogueOption> fEvent = new FormInputEvent<>(this, e);
         this.clickedEvents.onEvent(fEvent);
         if (!fEvent.hasPreventedDefault()) {
            this.pressed(e);
            e.use();
         }
      });
      this.drawOptions = new FairTypeDrawOptionsContainer(() -> {
         FairType type = new FairType().append(this.fontOptions, this.getText().translate());
         TypeParser[] parsers = this.getParsers();
         if (parsers != null) {
            type.applyParsers(parsers);
         }

         return type.getDrawOptions(FairType.TextAlign.LEFT, maxWidth - this.numberWidth, true, 0, -1, false, null, true);
      });
      this.setText(text, maxWidth);
   }

   public FormDialogueOption onClicked(FormEventListener<FormInputEvent<FormDialogueOption>> listener) {
      this.clickedEvents.addListener(listener);
      return this;
   }

   protected void pressed(InputEvent event) {
      this.playTickSound();
   }

   @Override
   public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
      if (event.isMouseMoveEvent()) {
         this.isHovering = this.isMouseOver(event);
         if (this.isHovering) {
            event.useMove();
         }
      }

      this.clickHandler.handleEvent(event);
      if (this.isActive()
         && event.isKeyboardEvent()
         && event.state
         && this.optionNumber >= 1
         && this.optionNumber <= 10
         && event.getID() == toolbarHotkeys[this.optionNumber - 1]) {
         this.clickHandler.forceClick(event);
      }
   }

   @Override
   public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
      if (this.isActive() && event.getState() == ControllerInput.MENU_SELECT) {
         if (this.isControllerFocus() && event.buttonState) {
            InputEvent inputEvent = InputEvent.ControllerButtonEvent(event, tickManager);
            this.clickHandler.forceHandleEvent(inputEvent);
            event.use();
         } else if (!event.buttonState && this.clickHandler.isDown()) {
            this.clickHandler.forceHandleEvent(InputEvent.ControllerButtonEvent(event, tickManager));
            event.use();
         }
      }
   }

   @Override
   public void addNextControllerFocus(
      List<ControllerFocus> list, int currentXOffset, int currentYOffset, ControllerNavigationHandler customNavigationHandler, Rectangle area, boolean draw
   ) {
      ControllerFocus.add(list, area, this, this.getBoundingBox(), currentXOffset, currentYOffset, this.controllerInitialFocusPriority, customNavigationHandler);
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      if (this.text.hasUpdated()) {
         this.drawOptions.reset();
      }

      int x = this.getX();
      int y = this.getY();
      Color drawColor = this.getDrawColor();
      FontOptions fontOptions = new FontOptions(this.fontOptions).color(drawColor);
      FairTypeDrawOptions fairTypeDrawOptions = this.drawOptions.get();
      int lineHeight = fairTypeDrawOptions.getLineHeightCeil(0);
      if (this.optionNumber > 0) {
         this.font.drawString(x, y + lineHeight - fontOptions.getSize(), this.optionNumber + ". ", fontOptions);
      }

      fairTypeDrawOptions.draw(x + this.numberWidth, y, drawColor);
      if (this.isHovering() && this.tooltipsSupplier != null) {
         GameTooltips tooltips = this.tooltipsSupplier.get();
         if (tooltips != null) {
            GameTooltipManager.addTooltip(tooltips, TooltipLocation.FORM_FOCUS);
         }
      }
   }

   @Override
   public void drawControllerFocus(ControllerFocus current) {
      super.drawControllerFocus(current);
      GameTooltipManager.addControllerGlyph(Localization.translate("ui", "selectbutton"), ControllerInput.MENU_SELECT);
   }

   @Override
   public List<Rectangle> getHitboxes() {
      Rectangle boundingBox = this.drawOptions.get().getBoundingBox();
      int height = Math.max(boundingBox.height, this.fontOptions.getSize());
      return singleBox(new Rectangle(this.getX(), this.getY(), this.numberWidth + boundingBox.width, height));
   }

   @Override
   public boolean isMouseOver(InputEvent event) {
      if (event.isMoveUsed()) {
         return false;
      }

      int x = this.getX();
      int y = this.getY();
      if (this.mouseOverCoversEntireWidth) {
         int height = Math.max(this.drawOptions.get().getBoundingBox().height, this.fontOptions.getSize());
         return new Rectangle(x, y, this.maxWidth, height).contains(event.pos.hudX, event.pos.hudY);
      }

      if (new Rectangle(x, y, this.numberWidth, this.fontOptions.getSize()).contains(event.pos.hudX, event.pos.hudY)) {
         return true;
      }

      Rectangle boundingBox = this.drawOptions.get().getBoundingBox(x + this.numberWidth, y);
      return boundingBox.contains(event.pos.hudX, event.pos.hudY);
   }

   public void setMaxWidth(int maxWidth) {
      this.maxWidth = maxWidth;
      this.drawOptions.reset();
   }

   public void setText(GameMessage text) {
      this.text = text;
      this.drawOptions.reset();
   }

   public void setText(GameMessage text, int maxWidth) {
      this.text = text;
      this.maxWidth = maxWidth;
      this.drawOptions.reset();
   }

   public GameMessage getText() {
      return this.text;
   }

   public TypeParser[] getParsers() {
      return this.parsers;
   }

   public FormDialogueOption setParsers(TypeParser... parsers) {
      this.parsers = parsers;
      this.drawOptions.reset();
      return this;
   }

   protected boolean acceptsEvents() {
      return this.isActive();
   }

   public boolean isActive() {
      return this.active;
   }

   public void setActive(boolean active) {
      this.active = active;
   }

   @Override
   public FormPosition getPosition() {
      return this.position;
   }

   @Override
   public void setPosition(FormPosition position) {
      this.position = position;
   }

   protected boolean isHovering() {
      return this.isHovering;
   }

   public Color getDrawColor() {
      if (this.isInsideChatBubble) {
         if (!this.isActive()) {
            return this.getInterfaceStyle().chatBubbleInactiveTextColor;
         } else {
            return this.isHovering() ? this.getInterfaceStyle().chatBubbleHighlightTextColor : this.getInterfaceStyle().chatBubbleActiveTextColor;
         }
      } else if (!this.isActive()) {
         return this.getInterfaceStyle().inactiveTextColor;
      } else {
         return this.isHovering() ? this.getInterfaceStyle().highlightTextColor : this.getInterfaceStyle().activeTextColor;
      }
   }
}
