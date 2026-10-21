package necesse.gfx.forms.presets.containerComponent.logicGate;

import necesse.engine.GameEventListener;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.gfx.forms.components.FormBreakLine;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.events.FormCheckboxesEventHandler;
import necesse.gfx.gameFont.FontOptions;
import necesse.inventory.container.logicGate.CountdownLogicGateContainer;
import necesse.level.gameLogicGate.entities.CountdownLogicGateEntity;
import necesse.level.gameLogicGate.entities.LogicGateEntity;

public class CountdownLogicGateContainerForm<T extends CountdownLogicGateContainer> extends LogicGateContainerForm<T> {
   private final GameEventListener<LogicGateEntity.ApplyPacketEvent> applyListener;

   public CountdownLogicGateContainerForm(Client client, final T container) {
      super(client, 660, 160, container);
      this.addComponent(new FormLocalLabel(container.entity.getLogicGate().getLocalization(), new FontOptions(20), -1, 4, 4));
      this.addWireCheckboxes(10, 40, new LocalMessage("ui", "wireinputs"), container.entity, e -> e.startInputs, container.setStartInputs);
      this.addComponent(new FormBreakLine(FormBreakLine.ALIGN_BEGINNING, 135, 45, 100, false));
      this.addWireCheckboxes(150, 40, new LocalMessage("ui", "rsreset"), container.entity, e -> e.resetInputs, container.setResetInputs);
      this.addComponent(new FormBreakLine(FormBreakLine.ALIGN_BEGINNING, 265, 45, 100, false));
      this.addWireCheckboxes(280, 40, new LocalMessage("ui", "wireoutputs"), container.entity, e -> e.wireOutputs, container.setOutputs);
      this.addComponent(new FormBreakLine(FormBreakLine.ALIGN_BEGINNING, 395, 45, 100, false));
      FormCheckboxesEventHandler handler = new FormCheckboxesEventHandler(
         this.addCheckboxList(
            410, 40, new LocalMessage("ui", "countdownrelaydirs"), 4, CountdownLogicGateEntity::getRelayDirName, container.entity, e -> e.relayDirections
         )
      );
      handler.onClicked(e -> container.setRelayDirections.runAndSend(handler.getStates()));
      this.addComponent(new FormBreakLine(FormBreakLine.ALIGN_BEGINNING, 535, 45, 100, false));
      FormFlow inputFlow = new FormFlow(40);
      this.addComponent((FormLocalLabel)inputFlow.nextY(new FormLocalLabel("ui", "timerticks", new FontOptions(16), -1, 550, 95), 2));
      final FormTextInput ticksInput = this.addComponent(inputFlow.nextY(new FormTextInput(550, 65, FormInputSize.SIZE_24, 100, -1, 20), 5));
      ticksInput.setRegexMatchFull("[0-9]+");
      ticksInput.onSubmit(e -> {
         try {
            container.setCountdownTime.runAndSend(Integer.parseInt(ticksInput.getText()));
         } catch (NumberFormatException ex) {
            ticksInput.setText(String.valueOf(container.entity.totalCountdownTime));
         }
      });
      ticksInput.setText(String.valueOf(container.entity.totalCountdownTime));
      this.addComponent(
         (FormLocalLabel)inputFlow.nextY(new FormLocalLabel(new LocalMessage("ui", "timertip", "ticks", 20), new FontOptions(12), -1, 550, 95, 100), 5)
      );
      this.applyListener = container.entity.applyPacketEvents.addListener(new GameEventListener<LogicGateEntity.ApplyPacketEvent>() {
         public void onEvent(LogicGateEntity.ApplyPacketEvent event) {
            ticksInput.setText(String.valueOf(container.entity.totalCountdownTime));
         }
      });
   }

   @Override
   public void dispose() {
      super.dispose();
      this.applyListener.dispose();
   }
}
