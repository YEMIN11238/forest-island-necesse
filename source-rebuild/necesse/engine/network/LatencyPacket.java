package necesse.engine.network;

public class LatencyPacket {
   public NetworkPacket packet;
   private long time;
   private int latency;

   public LatencyPacket(NetworkPacket packet, int latency) {
      this.packet = packet;
      this.latency = latency;
      this.time = System.currentTimeMillis();
   }

   public long getTime() {
      return this.time;
   }

   public int getLatency() {
      return this.latency;
   }

   public long getReadyTime() {
      return this.time + this.latency;
   }

   public boolean isReady() {
      return this.getReadyTime() <= System.currentTimeMillis();
   }
}
