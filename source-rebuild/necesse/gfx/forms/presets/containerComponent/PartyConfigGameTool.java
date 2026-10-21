package necesse.gfx.forms.presets.containerComponent;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.stream.Stream;
import necesse.engine.network.client.Client;
import necesse.entity.mobs.Mob;
import necesse.gfx.forms.presets.containerComponent.settlement.SettlementToolHandler;
import necesse.inventory.container.AdventurePartyConfigContainer;
import necesse.level.maps.levelData.settlementData.settler.CommandMob;

public class PartyConfigGameTool extends SelectSettlersContainerGameTool {
   public final AdventurePartyConfigContainer container;

   public PartyConfigGameTool(Client client, SelectedSettlersHandler selectedSettlers, AdventurePartyConfigContainer container) {
      super(client, selectedSettlers);
      this.container = container;
   }

   @Override
   public SettlementToolHandler getCurrentToolHandler() {
      return null;
   }

   @Override
   public Stream<CommandMob> streamAllLevelSettlers(Rectangle selectionBox) {
      return this.client
         .getLevel()
         .entityManager
         .mobs
         .streamInRegionsShape(selectionBox, 1)
         .filter(m -> m instanceof CommandMob)
         .map(m -> (CommandMob)m)
         .filter(m -> m.canBeCommandPartySelected(this.client));
   }

   @Override
   public Stream<SelectSettlersContainerGameTool.HudCommandMob> streamAllHudCommandMobs(Rectangle selectionBox) {
      return Stream.empty();
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
