package necesse.inventory.lootTable.lootItem;

import java.util.function.BiFunction;
import java.util.function.Function;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootTable;

public class MobConditionLootItemList extends ConditionLootItemList {
   public MobConditionLootItemList(BiFunction<GameRandom, Mob, Boolean> condition, LootItemInterface... items) {
      super((random, objects) -> {
         Mob mob = LootTable.expectExtra(Mob.class, objects, 0);
         return mob != null ? condition.apply(random, mob) : false;
      }, items);
   }

   public MobConditionLootItemList(Function<Mob, Boolean> condition, LootItemInterface... items) {
      super((random, objects) -> {
         Mob mob = LootTable.expectExtra(Mob.class, objects, 0);
         return mob != null ? condition.apply(mob) : false;
      }, items);
   }
}
