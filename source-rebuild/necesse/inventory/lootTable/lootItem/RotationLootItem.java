package necesse.inventory.lootTable.lootItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import necesse.engine.network.server.ServerClient;
import necesse.engine.playerStats.PlayerStats;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootList;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;

public class RotationLootItem implements LootItemInterface {
   public ArrayList<LootItemInterface> items;
   public RotationLootItem.SeedGetter seedGetter;
   public RotationLootItem.Counter counter;

   public RotationLootItem(RotationLootItem.SeedGetter seedGetter, RotationLootItem.Counter counter, LootItemInterface... items) {
      this.items = new ArrayList<>(Arrays.asList(items));
      this.seedGetter = seedGetter;
      if (counter == null) {
         counter = (random, extra) -> random.nextInt(Integer.MAX_VALUE);
      }

      this.counter = counter;
   }

   public static RotationLootItem privateLootRotation(
      BiFunction<Mob, ServerClient, Long> seedGetter, BiFunction<Mob, ServerClient, Integer> counter, LootItemInterface... items
   ) {
      AtomicInteger count = new AtomicInteger(0);
      return new RotationLootItem((random, extra) -> {
         Mob mob = LootTable.expectExtra(Mob.class, extra, 0);
         ServerClient client = LootTable.expectExtra(ServerClient.class, extra, 1);
         return client != null ? seedGetter.apply(mob, client) : random.nextInt(Integer.MAX_VALUE);
      }, (random, extra) -> {
         Mob mob = LootTable.expectExtra(Mob.class, extra, 0);
         ServerClient client = LootTable.expectExtra(ServerClient.class, extra, 1);
         return mob != null && client != null ? counter.apply(mob, client) : count.getAndAdd(1);
      }, items);
   }

   @Deprecated
   public static RotationLootItem privateLootRotation(
      Function<ServerClient, Long> seedGetter, BiFunction<Mob, ServerClient, Integer> counter, LootItemInterface... items
   ) {
      return privateLootRotation((mob, client) -> seedGetter.apply(client), counter, items);
   }

   public static RotationLootItem privateLootRotation(String mobStringID, LootItemInterface... items) {
      return privateLootRotation(
         (mob, client) -> GameUtils.longHash(client.authentication, client.getServer().world.worldEntity.getWorldSeed(), mobStringID.hashCode()),
         (mob, client) -> client.characterStats().mob_kills.getKills(mobStringID),
         items
      );
   }

   public static RotationLootItem privateLootRotation(BiFunction<Mob, ServerClient, Integer> counter, LootItemInterface... items) {
      return privateLootRotation(
         (mob, client) -> GameUtils.longHash(
            client.authentication, client.getServer().world.worldEntity.getWorldSeed(), mob == null ? null : mob.getStringID().hashCode()
         ),
         counter,
         items
      );
   }

   public static RotationLootItem privateLootRotation(LootItemInterface... items) {
      return privateLootRotation((mob, client) -> Math.max(client.characterStats().mob_kills.getKills(mob.getStringID()) - 1, 0), items);
   }

   public static RotationLootItem customLootRotation(int divider, Function<Mob, Integer> statGetter, LootItemInterface... items) {
      return new RotationLootItem((random, extra) -> {
         Mob mob = LootTable.expectExtra(Mob.class, extra, 0);
         return mob != null && mob.isServer() ? mob.getWorldEntity().getWorldSeed() : random.nextInt(Integer.MAX_VALUE);
      }, (random, extra) -> {
         Mob mob = LootTable.expectExtra(Mob.class, extra, 0);
         return mob != null && mob.isServer() ? statGetter.apply(mob) / divider : random.nextInt(Integer.MAX_VALUE);
      }, items);
   }

   public static RotationLootItem levelLootRotation(int divider, LootItemInterface... items) {
      return customLootRotation(divider, mob -> {
         PlayerStats stats = mob.getLevel().levelStats;
         return Math.max(stats.mob_kills.getKills(mob.getStringID()) - 1, 0);
      }, items);
   }

   public static RotationLootItem globalLootRotation(int divider, LootItemInterface... items) {
      return customLootRotation(divider, mob -> {
         PlayerStats stats = mob.getWorldEntity().worldStats;
         return Math.max(stats.mob_kills.getKills(mob.getStringID()) - 1, 0);
      }, items);
   }

   public static RotationLootItem globalLootRotation(LootItemInterface... items) {
      return globalLootRotation(1, items);
   }

   public static RotationLootItem presetRotation(LootItemInterface... items) {
      return new RotationLootItem((random, extra) -> {
         Level level = LootTable.expectExtra(Level.class, extra, 0);
         return level != null ? level.getSeed() : random.nextInt(Integer.MAX_VALUE);
      }, (random, extra) -> {
         AtomicInteger counter = LootTable.expectExtra(AtomicInteger.class, extra, 1);
         return counter != null ? counter.getAndAdd(1) : random.nextInt(Integer.MAX_VALUE);
      }, items);
   }

   @Override
   public void addPossibleLoot(LootList list, Object... extra) {
      for (LootItemInterface item : this.items) {
         item.addPossibleLoot(list, extra);
      }
   }

   @Override
   public void addItems(List<InventoryItem> list, GameRandom random, float lootMultiplier, Object... extra) {
      LootTable.runMultiplied(random, lootMultiplier, () -> {
         if (!this.items.isEmpty()) {
            long seed = this.seedGetter == null ? 0L : this.seedGetter.get(random, extra);
            int count = this.counter.get(random, extra);
            int item = Math.abs(Math.abs((int)seed) + count) % this.items.size();
            long shuffleSeed = seed * (count / this.items.size());
            ArrayList<LootItemInterface> shuffled = new ArrayList<>(this.items);
            if (seed != 0L) {
               Collections.shuffle(shuffled, new Random(shuffleSeed));
            }

            shuffled.get(item).addItems(list, random, 1.0F, extra);
         }
      });
   }

   public interface Counter {
      int get(Random var1, Object... var2);
   }

   public interface SeedGetter {
      long get(Random var1, Object... var2);
   }
}
