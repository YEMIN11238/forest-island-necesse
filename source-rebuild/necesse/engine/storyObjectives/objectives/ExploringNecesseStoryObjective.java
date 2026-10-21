package necesse.engine.storyObjectives.objectives;

import necesse.engine.GlobalData;
import necesse.engine.input.Control;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.playerStats.stats.ChallengesCompletedStat;
import necesse.engine.registries.JournalChallengeRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.state.MainGame;
import necesse.engine.storyObjectives.StoryObjective;
import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.engine.storyObjectives.listeners.ContainerOpenedObjectiveListener;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.fairType.FairTypeDrawOptions;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.components.FormContentButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormStoryObjectiveComponent;
import necesse.inventory.container.AdventureJournalContainer;
import necesse.inventory.container.Container;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

public class ExploringNecesseStoryObjective extends StoryObjective implements ContainerOpenedObjectiveListener {
   public static LootTable reward = new LootTable(new LootItem("potionpouch"));
   public boolean hasOpenedAdventureJournal;

   public ExploringNecesseStoryObjective(StoryObjectiveManager manager) {
      super(manager);
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      if (this.hasOpenedAdventureJournal) {
         save.addBoolean("hasOpenedAdventureJournal", this.hasOpenedAdventureJournal);
      }
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.hasOpenedAdventureJournal = save.getBoolean("hasOpenedAdventureJournal", this.hasOpenedAdventureJournal, false);
   }

   @Override
   public void writePacket(PacketWriter writer) {
      super.writePacket(writer);
      writer.putNextBoolean(this.hasOpenedAdventureJournal);
   }

   @Override
   public void readPacket(PacketReader reader) {
      super.readPacket(reader);
      this.hasOpenedAdventureJournal = reader.getNextBoolean();
   }

   @Override
   public void serverTickCurrentObjective() {
      super.serverTickCurrentObjective();
      if (this.hasOpenedAdventureJournal && !this.isCompleted()) {
         ChallengesCompletedStat challengesCompleted = this.manager.getServerClient().characterStats().challenges_completed;
         if (challengesCompleted.isChallengeCompleted(JournalChallengeRegistry.BEAT_A_BEAR_ID)
            || challengesCompleted.isChallengeCompleted(JournalChallengeRegistry.MINE_ORE_WITH_EXPLOSIVES_ID)
            || challengesCompleted.isChallengeCompleted(JournalChallengeRegistry.DEFEAT_CAVELING_ID)) {
            this.markCompleted();
         }
      }
   }

   @Override
   public void onContainerOpened(Container container) {
      if (!this.hasOpenedAdventureJournal && container instanceof AdventureJournalContainer) {
         this.hasOpenedAdventureJournal = true;
         this.markDirty();
      }
   }

   @Override
   public DrawOptions getCurrentObjectiveLevelHudDrawOptions(GameCamera camera, PlayerMob perspective) {
      if (!this.isCompleted() && !this.hasOpenedAdventureJournal && !perspective.isInventoryExtended()) {
         LocalMessage openInvMessage = new LocalMessage("objectives", "openinv", "key", TypeParsers.getInputParseString(Control.INVENTORY));
         return StoryObjective.getLevelTextDrawOptions(openInvMessage, perspective.getDrawX(), perspective.getDrawY() - 48, camera, perspective, false);
      } else {
         return super.getCurrentObjectiveLevelHudDrawOptions(camera, perspective);
      }
   }

   @Override
   public void drawCurrentObjectiveOverForms(MainGame mainGame, PlayerMob perspective) {
      super.drawCurrentObjectiveOverForms(mainGame, perspective);
      if (!this.isCompleted() && !this.hasOpenedAdventureJournal && perspective.isInventoryExtended()) {
         LocalMessage openJournalMessage = new LocalMessage("objectives", "openjournal");
         FormContentButton journalButton = ((MainGame)GlobalData.getCurrentState()).formManager.journalButton;
         if (journalButton != null) {
            FairTypeDrawOptions text = getTextDrawOptions(getObjectiveText(openJournalMessage.translate()));
            getTextDrawOptions(text, null, journalButton, 16, -6, true).draw();
         }
      }
   }

   @Override
   public LootTable getRewards() {
      return reward;
   }

   @Override
   public void setupFormComponent(FormStoryObjectiveComponent form, FormFlow flow) {
      form.addTitle(flow, new LocalMessage("objectives", "exploringtitle"));
      form.addObjective(flow, 1, new LocalMessage("objectives", "openjournalobj"), this.isCompleted() || this.hasOpenedAdventureJournal);
      if (this.isCompleted() || this.hasOpenedAdventureJournal) {
         form.addObjective(flow, 2, new LocalMessage("objectives", "completechallenge"), this.isCompleted());
      }

      if (this.isCompleted()) {
         form.addObjective(flow, 3, new LocalMessage("objectives", "elderclaimreward", "elder", TypeParsers.getMobAndNameParseString("elderhuman")), false);
      }

      form.addRewardsText(flow);
   }
}
