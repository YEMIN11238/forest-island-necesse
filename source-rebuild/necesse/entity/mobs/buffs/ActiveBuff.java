package necesse.entity.mobs.buffs;

import java.util.Collections;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicBoolean;
import necesse.engine.GameLog;
import necesse.engine.modifiers.Modifier;
import necesse.engine.modifiers.ModifierContainer;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobBeforeHitCalculatedEvent;
import necesse.entity.mobs.MobBeforeHitEvent;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.MobWasKilledEvent;
import necesse.entity.mobs.buffs.staticBuffs.Buff;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;

public class ActiveBuff extends ModifierContainer {
   public final Buff buff;
   public final Mob owner;
   private int stacks;
   private LinkedList<ActiveBuff.BuffTime> stackTimes;
   private Attacker source;
   private GNDItemMap gndData;
   private boolean isRemoved;
   protected float dotBuffer;

   public ActiveBuff(Buff buff, Mob owner, int duration, Attacker source) {
      super(BuffModifiers.LIST);
      if (buff.getID() == -1) {
         throw new NullPointerException("Buff \"" + buff.getClass().getSimpleName() + "\" has an invalid id.");
      }

      this.buff = buff;
      this.owner = owner;
      this.stacks = 1;
      this.stackTimes = new LinkedList<>(Collections.singleton(new ActiveBuff.BuffTime(duration, this.getTime())));
      this.source = buff.getSource(source);
      this.gndData = new GNDItemMap();
   }

   public ActiveBuff(int buffID, Mob owner, int duration, Attacker source) {
      this(BuffRegistry.getBuff(buffID), owner, duration, source);
   }

   public ActiveBuff(String buffType, Mob owner, int duration, Attacker source) {
      this(BuffRegistry.getBuff(BuffRegistry.getBuffID(buffType)), owner, duration, source);
   }

   public ActiveBuff(Buff buff, Mob owner, float durationSeconds, Attacker source) {
      this(buff, owner, (int)(durationSeconds * 1000.0F), source);
   }

   public ActiveBuff(int buffID, Mob owner, float durationSeconds, Attacker source) {
      this(BuffRegistry.getBuff(buffID), owner, durationSeconds, source);
   }

   public ActiveBuff(String buffType, Mob owner, float durationSeconds, Attacker source) {
      this(BuffRegistry.getBuff(buffType), owner, durationSeconds, source);
   }

   public void onOverridden(ActiveBuff otherBuff) {
      this.buff.onOverridden(this, otherBuff);
   }

   public void init(BuffEventSubscriber eventSubscriber) {
      this.buff.init(this, eventSubscriber);
      this.buff.init(this);
   }

   public void addStack(int duration, Attacker attacker) {
      this.stack(new ActiveBuff(this.buff, this.owner, duration, attacker));
   }

   public void setStacks(int stacks, int duration, Attacker attacker) {
      if (stacks <= 0) {
         GameLog.warn.println("Tried to set stacks to " + stacks + " for buff " + this.buff.getStringID() + ". Should always be at least 1");
         stacks = 1;
         duration = 0;
      }

      this.stacks = stacks;
      this.stackTimes.clear();

      for (int i = 0; i < stacks; i++) {
         this.stackTimes.add(new ActiveBuff.BuffTime(duration, this.getTime()));
      }

      if (this.source == null) {
         this.source = attacker;
      }
   }

   public void stack(ActiveBuff buff) {
      int maxStacks = this.getMaxStacks();
      if (maxStacks > 1 && !this.buff.overridesStackDuration()) {
         for (ActiveBuff.BuffTime time : buff.stackTimes) {
            if (this.stacks >= maxStacks) {
               ActiveBuff.BuffTime first = this.stackTimes.getFirst();
               if (first.getDurationLeft() < time.getDurationLeft()) {
                  this.stackTimes.removeFirst();
                  this.addTimeOrdered(time);
               }
            } else {
               this.addTimeOrdered(time);
               this.stacks++;
               this.buff.onStacksUpdated(this, buff);
            }
         }
      } else {
         ActiveBuff.BuffTime myTime = this.stackTimes.removeLast();
         ActiveBuff.BuffTime theirTime = buff.stackTimes.getLast();
         ActiveBuff.BuffTime biggest = myTime.getDurationLeft() < theirTime.getDurationLeft() ? theirTime : myTime;
         this.stackTimes.add(biggest);
         this.stacks = Math.min(maxStacks, this.stacks + buff.stacks);
         this.buff.onStacksUpdated(this, buff);
      }

      if (this.source == null) {
         this.source = buff.source;
      }
   }

   public void removeStack(boolean fromBeginning) {
      if (this.stacks > 1) {
         if (!this.buff.overridesStackDuration()) {
            if (fromBeginning) {
               this.stackTimes.removeFirst();
            } else {
               this.stackTimes.removeLast();
            }
         }

         this.stacks--;
         this.buff.onStacksUpdated(this, null);
      }
   }

   private void addTimeOrdered(ActiveBuff.BuffTime time) {
      ListIterator<ActiveBuff.BuffTime> iterator = this.stackTimes.listIterator();
      int timeDurationLeft = time.getDurationLeft();

      while (iterator.hasNext()) {
         ActiveBuff.BuffTime next = iterator.next();
         if (timeDurationLeft < next.getDurationLeft()) {
            if (iterator.previous() != null) {
               iterator.add(time);
            } else {
               this.stackTimes.addFirst(time);
            }

            return;
         }
      }

      this.stackTimes.addLast(time);
   }

   public boolean tickExpired() {
      if (this.buff.isPassive()) {
         return false;
      }

      boolean update = false;
      if (this.stackTimes.isEmpty()) {
         this.remove();
         return true;
      }

      while (!this.stackTimes.isEmpty()) {
         ActiveBuff.BuffTime time = this.stackTimes.getFirst();
         if (time.getModifiedDurationLeft() > 0) {
            break;
         }

         update = true;
         this.stacks--;
         if (this.stackTimes.size() <= 1) {
            if (this.stacks >= 1 && this.buff.overridesStackDuration()) {
               AtomicBoolean sendUpdatePacket = new AtomicBoolean(false);
               int remainingDuration = this.buff.getRemainingStacksDuration(this, sendUpdatePacket);
               if (remainingDuration > 0) {
                  time.duration = remainingDuration;
                  time.startTime = this.owner.getTime();
                  if (sendUpdatePacket.get() && this.owner.isServer()) {
                     BuffManager.sendUpdatePacket(this);
                  }

                  this.buff.onStacksUpdated(this, null);
                  break;
               }
            }

            this.remove();
            break;
         }

         this.stackTimes.removeFirst();
         this.buff.onStacksUpdated(this, null);
      }

      return update;
   }

   public void onBeforeHit(MobBeforeHitEvent event) {
      this.buff.onBeforeHit(this, event);
   }

   public void onBeforeAttacked(MobBeforeHitEvent event) {
      this.buff.onBeforeAttacked(this, event);
   }

   public void onBeforeHitCalculated(MobBeforeHitCalculatedEvent event) {
      this.buff.onBeforeHitCalculated(this, event);
   }

   public void onBeforeAttackedCalculated(MobBeforeHitCalculatedEvent event) {
      this.buff.onBeforeAttackedCalculated(this, event);
   }

   public void onWasHit(MobWasHitEvent event) {
      this.buff.onWasHit(this, event);
   }

   public void onHasAttacked(MobWasHitEvent event) {
      this.buff.onHasAttacked(this, event);
   }

   public void onItemAttacked(
      int targetX, int targetY, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, ItemAttackSlot slot, int animAttack, GNDItemMap attackMap
   ) {
      this.buff.onItemAttacked(this, targetX, targetY, attackerMob, attackHeight, item, slot, animAttack, attackMap);
   }

   public void onHasKilledTarget(MobWasKilledEvent event) {
      this.buff.onHasKilledTarget(this, event);
   }

   public void serverTick() {
      this.buff.serverTick(this);
   }

   public void clientTick() {
      this.buff.clientTick(this);
   }

   public void drawIcon(int x, int y) {
      this.buff.drawIcon(x, y, this);
   }

   public boolean isVisible() {
      return this.buff.isVisible(this);
   }

   public boolean canCancel() {
      return this.buff.canCancel(this);
   }

   public boolean shouldDrawDuration() {
      return this.buff.shouldDrawDuration(this);
   }

   public boolean isExpired() {
      return this.buff.isPassive() ? false : this.getDurationLeft() < 0;
   }

   public ListGameTooltips getTooltips(GameBlackboard blackboard) {
      return this.buff.getTooltip(this, blackboard);
   }

   @Override
   public int getStacks() {
      return this.stacks;
   }

   public int getMaxStacks() {
      return this.buff.getStackSize(this);
   }

   public void setDurationLeftSeconds(float seconds) {
      this.setDurationLeft((int)(seconds * 1000.0F));
   }

   public void setDurationLeft(int milliseconds) {
      ActiveBuff.BuffTime last = this.stackTimes.getLast();
      last.startTime = this.getTime();
      last.duration = milliseconds;
   }

   public int getDuration() {
      return this.stackTimes.getLast().duration;
   }

   public int getDurationLeft() {
      return this.buff.isPassive() ? 0 : this.stackTimes.getLast().getDurationLeft();
   }

   public int getModifiedDurationLeft() {
      return this.buff.isPassive() ? 0 : this.stackTimes.getLast().getModifiedDurationLeft();
   }

   public float getDurationModifier() {
      return this.owner.buffManager.getBuffDurationModifier(this);
   }

   public String getDurationText() {
      return this.buff.getDurationText(this);
   }

   public LinkedList<ActiveBuff.BuffTime> getStackTimes() {
      return this.stackTimes;
   }

   protected long getTime() {
      return this.owner.getWorldEntity() == null ? 0L : this.owner.getWorldEntity().getTime();
   }

   public static String convertSecondsToText(float seconds) {
      String out;
      if (seconds > 60.0F) {
         int min = (int)(seconds / 60.0F);
         int secLeft = (int)seconds % 60;
         if (min > 60) {
            int hours = min / 60;
            int minLeft = min % 60;
            if (hours >= 1000) {
               out = "LONG";
            } else if (hours > 10) {
               out = hours + "h";
            } else if (minLeft < 10) {
               out = hours + "h0" + minLeft;
            } else {
               out = hours + "h" + minLeft;
            }
         } else if (min > 10) {
            out = min + "m";
         } else if (secLeft < 10) {
            out = min + ":0" + secLeft;
         } else {
            out = min + ":" + secLeft;
         }
      } else if (seconds > 10.0F) {
         out = (int)seconds + "s";
      } else {
         out = (int)(seconds * 10.0F) / 10.0F + "s";
      }

      return out;
   }

   public void remove() {
      this.isRemoved = true;
   }

   public boolean isRemoved() {
      return this.isRemoved;
   }

   public Attacker getAttacker() {
      return this.source;
   }

   public GNDItemMap getGndData() {
      return this.gndData;
   }

   public void setGndData(GNDItemMap data) {
      if (data != null) {
         this.gndData = data.copy();
      } else {
         this.gndData = new GNDItemMap();
      }
   }

   @Override
   public <T> void setModifier(Modifier<T> modifier, T value) {
      super.setModifier(modifier, value);
      this.forceManagerUpdate();
   }

   @Override
   public <T> void addModifier(Modifier<T> modifier, T value, int count) {
      super.addModifier(modifier, value, count);
      this.forceManagerUpdate();
   }

   @Override
   public void onUpdate() {
      this.buff.onUpdate(this);
   }

   public void forceManagerUpdate() {
      this.owner.buffManager.updateBuffs();
   }

   public int getUpgradeLevel() {
      return this.getGndData().getInt("upgradeLevel");
   }

   public float getUpgradeTier() {
      return this.getUpgradeLevel() / 100.0F;
   }

   public ActiveBuff setUpgradeLevel(int upgradeLevel) {
      this.getGndData().setInt("upgradeLevel", upgradeLevel);
      return this;
   }

   public ActiveBuff setUpgradeTier(float tier) {
      return this.setUpgradeLevel((int)(tier * 100.0F));
   }

   public void addDebugTooltips(ListGameTooltips tips) {
      StringBuilder timesString = new StringBuilder();
      if (this.stacks > 1) {
         timesString.append(", ").append(this.stacks).append(" stacks");
      }

      if (!this.buff.isPassive()) {
         for (ActiveBuff.BuffTime time : this.stackTimes) {
            int durationLeft = time.getDurationLeft();
            int modifiedDurationLeft = time.getModifiedDurationLeft();
            timesString.append(", ").append(convertSecondsToText(modifiedDurationLeft / 1000.0F));
            if (durationLeft != modifiedDurationLeft) {
               timesString.append(" (").append(convertSecondsToText(durationLeft / 1000.0F)).append(")");
            }
         }
      } else {
         timesString.append(", passive");
      }

      tips.add(this.buff.getDisplayName() + timesString);
   }

   public void setupContentPacket(PacketWriter writer) {
      writer.putNextShortUnsigned(this.buff.getID());
      writer.putNextShortUnsigned(this.stacks);
      if (!this.buff.isPassive()) {
         for (ActiveBuff.BuffTime time : this.stackTimes) {
            writer.putNextInt(time.getDurationLeft());
         }
      }

      writer.putNextContentPacket(this.gndData.getContentPacket());
   }

   public Packet getContentPacket() {
      Packet out = new Packet();
      this.setupContentPacket(new PacketWriter(out));
      return out;
   }

   private ActiveBuff.BuffTime createBuffTime(int duration) {
      return new ActiveBuff.BuffTime(duration, this.getTime());
   }

   public static ActiveBuff fromPacketIterator(PacketReader reader, Mob owner) {
      int id = reader.getNextShortUnsigned();
      Buff buff = BuffRegistry.getBuff(id);
      ActiveBuff ab = new ActiveBuff(buff, owner, 1000, null);
      ab.stacks = reader.getNextShortUnsigned();
      int stackTimesSize = buff.overridesStackDuration() ? 1 : ab.stacks;
      ab.stackTimes = new LinkedList<>();

      for (int i = 0; i < stackTimesSize; i++) {
         int durationLeft = buff.isPassive() ? 0 : reader.getNextInt();
         ab.stackTimes.addLast(ab.createBuffTime(durationLeft));
      }

      ab.gndData = new GNDItemMap(reader.getNextContentPacket());
      return ab;
   }

   public static ActiveBuff fromContentPacket(Packet packet, Mob owner) {
      return fromPacketIterator(new PacketReader(packet), owner);
   }

   public void addSaveData(SaveData save) {
      save.addUnsafeString("stringID", this.buff.getStringID());
      if (this.stacks <= 1) {
         save.addInt("duration", this.getDurationLeft());
      } else {
         save.addInt("stacks", this.stacks);
         int stack = 0;

         for (ActiveBuff.BuffTime time : this.stackTimes) {
            save.addInt("stack" + stack, time.getDurationLeft());
            stack++;
         }
      }

      if (this.gndData.getMapSize() > 0) {
         SaveData gnd = new SaveData("GNDData");
         this.gndData.addSaveData(gnd);
         save.addSaveData(gnd);
      }
   }

   public static ActiveBuff fromLoadData(LoadData save, Mob owner) {
      String buffType = save.getFirstDataByName("stringID");
      Buff buff = BuffRegistry.getBuff(buffType);
      if (buff == null) {
         return null;
      }

      ActiveBuff ab = new ActiveBuff(buff, owner, 1000, null);
      ab.stacks = save.getInt("stacks", 1, false);
      ab.stackTimes = new LinkedList<>();
      if (ab.stacks > 1) {
         int expectedDurations = buff.overridesStackDuration() ? 1 : ab.stacks;

         for (int i = 0; i < expectedDurations; i++) {
            int durationLeft = save.getInt("stack" + i, 0);
            ab.addTimeOrdered(ab.createBuffTime(durationLeft));
         }
      } else {
         int durationLeft = save.getInt("duration", 0, false);
         ab.addTimeOrdered(ab.createBuffTime(durationLeft));
      }

      LoadData gnd = save.getFirstLoadDataByName("GNDData");
      if (gnd != null) {
         ab.gndData = new GNDItemMap(gnd);
      }

      return ab;
   }

   public class BuffTime {
      public int duration;
      public long startTime;

      private BuffTime(int duration, long startTime) {
         this.duration = duration;
         this.startTime = startTime;
      }

      public int getDurationLeft() {
         return (int)(this.duration + this.startTime - ActiveBuff.this.getTime());
      }

      public int getModifiedDuration() {
         return (int)(this.duration * ActiveBuff.this.getDurationModifier());
      }

      public int getModifiedDurationLeft() {
         return (int)(this.getModifiedDuration() + this.startTime - ActiveBuff.this.getTime());
      }
   }
}
