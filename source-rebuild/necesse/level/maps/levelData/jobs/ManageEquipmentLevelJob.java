package necesse.level.maps.levelData.jobs;

import java.awt.Point;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.LinkedListJobSequence;
import necesse.entity.mobs.job.activeJob.EquipItemActiveJob;
import necesse.entity.mobs.job.activeJob.PerformLevelJobEventActiveJob;
import necesse.entity.mobs.job.activeJob.PickupSettlementStorageActiveJob;
import necesse.entity.mobs.job.activeJob.UnequipItemActiveJob;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.ItemAttackerWeaponItem;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.itemFilter.ItemCategoriesFilter;
import necesse.level.maps.levelData.settlementData.SettlementStoragePickupFuture;
import necesse.level.maps.levelData.settlementData.SettlementStoragePickupSlot;
import necesse.level.maps.levelData.settlementData.ZoneTester;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageEquipmentTypeIndex;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageRecord;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageRecords;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageRecordsRegionData;
import necesse.level.maps.levelData.settlementData.storage.ValidatedSettlementStorageRecord;

public class ManageEquipmentLevelJob extends EntityLevelJob<Mob> {
   public JobTypeHandler.SubHandler<?> handler;
   public ItemCategoriesFilter equipmentFilter;
   public boolean preferArmorSets;

   public ManageEquipmentLevelJob(Mob mob, JobTypeHandler.SubHandler<?> handler, ItemCategoriesFilter equipmentFilter, boolean preferArmorSets) {
      super(mob);
      this.handler = handler;
      this.equipmentFilter = equipmentFilter;
      this.preferArmorSets = preferArmorSets;
   }

   @Override
   public boolean isWithinRestrictZone(ZoneTester zone) {
      return true;
   }

   @Override
   public boolean isValid() {
      return true;
   }

   @Override
   public int getFirstPriority(FoundJob<?> foundJob) {
      return Integer.MAX_VALUE;
   }

   public static <T extends ManageEquipmentLevelJob> JobSequence getJobSequence(EntityJobWorker worker, FoundJob<T> foundJob, HumanMob humanMob) {
      Predicate<InventoryItem> itemFilter = item -> foundJob.job.equipmentFilter == null || foundJob.job.equipmentFilter.isItemAllowed(item.item);
      SettlementStorageRecords storageRecords = PickupSettlementStorageActiveJob.getStorageRecords(worker);
      if (storageRecords == null) {
         return null;
      }

      Inventory inventory = humanMob.getInventory();
      InventoryItem weaponInventoryItem = inventory.getItem(6);
      ItemAttackerWeaponItem weaponItem = getValidWeaponItem(weaponInventoryItem, humanMob);
      boolean weaponValid = weaponItem != null
         && itemFilter.test(weaponInventoryItem)
         && weaponItem.getItemAttackerWeaponValue(humanMob, weaponInventoryItem) > 0.0F;
      ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord currentWeaponRecord = ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord.WeaponItem(
         weaponItem != null && weaponValid ? weaponInventoryItem : null, humanMob
      );
      InventoryItem helmetInventoryItem = inventory.getItem(0);
      ArmorItem helmetItem = getValidArmorItem(helmetInventoryItem, ArmorItem.ArmorType.HEAD, humanMob);
      boolean helmetValid = helmetItem != null
         && itemFilter.test(helmetInventoryItem)
         && helmetItem.getSettlerEquipmentValue(helmetInventoryItem, humanMob) > 0.0F;
      ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord currentHelmetRecord = ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord.ArmorItem(
         helmetItem != null && helmetValid ? helmetInventoryItem : null, humanMob
      );
      InventoryItem chestInventoryItem = inventory.getItem(1);
      ArmorItem chestItem = getValidArmorItem(chestInventoryItem, ArmorItem.ArmorType.CHEST, humanMob);
      boolean chestValid = chestItem != null && itemFilter.test(chestInventoryItem) && chestItem.getSettlerEquipmentValue(chestInventoryItem, humanMob) > 0.0F;
      ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord currentChestRecord = ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord.ArmorItem(
         chestItem != null && chestValid ? chestInventoryItem : null, humanMob
      );
      InventoryItem bootsInventoryItem = inventory.getItem(2);
      ArmorItem bootsItem = getValidArmorItem(bootsInventoryItem, ArmorItem.ArmorType.FEET, humanMob);
      boolean bootsValid = bootsItem != null && itemFilter.test(bootsInventoryItem) && bootsItem.getSettlerEquipmentValue(bootsInventoryItem, humanMob) > 0.0F;
      ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord currentBootsRecord = ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord.ArmorItem(
         bootsItem != null && bootsValid ? bootsInventoryItem : null, humanMob
      );
      float currentScore = getScore(currentHelmetRecord, currentChestRecord, currentBootsRecord, foundJob.job.preferArmorSets);
      SettlementStorageEquipmentTypeIndex index = storageRecords.getIndex(SettlementStorageEquipmentTypeIndex.class);
      HashMap<Integer, ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord> weaponItems = getAvailableWeaponItems(worker, humanMob, index, itemFilter);
      HashMap<Integer, ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord> headItems = getAvailableArmorItems(
         worker, humanMob, index, SettlementStorageEquipmentTypeIndex.EquipmentType.HEAD, itemFilter
      );
      HashMap<Integer, ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord> chestItems = getAvailableArmorItems(
         worker, humanMob, index, SettlementStorageEquipmentTypeIndex.EquipmentType.CHEST, itemFilter
      );
      HashMap<Integer, ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord> bootsItems = getAvailableArmorItems(
         worker, humanMob, index, SettlementStorageEquipmentTypeIndex.EquipmentType.FEET, itemFilter
      );
      weaponItems.put(-1, currentWeaponRecord);
      headItems.put(-1, currentHelmetRecord);
      chestItems.put(-1, currentChestRecord);
      bootsItems.put(-1, currentBootsRecord);
      float bestScore = currentScore;
      ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord bestWeapon = weaponItems.values()
         .stream()
         .max(Comparator.comparingDouble(r -> r.equipmentValue))
         .orElse(null);
      ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord bestHelmet = currentHelmetRecord;
      ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord bestChest = currentChestRecord;
      ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord bestBoots = currentBootsRecord;
      if (foundJob.job.preferArmorSets) {
         for (ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord helmet : headItems.values()) {
            for (ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord chest : chestItems.values()) {
               for (ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord boots : bootsItems.values()) {
                  float score = getScore(helmet, chest, boots, foundJob.job.preferArmorSets);
                  if (score > bestScore) {
                     bestScore = score;
                     bestHelmet = helmet;
                     bestChest = chest;
                     bestBoots = boots;
                  }
               }
            }
         }
      } else {
         bestHelmet = headItems.values().stream().max(Comparator.comparingDouble(r -> r.equipmentValue)).orElse(null);
         bestChest = chestItems.values().stream().max(Comparator.comparingDouble(r -> r.equipmentValue)).orElse(null);
         bestBoots = bootsItems.values().stream().max(Comparator.comparingDouble(r -> r.equipmentValue)).orElse(null);
      }

      final LinkedListJobSequence sequence = new LinkedListJobSequence(null, false);
      if (weaponInventoryItem != null && !weaponValid) {
         sequence.add(new UnequipItemActiveJob(worker, foundJob.priority, 6, humanMob));
      }

      if (helmetInventoryItem != null && !helmetValid) {
         sequence.add(new UnequipItemActiveJob(worker, foundJob.priority, 0, humanMob));
      }

      if (chestInventoryItem != null && !chestValid) {
         sequence.add(new UnequipItemActiveJob(worker, foundJob.priority, 1, humanMob));
      }

      if (bootsInventoryItem != null && !bootsValid) {
         sequence.add(new UnequipItemActiveJob(worker, foundJob.priority, 2, humanMob));
      }

      if (bestWeapon != null && bestWeapon.futurePickup != null) {
         AtomicReference<InventoryItem> pickedUp = new AtomicReference<>();
         final GameMessage activityDescription = new LocalMessage("activities", "equipping", "item", bestWeapon.invItem.getItemLocalization());
         SettlementStoragePickupSlot pickup = bestWeapon.futurePickup.accept(1);
         sequence.add(new PickupSettlementStorageActiveJob(worker, foundJob.priority, pickup, pickedUp) {
            @Override
            public void onMadeCurrent() {
               super.onMadeCurrent();
               sequence.setActivityDescription(activityDescription);
            }
         });
         sequence.add(new EquipItemActiveJob(worker, foundJob.priority, pickedUp, 6, humanMob) {
            @Override
            public boolean useItem(InventoryItem item, ListIterator<InventoryItem> li) {
               sequence.setActivityDescription(null);
               return super.useItem(item, li);
            }
         });
      }

      if (bestHelmet != null && bestHelmet.futurePickup != null) {
         AtomicReference<InventoryItem> pickedUp = new AtomicReference<>();
         final GameMessage activityDescription = new LocalMessage("activities", "equipping", "item", bestHelmet.invItem.getItemLocalization());
         SettlementStoragePickupSlot pickup = bestHelmet.futurePickup.accept(1);
         sequence.add(new PickupSettlementStorageActiveJob(worker, foundJob.priority, pickup, pickedUp) {
            @Override
            public void onMadeCurrent() {
               super.onMadeCurrent();
               sequence.setActivityDescription(activityDescription);
            }
         });
         sequence.add(new EquipItemActiveJob(worker, foundJob.priority, pickedUp, 0, humanMob) {
            @Override
            public boolean useItem(InventoryItem item, ListIterator<InventoryItem> li) {
               sequence.setActivityDescription(null);
               return super.useItem(item, li);
            }
         });
      }

      if (bestChest != null && bestChest.futurePickup != null) {
         AtomicReference<InventoryItem> pickedUp = new AtomicReference<>();
         final GameMessage activityDescription = new LocalMessage("activities", "equipping", "item", bestChest.invItem.getItemLocalization());
         SettlementStoragePickupSlot pickup = bestChest.futurePickup.accept(1);
         sequence.add(new PickupSettlementStorageActiveJob(worker, foundJob.priority, pickup, pickedUp) {
            @Override
            public void onMadeCurrent() {
               super.onMadeCurrent();
               sequence.setActivityDescription(activityDescription);
            }
         });
         sequence.add(new EquipItemActiveJob(worker, foundJob.priority, pickedUp, 1, humanMob) {
            @Override
            public boolean useItem(InventoryItem item, ListIterator<InventoryItem> li) {
               sequence.setActivityDescription(null);
               return super.useItem(item, li);
            }
         });
      }

      if (bestBoots != null && bestBoots.futurePickup != null) {
         AtomicReference<InventoryItem> pickedUp = new AtomicReference<>();
         final GameMessage activityDescription = new LocalMessage("activities", "equipping", "item", bestBoots.invItem.getItemLocalization());
         SettlementStoragePickupSlot pickup = bestBoots.futurePickup.accept(1);
         sequence.add(new PickupSettlementStorageActiveJob(worker, foundJob.priority, pickup, pickedUp) {
            @Override
            public void onMadeCurrent() {
               super.onMadeCurrent();
               sequence.setActivityDescription(activityDescription);
            }
         });
         sequence.add(new EquipItemActiveJob(worker, foundJob.priority, pickedUp, 2, humanMob) {
            @Override
            public boolean useItem(InventoryItem item, ListIterator<InventoryItem> li) {
               sequence.setActivityDescription(null);
               return super.useItem(item, li);
            }
         });
      }

      if (sequence.isEmpty()) {
         return null;
      }

      sequence.add(new PerformLevelJobEventActiveJob(worker, foundJob));
      worker.setPrioritizeNextJob(HasStorageLevelJob.class, true);
      return sequence;
   }

   protected static ItemAttackerWeaponItem getValidWeaponItem(InventoryItem invItem, HumanMob humanMob) {
      if (invItem == null) {
         return null;
      }

      if (!(invItem.item instanceof ItemAttackerWeaponItem)) {
         return null;
      }

      ItemAttackerWeaponItem weaponItem = (ItemAttackerWeaponItem)invItem.item;
      return weaponItem.getItemAttackerCanUseError(humanMob, invItem) != null ? null : weaponItem;
   }

   protected static HashMap<Integer, ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord> getAvailableWeaponItems(
      EntityJobWorker worker, HumanMob humanMob, SettlementStorageEquipmentTypeIndex index, Predicate<InventoryItem> itemFilter
   ) {
      SettlementStorageRecordsRegionData regionData = index.getEquipmentType(SettlementStorageEquipmentTypeIndex.EquipmentType.WEAPON);
      if (regionData == null) {
         return new HashMap<>();
      }

      GameLinkedList<Entry<Point, GameLinkedList<SettlementStorageRecord>>> records = regionData.getRecords(worker);
      return records.stream()
         .sorted(Comparator.comparingDouble(e -> e.getKey().distance(worker.getMobWorker().getTileX(), worker.getMobWorker().getTileY())))
         .flatMap(
            e -> e.getValue()
               .streamElements()
               .map(
                  rElement -> {
                     InventoryItem inventoryItem = regionData.validateItem((SettlementStorageRecord)rElement.object, null);
                     if (inventoryItem != null) {
                        if (!itemFilter.test(inventoryItem)) {
                           return null;
                        }

                        ItemAttackerWeaponItem weaponItem = (ItemAttackerWeaponItem)inventoryItem.item;
                        if (weaponItem.getItemAttackerCanUseError(humanMob, inventoryItem) != null) {
                           return null;
                        }

                        ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord record = ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord.WeaponItem(
                           (GameLinkedList<SettlementStorageRecord>.Element)rElement, inventoryItem, humanMob
                        );
                        return record.futurePickup == null ? null : record;
                     } else {
                        return null;
                     }
                  }
               )
         )
         .filter(Objects::nonNull)
         .reduce(
            new HashMap<>(),
            (map, record) -> {
               if (map == null) {
                  map = new HashMap<>();
               }

               map.compute(
                  record.invItem.item.getID(),
                  (k, prev) -> (ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord)(prev != null && !(prev.equipmentValue < record.equipmentValue)
                     ? prev
                     : record)
               );
               return map;
            },
            (map1, map2) -> {
               map1.putAll(map2);
               return map1;
            }
         );
   }

   protected static ArmorItem getValidArmorItem(InventoryItem invItem, ArmorItem.ArmorType type, HumanMob humanMob) {
      if (invItem == null) {
         return null;
      } else if (!invItem.item.isArmorItem()) {
         return null;
      } else {
         ArmorItem armorItem = (ArmorItem)invItem.item;
         if (armorItem.armorType != type) {
            return null;
         } else if (armorItem.getSettlerEquipmentValue(invItem, humanMob) <= 0.0F) {
            return null;
         } else {
            return !armorItem.canMobEquip(humanMob, invItem) ? null : armorItem;
         }
      }
   }

   protected static HashMap<Integer, ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord> getAvailableArmorItems(
      EntityJobWorker worker,
      HumanMob humanMob,
      SettlementStorageEquipmentTypeIndex index,
      SettlementStorageEquipmentTypeIndex.EquipmentType type,
      Predicate<InventoryItem> itemFilter
   ) {
      SettlementStorageRecordsRegionData regionData = index.getEquipmentType(type);
      if (regionData == null) {
         return new HashMap<>();
      }

      GameLinkedList<Entry<Point, GameLinkedList<SettlementStorageRecord>>> records = regionData.getRecords(worker);
      return records.stream()
         .sorted(Comparator.comparingDouble(e -> e.getKey().distance(worker.getMobWorker().getTileX(), worker.getMobWorker().getTileY())))
         .flatMap(
            e -> e.getValue()
               .streamElements()
               .map(
                  rElement -> {
                     InventoryItem inventoryItem = regionData.validateItem((SettlementStorageRecord)rElement.object, null);
                     if (inventoryItem != null) {
                        if (!itemFilter.test(inventoryItem)) {
                           return null;
                        }

                        if (((ArmorItem)inventoryItem.item).getSettlerEquipmentValue(inventoryItem, humanMob) <= 0.0F) {
                           return null;
                        }

                        ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord record = ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord.ArmorItem(
                           (GameLinkedList<SettlementStorageRecord>.Element)rElement, inventoryItem, humanMob
                        );
                        return record.futurePickup == null ? null : record;
                     } else {
                        return null;
                     }
                  }
               )
         )
         .filter(Objects::nonNull)
         .reduce(
            new HashMap<>(),
            (map, record) -> {
               if (map == null) {
                  map = new HashMap<>();
               }

               map.compute(
                  record.invItem.item.getID(),
                  (k, prev) -> (ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord)(prev != null && !(prev.equipmentValue < record.equipmentValue)
                     ? prev
                     : record)
               );
               return map;
            },
            (map1, map2) -> {
               map1.putAll(map2);
               return map1;
            }
         );
   }

   protected static float getScore(
      ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord head,
      ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord chest,
      ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord feet,
      boolean preferSet
   ) {
      float score = 0.0F;
      if (feet != null) {
         score += feet.equipmentValue;
      }

      if (chest != null) {
         score += chest.equipmentValue;
      }

      if (head != null) {
         score += head.equipmentValue;
         if (preferSet
            && head.invItem != null
            && chest != null
            && chest.invItem != null
            && feet != null
            && feet.invItem != null
            && ((ArmorItem)head.invItem.item).hasSet(head.invItem, chest.invItem, feet.invItem)) {
            score *= 1.5F;
         }
      }

      return score;
   }

   public static JobTypeHandler.JobStreamSupplier<? extends ManageEquipmentLevelJob> getJobStreamer(
      Supplier<ItemCategoriesFilter> equipmentItemFilter, Supplier<Boolean> preferArmorSetsGetter
   ) {
      return (worker, handler) -> {
         Mob mobWorker = worker.getMobWorker();
         return Stream.of(new ManageEquipmentLevelJob(mobWorker, handler, equipmentItemFilter.get(), preferArmorSetsGetter.get()));
      };
   }

   public static JobTypeHandler.SubHandler<ManageEquipmentLevelJob> handler(EntityJobWorker worker, JobTypeHandler handler) {
      if (worker instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)worker;
         return handler.<ManageEquipmentLevelJob>setJobHandler(ManageEquipmentLevelJob.class, foundJob -> {
               humanMob.searchEquipmentCooldown = humanMob.getTime() + GameRandom.globalRandom.getIntBetween(60, 300) * 1000L;
               return getJobSequence(humanMob, foundJob, humanMob);
            })
            .setPredicate(
               () -> !humanMob.isOnStrike()
                  && !humanMob.hasCompletedMission()
                  && humanMob.selfManageEquipment.get()
                  && humanMob.searchEquipmentCooldown <= humanMob.getTime()
            )
            .setExtraJobStreamer(
               false,
               getJobStreamer(
                  () -> humanMob.levelSettler == null ? null : humanMob.levelSettler.equipmentFilter,
                  () -> humanMob.levelSettler == null || humanMob.levelSettler.preferArmorSets
               )
            );
      } else {
         return null;
      }
   }

   protected static class ValidatedStorageEquipmentRecord extends ValidatedSettlementStorageRecord {
      public float equipmentValue;
      public SettlementStoragePickupFuture futurePickup;

      private ValidatedStorageEquipmentRecord(SettlementStorageRecord record, InventoryItem invItem, float equipmentValue) {
         super(record, invItem);
         this.equipmentValue = equipmentValue;
      }

      private ValidatedStorageEquipmentRecord(GameLinkedList<SettlementStorageRecord>.Element element, InventoryItem invItem, float equipmentValue) {
         super(element, invItem);
         this.equipmentValue = equipmentValue;
         if (invItem != null && this.record != null) {
            this.futurePickup = this.record.storage.getFutureReserve(this.record.inventorySlot, invItem.copy(1), 1, slot -> {
               this.record.itemAmount = this.record.itemAmount - slot.item.getAmount();
               if (this.record.itemAmount <= 0 && !element.isRemoved()) {
                  element.remove();
               }
            });
         }
      }

      public static ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord WeaponItem(InventoryItem invItem, HumanMob mob) {
         float equipmentValue = invItem == null ? 0.0F : ((ItemAttackerWeaponItem)invItem.item).getItemAttackerWeaponValue(mob, invItem);
         return new ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord((SettlementStorageRecord)null, invItem, equipmentValue);
      }

      public static ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord WeaponItem(
         GameLinkedList<SettlementStorageRecord>.Element element, InventoryItem invItem, HumanMob mob
      ) {
         float equipmentValue = invItem == null ? 0.0F : ((ItemAttackerWeaponItem)invItem.item).getItemAttackerWeaponValue(mob, invItem);
         return new ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord(element, invItem, equipmentValue);
      }

      public static ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord ArmorItem(InventoryItem invItem, HumanMob mob) {
         float equipmentValue = invItem == null ? 0.0F : ((ArmorItem)invItem.item).getSettlerEquipmentValue(invItem, mob);
         return new ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord((SettlementStorageRecord)null, invItem, equipmentValue);
      }

      public static ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord ArmorItem(
         GameLinkedList<SettlementStorageRecord>.Element element, InventoryItem invItem, HumanMob mob
      ) {
         float equipmentValue = invItem == null ? 0.0F : ((ArmorItem)invItem.item).getSettlerEquipmentValue(invItem, mob);
         return new ManageEquipmentLevelJob.ValidatedStorageEquipmentRecord(element, invItem, equipmentValue);
      }
   }
}
