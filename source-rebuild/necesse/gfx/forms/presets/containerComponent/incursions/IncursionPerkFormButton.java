package necesse.gfx.forms.presets.containerComponent.incursions;

import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.incursionPerkTree.IncursionPerk;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.registries.IncursionPerksRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameBackground;
import necesse.gfx.GameResources;
import necesse.gfx.forms.components.FormButton;
import necesse.gfx.forms.events.FormEventListener;
import necesse.gfx.forms.events.FormInputEvent;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.position.FormPosition;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.inventory.container.object.fallenAltar.FallenAltarContainer;
import necesse.level.maps.incursion.AltarData;

public class IncursionPerkFormButton extends FormButton implements FormPositionContainer {
   private FormPosition position;
   private final AltarData altarData;
   public final IncursionPerk perk;
   public final FallenAltarContainer altarContainer;
   private float grayscaleValue = 1.0F;
   private boolean goingUp = true;

   public IncursionPerkFormButton(
      int x, int y, final AltarData altarData, final IncursionPerk perk, final FallenAltarContainer altarContainer, final IncursionPerkTreeForm treeForm
   ) {
      this.altarData = altarData;
      this.perk = perk;
      this.altarContainer = altarContainer;
      this.position = new FormFixedPosition(x, y);
      this.acceptRightClicks = true;
      this.onClicked(
         new FormEventListener<FormInputEvent<FormButton>>() {
            public void onEvent(FormInputEvent<FormButton> event) {
               if (event.event.isControllerEvent()) {
                  if (event.event.getControllerEvent().getState() == ControllerInput.MENU_SELECT
                     && perk.canObtainPerk(altarData, altarContainer.canCheatAndShiftIsDown())
                     && (perk.canAffordToBuyPerk(altarContainer) || altarContainer.canCheatAndShiftIsDown())) {
                     altarContainer.obtainPerk.runAndSend(perk.getID());
                  }

                  if (event.event.getControllerEvent().getState() == ControllerInput.MENU_BACK
                     && perk.canRespecPerk(altarData)
                     && (perk.canAffordToRespecPerk(altarContainer) || altarContainer.canCheatAndShiftIsDown())) {
                     treeForm.updateGrayBackgroundHeight(altarData);
                     altarContainer.respecPerk.runAndSend(perk.getID());
                  }
               } else {
                  if (event.event.getID() == -100
                     && perk.canObtainPerk(altarData, altarContainer.canCheatAndShiftIsDown())
                     && (perk.canAffordToBuyPerk(altarContainer) || altarContainer.canCheatAndShiftIsDown())) {
                     altarContainer.obtainPerk.runAndSend(perk.getID());
                  }

                  if (event.event.getID() == -99
                     && perk.canRespecPerk(altarData)
                     && (perk.canAffordToRespecPerk(altarContainer) || altarContainer.canCheatAndShiftIsDown())) {
                     altarContainer.respecPerk.runAndSend(perk.getID());
                  }
               }
            }
         }
      );
   }

   public float getIconGreyScaleValue(TickManager tickManager) {
      if (this.perk.isPerkLocked(this.altarData)) {
         return 0.25F;
      }

      if (!this.altarData.obtainedPerkIDs.contains(this.perk.getID()) && this.perk.canObtainPerk(this.altarData, this.altarContainer.canCheatAndShiftIsDown())) {
         if (this.grayscaleValue >= 1.0F) {
            this.goingUp = false;
         } else if (this.grayscaleValue <= 0.75F) {
            this.goingUp = true;
         }

         float delta = tickManager.getDelta();
         if (this.goingUp) {
            this.grayscaleValue += delta * 1.5E-4F;
         } else {
            this.grayscaleValue -= delta * 1.5E-4F;
         }

         return this.grayscaleValue;
      } else {
         return this.altarData.obtainedPerkIDs.contains(this.perk.getID()) ? 1.0F : 0.4F;
      }
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      this.perk.iconTexture.initDraw().color(this.getIconGreyScaleValue(tickManager)).draw(this.getX(), this.getY());
      GameTexture perkTreePerkBorder = GameResources.perkTreePerkBorder;
      int perkTreePerkBorderOffSetRes = (perkTreePerkBorder.getWidth() - this.perk.iconTexture.getWidth()) / 2;
      boolean canCheat = this.altarContainer.canCheatAndShiftIsDown();
      perkTreePerkBorder.initDraw()
         .color(this.perk.canObtainPerk(this.altarData, canCheat) ? 1.0F : 0.4F)
         .draw(this.getX() - perkTreePerkBorderOffSetRes, this.getY() - perkTreePerkBorderOffSetRes);
      if (this.perk.canObtainPerk(this.altarData, canCheat) && (this.perk.canAffordToBuyPerk(this.altarContainer) || canCheat)) {
         GameTexture perkTreeCanBuyPerkBorderTexture = GameResources.perkTreeCanBuyPerkBorder;
         int offSetRes = (perkTreeCanBuyPerkBorderTexture.getWidth() - this.perk.iconTexture.getWidth()) / 2;
         perkTreeCanBuyPerkBorderTexture.initDraw().color(this.getIconGreyScaleValue(tickManager)).draw(this.getX() - offSetRes, this.getY() - offSetRes);
      }

      if (this.altarData.hasPerk(this.perk)) {
         GameTexture perkTreeOwnsPerkBorder = GameResources.perkTreeOwnsPerkBorder;
         int offSetRes = (perkTreeOwnsPerkBorder.getWidth() - this.perk.iconTexture.getWidth()) / 2;
         perkTreeOwnsPerkBorder.initDraw().draw(this.getX() - offSetRes, this.getY() - offSetRes);
      }

      if (this.perk.isPerkLocked(this.altarData)) {
         GameTexture lockedPerkOverlay = GameResources.lockedPerkOverlay;
         int offSetRes = (lockedPerkOverlay.getWidth() - this.perk.iconTexture.getWidth()) / 2;
         lockedPerkOverlay.initDraw().posMiddle(this.getX(), this.getY()).draw(this.getX() - offSetRes, this.getY() - offSetRes);
      }

      if (this.perk.highlightLock) {
         GameTexture lockedPerkOverlay = GameResources.lockedPerkOverlay;
         int offSetRes = (lockedPerkOverlay.getWidth() - this.perk.iconTexture.getWidth()) / 2;
         lockedPerkOverlay.initDraw().posMiddle(this.getX(), this.getY()).draw(this.getX() - offSetRes, this.getY() - offSetRes);
      }

      if (this.perk.locksAllOtherPerksOnTier() && !this.perk.hasObtainedPerk(this.altarData)) {
         GameTexture perkTreePerkLocksOtherPerksBorderTexture = GameResources.perkTreePerkLocksOtherPerksBorder;
         int offSetRes = (perkTreePerkLocksOtherPerksBorderTexture.getWidth() - this.perk.iconTexture.getWidth()) / 2;
         perkTreePerkLocksOtherPerksBorderTexture.initDraw().posMiddle(this.getX(), this.getY()).draw(this.getX() - offSetRes, this.getY() - offSetRes);
      }

      this.perk.highlightLock = false;
      if (this.isHovering()) {
         ListGameTooltips tooltips = this.perk.getTooltips(this.altarData, this.altarContainer, canCheat);
         if (tooltips != null) {
            GameTooltipManager.addTooltip(tooltips, GameBackground.getItemTooltipBackground(), TooltipLocation.FORM_FOCUS);
         }

         for (IncursionPerk currentPerk : IncursionPerksRegistry.getPerks()) {
            if (currentPerk.otherPerksThatLockThisPerk.contains(this.perk)
               && !currentPerk.isPerkLocked(this.altarData)
               && this.perk.canObtainPerk(this.altarData, canCheat)) {
               currentPerk.highlightLock = true;
            }
         }
      }
   }

   @Override
   public List<Rectangle> getHitboxes() {
      return singleBox(new Rectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight()));
   }

   @Override
   public FormPosition getPosition() {
      return this.position;
   }

   @Override
   public void setPosition(FormPosition position) {
      this.position = position;
   }

   public int getWidth() {
      return 64;
   }

   public int getHeight() {
      return 64;
   }
}
