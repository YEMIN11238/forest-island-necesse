package necesse.inventory.lootTable.lootItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;
import necesse.engine.GameLog;
import necesse.engine.network.server.ServerClient;
import necesse.engine.playerStats.stats.DamageTypesStat;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.gameDamageType.DamageType;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootList;
import necesse.inventory.lootTable.LootTable;

public class DamageRelativeRotationLootItem implements LootItemInterface {
   protected HashMap<String, LootItemInterface> items = new HashMap<>();
   protected RotationLootItem.Counter counter;

   public DamageRelativeRotationLootItem(RotationLootItem.Counter counter) {
      if (counter == null) {
         counter = (random, extra) -> {
            ServerClient client = LootTable.expectExtra(ServerClient.class, extra, 1);
            if (client == null) {
               return random.nextInt(Integer.MAX_VALUE);
            }

            Mob mob = LootTable.expectExtra(Mob.class, extra, 0);
            return mob == null ? random.nextInt(Integer.MAX_VALUE) : Math.max(client.characterStats().mob_kills.getKills(mob.getStringID()) - 1, 0);
         };
      }

      this.counter = counter;
   }

   public DamageRelativeRotationLootItem() {
      this(null);
   }

   public DamageRelativeRotationLootItem setLoot(String damageTypeStringID, LootItemInterface loot) {
      this.items.put(damageTypeStringID, loot);
      return this;
   }

   public DamageRelativeRotationLootItem setMeleeLoot(LootItemInterface loot) {
      return this.setLoot("melee", loot);
   }

   public DamageRelativeRotationLootItem setRangedLoot(LootItemInterface loot) {
      return this.setLoot("ranged", loot);
   }

   public DamageRelativeRotationLootItem setMagicLoot(LootItemInterface loot) {
      return this.setLoot("magic", loot);
   }

   public DamageRelativeRotationLootItem setSummonLoot(LootItemInterface loot) {
      return this.setLoot("summon", loot);
   }

   @Override
   public void addPossibleLoot(LootList list, Object... extra) {
      for (LootItemInterface item : this.items.values()) {
         item.addPossibleLoot(list, extra);
      }
   }

   @Override
   public void addItems(List<InventoryItem> list, GameRandom random, float lootMultiplier, Object... extra) {
      LootTable.runMultiplied(
         random,
         lootMultiplier,
         () -> {
            if (!this.items.isEmpty()) {
               ServerClient client = LootTable.expectExtra(ServerClient.class, extra, 1);
               long seed;
               DamageType mostDamageType;
               if (client != null) {
                  DamageRelativeRotationLootItem.PlayerDamageDealtGetter damageDealtGetter = LootTable.expectExtra(
                     DamageRelativeRotationLootItem.PlayerDamageDealtGetter.class, extra, 0
                  );
                  if (damageDealtGetter == null) {
                     damageDealtGetter = this::getMostCharacterDamageDealt;
                  }

                  mostDamageType = damageDealtGetter.getMostDamageTypeDealt(client);
                  Mob mob = LootTable.expectExtra(Mob.class, extra, 0);
                  seed = GameUtils.longHash(
                     client.authentication, client.getServer().world.worldEntity.getWorldSeed(), mob == null ? null : mob.getStringID().hashCode()
                  );
               } else {
                  mostDamageType = null;
                  GameLog.warn.println("DamageRelativeRotationLootItem: No server client provided in extra parameters, cannot determine damage type!");
                  seed = random.nextInt();
               }

               int count = this.counter.get(random, extra);
               if (count == 0 && mostDamageType != null) {
                  LootItemInterface items = this.items.get(mostDamageType.getStringID());
                  if (items != null) {
                     items.addItems(list, random, 1.0F, extra);
                     return;
                  }

                  count++;
               }

               int item = Math.floorMod((int)seed + count - 1, this.items.size());
               long shuffleSeed = seed * ((count - 1) / this.items.size());
               ArrayList<LootItemInterface> shuffled = new ArrayList<>(this.items.values());
               if (seed != 0L) {
                  Collections.shuffle(shuffled, new Random(shuffleSeed));
               }

               shuffled.get(item).addItems(list, random, 1.0F, extra);
            }
         }
      );
   }

   public DamageType getMostCharacterDamageDealt(ServerClient client) {
      DamageTypesStat stat = client.characterStats().type_damage_dealt;
      return stat.streamEach().max(Comparator.comparingInt(Entry::getValue)).map(Entry::getKey).orElse(null);
   }

   @FunctionalInterface
   public interface PlayerDamageDealtGetter {
      DamageType getMostDamageTypeDealt(ServerClient var1);
   }
}
