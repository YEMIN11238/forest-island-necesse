package necesse.entity.mobs.mobMovement;

import java.awt.geom.Point2D.Float;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.EmptyConstructorGameRegistry;
import necesse.engine.registries.IDData;
import necesse.engine.registries.IDDataContainer;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.Mob;

public abstract class MobMovement implements IDDataContainer {
   public final IDData idData = new IDData();
   public static EmptyConstructorGameRegistry<MobMovement> registry = new EmptyConstructorGameRegistry<MobMovement>("MobMovement", 32767) {
      @Override
      public void registerCore() {
         this.registerClass("constant", MobMovementConstant.class);
         this.registerClass("levelpos", MobMovementLevelPos.class);
         this.registerClass("relative", MobMovementRelative.class);
         this.registerClass("circlerelative", MobMovementCircleRelative.class);
         this.registerClass("circlelevel", MobMovementCircleLevelPos.class);
         this.registerClass("spiralrelative", MobMovementSpiralRelative.class);
         this.registerClass("spirallevel", MobMovementSpiralLevelPos.class);
         this.registerClass("diagonallinefixed", MobMovementDiagonalLineFixed.class);
         this.registerClass("lcirclerelative", MobMovementLimitedCircleRelative.class);
         this.registerClass("lcirclelevel", MobMovementLimitedCircleLevelPos.class);
         this.registerClass("whiprelative", MobMovementWhipPathRelative.class);
         this.registerClass("charge", MobMovementCharge.class);
      }

      @Override
      protected void onRegistryClose() {
      }
   };

   @Override
   public IDData getIDData() {
      return this.idData;
   }

   public MobMovement() {
      registry.applyIDData((Class<? extends MobMovement>)this.getClass(), this.idData);
   }

   public abstract void setupPacket(Mob var1, PacketWriter var2);

   public abstract void applyPacket(Mob var1, PacketReader var2);

   public abstract boolean tick(Mob var1);

   @Override
   public abstract boolean equals(Object var1);

   protected boolean moveTo(Mob mob, float targetX, float targetY, float errorValue) {
      float friction = mob.getFriction();
      float deltaX = targetX - mob.x;
      float deltaY = targetY - mob.y;
      float dist = mob.getDistance(targetX, targetY);
      if (dist > errorValue) {
         Float dir = GameMath.normalize(deltaX, deltaY);
         mob.moveX = dir.x;
         mob.moveY = dir.y;
         if (Math.abs(deltaX) < mob.stoppingDistance(friction, Math.abs(mob.dx)) / 2.0F) {
            mob.moveX = 0.0F;
         }

         if (Math.abs(deltaY) < mob.stoppingDistance(friction, Math.abs(mob.dy)) / 2.0F) {
            mob.moveY = 0.0F;
         }

         return false;
      } else {
         return dist <= errorValue;
      }
   }
}
