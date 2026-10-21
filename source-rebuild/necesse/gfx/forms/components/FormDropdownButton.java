package necesse.gfx.forms.components;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.fairType.FairType;
import necesse.gfx.fairType.FairTypeDrawOptions;
import necesse.gfx.forms.FormClickHandler;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.forms.controller.ControllerNavigationHandler;
import necesse.gfx.forms.floatMenu.SelectionFloatMenu;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.position.FormPosition;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.shader.FormShader;
import necesse.gfx.ui.ButtonColor;
import necesse.gfx.ui.ButtonState;
import necesse.gfx.ui.HoverStateTextures;

public class FormDropdownButton extends FormComponent implements FormPositionContainer {
   public boolean removingSubmenuRemovesParent = true;
   protected FormPosition position;
   protected boolean isActive = true;
   protected int width;
   private GameMessage text;
   protected FormInputSize size;
   protected ButtonColor color;
   protected boolean isHovering;
   public int textAlign = 0;
   public boolean alignLeftIfNotFit = true;
   public final FormDropdownButton.OptionsList options = new FormDropdownButton.OptionsList(this);
   private SelectionFloatMenu currentMenu;
   protected FormClickHandler clickHandler = new FormClickHandler(
      e -> this.isActive()
         && this.isMouseOver(e)
         && (this.currentMenu == null || this.currentMenu.isDisposed() && !InputEvent.isFromSameEvent(e, this.currentMenu.removeEvent)),
      -100,
      e -> {
         this.playTickSound();
         this.currentMenu = this.options.getMenu(this.width - 4, this.removingSubmenuRemovesParent);
         Rectangle contentBox = this.size.getContentRectangle(this.width);
         this.getManager().openFloatMenu(this.currentMenu, this.getX() - e.pos.hudX, this.getY() - e.pos.hudY + contentBox.y + contentBox.height + 2);
      }
   );

   public FormDropdownButton(int x, int y, FormInputSize size, ButtonColor color, int width, GameMessage text) {
      this.position = new FormFixedPosition(x, y);
      this.size = size;
      this.width = width;
      this.color = color;
      this.setText(text);
   }

   public FormDropdownButton(int x, int y, FormInputSize size, ButtonColor color, int width) {
      this(x, y, size, color, width, new LocalMessage("ui", "selectbutton"));
   }

   @Override
   public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
      if (event.isMouseMoveEvent()) {
         this.isHovering = this.isMouseOver(event);
         if (this.isHovering) {
            event.useMove();
         }
      } else {
         this.clickHandler.handleEvent(event);
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

   public void setText(GameMessage text) {
      this.text = text;
   }

   private FairTypeDrawOptions getTextDrawOptions() {
      return new FairType()
         .append(this.size.getFontOptions().color(this.getTextColor()), this.getDisplayText())
         .getDrawOptions(FairType.TextAlign.LEFT, -1, false, true);
   }

   public String getDisplayText() {
      return this.text.translate();
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      Color drawCol = this.getDrawColor();
      ButtonState state = this.getButtonState();
      int textOffset = 0;
      boolean useDownTexture = this.clickHandler.isDown() && this.isHovering;
      if (useDownTexture) {
         this.size.getButtonDownDrawOptions(this.getInterfaceStyle(), this.color, state, this.getX(), this.getY(), this.width, drawCol).draw();
         textOffset = this.size.buttonDownContentDrawOffset;
      } else {
         this.size.getButtonDrawOptions(this.getInterfaceStyle(), this.color, state, this.getX(), this.getY(), this.width, drawCol).draw();
      }

      HoverStateTextures endIcons = this.size.height < 20 ? this.getInterfaceStyle().button_select_small : this.getInterfaceStyle().button_select_big;
      GameTexture endIcon = state == ButtonState.HIGHLIGHTED ? endIcons.highlighted : endIcons.active;
      Rectangle contentRect = this.size.getContentRectangle(this.width);
      FormShader.FormShaderState textState = GameResources.formShader
         .startState(
            new Point(this.getX(), this.getY()), new Rectangle(contentRect.x, contentRect.y, contentRect.width - endIcon.getWidth() - 2, contentRect.height)
         );

      try {
         FairTypeDrawOptions textDrawOptions = this.getTextDrawOptions();
         if (this.textAlign == -1) {
            textDrawOptions.draw(contentRect.x, textOffset + this.size.fontDrawOffset, Color.BLACK);
         } else if (this.textAlign == 1) {
            textDrawOptions.draw(contentRect.x + this.width - 5 - textDrawOptions.getBoundingBox().width, textOffset + this.size.fontDrawOffset, Color.BLACK);
         } else if (this.alignLeftIfNotFit && textDrawOptions.getBoundingBox().width > contentRect.width) {
            textDrawOptions.draw(contentRect.x, textOffset + this.size.fontDrawOffset, Color.BLACK);
         } else {
            textDrawOptions.draw(this.width / 2 - textDrawOptions.getBoundingBox().width / 2, textOffset + this.size.fontDrawOffset, Color.BLACK);
         }
      } finally {
         textState.end();
      }

      FormShader.FormShaderState var21 = GameResources.formShader
         .startState(new Point(this.getX(), this.getY()), new Rectangle(contentRect.x, contentRect.y, contentRect.width, contentRect.height));

      try {
         int endIconHeight = endIcon.getHeight();
         endIcon.initDraw()
            .color(drawCol)
            .draw(contentRect.x + contentRect.width - endIcon.getWidth() - 2, contentRect.y + contentRect.height / 2 - endIconHeight / 2 + textOffset);
      } finally {
         var21.end();
      }

      if (useDownTexture) {
         this.size.getButtonDownEdgeDrawOptions(this.getInterfaceStyle(), this.color, state, this.getX(), this.getY(), this.width, drawCol).draw();
      } else {
         this.size.getButtonEdgeDrawOptions(this.getInterfaceStyle(), this.color, state, this.getX(), this.getY(), this.width, drawCol).draw();
      }
   }

   @Override
   public void drawControllerFocus(ControllerFocus current) {
      super.drawControllerFocus(current);
      GameTooltipManager.addControllerGlyph(Localization.translate("ui", "selectbutton"), ControllerInput.MENU_SELECT);
   }

   protected Color getDrawColor() {
      return this.size.getButtonColor(this.getInterfaceStyle(), this.getButtonState());
   }

   protected ButtonState getButtonState() {
      if (!this.isActive()) {
         return ButtonState.INACTIVE;
      } else {
         return this.isHovering ? ButtonState.HIGHLIGHTED : ButtonState.ACTIVE;
      }
   }

   public Color getTextColor() {
      return this.getButtonState().textColorGetter.apply(this.getInterfaceStyle());
   }

   @Override
   public List<Rectangle> getHitboxes() {
      return singleBox(new Rectangle(this.getX(), this.getY() + this.size.textureDrawOffset, this.width, this.size.height));
   }

   @Override
   public FormPosition getPosition() {
      return this.position;
   }

   @Override
   public void setPosition(FormPosition position) {
      this.position = position;
   }

   public boolean isActive() {
      return this.isActive;
   }

   public void setActive(boolean isActive) {
      this.isActive = isActive;
   }

   public static class Option {
      public final GameMessage text;
      public final Supplier<Boolean> isActive;
      public final Color textColor;
      public final Supplier<GameTooltips> hoverTooltips;
      public final Runnable action;

      public Option(GameMessage text, Supplier<Boolean> isActive, Color textColor, Supplier<GameTooltips> hoverTooltips, Runnable action) {
         this.text = text;
         this.isActive = isActive;
         this.textColor = textColor;
         this.hoverTooltips = hoverTooltips;
         this.action = action;
      }

      public Option(GameMessage text, Runnable action) {
         this(text, null, null, null, action);
      }
   }

   private abstract static class OptionContainer {
      private OptionContainer() {
      }

      public abstract void addToMenu(SelectionFloatMenu var1, boolean var2);
   }

   public static class OptionsList {
      private final FormDropdownButton button;
      private final LinkedList<FormDropdownButton.OptionContainer> options = new LinkedList<>();

      private OptionsList(FormDropdownButton button) {
         this.button = button;
      }

      public FormDropdownButton.OptionsList add(FormDropdownButton.Option option) {
         this.options.add(new FormDropdownButton.ValueOptionContainer(this.button, option));
         return this;
      }

      public FormDropdownButton.OptionsList add(GameMessage text, Runnable action) {
         return this.add(text, null, action);
      }

      public FormDropdownButton.OptionsList add(GameMessage text, Supplier<GameMessage> tooltip, Runnable action) {
         return this.add(text, tooltip, null, action);
      }

      public FormDropdownButton.OptionsList add(GameMessage text, Supplier<GameMessage> tooltip, Supplier<Boolean> isActive, Runnable action) {
         return this.add(new FormDropdownButton.Option(text, isActive, null, tooltip == null ? null : () -> {
            GameMessage tooltipMsg = tooltip.get();
            return tooltipMsg != null ? new StringTooltips(tooltipMsg.translate()) : null;
         }, action));
      }

      public FormDropdownButton.OptionsList addSub(GameMessage text) {
         FormDropdownButton.OptionsList subList = new FormDropdownButton.OptionsList(this.button);
         this.options.add(new FormDropdownButton.SubMenuOptionContainer(this.button, subList, text));
         return subList;
      }

      public void clear() {
         this.options.clear();
      }

      public int size() {
         return this.options.size();
      }

      public boolean isEmpty() {
         return this.options.isEmpty();
      }

      private SelectionFloatMenu getMenu(int minWidth, boolean removingSubmenuRemovesParent) {
         SelectionFloatMenu menu = new SelectionFloatMenu(this.button, SelectionFloatMenu.Solid(new FontOptions(12)), minWidth);
         this.options.forEach(o -> o.addToMenu(menu, removingSubmenuRemovesParent));
         return menu;
      }
   }

   private static class SubMenuOptionContainer extends FormDropdownButton.OptionContainer {
      public final FormDropdownButton button;
      public final FormDropdownButton.OptionsList subOptions;
      public final GameMessage text;

      public SubMenuOptionContainer(FormDropdownButton button, FormDropdownButton.OptionsList subOptions, GameMessage text) {
         this.button = button;
         this.subOptions = subOptions;
         this.text = text;
      }

      @Override
      public void addToMenu(SelectionFloatMenu menu, boolean removingSubmenuRemovesParent) {
         menu.add(this.text.translate(), this.subOptions.getMenu(0, removingSubmenuRemovesParent), removingSubmenuRemovesParent);
      }
   }

   private static class ValueOptionContainer extends FormDropdownButton.OptionContainer {
      public final FormDropdownButton button;
      public final FormDropdownButton.Option option;

      public ValueOptionContainer(FormDropdownButton button, FormDropdownButton.Option option) {
         this.button = button;
         this.option = option;
      }

      @Override
      public void addToMenu(SelectionFloatMenu menu, boolean removingSubmenuRemovesParent) {
         menu.add(this.option.text.translate(), this.option.isActive, this.option.textColor, this.option.hoverTooltips, () -> {
            this.option.action.run();
            if (this.button.currentMenu != null && !this.button.currentMenu.isDisposed()) {
               this.button.currentMenu.remove();
            }
         });
      }
   }
}
