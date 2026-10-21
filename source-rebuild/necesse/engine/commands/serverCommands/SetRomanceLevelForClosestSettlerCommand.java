package necesse.engine.commands.serverCommands;

import java.util.Comparator;
import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.IntParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;

public class SetRomanceLevelForClosestSettlerCommand extends ModularChatCommand {
   public SetRomanceLevelForClosestSettlerCommand() {
      super(
         "setromancelevelforclosestsettler",
         "Finds a nearby settler and sets their romance level to your param",
         PermissionLevel.ADMIN,
         true,
         new CmdParameter("romanceLevel", new IntParameterHandler(), false)
      );
   }

   @Override
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
      int romanceLevel = (Integer)args[0];
      HumanMob settlerMob = this.lookForNearbySettler(serverClient.playerMob);
      if (settlerMob == null) {
         logs.add("Could not find a nearby settler to set romance level for");
      } else {
         PlayerRomanceManager playerRomanceManager = settlerMob.getRomanceManager(serverClient, false);
         if (playerRomanceManager != null) {
            int minRomanceLevel = Math.min(romanceLevel, playerRomanceManager.romanceActivities.size());
            playerRomanceManager.romanceLevel = minRomanceLevel;
            logs.add("Set romance level for " + settlerMob.getSettlerName() + " to " + minRomanceLevel);
         } else {
            logs.add("Could not find a romance manager for " + settlerMob.getSettlerName() + " to set romance level for");
         }
      }
   }

   private HumanMob lookForNearbySettler(Mob owner) {
      int checkForMobsRange = 640;
      Level level = owner.getLevel();
      return level.entityManager
         .mobs
         .streamInRegionsInRange(owner.x, owner.y, checkForMobsRange)
         .filter(m -> m.isHuman && !m.isPlayer)
         .map(m -> (HumanMob)m)
         .filter(m -> m.getDistance(owner) <= checkForMobsRange)
         .min(Comparator.comparingDouble(m -> m.getDistance(owner)))
         .orElse(null);
   }
}
