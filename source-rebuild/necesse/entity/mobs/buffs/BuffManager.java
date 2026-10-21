package necesse.entity.mobs.buffs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.journal.listeners.BuffGainedJournalChallengeListener;
import necesse.engine.modifiers.Modifier;
import necesse.engine.modifiers.ModifierContainerLimits;
import necesse.engine.modifiers.ModifierManager;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketMobBuff;
import necesse.engine.network.packet.PacketMobBuffRemove;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.JournalChallengeRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.BuffAddedEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobAfterDamageOverTimeTakenEvent;
import necesse.entity.mobs.MobBeforeDamageOverTimeTakenEvent;
import necesse.entity.mobs.MobGenericEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.staticBuffs.Buff;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.level.maps.Level;

public class BuffManager extends ModifierManager<ActiveBuff> {
   private final Mob owner;
   private final HashMap<Integer, ActiveBuff> buffs = new HashMap<>();
   private final BuffEventSubscriptions eventSubscriptions = new BuffEventSubscriptions();
   private final HashSet<Integer> movementTickBuffs = new HashSet<>();
   private final HashSet<Integer> humanDrawBuffs = new HashSet<>();
   private final HashSet<Integer> mobExtraDrawBuffs = new HashSet<>();
   private final HashSet<Attacker> attackers = new HashSet<>();
   private int updateTimer;
   private boolean updateModifiers;

   public BuffManager(Mob owner) {
      super(BuffModifiers.LIST);
      this.owner = owner;
      this.makeQueryable(BuffModifiers.FIRE_DAMAGE_FLAT);
      this.makeQueryable(BuffModifiers.POISON_DAMAGE_FLAT);
      this.makeQueryable(BuffModifiers.FROST_DAMAGE_FLAT);
      this.makeQueryable(BuffModifiers.BLEED_DAMAGE_FLAT);
      super.updateModifiers();
   }

   public ActiveBuff addBuff(ActiveBuff ab, boolean sendUpdatePacket) {
      return this.addBuff(ab, sendUpdatePacket, false);
   }

   public ActiveBuff addBuff(ActiveBuff ab, boolean sendUpdatePacket, boolean forceOverride) {
      return this.addBuff(ab, sendUpdatePacket, forceOverride, false);
   }

   public ActiveBuff addBuff(ActiveBuff ab, boolean sendUpdatePacket, boolean forceOverride, boolean forceUpdateBuffs) {
      return this.addBuff(ab, sendUpdatePacket, forceOverride, forceUpdateBuffs, true);
   }

   private ActiveBuff addBuff(ActiveBuff ab, boolean sendUpdatePacket, boolean forceOverride, boolean forceUpdateBuffs, boolean firstAdd) {
      final int buffID = ab.buff.getID();
      ActiveBuff old = this.getBuff(buffID);
      boolean isOverride;
      if (old != null && !forceOverride) {
         ab.init(new BuffEventSubscriber() {
            @Override
            public <T extends MobGenericEvent> void subscribeEvent(Class<T> eventClass, Consumer<T> onEvent) {
               BuffManager.this.eventSubscriptions.addSubscription(buffID, eventClass, onEvent);
            }
         });
         old.stack(ab);
         ab = old;
         isOverride = true;
      } else {
         synchronized (this.buffs) {
            this.buffs.put(buffID, ab);
            if (ab.buff instanceof MovementTickBuff) {
               this.movementTickBuffs.add(buffID);
            }

            if (ab.buff instanceof HumanDrawBuff) {
               this.humanDrawBuffs.add(buffID);
            }

            if (ab.buff instanceof MobExtraDrawBuff) {
               this.mobExtraDrawBuffs.add(buffID);
            }
         }

         ab.init(new BuffEventSubscriber() {
            @Override
            public <T extends MobGenericEvent> void subscribeEvent(Class<T> eventClass, Consumer<T> onEvent) {
               BuffManager.this.eventSubscriptions.addSubscription(buffID, eventClass, onEvent);
            }
         });
         if (firstAdd) {
            ab.buff.firstAdd(ab);
         }

         if (old != null) {
            ab.onOverridden(old);
         }

         isOverride = false;
      }

      this.submitMobEvent(new BuffAddedEvent(ab, old, sendUpdatePacket));
      if (sendUpdatePacket) {
         if (this.owner.isServer()) {
            this.owner.getLevel().getServer().network.sendToClientsWithEntity(new PacketMobBuff(this.owner.getUniqueID(), ab, forceUpdateBuffs), this.owner);
         } else if (this.owner.isClient()) {
            this.owner.getLevel().getClient().network.sendPacket(new PacketMobBuff(this.owner.getUniqueID(), ab, forceUpdateBuffs));
         }
      }

      if (this.owner.isPlayer && this.owner.isServer() && ((PlayerMob)this.owner).isServerClient()) {
         ServerClient serverClient = ((PlayerMob)this.owner).getServerClient();
         ActiveBuff finalAb = ab;
         JournalChallengeRegistry.handleListeners(
            serverClient,
            BuffGainedJournalChallengeListener.class,
            challenge -> challenge.onBuffGained(serverClient, (PlayerMob)this.owner, finalAb, isOverride)
         );
      }

      if (forceUpdateBuffs) {
         this.forceUpdateBuffs();
      } else {
         this.updateBuffs();
      }

      return old != null ? old : ab;
   }

   public void tickMovement(float delta) {
      if (this.buffs != null) {
         HashSet<Integer> keys;
         synchronized (this.buffs) {
            keys = new HashSet<>(this.movementTickBuffs);
         }

         for (int id : keys) {
            ActiveBuff ab;
            synchronized (this.buffs) {
               ab = this.buffs.get(id);
            }

            if (ab != null && !ab.isRemoved()) {
               ((MovementTickBuff)ab.buff).tickMovement(ab, delta);
            }
         }
      }
   }

   public void addHumanDraws(HumanDrawOptions drawOptions) {
      if (this.buffs != null) {
         HashSet<Integer> keys;
         synchronized (this.buffs) {
            keys = new HashSet<>(this.humanDrawBuffs);
         }

         for (int id : keys) {
            ActiveBuff ab;
            synchronized (this.buffs) {
               ab = this.buffs.get(id);
            }

            if (ab != null && !ab.isRemoved()) {
               ((HumanDrawBuff)ab.buff).addHumanDraw(ab, drawOptions);
            }
         }
      }
   }

   public void addExtraDrawOptions(
      LinkedList<DrawOptions> back, LinkedList<DrawOptions> front, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective
   ) {
      if (this.buffs != null) {
         BuffManager drawBuffManager = this;
         if (this.owner.isRiding()) {
            Mob mount = this.owner.getMount();
            if (mount != null && !mount.shouldDrawRider()) {
               return;
            }
         } else if (this.owner.isMounted() && !this.owner.shouldDrawRider()) {
            Mob rider = this.owner.getRider();
            if (rider != null) {
               drawBuffManager = rider.buffManager;
            }
         }

         HashSet<Integer> keys;
         synchronized (drawBuffManager.buffs) {
            keys = new HashSet<>(drawBuffManager.mobExtraDrawBuffs);
         }

         for (int id : keys) {
            ActiveBuff ab;
            synchronized (drawBuffManager.buffs) {
               ab = drawBuffManager.buffs.get(id);
            }

            if (ab != null && !ab.isRemoved()) {
               ((MobExtraDrawBuff)ab.buff).addBackDrawOptions(ab, back, x, y, tickManager, camera, perspective);
               ((MobExtraDrawBuff)ab.buff).addFrontDrawOptions(ab, front, x, y, tickManager, camera, perspective);
            }
         }
      }
   }

   public void serverTick() {
      boolean update = false;
      if (this.buffs != null) {
         HashSet<Integer> keys;
         synchronized (this.buffs) {
            keys = new HashSet<>(this.buffs.keySet());
         }

         for (int id : keys) {
            ActiveBuff ab;
            synchronized (this.buffs) {
               ab = this.buffs.get(id);
            }

            if (ab != null) {
               update = ab.tickExpired() || update;
               if (ab.isRemoved()) {
                  ab.buff.onRemoved(ab);
                  synchronized (this.buffs) {
                     this.buffs.remove(id);
                     this.eventSubscriptions.removeSubscriptions(id);
                     this.movementTickBuffs.remove(id);
                     this.humanDrawBuffs.remove(id);
                     this.mobExtraDrawBuffs.remove(id);
                  }

                  update = true;
               } else {
                  ab.serverTick();
               }
            }
         }

         if (this.updateModifiers || ++this.updateTimer > 20) {
            update = true;
         }

         if (update) {
            synchronized (this.buffs) {
               this.updateModifiers();
            }
         }
      }
   }

   public void clientTick() {
      boolean update = false;
      if (this.buffs != null) {
         HashSet<Integer> keys;
         synchronized (this.buffs) {
            keys = new HashSet<>(this.buffs.keySet());
         }

         for (int id : keys) {
            ActiveBuff ab;
            synchronized (this.buffs) {
               ab = this.buffs.get(id);
            }

            if (ab != null) {
               update = ab.tickExpired() || update;
               if (ab.isRemoved()) {
                  ab.buff.onRemoved(ab);
                  synchronized (this.buffs) {
                     this.buffs.remove(id);
                     this.eventSubscriptions.removeSubscriptions(id);
                     this.movementTickBuffs.remove(id);
                     this.humanDrawBuffs.remove(id);
                     this.mobExtraDrawBuffs.remove(id);
                  }

                  update = true;
               } else {
                  ab.clientTick();
               }
            }
         }

         if (this.updateModifiers || ++this.updateTimer > 20) {
            update = true;
         }

         if (update) {
            synchronized (this.buffs) {
               this.updateModifiers();
            }
         }
      }
   }

   public void tickDamageOverTime() {
      if (!this.owner.removed()) {
         if (this.owner.canTakeDamage()) {
            AtomicInteger totalDamage = new AtomicInteger();
            this.tickDamageOverTime(BuffModifiers.POISON_DAMAGE_FLAT, BuffModifiers.POISON_DAMAGE, totalDamage);
            this.tickDamageOverTime(BuffModifiers.FIRE_DAMAGE_FLAT, BuffModifiers.FIRE_DAMAGE, totalDamage);
            this.tickDamageOverTime(BuffModifiers.FROST_DAMAGE_FLAT, BuffModifiers.FROST_DAMAGE, totalDamage);
            this.tickDamageOverTime(BuffModifiers.BLEED_DAMAGE_FLAT, BuffModifiers.BLEED_DAMAGE, totalDamage);
            if (this.owner.isClient() && Settings.showDoTText && totalDamage.get() > 0) {
               this.owner.spawnDamageText(totalDamage.get(), 12, false);
            }
         }
      }
   }

   protected void tickDamageOverTime(Modifier<Float> dotModifier, Modifier<Float> damageModifier, AtomicInteger totalDamage) {
      float damageMod = this.getModifier(damageModifier);
      if (!(damageMod <= 0.0F)) {
         if (this.owner.isPlayer) {
            damageMod *= this.owner.getLevel().getWorldSettings().difficulty.damageTakenModifier;
         }

         ModifierContainerLimits<Float> limits = this.getLimits(dotModifier);
         float max = limits.hasMax() ? limits.max() : Float.MAX_VALUE;
         float used = 0.0F;

         for (ActiveBuff buff : this.queryContainers(dotModifier)) {
            float dot = buff.getModifier(dotModifier) * buff.getStacks();
            used += dot;
            if (used > max) {
               dot -= used - max;
            }

            buff.dotBuffer += dot * damageMod / 20.0F;
            if (dot > 0.0F) {
               this.owner.lastCombatTime = this.owner.getWorldEntity().getTime();
            }

            if (buff.dotBuffer >= 1.0F) {
               int damage = (int)buff.dotBuffer;
               buff.dotBuffer -= damage;
               Attacker attacker = buff.getAttacker();
               MobBeforeDamageOverTimeTakenEvent beforeEvent = new MobBeforeDamageOverTimeTakenEvent(buff, damage);
               this.submitMobEvent(beforeEvent);
               int beforeHealth = this.owner.getHealth();
               if (!beforeEvent.isPrevented()) {
                  if (attacker == null) {
                     attacker = Mob.TOO_BUFFED_ATTACKER;
                  }

                  this.owner.setHealth(beforeHealth - damage, attacker);
               }

               this.submitMobEvent(new MobAfterDamageOverTimeTakenEvent(beforeEvent, beforeHealth));
               totalDamage.addAndGet(damage);
            }
         }
      }
   }

   public void updateBuffs() {
      this.updateModifiers = true;
   }

   public void forceUpdateBuffs() {
      synchronized (this.buffs) {
         this.updateModifiers();
      }
   }

   @Override
   protected void updateModifiers() {
      Level level = this.owner.getLevel();
      Performance.record(level == null ? null : level.tickManager(), "updateBuffs", () -> {
         synchronized (this.buffs) {
            this.updateTimer = 0;
            this.updateModifiers = false;
            super.updateModifiers();
            if (this.owner.getHealth() > this.owner.getMaxHealth()) {
               this.owner.setHealth(this.owner.getMaxHealth());
            }

            this.attackers.clear();
            this.buffs.values().stream().map(ActiveBuff::getAttacker).filter(Objects::nonNull).forEach(this.attackers::add);
         }
      });
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      Stream<ModifierValue<?>> out = this.owner.getDefaultModifiers();
      if (this.owner.getLevel() != null) {
         out = Stream.concat(out, this.owner.getLevel().getMobModifiers(this.owner));
      }

      Mob mount = this.owner.getMount();
      if (mount != null) {
         out = Stream.concat(out, mount.getDefaultRiderModifiers());
      }

      return out;
   }

   @Override
   protected Iterable<ActiveBuff> getModifierContainers() {
      synchronized (this.buffs) {
         return this.buffs.values();
      }
   }

   public void submitMobEvent(MobGenericEvent event) {
      this.eventSubscriptions.submitEvent(event);
   }

   public float getBuffDurationModifier(ActiveBuff buff) {
      float modifier = 1.0F;
      if (buff.buff.isPotionBuff()) {
         modifier *= this.getModifier(BuffModifiers.POTION_DURATION);
      }

      return modifier;
   }

   public HashMap<Integer, ActiveBuff> getBuffs() {
      return this.buffs;
   }

   public ArrayList<ActiveBuff> getArrayBuffs() {
      synchronized (this.buffs) {
         return new ArrayList<>(this.getBuffs().values());
      }
   }

   public ActiveBuff getBuff(int id) {
      synchronized (this.buffs) {
         return this.buffs.get(id);
      }
   }

   public ActiveBuff getBuff(Buff buff) {
      synchronized (this.buffs) {
         return this.buffs.get(buff.getID());
      }
   }

   public ActiveBuff getBuff(String type) {
      return this.getBuff(BuffRegistry.getBuffID(type));
   }

   public void clearBuffs() {
      synchronized (this.buffs) {
         this.buffs.clear();
         this.eventSubscriptions.clear();
         this.movementTickBuffs.clear();
         this.humanDrawBuffs.clear();
         this.mobExtraDrawBuffs.clear();
      }

      this.updateBuffs();
   }

   public boolean hasBuff(Buff buff) {
      return this.hasBuff(buff.getID());
   }

   public boolean hasBuff(int id) {
      synchronized (this.buffs) {
         return this.buffs.containsKey(id);
      }
   }

   public boolean hasBuff(String type) {
      return this.hasBuff(BuffRegistry.getBuffID(type));
   }

   public int getBuffDurationLeft(int id) {
      synchronized (this.buffs) {
         ActiveBuff buff = this.buffs.get(id);
         return buff != null ? buff.getDurationLeft() : 0;
      }
   }

   public int getBuffDurationLeft(String type) {
      return this.getBuffDurationLeft(BuffRegistry.getBuffID(type));
   }

   public int getBuffDurationLeft(Buff buff) {
      return this.getBuffDurationLeft(buff.getID());
   }

   public float getBuffDurationLeftSeconds(int id) {
      return this.getBuffDurationLeft(id) / 1000.0F;
   }

   public float getBuffDurationLeftSeconds(String type) {
      return this.getBuffDurationLeft(type) / 1000.0F;
   }

   public float getBuffDurationLeftSeconds(Buff buff) {
      return this.getBuffDurationLeft(buff) / 1000.0F;
   }

   public int getStacks(Buff buff) {
      synchronized (this.buffs) {
         ActiveBuff ab = this.buffs.get(buff.getID());
         return ab != null ? ab.getStacks() : 0;
      }
   }

   public boolean removeStack(Buff buff, boolean fromBeginning, boolean sendUpdatePacket) {
      synchronized (this.buffs) {
         ActiveBuff ab = this.getBuff(buff.getID());
         if (ab != null) {
            if (ab.getStacks() <= 1) {
               this.removeBuff(buff, sendUpdatePacket);
            } else {
               ab.removeStack(fromBeginning);
               if (sendUpdatePacket) {
                  if (this.owner.isServer()) {
                     this.owner.getLevel().getServer().network.sendToClientsWithEntity(new PacketMobBuff(this.owner.getUniqueID(), ab, false), this.owner);
                  } else if (this.owner.isClient()) {
                     this.owner.getLevel().getClient().network.sendPacket(new PacketMobBuff(this.owner.getUniqueID(), ab, false));
                  }
               }
            }

            this.updateBuffs();
            return true;
         } else {
            return false;
         }
      }
   }

   public void removeBuff(Buff buff, boolean sendUpdatePacket) {
      this.removeBuff(buff.getID(), sendUpdatePacket);
   }

   public void removeBuff(int id, boolean sendUpdatePacket) {
      ActiveBuff ab = this.getBuff(id);
      if (ab != null) {
         if (sendUpdatePacket) {
            if (this.owner.isServer()) {
               this.owner.getLevel().getServer().network.sendToClientsWithEntity(new PacketMobBuffRemove(this.owner.getRealUniqueID(), id), this.owner);
            } else if (this.owner.isClient()) {
               this.owner.getLevel().getClient().network.sendPacket(new PacketMobBuffRemove(this.owner.getRealUniqueID(), id));
            }
         }

         synchronized (this.buffs) {
            ab.buff.onRemoved(ab);
            this.buffs.remove(id);
            this.eventSubscriptions.removeSubscriptions(id);
            this.movementTickBuffs.remove(id);
            this.humanDrawBuffs.remove(id);
            this.mobExtraDrawBuffs.remove(id);
            this.updateBuffs();
         }
      }
   }

   public void removeBuff(String type, boolean sendUpdatePacket) {
      this.removeBuff(BuffRegistry.getBuffID(type), sendUpdatePacket);
   }

   public static void sendUpdatePacket(ActiveBuff ab) {
      sendUpdatePacket(ab, false);
   }

   public static void sendUpdatePacket(ActiveBuff ab, boolean forceUpdateBuffs) {
      if (ab.owner.isServer()) {
         ab.owner.getLevel().getServer().network.sendToClientsWithEntity(new PacketMobBuff(ab.owner.getUniqueID(), ab, forceUpdateBuffs), ab.owner);
      } else if (ab.owner.isClient()) {
         ab.owner.getLevel().getClient().network.sendPacket(new PacketMobBuff(ab.owner.getUniqueID(), ab, forceUpdateBuffs));
      }
   }

   public Iterable<Attacker> getAttackers() {
      return this.attackers;
   }

   public Stream<Attacker> streamAttackers() {
      return this.attackers.stream();
   }

   public void setupContentPacket(PacketWriter writer) {
      synchronized (this.buffs) {
         ArrayList<ActiveBuff> valid = this.buffs.values().stream().filter(b -> b.buff.shouldNetworkSync()).collect(Collectors.toCollection(ArrayList::new));
         writer.putNextShortUnsigned(valid.size());

         for (ActiveBuff ab : valid) {
            ab.setupContentPacket(writer);
         }
      }
   }

   public void applyContentPacket(PacketReader reader) {
      this.clearBuffs();
      int size = reader.getNextShortUnsigned();

      for (int i = 0; i < size; i++) {
         ActiveBuff ab = ActiveBuff.fromPacketIterator(reader, this.owner);
         this.addBuff(ab, false, true);
      }

      this.updateBuffs();
   }

   public void addSaveData(SaveData save) {
      synchronized (this.buffs) {
         for (ActiveBuff ab : this.getBuffs().values()) {
            if (ab.buff.shouldSave()) {
               SaveData buff = new SaveData("");
               ab.addSaveData(buff);
               save.addSaveData(buff);
            }
         }
      }
   }

   public void applyLoadData(LoadData save) {
      this.clearBuffs();

      for (LoadData buff : save.getLoadData()) {
         try {
            ActiveBuff ab = ActiveBuff.fromLoadData(buff, this.owner);
            if (ab != null) {
               this.addBuff(ab, false, true, false);
            }
         } catch (Exception e) {
            System.err.println("Could not load buff");
         }
      }

      this.updateBuffs();
   }
}
