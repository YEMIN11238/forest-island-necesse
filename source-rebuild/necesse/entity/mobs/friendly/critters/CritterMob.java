package necesse.entity.mobs.friendly.critters;

import java.util.stream.Stream;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.MobSpawnLocation;
import necesse.entity.mobs.ability.BooleanMobAbility;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.CritterAI;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.friendly.FriendlyMob;
import necesse.gfx.GameResources;
import necesse.level.maps.levelData.jobs.HuntMobLevelJob;

public class CritterMob extends FriendlyMob {
   protected boolean isRunning;
   protected final BooleanMobAbility setRunningAbility;
   public HuntMobLevelJob huntJob;

   public CritterMob() {
      this(10);
   }

   public CritterMob(int health) {
      super(health);
      this.isCritter = true;
      this.canDespawn = true;
      this.setRunningAbility = this.registerAbility(new BooleanMobAbility() {
         @Override
         protected void run(boolean value) {
            boolean oldRunning = CritterMob.this.isRunning;
            CritterMob.this.isRunning = value;
            if (CritterMob.this.isRunning != oldRunning) {
               CritterMob.this.changedRunning();
            }
         }
      });
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.manageHuntJob();
   }

   protected void manageHuntJob() {
      if (this.canTakeDamage()) {
         if (this.huntJob == null) {
            this.huntJob = new HuntMobLevelJob(this);
         }
      } else {
         this.huntJob = null;
      }
   }

   @Override
   public boolean canDespawn() {
      return !this.canDespawn
         ? false
         : GameUtils.streamServerClients(this.getLevel()).noneMatch(c -> this.getDistance(c.playerMob) < CRITTER_SPAWN_AREA.maxSpawnDistance + 100);
   }

   @Override
   public void setupMovementPacket(PacketWriter writer) {
      super.setupMovementPacket(writer);
      writer.putNextBoolean(this.isRunning);
   }

   @Override
   public void applyMovementPacket(PacketReader reader, boolean isDirect) {
      super.applyMovementPacket(reader, isDirect);
      this.isRunning = reader.getNextBoolean();
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new CritterAI<>());
   }

   @Override
   public boolean shouldSave() {
      return this.shouldSave && !this.canDespawn();
   }

   public void changedRunning() {
      this.buffManager.updateBuffs();
   }

   public void setRunning(boolean running) {
      if (running != this.isRunning) {
         this.setRunningAbility.runAndSend(running);
      }
   }

   public boolean isRunning() {
      return this.isRunning;
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return this.isRunning ? this.getRunningModifiers() : super.getDefaultModifiers();
   }

   protected Stream<ModifierValue<?>> getRunningModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.SPEED, 0.5F));
   }

   @Override
   public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
      return new MobSpawnLocation(this, targetX, targetY).checkMobSpawnLocation().validAndApply();
   }

   @Override
   protected SoundSettings getDeathSound() {
      return new SoundSettings(GameResources.squeak);
   }
}
