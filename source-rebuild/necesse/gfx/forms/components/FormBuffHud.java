package necesse.gfx.forms.components;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import necesse.engine.GlobalData;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Input;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.modifiers.ModifierTooltip;
import necesse.engine.util.GameBlackboard;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.gfx.fairType.FairType;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.forms.controller.ControllerFocusHandler;
import necesse.gfx.forms.controller.ControllerNavigationHandler;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.position.FormPosition;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.InputTooltip;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;

public class FormBuffHud extends FormComponent implements FormPositionContainer {
   private FormPosition position;
   public int columns;
   public FairType.TextAlign align;
   private Mob owner;
   private ArrayList<FormBuffHud.BuffHudElement> buffs = new ArrayList<>();
   private FormBuffHud.BuffHudElement hovering;
   protected Predicate<ActiveBuff> filter;

   public FormBuffHud(int x, int y, int columns, FairType.TextAlign align, Mob owner, Predicate<ActiveBuff> filter) {
      this.position = new FormFixedPosition(x, y);
      this.columns = columns;
      this.align = align;
      this.filter = filter;
      this.setOwner(owner);
   }

   @Override
   public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
      if (event.isMouseMoveEvent()) {
         this.hovering = null;
         this.getBuffs();
         int mouseOverIndex = this.getMouseOverIndex(event.pos.hudX, event.pos.hudY, this.getWidth());
         if (mouseOverIndex >= 0 && mouseOverIndex < this.buffs.size()) {
            this.hovering = this.buffs.get(mouseOverIndex);
         }
      } else if (event.state && !event.isKeyboardEvent()) {
         if (event.getID() == -99 && this.isMouseOver(event)) {
            this.getBuffs();
            int mouseOverIndex = this.getMouseOverIndex(event.pos.hudX, event.pos.hudY, this.getWidth());
            if (mouseOverIndex >= 0 && mouseOverIndex < this.buffs.size()) {
               FormBuffHud.BuffHudElement element = this.buffs.get(mouseOverIndex);
               if (element.activeBuff.canCancel() || GlobalData.debugCheatActive()) {
                  element.activeBuff.owner.buffManager.removeBuff(element.activeBuff.buff.getID(), true);
                  event.use();
                  this.playTickSound();
               }
            }
         }
      }
   }

   @Override
   public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
      if (event.getState() == ControllerInput.MENU_SELECT && event.buttonState) {
         ControllerFocusHandler currentFocus = this.getControllerFocusHandler();
         if (currentFocus instanceof FormBuffHud.BuffHudElement && ((FormBuffHud.BuffHudElement)currentFocus).hud == this) {
            FormBuffHud.BuffHudElement hovering = (FormBuffHud.BuffHudElement)currentFocus;
            if (hovering.activeBuff.canCancel() || GlobalData.debugCheatActive()) {
               hovering.activeBuff.owner.buffManager.removeBuff(hovering.activeBuff.buff.getID(), true);
               event.use();
               this.playTickSound();
            }
         }
      }
   }

   @Override
   public void addNextControllerFocus(
      List<ControllerFocus> list, int currentXOffset, int currentYOffset, ControllerNavigationHandler customNavigationHandler, Rectangle area, boolean draw
   ) {
      int xOffset;
      if (this.align == FairType.TextAlign.LEFT) {
         xOffset = 0;
      } else if (this.align == FairType.TextAlign.RIGHT) {
         xOffset = -this.getWidth();
      } else {
         xOffset = -this.getWidth() / 2;
      }

      for (int i = 0; i < this.buffs.size(); i++) {
         int column = i % this.columns;
         int row = i / this.columns;
         FormBuffHud.BuffHudElement element = this.buffs.get(i);
         ControllerFocus.add(
            list,
            area,
            element,
            new Rectangle(32, 32),
            currentXOffset + this.getX() + xOffset + column * 40 + 2,
            currentYOffset + this.getY() + row * 40,
            0,
            customNavigationHandler
         );
      }
   }

   public void setOwner(Mob mob) {
      this.owner = mob;
   }

   public Mob getOwner() {
      return this.owner;
   }

   public void getBuffs() {
      this.buffs = this.getOwner()
         .buffManager
         .getBuffs()
         .values()
         .stream()
         .filter(ab -> ab.isVisible() || GlobalData.debugActive())
         .filter(this.filter)
         .map(ab -> new FormBuffHud.BuffHudElement(this, ab))
         .collect(Collectors.toCollection(ArrayList::new));
      Comparator<FormBuffHud.BuffHudElement> byIsVisible = Comparator.comparing(e -> e.activeBuff.isVisible());
      Comparator<FormBuffHud.BuffHudElement> byCanCancel = Comparator.comparing(e -> e.activeBuff.canCancel());
      Comparator<FormBuffHud.BuffHudElement> byShowsDuration = Comparator.comparing(e -> e.activeBuff.shouldDrawDuration());
      Comparator<FormBuffHud.BuffHudElement> byDuration = Comparator.comparingInt(
         e -> e.activeBuff.buff.shouldSortByDuration(e.activeBuff) ? e.activeBuff.getDurationLeft() : 0
      );
      Comparator<FormBuffHud.BuffHudElement> byID = Comparator.comparingInt(e -> e.activeBuff.buff.getID());
      this.buffs
         .sort(
            byIsVisible.reversed()
               .thenComparing(byShowsDuration)
               .thenComparing(byCanCancel.reversed())
               .thenComparing(byDuration.reversed())
               .thenComparing(byID)
         );
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      FormBuffHud.BuffHudElement hovering = this.hovering;
      ControllerFocusHandler currentFocus = this.getControllerFocusHandler();
      if (currentFocus instanceof FormBuffHud.BuffHudElement && ((FormBuffHud.BuffHudElement)currentFocus).hud == this) {
         hovering = (FormBuffHud.BuffHudElement)currentFocus;
      }

      if (hovering != null) {
         if (!hovering.activeBuff.isRemoved() && hovering.activeBuff.owner.buffManager.hasBuff(hovering.activeBuff.buff)) {
            GameTooltipManager.addTooltip(hovering.getTooltips(), TooltipLocation.FORM_FOCUS);
         } else {
            ControllerInput.submitNextRefreshFocusEvent();
            WindowManager.getWindow().submitNextMoveEvent();
         }
      }

      int xOffset;
      if (this.align == FairType.TextAlign.LEFT) {
         xOffset = 0;
      } else if (this.align == FairType.TextAlign.RIGHT) {
         xOffset = -this.getWidth();
      } else {
         xOffset = -this.getWidth() / 2;
      }

      for (int i = 0; i < this.buffs.size(); i++) {
         int column = i % this.columns;
         int row = i / this.columns;
         FormBuffHud.BuffHudElement element = this.buffs.get(i);
         element.activeBuff.drawIcon(this.getX() + xOffset + column * 40 + 2, this.getY() + row * 40);
      }
   }

   public int getColumns() {
      return Math.min(this.buffs.size(), this.columns);
   }

   public int getWidth() {
      return this.getColumns() * 40;
   }

   public int getRows() {
      int columns = this.getColumns();
      return columns == 0 ? 0 : (this.buffs.size() + columns - 1) / columns;
   }

   public int getHeight() {
      return this.getRows() * 40;
   }

   @Override
   public List<Rectangle> getHitboxes() {
      LinkedList<Rectangle> hitBoxes = new LinkedList<>();
      this.getBuffs();
      int width = this.getWidth();

      for (int i = 0; i < this.buffs.size(); i++) {
         int column = i % this.columns;
         int row = i / this.columns;
         hitBoxes.add(this.getMouseOverRect(column, row, width));
      }

      return hitBoxes;
   }

   public Rectangle getMouseOverRect(int column, int row, int width) {
      int columnX = column * 40 + 2;
      int rowY = row * 40;
      if (this.align == FairType.TextAlign.LEFT) {
         return new Rectangle(this.getX() + columnX, this.getY() + rowY, 32, 32);
      } else {
         return this.align == FairType.TextAlign.RIGHT
            ? new Rectangle(this.getX() - width + columnX, this.getY() + rowY, 32, 32)
            : new Rectangle(this.getX() - width / 2 + columnX, this.getY() + rowY, 32, 32);
      }
   }

   public boolean checkMouseOver(InputEvent event, int column, int row, int width) {
      return event.isMoveUsed() ? false : this.getMouseOverRect(column, row, width).contains(event.pos.hudX, event.pos.hudY);
   }

   public int getMouseOverIndex(int mouseX, int mouseY, int width) {
      int xOffset;
      if (this.align == FairType.TextAlign.LEFT) {
         xOffset = 0;
      } else if (this.align == FairType.TextAlign.RIGHT) {
         xOffset = -this.getWidth();
      } else {
         xOffset = -this.getWidth() / 2;
      }

      mouseX -= this.getX() + xOffset;
      mouseY -= this.getY();
      int column = mouseX / 40;
      if (column >= 0 && column <= this.columns) {
         int row = mouseY / 40;
         return new Rectangle(column * 40 + 2, row * 40, 32, 32).contains(mouseX, mouseY) ? row * this.columns + column : -1;
      } else {
         return -1;
      }
   }

   @Override
   public boolean isMouseOver(InputEvent event) {
      if (event.isMoveUsed()) {
         return false;
      }

      int mouseOverIndex = this.getMouseOverIndex(event.pos.hudX, event.pos.hudY, this.getWidth());
      return mouseOverIndex >= 0 && mouseOverIndex < this.buffs.size();
   }

   @Override
   public FormPosition getPosition() {
      return this.position;
   }

   @Override
   public void setPosition(FormPosition position) {
      this.position = position;
   }

   protected static class BuffHudElement implements ControllerFocusHandler {
      public final FormBuffHud hud;
      public final ActiveBuff activeBuff;

      public BuffHudElement(FormBuffHud hud, ActiveBuff activeBuff) {
         this.hud = hud;
         this.activeBuff = activeBuff;
      }

      public GameTooltips getTooltips() {
         ListGameTooltips tooltips = this.activeBuff.getTooltips(new GameBlackboard());
         if (this.activeBuff.canCancel()) {
            if (Input.lastInputIsController) {
               tooltips.add(new InputTooltip(ControllerInput.MENU_SELECT, Localization.translate("bufftooltip", "canceltip")) {
                  @Override
                  public int getDrawOrder() {
                     return 0;
                  }
               });
            } else {
               tooltips.add(new InputTooltip(-99, Localization.translate("bufftooltip", "canceltip")) {
                  @Override
                  public int getDrawOrder() {
                     return 0;
                  }
               });
            }
         }

         if (WindowManager.getWindow().isKeyDown(340)) {
            LinkedList<ModifierTooltip> modifierTooltips = this.activeBuff.getModifierTooltips();
            if (modifierTooltips.isEmpty()) {
               tooltips.add(Localization.translate("bufftooltip", "nomodifiers"));
            } else {
               this.activeBuff.getModifierTooltips().stream().map(mft -> mft.toTooltip(true)).forEach(tooltips::add);
            }
         }

         return tooltips;
      }

      @Override
      public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
      }

      @Override
      public boolean handleControllerNavigate(int dir, ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
         return false;
      }

      @Override
      public int getControllerFocusHashcode() {
         return this.hud.hashCode() + this.activeBuff.buff.getID();
      }
   }
}
