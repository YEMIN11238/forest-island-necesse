package necesse.entity.mobs.hostile.bosses;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.hostile.HostileWormMobHead;

public abstract class BossWormMobHead<T extends BossWormMobBody<B, T>, B extends BossWormMobHead<T, B>> extends HostileWormMobHead<T, B> {
   public BossWormMobHead(int health, float waveLength, float distPerMoveSound, int totalBodyParts, float heightMultiplier, float heightOffset) {
      super(health, waveLength, distPerMoveSound, totalBodyParts, heightMultiplier, heightOffset);
      this.canDespawn = false;
      this.shouldSave = false;
      this.removeWhenTilesOutOfLevel = 200;
   }

   @Override
   public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
      return false;
   }

   @Override
   public int getRespawnTime() {
      return BossMob.getBossRespawnTime(this);
   }
}
