package necesse.entity.mobs.friendly.human.humanShop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.registries.SettlerPersonalityRegistry;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.mob.PawnbrokerContainer;
import necesse.inventory.lootTable.LootTable;

public class PawnBrokerHumanMob extends HumanShop {
   public PawnBrokerHumanMob() {
      super(500, 200, "pawnbroker");
      this.attackCooldown = 500;
      this.attackAnimTime = 500;
      this.setSwimSpeed(1.0F);
      this.equipmentInventory.setItem(6, new InventoryItem("ironsword"));
      this.shop.forceSaveWealth();
      this.shop.setMaxShopWealth(8000, 800);
   }

   @Override
   public LootTable getLootTable() {
      return super.getLootTable();
   }

   @Override
   protected void setupPersonalities() {
      this.personalities = new ArrayList<>(Collections.singletonList(SettlerPersonalityRegistry.getNewSettlerPersonality("pawnbroker", this)));
   }

   @Override
   protected ArrayList<GameMessage> getMessages(ServerClient client) {
      ArrayList<GameMessage> out = new ArrayList<>(this.getLocalMessages("pawnbrokertalk", 7));
      if (this.isVisitor()) {
         out.addAll(this.getLocalMessages("tpawnbrokertalk", 2));
      }

      return out;
   }

   @Override
   public PacketOpenContainer getOpenShopPacket(Server server, ServerClient client) {
      return PawnbrokerContainer.getBrokerContainerContent(this, client).getPacket(ContainerRegistry.PAWNBROKER_CONTAINER, this);
   }

   @Override
   public long getShopSeed() {
      return this.isVisitor() ? this.getUniqueID() : super.getShopSeed();
   }

   @Override
   public boolean isVisitorShop() {
      return true;
   }

   @Override
   public List<InventoryItem> getRecruitItems(ServerClient client) {
      return this.isTrapped() ? Collections.emptyList() : null;
   }
}
