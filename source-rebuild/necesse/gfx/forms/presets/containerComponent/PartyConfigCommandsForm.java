package necesse.gfx.forms.presets.containerComponent;

import java.util.Collection;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.components.localComponents.FormLocalTextButton;
import necesse.gfx.forms.presets.containerComponent.mobCommands.MobCommandForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.container.mobCommands.MobCommandsHandler;
import necesse.inventory.container.settlement.SettlementContainerSettlerGroup;
import necesse.inventory.container.settlement.data.SettlementSettlerData;

public class PartyConfigCommandsForm extends MobCommandForm {
   protected Runnable inventoryPressed;
   protected int lastPartySize;
   protected FormLocalLabel nonePartySizeLabel;
   protected FormLocalLabel selectedPartySizeLabel;

   public PartyConfigCommandsForm(Client client, MobCommandsHandler commandsHandler, SelectedSettlersHandler selectedHandler, Runnable inventoryPressed) {
      super(client, commandsHandler, selectedHandler);
      this.inventoryPressed = inventoryPressed;
   }

   @Override
   public void updateNoneSelectedForm() {
      this.noneSelectedForm.clearComponents();
      FormFlow noneFlow = new FormFlow(4);
      this.noneSelectedForm
         .addComponent(noneFlow.nextY(new FormLocalLabel("ui", "settlementcommand", new FontOptions(20), 0, this.noneSelectedForm.getWidth() / 2, 0), 4));
      this.nonePartySizeLabel = this.noneSelectedForm
         .addComponent(
            noneFlow.nextY(
               new FormLocalLabel(
                  new LocalMessage("ui", "adventurepartysize", "size", this.lastPartySize), new FontOptions(12), 0, this.noneSelectedForm.getWidth() / 2, 0
               ),
               5
            )
         );
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
      this.noneSelectedForm
         .addComponent(
            noneFlow.nextY(
               new FormLocalTextButton("ui", "adventurepartycommandall", 4, 0, this.noneSelectedForm.getWidth() - 8, FormInputSize.SIZE_32, ButtonColor.BASE),
               4
            )
         )
         .onClicked(e -> {
            synchronized (this.selectedSettlers) {
               synchronized (this.client.adventureParty) {
                  this.selectedSettlers.selectSettlers(this.client.adventureParty.getMobUniqueIDs());
               }
            }

            this.updateSelectedForm();
         });
      if (this.inventoryPressed != null) {
         this.noneSelectedForm
            .addComponent(
               noneFlow.nextY(
                  new FormLocalTextButton("ui", "adventurepartyinventory", 4, 0, this.noneSelectedForm.getWidth() - 8, FormInputSize.SIZE_32, ButtonColor.BASE),
                  4
               )
            )
            .onClicked(e -> this.inventoryPressed.run());
      }

      this.noneSelectedForm.setHeight(noneFlow.next());
      ContainerComponent.setPosInventory(this.noneSelectedForm);
   }

   @Override
   public void updateSelectedForm() {
      this.selectedForm.clearComponents();
      FormFlow flow = new FormFlow(5);
      this.setupBasicSelectedForm(flow);
      this.selectedPartySizeLabel = this.selectedForm
         .addComponent(
            flow.nextY(
               new FormLocalLabel(
                  new LocalMessage("ui", "adventurepartysize", "size", this.lastPartySize), new FontOptions(12), 0, this.selectedForm.getWidth() / 2, 0
               ),
               5
            )
         );
      if (this.inventoryPressed != null) {
         this.selectedForm
            .addComponent(
               flow.nextY(
                  new FormLocalTextButton("ui", "adventurepartyinventory", 4, 0, this.selectedForm.getWidth() - 8, FormInputSize.SIZE_32, ButtonColor.BASE), 4
               )
            )
            .onClicked(e -> this.inventoryPressed.run());
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
   public void updateCurrentForm() {
      super.updateCurrentForm();
      this.lastPartySize = this.client.adventureParty.getSize();
      if (this.nonePartySizeLabel != null) {
         this.nonePartySizeLabel.setLocalization(new LocalMessage("ui", "adventurepartysize", "size", this.lastPartySize));
      }

      if (this.selectedPartySizeLabel != null) {
         this.selectedPartySizeLabel.setLocalization(new LocalMessage("ui", "adventurepartysize", "size", this.lastPartySize));
      }
   }

   @Override
   public Collection<SettlementContainerSettlerGroup> getGroups() {
      return null;
   }

   @Override
   public SettlementSettlerData getSettlerData(int mobUniqueID) {
      return null;
   }
}
