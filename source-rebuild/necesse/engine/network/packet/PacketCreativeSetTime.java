package necesse.engine.network.packet;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

public class PacketCreativeSetTime extends PacketCreativeCheck {
   public PacketCreativeSetTime.TimeEnum newTime;

   public PacketCreativeSetTime(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      this.newTime = reader.getNextEnum(PacketCreativeSetTime.TimeEnum.class);
   }

   public PacketCreativeSetTime(PacketCreativeSetTime.TimeEnum newTime) {
      this.newTime = newTime;
      PacketWriter writer = new PacketWriter(this);
      writer.putNextEnum(newTime);
   }

   @Override
   public void processServer(NetworkPacket packet, Server server, ServerClient client) {
      if (checkCreativeAndSendUpdate(server, client)) {
         long oldTime = server.world.worldEntity.getWorldTime();
         switch (this.newTime) {
            case Dawn:
               server.world.setDawn();
               break;
            case Morning:
               server.world.setMorning();
               break;
            case Midday:
               server.world.setMidday();
               break;
            case Dusk:
               server.world.setDusk();
               break;
            case Night:
               server.world.setNight();
               break;
            case Midnight:
               server.world.setMidnight();
         }

         if (server.world.worldEntity.getWorldTime() != oldTime) {
            server.world.simulateWorldTime(server.world.worldEntity.getWorldTime() - oldTime, true);
            client.sendChatMessage(
               new LocalMessage(
                  "ui",
                  "creativetimechanged",
                  "player",
                  client.getName(),
                  "time",
                  server.world.worldEntity.getDayTimeInt(),
                  "day",
                  server.world.worldEntity.getDay()
               )
            );
         }
      }
   }

   public enum TimeEnum {
      Dawn,
      Morning,
      Midday,
      Dusk,
      Night,
      Midnight;
   }
}
