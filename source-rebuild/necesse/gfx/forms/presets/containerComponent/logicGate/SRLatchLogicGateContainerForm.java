package necesse.gfx.forms.presets.containerComponent.logicGate;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.gfx.forms.components.FormBreakLine;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.gameFont.FontOptions;
import necesse.inventory.container.logicGate.SRLatchLogicGateContainer;

public class SRLatchLogicGateContainerForm<T extends SRLatchLogicGateContainer> extends LogicGateContainerForm<T> {
   public SRLatchLogicGateContainerForm(Client client, T container) {
      super(client, 400, 160, container);
      this.addComponent(new FormLocalLabel(container.entity.getLogicGate().getLocalization(), new FontOptions(20), -1, 4, 4));
      this.addWireCheckboxes(10, 40, new LocalMessage("ui", "rsactivate"), container.entity, e -> e.activateInputs, container.setActivateInputs);
      this.addComponent(new FormBreakLine(FormBreakLine.ALIGN_BEGINNING, 130, 45, 100, false));
      this.addWireCheckboxes(140, 40, new LocalMessage("ui", "rsreset"), container.entity, e -> e.resetInputs, container.setResetInputs);
      this.addComponent(new FormBreakLine(FormBreakLine.ALIGN_BEGINNING, 260, 45, 100, false));
      this.addWireCheckboxes(270, 40, new LocalMessage("ui", "wireoutputs"), container.entity, e -> e.wireOutputs, container.setOutputs);
   }
}
