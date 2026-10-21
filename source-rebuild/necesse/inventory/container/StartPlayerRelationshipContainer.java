package necesse.inventory.container;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketClientRelationshipData;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.util.GameRandom;
import necesse.gfx.GameColor;
import necesse.inventory.container.customAction.EmptyCustomAction;

public class StartPlayerRelationshipContainer extends Container {
   public final int queryUniqueID;
   public final long targetAuthentication;
   public final String targetName;
   public final boolean isAlreadyInRelationship;
   public final boolean isReceivingEnd;
   public final EmptyCustomAction acceptButton;
   public final EmptyCustomAction declineButton;
   public boolean isAcceptedFromOtherParty;
   protected boolean hasSentResponse;

   public StartPlayerRelationshipContainer(final NetworkClient client, int uniqueSeed, PacketReader reader) {
      super(client, uniqueSeed);
      this.queryUniqueID = reader.getNextInt();
      this.targetAuthentication = reader.getNextLong();
      this.targetName = reader.getNextString();
      this.isAlreadyInRelationship = reader.getNextBoolean();
      this.isReceivingEnd = reader.getNextBoolean();
      this.acceptButton = this.registerAction(
         new EmptyCustomAction() {
            @Override
            protected void run() {
               StartPlayerRelationshipContainer.this.hasSentResponse = true;
               if (client.isServer()) {
                  if (StartPlayerRelationshipContainer.this.isAcceptedFromOtherParty) {
                     ServerClient me = client.getServerClient();
                     Server server = me.getServer();
                     ServerClient them = server.getClientByAuth(StartPlayerRelationshipContainer.this.targetAuthentication);
                     if (them == null) {
                        String savedClientName = server.getSavedClientName(StartPlayerRelationshipContainer.this.targetAuthentication, "N/A");
                        GameMessage message = new GameMessageBuilder()
                           .append(GameColor.RED.getColorCode())
                           .append(new LocalMessage("romance", "playerdeclined", "name", savedClientName));
                        me.sendChatMessage(message);
                     } else {
                        them.relationshipData.setIsInRelationshipWith(me);
                        me.relationshipData.setIsInRelationshipWith(them);
                        server.network.sendToAllClients(new PacketClientRelationshipData(them));
                        server.network.sendToAllClients(new PacketClientRelationshipData(me));
                        GameMessage myMessage = new GameMessageBuilder()
                           .append(GameColor.GREEN.getColorCode())
                           .append(new LocalMessage("romance", "playeraccepted", "name", them.getName()));
                        GameMessage theirMessage = new GameMessageBuilder()
                           .append(GameColor.GREEN.getColorCode())
                           .append(new LocalMessage("romance", "playeraccepted", "name", me.getName()));
                        me.sendChatMessage(myMessage);
                        them.sendChatMessage(theirMessage);
                     }
                  } else {
                     ServerClient serverClient = client.getServerClient();
                     StartPlayerRelationshipContainer targetContainer = StartPlayerRelationshipContainer.this.getTargetContainer(serverClient.getServer());
                     if (targetContainer != null) {
                        targetContainer.isAcceptedFromOtherParty = true;
                     }
                  }
               }

               StartPlayerRelationshipContainer.this.close();
            }
         }
      );
      this.declineButton = this.registerAction(new EmptyCustomAction() {
         @Override
         protected void run() {
            StartPlayerRelationshipContainer.this.hasSentResponse = true;
            if (client.isServer()) {
               StartPlayerRelationshipContainer.this.sendServerReject();
            }

            StartPlayerRelationshipContainer.this.close();
         }
      });
   }

   public StartPlayerRelationshipContainer getTargetContainer(Server server) {
      ServerClient target = server.getClientByAuth(this.targetAuthentication);
      if (target != null) {
         Container container = target.getContainer();
         if (container instanceof StartPlayerRelationshipContainer) {
            StartPlayerRelationshipContainer targetContainer = (StartPlayerRelationshipContainer)container;
            if (targetContainer.queryUniqueID == this.queryUniqueID) {
               return targetContainer;
            }
         }
      }

      return null;
   }

   public void sendServerReject() {
      if (!this.client.isServer()) {
         throw new IllegalStateException("sendServerReject can only be called on the server side");
      }

      ServerClient serverClient = this.client.getServerClient();
      StartPlayerRelationshipContainer targetContainer = this.getTargetContainer(serverClient.getServer());
      if (targetContainer != null) {
         ServerClient targetServerClient = targetContainer.client.getServerClient();
         GameMessage message = new GameMessageBuilder()
            .append(GameColor.RED.getColorCode())
            .append(new LocalMessage("romance", "playerdeclined", "name", serverClient.getName()));
         targetServerClient.sendChatMessage(message);
         targetContainer.hasSentResponse = true;
         targetContainer.close();
      } else {
         ServerClient target = serverClient.getServer().getClientByAuth(this.targetAuthentication);
         if (target != null) {
            GameMessage message = new GameMessageBuilder()
               .append(GameColor.RED.getColorCode())
               .append(new LocalMessage("romance", "playerdeclined", "name", serverClient.getName()));
            target.sendChatMessage(message);
         }
      }
   }

   @Override
   public void onClose() {
      super.onClose();
      if (!this.hasSentResponse && this.client.isServer()) {
         this.sendServerReject();
      }
   }

   public static void openContainer(ServerClient from, ServerClient to) {
      Packet fromPacket = new Packet();
      Packet toPacket = new Packet();
      PacketWriter toWriter = new PacketWriter(toPacket);
      PacketWriter fromWriter = new PacketWriter(fromPacket);
      int uniqueID = GameRandom.globalRandom.nextInt();
      toWriter.putNextInt(uniqueID);
      fromWriter.putNextInt(uniqueID);
      toWriter.putNextLong(from.authentication);
      fromWriter.putNextLong(to.authentication);
      toWriter.putNextString(from.getName());
      fromWriter.putNextString(to.getName());
      toWriter.putNextBoolean(to.relationshipData.isInAnyRelationship());
      fromWriter.putNextBoolean(from.relationshipData.isInAnyRelationship());
      toWriter.putNextBoolean(true);
      fromWriter.putNextBoolean(false);
      ContainerRegistry.openAndSendContainer(from, new PacketOpenContainer(ContainerRegistry.START_PLAYER_RELATIONSHIP_CONTAINER, fromPacket));
      ContainerRegistry.openAndSendContainer(to, new PacketOpenContainer(ContainerRegistry.START_PLAYER_RELATIONSHIP_CONTAINER, toPacket));
   }
}
