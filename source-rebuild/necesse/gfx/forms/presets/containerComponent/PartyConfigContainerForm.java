package necesse.gfx.forms.presets.containerComponent;

import necesse.engine.gameTool.GameToolManager;
import necesse.engine.network.client.Client;
import necesse.engine.window.GameWindow;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.forms.ContainerComponent;
import necesse.inventory.container.AdventurePartyConfigContainer;

public class PartyConfigContainerForm extends ContainerFormSwitcher<AdventurePartyConfigContainer> {
   public static boolean startInCommand = true;
   public PartyConfigForm partyConfigForm;
   public PartyConfigCommandsForm commandsForm;
   public final SelectedSettlersHandler selectedSettlers;
   public SelectSettlersContainerGameTool tool;

   public PartyConfigContainerForm(Client client, AdventurePartyConfigContainer container) {
      super(client, container);
      this.selectedSettlers = new SelectedSettlersHandler(client) {
         @Override
         public void updateSelectedSettlers(boolean switchToCommandForm) {
            PartyConfigContainerForm.this.updateSelectedSettlers(switchToCommandForm);
         }
      };
      this.partyConfigForm = this.addComponent(new PartyConfigForm(client, container, container.PARTY_SLOTS_START, container.PARTY_SLOTS_END, 408, () -> {
         this.commandsForm.updateCurrentForm();
         this.makeCurrent(this.commandsForm);
         synchronized (this.selectedSettlers) {
            synchronized (client.adventureParty) {
               this.selectedSettlers.selectSettlers(client.adventureParty.getMobUniqueIDs());
            }
         }

         this.commandsForm.updateSelectedForm();
      }, null, () -> ContainerComponent.setPosFocus(this.partyConfigForm)));
      this.commandsForm = this.addComponent(
         new PartyConfigCommandsForm(client, container.commandsHandler, this.selectedSettlers, () -> this.makeCurrent(this.partyConfigForm)),
         (form, isCurrent) -> {
            if (!isCurrent) {
               if (this.tool != null) {
                  GameToolManager.clearGameTool(this.tool);
               }

               this.tool = null;
            } else {
               if (this.tool != null) {
                  GameToolManager.clearGameTool(this.tool);
               }

               GameToolManager.setGameTool(this.tool = new PartyConfigGameTool(client, this.selectedSettlers, container));
            }
         }
      );
   }

   @Override
   protected void init() {
      super.init();
      this.selectedSettlers.init();
      this.client.adventureParty.updateMobsFromLevel(this.client.getLevel());
      this.selectedSettlers.cleanUp(uniqueID -> {
         if (this.client.adventureParty.contains(uniqueID)) {
            return true;
         }

         Mob mob = this.client.getLevel().entityManager.mobs.get(uniqueID, false);
         return mob instanceof HumanMob && ((HumanMob)mob).canBeCommandPartySelected(this.client);
      });
      this.commandsForm.updateSelectedForm();
      this.commandsForm.updateCurrentForm();
      boolean shouldBeInventoryForm = this.selectedSettlers.isEmpty()
         && this.client.adventureParty.isEmpty()
         && this.client.getPlayer().getInv().hasPartyItems();
      this.makeCurrent(startInCommand && !shouldBeInventoryForm ? this.commandsForm : this.partyConfigForm);
   }

   @Override
   public boolean shouldOpenInventory() {
      return true;
   }

   public void updateSelectedSettlers(boolean switchToCommandForm) {
      if (!this.selectedSettlers.isEmpty()) {
         if (switchToCommandForm && !this.isCurrent(this.commandsForm) || this.getCurrent() == null) {
            this.commandsForm.updateCurrentForm();
            this.makeCurrent(this.commandsForm);
         }

         this.commandsForm.updateSelectedForm();
      } else if (this.isCurrent(this.commandsForm)) {
         this.commandsForm.updateCurrentForm();
      }
   }

   @Override
   public boolean shouldShowInventory() {
      return !this.isCurrent(this.commandsForm);
   }

   @Override
   public void onWindowResized(GameWindow window) {
      super.onWindowResized(window);
      ContainerComponent.setPosFocus(this.partyConfigForm);
   }

   @Override
   public void dispose() {
      startInCommand = this.isCurrent(this.commandsForm);
      super.dispose();
      this.selectedSettlers.dispose();
      if (this.tool != null) {
         GameToolManager.clearGameTool(this.tool);
      }
   }
}
