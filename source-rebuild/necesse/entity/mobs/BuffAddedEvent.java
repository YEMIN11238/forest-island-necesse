package necesse.entity.mobs;

import necesse.entity.mobs.buffs.ActiveBuff;

public class BuffAddedEvent extends MobGenericEvent {
   public final ActiveBuff ab;
   public final ActiveBuff previousAb;
   public final boolean sendUpdatePacket;

   public BuffAddedEvent(ActiveBuff ab, ActiveBuff previousAb, boolean sendUpdatePacket) {
      this.ab = ab;
      this.previousAb = previousAb;
      this.sendUpdatePacket = sendUpdatePacket;
   }
}
