package necesse.inventory.container.mobCommands;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.level.maps.levelData.settlementData.settler.CommandMob;

public class CommandGuardAction extends CommandCustomAction {
   public CommandGuardAction(MobCommandsHandler handler) {
      super(handler, false);
   }

   public void runAndSend(Collection<Integer> mobUniqueIDs, int x, int y) {
      PacketWriter writer = this.setupPacket(mobUniqueIDs);
      writer.putNextBoolean(false);
      writer.putNextInt(x);
      writer.putNextInt(y);
      this.runAndSendAction(writer.getPacket());
   }

   public void runAndSend(Collection<Integer> mobUniqueIDs, ArrayList<Point> points) {
      PacketWriter writer = this.setupPacket(mobUniqueIDs);
      writer.putNextBoolean(true);
      writer.putNextShortUnsigned(points.size());

      for (Point point : points) {
         writer.putNextInt(point.x);
         writer.putNextInt(point.y);
      }

      this.runAndSendAction(writer.getPacket());
   }

   @Override
   public void executePacket(PacketReader reader, ServerClient client, ArrayList<CommandMob> mobs) {
      if (reader.getNextBoolean()) {
         int size = reader.getNextShortUnsigned();
         if (size <= 0) {
            return;
         }

         ArrayList<Point> points = new ArrayList<>(size);

         for (int i = 0; i < size; i++) {
            int x = reader.getNextInt();
            int y = reader.getNextInt();
            points.add(new Point(x, y));
         }

         if (size < mobs.size()) {
            double mobsPerPoint = (double)mobs.size() / size;
            double mobCounter = 0.0;

            for (CommandMob mob : mobs) {
               Point point = points.get(Math.min((int)mobCounter, points.size() - 1));
               mob.commandGuard(client, point.x, point.y);
               mobCounter += mobsPerPoint;
            }
         } else {
            for (int i = 0; i < mobs.size(); i++) {
               CommandMob mob = mobs.get(i);
               Point point = points.get(i);
               mob.commandGuard(client, point.x, point.y);
            }
         }
      } else {
         int x = reader.getNextInt();
         int y = reader.getNextInt();

         for (CommandMob mob : mobs) {
            mob.commandGuard(client, x, y);
         }
      }
   }
}
