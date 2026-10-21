package necesse.gfx.forms.presets.containerComponent.mob;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.engine.storyObjectives.StoryObjective;
import necesse.engine.util.GameRandom;
import necesse.engine.window.GameWindow;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormStoryObjectiveComponent;
import necesse.gfx.forms.components.containerSlot.FormContainerMaterialSlot;
import necesse.gfx.forms.components.containerSlot.FormContainerSlot;
import necesse.gfx.forms.components.lists.FormIngredientRecipeList;
import necesse.gfx.gameFont.FontOptions;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.mob.ElderContainer;
import necesse.inventory.recipe.Recipes;

public class ElderContainerForm<T extends ElderContainer> extends ShopContainerForm<T> {
   public static int[] tipIndexes = new int[]{1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
   public DialogueForm objectiveForm;
   public StoryObjective currentObjective;
   public DialogueForm tipsForm;
   public DialogueForm helpForm;
   public ArrayList<ElderContainerForm<T>.HelpDialogueOption> helpForms;
   public int currentTipIndex;
   public FormContainerSlot ingredientSlot;
   public FormIngredientRecipeList ingredientList;
   private int itemID;

   public ElderContainerForm(Client client, T container) {
      super(client, container);
      this.objectiveForm = this.addComponent(
         new DialogueForm("storyobjectives", this.width, this.minHeight, this.maxHeight, true, container.humanShop, true, container.romanceLevel, null)
      );
      this.tipsForm = this.addComponent(
         new DialogueForm("eldertips", this.width, this.minHeight, this.maxHeight, true, container.humanShop, true, container.romanceLevel, null)
      );
      this.helpForms = new ArrayList<>();
      this.helpForm = this.addComponent(
         new DialogueForm(
            "elderhelp",
            this.width,
            this.minHeight,
            this.maxHeight,
            true,
            container.humanShop,
            true,
            container.romanceLevel,
            new LocalMessage("ui", "elderpicktopic")
         )
      );
      DialogueForm crafting = new DialogueForm("crafting", this.width, this.minHeight, this.maxHeight, true);
      crafting.reset(
         container.humanShop,
         false,
         -1,
         (content, flow) -> {
            Runnable chatBubble = DialogueForm.startChatBubble(content, flow);
            DialogueForm.addText(content, flow, new LocalMessage("ui", "eldercrafting"), new FontOptions(20), true);
            flow.next(5);
            DialogueForm.addText(content, flow, new LocalMessage("ui", "eldercraftingtip"), true);
            chatBubble.run();
            flow.next(10);
            int craftingNext = flow.next(100);
            int craftingWidth = content.getWidth() - 80 - 8;
            content.addComponent(
               this.ingredientSlot = new FormContainerMaterialSlot(
                  client, container, container.INGREDIENT_SLOT, 4 + craftingWidth + (content.getWidth() - craftingWidth) / 2 - 40, craftingNext + 15
               )
            );
            content.addComponent(this.ingredientList = new FormIngredientRecipeList(4, craftingNext, craftingWidth, 100, client));
         }
      );
      crafting.addDialogueOption(new LocalMessage("ui", "backbutton"), () -> {
         container.setInCraftingForm.runAndSend(false);
         this.makeCurrent(this.helpForm);
      });
      this.addHelpForm(new LocalMessage("ui", "eldercrafting"), crafting, () -> container.setInCraftingForm.runAndSend(true));
      this.addHelpForm(
         new LocalMessage("ui", "eldertrading"),
         this.simpleDialogueForm(
            "trading", this.width, this.minHeight, this.maxHeight, new LocalMessage("ui", "eldertrading"), new LocalMessage("ui", "eldertradingtipnew")
         )
      );
      this.addHelpForm(
         new LocalMessage("ui", "elderobjects"),
         this.simpleDialogueForm(
            "objects", this.width, this.minHeight, this.maxHeight, new LocalMessage("ui", "elderobjects"), new LocalMessage("ui", "elderobjectstip")
         )
      );
      this.addHelpForm(
         new LocalMessage("ui", "eldertiles"),
         this.simpleDialogueForm(
            "tiles", this.width, this.minHeight, this.maxHeight, new LocalMessage("ui", "eldertiles"), new LocalMessage("ui", "eldertilestip")
         )
      );
      this.addHelpForm(
         new LocalMessage("ui", "eldertools"),
         this.simpleDialogueForm(
            "tools", this.width, this.minHeight, this.maxHeight, new LocalMessage("ui", "eldertools"), new LocalMessage("ui", "eldertoolstip")
         )
      );
      this.addHelpForm(
         new LocalMessage("ui", "elderweapons"),
         this.simpleDialogueForm(
            "weapons", this.width, this.minHeight, this.maxHeight, new LocalMessage("ui", "elderweapons"), new LocalMessage("ui", "elderweaponstip")
         )
      );
      this.addHelpForm(
         new LocalMessage("ui", "elderarmor"),
         this.simpleDialogueForm(
            "armor", this.width, this.minHeight, this.maxHeight, new LocalMessage("ui", "elderarmor"), new LocalMessage("ui", "elderarmortip")
         )
      );
      this.addHelpForm(
         new LocalMessage("ui", "eldertrinkets"),
         this.simpleDialogueForm(
            "trinkets", this.width, this.minHeight, this.maxHeight, new LocalMessage("ui", "eldertrinkets"), new LocalMessage("ui", "eldertrinketstip")
         )
      );
      this.addHelpForm(
         new LocalMessage("ui", "elderenchants"),
         this.simpleDialogueForm(
            "enchants", this.width, this.minHeight, this.maxHeight, new LocalMessage("ui", "elderenchants"), new LocalMessage("ui", "elderenchantstip")
         )
      );
      this.addHelpForm(
         new LocalMessage("ui", "elderpotions"),
         this.simpleDialogueForm(
            "potions", this.width, this.minHeight, this.maxHeight, new LocalMessage("ui", "elderpotions"), new LocalMessage("ui", "elderpotionstip")
         )
      );
      this.addHelpForm(
         new LocalMessage("ui", "eldersettlements"),
         this.simpleDialogueForm(
            "settlements",
            this.width,
            this.minHeight,
            this.maxHeight,
            new LocalMessage("ui", "eldersettlements"),
            new LocalMessage("ui", "eldersettlementstip")
         )
      );
      this.helpForms.sort(Comparator.comparing(c -> c.text.translate()));
      this.helpForms.forEach(ElderContainerForm.HelpDialogueOption::addDialogueOption);
      this.helpForm.addDialogueOption(new LocalMessage("ui", "backbutton"), () -> this.makeCurrent(this.dialogueForm));
      this.updateStoryForm();
      this.updateStoryFormActive();
   }

   public void updateStoryForm() {
      this.currentObjective = this.client.storyManager.getNextClaimableObjective();
      if (this.currentObjective != null) {
         this.objectiveForm.reset(this.container.humanShop, true, this.container.romanceLevel, (content, flow) -> {
            Runnable chatBubble = DialogueForm.startChatBubble(content, flow);
            DialogueForm.addText(content, flow, new LocalMessage("ui", "elderobjectiveintro"), true);
            chatBubble.run();
            flow.next(10);
            FormStoryObjectiveComponent component = content.addComponent(new FormStoryObjectiveComponent(content.getWidth() - 40, this.currentObjective, true));
            component.setX(10);
            flow.nextY(component);
         });
         this.objectiveForm
            .addDialogueOption(
               new LocalMessage("ui", "elderobjectiveclaim"), () -> this.container.claimStoryObjectiveAction.runAndSend(this.currentObjective.getID())
            );
      } else {
         this.objectiveForm.addDialogueOption(new LocalMessage("ui", "backbutton"), () -> this.makeCurrent(this.dialogueForm));
      }
   }

   public void updateStoryFormActive() {
      if (this.currentObjective != null && (!this.currentObjective.isCompleted() || this.currentObjective.isClaimed())) {
         this.currentObjective = null;
         this.updateStoryForm();
      }

      if (this.currentObjective != null) {
         this.makeCurrent(this.objectiveForm);
      } else if (this.isCurrent(this.objectiveForm)) {
         this.makeCurrent(this.dialogueForm);
      }
   }

   private DialogueForm simpleDialogueForm(String name, int width, int minHeight, int maxHeight, GameMessage header, GameMessage text) {
      DialogueForm dialogueForm = new DialogueForm(name, width, minHeight, maxHeight, true);
      dialogueForm.reset(this.container.humanShop, false, -1, (content, flow) -> {
         Runnable chatBubble = DialogueForm.startChatBubble(content, flow);
         DialogueForm.addText(content, flow, header, new FontOptions(20), true);
         flow.next(5);
         DialogueForm.addText(content, flow, text, true);
         chatBubble.run();
      });
      dialogueForm.addDialogueOption(new LocalMessage("ui", "backbutton"), () -> this.makeCurrent(this.helpForm));
      return dialogueForm;
   }

   @Override
   protected void addQuestsDialogueOption() {
      super.addQuestsDialogueOption();
      this.dialogueForm.addDialogueOption(new LocalMessage("ui", "eldertips"), () -> {
         this.currentTipIndex = GameRandom.globalRandom.nextInt(tipIndexes.length);
         this.updateTip();
         this.makeCurrent(this.tipsForm);
      });
      this.dialogueForm.addDialogueOption(new LocalMessage("ui", "elderhelpbutton"), () -> this.makeCurrent(this.helpForm));
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      InventoryItem item = this.ingredientSlot.getContainerSlot().getItem();
      int nextItemID = item == null ? -1 : item.item.getID();
      if (this.itemID != nextItemID) {
         this.itemID = nextItemID;
         if (this.itemID == -1) {
            this.ingredientList.setRecipes(null);
         } else {
            this.ingredientList.setRecipes(Recipes.getRecipesFromResultAndIngredient(nextItemID));
         }
      }

      this.updateStoryFormActive();
      super.draw(tickManager, perspective, renderBox);
   }

   public void updateTip() {
      int tipIndex = Math.floorMod(this.currentTipIndex, tipIndexes.length);
      int tipNum = tipIndexes[tipIndex];
      this.tipsForm.reset(this.container.humanShop, true, this.container.romanceLevel, new LocalMessage("ui", "eldertip" + tipNum));
      this.tipsForm.addDialogueOption(new LocalMessage("ui", "eldernexttip"), () -> {
         this.currentTipIndex++;
         this.updateTip();
      });
      this.tipsForm.addDialogueOption(new LocalMessage("ui", "elderprevtip"), () -> {
         this.currentTipIndex--;
         this.updateTip();
      });
      this.tipsForm.addDialogueOption(new LocalMessage("ui", "backbutton"), () -> this.makeCurrent(this.dialogueForm));
      ContainerComponent.setPosFocus(this.tipsForm);
   }

   @Override
   public void onWindowResized(GameWindow window) {
      super.onWindowResized(window);
      ContainerComponent.setPosFocus(this.tipsForm);
      ContainerComponent.setPosFocus(this.helpForm);
      ContainerComponent.setPosFocus(this.objectiveForm);
      this.helpForms.forEach(h -> ContainerComponent.setPosFocus(h.form));
   }

   private void addHelpForm(GameMessage buttonText, Form helpForm) {
      this.addHelpForm(buttonText, helpForm, null);
   }

   private void addHelpForm(GameMessage buttonText, Form helpForm, Runnable buttonPressed) {
      this.addComponent(helpForm);
      this.helpForms.add(new ElderContainerForm.HelpDialogueOption(buttonText, buttonPressed, helpForm));
   }

   private class HelpDialogueOption {
      public final GameMessage text;
      public final Runnable entered;
      public final Form form;

      public HelpDialogueOption(GameMessage text, Runnable entered, Form form) {
         this.text = text;
         this.entered = entered;
         this.form = form;
      }

      public void addDialogueOption() {
         ElderContainerForm.this.helpForm.addDialogueOption(this.text, () -> {
            if (this.entered != null) {
               this.entered.run();
            }

            ElderContainerForm.this.makeCurrent(this.form);
         });
      }
   }
}
