package necesse.entity.mobs.friendly.human.humanWorkSetting;

import java.util.function.Supplier;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.presets.containerComponent.mob.EquipmentFiltersForm;
import necesse.gfx.forms.presets.containerComponent.mob.EquipmentForm;
import necesse.gfx.forms.presets.containerComponent.mob.ShopContainerForm;
import necesse.gfx.forms.presets.containerComponent.mob.ShopEquipmentForm;
import necesse.inventory.Inventory;
import necesse.inventory.container.customAction.BooleanCustomAction;
import necesse.inventory.container.mob.ShopContainer;
import necesse.inventory.container.settlement.events.SettlementSettlerEquipmentFilterChangedEvent;
import necesse.inventory.container.slots.ArmorContainerSlot;
import necesse.inventory.container.slots.SettlerWeaponContainerSlot;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.itemFilter.ItemCategoriesFilter;
import necesse.inventory.itemFilter.ItemCategoriesFilterChange;
import necesse.level.maps.levelData.settlementData.LevelSettler;

public class HumanEquipmentFilterSetting extends HumanWorkSetting<Object> {
   public final HumanMob mob;

   public HumanEquipmentFilterSetting(HumanMob mob) {
      this.mob = mob;
   }

   @Override
   public void writeContainerPacket(ServerClient client, PacketWriter writer) {
   }

   @Override
   public Object readContainer(ShopContainer container, PacketReader reader) {
      return null;
   }

   @Override
   public HumanWorkContainerHandler<Object> setupHandler(ShopContainer container, ShopContainer.ContainerWorkSetting<Object> setting) {
      return new HumanEquipmentFilterSetting.ContainerHandler(container, setting);
   }

   public class ContainerHandler extends HumanWorkContainerHandler<Object> {
      public final BooleanCustomAction setIsInEquipment;
      public boolean isInEquipment;
      public final BooleanCustomAction setSelfManageEquipment;
      public final BooleanCustomAction setIsInFilters;
      public boolean isInFilters;
      public final SetEquipmentFilterSettingAction setEquipmentFilterSetting;
      public int EQUIPMENT_HEAD_SLOT = -1;
      public int EQUIPMENT_CHEST_SLOT = -1;
      public int EQUIPMENT_FEET_SLOT = -1;
      public int EQUIPMENT_COSM_HEAD_SLOT = -1;
      public int EQUIPMENT_COSM_CHEST_SLOT = -1;
      public int EQUIPMENT_COSM_FEET_SLOT = -1;
      public int EQUIPMENT_WEAPON_SLOT = -1;
      public EquipmentForm equipmentForm;
      public boolean preferArmorSets;
      public EquipmentFiltersForm filtersForm;

      public ContainerHandler(final ShopContainer container, ShopContainer.ContainerWorkSetting<Object> setting) {
         super(container, setting);
         if (container.hasSettlerAccess) {
            Inventory equipmentInventory = HumanEquipmentFilterSetting.this.mob.getInventory();
            this.EQUIPMENT_HEAD_SLOT = container.addSlot(new ArmorContainerSlot(equipmentInventory, 0, ArmorItem.ArmorType.HEAD));
            this.EQUIPMENT_CHEST_SLOT = container.addSlot(new ArmorContainerSlot(equipmentInventory, 1, ArmorItem.ArmorType.CHEST));
            this.EQUIPMENT_FEET_SLOT = container.addSlot(new ArmorContainerSlot(equipmentInventory, 2, ArmorItem.ArmorType.FEET));
            this.EQUIPMENT_COSM_HEAD_SLOT = container.addSlot(new ArmorContainerSlot(equipmentInventory, 3, ArmorItem.ArmorType.HEAD));
            this.EQUIPMENT_COSM_CHEST_SLOT = container.addSlot(new ArmorContainerSlot(equipmentInventory, 4, ArmorItem.ArmorType.CHEST));
            this.EQUIPMENT_COSM_FEET_SLOT = container.addSlot(new ArmorContainerSlot(equipmentInventory, 5, ArmorItem.ArmorType.FEET));
            this.EQUIPMENT_WEAPON_SLOT = container.addSlot(new SettlerWeaponContainerSlot(equipmentInventory, 6, HumanEquipmentFilterSetting.this.mob));
            container.addInventoryQuickTransfer(s -> this.isInEquipment, this.EQUIPMENT_HEAD_SLOT, this.EQUIPMENT_WEAPON_SLOT);
            container.subscribeEvent(
               SettlementSettlerEquipmentFilterChangedEvent.class,
               event -> this.isInFilters
                  && event.settlementUniqueID == HumanEquipmentFilterSetting.this.mob.getSettlementUniqueID()
                  && event.mobUniqueID == HumanEquipmentFilterSetting.this.mob.getUniqueID(),
               () -> true
            );
         }

         this.setIsInEquipment = container.registerAction(new BooleanCustomAction() {
            @Override
            protected void run(boolean value) {
               ContainerHandler.this.isInEquipment = value;
            }
         });
         this.setSelfManageEquipment = container.registerAction(new BooleanCustomAction() {
            @Override
            protected void run(boolean value) {
               if (container.hasSettlerAccess) {
                  HumanEquipmentFilterSetting.this.mob.selfManageEquipment.set(value);
               }
            }
         });
         this.setIsInFilters = container.registerAction(
            new BooleanCustomAction() {
               @Override
               protected void run(boolean value) {
                  ContainerHandler.this.isInFilters = value;
                  if (value && container.client.isServer()) {
                     LevelSettler levelSettler = HumanEquipmentFilterSetting.this.mob.levelSettler;
                     if (levelSettler != null) {
                        new SettlementSettlerEquipmentFilterChangedEvent(
                              levelSettler.data,
                              HumanEquipmentFilterSetting.this.mob.getUniqueID(),
                              levelSettler.preferArmorSets,
                              ItemCategoriesFilterChange.fullChange(levelSettler.equipmentFilter)
                           )
                           .applyAndSendToClient(container.client.getServerClient());
                     }
                  }
               }
            }
         );
         this.setEquipmentFilterSetting = container.registerAction(new SetEquipmentFilterSettingAction(container));
      }

      @Override
      public void initForm(final ShopContainerForm<?> form) {
         if (this.container.hasSettlerAccess) {
            this.container
               .onEvent(
                  SettlementSettlerEquipmentFilterChangedEvent.class,
                  event -> {
                     if (this.filtersForm != null) {
                        if (event.change != null) {
                           event.change.applyTo(this.filtersForm.equipmentFilter);
                        }
                     } else {
                        ItemCategoriesFilter filter = new ItemCategoriesFilter(ItemCategory.equipmentMasterCategory, true);
                        int height = Math.min(ContainerComponent.getMaxFocusHeight(), 300);
                        this.filtersForm = new EquipmentFiltersForm(
                           "equipmentFilter",
                           408,
                           height,
                           HumanEquipmentFilterSetting.this.mob.getLocalization(),
                           filter,
                           () -> this.preferArmorSets,
                           form.getClient()
                        ) {
                           @Override
                           public void onSetPreferArmorSets(boolean newPreferArmorSets) {
                              ContainerHandler.this.preferArmorSets = newPreferArmorSets;
                              ContainerHandler.this.setEquipmentFilterSetting.runAndSendPreferArmorSets(newPreferArmorSets);
                           }

                           @Override
                           public void onItemsChanged(Item[] items, boolean allowed) {
                              ContainerHandler.this.setEquipmentFilterSetting.runAndSendChange(ItemCategoriesFilterChange.itemsAllowed(items, allowed));
                           }

                           @Override
                           public void onCategoryChanged(ItemCategoriesFilter.ItemCategoryFilter category, boolean allowed) {
                              ContainerHandler.this.setEquipmentFilterSetting.runAndSendChange(ItemCategoriesFilterChange.categoryAllowed(category, allowed));
                           }

                           @Override
                           public void onFullChange(ItemCategoriesFilter filter) {
                              ContainerHandler.this.setEquipmentFilterSetting.runAndSendChange(ItemCategoriesFilterChange.fullChange(filter));
                           }

                           @Override
                           public void onButtonPressed() {
                              form.makeCurrent(ContainerHandler.this.equipmentForm);
                           }

                           @Override
                           public void onWindowResized(GameWindow window) {
                              super.onWindowResized(window);
                              ContainerComponent.setPosFocus(this);
                           }
                        };
                        form.addAndMakeCurrentTemporary(this.filtersForm, () -> {
                           this.filtersForm = null;
                           this.setIsInFilters.runAndSend(false);
                        });
                        this.filtersForm.onWindowResized(WindowManager.getWindow());
                        this.setIsInFilters.runAndSend(true);
                     }

                     this.preferArmorSets = event.preferArmorSets;
                  },
                  () -> this.isInFilters
               );
            this.equipmentForm = form.addComponent(new ShopEquipmentForm(form.getClient(), this) {
               @Override
               public void onBackPressed() {
                  ContainerHandler.this.setIsInEquipment.runAndSend(false);
                  form.makeCurrent(form.dialogueForm);
               }
            });
         }
      }

      @Override
      public void updateDialogue(ShopContainerForm<?> form) {
         if (this.container.hasSettlerAccess) {
            form.dialogueForm.addDialogueOption(new LocalMessage("ui", "settlerchangeequipment"), () -> {
               form.makeCurrent(this.equipmentForm);
               this.setIsInEquipment.runAndSend(true);
            });
         }
      }
   }
}
