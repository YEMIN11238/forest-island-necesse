package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import necesse.engine.input.Input;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketForceOfWind;
import necesse.engine.network.packet.PacketVoidPhasingStaff;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffAbility;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.gfx.camera.GameCamera;
import necesse.level.maps.Level;

public class VoidPhasingStaffTrinketBuff extends TrinketBuff implements BuffAbility {
   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   @Override
   public Packet getAbilityContent(PlayerMob player, ActiveBuff buff, GameCamera camera) {
      Packet packet = new Packet();
      PacketWriter writer = new PacketWriter(packet);
      Point levelPos = getNextClientLevelPos(player, camera);
      writer.putNextInt(levelPos.x);
      writer.putNextInt(levelPos.y);
      return packet;
   }

   @Override
   public void runAbility(PlayerMob player, ActiveBuff buff, Packet content) {
      PacketReader reader = new PacketReader(content);
      int levelX = reader.getNextInt();
      int levelY = reader.getNextInt();
      useBlinkAbility(player, levelX, levelY);
   }

   @Override
   public boolean canRunAbility(PlayerMob player, ActiveBuff buff, Packet content) {
      return !buff.owner.isRiding() && !PacketForceOfWind.isOnCooldown(buff.owner);
   }

   public static Point getNextClientLevelPos(PlayerMob player, GameCamera camera) {
      if (Input.lastInputIsController && !ControllerInput.isCursorVisible()) {
         Float aimDir = player.getControllerAimDir();
         return new Point((int)(player.x + aimDir.x * 100.0F), (int)(player.y + aimDir.y * 100.0F));
      } else {
         return new Point(camera.getMouseLevelPosX(), camera.getMouseLevelPosY());
      }
   }

   public static void useBlinkAbility(PlayerMob player, int levelX, int levelY) {
      Point targetPos = new Point(levelX, levelY);
      if (!player.isClient()) {
         Float dir = GameMath.normalize(targetPos.x - player.x, targetPos.y - player.y);
         float range = player.getDistance(targetPos.x, targetPos.y);
         if (range > 2000.0F) {
            targetPos.x = (int)(player.x + dir.x * 2000.0F);
            targetPos.y = (int)(player.y + dir.y * 2000.0F);
         }

         if (player.collidesWith(player.getLevel(), targetPos.x, targetPos.y)) {
            targetPos = findSpawnLocation(player, player.getLevel(), targetPos.x, targetPos.y, dir);
            if (targetPos == null) {
               player.getServer().network.sendToClientsWithEntity(new PacketVoidPhasingStaff(player, player.getX(), player.getY(), false), player);
               PacketVoidPhasingStaff.applyToMob(player.getLevel(), player, player.getX(), player.getY(), false);
               return;
            }
         }

         player.getServer().network.sendToClientsWithEntity(new PacketVoidPhasingStaff(player, targetPos.x, targetPos.y, true), player);
         PacketVoidPhasingStaff.applyToMob(player.getLevel(), player, targetPos.x, targetPos.y, true);
         PacketForceOfWind.addCooldownStack(player, 10.0F, player.isServer());
      }
   }

   public static Point findSpawnLocation(Mob mob, Level level, int centerX, int centerY, Float dir) {
      Point flattenedDir = new Point(GameMath.roundToNearest(dir.x, 1), GameMath.roundToNearest(dir.y, 1));
      Point flatCheck = new Point(centerX + flattenedDir.x * 32, centerY + flattenedDir.y * 32);
      if (!mob.collidesWith(level, flatCheck.x, flatCheck.y)) {
         return flatCheck;
      }

      for (int cX = -1; cX <= 1; cX++) {
         for (int cY = -1; cY <= 1; cY++) {
            if (cX != 0 || cY != 0) {
               int posX = centerX + cX * 32;
               int posY = centerY + cY * 32;
               if (!mob.collidesWith(level, posX, posY)) {
                  return new Point(posX, posY);
               }
            }
         }
      }

      return null;
   }
}
