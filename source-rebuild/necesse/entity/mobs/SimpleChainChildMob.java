package necesse.entity.mobs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.GameRandom;
import necesse.entity.Entity;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffManager;
import necesse.level.maps.CollisionFilter;

public class SimpleChainChildMob<T extends Mob, C extends SimpleChainChildMob<T, C>> extends Mob {
   public int removeTicker;
   public final LevelMob<T> master = new LevelMob<>();
   public C next;
   public boolean canHit = true;
   public boolean sharesHitCooldownWithNext = false;
   public boolean relayHitToNext = false;
   public boolean relaysBuffsToNext = false;
   public boolean doesChainChildMobSendMovementPackets = false;

   public SimpleChainChildMob(int health) {
      super(health);
      this.isSummoned = true;
      this.dropsLoot = false;
      this.setKnockbackModifier(0.0F);
      this.setRegen(0.0F);
      this.buffManager = new BuffManager(this) {
         @Override
         public ActiveBuff addBuff(ActiveBuff ab, boolean sendUpdatePacket, boolean forceOverride, boolean forceUpdateBuffs) {
            if (SimpleChainChildMob.this.relaysBuffsToNext) {
               if (SimpleChainChildMob.this.next != null) {
                  ActiveBuff newBuff = new ActiveBuff(ab.buff, SimpleChainChildMob.this.next, ab.getDuration(), ab.getAttacker());
                  newBuff.setGndData(newBuff.getGndData());
                  return SimpleChainChildMob.this.next.buffManager.addBuff(newBuff, sendUpdatePacket, forceOverride, forceUpdateBuffs);
               } else {
                  T master = SimpleChainChildMob.this.master.get(SimpleChainChildMob.this.getLevel());
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
   public void setupHealthPacket(PacketWriter writer, boolean isFull) {
   }

   @Override
   public void applyHealthPacket(PacketReader reader, boolean isFull) {
   }

   @Override
   public void setupResiliencePacket(PacketWriter writer, boolean isFull) {
   }

   @Override
   public void applyResiliencePacket(PacketReader reader, boolean isFull) {
   }

   @Override
   public void setupManaPacket(PacketWriter writer, boolean isFull) {
   }

   @Override
   public void applyManaPacket(PacketReader reader, boolean isFull) {
   }

   @Override
   public void init() {
      super.init();
      this.countStats = false;
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.tickMaster();
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (!this.doesChainChildMobSendMovementPackets) {
         this.movementUpdateTime = this.getTime();
      }

      this.healthUpdateTime = this.getTime();
      this.tickMaster();
   }

   @Override
   public void requestServerUpdate() {
   }

   @Override
   public void sendMovementPacket(boolean isDirect) {
      if (this.doesChainChildMobSendMovementPackets) {
         super.sendMovementPacket(isDirect);
      }
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
   public float getIncomingDamageModifier() {
      T master = this.master.get(this.getLevel());
      return master == null ? super.getIncomingDamageModifier() : master.getIncomingDamageModifier();
   }

   public static class ChildrenManager<T extends Mob, C extends SimpleChainChildMob<T, C>> implements Iterable<LevelMob<C>> {
      public final T master;
      private final ArrayList<LevelMob<C>> children = new ArrayList<>();

      public ChildrenManager(T master) {
         this.master = master;
      }

      public void setupSpawnPacket(PacketWriter writer) {
         writer.putNextShortUnsigned(this.children.size());

         for (LevelMob<C> child : this.children) {
            writer.putNextInt(child.uniqueID);
         }
      }

      public void applySpawnPacket(PacketReader reader) {
         for (LevelMob<C> child : this.children) {
            C last = child.get(this.master.getLevel());
            if (last != null) {
               last.remove();
            }
         }

         this.children.clear();
         int childrenCount = reader.getNextShortUnsigned();

         for (int i = 0; i < childrenCount; i++) {
            this.children.add(new LevelMob<>(reader.getNextInt()));
         }
      }

      public void initChildren(int totalChildren, Function<Integer, C> createBodyPart, BiConsumer<Integer, C> modifyBodyPart) {
         if (this.children.size() < totalChildren) {
            GameRandom uniqueIDRandom = new GameRandom();
            int start = this.children.size();
            int lastUniqueID = this.children.isEmpty() ? this.master.getUniqueID() : this.children.get(this.children.size() - 1).uniqueID;

            for (int i = 0; i < totalChildren - start; i++) {
               uniqueIDRandom.setSeed(lastUniqueID);
               int newUniqueID = Entity.getNewUniqueID(this.master.getLevel(), uniqueIDRandom);
               this.children.add(new LevelMob<>(newUniqueID));
               lastUniqueID = newUniqueID;
            }
         }

         C lastBodyPart = null;

         for (int i = 0; i < this.children.size(); i++) {
            LevelMob<C> levelMob = this.children.get(i);
            C bodyPart = createBodyPart.apply(i);
            bodyPart.next = lastBodyPart;
            lastBodyPart = bodyPart;
            bodyPart.setLevel(this.master.getLevel());
            bodyPart.setUniqueID(levelMob.uniqueID);
            bodyPart.setPos(this.master.x, this.master.y, true);
            bodyPart.setMaxHealth(this.master.getMaxHealth());
            bodyPart.setHealthHidden(this.master.getHealth());
            bodyPart.master.uniqueID = this.master.getUniqueID();
            if (modifyBodyPart != null) {
               modifyBodyPart.accept(i, bodyPart);
            }

            this.master.getLevel().entityManager.mobs.addHidden(bodyPart);
            this.children.set(i, new LevelMob<>(bodyPart));
         }
      }

      public void tickKeepAlive() {
         for (LevelMob<C> bodyPart : this.children) {
            bodyPart.computeIfPresent(this.master.getLevel(), bp -> bp.removeTicker = 0);
         }
      }

      public void removeChildren(float knockbackX, float knockbackY, Attacker attacker, boolean isDeath) {
         for (LevelMob<C> child : this.children) {
            child.computeIfPresent(this.master.getLevel(), bp -> bp.remove(knockbackX, knockbackY, attacker, isDeath));
         }
      }

      @Override
      public Iterator<LevelMob<C>> iterator() {
         return this.children.iterator();
      }

      public Iterator<C> getChildrenNonNull() {
         return this.streamChildrenNonNull().iterator();
      }

      public Iterable<C> iterateChildrenNonNull() {
         return this::getChildrenNonNull;
      }

      public Stream<LevelMob<C>> streamChildren() {
         return this.children.stream();
      }

      public Stream<C> streamChildrenNonNull() {
         return this.streamChildren().map(m -> m.get(this.master.getLevel())).filter(Objects::nonNull);
      }

      public int getChildrenSize() {
         return this.children.size();
      }

      public LevelMob<C> getChildLevelMob(int index) {
         return this.children.get(index);
      }

      public C getChild(int index) {
         return this.getChildLevelMob(index).get(this.master.getLevel());
      }

      public boolean isEmpty() {
         return this.children.isEmpty();
      }
   }
}
