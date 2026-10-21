package necesse.gfx.forms.components;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.quest.Quest;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.drawOptions.DrawOptionsBox;
import necesse.gfx.fairType.FairType;
import necesse.gfx.fairType.FairTypeDrawOptions;
import necesse.gfx.fairType.FairTypeDrawOptionsContainer;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.forms.controller.ControllerNavigationHandler;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.position.FormPosition;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;

public class FormQuestComponent extends FormComponent implements FormPositionContainer {
   protected FormPosition position;
   protected int width;
   public final NetworkClient client;
   public final Quest quest;
   public boolean outlined = false;
   public boolean showTitle = true;
   public boolean showDescription = true;
   public boolean showReward = true;
   public boolean showHandIn = true;
   protected FairTypeDrawOptionsContainer titleDrawOptions;
   protected FairTypeDrawOptionsContainer descDrawOptions;
   protected FairTypeDrawOptionsContainer rewardDrawOptions;
   protected FairTypeDrawOptionsContainer handInDrawOptions;
   protected boolean isHovering;
   public Color titleColor = this.getInterfaceStyle().highlightTextColor;
   public Color textColor = this.getInterfaceStyle().activeTextColor;

   public FormQuestComponent(int x, int y, int width, NetworkClient client, Quest quest) {
      this.position = new FormFixedPosition(x, y);
      this.width = width;
      this.client = client;
      this.quest = quest;
      this.titleDrawOptions = new FairTypeDrawOptionsContainer(() -> {
         GameMessage title = this.quest.getTitle();
         if (title == null) {
            return null;
         }

         FairType type = new FairType().append(new FontOptions(16).outline(this.outlined), title.translate());
         return type.getDrawOptions(FairType.TextAlign.LEFT, this.width - 20, true, 0, 1, true, null, true);
      });
      this.descDrawOptions = new FairTypeDrawOptionsContainer(() -> {
         GameMessage description = this.quest.getDescription();
         if (description == null) {
            return null;
         }

         FairType type = new FairType().append(new FontOptions(this.showTitle ? 12 : 16).outline(this.outlined), description.translate());
         return type.getDrawOptions(FairType.TextAlign.LEFT, this.width - 20, true, true);
      });
      this.rewardDrawOptions = new FairTypeDrawOptionsContainer(() -> {
         FairType type = this.quest.getRewardType(this.client, this.outlined);
         return type != null ? type.getDrawOptions(FairType.TextAlign.LEFT, this.width - 20, true, true) : null;
      });
      this.handInDrawOptions = new FairTypeDrawOptionsContainer(() -> {
         FairType type = this.quest.getHandInType(this.client, this.outlined);
         return type != null ? type.getDrawOptions(FairType.TextAlign.LEFT, this.width - 20, true, true) : null;
      });
   }

   @Override
   public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
      if (event.isMouseMoveEvent()) {
         this.isHovering = this.isMouseOver(event);
         if (this.isHovering) {
            event.useMove();
         }
      }

      int drawY = this.getY();
      if (this.showTitle) {
         FairTypeDrawOptions titleDrawOptions = this.titleDrawOptions.get();
         if (titleDrawOptions != null) {
            titleDrawOptions.handleInputEvent(this.getX(), drawY, event);
            drawY += titleDrawOptions.getBoundingBox().height + 2;
         }
      }

      if (this.showDescription) {
         FairTypeDrawOptions descDrawOptions = this.descDrawOptions.get();
         if (descDrawOptions != null) {
            descDrawOptions.handleInputEvent(this.getX(), drawY, event);
            drawY += descDrawOptions.getBoundingBox().height + 2;
         }
      }

      DrawOptionsBox progressDrawBox = this.quest.getProgressDrawBox(this.client, this.getX(), drawY, this.width - 20, this.textColor, false);
      drawY += progressDrawBox.getBoundingBox().height;
      if (this.showReward) {
         FairTypeDrawOptions rewardDrawOptions = this.rewardDrawOptions.get();
         if (rewardDrawOptions != null) {
            rewardDrawOptions.handleInputEvent(this.getX(), drawY, event);
            drawY += rewardDrawOptions.getBoundingBox().height + 2;
         }
      }

      if (this.showHandIn) {
         FairTypeDrawOptions handInDrawOptions = this.handInDrawOptions.get();
         if (handInDrawOptions != null) {
            handInDrawOptions.handleInputEvent(this.getX(), drawY, event);
            drawY += handInDrawOptions.getBoundingBox().height + 2;
         }
      }
   }

   @Override
   public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
   }

   @Override
   public void addNextControllerFocus(
      List<ControllerFocus> list, int currentXOffset, int currentYOffset, ControllerNavigationHandler customNavigationHandler, Rectangle area, boolean draw
   ) {
   }

   public void updateDrawables() {
      this.titleDrawOptions.reset();
      this.descDrawOptions.reset();
      this.rewardDrawOptions.reset();
      this.handInDrawOptions.reset();
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      ListGameTooltips tooltips = new ListGameTooltips();
      int drawY = this.getY();
      if (this.showTitle) {
         FairTypeDrawOptions titleDrawOptions = this.titleDrawOptions.get();
         if (titleDrawOptions != null) {
            titleDrawOptions.draw(this.getX(), drawY, this.titleColor);
            drawY += titleDrawOptions.getBoundingBox().height + 2;
            if (this.isHovering && !titleDrawOptions.displaysEverything()) {
               tooltips.add(this.quest.getTitle().translate());
            }
         }
      }

      if (this.showDescription) {
         FairTypeDrawOptions descDrawOptions = this.descDrawOptions.get();
         if (descDrawOptions != null) {
            descDrawOptions.draw(this.getX(), drawY, this.textColor);
            drawY += descDrawOptions.getBoundingBox().height + 2;
         }
      }

      DrawOptionsBox progressDrawBox = this.quest.getProgressDrawBox(this.client, this.getX(), drawY, this.width - 20, this.textColor, false);
      progressDrawBox.draw();
      drawY += progressDrawBox.getBoundingBox().height;
      if (this.showReward) {
         FairTypeDrawOptions rewardDrawOptions = this.rewardDrawOptions.get();
         if (rewardDrawOptions != null) {
            rewardDrawOptions.draw(this.getX(), drawY, this.textColor);
            drawY += rewardDrawOptions.getBoundingBox().height + 2;
         }
      }

      if (this.showHandIn) {
         FairTypeDrawOptions handInDrawOptions = this.handInDrawOptions.get();
         if (handInDrawOptions != null) {
            handInDrawOptions.draw(this.getX(), drawY, this.textColor);
            drawY += handInDrawOptions.getBoundingBox().height + 2;
         }
      }

      if (!tooltips.isEmpty()) {
         GameTooltipManager.addTooltip(tooltips, TooltipLocation.FORM_FOCUS);
      }
   }

   @Override
   public List<Rectangle> getHitboxes() {
      int width = 0;
      int height = 0;
      if (this.showTitle) {
         FairTypeDrawOptions titleDrawOptions = this.titleDrawOptions.get();
         if (titleDrawOptions != null) {
            Rectangle box = titleDrawOptions.getBoundingBox();
            height += box.height + 2;
            width = Math.max(width, box.width);
         }
      }

      if (this.showDescription) {
         FairTypeDrawOptions descDrawOptions = this.descDrawOptions.get();
         if (descDrawOptions != null) {
            Rectangle box = descDrawOptions.getBoundingBox();
            height += box.height + 2;
            width = Math.max(width, box.width);
         }
      }

      Rectangle progressBox = this.quest
         .getProgressDrawBox(this.client, this.getX(), this.getY() + height, this.width - 20, this.textColor, false)
         .getBoundingBox();
      height += progressBox.height;
      width = Math.max(width, progressBox.width);
      if (this.showReward) {
         FairTypeDrawOptions rewardDrawOptions = this.rewardDrawOptions.get();
         if (rewardDrawOptions != null) {
            Rectangle box = rewardDrawOptions.getBoundingBox();
            height += box.height + 2;
            width = Math.max(width, box.width);
         }
      }

      if (this.showHandIn) {
         FairTypeDrawOptions handInDrawOptions = this.handInDrawOptions.get();
         if (handInDrawOptions != null) {
            Rectangle box = handInDrawOptions.getBoundingBox();
            height += box.height + 2;
            width = Math.max(width, box.width);
         }
      }

      return singleBox(new Rectangle(this.getX(), this.getY(), width, height));
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
