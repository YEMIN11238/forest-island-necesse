package necesse.gfx.forms.presets.containerComponent.logicGate;

import necesse.engine.GameEventListener;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.gfx.forms.components.FormHorizontalIntScroll;
import necesse.gfx.forms.components.FormHorizontalScroll;
import necesse.gfx.forms.components.lists.FormStringSelectList;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.gameFont.FontOptions;
import necesse.inventory.container.logicGate.SoundLogicGateContainer;
import necesse.level.gameLogicGate.entities.LogicGateEntity;
import necesse.level.gameLogicGate.entities.SoundLogicGateEntity;

public class SoundLogicGateContainerForm<T extends SoundLogicGateContainer> extends LogicGateContainerForm<T> {
   private final GameEventListener<LogicGateEntity.ApplyPacketEvent> applyListener;
   public FormStringSelectList sounds;
   public FormHorizontalIntScroll semitone;

   public SoundLogicGateContainerForm(Client client, final T container) {
      super(client, 400, 160, container);
      this.addComponent(new FormLocalLabel(container.entity.getLogicGate().getLocalization(), new FontOptions(20), -1, 4, 4));
      this.sounds = this.addComponent(new FormStringSelectList(0, 40, 160, this.getHeight() - 40, SoundLogicGateEntity.getSoundNames()));
      this.sounds.setSelected(container.entity.sound);
      this.sounds.onSelect(e -> {
         this.playTest();
         container.setSound.runAndSend(e.index);
      });
      this.addComponent(new FormLocalLabel("ui", "soundsemitone", new FontOptions(16), 0, 250, 50));
      this.semitone = this.addComponent(
         new FormHorizontalIntScroll(
            220, 75, 60, FormHorizontalScroll.DrawOption.value, new LocalMessage("ui", "soundsemitone"), container.entity.semitone, -12, 12
         )
      );
      this.semitone.onChanged(e -> {
         this.playTest();
         container.setSemitone.runAndSend(e.from.getValue());
      });
      this.applyListener = container.entity.applyPacketEvents.addListener(new GameEventListener<LogicGateEntity.ApplyPacketEvent>() {
         public void onEvent(LogicGateEntity.ApplyPacketEvent event) {
            SoundLogicGateContainerForm.this.sounds.setSelected(container.entity.sound);
            SoundLogicGateContainerForm.this.semitone.setValue(container.entity.semitone);
         }
      });
   }

   public void playTest() {
      int sound = this.sounds.getSelectedIndex();
      SoundLogicGateEntity.playSound(sound, this.semitone.getValue(), this.container.entity.tileX * 32 + 16, this.container.entity.tileY * 32 + 16);
   }

   @Override
   public void dispose() {
      super.dispose();
      this.applyListener.dispose();
   }
}
