package necesse.inventory.container.settlement.actions;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketPlayerTeamRequestReceive;
import necesse.engine.network.server.ServerClient;
import necesse.engine.team.PlayerTeam;
import necesse.inventory.container.customAction.BooleanCustomAction;
import necesse.inventory.container.settlement.SettlementDependantContainer;
import necesse.inventory.container.settlement.events.SettlementDataEvent;
import necesse.inventory.container.settlement.events.SettlementRemovedEvent;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class RequestJoinSettlementAction extends BooleanCustomAction {
   public final SettlementDependantContainer container;

   public RequestJoinSettlementAction(SettlementDependantContainer container) {
      this.container = container;
   }

   @Override
   protected void run(boolean assumedIsPublic) {
      if (this.container.client.isServer()) {
         ServerClient client = this.container.client.getServerClient();
         ServerSettlementData serverData = this.container.getServerData();
         if (serverData != null) {
            long ownerAuth = serverData.networkData.getOwnerAuth();
            if (ownerAuth != -1L) {
               int teamID = serverData.networkData.getTeamID();
               PlayerTeam team = teamID == -1 ? null : client.getServer().world.getTeams().getTeam(teamID);
               if (team == null) {
                  if (assumedIsPublic) {
                     new SettlementDataEvent(serverData).applyAndSendToClient(client);
                  }

                  ServerClient ownerClient = client.getServer().getClientByAuth(ownerAuth);
                  if (ownerClient != null) {
                     ownerClient.joinRequests.add(client.authentication);
                     ownerClient.sendPacket(new PacketPlayerTeamRequestReceive(client.authentication, client.getName()));
                  } else {
                     client.sendChatMessage(new LocalMessage("ui", "teamownerisnotonline"));
                  }
               } else {
                  if (team.isPublic()) {
                     PlayerTeam.addMember(client.getServer(), team, client.authentication);
                  } else {
                     PlayerTeam.addJoinRequest(client.getServer(), team, client.authentication);
                  }

                  if (team.isPublic() != assumedIsPublic) {
                     new SettlementDataEvent(serverData).applyAndSendToClient(client);
                  }
               }
            }
         } else {
            new SettlementRemovedEvent(0).applyAndSendToClient(client);
         }
      }
   }
}
