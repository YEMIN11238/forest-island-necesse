package necesse.engine.journal;

import necesse.engine.journal.listeners.SettlerRecruitedJournalChallengeListener;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class FreeMageJournalChallenge extends SimpleJournalChallenge implements SettlerRecruitedJournalChallengeListener {
   @Override
   public void onSettlerRecruited(
      ServerClient serverClient, ServerClient recruiterClient, Level recruitedOnLevel, ServerSettlementData settlement, LevelSettler settler, HumanMob humanMob
   ) {
      if (!this.isCompleted(serverClient) && this.isJournalEntryDiscovered(serverClient)) {
         if (recruitedOnLevel.getStringID().equals("dungeon") || recruitedOnLevel.getStringID().equals("dungeonarena")) {
            if (humanMob != null && humanMob.getStringID().equals("magehuman")) {
               this.markCompleted(serverClient);
               serverClient.forceCombineNewStats();
            }
         }
      }
   }
}
