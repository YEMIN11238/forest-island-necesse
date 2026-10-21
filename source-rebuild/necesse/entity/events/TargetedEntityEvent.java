package necesse.entity.events;

import java.util.function.Supplier;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.GameUtils;
import necesse.entity.Entity;
import necesse.entity.mobs.Mob;
import necesse.level.maps.Level;

public abstract class TargetedEntityEvent extends EntityEvent {
   private final Supplier<Level> levelSupplier;

   public TargetedEntityEvent(Supplier<Level> levelSupplier) {
      this.levelSupplier = levelSupplier;
   }

   public TargetedEntityEvent(Entity entity) {
      this(entity::getLevel);
   }

   public void runAndSend(Mob target) {
      Packet content = new Packet();
      PacketWriter writer = new PacketWriter(content);
      writer.putNextInt(target.getUniqueID());
      this.runAndSendAbility(content);
   }

   @Override
   public void executePacket(PacketReader reader) {
      int uniqueID = reader.getNextInt();
      Mob target = GameUtils.getLevelMob(uniqueID, this.levelSupplier.get());
      this.run(target);
   }

   protected abstract void run(Mob var1);
}
