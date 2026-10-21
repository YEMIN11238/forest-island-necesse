package necesse.entity.mobs;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

public class MobHitCooldowns {
   private final HashMap<Integer, Long> hitCooldowns = new HashMap<>();
   public int hitCooldown;

   public MobHitCooldowns(int hitCooldown) {
      this.hitCooldown = hitCooldown;
   }

   public MobHitCooldowns() {
      this(500);
   }

   public void setupPacket(PacketWriter writer, boolean onlyCooldowns, long currentTime) {
      if (onlyCooldowns) {
         List<Entry<Integer, Long>> list = this.hitCooldowns.entrySet().stream().filter(ex -> (Long)ex.getValue() > currentTime).collect(Collectors.toList());
         writer.putNextShortUnsigned(list.size());

         for (Entry<Integer, Long> e : list) {
            writer.putNextInt(e.getKey());
            writer.putNextInt((int)(e.getValue() - currentTime));
         }
      } else {
         writer.putNextShortUnsigned(this.hitCooldowns.size());

         for (Entry<Integer, Long> e : this.hitCooldowns.entrySet()) {
            writer.putNextInt(e.getKey());
            writer.putNextInt((int)(e.getValue() - currentTime));
         }
      }
   }

   public void applyPacket(PacketReader reader, long currentTime) {
      int size = reader.getNextShortUnsigned();

      for (int i = 0; i < size; i++) {
         int mobUniqueID = reader.getNextInt();
         long cooldown = currentTime + reader.getNextInt();
         this.hitCooldowns.put(mobUniqueID, cooldown);
      }
   }

   public boolean canHit(Mob target, long currentTime, int toleration) {
      long cooldown = this.hitCooldowns.getOrDefault(target.getHitCooldownUniqueID(), 0L);
      return cooldown - toleration <= currentTime;
   }

   public boolean canHit(Mob target, int toleration) {
      return this.canHit(target, target.getTime(), toleration);
   }

   public boolean canHit(Mob target, long currentTime) {
      return this.canHit(target, currentTime, 0);
   }

   public boolean canHit(Mob target) {
      return this.canHit(target, target.getTime());
   }

   public void startCooldown(Mob target, long currentTime) {
      this.hitCooldowns.put(target.getHitCooldownUniqueID(), currentTime + this.hitCooldown);
   }

   public void startCooldown(Mob target) {
      this.startCooldown(target, target.getTime());
   }

   public void resetCooldown(Mob target) {
      this.hitCooldowns.remove(target.getHitCooldownUniqueID());
   }

   public void resetCooldowns() {
      this.hitCooldowns.clear();
   }
}
