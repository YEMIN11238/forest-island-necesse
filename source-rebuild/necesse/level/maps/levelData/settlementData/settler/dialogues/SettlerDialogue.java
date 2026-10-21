package necesse.level.maps.levelData.settlementData.settler.dialogues;

import java.util.ArrayList;
import java.util.Collection;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.IDData;
import necesse.engine.registries.IDDataContainer;
import necesse.engine.registries.SettlerDialogueRegistry;
import necesse.engine.window.GameWindow;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.forms.presets.containerComponent.mob.ShopContainerForm;
import necesse.inventory.container.mob.ShopContainer;
import necesse.inventory.container.mob.ShopContainerPartyResponseEvent;

public class SettlerDialogue implements IDDataContainer {
   public final IDData idData = new IDData();
   public final HumanMob settlerMob;

   @Override
   public IDData getIDData() {
      return this.idData;
   }

   public SettlerDialogue(HumanMob settlerMob) {
      SettlerDialogueRegistry.instance.applyIDData((Class<? extends SettlerDialogue>)this.getClass(), this.idData);
      this.settlerMob = settlerMob;
   }

   protected void setupContainerPacket(PacketWriter writer) {
   }

   protected void applyContainerPacket(PacketReader reader) {
   }

   public void init(ShopContainer container) {
   }

   public void joinAdventureParty(ShopContainer container) {
      if (container.hasSettlerAccess && container.canJoinAdventureParties && container.client.isServer()) {
         ServerClient serverClient = container.client.getServerClient();
         GameMessage message = container.humanShop.willJoinAdventureParty(serverClient);
         new ShopContainerPartyResponseEvent(message).applyAndSendToClient(serverClient);
         if (message == null) {
            this.settlerMob.commandFollow(serverClient, container.client.playerMob);
            this.settlerMob.adventureParty.set(serverClient);
         }
      }
   }

   public void initForm(ShopContainer container, ShopContainerForm<?> containerForm) {
   }

   public void setupDialogueOptions(ShopContainer container, ShopContainerForm<?> containerForm) {
   }

   public <T extends ShopContainer> void onWindowResized(GameWindow window, ShopContainer container, ShopContainerForm<?> containerForm) {
   }

   public static void writeDialoguesList(PacketWriter writer, Collection<SettlerDialogue> dialogues) {
      Packet packet = new Packet();
      PacketWriter listWriter = new PacketWriter(packet);
      listWriter.putNextShortUnsigned(dialogues.size());

      for (SettlerDialogue dialogue : dialogues) {
         listWriter.putNextShortUnsigned(dialogue.getID());
         dialogue.setupContainerPacket(listWriter);
      }

      writer.putNextContentPacket(packet);
   }

   public static ArrayList<SettlerDialogue> readDialoguesList(PacketReader reader, HumanMob mob) {
      Packet listContent = reader.getNextContentPacket();
      PacketReader listReader = new PacketReader(listContent);
      int size = listReader.getNextShortUnsigned();
      ArrayList<SettlerDialogue> list = new ArrayList<>(size);

      for (int i = 0; i < size; i++) {
         int dialogueID = listReader.getNextShortUnsigned();
         SettlerDialogue dialogue = SettlerDialogueRegistry.getNewSettlerDialogue(dialogueID, mob);
         if (dialogue == null) {
            throw new IllegalArgumentException("Could not find SettlerDialogue with ID " + dialogueID);
         }

         dialogue.applyContainerPacket(listReader);
         list.add(dialogue);
      }

      return list;
   }

   public static void skipReadDialoguesList(PacketReader reader) {
      reader.skipNextContentPacket();
   }
}
