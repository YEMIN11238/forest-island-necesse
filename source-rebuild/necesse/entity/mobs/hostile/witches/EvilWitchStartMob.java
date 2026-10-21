package necesse.entity.mobs.hostile.witches;

import java.awt.Point;
import necesse.engine.registries.MobRegistry;
import necesse.entity.mobs.Mob;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.Waystone;

public class EvilWitchStartMob extends Mob {
   public EvilWitchStartMob() {
      super(100000);
      this.isHostile = true;
   }

   @Override
   public void init() {
      super.init();
      Level level = this.getLevel();
      if (this.isServer()) {
         Mob evilWitch = MobRegistry.getMob("evilwitchflask", level);
         Point spawnPos = Waystone.findTeleportLocation(level, this.getTileX(), this.getTileY(), evilWitch);
         float var10002 = spawnPos.x;
         this.getLevel().entityManager.addMob(evilWitch, var10002, spawnPos.y);
         evilWitch = MobRegistry.getMob("evilwitchbow", level);
         spawnPos = Waystone.findTeleportLocation(level, this.getTileX(), this.getTileY(), evilWitch);
         var10002 = spawnPos.x;
         this.getLevel().entityManager.addMob(evilWitch, var10002, spawnPos.y);
         evilWitch = MobRegistry.getMob("evilwitchgreatsword", level);
         spawnPos = Waystone.findTeleportLocation(level, this.getTileX(), this.getTileY(), evilWitch);
         var10002 = spawnPos.x;
         this.getLevel().entityManager.addMob(evilWitch, var10002, spawnPos.y);
      }

      this.remove();
   }

   @Override
   public LootTable getLootTable() {
      return EvilWitchMob.lootTable;
   }
}
