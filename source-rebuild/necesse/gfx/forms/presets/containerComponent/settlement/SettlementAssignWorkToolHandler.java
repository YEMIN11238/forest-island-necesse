package necesse.gfx.forms.presets.containerComponent.settlement;

import java.awt.Point;
import java.util.ArrayList;
import java.util.function.Consumer;
import necesse.engine.Settings;
import necesse.engine.input.Input;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.client.Client;
import necesse.engine.util.GameMath;
import necesse.engine.window.GameWindow;
import necesse.gfx.forms.floatMenu.SelectionFloatMenu;
import necesse.gfx.gameTooltips.InputTooltip;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.zones.SettlementWorkZone;
import necesse.level.maps.multiTile.MultiTile;

public class SettlementAssignWorkToolHandler implements SettlementToolHandler {
   public final Level level;
   public final SettlementContainer container;
   public final SettlementAssignWorkForm<?> workForm;

   public SettlementAssignWorkToolHandler(Client client, SettlementContainer container, SettlementAssignWorkForm<?> workForm) {
      this.level = client.getLevel();
      this.container = container;
      this.workForm = workForm;
   }

   @Override
   public boolean onLeftClick(Point pos) {
      ArrayList<SettlementAssignWorkToolHandler.ConfigureOption> options = this.addOptionsAndTooltips(pos, null, null);
      if (options.isEmpty()) {
         return false;
      }

      if (options.size() == 1) {
         SettlementAssignWorkToolHandler.ConfigureOption first = options.get(0);
         this.workForm.playTickSound();
         first.onClicked.run();
         return true;
      }

      this.workForm.playTickSound();
      SelectionFloatMenu selection = new SelectionFloatMenu(this.workForm);

      for (SettlementAssignWorkToolHandler.ConfigureOption option : options) {
         selection.add(option.str, () -> {
            option.onClicked.run();
            selection.remove();
         });
      }

      this.workForm.getManager().openFloatMenu(selection, -5, -5);
      return true;
   }

   @Override
   public boolean onHover(Point pos, Consumer<ListGameTooltips> setTooltips, Consumer<GameWindow.CURSOR> setCursor) {
      this.addOptionsAndTooltips(pos, setTooltips, setCursor);
      return false;
   }

   public ArrayList<SettlementAssignWorkToolHandler.ConfigureOption> addOptionsAndTooltips(
      Point pos, Consumer<ListGameTooltips> setTooltips, Consumer<GameWindow.CURSOR> setCursor
   ) {
      int tileX = GameMath.getTileCoordinate(pos.x);
      int tileY = GameMath.getTileCoordinate(pos.y);
      ArrayList<SettlementAssignWorkToolHandler.ConfigureOption> options = new ArrayList<>();
      ListGameTooltips tooltips = new ListGameTooltips();
      if (setTooltips != null) {
         setTooltips.accept(tooltips);
      }

      if (!Settings.hideSettlementStorage.get()) {
         for (Point point : this.workForm.storagePositions) {
            MultiTile multiTile = this.level.getObject(point.x, point.y).getMultiTile(this.level, 0, point.x, point.y);
            if (multiTile.getTileRectangle(point.x, point.y).contains(tileX, tileY)) {
               options.add(
                  new SettlementAssignWorkToolHandler.ConfigureOption(
                     Localization.translate("ui", "settlementconfigurestorage"), () -> this.workForm.openStorageConfig(point.x, point.y)
                  )
               );
            }
         }
      }

      if (!Settings.hideSettlementWorkstations.get()) {
         for (Point point : this.workForm.workstationPositions) {
            MultiTile multiTile = this.level.getObject(point.x, point.y).getMultiTile(this.level, 0, point.x, point.y);
            if (multiTile.getTileRectangle(point.x, point.y).contains(tileX, tileY)) {
               options.add(
                  new SettlementAssignWorkToolHandler.ConfigureOption(
                     Localization.translate("ui", "settlementconfigureworkstation"), () -> this.workForm.openWorkstationConfig(point.x, point.y)
                  )
               );
            }
         }
      }

      for (SettlementWorkZone zone : this.workForm.settlementWorkZones.values()) {
         if (!zone.isHiddenSetting() && zone.containsTile(tileX, tileY)) {
            GameMessage displayName = zone.getName();
            if (displayName != null) {
               tooltips.add(displayName);
               if (zone.canConfigure()) {
                  options.add(
                     new SettlementAssignWorkToolHandler.ConfigureOption(
                        Localization.translate("ui", "settlementconfigurezone", "name", displayName.translate()), () -> this.workForm.openWorkZoneConfig(zone)
                     )
                  );
               }
            }
         }
      }

      if (!options.isEmpty()) {
         if (setCursor != null) {
            setCursor.accept(GameWindow.CURSOR.INTERACT);
         }

         if (options.size() == 1) {
            SettlementAssignWorkToolHandler.ConfigureOption first = options.get(0);
            if (Input.lastInputIsController) {
               tooltips.add(new InputTooltip(ControllerInput.MENU_NEXT, first.str));
            } else {
               tooltips.add(new InputTooltip(-100, first.str));
            }
         } else if (Input.lastInputIsController) {
            tooltips.add(new InputTooltip(ControllerInput.MENU_NEXT, Localization.translate("ui", "configurebutton")));
         } else {
            tooltips.add(new InputTooltip(-100, Localization.translate("ui", "configurebutton")));
         }
      }

      return options;
   }

   private static class ConfigureOption {
      public final String str;
      public final Runnable onClicked;

      public ConfigureOption(String str, Runnable onClicked) {
         this.str = str;
         this.onClicked = onClicked;
      }
   }
}
