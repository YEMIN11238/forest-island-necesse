package necesse.gfx.forms.components.lists;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameBackground;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.inventory.InventoryItem;

public abstract class FormItemList extends FormGeneralGridList<FormItemList.ItemElement> {
   protected LinkedList<InventoryItem> allItems;
   protected Predicate<InventoryItem> filter;
   public FormItemList.UpdateMode updateMode;
   private Thread filterUpdater;
   protected boolean sorted = true;

   public FormItemList(int x, int y, int width, int height, FormItemList.UpdateMode updateMode) {
      super(x, y, width, height, 36, 36);
      this.filter = null;
      this.updateMode = updateMode;
      this.reset();
   }

   @Override
   public void reset() {
      this.allItems = new LinkedList<>();
      this.resetScroll();
   }

   public void populateIfNotAlready() {
      if (this.allItems.isEmpty()) {
         this.addAllItems(this.allItems);
         this.setFilter(this.filter, false);
         this.resetScroll();
      }
   }

   public abstract void addAllItems(List<InventoryItem> var1);

   public void setFilter(Predicate<InventoryItem> filter) {
      this.setFilter(filter, true);
   }

   private void setFilter(Predicate<InventoryItem> filter, boolean limitScroll) {
      this.filter = filter;
      this.updateList(limitScroll);
   }

   public void setSorted(boolean sorted) {
      if (this.sorted != sorted) {
         this.sorted = sorted;
         this.updateList(true);
      }
   }

   protected void updateList(final boolean limitScroll) {
      if (this.filterUpdater != null) {
         this.filterUpdater.interrupt();
         this.filterUpdater = null;
      }

      final Predicate<InventoryItem> finalFilter = this.filter;
      if (this.updateMode == FormItemList.UpdateMode.CONCURRENT_CONTINUOUS) {
         this.elements = new ArrayList<>();
         final int lastScroll = this.scroll;
         this.filterUpdater = new Thread("item-list-filter-continuous") {
            @Override
            public void run() {
               super.run();

               for (InventoryItem invItem : FormItemList.this.allItems) {
                  if (this.isInterrupted()) {
                     return;
                  }

                  if (finalFilter == null || finalFilter.test(invItem)) {
                     if (FormItemList.this.sorted) {
                        synchronized (FormItemList.this) {
                           GameUtils.insertSortedList(FormItemList.this.elements, FormItemList.this.new ItemElement(invItem), Comparator.comparing(i -> i.item));
                        }
                     } else {
                        synchronized (FormItemList.this) {
                           FormItemList.this.elements.add(FormItemList.this.new ItemElement(invItem));
                        }
                     }
                  }

                  if (limitScroll) {
                     FormItemList.this.scroll = lastScroll;
                     FormItemList.this.limitMaxScroll();
                  }
               }
            }
         };
         this.filterUpdater.start();
      } else if (this.updateMode == FormItemList.UpdateMode.CONCURRENT_FULL) {
         this.filterUpdater = new Thread("item-list-filter-wait") {
            @Override
            public void run() {
               super.run();
               ArrayList<FormItemList.ItemElement> list = new ArrayList<>(FormItemList.this.allItems.size());

               for (InventoryItem invItem : FormItemList.this.allItems) {
                  if (this.isInterrupted()) {
                     return;
                  }

                  if (finalFilter == null || finalFilter.test(invItem)) {
                     list.add(FormItemList.this.new ItemElement(invItem));
                  }
               }

               synchronized (FormItemList.this) {
                  if (!this.isInterrupted()) {
                     if (FormItemList.this.sorted) {
                        list.sort(Comparator.comparing(i -> i.item));
                     }

                     FormItemList.this.elements = new ArrayList<>();
                     FormItemList.this.elements.addAll(list);
                     if (limitScroll) {
                        FormItemList.this.limitMaxScroll();
                     }
                  }
               }
            }
         };
         this.filterUpdater.start();
      } else {
         synchronized (this) {
            this.elements = new ArrayList<>();

            for (InventoryItem invItem : this.allItems) {
               if (finalFilter == null || finalFilter.test(invItem)) {
                  if (this.sorted) {
                     GameUtils.insertSortedList(this.elements, new FormItemList.ItemElement(invItem), Comparator.comparing(i -> i.item));
                  } else {
                     this.elements.add(new FormItemList.ItemElement(invItem));
                  }
               }
            }

            if (this.sorted) {
               this.elements.sort(Comparator.comparing(i -> i.item));
            }

            if (limitScroll) {
               this.limitMaxScroll();
            }
         }
      }
   }

   public abstract void onItemClicked(InventoryItem var1, InputEvent var2);

   public void addTooltips(InventoryItem item, PlayerMob perspective) {
      GameTooltipManager.addTooltip(item.getTooltip(perspective, new GameBlackboard()), GameBackground.getItemTooltipBackground(), TooltipLocation.FORM_FOCUS);
   }

   public class ItemElement extends FormListGridElement<FormItemList> {
      public final InventoryItem item;

      public ItemElement(InventoryItem item) {
         this.item = item;
      }

      protected void draw(FormItemList parent, TickManager tickManager, PlayerMob perspective, int elementIndex) {
         if (this.isMouseOver(parent)) {
            FormItemList.this.getInterfaceStyle()
               .inventoryslot_small
               .highlighted
               .initDraw()
               .color(FormItemList.this.getInterfaceStyle().highlightElementColor)
               .draw(2, 2);
            FormItemList.this.addTooltips(this.item, perspective);
         } else {
            FormItemList.this.getInterfaceStyle()
               .inventoryslot_small
               .active
               .initDraw()
               .color(FormItemList.this.getInterfaceStyle().activeElementColor)
               .draw(2, 2);
         }

         this.item.draw(perspective, 2, 2);
      }

      protected void onClick(FormItemList parent, int elementIndex, InputEvent event, PlayerMob perspective) {
         FormItemList.this.onItemClicked(this.item, event);
      }

      protected void onControllerEvent(FormItemList parent, int elementIndex, ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
         if (event.getState() == ControllerInput.MENU_SELECT) {
            FormItemList.this.onItemClicked(this.item, InputEvent.ControllerButtonEvent(event, tickManager));
            event.use();
         }
      }

      @Override
      public void drawControllerFocus(ControllerFocus current) {
         super.drawControllerFocus(current);
         GameTooltipManager.addControllerGlyph(Localization.translate("ui", "selectbutton"), ControllerInput.MENU_SELECT);
      }
   }

   public enum UpdateMode {
      CONCURRENT_CONTINUOUS,
      CONCURRENT_FULL,
      WAIT_FULl;
   }
}
