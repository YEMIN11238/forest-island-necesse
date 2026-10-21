package necesse.entity.projectile;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.entity.levelEvent.explosionEvent.DynamiteExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.ExplosionEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;

public class RCToySoldierDynamiteProjectile extends BombProjectile {
   private int fuseTime = 8000;

   public RCToySoldierDynamiteProjectile() {
   }

   public RCToySoldierDynamiteProjectile(float x, float y, float targetX, float targetY, int speed, int distance, GameDamage damage, int fuseTime, Mob owner) {
      super(x, y, targetX, targetY, speed, distance, damage, owner);
      this.fuseTime = fuseTime;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.fuseTime);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.fuseTime = reader.getNextInt();
   }

   @Override
   public void init() {
      super.init();
      this.stopsRotatingOnStationary = true;
      this.throwHeight = this.distance * 0.4F;
   }

   @Override
   public int getFuseTime() {
      return this.fuseTime;
   }

   @Override
   public float getParticleAngle() {
      return 220.0F;
   }

   @Override
   public float getParticleDistance() {
      return 14.0F;
   }

   @Override
   public ExplosionEvent getExplosionEvent(float x, float y) {
      return new DynamiteExplosionEvent(x, y, 100, this.getDamage(), false, false, 0.0F, null);
   }
}
