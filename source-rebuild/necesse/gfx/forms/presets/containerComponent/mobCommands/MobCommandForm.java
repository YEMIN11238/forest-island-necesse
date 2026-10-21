package necesse.gfx.forms.presets.containerComponent.mobCommands;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.fairType.FairType;
import necesse.gfx.forms.ComponentListContainer;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.FormSwitcher;
import necesse.gfx.forms.FormUtils;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormSettlerDataNameLabel;
import necesse.gfx.forms.components.FormSettlerIconAbstract;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.presets.containerComponent.SelectedSettlersHandler;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.container.mobCommands.MobCommandsHandler;
import necesse.inventory.container.settlement.SettlementContainerSettlerGroup;
import necesse.inventory.container.settlement.data.SettlementSettlerData;
import necesse.level.maps.levelData.settlementData.settler.CommandMob;
import necesse.level.maps.levelData.settlementData.settler.Settler;

public abstract class MobCommandForm extends FormSwitcher {
   public final Client client;
   public final MobCommandsHandler handler;
   public final SelectedSettlersHandler selectedSettlers;
   protected Form noneSelectedForm;
   protected Form selectedForm;

   public MobCommandForm(Client client, MobCommandsHandler handler, SelectedSettlersHandler selectedSettlers) {
      this.client = client;
      this.handler = handler;
      this.selectedSettlers = selectedSettlers;
      this.noneSelectedForm = this.addComponent(new Form(408, 200));
      this.selectedForm = this.addComponent(new Form(508, 200));
   }

   @Override
   public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
      super.handleInputEvent(event, tickManager, perspective);
      if (!this.selectedSettlers.isEmpty() && event.getID() == 256) {
         if (!event.state) {
            synchronized (this.selectedSettlers) {
               this.selectedSettlers.clear();
               this.updateCurrentForm();
            }
         }

         event.use();
      }
   }

   @Override
   public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
      super.handleControllerEvent(event, tickManager, perspective);
      if (!this.selectedSettlers.isEmpty() && event.getState() == ControllerInput.MENU_BACK) {
         if (!event.buttonState) {
            synchronized (this.selectedSettlers) {
               this.selectedSettlers.clear();
               this.updateCurrentForm();
            }
         }

         event.use();
      }
   }

   public abstract Collection<SettlementContainerSettlerGroup> getGroups();

   public abstract SettlementSettlerData getSettlerData(int var1);

   public void addGroupButtons(
      Collection<SettlementContainerSettlerGroup> groups,
      ComponentListContainer<? super FormComponent> container,
      FormFlow flow,
      int startX,
      boolean centered,
      int maxWidth,
      final boolean asNoneSelected
   ) {
      FormUtils.GridButtonOptions groupOptions = FormUtils.setupGridButtons(32, 2);
      int currentGroupIndex = 0;

      for (final SettlementContainerSettlerGroup group : groups) {
         FormTextButton button = new FormTextButton("" + (currentGroupIndex + 1), 0, 0, 32, FormInputSize.SIZE_32, ButtonColor.BASE) {
            @Override
            protected void addTooltips(PlayerMob perspective) {
               super.addTooltips(perspective);
               if (asNoneSelected) {
                  StringTooltips tooltips = new StringTooltips();
                  if (group.mobUniqueIDs.isEmpty()) {
                     tooltips.add(Localization.translate("ui", "settlementgroupselectempty"));
                  } else {
                     tooltips.add(Localization.translate("ui", "settlementgroupselectsettlers", "count", group.mobUniqueIDs.size()));
                  }

                  GameTooltipManager.addTooltip(tooltips, TooltipLocation.FORM_FOCUS);
               }
            }
         };
         if (asNoneSelected) {
            if (group.mobUniqueIDs.isEmpty()) {
               button.setActive(false);
            } else {
               button.onClicked(e -> this.selectedSettlers.selectSettlers(false, group.mobUniqueIDs));
            }
         } else {
            boolean anyInGroup = false;
            boolean allInGroup = !this.selectedSettlers.isEmpty();

            for (int mobUniqueID : this.selectedSettlers.get()) {
               if (group.mobUniqueIDs.contains(mobUniqueID)) {
                  anyInGroup = true;
               } else {
                  allInGroup = false;
               }
            }

            if (allInGroup) {
               button.color = ButtonColor.GREEN;
            } else if (anyInGroup) {
               button.color = ButtonColor.YELLOW;
            }

            boolean finalAllInGroup = allInGroup;
            if (allInGroup) {
               button.setTooltip(Localization.translate("ui", "settlementgroupremovesettlers"));
            } else {
               button.setTooltip(Localization.translate("ui", "settlementgroupaddsettlers"));
            }

            button.onClicked(e -> this.handler.commandGroupAddOrRemoveAction.runAndSend(this.selectedSettlers.get(), group.groupUniqueID, !finalAllInGroup));
         }

         groupOptions.addButton(button);
         currentGroupIndex++;
      }

      groupOptions.apply(container, flow, startX, centered, maxWidth);
   }

   public abstract void updateNoneSelectedForm();

   public abstract void updateSelectedForm();

   public ArrayList<MobCommandForm.MobCommandData> setupBasicSelectedForm(FormFlow flow) {
      if (this.selectedSettlers.isEmpty()) {
         return new ArrayList<>();
      }

      ArrayList<MobCommandForm.MobCommandData> mobs = new ArrayList<>(this.selectedSettlers.getSize());
      synchronized (this.selectedSettlers) {
         for (int uniqueID : this.selectedSettlers.get()) {
            Mob mob = this.client.getLevel().entityManager.mobs.get(uniqueID, false);
            SettlementSettlerData settlerData = this.getSettlerData(uniqueID);
            if (mob instanceof CommandMob) {
               mobs.add(new MobCommandForm.MobCommandData((CommandMob)mob, settlerData));
            } else if (settlerData != null) {
               mobs.add(new MobCommandForm.MobCommandData(null, settlerData));
            }
         }
      }

      boolean canBeCommanded = false;
      boolean addedHeader = false;
      if (mobs.isEmpty()) {
         flow.next(5);
         GameMessage typeMessage = new LocalMessage("ui", "commandtypeunknowns");
         GameMessage headerLabel = new LocalMessage("ui", "commandcountselected", "count", this.selectedSettlers.getSize(), "type", typeMessage);
         this.selectedForm.addComponent(flow.nextY(new FormLocalLabel(headerLabel, new FontOptions(20), -1, 10, 0, this.selectedForm.getWidth() - 20), 8));
         canBeCommanded = true;
         addedHeader = true;
      } else if (mobs.size() == 1 && this.selectedSettlers.getSize() == 1) {
         MobCommandForm.MobCommandData data = mobs.get(0);
         if (data.commandMob != null) {
            data.commandMob.addCommandFormHeader(this.selectedForm, flow, this.selectedForm.getWidth());
            canBeCommanded = data.commandMob.canBeCommanded(this.client);
            addedHeader = true;
         }
      }

      if (!addedHeader) {
         for (MobCommandForm.MobCommandData data : mobs) {
            boolean thisCanBeCommanded = data.commandMob == null || data.commandMob.canBeCommanded(this.client);
            if (thisCanBeCommanded) {
               canBeCommanded = true;
               break;
            }
         }

         flow.next(5);
         int iconsY = flow.next();
         int iconsAdded = 0;
         int spacePerIcon = 10;

         for (final MobCommandForm.MobCommandData data : mobs) {
            FormSettlerIconAbstract icon = this.selectedForm
               .addComponent(
                  new FormSettlerIconAbstract(10 + iconsAdded * spacePerIcon, iconsY) {
                     @Override
                     public DrawOptions getFaceDrawOptions(int size, int drawX, int drawY) {
                        return data.commandMob != null
                           ? Settler.getHumanFaceDrawOptions(data.commandMob.getCommandFaceDrawOptions(), 32, drawX, drawY)
                           : data.settlerData.getIconDrawOptions(MobCommandForm.this.client.getLevel(), size, drawX, drawY);
                     }

                     @Override
                     public GameTooltips getTooltips() {
                        return null;
                     }
                  }
               );
            icon.canBePutOnTopByClick = false;
            if (++iconsAdded >= 5) {
               break;
            }
         }

         int labelX = 10 + iconsAdded * spacePerIcon + 32;
         FormComponent headerLabel;
         if (mobs.size() == 1 && this.selectedSettlers.getSize() == 1) {
            MobCommandForm.MobCommandData data = mobs.get(0);
            headerLabel = this.selectedForm
               .addComponent(
                  new FormSettlerDataNameLabel(
                     this.client,
                     data.settlerData,
                     new FontOptions(20),
                     FairType.TextAlign.LEFT,
                     labelX,
                     iconsY + 4,
                     this.selectedForm.getWidth() - labelX - 10
                  )
               );
         } else {
            GameMessage typeName = null;

            for (MobCommandForm.MobCommandData data : mobs) {
               GameMessage currentName = data.commandMob != null ? data.commandMob.getCommandMobTypeName() : new LocalMessage("ui", "commandtypesettlers");
               if (typeName != null && !currentName.isSame(typeName)) {
                  typeName = new LocalMessage("ui", "commandtypeunknowns");
                  break;
               }

               typeName = currentName;
            }

            GameMessage header = new LocalMessage("ui", "commandcountselected", "count", mobs.size(), "type", typeName);
            headerLabel = this.selectedForm
               .addComponent(new FormLocalLabel(header, new FontOptions(20), -1, labelX, iconsY + 4, this.selectedForm.getWidth() - labelX - 10));
         }

         int labelHeight = headerLabel.getBoundingBox().height;
         if (this.selectedSettlers.getSize() != mobs.size()) {
            int unknowns = this.selectedSettlers.getSize() - mobs.size();
            GameMessage unknownsType = new LocalMessage("ui", "commandtypeunknowns");
            GameMessage unknownsMessage = new LocalMessage("ui", "commandcountselected", "count", unknowns, "type", unknownsType);
            unknownsMessage = new GameMessageBuilder().append("+").append(unknownsMessage);
            FormLocalLabel unknownsLabel = this.selectedForm
               .addComponent(
                  new FormLocalLabel(unknownsMessage, new FontOptions(12), -1, labelX, iconsY + 4 + labelHeight, this.selectedForm.getWidth() - labelX - 10)
               );
            labelHeight += unknownsLabel.getBoundingBox().height;
         }

         flow.next(Math.max(32, labelHeight) + 8);
         if (mobs.size() == 1 && this.selectedSettlers.getSize() == 1) {
            FormLocalLabel label = this.selectedForm
               .addComponent(flow.nextY(new FormLocalLabel("settlement", "movecloser", new FontOptions(16), -1, 10, 0, this.selectedForm.getWidth() - 20), 10));
            label.setColor(this.getInterfaceStyle().inactiveTextColor);
         }
      }

      int centerSplitWidth = 20;
      int columnWidth = (this.selectedForm.getWidth() - 20 - centerSplitWidth) / 2;
      int startColumn1X = 10;
      int startColumn2X = 10 + columnWidth + centerSplitWidth;
      FormFlow column1Split = flow.split();
      FormFlow column2Split = flow.split();
      Collection<SettlementContainerSettlerGroup> groups = this.getGroups();
      if (groups != null && !groups.isEmpty()) {
         this.selectedForm
            .addComponent(column2Split.nextY(new FormLocalLabel("ui", "settlementchangegroup", new FontOptions(16), -1, startColumn2X, 0, columnWidth), 5));
         this.addGroupButtons(groups, this.selectedForm, column2Split, startColumn2X, false, columnWidth, false);
      } else {
         columnWidth = this.selectedForm.getWidth() - 20;
      }

      this.selectedForm
         .addComponent(column1Split.nextY(new FormLocalLabel("ui", "commandcommands", new FontOptions(16), -1, startColumn1X, 0, columnWidth), 5));
      FormUtils.GridButtonOptions commandButtons = FormUtils.setupGridButtons(32, 2);
      FormContentIconButton followButton = new FormContentIconButton(0, 0, FormInputSize.SIZE_32, ButtonColor.BASE, this.getInterfaceStyle().button_follow);
      followButton.onClicked(e -> {
         synchronized (this.selectedSettlers) {
            this.handler.commandFollowMeAction.runAndSend(this.selectedSettlers.get());
         }
      });
      followButton.setActive(canBeCommanded);
      followButton.setCooldown(500);
      if (!canBeCommanded) {
         followButton.setTooltips(new LocalMessage("ui", "commandcannot"));
      } else {
         followButton.setTooltips(new LocalMessage("ui", "settlementcommandfollow"));
      }

      commandButtons.addButton(followButton);
      FormContentIconButton clearCommandsButton = new FormContentIconButton(
         0, 0, FormInputSize.SIZE_32, ButtonColor.BASE, this.getInterfaceStyle().button_clear
      );
      clearCommandsButton.onClicked(e -> {
         synchronized (this.selectedSettlers) {
            this.handler.commandClearOrdersAction.runAndSend(this.selectedSettlers.get());
            this.selectedSettlers.clear();
            this.updateCurrentForm();
         }
      });
      clearCommandsButton.setActive(canBeCommanded);
      clearCommandsButton.setCooldown(500);
      if (!canBeCommanded) {
         clearCommandsButton.setTooltips(new LocalMessage("ui", "commandcannot"));
      } else {
         clearCommandsButton.setTooltips(new LocalMessage("ui", "settlementcommandclear"));
      }

      commandButtons.addButton(clearCommandsButton);
      commandButtons.apply(this.selectedForm, column1Split, startColumn1X, false, columnWidth);
      flow.next(Math.max(column1Split.next(), column2Split.next()) - flow.next() + 8);
      return mobs;
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      this.updateCurrentForm();
      super.draw(tickManager, perspective, renderBox);
   }

   public void updateCurrentForm() {
      if (this.selectedSettlers.isEmpty()) {
         if (!this.isCurrent(this.noneSelectedForm)) {
            this.updateNoneSelectedForm();
            this.makeCurrent(this.noneSelectedForm);
         }
      } else if (!this.isCurrent(this.selectedForm)) {
         this.updateSelectedForm();
         this.makeCurrent(this.selectedForm);
      }
   }

   public static class MobCommandData {
      public final int mobUniqueID;
      public CommandMob commandMob;
      public SettlementSettlerData settlerData;

      public MobCommandData(CommandMob commandMob, SettlementSettlerData settlerData) {
         this.mobUniqueID = commandMob != null ? commandMob.getMob().getUniqueID() : settlerData.mobUniqueID;
         this.commandMob = commandMob;
         this.settlerData = settlerData;
      }
   }
}
