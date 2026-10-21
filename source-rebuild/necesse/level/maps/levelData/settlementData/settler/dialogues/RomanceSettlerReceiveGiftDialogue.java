package necesse.level.maps.levelData.settlementData.settler.dialogues;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.mob.ShopContainer;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;

public class RomanceSettlerReceiveGiftDialogue extends RomanceSettlerDialogue {
   protected String giftItemStringID;
   protected int amount;

   public RomanceSettlerReceiveGiftDialogue(HumanMob settlerMob) {
      super(settlerMob);
   }

   public RomanceSettlerReceiveGiftDialogue(HumanMob settlerMob, String giftItemStringID, int amount) {
      super(settlerMob);
      this.giftItemStringID = giftItemStringID;
      this.amount = amount;
   }

   @Override
   protected void setupContainerPacket(PacketWriter writer) {
      super.setupContainerPacket(writer);
      writer.putNextString(this.giftItemStringID);
      writer.putNextInt(this.amount);
   }

   @Override
   protected void applyContainerPacket(PacketReader reader) {
      super.applyContainerPacket(reader);
      this.giftItemStringID = reader.getNextString();
      this.amount = reader.getNextInt();
   }

   @Override
   public boolean onAcceptClicked(ShopContainer container) {
      if (!container.client.isServer()) {
         return false;
      }

      ServerClient serverClient = container.client.getServerClient();
      InventoryItem giftItem = new InventoryItem(this.giftItemStringID, this.amount);
      container.client.playerMob.getInv().addItemsDropRemaining(giftItem, "gift", serverClient.playerMob, true, false);
      PlayerRomanceManager romanceManager = this.romancePersonality.getPlayerManager(serverClient, true);
      romanceManager.startGiftCooldown(GameRandom.globalRandom.getIntBetween(1, 3));
      this.romancePersonality.spawnSettlerHeartParticlesAndHappyExpression();
      this.startYapping(serverClient, this.settlerChatsAfterAccepting, 4.0F, true, null);
      return true;
   }
}
