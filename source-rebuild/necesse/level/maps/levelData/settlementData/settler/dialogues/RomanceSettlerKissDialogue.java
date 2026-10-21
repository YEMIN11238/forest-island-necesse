package necesse.level.maps.levelData.settlementData.settler.dialogues;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketMobChat;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameRandom;
import necesse.engine.window.GameWindow;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.presets.containerComponent.mob.DialogueForm;
import necesse.gfx.forms.presets.containerComponent.mob.ShopContainerForm;
import necesse.inventory.container.customAction.EmptyCustomAction;
import necesse.inventory.container.mob.ShopContainer;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.SettlerRomancePersonality;

public class RomanceSettlerKissDialogue extends SettlerDialogue {
   protected SettlerRomancePersonality romancePersonality;
   protected EmptyCustomAction kissAction;
   protected boolean completed;
   protected DialogueForm subForm;

   public RomanceSettlerKissDialogue(HumanMob mob) {
      super(mob);
      this.romancePersonality = mob.getRomancePersonality();
   }

   @Override
   public void init(final ShopContainer container) {
      super.init(container);
      this.kissAction = container.registerAction(
         new EmptyCustomAction() {
            @Override
            protected void run() {
               if (container.client.isServer()) {
                  ServerClient serverClient = container.client.getServerClient();
                  RomanceSettlerKissDialogue.this.romancePersonality.kissAndAddBuff(serverClient);
                  RomanceSettlerKissDialogue.this.settlerMob
                     .getServer()
                     .network
                     .sendToClientsWithEntity(
                        new PacketMobChat(
                           RomanceSettlerKissDialogue.this.settlerMob.getUniqueID(),
                           Localization.translate(
                              "romance",
                              RomanceSettlerKissDialogue.this.romancePersonality.getStringID() + "_kisschat" + GameRandom.globalRandom.getIntBetween(1, 3),
                              "name",
                              serverClient.getName()
                           )
                        ),
                        RomanceSettlerKissDialogue.this.settlerMob
                     );
                  container.close();
               }

               RomanceSettlerKissDialogue.this.completed = true;
            }
         }
      );
   }

   @Override
   public void setupDialogueOptions(ShopContainer container, ShopContainerForm<?> containerForm) {
      super.setupDialogueOptions(container, containerForm);
      containerForm.dialogueForm.addDialogueOption(new LocalMessage("romance", "kiss"), () -> this.kissAction.runAndSend());
   }

   @Override
   public <T extends ShopContainer> void onWindowResized(GameWindow window, ShopContainer container, ShopContainerForm<?> containerForm) {
      super.onWindowResized(window, container, containerForm);
      if (this.subForm != null) {
         ContainerComponent.setPosFocus(this.subForm);
      }
   }
}
