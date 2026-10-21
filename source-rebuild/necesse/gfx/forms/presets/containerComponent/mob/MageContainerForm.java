package necesse.gfx.forms.presets.containerComponent.mob;

import java.awt.Rectangle;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Language;
import necesse.engine.localization.Localization;
import necesse.engine.localization.LocalizationChangeListener;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.engine.window.GameWindow;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.humanShop.MageHumanMob;
import necesse.gfx.fairType.FairType;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormFairTypeLabel;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormItemPreview;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.containerSlot.FormContainerEnchantSlot;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.components.localComponents.FormLocalTextButton;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.container.mob.MageContainer;
import necesse.level.maps.levelData.settlementData.settler.Settler;

public class MageContainerForm<T extends MageContainer> extends ShopContainerForm<T> {
   public Form enchantForm;
   public FormLabel costText;
   public FormLocalTextButton enchantButton;
   public FormLocalLabel costLabel;
   public FormItemPreview preview;

   public MageContainerForm(Client client, T container) {
      super(client, container);
      FormFlow heightFlow = new FormFlow(5);
      this.enchantForm = this.addComponent(new Form("enchant", this.width, this.minHeight), (form, active) -> container.setIsEnchanting.runAndSend(active));
      this.enchantForm
         .addComponent(new FormLocalTextButton("ui", "backbutton", this.enchantForm.getWidth() - 104, 4, 100, FormInputSize.SIZE_20, ButtonColor.BASE))
         .onClicked(e -> this.makeCurrent(this.dialogueForm));
      this.enchantForm.addComponent(new FormLocalLabel("ui", "mageenchant", new FontOptions(20), -1, 4, heightFlow.next(40)));
      int enchantSlotY = heightFlow.next(50);
      this.enchantForm.addComponent(new FormContainerEnchantSlot(client, container, container.ENCHANT_SLOT, 40, enchantSlotY));
      this.enchantButton = this.enchantForm
         .addComponent(new FormLocalTextButton("ui", "mageconfirm", 90, enchantSlotY + 10, 150, FormInputSize.SIZE_24, ButtonColor.BASE));
      this.enchantButton.onClicked(e -> container.enchantButton.runAndSend());
      this.costLabel = this.enchantForm.addComponent(new FormLocalLabel("ui", "magecost", new FontOptions(16), -1, 260, enchantSlotY - 4));
      this.preview = this.enchantForm.addComponent(new FormItemPreview(250, enchantSlotY + 10, "coin"));
      this.costText = this.enchantForm
         .addComponent(new FormLabel("x " + container.getEnchantCost(), new FontOptions(16), -1, this.preview.getX() + 30, enchantSlotY + 20));
      this.enchantForm
         .addComponent(
            heightFlow.nextY(
               new FormFairTypeLabel(new LocalMessage("ui", "mageenchanttip"), this.enchantForm.getWidth() / 2, 0)
                  .setFontOptions(new FontOptions(16))
                  .setTextAlign(FairType.TextAlign.CENTER)
                  .setMaxWidth(this.enchantForm.getWidth() - 20),
               10
            )
         );
      if (container.mageMob.isSettler()) {
         float weight = MageContainer.getEnchantmentLotteryWeight(container.settlerQualityOfLife, container.settlerHappiness);
         int weightPercent = (int)(weight * 100.0F);
         GameMessageBuilder happinessDescription = new GameMessageBuilder()
            .append(Settler.getQualityOfLifeAndMoodMessage(container.settlerQualityOfLife, container.settlerHappiness, null, false, false))
            .append(" (")
            .append(weightPercent >= 0 ? "+" : "")
            .append(Integer.toString(weightPercent))
            .append("%)");
         this.enchantForm
            .addComponent(
               heightFlow.nextY(
                  new FormFairTypeLabel(happinessDescription, this.enchantForm.getWidth() / 2, 0)
                     .setFontOptions(new FontOptions(16))
                     .setTextAlign(FairType.TextAlign.CENTER)
                     .setMaxWidth(this.enchantForm.getWidth() - 20),
                  5
               )
            );
         FormContentIconButton helpIcon = this.enchantForm
            .addComponent(
               new FormContentIconButton(
                  this.enchantForm.getWidth() / 2 - 10,
                  heightFlow.next(30),
                  FormInputSize.SIZE_20,
                  ButtonColor.BASE,
                  this.getInterfaceStyle().button_help_20,
                  new LocalMessage("ui", "mageenchantbiastip2")
               )
            );
         helpIcon.handleClicksIfNoEventHandlers = true;
      }

      this.enchantForm.setHeight(heightFlow.next());
      this.updateEnchantActive();
   }

   @Override
   protected void init() {
      super.init();
      Localization.addListener(new LocalizationChangeListener() {
         @Override
         public void onChange(Language language) {
            MageContainerForm.this.preview.setX(MageContainerForm.this.costLabel.getX() + MageContainerForm.this.costLabel.getBoundingBox().width);
            MageContainerForm.this.costText.setX(MageContainerForm.this.preview.getX() + 30);
         }

         @Override
         public boolean isDisposed() {
            return MageContainerForm.this.isDisposed();
         }
      });
   }

   @Override
   protected void addShopDialogueOptions() {
      super.addShopDialogueOptions();
      if (this.container.humanShop instanceof MageHumanMob) {
         boolean valid = false;
         if (this.container.sellingItems != null && !this.container.sellingItems.isEmpty()) {
            valid = true;
         } else if (this.container.buyingItems != null && !this.container.buyingItems.isEmpty()) {
            valid = true;
         }

         if (valid) {
            this.dialogueForm.addDialogueOption(new LocalMessage("ui", "magewantenchant"), () -> this.makeCurrent(this.enchantForm));
         }
      }
   }

   private void updateEnchantActive() {
      this.costText.setText("x " + this.container.getEnchantCost());
      this.enchantButton.setActive(this.container.canEnchant());
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      if (this.isCurrent(this.enchantForm)) {
         this.updateEnchantActive();
      }

      super.draw(tickManager, perspective, renderBox);
   }

   @Override
   public void onWindowResized(GameWindow window) {
      super.onWindowResized(window);
      ContainerComponent.setPosFocus(this.enchantForm);
   }
}
