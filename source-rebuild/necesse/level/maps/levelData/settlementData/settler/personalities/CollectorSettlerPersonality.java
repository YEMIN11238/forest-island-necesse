package necesse.level.maps.levelData.settlementData.settler.personalities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.JobTypeRegistry;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.ComputedValue;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.FoundJobGenerator;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.inventory.recipe.Ingredient;
import necesse.level.maps.levelData.jobs.FindCollectorItemsLevelJob;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.dialogues.CollectorSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.SettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleScalingHappinessSettlerThought;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public abstract class CollectorSettlerPersonality extends SettlerPersonality {
   public static int SECONDS_COOLDOWN_BETWEEN_GIFTS = 1800;
   public static int SECONDS_THOUGHT_DURATION = 7200;
   public long lastGiftTime;
   public boolean allowTakeFromStorage;
   public long lastLookForItemTime;

   public CollectorSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      LocalMessage happyMessage = new LocalMessage("personalities", this.getStringID() + "received");
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_received", new SimpleScalingHappinessSettlerThought(happyMessage, 20, -4, 0).setPersonalitySubmitter(this)
      );
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_idle", new SimpleSettlerThought("personalities", this.getStringID() + "idle", 0).setPersonalitySubmitter(this)
      );
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addLong("lastCollectorGiftTime", this.lastGiftTime);
      save.addBoolean("allowTakeFromStorage", this.allowTakeFromStorage);
      save.addLong("lastLookForItemTime", this.lastLookForItemTime);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.lastGiftTime = save.getLong("lastCollectorGiftTime", this.lastGiftTime, false);
      this.allowTakeFromStorage = save.getBoolean("allowTakeFromStorage", this.allowTakeFromStorage, false);
      this.lastLookForItemTime = save.getLong("lastLookForItemTime", this.lastLookForItemTime, false);
   }

   public boolean canGiveGift() {
      return this.lastGiftTime <= 0L
         || this.mob.getTime() - this.lastGiftTime >= SECONDS_COOLDOWN_BETWEEN_GIFTS * 1000L
         || !this.mob.getActiveThoughts().hasThought(this.getStringID() + "_received");
   }

   public boolean canLookForItem() {
      return this.allowTakeFromStorage && this.canGiveGift() && (this.lastLookForItemTime == 0L || this.mob.getTime() - this.lastLookForItemTime >= 30000L);
   }

   public void onGiftGiven() {
      this.lastGiftTime = this.mob.getTime();
      this.mob.getActiveThoughts().addThoughtSeconds(this.getStringID() + "_received", SECONDS_THOUGHT_DURATION);
   }

   @Override
   public Collection<SettlerDialogue> getDialogues(ServerClient client, ServerSettlementData settlementData, boolean clientHasAccess) {
      ArrayList<SettlerDialogue> list = new ArrayList<>();
      list.add(this.getCollectorDialogue());
      return list;
   }

   public CollectorSettlerDialogue getCollectorDialogue() {
      return new CollectorSettlerDialogue(this.mob, this);
   }

   public abstract Ingredient[] getCollectorCost();

   @Override
   public Stream<FoundJobGenerator<?>> streamExtraJobs() {
      if (this.canLookForItem()) {
         Ingredient[] cost = this.getCollectorCost();
         FindCollectorItemsLevelJob levelJob = new FindCollectorItemsLevelJob(this.mob, this, cost);
         return Stream.of(new FoundJobGenerator<FindCollectorItemsLevelJob>(levelJob) {
            @Override
            public FoundJob<FindCollectorItemsLevelJob> generateJob(EntityJobWorker worker) {
               JobTypeHandler.TypePriority priority = worker.getJobTypeHandler().getPriority(JobTypeRegistry.needsID);
               return priority == null ? null : new FoundJob<FindCollectorItemsLevelJob>(worker, this.levelJob, priority, null) {
                  @Override
                  public void startCooldown(long currentTime) {
                     CollectorSettlerPersonality.this.lastLookForItemTime = this.worker.getMobWorker().getTime();
                  }

                  @Override
                  protected JobSequence getNewSequence() {
                     return FindCollectorItemsLevelJob.getJobSequence(this.worker, this);
                  }
               };
            }
         });
      } else {
         return super.streamExtraJobs();
      }
   }

   @Override
   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      if (!this.mob.getActiveThoughts().hasThought(this.getStringID() + "_received")) {
         SettlerThoughtsList list = new SettlerThoughtsList();
         list.addThought(this.getStringID() + "_idle");
         return list;
      } else {
         return super.getThoughts(bed);
      }
   }
}
