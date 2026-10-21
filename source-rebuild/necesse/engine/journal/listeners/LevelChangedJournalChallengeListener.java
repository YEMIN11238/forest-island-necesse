package necesse.engine.journal.listeners;

import necesse.engine.network.server.ServerClient;
import necesse.level.maps.Level;

public interface LevelChangedJournalChallengeListener {
   void onLevelChanged(ServerClient var1, Level var2, Level var3);
}
