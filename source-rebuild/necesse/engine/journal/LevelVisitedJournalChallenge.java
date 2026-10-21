package necesse.engine.journal;

import necesse.engine.journal.listeners.LevelChangedJournalChallengeListener;
import necesse.engine.network.server.ServerClient;
import necesse.level.maps.Level;

public class LevelVisitedJournalChallenge extends SimpleJournalChallenge implements LevelChangedJournalChallengeListener {
   protected String[] levelStringIDs;

   public LevelVisitedJournalChallenge(String... levelStringIDs) {
      this.levelStringIDs = levelStringIDs;
   }

   @Override
   public void onLevelChanged(ServerClient serverClient, Level oldLevel, Level newLevel) {
      if (!this.isCompleted(serverClient) && this.isJournalEntryDiscovered(serverClient)) {
         for (String levelStringID : this.levelStringIDs) {
            if (newLevel.getStringID().equals(levelStringID)) {
               this.markCompleted(serverClient);
               serverClient.forceCombineNewStats();
               return;
            }
         }
      }
   }
}
