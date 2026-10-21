package necesse.inventory.container.mobCommands;

import java.util.ArrayList;
import java.util.Collection;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.world.worldData.SettlersWorldData;
import necesse.entity.mobs.Mob;
import necesse.inventory.container.customAction.ContainerCustomAction;
import necesse.level.maps.levelData.settlementData.settler.CommandMob;

public abstract class CommandCustomAction extends ContainerCustomAction {
   public final MobCommandsHandler handler;
   public final boolean isOnlySetting;

   public CommandCustomAction(MobCommandsHandler handler, boolean isOnlySetting) {
      this.handler = handler;
      this.isOnlySetting = isOnlySetting;
   }

   protected PacketWriter setupPacket(Collection<Integer> mobUniqueIDs) {
      Packet content = new Packet();
      PacketWriter writer = new PacketWriter(content);
      writer.putNextShortUnsigned(mobUniqueIDs.size());

      for (int uniqueID : mobUniqueIDs) {
         writer.putNextInt(uniqueID);
      }

      return writer;
   }

   protected boolean preCheckIfCanExecute(ServerClient client) {
      return true;
   }

   @Override
   public void executePacket(PacketReader reader) {
      if (this.handler.container.client.isServer()) {
         ServerClient client = this.handler.container.client.getServerClient();
         if (this.preCheckIfCanExecute(client)) {
            int totalMobs = reader.getNextShortUnsigned();
            ArrayList<CommandMob> mobs = new ArrayList<>(totalMobs);
            boolean sendUpdate = false;

            for (int i = 0; i < totalMobs; i++) {
               int mobUniqueID = reader.getNextInt();
               Mob mob = client.getLevel().entityManager.mobs.get(mobUniqueID, false);
               if (mob == null) {
                  mob = client.adventureParty.getMemberMob(mobUniqueID);
               }

               if (mob == null) {
                  SettlersWorldData settlersData = SettlersWorldData.getSettlersData(client.getServer());
                  mob = settlersData.getSettler(mobUniqueID);
               }

               if (mob instanceof CommandMob && ((CommandMob)mob).canBeCommanded(client, this.isOnlySetting)) {
                  mobs.add((CommandMob)mob);
               } else {
                  sendUpdate = true;
               }
            }

            this.executePacket(reader, client, mobs);
            if (sendUpdate) {
               this.handler.sendClientOutOfSyncPacket(client);
            }
         }
      }
   }

   public abstract void executePacket(PacketReader var1, ServerClient var2, ArrayList<CommandMob> var3);
}
