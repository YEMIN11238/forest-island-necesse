package necesse.gfx.forms.presets.containerComponent.logicGate;

import necesse.engine.GameEventListener;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.gfx.forms.components.FormBreakLine;
import necesse.gfx.forms.components.FormSlider;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.gameFont.FontOptions;
import necesse.inventory.container.logicGate.TimerLogicGateContainer;
import necesse.level.gameLogicGate.entities.LogicGateEntity;

public class TimerLogicGateContainerForm<T extends TimerLogicGateContainer> extends LogicGateContainerForm<T> {
   private final GameEventListener<LogicGateEntity.ApplyPacketEvent> applyListener;

   public TimerLogicGateContainerForm(Client client, final T container) {
      super(client, 400, 160, container);
      this.addComponent(new FormLocalLabel(container.entity.getLogicGate().getLocalization(), new FontOptions(20), -1, 4, 4));
      this.addWireCheckboxes(10, 40, new LocalMessage("ui", "wireinputs"), container.entity, e -> e.wireInputs, container.setInputs);
      this.addComponent(new FormBreakLine(FormBreakLine.ALIGN_BEGINNING, 135, 45, 100, false));
      this.addWireCheckboxes(150, 40, new LocalMessage("ui", "wireoutputs"), container.entity, e -> e.wireOutputs, container.setOutputs);
      this.addComponent(new FormBreakLine(FormBreakLine.ALIGN_BEGINNING, 265, 45, 100, false));
      this.addComponent(new FormLocalLabel("ui", "timerticks", new FontOptions(16), -1, 280, 40));
      final FormSlider ticks = ((FormSlider)this.addComponent(new FormSlider("", 280, 65, container.entity.timerTicks, 10, 200, 100))).onGrab(e -> {
         if (!e.grabbed) {
            container.setTicks.runAndSend(e.from.getValue());
         }
      });
      ticks.onScroll(e -> container.setTicks.runAndSend(e.from.getValue()));
      ticks.drawValueInPercent = false;
      this.addComponent(
         new FormLocalLabel(new LocalMessage("ui", "timertip", "ticks", 20), new FontOptions(12), -1, 280, ticks.getY() + ticks.getTotalHeight() + 5, 100)
      );
      this.applyListener = container.entity.applyPacketEvents.addListener(new GameEventListener<LogicGateEntity.ApplyPacketEvent>() {
         public void onEvent(LogicGateEntity.ApplyPacketEvent event) {
            ticks.setValue(container.entity.timerTicks);
         }
      });
   }

   @Override
   public void dispose() {
      super.dispose();
      this.applyListener.dispose();
   }
}
