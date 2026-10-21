package necesse.level.maps.levelData.settlementData;

public abstract class SettlementVisitorOdds {
   public final String identifier;

   public SettlementVisitorOdds(String identifier) {
      this.identifier = identifier;
   }

   public abstract boolean canSpawn(ServerSettlementData var1);

   public abstract int getTickets(ServerSettlementData var1);

   public abstract SettlementVisitorSpawner getNewVisitorSpawner(ServerSettlementData var1);
}
