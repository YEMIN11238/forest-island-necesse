package necesse.gfx.forms.presets.containerComponent.mob;

import java.util.Collection;
import java.util.LinkedHashMap;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.window.GameWindow;
import necesse.gfx.fairType.FairItemGlyph;
import necesse.gfx.fairType.FairType;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.components.FormBuyingShopComponent;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormFairTypeLabel;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormSellingShopComponent;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.gameFont.FontOptions;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.events.FullShopStockUpdateEvent;
import necesse.inventory.container.events.ShopWealthUpdateEvent;
import necesse.inventory.container.events.SingleShopStockUpdateEvent;
import necesse.inventory.container.mob.NetworkBuyingShopItem;
import necesse.inventory.container.mob.NetworkSellingShopItem;
import necesse.inventory.container.mob.ShopContainer;

public abstract class ShopContainerShopForm extends DialogueForm {
   public ShopContainer container;
   protected LinkedHashMap<Integer, FormSellingShopComponent> sellingItems = new LinkedHashMap<>();
   protected LinkedHashMap<Integer, FormBuyingShopComponent> buyingItems = new LinkedHashMap<>();
   protected FormFairTypeLabel wealthLabel;

   public ShopContainerShopForm(ShopContainer container, int width, int minHeight, int maxHeight) {
      super("shopForm", width, minHeight, maxHeight, false);
      this.container = container;
      this.updateFullContent();
   }

   @Override
   protected void init() {
      super.init();
      this.container.onEvent(ShopWealthUpdateEvent.class, e -> {
         this.updateWealthLabel();

         for (FormBuyingShopComponent comp : this.buyingItems.values()) {
            comp.updateCanCraft();
         }
      });
      this.container.onEvent(FullShopStockUpdateEvent.class, e -> {
         this.updateFullContent();
         this.updateWealthLabel();

         for (FormSellingShopComponent comp : this.sellingItems.values()) {
            comp.updateCanCraft();
         }

         for (FormBuyingShopComponent comp : this.buyingItems.values()) {
            comp.updateCanCraft();
         }
      });
      this.container.onEvent(SingleShopStockUpdateEvent.class, e -> {
         this.updateWealthLabel();
         boolean foundComponent = false;
         FormSellingShopComponent sellingItem = this.sellingItems.get(e.shopItemID);
         if (sellingItem != null) {
            foundComponent = true;
            sellingItem.updateCanCraft();
         }

         FormBuyingShopComponent buyingItem = this.buyingItems.get(e.shopItemID);
         if (buyingItem != null) {
            foundComponent = true;
         }

         for (FormBuyingShopComponent comp : this.buyingItems.values()) {
            comp.updateCanCraft();
         }

         if (!foundComponent) {
            this.updateFullContent();
         }
      });
   }

   protected void updateFullContent() {
      this.sellingItems = new LinkedHashMap<>();
      this.buyingItems = new LinkedHashMap<>();
      this.reset(
         this.container.humanShop,
         true,
         this.container.romanceLevel,
         (content, flow) -> {
            if (this.container.sellingItems != null) {
               for (NetworkSellingShopItem sellingItem : this.container.sellingItems.values()) {
                  FormSellingShopComponent comp = content.addComponent(new FormSellingShopComponent(0, 0, this.container, sellingItem));
                  this.sellingItems.put(sellingItem.shopItemID, comp);
               }
            }

            if (this.container.buyingItems != null) {
               for (NetworkBuyingShopItem buyingItem : this.container.buyingItems.values()) {
                  FormBuyingShopComponent comp = content.addComponent(new FormBuyingShopComponent(0, 0, this.container, buyingItem));
                  this.buyingItems.put(buyingItem.shopItemID, comp);
               }
            }

            if (!this.sellingItems.isEmpty()) {
               addText(content, flow, new LocalMessage("ui", "shopsellingitems"), false);
               this.updateItemPositions(content, flow, this.sellingItems.values());
            }

            if (!this.buyingItems.isEmpty()) {
               addText(content, flow, new LocalMessage("ui", "shopbuyingitems"), false);
               this.updateItemPositions(content, flow, this.buyingItems.values());
            }

            if (this.container.shopWealth >= 0) {
               flow.next(4);
               this.wealthLabel = content.addComponent(
                  new FormFairTypeLabel(new StaticMessage(""), new FontOptions(16), FairType.TextAlign.LEFT, 15, flow.next(16))
               );
               this.updateWealthLabel();
            }
         }
      );
      this.addDialogueOption(new LocalMessage("ui", "backbutton"), this::onBackPressed);
   }

   protected void updateItemPositions(FormContentBox content, FormFlow flow, Collection<? extends FormPositionContainer> component) {
      int padding = 4;
      int compWidth = 40;
      int compHeight = 40;
      flow.next(padding);
      int currentX = 10;
      int currentY = flow.next(compHeight + padding);

      for (FormPositionContainer comp : component) {
         if (currentX + compWidth + 4 > content.getWidth() - content.getScrollBarWidth() - 20) {
            currentX = 10;
            currentY = flow.next(compHeight + padding);
         }

         comp.setPosition(currentX, currentY);
         currentX += compWidth + padding;
      }
   }

   public void updateWealthLabel() {
      if (this.wealthLabel != null) {
         FairType fairType = new FairType();
         FontOptions fontOptions = this.wealthLabel.getFontOptions();
         fairType.append(fontOptions, Localization.translate("ui", "shopwealth"));
         fairType.append(new FairItemGlyph(fontOptions.getSize(), new InventoryItem("coin")).dontShowTooltip());
         fairType.append(fontOptions, Integer.toString(this.container.shopWealth));
         this.wealthLabel.setCustomFairType(fairType);
      }
   }

   public abstract void onBackPressed();

   @Override
   public void onWindowResized(GameWindow window) {
      super.onWindowResized(window);
      ContainerComponent.setPosFocus(this);
   }
}
