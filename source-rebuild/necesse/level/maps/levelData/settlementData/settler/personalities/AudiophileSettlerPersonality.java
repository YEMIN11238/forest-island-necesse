package necesse.level.maps.levelData.settlementData.settler.personalities;

import java.util.HashMap;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.engine.util.PointHashMap;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.interfaces.OEInventory;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.InventoryUpdateListener;
import necesse.inventory.item.miscItem.VinylItem;
import necesse.level.maps.LevelObject;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.SettlementRoom;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleScalingHappinessSettlerThought;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public class AudiophileSettlerPersonality extends SettlerPersonality {
   public boolean hasMusicPlayer;
   public int totalVinyls;
   protected PointHashMap<AudiophileSettlerPersonality.TrackedVinylInventory> trackedInventories = new PointHashMap<>();

   public AudiophileSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_hasplayer", new SimpleSettlerThought("personalities", "hasmusicplayer", 20).setPersonalitySubmitter(this)
      );
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_missingplayer", new SimpleSettlerThought("personalities", "missingmusicplayer", 0).setPersonalitySubmitter(this)
      );
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_none", new SimpleSettlerThought("personalities", "novinyls", 0).setPersonalitySubmitter(this)
      );
      LocalMessage vinylsMessage = new LocalMessage("personalities", "somevinyls");
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_some", new SimpleScalingHappinessSettlerThought(vinylsMessage, 5, -1, 0).setPersonalitySubmitter(this)
      );
   }

   @Override
   public void onRoomStatsCalculated(SettlementBed bed, SettlementRoom settlementRoom) {
      super.onRoomStatsCalculated(bed, settlementRoom);
      this.hasMusicPlayer = false;
      this.totalVinyls = 0;

      for (AudiophileSettlerPersonality.TrackedVinylInventory trackedInventory : this.trackedInventories.values()) {
         trackedInventory.listener.dispose();
      }

      this.trackedInventories.clear();
      if (settlementRoom != null) {
         this.hasMusicPlayer = settlementRoom.getRoomProperty("musicplayer") > 0;

         for (LevelObject levelObject : settlementRoom.getLevelObjectsInRoom(true, true)) {
            ObjectEntity objectEntity = levelObject.getCurrentObjectEntity();
            if (objectEntity instanceof OEInventory) {
               Inventory inventory = ((OEInventory)objectEntity).getInventory();
               if (inventory != null) {
                  AudiophileSettlerPersonality.TrackedVinylInventory tracked = new AudiophileSettlerPersonality.TrackedVinylInventory(objectEntity, inventory);
                  this.trackedInventories.put(tracked.tileX, tracked.tileY, tracked);
               }
            }
         }
      }
   }

   @Override
   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      SettlerThoughtsList list = new SettlerThoughtsList();
      if (this.hasMusicPlayer) {
         list.addThought(this.getStringID() + "_hasplayer");
         if (this.totalVinyls == 0) {
            list.addThought(this.getStringID() + "_none");
         } else {
            list.addThought(this.getStringID() + "_some", this.totalVinyls);
         }
      } else {
         list.addThought(this.getStringID() + "_missingplayer");
      }

      return list;
   }

   protected class TrackedVinylInventory {
      public final int tileX;
      public final int tileY;
      public final HashMap<Integer, Integer> slotToAmountMap = new HashMap<>();
      public final InventoryUpdateListener listener;

      public TrackedVinylInventory(ObjectEntity objectEntity, final Inventory inventory) {
         this.tileX = objectEntity.tileX;
         this.tileY = objectEntity.tileY;
         inventory.addSlotUpdateListener(
            this.listener = new InventoryUpdateListener() {
               @Override
               public void onSlotUpdate(int slot) {
                  TrackedVinylInventory.this.updateSlot(inventory, slot);
               }

               @Override
               public boolean isDisposed() {
                  return AudiophileSettlerPersonality.this.trackedInventories.get(TrackedVinylInventory.this.tileX, TrackedVinylInventory.this.tileY)
                     != TrackedVinylInventory.this;
               }
            }
         );

         for (int slot = 0; slot < inventory.getSize(); slot++) {
            this.updateSlot(inventory, slot);
         }
      }

      public void updateSlot(Inventory inventory, int slot) {
         InventoryItem item = inventory.getItem(slot);
         if (item != null && item.item instanceof VinylItem) {
            Integer previous = this.slotToAmountMap.put(slot, item.getAmount());
            if (previous != null) {
               AudiophileSettlerPersonality.this.totalVinyls = AudiophileSettlerPersonality.this.totalVinyls + (item.getAmount() - previous);
            } else {
               AudiophileSettlerPersonality.this.totalVinyls = AudiophileSettlerPersonality.this.totalVinyls + item.getAmount();
            }
         } else {
            Integer previous = this.slotToAmountMap.remove(slot);
            if (previous != null) {
               AudiophileSettlerPersonality.this.totalVinyls = AudiophileSettlerPersonality.this.totalVinyls - previous;
            }
         }
      }

      public int getTotalVinyls() {
         return this.slotToAmountMap.values().stream().mapToInt(Integer::intValue).sum();
      }
   }
}
