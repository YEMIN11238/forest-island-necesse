package necesse.engine.network.packet;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.MovedRectangle;
import necesse.entity.mobs.Mob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.level.maps.Level;

public class PacketBlinkScepter extends Packet {
   public final int mobUniqueID;
   public final float dirX;
   public final float dirY;
   public final float range;

   public PacketBlinkScepter(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      this.mobUniqueID = reader.getNextInt();
      this.dirX = reader.getNextFloat();
      this.dirY = reader.getNextFloat();
      this.range = reader.getNextFloat();
   }

   public PacketBlinkScepter(Mob mob, float dirX, float dirY, float range) {
      this.mobUniqueID = mob.getUniqueID();
      this.dirX = dirX;
      this.dirY = dirY;
      this.range = range;
      PacketWriter writer = new PacketWriter(this);
      writer.putNextInt(this.mobUniqueID);
      writer.putNextFloat(dirX);
      writer.putNextFloat(dirY);
      writer.putNextFloat(range);
   }

   @Override
   public void processClient(NetworkPacket packet, Client client) {
      if (client.getLevel() != null) {
         Mob target = GameUtils.getLevelMob(this.mobUniqueID, client.getLevel());
         if (target != null) {
            applyToMob(target.getLevel(), target, this.dirX, this.dirY, this.range);
         } else {
            client.network.sendPacket(new PacketRequestMobData(this.mobUniqueID));
         }
      }
   }

   public static Float getMobDir(Mob mob) {
      if (mob.moveX == 0.0F && mob.moveY == 0.0F) {
         int dir = mob.getDir();
         if (dir == 0) {
            return new Float(0.0F, -1.0F);
         } else if (dir == 1) {
            return new Float(1.0F, 0.0F);
         } else if (dir == 2) {
            return new Float(0.0F, 1.0F);
         } else {
            return dir == 3 ? new Float(-1.0F, 0.0F) : new Float(0.0F, 0.0F);
         }
      } else {
         return GameMath.normalize(mob.moveX, mob.moveY);
      }
   }

   public static void applyToMob(Level level, Mob mob, float dirX, float dirY, float range) {
      if (level != null) {
         int lastPosX = mob.getX();
         int lastPosY = mob.getY();

         int newPosX;
         int newPosY;
         while (true) {
            newPosX = mob.getX() + (int)(dirX * range);
            newPosY = mob.getY() + (int)(dirY * range);
            MovedRectangle moveRect = new MovedRectangle(mob, mob.getX(), mob.getY(), newPosX, newPosY);
            if (!level.collides(moveRect, mob.getLevelCollisionFilter())) {
               break;
            }

            range -= 4.0F;
            if (range <= 0.0F) {
               newPosX = mob.getX();
               newPosY = mob.getY();
               break;
            }
         }

         mob.setPos(newPosX, newPosY, true);
         if (level.isClient()) {
            if (lastPosX != newPosX || lastPosY != newPosY) {
               spawnPoofParticles(level, mob, lastPosX, lastPosY);
            }

            spawnPoofParticles(level, mob, mob.x, mob.y);
            if (mob == level.getClient().getPlayer()) {
               level.getClient().sendMovementPacket(true);
            }
         }
      }
   }

   protected static void spawnPoofParticles(Level level, Mob tpMob, float x, float y) {
      for (int i = 0; i < 11; i++) {
         level.entityManager
            .addParticle(
               x + (float)GameRandom.globalRandom.nextGaussian() * 5.0F + tpMob.dx / 2.0F,
               y + (float)GameRandom.globalRandom.nextGaussian() * 10.0F + tpMob.dy / 2.0F,
               Particle.GType.IMPORTANT_COSMETIC
            )
            .movesConstant(
               tpMob.dx * GameRandom.globalRandom.getFloatBetween(0.8F, 1.2F) / 10.0F, tpMob.dy * GameRandom.globalRandom.getFloatBetween(0.8F, 1.2F) / 10.0F
            )
            .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 4), 0, 12))
            .sizeFades(25, 40)
            .fadesAlpha(0.1F, 0.35F)
            .color(new Color(236, 221, 197))
            .height(18.0F)
            .lifeTime(700);
      }
   }
}
