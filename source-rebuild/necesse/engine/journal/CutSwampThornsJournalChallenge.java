package necesse.engine.journal;

import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.Attacker;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;

public class CutSwampThornsJournalChallenge extends ObjectsDestroyedJournalChallenge {
   public CutSwampThornsJournalChallenge() {
      super(200, true, "thorns");
   }

   @Override
   public void onObjectDestroyed(GameObject object, Level level, int layerID, int tileX, int tileY, int objectRotation, Attacker attacker, ServerClient client) {
      if (level.isCave && JournalChallengeUtils.isSwampBiome(level.getBiome(tileX, tileY))) {
         super.onObjectDestroyed(object, level, layerID, tileX, tileY, objectRotation, attacker, client);
      }
   }
}
