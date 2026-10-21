package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public class WeatherBoundSettlerPersonality extends SettlerPersonality {
   public WeatherBoundSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_clear", new SimpleSettlerThought("personalities", "weatherboundclear", 20).setPersonalitySubmitter(this)
      );
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_rain", new SimpleSettlerThought("personalities", "weatherboundrain", 0).setPersonalitySubmitter(this)
      );
   }

   @Override
   public void settlementTick(ServerSettlementData data) {
      super.settlementTick(data);
      if (this.mob.getLevel() != null && !this.mob.getActiveThoughts().hasThought(this.getStringID() + "_rain") && this.mob.getLevel().weatherLayer.isRaining()
         )
       {
         this.mob.getActiveThoughts().addThoughtSeconds(this.getStringID() + "_rain", (float)this.mob.getLevel().weatherLayer.getRemainingRainTime() / 1000.0F);
      }
   }

   @Override
   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      if (!this.mob.getActiveThoughts().hasThought(this.getStringID() + "_rain")) {
         SettlerThoughtsList list = new SettlerThoughtsList();
         list.addThought(this.getStringID() + "_clear");
         return list;
      } else {
         return super.getThoughts(bed);
      }
   }
}
