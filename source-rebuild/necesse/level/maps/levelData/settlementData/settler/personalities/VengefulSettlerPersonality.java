package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public class VengefulSettlerPersonality extends SettlerPersonality {
   public static int MILLISECONDS_THOUGHT_DURATION = 7200000;
   public long lastRaidedTime = 0L;

   public VengefulSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_happy", new SimpleSettlerThought("personalities", "vengefulhappy", 30).setPersonalitySubmitter(this)
      );
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addLong("lastRaidedTime", this.lastRaidedTime);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.lastRaidedTime = save.getLong("lastRaidedTime", this.lastRaidedTime, false);
   }

   @Override
   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      SettlerThoughtsList list = new SettlerThoughtsList();
      if (this.lastRaidedTime + MILLISECONDS_THOUGHT_DURATION > this.mob.getTime() && this.lastRaidedTime != 0L) {
         list.addThought(this.getStringID() + "_happy");
      }

      return list;
   }

   @Override
   public void settlementTick(ServerSettlementData data) {
      super.settlementTick(data);
      ServerSettlementData serverData = this.mob.getSettlerSettlementServerData();
      if (serverData != null && serverData.isRaidOngoing()) {
         this.lastRaidedTime = this.mob.getTime();
      }
   }
}
