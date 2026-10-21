package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleScalingHappinessSettlerThought;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public class AdventurerSettlerPersonality extends SettlerPersonality {
   public static int SECONDS_BETWEEN_EACH_STACK_GAIN = 60;
   public static int SECONDS_BETWEEN_EACH_STACK_LOSS = 240;
   public static int BUFFER_OVERFLOW_SECONDS = 1800;
   public static int MAX_THOUGHT_STACKS = 10;
   public float secondsBuffer;

   public AdventurerSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      LocalMessage happyMessage = new LocalMessage("personalities", "adventurerhappy");
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_happy", new SimpleScalingHappinessSettlerThought(happyMessage, 9, -1, 1).setPersonalitySubmitter(this)
      );
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_idle", new SimpleSettlerThought("personalities", "adventureridle", 0).setPersonalitySubmitter(this)
      );
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addFloat("secondsBuffer", this.secondsBuffer);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.secondsBuffer = save.getFloat("secondsBuffer", this.secondsBuffer, false);
   }

   @Override
   public void settlementTick(ServerSettlementData data) {
      super.settlementTick(data);
      if (this.mob.adventureParty.isInAdventureParty()) {
         if (!this.mob.isSettlerWithinSettlement(data.networkData)) {
            int maxSecondsBuffer = SECONDS_BETWEEN_EACH_STACK_GAIN * MAX_THOUGHT_STACKS + BUFFER_OVERFLOW_SECONDS;
            this.secondsBuffer = Math.min(this.secondsBuffer + 1.0F, maxSecondsBuffer);
         }
      } else {
         float increaseToDecreaseRatio = (float)SECONDS_BETWEEN_EACH_STACK_GAIN / SECONDS_BETWEEN_EACH_STACK_LOSS;
         this.secondsBuffer = Math.max(this.secondsBuffer - increaseToDecreaseRatio, 0.0F);
      }
   }

   @Override
   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      SettlerThoughtsList list = new SettlerThoughtsList();
      if (this.secondsBuffer > 0.0F) {
         int stacks = GameMath.limit((int)((this.secondsBuffer + SECONDS_BETWEEN_EACH_STACK_GAIN) / SECONDS_BETWEEN_EACH_STACK_GAIN), 1, MAX_THOUGHT_STACKS);
         list.addThought(this.getStringID() + "_happy", stacks);
      } else {
         list.addThought(this.getStringID() + "_idle");
      }

      return list;
   }
}
