package necesse.engine.network.client.loading;

import java.util.LinkedList;
import necesse.engine.GameLog;
import necesse.engine.network.PacketReader;
import necesse.engine.network.packet.PacketObjectEntity;
import necesse.engine.network.packet.PacketObjectEntityError;
import necesse.engine.network.packet.PacketRequestObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;

public class ClientObjectEntityLoading extends ClientLoadingUtil {
   private final LinkedList<ClientObjectEntityLoading.Requested> requests = new LinkedList<>();

   public ClientObjectEntityLoading(ClientLoading loading) {
      super(loading);
   }

   public void tick() {
      if (!this.isWaiting() && !this.requests.isEmpty()) {
         long cTime = this.client.worldEntity.getLocalTime();

         while (!this.requests.isEmpty()) {
            ClientObjectEntityLoading.Requested first = this.requests.getFirst();
            if (first.time + 2000L >= cTime) {
               break;
            }

            this.client.network.sendPacket(new PacketRequestObjectEntity(this.client.getLevel(), first.x, first.y));
            this.requests.removeFirst();
            this.requests.add(new ClientObjectEntityLoading.Requested(first.x, first.y, cTime));
         }

         this.setWait(250);
      }
   }

   public void reset() {
      this.requests.clear();
   }

   public void addObjectEntityRequest(int x, int y) {
      this.requests.addFirst(new ClientObjectEntityLoading.Requested(x, y, -2147483648L));
   }

   public void submitObjectEntityPacket(PacketObjectEntity p) {
      if (this.client.getLevel().getObjectID(p.tileX, p.tileY) == p.objectID) {
         ObjectEntity ent = this.client.getLevel().entityManager.getObjectEntity(p.tileX, p.tileY);
         if (ent != null) {
            ent.applyContentPacket(new PacketReader(p.content));
         } else {
            GameLog.warn.println("Client received unknown object entity packet at " + p.tileX + ", " + p.tileY);
         }
      } else {
         GameLog.warn.println("Client received wrong object entity packet at " + p.tileX + ", " + p.tileY);
      }

      this.requests.removeIf(r -> r.x == p.tileX && r.y == p.tileY);
   }

   public void submitObjectEntityErrorPacket(PacketObjectEntityError p) {
      this.requests.removeIf(r -> r.x == p.tileX && r.y == p.tileY);
   }

   private static class Requested {
      public final int x;
      public final int y;
      public long time;

      public Requested(int x, int y, long time) {
         this.x = x;
         this.y = y;
         this.time = time;
      }
   }
}
