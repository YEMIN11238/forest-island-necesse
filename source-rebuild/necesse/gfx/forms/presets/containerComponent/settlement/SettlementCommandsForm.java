package necesse.gfx.forms.presets.containerComponent.settlement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.engine.window.GameWindow;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.FormSwitcher;
import necesse.gfx.forms.components.FormCheckBox;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.localComponents.FormLocalCheckBox;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.components.localComponents.FormLocalTextButton;
import necesse.gfx.forms.presets.containerComponent.mobCommands.MobCommandForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.inventory.container.settlement.SettlementContainerSettlerGroup;
import necesse.inventory.container.settlement.data.SettlementSettlerData;

public class SettlementCommandsForm<T extends SettlementContainer> extends MobCommandForm implements SettlementSubForm {
   public final SettlementContainerForm<T> containerForm;

   public SettlementCommandsForm(Client client, SettlementContainerForm<T> containerForm) {
      super(client, containerForm.getContainer().commandsHandler, containerForm.selectedSettlers);
      this.containerForm = containerForm;
   }

   @Override
   public Collection<SettlementContainerSettlerGroup> getGroups() {
      return this.containerForm.dataManager.settlerGroups.values();
   }

   @Override
   public SettlementSettlerData getSettlerData(int mobUniqueID) {
      return this.containerForm.dataManager.getSettler(mobUniqueID);
   }

   @Override
   public void updateNoneSelectedForm() {
      this.noneSelectedForm.clearComponents();
      FormFlow noneFlow = new FormFlow(4);
      this.noneSelectedForm
         .addComponent(noneFlow.nextY(new FormLocalLabel("ui", "settlementcommand", new FontOptions(20), 0, this.noneSelectedForm.getWidth() / 2, 0), 4));
      this.noneSelectedForm
         .addComponent(
            noneFlow.nextY(
               new FormLocalLabel(
                  "ui",
                  "settlementcommandtip",
                  new FontOptions(16),
                  0,
                  this.noneSelectedForm.getWidth() / 2,
                  this.noneSelectedForm.getWidth() / 2,
                  this.noneSelectedForm.getWidth() - 20
               ),
               8
            )
         );
      Collection<SettlementContainerSettlerGroup> groups = this.getGroups();
      if (groups != null && !groups.isEmpty()) {
         this.noneSelectedForm
            .addComponent(
               noneFlow.nextY(
                  new FormLocalLabel(
                     "ui",
                     "settlementorselectgroup",
                     new FontOptions(16),
                     0,
                     this.noneSelectedForm.getWidth() / 2,
                     this.noneSelectedForm.getWidth() / 2,
                     this.noneSelectedForm.getWidth() - 20
                  ),
                  8
               )
            );
         this.addGroupButtons(
            this.containerForm.dataManager.settlerGroups.values(), this.noneSelectedForm, noneFlow, 10, true, this.noneSelectedForm.getWidth() - 20, true
         );
         noneFlow.next(4);
      }

      this.noneSelectedForm
         .addComponent(
            noneFlow.nextY(
               new FormLocalTextButton("ui", "settlementcommandall", 4, 0, this.noneSelectedForm.getWidth() - 8, FormInputSize.SIZE_32, ButtonColor.BASE), 4
            )
         )
         .onClicked(e -> {
            synchronized (this.containerForm.selectedSettlers) {
               List<Integer> newSelected = this.containerForm.dataManager.streamSettlers().map(m -> m.mobUniqueID).collect(Collectors.toList());
               this.containerForm.selectedSettlers.selectSettlers(newSelected);
            }

            this.updateSelectedForm();
         });
      this.noneSelectedForm
         .addComponent(
            noneFlow.nextY(
               new FormLocalTextButton("ui", "settlementcommandclearall", 4, 0, this.noneSelectedForm.getWidth() - 8, FormInputSize.SIZE_32, ButtonColor.BASE),
               4
            )
         )
         .onClicked(e -> {
            HashSet<Integer> mobUniqueIDs = new HashSet<>();

            for (SettlementSettlerData settler : this.containerForm.dataManager.getSettlers()) {
               mobUniqueIDs.add(settler.mobUniqueID);
            }

            this.handler.commandClearOrdersAction.runAndSend(mobUniqueIDs);
         });
      this.noneSelectedForm.setHeight(noneFlow.next());
      ContainerComponent.setPosInventory(this.noneSelectedForm);
   }

   @Override
   public void updateSelectedForm() {
      this.selectedForm.clearComponents();
      FormFlow flow = new FormFlow(5);
      ArrayList<MobCommandForm.MobCommandData> mobs = this.setupBasicSelectedForm(flow);
      FormFlow settingsFlowX = new FormFlow(10);
      if (mobs.size() == 1 && this.selectedSettlers.getSize() == 1) {
         final MobCommandForm.MobCommandData data = mobs.get(0);
         if (data.settlerData != null) {
            FormContentIconButton assignBedButton = this.selectedForm
               .addComponent(
                  new FormContentIconButton(
                     settingsFlowX.next(36),
                     flow.next(),
                     FormInputSize.SIZE_32,
                     data.settlerData.bedPosition == null ? ButtonColor.RED : ButtonColor.BASE,
                     this.getInterfaceStyle().settler_assign_bed,
                     new GameMessage[0]
                  ) {
                     @Override
                     public GameTooltips getTooltips(PlayerMob perspective) {
                        ListGameTooltips tooltips = new ListGameTooltips();
                        tooltips.add(Localization.translate("ui", "settlerassignbed"));
                        if (data.settlerData.bedPosition == null) {
                           tooltips.add(Localization.translate("ui", "settlerhasnobed"));
                        }

                        return tooltips;
                     }
                  }
               );
            assignBedButton.onClicked(
               e -> this.containerForm.settlersForm.startAssignSettlerBedTool(data.mobUniqueID, data.settlerData.getSettlerName(this.client.getLevel(), "N/A"))
            );
         }
      }

      HashMap<Integer, Boolean> hideInsideMap = new HashMap<>();

      for (MobCommandForm.MobCommandData mob : mobs) {
         if (mob.commandMob != null) {
            hideInsideMap.put(mob.mobUniqueID, mob.commandMob.getHideOnLowHealth());
         } else if (mob.settlerData != null && mob.settlerData.commandData != null) {
            hideInsideMap.put(mob.mobUniqueID, mob.settlerData.commandData.hideOnLowHealth);
         }
      }

      if (!hideInsideMap.isEmpty()) {
         int checkboxX = settingsFlowX.next();
         boolean allHideInside = hideInsideMap.values().stream().allMatch(Boolean::booleanValue);
         FormLocalCheckBox allHideInsideCheckbox = this.selectedForm
            .addComponent(
               new FormLocalCheckBox(
                     "ui", "settlementcommandhidelowhealth", checkboxX, flow.next() + 8, allHideInside, this.selectedForm.getWidth() - checkboxX - 10
                  )
                  .useButtonTexture()
            );
         allHideInsideCheckbox.onClicked(e -> {
            mobs.forEach(m -> {
               if (m.commandMob != null) {
                  m.commandMob.setHideOnLowHealth(((FormCheckBox)e.from).checked);
               }

               if (m.settlerData != null && m.settlerData.commandData != null) {
                  m.settlerData.commandData.hideOnLowHealth = ((FormCheckBox)e.from).checked;
               }
            });
            synchronized (this.selectedSettlers) {
               this.containerForm.getContainer().commandHideOnLowHealthAction.runAndSend(this.selectedSettlers.get(), e.from.checked);
            }
         });
         flow.next(Math.max(32, allHideInsideCheckbox.getBoundingBox().height) + 4);
      }

      this.selectedForm
         .addComponent(
            flow.nextY(new FormLocalTextButton("ui", "cancelbutton", 4, 0, this.selectedForm.getWidth() - 8, FormInputSize.SIZE_32, ButtonColor.BASE), 4)
         )
         .onClicked(e -> {
            synchronized (this.selectedSettlers) {
               this.selectedSettlers.clear();
               this.updateCurrentForm();
            }
         });
      this.selectedForm.setHeight(flow.next());
      ContainerComponent.setPosInventory(this.selectedForm);
   }

   @Override
   public void onSetCurrent(boolean current) {
      if (current) {
         this.updateCurrentForm();
      }
   }

   @Override
   public void onMenuButtonClicked(FormSwitcher switcher) {
      SettlementSubForm.super.onMenuButtonClicked(switcher);
      this.updateCurrentForm();
   }

   @Override
   public void onWindowResized(GameWindow window) {
      super.onWindowResized(window);
      ContainerComponent.setPosInventory(this.noneSelectedForm);
      ContainerComponent.setPosInventory(this.selectedForm);
   }

   @Override
   public GameMessage getMenuButtonName() {
      return new LocalMessage("ui", "settlementcommand");
   }

   @Override
   public String getTypeString() {
      return "command";
   }
}
