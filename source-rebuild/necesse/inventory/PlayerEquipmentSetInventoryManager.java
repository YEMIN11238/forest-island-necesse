package necesse.inventory;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import necesse.engine.achievements.AchievementManager;
import necesse.engine.journal.listeners.EquipmentChangedJournalChallengeListener;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.JournalChallengeRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.inventory.item.Item;
import necesse.inventory.recipe.IngredientCounter;
import necesse.inventory.recipe.IngredientStep;

public class PlayerEquipmentSetInventoryManager {
   private final PlayerInventoryManager manager;
   public final ArrayList<PlayerEquipmentInventory> armor;
   public final ArrayList<PlayerEquipmentInventory> cosmetic;
   public final ArrayList<PlayerEquipmentInventory> trinkets;
   public final ArrayList<PlayerEquipmentInventory> equipment;
   private int totalSets = 1;
   private int selectedSet = 0;

   public PlayerEquipmentSetInventoryManager(PlayerInventoryManager manager) {
      this.manager = manager;
      this.armor = new ArrayList<>(this.totalSets);
      this.cosmetic = new ArrayList<>(this.totalSets);
      this.trinkets = new ArrayList<>(this.totalSets);
      this.equipment = new ArrayList<>(this.totalSets);
      this.adjustSize(false);
   }

   private void adjustSize(boolean refreshIDs) {
      this.adjustSize(this.armor, this.totalSets, this::createNewArmorInventory);
      this.adjustSize(this.cosmetic, this.totalSets, this::createNewCosmeticInventory);
      if (this.adjustSize(this.trinkets, this.totalSets, this::createNewTrinketsInventory)) {
         int trinketsSize = this.trinkets.get(0).getSize();

         for (PlayerEquipmentInventory inventory : this.trinkets) {
            inventory.changeSize(trinketsSize);
         }
      }

      this.adjustSize(this.equipment, this.totalSets, this::createNewEquipmentInventory);
      if (refreshIDs) {
         this.manager.refreshInventoryIDs();
      }
   }

   private boolean adjustSize(ArrayList<PlayerEquipmentInventory> list, int expectedSize, Consumer<ArrayList<PlayerEquipmentInventory>> generator) {
      boolean out;
      for (out = false; list.size() > expectedSize; out = true) {
         list.remove(list.size() - 1);
      }

      for (int i = list.size(); i < expectedSize; i++) {
         generator.accept(list);
         out = true;
      }

      return out;
   }

   public void checkEquipmentAchievementsAndChallenges(ArrayList<PlayerEquipmentInventory> list, PlayerEquipmentInventory changedInventory, int changedSlot) {
      if (this.manager.player.getLevel() != null && this.manager.player.isServer()) {
         ServerClient serverClient = this.manager.player.getServerClient();
         if (serverClient != null && serverClient.achievementsLoaded()) {
            this.checkEquipmentAchievementsAndChallenges(serverClient, serverClient.achievements(), list, changedInventory, changedSlot);
         }
      }
   }

   public void checkEquipmentAchievementsAndChallenges(
      ServerClient client,
      AchievementManager achievements,
      ArrayList<PlayerEquipmentInventory> list,
      PlayerEquipmentInventory changedInventory,
      int changedSlot
   ) {
      if (list == this.armor) {
         InventoryItem changedItem = changedInventory.getItem(changedSlot);
         if (changedItem != null && changedItem.item.getID() == ItemRegistry.getItemID("goldcrown")) {
            achievements.SELF_PROCLAIMED.markCompleted(client);
         }

         if (changedItem != null && changedItem.item.getUpgradeTier(changedItem) >= 5.0F) {
            float lowestTier = 10.0F;

            for (int slot = 0; slot < changedInventory.getSize(); slot++) {
               InventoryItem item = changedInventory.getCurrentUsedItem(slot);
               if (item == null) {
                  lowestTier = 0.0F;
                  break;
               }

               float tier = item.item.getUpgradeTier(item);
               lowestTier = Math.min(tier, lowestTier);
               if (lowestTier < 5.0F) {
                  break;
               }
            }

            if (lowestTier >= 5.0F) {
               achievements.EMPOWERED.markCompleted(client);
            }

            if (lowestTier >= 10.0F) {
               achievements.OVERPOWERED.markCompleted(client);
            }
         }
      } else if (list == this.equipment && changedSlot == 1 && !changedInventory.isSlotClear(changedSlot)) {
         Item item = changedInventory.getItemSlot(changedSlot);
         if (item.isTrinketItem()) {
            achievements.EQUIP_ABILITY.markCompleted(client);
         }
      }

      JournalChallengeRegistry.handleListeners(
         client, EquipmentChangedJournalChallengeListener.class, challenge -> challenge.onEquipmentChanged(client, this, list, changedInventory, changedSlot)
      );
   }

   private void createNewArmorInventory(final ArrayList<PlayerEquipmentInventory> inventories) {
      PlayerEquipmentInventory inv = new PlayerEquipmentInventory(this.manager.player, 3, false, false, true, inventories, inventories.size()) {
         @Override
         public void updateSlot(int slot) {
            super.updateSlot(slot);
            if (this.isCurrentlySelected() || this.getProxy(slot) == this.setIndex) {
               this.player.equipmentBuffManager.updateArmorBuffs();
            }

            PlayerEquipmentSetInventoryManager.this.checkEquipmentAchievementsAndChallenges(inventories, this, slot);
         }
      };
      inventories.add(inv);
   }

   private void createNewCosmeticInventory(final ArrayList<PlayerEquipmentInventory> inventories) {
      PlayerEquipmentInventory inv = new PlayerEquipmentInventory(this.manager.player, 3, false, false, true, inventories, inventories.size()) {
         @Override
         public void updateSlot(int slot) {
            super.updateSlot(slot);
            if (this.isCurrentlySelected() || this.getProxy(slot) == this.setIndex) {
               this.player.equipmentBuffManager.updateCosmeticSetBonus();
            }

            PlayerEquipmentSetInventoryManager.this.checkEquipmentAchievementsAndChallenges(inventories, this, slot);
         }
      };
      inventories.add(inv);
   }

   private void createNewTrinketsInventory(final ArrayList<PlayerEquipmentInventory> inventories) {
      PlayerEquipmentInventory inv = new PlayerEquipmentInventory(this.manager.player, 4, true, false, true, inventories, inventories.size()) {
         @Override
         public void updateSlot(int slot) {
            super.updateSlot(slot);
            if (this.isCurrentlySelected() || this.getProxy(slot) == this.setIndex) {
               this.player.equipmentBuffManager.updateTrinketBuffs();
            }

            if (this.player.getLevel() != null && this.player.isServer() && this.player.isServerClient() && !this.isSlotClear(slot)) {
               this.player.getServerClient().newStats.trinkets_worn.addTrinketWorn(this.getItemSlot(slot));
            }

            PlayerEquipmentSetInventoryManager.this.checkEquipmentAchievementsAndChallenges(inventories, this, slot);
         }
      };
      inventories.add(inv);
   }

   private void createNewEquipmentInventory(final ArrayList<PlayerEquipmentInventory> inventories) {
      PlayerEquipmentInventory inv = new PlayerEquipmentInventory(this.manager.player, 2, false, false, true, inventories, inventories.size()) {
         @Override
         public void updateSlot(int slot) {
            super.updateSlot(slot);
            if (slot == 1) {
               if (this.isCurrentlySelected() || this.getProxy(slot) == this.setIndex) {
                  this.player.equipmentBuffManager.updateTrinketBuffs();
               }

               if (this.player.getLevel() != null && this.player.isServer() && this.player.isServerClient() && !this.isSlotClear(slot)) {
                  this.player.getServerClient().newStats.trinkets_worn.addTrinketWorn(this.getItemSlot(slot));
               }
            }

            PlayerEquipmentSetInventoryManager.this.checkEquipmentAchievementsAndChallenges(inventories, this, slot);
         }
      };
      inventories.add(inv);
   }

   public void refreshInventoryIDs(ArrayList<PlayerInventory> list) {
      for (PlayerEquipmentInventory inventory : this.armor) {
         inventory.setIDAndAddToList(list);
      }

      for (PlayerEquipmentInventory inventory : this.cosmetic) {
         inventory.setIDAndAddToList(list);
      }

      for (PlayerEquipmentInventory inventory : this.trinkets) {
         inventory.setIDAndAddToList(list);
      }

      for (PlayerEquipmentInventory inventory : this.equipment) {
         inventory.setIDAndAddToList(list);
      }
   }

   public int getSelectedSet() {
      if (this.selectedSet >= this.totalSets || this.selectedSet < 0) {
         this.selectedSet = 0;
      }

      return this.selectedSet;
   }

   public PlayerEquipmentInventory getSelectedArmorInventory() {
      return this.armor.get(this.getSelectedSet());
   }

   public PlayerEquipmentInventory getSelectedCosmeticInventory() {
      return this.cosmetic.get(this.getSelectedSet());
   }

   public PlayerEquipmentInventory getSelectedTrinketsInventory() {
      return this.trinkets.get(this.getSelectedSet());
   }

   public PlayerEquipmentInventory getSelectedEquipmentInventory() {
      return this.equipment.get(this.getSelectedSet());
   }

   public PlayerEquipmentInventory getSelectedArmorInventory(int slot) {
      return this.getSelectedArmorInventory().getCurrentUsedInventory(slot);
   }

   public PlayerEquipmentInventory getSelectedCosmeticInventory(int slot) {
      return this.getSelectedCosmeticInventory().getCurrentUsedInventory(slot);
   }

   public PlayerEquipmentInventory getSelectedTrinketsInventory(int slot) {
      return this.getSelectedTrinketsInventory().getCurrentUsedInventory(slot);
   }

   public PlayerEquipmentInventory getSelectedEquipmentInventory(int slot) {
      return this.getSelectedEquipmentInventory().getCurrentUsedInventory(slot);
   }

   public InventorySlot getSelectedArmorSlot(int slot) {
      return this.getSelectedArmorInventory().getCurrentUsedSlot(slot);
   }

   public InventorySlot getSelectedCosmeticSlot(int slot) {
      return this.getSelectedCosmeticInventory().getCurrentUsedSlot(slot);
   }

   public InventorySlot getSelectedTrinketsSlot(int slot) {
      return this.getSelectedTrinketsInventory().getCurrentUsedSlot(slot);
   }

   public InventorySlot getSelectedEquipmentSlot(int slot) {
      return this.getSelectedEquipmentInventory().getCurrentUsedSlot(slot);
   }

   public Stream<InventorySlot> streamSelectedArmorSlots() {
      Builder<InventorySlot> builder = Stream.builder();
      int size = this.armor.get(0).getSize();

      for (int i = 0; i < size; i++) {
         builder.add(this.getSelectedArmorSlot(i));
      }

      return builder.build();
   }

   public Iterable<InventorySlot> iterateSelectedArmorSlots() {
      return () -> this.streamSelectedArmorSlots().iterator();
   }

   public Stream<InventorySlot> streamSelectedCosmeticSlots() {
      Builder<InventorySlot> builder = Stream.builder();
      int size = this.cosmetic.get(0).getSize();

      for (int i = 0; i < size; i++) {
         builder.add(this.getSelectedCosmeticSlot(i));
      }

      return builder.build();
   }

   public Iterable<InventorySlot> iterateSelectedCosmeticSlots() {
      return () -> this.streamSelectedCosmeticSlots().iterator();
   }

   public Stream<InventorySlot> streamSelectedTrinketSlots() {
      Builder<InventorySlot> builder = Stream.builder();
      int size = this.trinkets.get(0).getSize();

      for (int i = 0; i < size; i++) {
         builder.add(this.getSelectedTrinketsSlot(i));
      }

      return builder.build();
   }

   public Iterable<InventorySlot> iterateSelectedTrinketSlots() {
      return () -> this.streamSelectedTrinketSlots().iterator();
   }

   public Stream<InventorySlot> streamSelectedEquipmentSlots() {
      Builder<InventorySlot> builder = Stream.builder();
      int size = this.equipment.get(0).getSize();

      for (int i = 0; i < size; i++) {
         builder.add(this.getSelectedEquipmentSlot(i));
      }

      return builder.build();
   }

   public Iterable<InventorySlot> iterateSelectedEquipmentSlots() {
      return () -> this.streamSelectedEquipmentSlots().iterator();
   }

   public int getTotalSets() {
      return this.totalSets;
   }

   public int getTrinketSlotsSize() {
      return this.trinkets.get(0).getSize();
   }

   public void changeTrinketSlotsSize(int count) {
      for (PlayerEquipmentInventory inventory : this.trinkets) {
         inventory.changeSize(count);
      }

      if (count > 4 && this.manager.player.isServerClient()) {
         ServerClient client = this.manager.player.getServerClient();
         if (client.achievementsLoaded()) {
            client.achievements().MAGICAL_DROP.markCompleted(client);
         }
      }
   }

   public void changeTotalItemSets(int count) {
      count = Math.max(1, count);
      if (this.totalSets != count) {
         this.totalSets = count;
         this.selectedSet = GameMath.limit(this.selectedSet, 0, this.totalSets - 1);
         this.adjustSize(true);
      }

      if (count >= 4 && this.manager.player.isServerClient()) {
         ServerClient client = this.manager.player.getServerClient();
         if (client.achievementsLoaded()) {
            client.achievements().GET_4_ITEM_SETS.markCompleted(client);
         }
      }
   }

   public int getAmount(Item item, boolean includeInactiveSets, String purpose) {
      int amount = 0;
      if (includeInactiveSets) {
         for (PlayerEquipmentInventory inventory : this.armor) {
            amount += inventory.getAmount(this.manager.player.getLevel(), this.manager.player, item, purpose);
         }

         for (PlayerEquipmentInventory inventory : this.cosmetic) {
            amount += inventory.getAmount(this.manager.player.getLevel(), this.manager.player, item, purpose);
         }

         for (PlayerEquipmentInventory inventory : this.trinkets) {
            amount += inventory.getAmount(this.manager.player.getLevel(), this.manager.player, item, purpose);
         }

         for (PlayerEquipmentInventory inventory : this.equipment) {
            amount += inventory.getAmount(this.manager.player.getLevel(), this.manager.player, item, purpose);
         }
      } else {
         amount += this.streamSelectedArmorSlots()
            .mapToInt(s -> s.inventory.getAmount(this.manager.player.getLevel(), this.manager.player, item, s.slot, s.slot, purpose))
            .sum();
         amount += this.streamSelectedCosmeticSlots()
            .mapToInt(s -> s.inventory.getAmount(this.manager.player.getLevel(), this.manager.player, item, s.slot, s.slot, purpose))
            .sum();
         amount += this.streamSelectedTrinketSlots()
            .mapToInt(s -> s.inventory.getAmount(this.manager.player.getLevel(), this.manager.player, item, s.slot, s.slot, purpose))
            .sum();
         amount += this.streamSelectedEquipmentSlots()
            .mapToInt(s -> s.inventory.getAmount(this.manager.player.getLevel(), this.manager.player, item, s.slot, s.slot, purpose))
            .sum();
      }

      return amount;
   }

   public IngredientStep countIngredientAmount(boolean includeInactiveSets, String purpose, IngredientCounter counter) {
      if (includeInactiveSets) {
         for (PlayerEquipmentInventory inventory : this.armor) {
            IngredientStep result = inventory.countIngredientAmount(this.manager.player.getLevel(), this.manager.player, purpose, counter);
            if (result == IngredientStep.BREAK) {
               return IngredientStep.BREAK;
            }
         }

         for (PlayerEquipmentInventory inventory : this.cosmetic) {
            IngredientStep result = inventory.countIngredientAmount(this.manager.player.getLevel(), this.manager.player, purpose, counter);
            if (result == IngredientStep.BREAK) {
               return IngredientStep.BREAK;
            }
         }

         for (PlayerEquipmentInventory inventory : this.trinkets) {
            IngredientStep result = inventory.countIngredientAmount(this.manager.player.getLevel(), this.manager.player, purpose, counter);
            if (result == IngredientStep.BREAK) {
               return IngredientStep.BREAK;
            }
         }

         for (PlayerEquipmentInventory inventory : this.equipment) {
            IngredientStep result = inventory.countIngredientAmount(this.manager.player.getLevel(), this.manager.player, purpose, counter);
            if (result == IngredientStep.BREAK) {
               return IngredientStep.BREAK;
            }
         }
      } else {
         for (InventorySlot slot : this.iterateSelectedArmorSlots()) {
            IngredientStep result = slot.inventory
               .countIngredientAmount(this.manager.player.getLevel(), this.manager.player, slot.slot, slot.slot, purpose, counter);
            if (result == IngredientStep.BREAK) {
               return IngredientStep.BREAK;
            }
         }

         for (InventorySlot slot : this.iterateSelectedCosmeticSlots()) {
            IngredientStep result = slot.inventory
               .countIngredientAmount(this.manager.player.getLevel(), this.manager.player, slot.slot, slot.slot, purpose, counter);
            if (result == IngredientStep.BREAK) {
               return IngredientStep.BREAK;
            }
         }

         for (InventorySlot slot : this.iterateSelectedTrinketSlots()) {
            IngredientStep result = slot.inventory
               .countIngredientAmount(this.manager.player.getLevel(), this.manager.player, slot.slot, slot.slot, purpose, counter);
            if (result == IngredientStep.BREAK) {
               return IngredientStep.BREAK;
            }
         }

         for (InventorySlot slot : this.iterateSelectedEquipmentSlots()) {
            IngredientStep result = slot.inventory
               .countIngredientAmount(this.manager.player.getLevel(), this.manager.player, slot.slot, slot.slot, purpose, counter);
            if (result == IngredientStep.BREAK) {
               return IngredientStep.BREAK;
            }
         }
      }

      return IngredientStep.CONTINUE;
   }

   public void addSaveData(SaveData save) {
      this.armor.get(0).addSaveData(save, "ARMOR");
      this.cosmetic.get(0).addSaveData(save, "COSMETIC");
      this.equipment.get(0).addSaveData(save, "EQUIPMENT");
      this.trinkets.get(0).addSaveData(save, "TRINKETS");

      for (int i = 1; i < this.totalSets; i++) {
         SaveData setSave = new SaveData("SET" + (i + 1));
         this.armor.get(i).addSaveData(setSave, "ARMOR");
         this.cosmetic.get(i).addSaveData(setSave, "COSMETIC");
         this.equipment.get(i).addSaveData(setSave, "EQUIPMENT");
         this.trinkets.get(i).addSaveData(setSave, "TRINKETS");
         save.addSaveData(setSave);
      }

      save.addInt("selectedSet", this.selectedSet);
   }

   public void applyLoadData(LoadData save) {
      int trinketsSize = this.trinkets.get(0).getSize();
      this.armor.get(0).applyLoadData(save, "ARMOR", false);
      this.cosmetic.get(0).applyLoadData(save, "COSMETIC", false);
      this.equipment.get(0).applyLoadData(save, "EQUIPMENT", false);
      this.trinkets.get(0).applyLoadData(save, "TRINKETS", true);
      trinketsSize = Math.max(trinketsSize, this.trinkets.get(0).getSize());

      for (int i = 2; i < 100; i++) {
         LoadData setSave = save.getFirstLoadDataByName("SET" + i);
         if (setSave == null) {
            if (this.totalSets >= i) {
               this.totalSets = i - 1;
               this.adjustSize(false);
            }
            break;
         }

         if (this.totalSets < i) {
            this.totalSets = i;
            this.adjustSize(false);
         }

         this.armor.get(i - 1).applyLoadData(setSave, "ARMOR", false);
         this.cosmetic.get(i - 1).applyLoadData(setSave, "COSMETIC", false);
         this.equipment.get(i - 1).applyLoadData(setSave, "EQUIPMENT", false);
         this.trinkets.get(i - 1).applyLoadData(setSave, "TRINKETS", true);
         trinketsSize = Math.max(trinketsSize, this.trinkets.get(i - 1).getSize());
      }

      if (trinketsSize != 0) {
         for (PlayerEquipmentInventory inventory : this.trinkets) {
            inventory.changeSize(trinketsSize);
         }
      }

      this.selectedSet = save.getInt("selectedSet", this.selectedSet, false);

      for (PlayerEquipmentInventory inv : this.armor) {
         inv.cleanInvalidProxies();
      }

      for (PlayerEquipmentInventory inv : this.cosmetic) {
         inv.cleanInvalidProxies();
      }

      for (PlayerEquipmentInventory inv : this.equipment) {
         inv.cleanInvalidProxies();
      }

      for (PlayerEquipmentInventory inv : this.trinkets) {
         inv.cleanInvalidProxies();
      }
   }

   public void setupContentPacket(PacketWriter writer) {
      writer.putNextShortUnsigned(this.totalSets);

      for (PlayerEquipmentInventory inventory : this.armor) {
         inventory.writePlayerEquipmentContent(writer);
      }

      for (PlayerEquipmentInventory inventory : this.cosmetic) {
         inventory.writePlayerEquipmentContent(writer);
      }

      for (PlayerEquipmentInventory inventory : this.trinkets) {
         inventory.writePlayerEquipmentContent(writer);
      }

      for (PlayerEquipmentInventory inventory : this.equipment) {
         inventory.writePlayerEquipmentContent(writer);
      }

      this.writeSelectedSet(writer);
   }

   public void applyContentPacket(PacketReader reader) {
      int totalSets = reader.getNextShortUnsigned();
      if (totalSets != this.totalSets) {
         this.totalSets = totalSets;
         this.adjustSize(true);
      }

      for (PlayerEquipmentInventory inventory : this.armor) {
         inventory.readPlayerEquipmentContent(reader);
      }

      for (PlayerEquipmentInventory inventory : this.cosmetic) {
         inventory.readPlayerEquipmentContent(reader);
      }

      for (PlayerEquipmentInventory inventory : this.trinkets) {
         inventory.readPlayerEquipmentContent(reader);
      }

      for (PlayerEquipmentInventory inventory : this.equipment) {
         inventory.readPlayerEquipmentContent(reader);
      }

      this.readSelectedSet(reader);
   }

   public void writeSelectedSet(PacketWriter writer) {
      writer.putNextByteUnsigned(this.getSelectedSet());
   }

   public void readSelectedSet(PacketReader reader) {
      this.setSelectedSet(reader.getNextByteUnsigned());
   }

   public void setSelectedSet(int selectedSet) {
      selectedSet = GameMath.limit(selectedSet, 0, this.totalSets - 1);
      if (selectedSet != this.selectedSet) {
         this.selectedSet = selectedSet;
         this.updateSelectedSet();
      }
   }

   private void updateSelectedSet() {
      this.manager.player.equipmentBuffManager.updateArmorBuffs();
      this.manager.player.equipmentBuffManager.updateCosmeticSetBonus();
      this.manager.player.equipmentBuffManager.updateTrinketBuffs();
   }

   public void dropInventory() {
      for (PlayerEquipmentInventory inventory : this.armor) {
         this.manager.dropInventory(inventory);
      }

      for (PlayerEquipmentInventory inventory : this.cosmetic) {
         this.manager.dropInventory(inventory);
      }

      for (PlayerEquipmentInventory inventory : this.trinkets) {
         this.manager.dropInventory(inventory);
      }

      for (PlayerEquipmentInventory inventory : this.equipment) {
         this.manager.dropInventory(inventory);
      }
   }

   public Stream<PlayerInventory> streamAllInventories() {
      Builder<PlayerInventory> builder = Stream.builder();

      for (PlayerEquipmentInventory inventory : this.armor) {
         builder.add(inventory);
      }

      for (PlayerEquipmentInventory inventory : this.cosmetic) {
         builder.add(inventory);
      }

      for (PlayerEquipmentInventory inventory : this.trinkets) {
         builder.add(inventory);
      }

      for (PlayerEquipmentInventory inventory : this.equipment) {
         builder.add(inventory);
      }

      return builder.build();
   }

   public Stream<InventorySlot> streamActiveSlots() {
      return GameUtils.<InventorySlot>concat(
            this.streamSelectedArmorSlots(), this.streamSelectedCosmeticSlots(), this.streamSelectedEquipmentSlots(), this.streamSelectedTrinketSlots()
         )
         .map(s -> (InventorySlot)s);
   }

   public int removeItems(Item item, int amount, boolean includeInactiveSets, String purpose) {
      if (includeInactiveSets) {
         for (PlayerEquipmentInventory inventory : this.armor) {
            amount -= inventory.removeItems(this.manager.player.getLevel(), this.manager.player, item, amount, purpose);
            if (amount < 0) {
               return amount;
            }
         }

         for (PlayerEquipmentInventory inventory : this.cosmetic) {
            amount -= inventory.removeItems(this.manager.player.getLevel(), this.manager.player, item, amount, purpose);
            if (amount < 0) {
               return amount;
            }
         }

         for (PlayerEquipmentInventory inventory : this.trinkets) {
            amount -= inventory.removeItems(this.manager.player.getLevel(), this.manager.player, item, amount, purpose);
            if (amount < 0) {
               return amount;
            }
         }

         for (PlayerEquipmentInventory inventory : this.equipment) {
            amount -= inventory.removeItems(this.manager.player.getLevel(), this.manager.player, item, amount, purpose);
            if (amount < 0) {
               return amount;
            }
         }
      } else {
         for (InventorySlot slot : this.iterateSelectedArmorSlots()) {
            amount -= slot.inventory.removeItems(this.manager.player.getLevel(), this.manager.player, item, amount, slot.slot, slot.slot, purpose);
            if (amount < 0) {
               return amount;
            }
         }

         for (InventorySlot slot : this.iterateSelectedArmorSlots()) {
            amount -= slot.inventory.removeItems(this.manager.player.getLevel(), this.manager.player, item, amount, slot.slot, slot.slot, purpose);
            if (amount < 0) {
               return amount;
            }
         }

         for (InventorySlot slot : this.iterateSelectedArmorSlots()) {
            amount -= slot.inventory.removeItems(this.manager.player.getLevel(), this.manager.player, item, amount, slot.slot, slot.slot, purpose);
            if (amount < 0) {
               return amount;
            }
         }

         for (InventorySlot slot : this.iterateSelectedArmorSlots()) {
            amount -= slot.inventory.removeItems(this.manager.player.getLevel(), this.manager.player, item, amount, slot.slot, slot.slot, purpose);
            if (amount < 0) {
               return amount;
            }
         }
      }

      return amount;
   }
}
