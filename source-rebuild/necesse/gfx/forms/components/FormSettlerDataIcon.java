package necesse.gfx.forms.components;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import java.util.function.Supplier;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Input;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.PlayerSprite;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.position.FormPosition;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.forms.presets.containerComponent.settlement.SettlementContainerForm;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.InputTooltip;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.gfx.ui.HUD;
import necesse.inventory.container.settlement.data.SettlementSettlerData;

public class FormSettlerDataIcon extends FormButton implements FormPositionContainer {
   private FormPosition position;
   private final SettlementSettlerData data;
   private final SettlementContainerForm<?> containerForm;
   public Supplier<GameTooltips> extraTooltips;

   public FormSettlerDataIcon(int x, int y, SettlementSettlerData data, SettlementContainerForm<?> containerForm) {
      this.position = new FormFixedPosition(x, y);
      this.data = data;
      this.containerForm = containerForm;
      if (containerForm != null) {
         this.onClicked(e -> containerForm.selectedSettlers.selectOrDeselectSettler(data.mobUniqueID));
      }
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      PlayerSprite.drawInForms(
         (drawX, drawY) -> this.data.getIconDrawOptions(this.containerForm.getClient().getLevel(), 32, drawX, drawY).draw(), this.getX(), this.getY()
      );
      if (this.containerForm != null && this.containerForm.selectedSettlers.contains(this.data.mobUniqueID)) {
         HUD.selectBoundOptions(new Color(255, 255, 255), false, this.getX(), this.getY(), this.getX() + 32, this.getY() + 32).draw();
      }

      if (this.isHovering()) {
         GameTooltips tooltips = this.getTooltips();
         if (tooltips != null) {
            GameTooltipManager.addTooltip(tooltips, TooltipLocation.FORM_FOCUS);
         }
      }
   }

   public GameTooltips getTooltips() {
      ListGameTooltips tooltips = new ListGameTooltips(this.data.settler.getGenericMobName());
      if (this.containerForm != null) {
         if (Input.lastInputIsController) {
            tooltips.add(new InputTooltip(ControllerInput.MENU_SELECT, Localization.translate("ui", "settlementselectsettler")));
         } else {
            tooltips.add(new InputTooltip(-100, Localization.translate("ui", "settlementselectsettler")));
         }
      }

      if (this.extraTooltips != null) {
         GameTooltips extra = this.extraTooltips.get();
         if (extra != null) {
            tooltips.add(extra);
         }
      }

      return tooltips;
   }

   @Override
   public List<Rectangle> getHitboxes() {
      return singleBox(new Rectangle(this.getX(), this.getY(), 32, 32));
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
