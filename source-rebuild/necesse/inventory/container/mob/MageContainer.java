package necesse.inventory.container.mob;

import java.util.Objects;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.EnchantmentRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.entity.mobs.friendly.human.humanShop.MageHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.ShopContainerData;
import necesse.gfx.GameResources;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerTempInventory;
import necesse.inventory.container.customAction.BooleanCustomAction;
import necesse.inventory.container.customAction.ContentCustomAction;
import necesse.inventory.container.customAction.EmptyCustomAction;
import necesse.inventory.container.slots.EnchantableSlot;
import necesse.inventory.enchants.Enchantable;
import necesse.inventory.enchants.ItemEnchantment;
import necesse.level.maps.hudManager.floatText.ItemPickupText;

public class MageContainer extends ShopContainer {
   public final EmptyCustomAction enchantButton;
   public final ContentCustomAction enchantButtonResponse;
   public final BooleanCustomAction setIsEnchanting;
   private boolean isEnchanting;
   public final int ENCHANT_SLOT;
   public MageHumanMob mageMob;
   public final long enchantCostSeed = this.priceSeed * GameRandom.prime(28);
   public final PlayerTempInventory enchantInv;

   public static float getEnchantmentLotteryWeight(int qualityOfLifeLevel, int happinessLevel) {
      return GameMath.lerp(HumanShop.getHappinessPriceDiscountPercent(qualityOfLifeLevel, happinessLevel), -1.0F, 1.0F);
   }

   public MageContainer(final NetworkClient client, int uniqueSeed, MageHumanMob mob, PacketReader contentReader, ShopContainerData serverData) {
      super(client, uniqueSeed, mob, contentReader.getNextContentPacket(), serverData);
      this.enchantInv = client.playerMob.getInv().applyTempInventoryPacket(contentReader.getNextContentPacket(), m -> this.isClosed());
      this.ENCHANT_SLOT = this.addSlot(new EnchantableSlot(this.enchantInv, 0));
      this.addInventoryQuickTransfer(s -> this.isEnchanting, this.ENCHANT_SLOT, this.ENCHANT_SLOT);
      this.mageMob = mob;
      this.isEnchanting = false;
      this.enchantButton = this.registerAction(new EmptyCustomAction() {
         @Override
         protected void run() {
            if (client.isServer()) {
               if (MageContainer.this.canEnchant()) {
                  int enchantCost = MageContainer.this.getEnchantCost();
                  InventoryItem item = MageContainer.this.getSlot(MageContainer.this.ENCHANT_SLOT).getItem();
                  ((Enchantable)item.item).setEnchantment(item, MageContainer.this.getBiasedEnchantmentID(item));
                  if (client.getServerClient().achievementsLoaded()) {
                     client.getServerClient().achievements().ENCHANT_ITEM.markCompleted(client.getServerClient());
                  }

                  client.getServerClient().newStats.money_spent.increment(enchantCost);
                  client.playerMob.getInv().main.removeItems(client.playerMob.getLevel(), client.playerMob, ItemRegistry.getItem("coin"), enchantCost, "buy");
                  client.getServerClient().newStats.items_enchanted.increment(1);
                  Packet itemContent = InventoryItem.getContentPacket(item);
                  MageContainer.this.enchantButtonResponse.runAndSend(itemContent);
               }

               MageContainer.this.getSlot(MageContainer.this.ENCHANT_SLOT).markDirty();
            }
         }
      });
      this.enchantButtonResponse = this.registerAction(new ContentCustomAction() {
         @Override
         protected void run(Packet content) {
            if (client.isClient()) {
               InventoryItem enchantedItem = InventoryItem.fromContentPacket(content);
               client.playerMob.getLevel().hudManager.addElement(new ItemPickupText(client.playerMob, enchantedItem));
               SoundManager.playSound(GameResources.pop, SoundEffect.effect(client.playerMob));
            }
         }
      });
      this.setIsEnchanting = this.registerAction(new BooleanCustomAction() {
         @Override
         protected void run(boolean value) {
            MageContainer.this.isEnchanting = value;
         }
      });
   }

   public int getEnchantCost() {
      if (this.getSlot(this.ENCHANT_SLOT).isClear()) {
         return 0;
      } else {
         InventoryItem item = this.getSlot(this.ENCHANT_SLOT).getItem();
         if (item.item.isEnchantable(item)) {
            Enchantable<?> enchantItem = (Enchantable<?>)item.item;
            GameRandom random = new GameRandom(
               this.enchantCostSeed + (long)item.item.getID() * GameRandom.prime(54) + (long)enchantItem.getEnchantmentID(item) * GameRandom.prime(13)
            );
            return Math.abs((int)(enchantItem.getRandomEnchantCost(item, random, this.settlerQualityOfLife, this.settlerHappiness) * 0.65F));
         } else {
            return 0;
         }
      }
   }

   private int getBiasedEnchantmentID(InventoryItem item) {
      float weight = getEnchantmentLotteryWeight(this.settlerQualityOfLife, this.settlerHappiness);
      TicketSystemList<ItemEnchantment> lottery = getBiasedEnchantmentLottery(item, weight);
      return lottery.isEmpty() ? 0 : lottery.getRandomObject(GameRandom.globalRandom).getID();
   }

   public static TicketSystemList<ItemEnchantment> getBiasedEnchantmentLottery(InventoryItem item, float weight) {
      if (item != null && item.item.isEnchantable(item)) {
         Enchantable<?> enchantItem = (Enchantable<?>)item.item;
         ItemEnchantment[] positiveEnchantments = enchantItem.getValidEnchantmentIDs(item)
            .stream()
            .filter(id -> id != enchantItem.getEnchantmentID(item))
            .map(EnchantmentRegistry::getEnchantment)
            .filter(Objects::nonNull)
            .filter(e -> e.getEnchantCostMod() >= 1.0F)
            .toArray(ItemEnchantment[]::new);
         ItemEnchantment[] negativeEnchantments = enchantItem.getValidEnchantmentIDs(item)
            .stream()
            .filter(id -> id != enchantItem.getEnchantmentID(item))
            .map(EnchantmentRegistry::getEnchantment)
            .filter(Objects::nonNull)
            .filter(e -> e.getEnchantCostMod() <= 1.0F)
            .toArray(ItemEnchantment[]::new);
         float lotteryBias = 2.5F * weight;
         TicketSystemList<ItemEnchantment> lottery = new TicketSystemList<>();

         for (ItemEnchantment positiveEnchantment : positiveEnchantments) {
            float enchantCostMod = Math.abs(positiveEnchantment.getEnchantCostMod() - 1.0F) * 100.0F;
            lottery.addObject(Math.max(100 + (int)(lotteryBias * enchantCostMod), 1), positiveEnchantment);
         }

         for (ItemEnchantment negativeEnchantment : negativeEnchantments) {
            float enchantCostMod = Math.abs(negativeEnchantment.getEnchantCostMod() - 1.0F) * 100.0F;
            lottery.addObject(Math.max(100 - (int)(lotteryBias * enchantCostMod), 1), negativeEnchantment);
         }

         return lottery;
      } else {
         return new TicketSystemList<>();
      }
   }

   public boolean isItemEnchantable() {
      if (this.getSlot(this.ENCHANT_SLOT).isClear()) {
         return false;
      }

      InventoryItem item = this.getSlot(this.ENCHANT_SLOT).getItem();
      return item.item.isEnchantable(item);
   }

   public boolean canEnchant() {
      if (!this.isItemEnchantable()) {
         return false;
      }

      boolean out = !this.getSlot(this.ENCHANT_SLOT).isClear();
      if (out) {
         int amount = this.client
            .playerMob
            .getInv()
            .main
            .getAmount(this.client.playerMob.getLevel(), this.client.playerMob, ItemRegistry.getItem("coin"), "buy");
         if (amount < this.getEnchantCost()) {
            out = false;
         }
      }

      return out;
   }

   public static ShopContainerData getMageContainerContent(MageHumanMob mob, ServerClient client) {
      ShopContainerData baseData = mob.getShopContainerData(client);
      Packet packet = new Packet();
      PacketWriter writer = new PacketWriter(packet);
      writer.putNextContentPacket(baseData.content);
      writer.putNextContentPacket(client.playerMob.getInv().getTempInventoryPacket(1));
      return new ShopContainerData(packet, baseData.shopManager, baseData.dialogues);
   }
}
