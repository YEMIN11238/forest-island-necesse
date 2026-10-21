package necesse.engine.network.packet;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.level.maps.Level;

public class PacketVoidPhasingStaff extends Packet {
   public final int mobUniqueID;
   public final float levelX;
   public final float levelY;
   public final boolean success;

   public PacketVoidPhasingStaff(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      this.mobUniqueID = reader.getNextInt();
      this.levelX = reader.getNextFloat();
      this.levelY = reader.getNextFloat();
      this.success = reader.getNextBoolean();
   }

   public PacketVoidPhasingStaff(Mob mob, float levelX, float levelY, boolean success) {
      this.mobUniqueID = mob.getUniqueID();
      this.levelX = levelX;
      this.levelY = levelY;
      this.success = success;
      PacketWriter writer = new PacketWriter(this);
      writer.putNextInt(this.mobUniqueID);
      writer.putNextFloat(levelX);
      writer.putNextFloat(levelY);
      writer.putNextBoolean(success);
   }

   @Override
   public void processClient(NetworkPacket packet, Client client) {
      if (client.getLevel() != null) {
         Mob target = GameUtils.getLevelMob(this.mobUniqueID, client.getLevel());
         if (target != null) {
            applyToMob(target.getLevel(), target, this.levelX, this.levelY, this.success);
         } else {
            client.network.sendPacket(new PacketRequestMobData(this.mobUniqueID));
         }
      }
   }

   public static void applyToMob(Level level, Mob mob, float levelX, float levelY, boolean success) {
      if (level != null) {
         int lastPosX = mob.getX();
         int lastPosY = mob.getY();
         Float dir = GameMath.normalize(lastPosX - levelX, lastPosY - levelY);
         if (!success) {
            if (!level.isServer()) {
               spawnFailedParticles(level, levelX, levelY, dir.x, dir.y);
            }
         } else {
            mob.setPos(levelX, levelY, true);
            if (!level.isServer()) {
               if (lastPosX != levelX || lastPosY != levelY) {
                  spawnPoofParticles(level, mob, lastPosX, lastPosY, dir.x, dir.y, false);
                  spawnBridgeParticles(level, lastPosX, lastPosY, levelX, levelY);
               }

               spawnPoofParticles(level, mob, mob.x, mob.y, -dir.x, -dir.y, true);
               SoundManager.playSound(GameResources.swoosh2, SoundEffect.effect(lastPosX, lastPosY).volume(0.5F));
               SoundManager.playSound(GameResources.swoosh2, SoundEffect.effect(mob).volume(0.5F));
               if (mob == level.getClient().getPlayer()) {
                  level.getClient().sendMovementPacket(true);
               }
            }
         }
      }
   }

   public static void spawnPoofParticles(Level level, Mob tpMob, float x, float y, float dirX, float dirY, boolean extra) {
      GameRandom random = GameRandom.globalRandom;

      for (int i = 0; i < 11; i++) {
         level.entityManager
            .addParticle(
               x + (float)random.nextGaussian() * 5.0F + tpMob.dx / 2.0F,
               y + (float)random.nextGaussian() * 10.0F + tpMob.dy / 2.0F,
               Particle.GType.IMPORTANT_COSMETIC
            )
            .movesConstant(tpMob.dx * random.getFloatBetween(0.8F, 1.2F) / 10.0F, tpMob.dy * random.getFloatBetween(0.8F, 1.2F) / 10.0F)
            .sprite(GameResources.puffParticles.sprite(random.getIntBetween(0, 4), 0, 12))
            .sizeFades(25, 40)
            .fadesAlpha(0.1F, 0.35F)
            .color(new Color(223, 124, 255))
            .height(18.0F)
            .lifeTime(700);
      }

      if (extra) {
         for (int i = 0; i < 5; i++) {
            float anglePerParticle = 72.0F;
            int angle = (int)(i * anglePerParticle * random.getFloatBetween(1.0F, 3.0F));
            float dx = (float)(Math.sin(Math.toRadians(angle)) + dirX) * 8.0F;
            float dy = (float)(Math.cos(Math.toRadians(angle)) + dirY) * 8.0F;
            level.entityManager
               .addParticle(x, y, Particle.GType.IMPORTANT_COSMETIC)
               .sprite(GameResources.ascendedParticle.sprite(0, 0, 20))
               .sizeFades(8, 16)
               .ignoreLight(true)
               .heightMoves(30.0F, 20.0F)
               .movesFriction(dx * random.getFloatBetween(2.0F, 3.0F), dy * random.getFloatBetween(2.0F, 3.0F), 0.8F)
               .lifeTime(800);
         }
      }
   }

   public static void spawnBridgeParticles(Level level, float fromX, float fromY, float toX, float toY) {
      GameRandom random = new GameRandom();
      Float normalizedDir = GameMath.normalize(toX - fromX, toY - fromY);
      float distance = GameMath.preciseDistance(fromX, fromY, toX, toY);
      float baseAngle = (float)Math.toDegrees(Math.atan2(normalizedDir.y, normalizedDir.x));
      int totalParticles = 16;

      for (int i = 0; i < totalParticles; i++) {
         float maxSpeed = 35.0F * distance * 0.01F;
         float speed = random.getFloatBetween(0.0F, 15.0F) * distance * 0.01F;
         float spread = (1.0F - speed / maxSpeed) * 35.0F;
         float angleOffset = random.getFloatBetween(-spread / 2.0F, spread / 2.0F);
         float angle = baseAngle + angleOffset;
         float dx = (float)Math.cos(Math.toRadians(angle)) * speed;
         float dy = (float)Math.sin(Math.toRadians(angle)) * speed;
         level.entityManager
            .addParticle(fromX, fromY, Particle.GType.IMPORTANT_COSMETIC)
            .sprite(GameResources.puffParticles.sprite(random.getIntBetween(0, 4), 0, 12))
            .color(new Color(223, 124, 255))
            .sizeFades(12, 18)
            .heightMoves(20.0F, 10.0F)
            .movesFriction(dx, dy, 0.6F)
            .lifeTime(700);
      }
   }

   public static void spawnFailedParticles(Level level, float x, float y, float dirX, float dirY) {
      GameRandom random = GameRandom.globalRandom;

      for (int i = 0; i < 5; i++) {
         float anglePerParticle = 72.0F;
         int angle = (int)(i * anglePerParticle * random.getFloatBetween(1.0F, 3.0F));
         float dx = (float)(Math.sin(Math.toRadians(angle)) + dirX) * 8.0F;
         float dy = (float)(Math.cos(Math.toRadians(angle)) + dirY) * 8.0F;
         level.entityManager
            .addParticle(x, y, Particle.GType.IMPORTANT_COSMETIC)
            .sprite(GameResources.ascendedParticle.sprite(0, 0, 20))
            .sizeFades(8, 16)
            .color(Color.RED)
            .ignoreLight(true)
            .heightMoves(16.0F, 10.0F)
            .movesFriction(dx * random.getFloatBetween(2.0F, 3.0F), dy * random.getFloatBetween(2.0F, 3.0F), 0.8F)
            .lifeTime(800);
      }
   }
}
