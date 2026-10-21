package necesse.gfx.forms.presets.containerComponent.mob;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.engine.window.GameWindow;
import necesse.gfx.forms.ContainerComponent;
import necesse.inventory.container.mob.ShopContainer;

public class JonasContainerForm<T extends ShopContainer> extends ShopContainerForm<T> {
   public DialogueForm jonasSpeaksForm;
   private final int voiceLines = 5;
   private int currentVoiceLine;

   public JonasContainerForm(Client client, T container) {
      super(client, container);
      this.jonasSpeaksForm = this.addComponent(new DialogueForm("jonasspeaks", this.width, 150, this.maxHeight, true, container.humanShop, true, -1, null));
   }

   @Override
   protected void init() {
      super.init();
      this.currentVoiceLine = 0;
      this.updateJonasDialogue();
      this.makeCurrent(this.jonasSpeaksForm);
   }

   public void updateJonasDialogue() {
      int tipIndex = this.currentVoiceLine < 5 ? this.currentVoiceLine : 0;
      this.jonasSpeaksForm.reset(this.container.humanShop, true, this.container.romanceLevel, new LocalMessage("mobmsg", "friendlyjonastalk" + (tipIndex + 1)));
      this.jonasSpeaksForm.addDialogueOption(new LocalMessage("mobmsg", "friendlyjonasresponse" + (tipIndex + 1)), () -> {
         if (this.currentVoiceLine == 4) {
            this.client.closeContainer(true);
         }

         this.currentVoiceLine = this.currentVoiceLine < 5 ? this.currentVoiceLine + 1 : 0;
         this.updateJonasDialogue();
      });
   }

   @Override
   public void onWindowResized(GameWindow window) {
      super.onWindowResized(window);
      ContainerComponent.setPosFocus(this.jonasSpeaksForm);
   }
}
