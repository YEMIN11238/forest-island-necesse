package necesse.entity.levelEvent.IncursionPerkModifiers;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.explosionEvent.MobsExplodeUpgradeShardsPerkExplosionLevelEvent;
import necesse.entity.manager.MobDeathListenerEntityComponent;
import necesse.entity.manager.MobLootTableDropsListenerEntityComponent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.levelBuffManager.LevelModifiers;

public class MobsExplodeUpgradeShardsPerkLevelEvent extends LevelEvent implements MobLootTableDropsListenerEntityComponent, MobDeathListenerEntityComponent {
   public static LootTable altarDustLootTable = new LootTable(LootItem.offset("upgradeshard", 30, 10).splitItems(5, 10));

   public MobsExplodeUpgradeShardsPerkLevelEvent() {
      super(true);
      this.shouldSave = true;
   }

   @Override
   public boolean isNetworkImportant() {
      return true;
   }

   @Override
   public void onLevelMobDropsLoot(Mob mob, Point dropPosition, ArrayList<InventoryItem> drops) {
      if (mob.isHostile && !mob.isSummoned) {
         GameRandom seededRandom = new GameRandom(mob.getUniqueID());
         if (seededRandom.getChance(0.05F)) {
            altarDustLootTable.addItems(drops, GameRandom.globalRandom, this.getLevel().buffManager.getModifier(LevelModifiers.LOOT), mob);
         }
      }
   }

   @Override
   public void onLevelMobDied(Mob mob, Attacker attacker, HashSet<Attacker> attackers) {
      if (mob.isHostile && !mob.isSummoned) {
         GameRandom seededRandom = new GameRandom(mob.getUniqueID());
         if (seededRandom.getChance(0.05F)) {
            MobsExplodeUpgradeShardsPerkExplosionLevelEvent explosion = new MobsExplodeUpgradeShardsPerkExplosionLevelEvent(
               mob.getX(), mob.getY(), 20, new GameDamage(0.0F), false, 0.0F, mob
            );
            this.getLevel().entityManager.events.add(explosion);
         }
      }
   }
}
