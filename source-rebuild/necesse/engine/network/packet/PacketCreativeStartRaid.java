package necesse.engine.network.packet;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.LevelEventRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.entity.levelEvent.settlementRaidEvent.SettlementRaidLevelEvent;
import necesse.gfx.forms.presets.creative.CreativeToolsTab;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementRaidOptions;

public class PacketCreativeStartRaid extends PacketCreativeCheck {
   public final int raidEventID;
   public final SettlementRaidLevelEvent.RaidDir direction;
   public final float difficulty;

   public PacketCreativeStartRaid(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      this.raidEventID = reader.getNextShort();
      if (reader.getNextBoolean()) {
         this.direction = reader.getNextEnum(SettlementRaidLevelEvent.RaidDir.class);
      } else {
         this.direction = null;
      }

      this.difficulty = reader.getNextFloat();
   }

   public PacketCreativeStartRaid(int raidEventID, SettlementRaidLevelEvent.RaidDir direction, float difficulty) {
      this.raidEventID = raidEventID;
      this.direction = direction;
      this.difficulty = difficulty;
      PacketWriter writer = new PacketWriter(this);
      writer.putNextShort((short)raidEventID);
      writer.putNextBoolean(direction != null);
      if (direction != null) {
         writer.putNextEnum(direction);
      }

      writer.putNextFloat(difficulty);
   }

   @Override
   public void processServer(NetworkPacket packet, Server server, ServerClient client) {
      if (checkCreativeAndSendUpdate(server, client)) {
         SettlementRaidLevelEvent.RaidDir direction = this.direction;
         if (direction == null) {
            direction = GameRandom.globalRandom.getOneOf(SettlementRaidLevelEvent.RaidDir.values());
         }

         Level level = client.getLevel();
         ServerSettlementData settlementData = SettlementsWorldData.getSettlementsData(level)
            .getServerDataAtTile(level.getIdentifier(), client.playerMob.getTileX(), client.playerMob.getTileY());
         if (settlementData == null) {
            client.sendChatMessage(new LocalMessage("ui", "creativestartraidfailed"));
         } else {
            if (settlementData.isRaidOngoing()) {
               client.sendChatMessage(new LocalMessage("ui", "creativestartraidalreadygoing"));
            } else {
               SettlementRaidOptions options = settlementData.getRaidOptions(false);
               options.difficultyModifier = this.difficulty;
               SettlementRaidLevelEvent raidEvent;
               if (this.raidEventID == -1) {
                  raidEvent = (SettlementRaidLevelEvent)LevelEventRegistry.getEvent(CreativeToolsTab.getRandomRaidType().levelEventID);
               } else {
                  raidEvent = (SettlementRaidLevelEvent)LevelEventRegistry.getEvent(this.raidEventID);
               }

               if (raidEvent == null) {
                  raidEvent = settlementData.getNextRaid(options);
               }

               boolean raidSuccess = settlementData.spawnRaid(raidEvent, options);
               if (raidSuccess) {
                  server.network
                     .sendToAllClients(
                        new PacketChatMessage(
                           new LocalMessage(
                              "ui",
                              "creativestartraidsuccessful",
                              "player",
                              client.getName(),
                              "settlement",
                              settlementData.networkData.getSettlementName().translate(),
                              "direction",
                              direction.displayName.translate(),
                              "difficulty",
                              (int)(this.difficulty * 100.0F)
                           )
                        )
                     );
               } else {
                  client.sendChatMessage(new LocalMessage("ui", "creativestartraidfailed"));
               }
            }
         }
      }
   }
}
