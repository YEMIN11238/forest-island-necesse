package necesse.engine.journal.listeners;

import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.friendly.human.HumanMob;

public interface AdventurePartyChangedJournalChallengeListener {
   void onPartyMemberAdded(ServerClient var1, HumanMob var2);

   void onPartyMemberRemoved(ServerClient var1, HumanMob var2, boolean var3, boolean var4, boolean var5);
}
