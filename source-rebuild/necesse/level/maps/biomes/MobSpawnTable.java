package necesse.level.maps.biomes;

import java.awt.Point;
import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.level.maps.Level;

public class MobSpawnTable {
   private final LinkedList<MobSpawnTable> includes = new LinkedList<>();
   private final LinkedList<MobChance> table = new LinkedList<>();

   public static MobSpawnTable.CanSpawnPredicate canSpawnEither(int minDaysPlayed, int minArmor, int aboveMaxHealth) {
      return (level, client, spawnTile, purpose) -> client == null
         || client.characterStats().time_played.get() >= level.getWorldEntity().getDayTimeMax() * minDaysPlayed
         || client.playerMob.getArmor() >= minArmor
         || client.playerMob.getMaxHealthFlat() >= aboveMaxHealth;
   }

   public MobSpawnTable include(MobSpawnTable other) {
      this.includes.add(other);
      return this;
   }

   public MobSpawnTable clear() {
      this.includes.clear();
      this.table.clear();
      return this;
   }

   public MobSpawnTable add(MobChance mobChance) {
      this.table.add(mobChance);
      return this;
   }

   public MobSpawnTable addLimited(int tickets, String mobStringID, int maxMobs, int searchRange, Predicate<Mob> searchFilter) {
      return this.add(
         tickets,
         (level, client, spawnTile, purpose) -> {
            Point spawnPos = new Point(spawnTile.x * 32 + 16, spawnTile.y * 32 + 16);
            long count = level.entityManager
               .mobs
               .streamInRegionsShape(GameUtils.rangeBounds(spawnPos.x, spawnPos.y, searchRange), 0)
               .filter(searchFilter)
               .filter(m -> m.getDistance(spawnPos.x, spawnPos.y) <= searchRange)
               .count();
            return count < maxMobs;
         },
         mobStringID
      );
   }

   public MobSpawnTable addLimited(int tickets, String mobStringID, int maxMobs, int searchRange) {
      return this.addLimited(tickets, mobStringID, maxMobs, searchRange, m -> m.getStringID().equals(mobStringID));
   }

   public MobSpawnTable add(int tickets, final MobSpawnTable.CanSpawnPredicate canSpawn, final MobSpawnTable.MobProducer mobProducer) {
      return this.add(new MobChance(tickets) {
         @Override
         public boolean canSpawn(Level level, ServerClient client, Point spawnTile, String purpose) {
            return canSpawn.canSpawn(level, client, spawnTile, purpose);
         }

         @Override
         public Mob getMob(Level level, ServerClient client, Point spawnTile) {
            return mobProducer.getMob(level, client, spawnTile);
         }
      });
   }

   public MobSpawnTable add(int tickets, MobSpawnTable.CanSpawnPredicate canSpawn, String mobStringID) {
      return this.add(tickets, canSpawn, (level, client, spawnTile) -> MobRegistry.getMob(mobStringID, level));
   }

   public MobSpawnTable add(int tickets, String mobStringID) {
      return this.add(tickets, (level, client, spawnTile, purpose) -> true, mobStringID);
   }

   public MobSpawnTable add(int tickets, MobSpawnTable.MobProducer mobProducer) {
      return this.add(tickets, (level, client, spawnTile, purpose) -> true, mobProducer);
   }

   public MobSpawnTable withoutRandomMob(MobChance mob) {
      MobSpawnTable out = new MobSpawnTable();

      for (MobSpawnTable include : this.includes) {
         out.include(include.withoutRandomMob(mob));
      }

      out.table.addAll(this.table);
      out.table.remove(mob);
      return out;
   }

   public MobChance getRandomMob(Level level, ServerClient client, Point spawnTile, GameRandom random, String purpose) {
      int maxTickets = 0;
      LinkedList<MobChance> canSpawns = new LinkedList<>();
      maxTickets = this.addCanSpawns(canSpawns, maxTickets, level, client, spawnTile, purpose);
      if (maxTickets <= 0) {
         return null;
      }

      int ticket = random.nextInt(maxTickets);
      int ticketCounter = 0;

      for (MobChance canSpawn : canSpawns) {
         if (ticket >= ticketCounter && ticket < ticketCounter + canSpawn.tickets) {
            return canSpawn;
         }

         ticketCounter += canSpawn.tickets;
      }

      return null;
   }

   private int addCanSpawns(LinkedList<MobChance> canSpawns, int maxTickets, Level level, ServerClient client, Point spawnTile, String purpose) {
      for (MobSpawnTable include : this.includes) {
         maxTickets = include.addCanSpawns(canSpawns, maxTickets, level, client, spawnTile, purpose);
      }

      for (MobChance mobChance : this.table) {
         if (mobChance.canSpawn(level, client, spawnTile, purpose)) {
            canSpawns.add(mobChance);
            maxTickets += mobChance.tickets;
         }
      }

      return maxTickets;
   }

   @FunctionalInterface
   public interface CanSpawnPredicate {
      boolean canSpawn(Level var1, ServerClient var2, Point var3, String var4);
   }

   @FunctionalInterface
   public interface MobProducer {
      Mob getMob(Level var1, ServerClient var2, Point var3);
   }

   @FunctionalInterface
   public interface MobSpawner {
      Collection<Mob> spawnMob(Level var1, ServerClient var2, Point var3, Predicate<Mob> var4, Consumer<Mob> var5, String var6);
   }
}
