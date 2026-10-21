package necesse.level.maps.levelData.settlementData;

import java.awt.Point;
import java.util.ArrayList;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.leaves.FollowerAINode;
import necesse.entity.mobs.friendly.FriendlyRopableMob;
import necesse.entity.mobs.friendly.HusbandryMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;

public class AnimalMerchantSettlementVisitorSpawner extends SettlementVisitorSpawner {
   public static ArrayList<AnimalMerchantSettlementVisitorSpawner.AnimalSpawner> ANIMAL_SPAWNERS = new ArrayList<>();
   protected int animalCount;

   public AnimalMerchantSettlementVisitorSpawner(SettlementVisitorOdds odds, ServerSettlementData data, String mobStringID, int animalCount) {
      super(odds, (HumanMob)MobRegistry.getMob(mobStringID, data.getLevel()));
      this.animalCount = animalCount;
   }

   @Override
   public void onSpawned(Level level, ServerSettlementData data, Point spawnPos) {
      super.onSpawned(level, data, spawnPos);
      TicketSystemList<AnimalMerchantSettlementVisitorSpawner.AnimalSpawner> spawners = new TicketSystemList<>();

      for (AnimalMerchantSettlementVisitorSpawner.AnimalSpawner spawner : ANIMAL_SPAWNERS) {
         int tickets = spawner.getTickets(data);
         if (tickets > 0) {
            spawners.addObject(tickets, spawner);
         }
      }

      if (!spawners.isEmpty()) {
         AnimalMerchantSettlementVisitorSpawner.AnimalSpawner spawner = spawners.getRandomObject(GameRandom.globalRandom);
         spawner.spawnMobs(data, this.mob, this.animalCount);
      }
   }

   public static boolean hasLessThanOrEqualAnimals(ServerSettlementData data, String mobStringID, int max) {
      int mobID = MobRegistry.getMobID(mobStringID);
      if (mobID == -1) {
         return false;
      }

      Level level = data.getLevel();
      Point pos = data.getFlagTile();
      if (pos == null) {
         return false;
      }

      long count = level.entityManager.mobs.streamInRegionsShape(data.networkData.getLevelRectangle(), 1).filter(m -> m.getID() == mobID).limit(max).count();
      return count <= max;
   }

   static {
      ANIMAL_SPAWNERS.add(
         new AnimalMerchantSettlementVisitorSpawner.AnimalSpawner() {
            @Override
            public int getTickets(ServerSettlementData data) {
               if (AnimalMerchantSettlementVisitorSpawner.hasLessThanOrEqualAnimals(data, "cow", 10)) {
                  return 1000;
               } else {
                  return AnimalMerchantSettlementVisitorSpawner.hasLessThanOrEqualAnimals(data, "bull", 2) ? 1000 : 0;
               }
            }

            @Override
            public FriendlyRopableMob getNewMob(ServerSettlementData data, int index, int total) {
               return (index == 0 || GameRandom.globalRandom.getEveryXthChance(4)) && index != 1
                  ? (FriendlyRopableMob)MobRegistry.getMob("bull", data.getLevel())
                  : (FriendlyRopableMob)MobRegistry.getMob("cow", data.getLevel());
            }
         }
      );
      ANIMAL_SPAWNERS.add(
         new AnimalMerchantSettlementVisitorSpawner.AnimalSpawner() {
            @Override
            public int getTickets(ServerSettlementData data) {
               if (AnimalMerchantSettlementVisitorSpawner.hasLessThanOrEqualAnimals(data, "sheep", 10)) {
                  return 1000;
               } else {
                  return AnimalMerchantSettlementVisitorSpawner.hasLessThanOrEqualAnimals(data, "ram", 2) ? 1000 : 0;
               }
            }

            @Override
            public FriendlyRopableMob getNewMob(ServerSettlementData data, int index, int total) {
               return (index == 0 || GameRandom.globalRandom.getEveryXthChance(4)) && index != 1
                  ? (FriendlyRopableMob)MobRegistry.getMob("ram", data.getLevel())
                  : (FriendlyRopableMob)MobRegistry.getMob("sheep", data.getLevel());
            }
         }
      );
      ANIMAL_SPAWNERS.add(
         new AnimalMerchantSettlementVisitorSpawner.AnimalSpawner() {
            @Override
            public int getTickets(ServerSettlementData data) {
               if (AnimalMerchantSettlementVisitorSpawner.hasLessThanOrEqualAnimals(data, "chicken", 10)) {
                  return 1000;
               } else {
                  return AnimalMerchantSettlementVisitorSpawner.hasLessThanOrEqualAnimals(data, "rooster", 2) ? 1000 : 0;
               }
            }

            @Override
            public FriendlyRopableMob getNewMob(ServerSettlementData data, int index, int total) {
               return (index == 0 || GameRandom.globalRandom.getEveryXthChance(4)) && index != 1
                  ? (FriendlyRopableMob)MobRegistry.getMob("rooster", data.getLevel())
                  : (FriendlyRopableMob)MobRegistry.getMob("chicken", data.getLevel());
            }
         }
      );
      ANIMAL_SPAWNERS.add(
         new AnimalMerchantSettlementVisitorSpawner.AnimalSpawner() {
            @Override
            public int getTickets(ServerSettlementData data) {
               if (!data.storyObjectives.hasCompletedStoryObjectiveOrHigher("defeatpiratecaptain")) {
                  return 0;
               } else if (AnimalMerchantSettlementVisitorSpawner.hasLessThanOrEqualAnimals(data, "pig", 10)) {
                  return 1000;
               } else {
                  return AnimalMerchantSettlementVisitorSpawner.hasLessThanOrEqualAnimals(data, "boar", 2) ? 1000 : 0;
               }
            }

            @Override
            public FriendlyRopableMob getNewMob(ServerSettlementData data, int index, int total) {
               return (index == 0 || GameRandom.globalRandom.getEveryXthChance(4)) && index != 1
                  ? (FriendlyRopableMob)MobRegistry.getMob("boar", data.getLevel())
                  : (FriendlyRopableMob)MobRegistry.getMob("pig", data.getLevel());
            }
         }
      );
   }

   public abstract static class AnimalSpawner {
      public abstract int getTickets(ServerSettlementData var1);

      public abstract FriendlyRopableMob getNewMob(ServerSettlementData var1, int var2, int var3);

      public void spawnMobs(ServerSettlementData data, Mob visitor, int animalCount) {
         for (int i = 0; i < animalCount; i++) {
            FriendlyRopableMob newMob = this.getNewMob(data, i, animalCount);
            if (newMob != null) {
               Point animalPos = FollowerAINode.getTeleportCloseToPos(newMob, visitor, 1);
               newMob.onRope(visitor.getUniqueID(), new InventoryItem("rope"));
               newMob.removeIfRoperRemoved = true;
               newMob.setDefaultBuyPrice(GameRandom.globalRandom);
               if (newMob instanceof HusbandryMob) {
                  ((HusbandryMob)newMob).setImported();
               }

               float var10002 = animalPos.x;
               data.getLevel().entityManager.addMob(newMob, var10002, animalPos.y);
            }
         }
      }
   }
}
