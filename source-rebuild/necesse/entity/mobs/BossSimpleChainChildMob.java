package necesse.entity.mobs;

import necesse.entity.mobs.hostile.bosses.BossMob;

public class BossSimpleChainChildMob<T extends Mob, C extends BossSimpleChainChildMob<T, C>> extends HostileSimpleChainChildMob<T, C> {
   public BossSimpleChainChildMob(int health) {
      super(health);
      this.canDespawn = false;
      this.shouldSave = false;
   }

   @Override
   public int getRespawnTime() {
      return BossMob.getBossRespawnTime(this);
   }
}
