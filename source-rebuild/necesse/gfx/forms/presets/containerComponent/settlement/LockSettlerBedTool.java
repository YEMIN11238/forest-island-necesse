package necesse.gfx.forms.presets.containerComponent.settlement;

import java.awt.Color;
import necesse.engine.input.InputEvent;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.window.GameWindow;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.level.gameObject.furniture.SettlerBedObject;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.TilePosition;

public class LockSettlerBedTool extends SelectTileGameTool {
   private final SettlementContainer container;

   public LockSettlerBedTool(SettlementContainer container, Level level) {
      super(level, new LocalMessage("ui", "settlementlockbed"));
      this.container = container;
   }

   @Override
   public DrawOptions getIconTexture(Color color, int drawX, int drawY) {
      return null;
   }

   @Override
   public boolean onSelected(InputEvent event, TilePosition pos) {
      if (pos == null) {
         return true;
      }

      LevelObject lo = pos.object();
      if (lo != null && lo.object instanceof SettlerBedObject) {
         LevelObject master = ((SettlerBedObject)lo.object).getSettlerBedMasterLevelObject(lo.level, lo.tileX, lo.tileY);
         if (master != null && master.object instanceof SettlerBedObject) {
            this.container.lockNoSettlerRoom.runAndSend(master.tileX, master.tileY);
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public GameMessage isValidTile(TilePosition pos) {
      this.lastHoverBounds = null;
      LevelObject lo = pos.object();
      if (lo != null && lo.object instanceof SettlerBedObject) {
         LevelObject master = ((SettlerBedObject)lo.object).getSettlerBedMasterLevelObject(lo.level, lo.tileX, lo.tileY);
         if (master != null && master.object instanceof SettlerBedObject) {
            this.lastHoverBounds = ((SettlerBedObject)master.object).getSettlerBedTileRectangle(master.level, master.tileX, master.tileY);
         }

         return null;
      } else {
         return new LocalMessage("ui", "settlmentnotbed");
      }
   }

   @Override
   public GameWindow.CURSOR getCursor() {
      return GameWindow.CURSOR.LOCK;
   }
}
