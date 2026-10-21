package necesse.entity.objectEntity;

import java.awt.Point;
import java.awt.Rectangle;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.client.Client;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.OneWorldMigration;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.level.maps.Level;

public class WaystoneObjectEntity extends ObjectEntity {
   public int settlementUniqueID;
   private final GameTexture mapTexture;

   public WaystoneObjectEntity(Level level, int x, int y, GameTexture mapTexture) {
      super(level, "waystone", x, y);
      this.mapTexture = mapTexture;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      if (this.settlementUniqueID != 0) {
         save.addInt("settlementUniqueID", this.settlementUniqueID);
      }
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.settlementUniqueID = save.getInt("settlementUniqueID", this.settlementUniqueID, false);
      Point homeIsland = save.getPoint("homeIsland", null, false);
      if (homeIsland != null && this.isServer()) {
         OneWorldMigration migration = this.getServer().world.oneWorldMigration;
         if (migration != null) {
            this.settlementUniqueID = migration.getOldSettlementAtLevelUniqueID(new LevelIdentifier(homeIsland.x, homeIsland.y, 0));
         }
      }
   }

   @Override
   public boolean shouldDrawOnMap() {
      return true;
   }

   @Override
   public Rectangle drawOnMapBox(double tileScale, boolean isMinimap) {
      return new Rectangle(-16, -16, 32, 32);
   }

   @Override
   public void drawOnMap(TickManager tickManager, Client client, int x, int y, double tileScale, Rectangle drawBounds, boolean isMinimap) {
      this.mapTexture.initDraw().sprite(0, 0, 32, this.mapTexture.getHeight()).size(32).draw(x - 16, y - 16 - 8);
   }

   @Override
   public GameTooltips getMapTooltips() {
      return new StringTooltips(this.getObject().getDisplayName());
   }
}
