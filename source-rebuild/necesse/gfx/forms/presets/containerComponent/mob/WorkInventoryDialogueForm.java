package necesse.gfx.forms.presets.containerComponent.mob;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.fairType.FairItemGlyph;
import necesse.gfx.fairType.FairType;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormComponentList;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormDialogueOption;
import necesse.gfx.forms.components.FormFairTypeLabel;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.position.FormPositionDynamic;
import necesse.gfx.forms.position.FormRelativePosition;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.mob.ShopContainer;

public class WorkInventoryDialogueForm extends FormComponentList {
   public final ShopContainerForm<?> shopContainerForm;
   public final DialogueForm dialogueForm;
   public Form inventoryForm;
   public FormContentIconButton takeItemsButton;
   public FormContentBox inventoryContent;

   public WorkInventoryDialogueForm(ShopContainerForm<?> shopContainerForm, DialogueForm dialogueForm) {
      this.shopContainerForm = shopContainerForm;
      this.dialogueForm = this.addComponent(dialogueForm);
      this.inventoryForm = this.addComponent(new Form(100, 100));
      this.inventoryForm
         .setPosition(new FormRelativePosition(dialogueForm, () -> -this.inventoryForm.getWidth() - this.getInterfaceStyle().formSpacing, () -> 0));
      this.takeItemsButton = this.inventoryForm
         .addComponent(
            new FormContentIconButton(
               4, 0, 0, FormInputSize.SIZE_32, ButtonColor.BASE, this.getInterfaceStyle().container_loot_all, new LocalMessage("ui", "settlerreceiveitems")
            )
         );
      this.takeItemsButton.setPosition(new FormPositionDynamic(() -> 4, () -> this.inventoryForm.getHeight() - 4 - 32));
      this.takeItemsButton.onClicked(e -> shopContainerForm.getContainer().workItemsAction.runAndSend(ShopContainer.WorkItemsAction.RECEIVE));
      this.inventoryContent = this.inventoryForm
         .addComponent(new FormContentBox(0, 0, this.inventoryForm.getWidth(), this.inventoryForm.getHeight() - 32 - 8) {
            @Override
            public boolean hasScrollbarX() {
               return super.hasScrollbarX();
            }
         });
   }

   @Override
   protected void init() {
      super.init();
      this.updateInventoryForm();
   }

   public void updateInventoryForm() {
      this.inventoryContent.clearComponents();
      ShopContainer container = this.shopContainerForm.getContainer();
      ArrayList<InventoryItem> inventory = container.humanShop.workInventory;
      if (!inventory.isEmpty() && container.hasSettlerAccess) {
         this.inventoryForm.setHidden(false);
         this.inventoryForm.setWidth(145);
         FormFlow flow = new FormFlow(5);
         FontOptions fontOptions = new FontOptions(16);
         this.inventoryContent
            .addComponent(flow.nextY(new FormLocalLabel("ui", "settlerinventory", fontOptions, 0, this.inventoryContent.getWidth() / 2, 0), 10));
         boolean secondColumn = false;

         for (InventoryItem item : inventory) {
            FormFairTypeLabel label = getItemLabel(item);
            if (item.getAmount() >= 1000) {
               if (secondColumn) {
                  flow.next(24);
               }

               secondColumn = false;
            }

            this.inventoryContent.addComponent(label);
            label.setPosition(secondColumn ? this.inventoryContent.getWidth() / 2 + 5 : 5, flow.next());
            if (item.getAmount() >= 1000) {
               secondColumn = false;
               flow.next(24);
            } else {
               if (secondColumn) {
                  flow.next(24);
               }

               secondColumn = !secondColumn;
            }
         }

         if (secondColumn) {
            flow.next(24);
         }

         flow.next(5);
         this.inventoryContent.setContentBox(new Rectangle(this.inventoryContent.getWidth(), flow.next()));
         this.inventoryContent.setWidth(this.inventoryForm.getWidth());
         this.updateInventoryHeight();
         if (this.inventoryContent.hasScrollbarY()) {
            this.inventoryForm.setWidth(this.inventoryForm.getWidth() + this.inventoryContent.getScrollBarWidth() + 10);
            this.inventoryContent.setWidth(this.inventoryForm.getWidth());
         }

         this.takeItemsButton.setWidth(this.inventoryForm.getWidth() - 8);
      } else {
         this.inventoryForm.setHidden(true);
      }
   }

   protected static FormFairTypeLabel getItemLabel(InventoryItem item) {
      FontOptions fontOptions = new FontOptions(16);
      FairType fairType = new FairType();
      fairType.append(new FairItemGlyph(fontOptions.getSize(), item));
      fairType.append(fontOptions, "x" + item.getAmount());
      FormFairTypeLabel label = new FormFairTypeLabel("", 0, 0);
      label.setCustomFairType(fairType);
      return label;
   }

   public void reset(BiConsumer<FormContentBox, FormFlow> adder) {
      this.dialogueForm.reset(adder);
   }

   public void reset(HumanMob humanMob, boolean showPersonality, int romanceLevel, BiConsumer<FormContentBox, FormFlow> adder) {
      this.dialogueForm.reset(humanMob, showPersonality, romanceLevel, adder);
   }

   public void reset(HumanMob humanMob, boolean showPersonality, int romanceLevel, GameMessage intro) {
      this.dialogueForm.reset(humanMob, showPersonality, romanceLevel, intro);
   }

   public void reset(GameMessage header, BiConsumer<FormContentBox, FormFlow> adder) {
      this.dialogueForm.reset(header, adder);
   }

   public void reset(GameMessage header) {
      this.dialogueForm.reset(header);
   }

   public void reset(GameMessage header, GameMessage intro, BiConsumer<FormContentBox, FormFlow> adder) {
      this.dialogueForm.reset(header, intro, adder);
   }

   public void reset(GameMessage header, GameMessage intro) {
      this.dialogueForm.reset(header, intro);
   }

   public FormDialogueOption addDialogueOption(GameMessage message, Runnable onClick) {
      return this.dialogueForm.addDialogueOption(message, onClick);
   }

   public FormDialogueOption addDialogueOption(int optionNumber, GameMessage message, Runnable onClick) {
      return this.dialogueForm.addDialogueOption(optionNumber, message, onClick);
   }

   public FormDialogueOption addDialogueOption(FormDialogueOption dialogueOption) {
      return this.dialogueForm.addDialogueOption(dialogueOption);
   }

   public void updateInventoryHeight() {
      int contentHeight = this.inventoryContent.getContentBox().height;
      int formHeight = Math.min(contentHeight + 32 + 8, this.dialogueForm.getHeight());
      if (this.inventoryForm.getHeight() != formHeight) {
         this.inventoryForm.setHeight(formHeight);
         this.inventoryContent.setHeight(formHeight - 32 - 8);
      }
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      this.updateInventoryHeight();
      super.draw(tickManager, perspective, renderBox);
   }
}
