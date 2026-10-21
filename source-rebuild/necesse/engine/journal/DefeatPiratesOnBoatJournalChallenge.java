package necesse.engine.journal;

import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.Mob;

public class DefeatPiratesOnBoatJournalChallenge extends MobsKilledJournalChallenge {
   public static String[] BOAT_MOB_STRING_IDS = new String[]{"woodboatmount", "steelboat", "woodboat"};

   public DefeatPiratesOnBoatJournalChallenge() {
      super(5, "piratecaptain", "piraterecruit", "pirateparrot");
   }

   @Override
   public void onMobKilled(ServerClient serverClient, Mob mob) {
      if (!this.isCompleted(serverClient) && this.isJournalEntryDiscovered(serverClient)) {
         if (serverClient.playerMob.isRiding()) {
            boolean validMob = false;

            for (String mobStringID : this.mobStringIDs) {
               if (mob.getStringID().equals(mobStringID)) {
                  validMob = true;
                  break;
               }
            }

            if (validMob) {
               Mob mount = serverClient.playerMob.getMount();
               if (mount != null) {
                  for (String boatStringID : BOAT_MOB_STRING_IDS) {
                     if (mount.getStringID().equals(boatStringID)) {
                        this.addKill(serverClient);
                        return;
                     }
                  }
               }
            }
         }
      }
   }
}
