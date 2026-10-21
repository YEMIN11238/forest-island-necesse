package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementWorkstationRecipeRemoveEvent extends SettlementContainerEvent {
   public final int tileX;
   public final int tileY;
   public final int recipeUniqueID;

   public SettlementWorkstationRecipeRemoveEvent(ServerSettlementData data, int tileX, int tileY, int recipeUniqueID) {
      super(data);
      this.tileX = tileX;
      this.tileY = tileY;
      this.recipeUniqueID = recipeUniqueID;
   }

   public SettlementWorkstationRecipeRemoveEvent(PacketReader reader) {
      super(reader);
      this.tileX = reader.getNextInt();
      this.tileY = reader.getNextInt();
      this.recipeUniqueID = reader.getNextInt();
   }

   @Override
   public void settlementWrite(PacketWriter writer) {
      writer.putNextInt(this.tileX);
      writer.putNextInt(this.tileY);
      writer.putNextInt(this.recipeUniqueID);
   }
}
