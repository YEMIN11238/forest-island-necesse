package necesse.gfx.forms.components;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.function.Supplier;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Input;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.Renderer;
import necesse.gfx.fairType.FairType;
import necesse.gfx.forms.events.FormEventListener;
import necesse.gfx.forms.events.FormEventsHandler;
import necesse.gfx.forms.events.FormInputEvent;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.position.FormPosition;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.InputTooltip;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.gfx.shader.FormShader;

public class FormTextInput extends FormFairTypeEdit implements FormPositionContainer {
   protected FormPosition position;
   public final FormInputSize size;
   public final int width;
   private boolean active = true;
   private int scrollX;
   private final FormEventsHandler<FormInputEvent<FormTextInput>> submitEvents;
   public GameMessage placeHolder;
   public GameMessage tooltip;
   public Supplier<FormTypingComponent> tabTypingComponent;
   public boolean rightClickToClear;
   public GameMessage rightClickToClearTooltip;

   public FormTextInput(int x, int y, FormInputSize size, int width, int maxWidth, int maxLength) {
      super(size.getFontOptions(), FairType.TextAlign.CENTER, Color.WHITE, maxWidth, 1, maxLength);
      this.size = size;
      this.submitEvents = new FormEventsHandler<>();
      this.position = new FormFixedPosition(x, y);
      this.width = width;
      this.onCaretMove(e -> {
         if (!e.causedByMouse) {
            Rectangle caretBox = this.getCaretBoundingBox();
            caretBox.x = caretBox.x - this.getTextX();
            if (this.getDrawOptions().getBoundingBox().width < this.width) {
               this.scrollX = 0;
            } else {
               int minX = caretBox.x;
               int maxX = caretBox.x + caretBox.width;
               int limit = (this.width - 16) / 2;
               if (maxX > limit + this.scrollX) {
                  this.scrollX = Math.max(this.scrollX, maxX - limit);
               }

               if (minX < -limit + this.scrollX) {
                  this.scrollX = Math.min(this.scrollX, minX + limit);
               }
            }
         }
      });
      this.onMouseChangedTyping(e -> {
         if (!this.isTyping()) {
            this.submitEvents.onEvent(new FormInputEvent<>(this, e.event));
         }
      });
   }

   public FormTextInput(int x, int y, FormInputSize size, int width, int maxLength) {
      this(x, y, size, width, -1, maxLength);
   }

   public FormTextInput onSubmit(FormEventListener<FormInputEvent<FormTextInput>> listener) {
      this.submitEvents.addListener(listener);
      return this;
   }

   public void setActive(boolean active) {
      this.active = active;
      if (!this.isActive()) {
         this.clearSelection();
         if (this.isTyping()) {
            this.setTyping(false);
         }
      }
   }

   public boolean isActive() {
      return this.active;
   }

   @Override
   public boolean submitControllerEnter(InputEvent event) {
      FormInputEvent<FormTextInput> ent = new FormInputEvent<>(this, event);
      this.setTyping(false);
      this.submitEvents.onEvent(ent);
      if (ent.hasPreventedDefault()) {
         this.setTyping(true);
         return false;
      } else {
         event.use();
         return true;
      }
   }

   @Override
   public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
      if (this.isActive()) {
         if (this.rightClickToClear && !event.state && event.getID() == -99 && this.isMouseOver(event)) {
            this.setText("");
            this.submitChangeEvent();
            this.setTyping(true);
            event.use();
         }

         if (this.isTyping()) {
            if (event.state && this.tabTypingComponent != null && event.getID() == 258) {
               FormTypingComponent nextTypingComponent = this.tabTypingComponent.get();
               if (nextTypingComponent != null) {
                  event.use();
                  this.clearSelection();
                  nextTypingComponent.setTyping(true);
               }
            }

            if (event.state && (event.getID() == 257 || event.getID() == 335 || event.getID() == 256)) {
               FormInputEvent<FormTextInput> ent = new FormInputEvent<>(this, event);
               this.clearSelection();
               this.setTyping(false);
               this.submitEvents.onEvent(ent);
               if (ent.hasPreventedDefault()) {
                  this.setTyping(true);
               } else {
                  event.use();
               }
            }
         }

         super.handleInputEvent(
            InputEvent.OffsetHudEvent(WindowManager.getWindow().getInput(), event, -(this.getX() + this.width / 2), -(this.getY() + 5)),
            tickManager,
            perspective
         );
      }

      if (event.isMouseMoveEvent()) {
         this.isHovering = this.isMouseOver(event);
         if (this.isHovering) {
            event.useMove();
         }
      }
   }

   @Override
   public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
      if (this.rightClickToClear && this.isControllerFocus() && event.buttonState && event.getState() == ControllerInput.MENU_ITEM_ACTIONS_MENU) {
         this.setText("");
         this.submitChangeEvent();
         this.setTyping(true);
         event.use();
      }

      super.handleControllerEvent(event, tickManager, perspective);
   }

   @Override
   public List<Rectangle> getHitboxes() {
      return singleBox(new Rectangle(this.getX(), this.getY() + this.size.textureDrawOffset, this.width, this.size.height));
   }

   @Override
   protected Rectangle getTextBoundingBox() {
      Rectangle contentBox = this.size.getContentRectangle(this.width);
      return new Rectangle(contentBox.x - this.width / 2, contentBox.y - 5, contentBox.width, contentBox.height);
   }

   public GameTooltips getTooltips() {
      ListGameTooltips tooltips = null;
      if (this.rightClickToClear && this.rightClickToClearTooltip != null && !this.getText().isEmpty()) {
         tooltips = new ListGameTooltips();
         if (Input.lastInputIsController) {
            if (this.getManager().isControllerFocus(this)) {
               tooltips.add(new InputTooltip(ControllerInput.MENU_ITEM_ACTIONS_MENU, this.rightClickToClearTooltip.translate()));
            }
         } else {
            tooltips.add(new InputTooltip(-99, this.rightClickToClearTooltip.translate()));
         }
      }

      if (this.tooltip != null) {
         if (tooltips == null) {
            tooltips = new ListGameTooltips();
         }

         tooltips.add(this.tooltip);
      }

      return tooltips;
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      this.size.getInputDrawOptions(this.getInterfaceStyle(), this.getX(), this.getY(), this.width).draw();
      Rectangle contentRect = this.size.getContentRectangle(this.width);
      FormShader.FormShaderState shaderState = GameResources.formShader
         .startState(
            new Point(this.getX() + this.width / 2, this.getY()),
            new Rectangle(contentRect.x - this.width / 2, contentRect.y, contentRect.width, contentRect.height)
         );

      try {
         if (!this.isTyping() && this.getTextLength() == 0 && this.placeHolder != null) {
            FontOptions newOptions = new FontOptions(this.fontOptions).colorf(1.0F, 1.0F, 1.0F, 0.5F);
            int drawX = -FontManager.bit.getWidthCeil(this.placeHolder.translate(), newOptions) / 2;
            FontManager.bit.drawString(drawX, this.size.fontDrawOffset, this.placeHolder.translate(), newOptions);
         }

         super.draw(tickManager, perspective, renderBox);
         if (!this.isActive()) {
            Renderer.initQuadDraw(contentRect.width, contentRect.height).color(0.0F, 0.0F, 0.0F, 0.5F).draw(-this.width / 2, contentRect.y);
         }
      } finally {
         shaderState.end();
      }

      if (this.isHovering || this.isControllerFocus()) {
         GameTooltips tooltips = this.getTooltips();
         if (tooltips != null) {
            GameTooltipManager.addTooltip(tooltips, TooltipLocation.FORM_FOCUS);
         }
      }
   }

   @Override
   protected int getTextX() {
      return -this.scrollX;
   }

   @Override
   protected int getTextY() {
      return this.size.fontDrawOffset;
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
