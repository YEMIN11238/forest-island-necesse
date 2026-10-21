package necesse.inventory.container.mob;

import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.ServerClient;
import necesse.engine.storyObjectives.StoryObjective;
import necesse.entity.mobs.friendly.human.ElderHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.ShopContainerData;
import necesse.inventory.PlayerTempInventory;
import necesse.inventory.container.customAction.BooleanCustomAction;
import necesse.inventory.container.customAction.IntCustomAction;
import necesse.inventory.container.slots.ContainerSlot;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class ElderContainer extends ShopContainer {
   public final BooleanCustomAction setInCraftingForm;
   public final IntCustomAction claimStoryObjectiveAction;
   private boolean inCraftingForm;
   public final int INGREDIENT_SLOT;
   public final PlayerTempInventory ingredientInv;

   public ElderContainer(final NetworkClient client, int uniqueSeed, ElderHumanMob mob, PacketReader reader, ShopContainerData serverData) {
      super(client, uniqueSeed, mob, reader.getNextContentPacket(), serverData);
      this.thoughts = null;
      this.ingredientInv = client.playerMob.getInv().applyTempInventoryPacket(reader.getNextContentPacket(), m -> this.isClosed());
      this.INGREDIENT_SLOT = this.addSlot(new ContainerSlot(this.ingredientInv, 0));
      this.addInventoryQuickTransfer(s -> this.inCraftingForm, this.INGREDIENT_SLOT, this.INGREDIENT_SLOT);
      this.inCraftingForm = false;
      this.setInCraftingForm = this.registerAction(new BooleanCustomAction() {
         @Override
         protected void run(boolean value) {
            ElderContainer.this.inCraftingForm = value;
         }
      });
      this.claimStoryObjectiveAction = this.registerAction(new IntCustomAction() {
         @Override
         protected void run(int value) {
            if (client.isServer()) {
               ServerClient serverClient = client.getServerClient();
               StoryObjective objective = serverClient.storyManager.getObjective(value);
               if (objective.isCompleted() && !objective.isClaimed()) {
                  ServerSettlementData serverDatax = ElderContainer.this.humanShop.getSettlementServerData();
                  objective.claimRewards(serverDatax);
               } else {
                  objective.markDirty();
               }
            } else if (client.isClient()) {
               Client thisClient = client.getClientClient().getClient();
               StoryObjective objective = thisClient.storyManager.getObjective(value);
               if (objective.isCompleted() && !objective.isClaimed()) {
                  objective.claimRewards(null);
               }
            }
         }
      });
   }

   public static ShopContainerData getElderContainerData(ElderHumanMob mob, ServerClient client) {
      ShopContainerData baseData = mob.getShopContainerData(client);
      Packet packet = new Packet();
      PacketWriter writer = new PacketWriter(packet);
      writer.putNextContentPacket(baseData.content);
      writer.putNextContentPacket(client.playerMob.getInv().getTempInventoryPacket(1));
      return new ShopContainerData(packet, baseData.shopManager, baseData.dialogues);
   }
}
