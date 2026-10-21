package necesse.entity.mobs;

import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.ComputedObjectValue;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffManager;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.level.maps.CollisionFilter;

public class WormMobBody<T extends WormMobHead<B, T>, B extends WormMobBody<T, B>> extends Mob {
   public int removeTicker;
   public final LevelMob<T> master = new LevelMob<>();
   public GameLinkedList<WormMoveLine>.Element moveLine;
   public float moveLineExtraDist;
   public float height = 0.0F;
   public B next;
   public boolean canHit = true;
   public boolean sharesHitCooldownWithNext = false;
   public boolean relayHitToNext = false;
   public boolean relaysBuffsToNext = false;

   public WormMobBody(int health) {
      super(health);
      this.isSummoned = true;
      this.dropsLoot = false;
      this.setKnockbackModifier(0.0F);
      this.setRegen(0.0F);
      this.buffManager = new BuffManager(this) {
         @Override
         public ActiveBuff addBuff(ActiveBuff ab, boolean sendUpdatePacket, boolean forceOverride, boolean forceUpdateBuffs) {
            if (WormMobBody.this.relaysBuffsToNext) {
               if (WormMobBody.this.next != null) {
                  ActiveBuff newBuff = new ActiveBuff(ab.buff, WormMobBody.this.next, ab.getDuration(), ab.getAttacker());
                  newBuff.setGndData(newBuff.getGndData());
                  return WormMobBody.this.next.buffManager.addBuff(newBuff, sendUpdatePacket, forceOverride, forceUpdateBuffs);
               } else {
                  T master = WormMobBody.this.master.get(WormMobBody.this.getLevel());
                  if (master != null) {
                     ActiveBuff newBuff = new ActiveBuff(ab.buff, master, ab.getDuration(), ab.getAttacker());
                     newBuff.setGndData(newBuff.getGndData());
                     return master.buffManager.addBuff(newBuff, sendUpdatePacket, forceOverride, forceUpdateBuffs);
                  } else {
                     return super.addBuff(ab, sendUpdatePacket, forceOverride, forceUpdateBuffs);
                  }
               }
            } else {
               return super.addBuff(ab, sendUpdatePacket, forceOverride, forceUpdateBuffs);
            }
         }
      };
   }

   @Override
   public boolean shouldSendSpawnPacket() {
      return false;
   }

   @Override
   public Mob getSpawnPacketMaster() {
      return this.master.get(this.getLevel());
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.master.uniqueID = reader.getNextInt();
      this.canHit = reader.getNextBoolean();
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.master.uniqueID);
      writer.putNextBoolean(this.canHit);
   }

   @Override
   public void init() {
      super.init();
      this.countStats = false;
   }

   public void updateBodyPartPosition(T head, float x, float y) {
      this.setPos(x, y, true);
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.tickMaster();
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.movementUpdateTime = this.getWorldEntity().getTime();
      this.healthUpdateTime = this.getWorldEntity().getTime();
      this.tickMaster();
   }

   @Override
   public void tickMovement(float delta) {
      if (!this.removed()) {
         Float moveDir = null;
         if (this.moveLine != null) {
            ComputedObjectValue<GameLinkedList<WormMoveLine>.Element, Double> dist = WormMobHead.moveDistance(this.moveLine, -20.0);
            if (dist.object != null) {
               java.awt.geom.Point2D.Double pos = WormMobHead.linePos(dist);
               moveDir = GameMath.normalize((float)pos.x - this.x, (float)pos.y - this.y);
            }
         }

         if (moveDir != null) {
            T master = this.master.get(this.getLevel());
            if (master != null) {
               float speed = master.getSpeed();
               this.dx = moveDir.x * speed;
               this.dy = moveDir.y * speed;
            } else {
               this.dx = 0.0F;
               this.dy = 0.0F;
            }
         } else {
            this.dx = 0.0F;
            this.dy = 0.0F;
         }

         this.checkCollision();
      }
   }

   @Override
   public void requestServerUpdate() {
   }

   @Override
   public void sendMovementPacket(boolean isDirect) {
   }

   public void tickMaster() {
      if (!this.removed()) {
         this.master.computeIfPresent(this.getLevel(), m -> {
            this.setMaxHealth(m.getMaxHealth());
            this.setHealthHidden(m.getHealth(), 0.0F, 0.0F, null);
            this.setArmor(m.getArmorFlat());
         });
         this.removeTicker++;
         if (this.removeTicker > 20) {
            this.remove();
         }
      }
   }

   @Override
   public void playHurtSound() {
      T master = this.master.get(this.getLevel());
      if (master != null) {
         master.playHurtSound();
      }
   }

   @Override
   public void playDeathSound() {
   }

   @Override
   public boolean canHitThroughCollision() {
      return true;
   }

   @Override
   public CollisionFilter getLevelCollisionFilter() {
      return null;
   }

   @Override
   public boolean canBePushed(Mob other) {
      return false;
   }

   @Override
   public boolean canCollisionHit(Mob target) {
      return this.isVisible() && super.canCollisionHit(target);
   }

   @Override
   public boolean isVisible() {
      return this.moveLine != null && !((WormMoveLine)this.moveLine.object).isUnderground;
   }

   @Override
   public void startHitCooldown() {
      super.startHitCooldown();
      if (this.master != null) {
         this.master.computeIfPresent(this.getLevel(), Mob::startHitCooldown);
      }
   }

   @Override
   public int getHitCooldownUniqueID() {
      if (this.sharesHitCooldownWithNext) {
         if (this.next != null) {
            return this.next.getHitCooldownUniqueID();
         }

         T master = this.master.get(this.getLevel());
         return master != null ? master.getHitCooldownUniqueID() : this.master.uniqueID;
      } else {
         return super.getHitCooldownUniqueID();
      }
   }

   @Override
   public boolean canBeHit(Attacker attacker) {
      return this.isVisible() && this.canHit ? super.canBeHit(attacker) : false;
   }

   @Override
   public boolean shouldSave() {
      return false;
   }

   @Override
   public int getHealth() {
      if (this.master != null) {
         T head = this.master.get(this.getLevel());
         if (head != null) {
            return head.getHealth();
         }
      }

      return super.getHealth();
   }

   @Override
   public int getMaxHealth() {
      if (this.master != null) {
         T head = this.master.get(this.getLevel());
         if (head != null) {
            return head.getMaxHealth();
         }
      }

      return super.getMaxHealth();
   }

   @Override
   public MobWasHitEvent isHit(MobWasHitEvent event, Attacker attacker) {
      if (this.relayHitToNext) {
         if (this.next != null) {
            return this.next.isHit(event, attacker);
         }

         T master = this.master.get(this.getLevel());
         return master != null ? master.isHit(event, attacker) : super.isHit(event, attacker);
      } else {
         return super.isHit(event, attacker);
      }
   }

   @Override
   public MobWasHitEvent isServerHit(GameDamage damage, float x, float y, float knockback, Attacker attacker) {
      if (this.relayHitToNext) {
         if (this.next != null) {
            return this.next.isServerHit(damage, x, y, knockback, attacker);
         }

         T master = this.master.get(this.getLevel());
         return master != null ? master.isServerHit(damage, x, y, knockback, attacker) : super.isServerHit(damage, x, y, knockback, attacker);
      } else {
         return super.isServerHit(damage, x, y, knockback, attacker);
      }
   }

   @Override
   public void setHealthHidden(int health, float knockbackX, float knockbackY, Attacker attacker, boolean fromNetworkUpdate) {
      if (this.master != null) {
         this.master.computeIfPresent(this.getLevel(), m -> m.setHealthHidden(health, knockbackX, knockbackY, attacker, fromNetworkUpdate));
      }

      super.setHealthHidden(health, knockbackX, knockbackY, attacker, fromNetworkUpdate);
   }

   @Override
   public Rectangle getSelectBox(int x, int y) {
      Rectangle selectBox = super.getSelectBox(x, y);
      selectBox.y = (int)(selectBox.y - this.height);
      if (this.height < 0.0F) {
         selectBox.height = (int)(selectBox.height + this.height);
      }

      return selectBox;
   }

   @Override
   public boolean isHealthBarVisible() {
      return false;
   }

   @Override
   public void remove(float knockbackX, float knockbackY, Attacker attacker, boolean isDeath) {
      boolean removed = this.removed();
      super.remove(knockbackX, knockbackY, attacker, isDeath);
      if (!removed && this.master != null) {
         this.master.computeIfPresent(this.getLevel(), m -> m.remove(knockbackX, knockbackY, attacker));
      }
   }

   @Override
   protected void addDebugTooltips(ListGameTooltips tooltips) {
      super.addDebugTooltips(tooltips);
      tooltips.add("Height: " + this.height);
   }

   @Override
   public float getIncomingDamageModifier() {
      T master = this.master.get(this.getLevel());
      return master == null ? super.getIncomingDamageModifier() : master.getIncomingDamageModifier();
   }
}
