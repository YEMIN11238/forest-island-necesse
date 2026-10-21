package necesse.level.maps.levelData.settlementData.settler;

import necesse.engine.playerStats.PlayerStats;
import necesse.entity.mobs.friendly.human.ElderHumanMob;
import necesse.gfx.HumanLook;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class ElderSettler extends Settler {
   public ElderSettler() {
      super("elderhuman");
   }

   @Override
   public float getArriveAsRecruitAfterDeathChance(ServerSettlementData settlement) {
      return 0.0F;
   }

   @Override
   public boolean canSpawnInSettlement(ServerSettlementData settlement, PlayerStats stats) {
      return settlement.countSettlersWithoutBed() < 2 && !settlement.hasSettler(this);
   }

   @Override
   public boolean canMoveOut(LevelSettler settler, ServerSettlementData settlement) {
      return false;
   }

   @Override
   public boolean canBanish(LevelSettler settler, ServerSettlementData settlement) {
      return false;
   }

   @Override
   public void setDefaultArmor(HumanDrawOptions drawOptions, int settlerSeed, HumanLook look, boolean customLook) {
      drawOptions.helmet(new InventoryItem("elderhat"));
      drawOptions.chestplate(new InventoryItem("eldershirt"));
      drawOptions.boots(new InventoryItem("eldershoes"));
   }

   @Override
   public SettlerMob getNewSettlerMob(Level level, ServerSettlementData settlement) {
      return settlement == null
         ? super.getNewSettlerMob(level, null)
         : level.entityManager
            .mobs
            .streamInRegionsShape(settlement.networkData.getLevelRectangle(), 0)
            .filter(m -> m.getStringID().equals(this.mobStringID))
            .map(m -> (ElderHumanMob)m)
            .filter(m -> !m.isSettler())
            .map(m -> (SettlerMob)m)
            .findFirst()
            .filter(m -> !m.isSettler())
            .orElseGet(() -> super.getNewSettlerMob(level, settlement));
   }
}
