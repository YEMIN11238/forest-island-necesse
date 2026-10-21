package necesse.entity.mobs.attackHandler;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import necesse.engine.GlobalData;
import necesse.engine.input.Input;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.camera.GameCamera;
import necesse.inventory.item.ItemAttackerWeaponItem;

public abstract class MouseAngleAttackHandler extends AttackHandler {
   protected float currentAngle;
   protected int targetAngle;
   protected float speed;

   public MouseAngleAttackHandler(ItemAttackerMob itemAttacker, ItemAttackSlot slot, int updateInterval, float speed, int startTargetX, int startTargetY) {
      super(itemAttacker, slot, updateInterval);
      this.speed = speed;
      this.targetAngle = (int)GameMath.fixAngle(GameMath.getAngle(new Float(startTargetX - itemAttacker.x, startTargetY - itemAttacker.y)));
      this.currentAngle = this.targetAngle;
   }

   public int getNextClientAngle(PlayerMob player, GameCamera camera) {
      if (Input.lastInputIsController && !ControllerInput.isCursorVisible()) {
         Float aimDir = player.getControllerAimDir();
         return (int)GameMath.fixAngle(GameMath.getAngle(aimDir));
      } else {
         return camera == null
            ? this.targetAngle
            : (int)GameMath.fixAngle(GameMath.getAngle(new Float(camera.getMouseLevelPosX() - player.x, camera.getMouseLevelPosY() - player.y)));
      }
   }

   public int getNextItemAttackerAngle(Mob currentTarget) {
      Point attackPos = ((ItemAttackerWeaponItem)this.item.item)
         .getItemAttackerAttackPosition(this.attackerMob.getLevel(), this.attackerMob, currentTarget, -1, this.item);
      return (int)GameMath.fixAngle(GameMath.getAngle(new Float(attackPos.x - this.attackerMob.x, attackPos.y - this.attackerMob.y)));
   }

   @Override
   protected void setupClientUpdatePacket(PlayerMob player, PacketWriter writer) {
      super.setupClientUpdatePacket(player, writer);
      GameCamera camera = GlobalData.getCurrentState().getCamera();
      writer.putNextInt(this.getNextClientAngle(player, camera));
   }

   @Override
   public void onUpdatePacket(PacketReader reader) {
      super.onUpdatePacket(reader);
      this.targetAngle = reader.getNextInt();
   }

   @Override
   public void onUpdate() {
      if (this.attackerMob.isPlayer) {
         if (this.attackerMob.isClient()) {
            PlayerMob player = (PlayerMob)this.attackerMob;
            GameCamera camera = GlobalData.getCurrentState().getCamera();
            if (this.getNextClientAngle(player, camera) != this.targetAngle) {
               this.sendPacketUpdate(true);
            }
         }
      } else if (this.lastItemAttackerTarget != null) {
         this.targetAngle = this.getNextItemAttackerAngle(this.lastItemAttackerTarget);
      }

      if (this.currentAngle != this.targetAngle) {
         float delta = GameMath.getAngleDifference(this.currentAngle, this.targetAngle);
         float change = this.speed * this.updateInterval / 250.0F;
         if (Math.abs(delta) < change) {
            this.currentAngle = this.targetAngle;
         } else if (delta < 0.0F) {
            this.currentAngle = GameMath.fixAngle(this.currentAngle + change);
            if (GameMath.getAngleDifference(this.currentAngle, this.targetAngle) > 0.0F) {
               this.currentAngle = this.targetAngle;
            }
         } else if (delta > 0.0F) {
            this.currentAngle = GameMath.fixAngle(this.currentAngle - change);
            if (GameMath.getAngleDifference(this.currentAngle, this.targetAngle) < 0.0F) {
               this.currentAngle = this.targetAngle;
            }
         }
      }
   }
}
