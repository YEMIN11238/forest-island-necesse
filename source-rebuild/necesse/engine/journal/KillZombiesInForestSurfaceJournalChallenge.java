package necesse.engine.journal;

import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.Mob;
import necesse.level.maps.Level;

public class KillZombiesInForestSurfaceJournalChallenge extends MobsKilledJournalChallenge {
   public KillZombiesInForestSurfaceJournalChallenge() {
      super(25, "zombie", "zombiearcher");
   }

   @Override
   public void onMobKilled(ServerClient serverClient, Mob mob) {
      Level level = mob.getLevel();
      if (!level.isCave) {
         if (JournalChallengeUtils.isForestBiome(level.getBiome(mob.getTileX(), mob.getTileY()))) {
            super.onMobKilled(serverClient, mob);
         }
      }
   }
}
