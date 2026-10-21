package necesse.level.maps.levelData.settlementData.settler.dialogues;

import java.awt.Rectangle;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.forms.presets.containerComponent.mob.DialogueForm;
import necesse.gfx.forms.presets.containerComponent.mob.ShopContainerForm;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.mob.ShopContainer;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Recipe;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;

public class RomanceSettlerGiveItemDialogue extends RomanceSettlerDialogue {
   protected String turnInItemStringID;
   protected GameMessage initialButtonText;
   protected boolean shouldCompleteActivityOnTurnIn;
   protected boolean startFromSubForm;
   protected int equipItemOnFollowingSlot = -1;
   protected int amount;

   public RomanceSettlerGiveItemDialogue(HumanMob mob) {
      super(mob);
   }

   public RomanceSettlerGiveItemDialogue(HumanMob mob, String turnInItemStringID, int amount, boolean startFromSubForm, boolean shouldCompleteActivityOnTurnIn) {
      this(mob);
      this.turnInItemStringID = turnInItemStringID;
      this.shouldCompleteActivityOnTurnIn = shouldCompleteActivityOnTurnIn;
      this.startFromSubForm = startFromSubForm;
      this.amount = amount;
   }

   public RomanceSettlerGiveItemDialogue(
      HumanMob mob, String turnInItemStringID, int amount, boolean startFromSubForm, boolean shouldCompleteActivityOnTurnIn, int equipItemOnFollowingSlot
   ) {
      this(mob, turnInItemStringID, amount, startFromSubForm, shouldCompleteActivityOnTurnIn);
      this.equipItemOnFollowingSlot = equipItemOnFollowingSlot;
   }

   public RomanceSettlerDialogue addInitialButtonText(GameMessage message) {
      this.initialButtonText = message;
      return this;
   }

   @Override
   public boolean onAcceptClicked(ShopContainer container) {
      if (!container.client.isServer()) {
         return false;
      }

      if (!this.canPayForItem(container)) {
         return false;
      }

      ServerClient serverClient = container.client.getServerClient();
      Recipe.craft(this.getCost(), this.settlerMob.getLevel(), serverClient.playerMob, container.getCraftInventories(), null);
      if (this.equipItemOnFollowingSlot != -1) {
         InventoryItem existingItem = this.settlerMob.equipmentInventory.getItem(this.equipItemOnFollowingSlot);
         if (existingItem != null) {
            this.settlerMob
               .getLevel()
               .entityManager
               .pickups
               .add(existingItem.getPickupEntity(this.settlerMob.getLevel(), this.settlerMob.getX(), this.settlerMob.getY()));
         }

         InventoryItem newItem = new InventoryItem(this.turnInItemStringID);
         this.settlerMob.equipmentInventory.setItem(this.equipItemOnFollowingSlot, newItem);
      }

      if (this.shouldJoinAdventurePartyOnAccept) {
         this.joinAdventureParty(container);
      }

      PlayerRomanceManager romanceManager = this.romancePersonality.getPlayerManager(serverClient, true);
      if (this.shouldCompleteActivityOnTurnIn) {
         romanceManager.getCurrentRomanceActivity().activityCompleted = true;
      } else if (this.cooldownInDaysBeforeNextActivity != -1) {
         romanceManager.increaseRomanceLevel(this.cooldownInDaysBeforeNextActivity);
      } else {
         romanceManager.increaseRomanceLevel();
      }

      this.startYapping(serverClient, this.settlerChatsAfterAccepting, 4.0F, true, null);
      return true;
   }

   @Override
   public void onDeclineClicked(ShopContainer container) {
      super.onDeclineClicked(container);
   }

   @Override
   protected void setupContainerPacket(PacketWriter writer) {
      super.setupContainerPacket(writer);
      writer.putNextBoolean(this.startFromSubForm);
      writer.putNextString(this.turnInItemStringID);
      writer.putNextInt(this.amount);
      boolean initialButtonTextNotNull = this.initialButtonText != null;
      writer.putNextBoolean(initialButtonTextNotNull);
      if (initialButtonTextNotNull) {
         this.initialButtonText.writePacket(writer);
      }

      writer.putNextBoolean(this.shouldCompleteActivityOnTurnIn);
      boolean hasEquipSlotSaved = this.equipItemOnFollowingSlot != -1;
      writer.putNextBoolean(hasEquipSlotSaved);
      if (hasEquipSlotSaved) {
         writer.putNextInt(this.equipItemOnFollowingSlot);
      }
   }

   @Override
   protected void applyContainerPacket(PacketReader reader) {
      super.applyContainerPacket(reader);
      this.startFromSubForm = reader.getNextBoolean();
      this.turnInItemStringID = reader.getNextString();
      this.amount = reader.getNextInt();
      if (reader.getNextBoolean()) {
         this.initialButtonText = GameMessage.fromPacket(reader);
      }

      this.shouldCompleteActivityOnTurnIn = reader.getNextBoolean();
      if (reader.getNextBoolean()) {
         this.equipItemOnFollowingSlot = reader.getNextInt();
      }
   }

   @Override
   public void initForm(final ShopContainer container, ShopContainerForm<?> containerForm) {
      this.subForm = containerForm.addComponent(
         new DialogueForm("giveitemdialogueform", containerForm.width, containerForm.minHeight, containerForm.maxHeight, true) {
            @Override
            public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
               RomanceSettlerGiveItemDialogue.this.updateGiveItemButton(container);
               super.draw(tickManager, perspective, renderBox);
            }
         }
      );
      if (this.startFromSubForm || this.initialButtonText == null) {
         containerForm.makeCurrent(this.subForm);
      }
   }

   @Override
   public void setupDialogueOptions(ShopContainer container, ShopContainerForm<?> containerForm) {
      super.setupDialogueOptions(container, containerForm);
      if (this.completed) {
         this.subForm = null;
      } else {
         if (!this.startFromSubForm && this.initialButtonText != null) {
            containerForm.dialogueForm.addDialogueOption(this.initialButtonText, () -> containerForm.makeCurrent(this.subForm));
         }
      }
   }

   public Ingredient[] getCost() {
      return new Ingredient[]{new Ingredient(this.turnInItemStringID, this.amount)};
   }

   public boolean canPayForItem(ShopContainer container) {
      return container.canCraftRecipe(this.getCost(), container.getCraftInventories(), false, null).canCraft();
   }

   public void updateGiveItemButton(ShopContainer container) {
      if (this.startActivityButton != null) {
         this.startActivityButton.setActive(this.canPayForItem(container));
      }
   }
}
