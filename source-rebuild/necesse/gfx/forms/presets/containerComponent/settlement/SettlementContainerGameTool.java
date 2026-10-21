package necesse.gfx.forms.presets.containerComponent.settlement;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Stream;
import necesse.engine.network.client.Client;
import necesse.entity.mobs.Mob;
import necesse.gfx.forms.presets.containerComponent.SelectSettlersContainerGameTool;
import necesse.gfx.forms.presets.containerComponent.SelectedSettlersHandler;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.level.maps.levelData.settlementData.settler.CommandMob;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;

public class SettlementContainerGameTool extends SelectSettlersContainerGameTool {
   public final SettlementContainer container;
   public final SettlementContainerForm<?> containerForm;

   public SettlementContainerGameTool(
      Client client, SelectedSettlersHandler selectedSettlers, SettlementContainer container, SettlementContainerForm<?> containerForm
   ) {
      super(client, selectedSettlers);
      this.container = container;
      this.containerForm = containerForm;
   }

   @Override
   public SettlementToolHandler getCurrentToolHandler() {
      return this.containerForm.getCurrentToolHandler();
   }

   @Override
   public Stream<CommandMob> streamAllLevelSettlers(Rectangle selectionBox) {
      return this.containerForm
         .dataManager
         .streamSettlers()
         .map(data -> data.getSettlerMob(this.level))
         .filter(Objects::nonNull)
         .map(SettlerMob::getMob)
         .filter(m -> m instanceof CommandMob)
         .map(m -> (CommandMob)m);
   }

   @Override
   public Stream<SelectSettlersContainerGameTool.HudCommandMob> streamAllHudCommandMobs(Rectangle selectionBox) {
      return this.containerForm.settlerBarCommandMobs.stream();
   }

   @Override
   public void commandAttack(Mob target) {
      this.container.commandsHandler.commandAttackAction.runAndSend(this.selectedSettlers.get(), target);
   }

   @Override
   public void commandGuard(int levelX, int levelY) {
      this.container.commandsHandler.commandGuardAction.runAndSend(this.selectedSettlers.get(), levelX, levelY);
   }

   @Override
   public void commandGuard(ArrayList<Point> movePositions) {
      this.container.commandsHandler.commandGuardAction.runAndSend(this.selectedSettlers.get(), movePositions);
   }
}
