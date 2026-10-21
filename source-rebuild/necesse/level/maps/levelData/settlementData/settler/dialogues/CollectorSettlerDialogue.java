package necesse.level.maps.levelData.settlementData.settler.dialogues;

import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Collectors;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.SettlerPersonalityRegistry;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.components.FormDialogueOption;
import necesse.gfx.forms.presets.containerComponent.mob.DialogueForm;
import necesse.gfx.forms.presets.containerComponent.mob.ShopContainerForm;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.customAction.EmptyCustomAction;
import necesse.inventory.container.mob.ShopContainer;
import necesse.inventory.recipe.GlobalIngredient;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Recipe;
import necesse.level.maps.levelData.settlementData.settler.personalities.CollectorSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.SettlerPersonality;

public class CollectorSettlerDialogue extends SettlerDialogue {
   protected CollectorSettlerPersonality personality;
   protected EmptyCustomAction giveGiftAction;
   protected EmptyCustomAction startTakeFromStorageAction;
   protected EmptyCustomAction stopTakingFromStorageAction;
   protected DialogueForm subForm;
   protected FormDialogueOption giveGiftButton;
   protected boolean stoppedAllowedToTakeFromStorage;
   protected int personalityID;
   protected Ingredient[] cost;
   protected boolean canGiveGift;
   protected boolean isAllowedToTakeFromStorage;

   public CollectorSettlerDialogue(HumanMob mob) {
      super(mob);
   }

   public CollectorSettlerDialogue(HumanMob mob, CollectorSettlerPersonality personality) {
      this(mob);
      this.personality = personality;
      this.personalityID = personality.getID();
      this.cost = personality.getCollectorCost();
      this.canGiveGift = personality.canGiveGift();
      this.isAllowedToTakeFromStorage = personality.allowTakeFromStorage;
   }

   @Override
   protected void setupContainerPacket(PacketWriter writer) {
      super.setupContainerPacket(writer);
      writer.putNextShortUnsigned(this.personalityID);
      writer.putNextBoolean(this.canGiveGift);
      writer.putNextBoolean(this.isAllowedToTakeFromStorage);
   }

   @Override
   protected void applyContainerPacket(PacketReader reader) {
      super.applyContainerPacket(reader);
      this.personalityID = reader.getNextShortUnsigned();
      this.canGiveGift = reader.getNextBoolean();
      this.isAllowedToTakeFromStorage = reader.getNextBoolean();
   }

   @Override
   public void init(final ShopContainer container) {
      super.init(container);
      if (this.cost == null) {
         for (SettlerPersonality personality : this.settlerMob.getPersonalities()) {
            if (personality.getID() == this.personalityID && personality instanceof CollectorSettlerPersonality) {
               this.cost = ((CollectorSettlerPersonality)personality).getCollectorCost();
               break;
            }
         }
      }

      this.giveGiftAction = container.registerAction(
         new EmptyCustomAction() {
            @Override
            protected void run() {
               if (CollectorSettlerDialogue.this.settlerMob.isServer()
                  && CollectorSettlerDialogue.this.personality != null
                  && CollectorSettlerDialogue.this.personality.canGiveGift()
                  && CollectorSettlerDialogue.this.canPayForIngredient(container)) {
                  CollectorSettlerDialogue.this.personality.onGiftGiven();
                  Recipe.craft(
                     CollectorSettlerDialogue.this.cost,
                     CollectorSettlerDialogue.this.settlerMob.getLevel(),
                     container.client.playerMob,
                     container.getCraftInventories(),
                     null
                  );
               }

               CollectorSettlerDialogue.this.canGiveGift = false;
            }
         }
      );
      this.startTakeFromStorageAction = container.registerAction(new EmptyCustomAction() {
         @Override
         protected void run() {
            if (CollectorSettlerDialogue.this.settlerMob.isServer() && CollectorSettlerDialogue.this.personality != null) {
               CollectorSettlerDialogue.this.personality.allowTakeFromStorage = true;
            }

            CollectorSettlerDialogue.this.isAllowedToTakeFromStorage = true;
         }
      });
      this.stopTakingFromStorageAction = container.registerAction(new EmptyCustomAction() {
         @Override
         protected void run() {
            if (CollectorSettlerDialogue.this.settlerMob.isServer() && CollectorSettlerDialogue.this.personality != null) {
               CollectorSettlerDialogue.this.personality.allowTakeFromStorage = false;
            }

            CollectorSettlerDialogue.this.isAllowedToTakeFromStorage = false;
            CollectorSettlerDialogue.this.stoppedAllowedToTakeFromStorage = true;
         }
      });
   }

   @Override
   public void initForm(final ShopContainer container, ShopContainerForm<?> containerForm) {
      super.initForm(container, containerForm);
      this.subForm = containerForm.addComponent(
         new DialogueForm("collectorgift", containerForm.width, containerForm.minHeight, containerForm.maxHeight, true) {
            @Override
            public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
               CollectorSettlerDialogue.this.updateGiveGiftButton(container);
               super.draw(tickManager, perspective, renderBox);
            }
         }
      );
   }

   @Override
   public void setupDialogueOptions(ShopContainer container, ShopContainerForm<?> containerForm) {
      super.setupDialogueOptions(container, containerForm);
      this.giveGiftButton = null;
      if (container.hasSettlerAccess && this.cost != null) {
         this.subForm
            .reset(
               container.humanShop,
               true,
               container.romanceLevel,
               (contentBox, flow) -> {
                  Runnable chatBubble = DialogueForm.startChatBubble(contentBox, flow);
                  if (this.stoppedAllowedToTakeFromStorage) {
                     DialogueForm.addText(contentBox, flow, this.getStoppedTakingFromChestMessage(), true);
                  } else {
                     DialogueForm.addText(contentBox, flow, this.getIntroMessage(), true);
                     flow.next(5);

                     for (Ingredient ingredient : this.cost) {
                        int ingredientAmount = ingredient.getIngredientAmount();
                        String amountString = ingredientAmount == 1 ? "" : ingredientAmount + " ";
                        GameMessage itemMessage;
                        if (ingredient.isGlobalIngredient()) {
                           GlobalIngredient globalIngredient = ingredient.getGlobalIngredient();
                           List<InventoryItem> anyGiftItems = globalIngredient.getObtainableRegisteredItemIDs()
                              .stream()
                              .map(itemID -> new InventoryItem(ItemRegistry.getItem(itemID)))
                              .collect(Collectors.toList());
                           itemMessage = new GameMessageBuilder()
                              .append(TypeParsers.getItemsParseString(anyGiftItems) + " ")
                              .append(amountString)
                              .append(globalIngredient.displayName);
                        } else {
                           itemMessage = new GameMessageBuilder()
                              .append(TypeParsers.getItemParseString(ingredient.ingredientStringID) + " ")
                              .append(amountString)
                              .append(ItemRegistry.getLocalization(ingredient.ingredientStringID));
                        }

                        DialogueForm.addText(contentBox, flow, itemMessage, true);
                     }
                  }

                  chatBubble.run();
               }
            );
         if (this.stoppedAllowedToTakeFromStorage) {
            this.subForm.addDialogueOption(new LocalMessage("ui", "backbutton"), () -> {
               this.stoppedAllowedToTakeFromStorage = false;
               containerForm.updateDialogue();
               containerForm.makeCurrent(containerForm.dialogueForm);
            });
         } else {
            this.giveGiftButton = this.subForm.addDialogueOption(this.getGiveItemMessage(), () -> {
               this.giveGiftAction.runAndSend();
               containerForm.updateDialogue();
               containerForm.makeCurrent(containerForm.dialogueForm);
            });
            this.subForm.addDialogueOption(this.getStartTakingFromStorageMessage(), () -> {
               this.startTakeFromStorageAction.runAndSend();
               this.subForm.reset(container.humanShop, true, container.romanceLevel, this.getAllowedToTakeFromStorageResponse());
               this.subForm.addDialogueOption(new LocalMessage("ui", "backbutton"), () -> {
                  containerForm.updateDialogue();
                  containerForm.makeCurrent(containerForm.dialogueForm);
               });
               containerForm.makeCurrent(this.subForm);
               containerForm.onWindowResized(WindowManager.getWindow());
            });
            this.subForm.addDialogueOption(new LocalMessage("ui", "backbutton"), () -> containerForm.makeCurrent(containerForm.dialogueForm));
         }

         if (this.isAllowedToTakeFromStorage) {
            containerForm.dialogueForm.addDialogueOption(this.getStopTakingFromStorageMessage(), () -> {
               this.stopTakingFromStorageAction.runAndSend();
               containerForm.updateDialogue();
               containerForm.makeCurrent(this.subForm);
            });
         } else if (this.canGiveGift) {
            containerForm.dialogueForm.addDialogueOption(this.getHaveItemMessage(), () -> containerForm.makeCurrent(this.subForm));
         } else {
            containerForm.dialogueForm.addDialogueOption(this.getStartTakingFromStorageMessage(), () -> {
               this.startTakeFromStorageAction.runAndSend();
               this.subForm.reset(container.humanShop, true, container.romanceLevel, this.getAllowedToTakeFromStorageResponse());
               this.subForm.addDialogueOption(new LocalMessage("ui", "backbutton"), () -> {
                  containerForm.updateDialogue();
                  containerForm.makeCurrent(containerForm.dialogueForm);
               });
               containerForm.makeCurrent(this.subForm);
               containerForm.onWindowResized(WindowManager.getWindow());
            });
         }
      } else {
         this.subForm = null;
      }
   }

   public GameMessage getHaveItemMessage() {
      String personalityStringID = SettlerPersonalityRegistry.getSettlerPersonalityStringID(this.personalityID);
      return new LocalMessage("personalities", personalityStringID + "have");
   }

   public GameMessage getIntroMessage() {
      String personalityStringID = SettlerPersonalityRegistry.getSettlerPersonalityStringID(this.personalityID);
      return new LocalMessage("personalities", personalityStringID + "intro");
   }

   public GameMessage getGiveItemMessage() {
      String personalityStringID = SettlerPersonalityRegistry.getSettlerPersonalityStringID(this.personalityID);
      return new LocalMessage("personalities", personalityStringID + "give");
   }

   public GameMessage getStartTakingFromStorageMessage() {
      String personalityStringID = SettlerPersonalityRegistry.getSettlerPersonalityStringID(this.personalityID);
      return new LocalMessage("personalities", personalityStringID + "start");
   }

   public GameMessage getAllowedToTakeFromStorageResponse() {
      return new LocalMessage("personalities", "collectortakethanks");
   }

   public GameMessage getStopTakingFromStorageMessage() {
      String personalityStringID = SettlerPersonalityRegistry.getSettlerPersonalityStringID(this.personalityID);
      return new LocalMessage("personalities", personalityStringID + "stop");
   }

   public GameMessage getStoppedTakingFromChestMessage() {
      return new LocalMessage("personalities", "collectorstoppedtaking");
   }

   public boolean canPayForIngredient(ShopContainer container) {
      return container.canCraftRecipe(this.cost, container.getCraftInventories(), true, null).canCraft();
   }

   public void updateGiveGiftButton(ShopContainer container) {
      if (this.giveGiftButton != null) {
         this.giveGiftButton.setActive(this.canPayForIngredient(container));
      }
   }

   @Override
   public <T extends ShopContainer> void onWindowResized(GameWindow window, ShopContainer container, ShopContainerForm<?> containerForm) {
      super.onWindowResized(window, container, containerForm);
      if (this.subForm != null) {
         ContainerComponent.setPosFocus(this.subForm);
      }
   }
}
