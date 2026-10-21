package necesse.level.maps.levelData.settlementData.settler.personalities;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Stream;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.IDData;
import necesse.engine.registries.IDDataContainer;
import necesse.engine.registries.SettlerPersonalityRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobBeforeHitCalculatedEvent;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.MobWasKilledEvent;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.FoundJobGenerator;
import necesse.entity.mobs.job.JobType;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.gfx.GameColor;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.InventoryRange;
import necesse.level.maps.levelData.jobs.AbstractLevelJob;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.SettlementRoom;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.dialogues.SettlerDialogue;

public abstract class SettlerPersonality implements IDDataContainer {
   public final IDData idData = new IDData();
   public final HumanMob mob;

   @Override
   public IDData getIDData() {
      return this.idData;
   }

   public SettlerPersonality(HumanMob mob) {
      this.mob = mob;
   }

   public void onRegistered() {
   }

   public GameMessage getName() {
      return new LocalMessage("personalities", this.getStringID());
   }

   public GameMessage getDescription() {
      return new LocalMessage("personalities", this.getStringID() + "_desc")
         .addReplacement("name", GameColor.ITEM_QUEST.getColorCode() + this.mob.getSettlerName() + GameColor.NO_COLOR.getColorCode());
   }

   public void addSaveData(SaveData save) {
   }

   public void applyLoadData(LoadData save) {
   }

   public void setupSpawnPacket(PacketWriter writer) {
   }

   public void applySpawnPacket(PacketReader reader) {
   }

   public void init() {
   }

   public void gameTick() {
   }

   public void settlementTick(ServerSettlementData data) {
   }

   public void onRoomStatsCalculated(SettlementBed bed, SettlementRoom settlementRoom) {
   }

   public void onPerformedJob(AbstractLevelJob job, JobTypeHandler.TypePriority priority) {
   }

   public void onEquipmentChanged(InventoryRange inventoryRange, int slot, boolean isCosmetic) {
   }

   public void doBeforeHitCalculatedLogic(MobBeforeHitCalculatedEvent event) {
   }

   public void onMobWasHit(MobWasHitEvent event) {
   }

   public void onMobHasAttacked(MobWasHitEvent event) {
   }

   public void onAdventurePartyHasKilledTarget(ServerClient partyOwner, MobWasKilledEvent event) {
   }

   public void onConversationEnded(Mob otherChatter) {
   }

   public void onExpeditionStarted() {
   }

   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      return null;
   }

   public Collection<SettlerDialogue> getDialogues(ServerClient client, ServerSettlementData settlementData, boolean clientHasAccess) {
      return null;
   }

   public GameTooltips getTooltip() {
      return new StringTooltips(this.getDescription().translate(), 400);
   }

   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return null;
   }

   public GameMessage getIdleActivityMessage() {
      return null;
   }

   public float getExpeditionTimeModifier() {
      return 1.0F;
   }

   public Stream<FoundJobGenerator<?>> streamExtraJobs() {
      return null;
   }

   public boolean allowsFindingJobType(JobType jobType) {
      return true;
   }

   public boolean preventsSharingRoomThought() {
      return false;
   }

   public boolean preventsMissingFloorThought() {
      return false;
   }

   public boolean preventsLastFoodEatenThought() {
      return false;
   }

   public boolean preventsDietVarietyThought() {
      return false;
   }

   public HashSet<Integer> getPreferredFoodItemIDs() {
      return null;
   }

   public boolean isBonusPerk() {
      return SettlerPersonalityRegistry.isBonusPerk(this.getID());
   }
}
