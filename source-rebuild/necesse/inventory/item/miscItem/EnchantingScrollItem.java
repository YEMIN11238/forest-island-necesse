package necesse.inventory.item.miscItem;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.gameNetworkData.GNDItem;
import necesse.engine.network.gameNetworkData.GNDItemEnchantment;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.registries.EnchantmentRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.Container;
import necesse.inventory.container.ContainerActionResult;
import necesse.inventory.container.item.EnchantingScrollContainer;
import necesse.inventory.container.slots.ContainerSlot;
import necesse.inventory.enchants.EquipmentItemEnchant;
import necesse.inventory.enchants.ItemEnchantment;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;

public class EnchantingScrollItem extends Item {
   private final HashMap<String, GameTexture> typeTextures = new HashMap<>();
   public static ArrayList<EnchantingScrollItem.EnchantScrollType> types = new ArrayList<>();

   public EnchantingScrollItem() {
      super(1);
      this.rarity = Item.Rarity.RARE;
      this.setItemCategory("misc");
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      ItemEnchantment enchantment = this.getEnchantment(item);
      if (enchantment != null && enchantment != EquipmentItemEnchant.noEnchant) {
         EnchantingScrollItem.EnchantScrollType type = this.getType(enchantment);
         if (type != null) {
            tooltips.add(type.itemTooltip.apply(enchantment), 300);
         } else {
            tooltips.add("UNKNOWN_ENCHANT_TYPE");
         }

         tooltips.add(Localization.translate("itemtooltip", "singleuse"));
         tooltips.add(Localization.translate("itemtooltip", "rclickinvopentip"));
         tooltips.add(enchantment.getTooltips());
      } else {
         tooltips.add(Localization.translate("itemtooltip", "singleuse"));
         tooltips.add(Localization.translate("itemtooltip", "rclickinvopentip"));
      }

      return tooltips;
   }

   public int getTypeIndex(ItemEnchantment enchantment) {
      if (enchantment != null) {
         for (int i = 0; i < types.size(); i++) {
            EnchantingScrollItem.EnchantScrollType type = types.get(i);
            if (type.isPartOfThis.test(enchantment)) {
               return i;
            }
         }
      }

      return -1;
   }

   public EnchantingScrollItem.EnchantScrollType getType(ItemEnchantment enchantment) {
      int index = this.getTypeIndex(enchantment);
      return index != -1 ? types.get(index) : null;
   }

   @Override
   public GameSprite getItemSprite(InventoryItem item, PlayerMob perspective) {
      ItemEnchantment enchantment = this.getEnchantment(item);
      if (enchantment != null && enchantment != EquipmentItemEnchant.noEnchant) {
         EnchantingScrollItem.EnchantScrollType type = this.getType(enchantment);
         if (type != null) {
            GameTexture texture = this.typeTextures.get(type.stringID);
            if (texture != null) {
               return new GameSprite(texture);
            }
         }
      }

      return super.getItemSprite(item, perspective);
   }

   @Override
   protected void loadItemTextures() {
      super.loadItemTextures();

      for (EnchantingScrollItem.EnchantScrollType type : types) {
         try {
            GameTexture texture = GameTexture.fromFileRaw("items/" + this.getStringID() + "_" + type.stringID);
            this.typeTextures.put(type.stringID, texture);
         } catch (FileNotFoundException var4) {
         }
      }
   }

   @Override
   public Supplier<ContainerActionResult> getInventoryRightClickAction(Container container, InventoryItem item, int slotIndex, ContainerSlot slot) {
      return () -> {
         if (slot.getInventory() == container.getClient().playerMob.getInv().main) {
            if (container.getClient().isServer()) {
               ServerClient client = container.getClient().getServerClient();
               PacketOpenContainer p = new PacketOpenContainer(
                  ContainerRegistry.ENCHANTING_SCROLL_CONTAINER, EnchantingScrollContainer.getContainerContent(client, slotIndex)
               );
               ContainerRegistry.openAndSendContainer(client, p);
            }

            return new ContainerActionResult(-1390943614);
         } else {
            return new ContainerActionResult(-1281215057, Localization.translate("itemtooltip", "rclickinvopenerror"));
         }
      };
   }

   public void setEnchantment(InventoryItem item, int enchantment) {
      item.getGndData().setItem("enchantment", new GNDItemEnchantment(enchantment));
   }

   public int getEnchantmentID(InventoryItem item) {
      GNDItem enchantment = item.getGndData().getItem("enchantment");
      if (enchantment == null) {
         int seed = item.getGndData().getInt("generateScrollSeed");
         if (seed != 0) {
            item.getGndData().clearItem("generateScrollSeed");
            int enchantmentID = getRandomEnchantmentID(seed == -1 ? GameRandom.globalRandom : new GameRandom(seed));
            this.setEnchantment(item, enchantmentID);
            return enchantmentID;
         }
      }

      GNDItemEnchantment enchantmentItem = GNDItemEnchantment.convertEnchantmentID(enchantment);
      item.getGndData().setItem("enchantment", enchantmentItem);
      return enchantmentItem.getRegistryID();
   }

   public ItemEnchantment getEnchantment(InventoryItem item) {
      return EnchantmentRegistry.getEnchantment(this.getEnchantmentID(item), ItemEnchantment.class, EquipmentItemEnchant.noEnchant);
   }

   @Override
   public boolean isSameGNDData(Level level, InventoryItem me, InventoryItem them, String purpose) {
      return me.getGndData().sameKeys(them.getGndData(), "enchantment");
   }

   @Override
   public boolean canCombineItem(Level level, PlayerMob player, InventoryItem me, InventoryItem them, String purpose) {
      return !super.canCombineItem(level, player, me, them, purpose) ? false : this.isSameGNDData(level, me, them, purpose);
   }

   @Override
   public float getBrokerValue(InventoryItem item) {
      return super.getBrokerValue(item) * this.getEnchantment(item).getEnchantCostMod();
   }

   @Override
   public GameMessage getLocalization(InventoryItem item) {
      ItemEnchantment enchant = this.getEnchantment(item);
      return enchant != null && enchant.getID() > 0
         ? new LocalMessage("enchantment", "format", "enchantment", enchant.getLocalization(), "item", super.getLocalization(item))
         : super.getLocalization(item);
   }

   @Override
   public void addDefaultItems(List<InventoryItem> list, PlayerMob player) {
      for (ItemEnchantment enchantment : EnchantmentRegistry.getEnchantments()) {
         if (enchantment.getID() != 0) {
            InventoryItem item = this.getDefaultItem(player, 1);
            this.setEnchantment(item, enchantment.getID());
            list.add(item);
         }
      }
   }

   @Override
   public int compareToSameItem(InventoryItem me, InventoryItem them) {
      ItemEnchantment myEnchantment = this.getEnchantment(me);
      ItemEnchantment theirEnchantment = this.getEnchantment(them);
      if (myEnchantment == theirEnchantment) {
         return super.compareToSameItem(me, them);
      } else if (myEnchantment == null || myEnchantment == EquipmentItemEnchant.noEnchant) {
         return 1;
      } else if (theirEnchantment != null && theirEnchantment != EquipmentItemEnchant.noEnchant) {
         int myIndex = this.getTypeIndex(myEnchantment);
         int theirIndex = this.getTypeIndex(theirEnchantment);
         return myIndex != theirIndex ? Integer.compare(myIndex, theirIndex) : super.compareToSameItem(me, them);
      } else {
         return -1;
      }
   }

   @Override
   public InventoryItem getDefaultLootItem(GameRandom random, int amount) {
      InventoryItem item = super.getDefaultLootItem(random, amount);
      this.setEnchantment(item, getRandomEnchantmentID(random));
      return item;
   }

   public static ItemEnchantment getRandomEnchantment(GameRandom random) {
      TicketSystemList<EnchantingScrollItem.EnchantScrollType> ticketSystem = new TicketSystemList<>();

      for (EnchantingScrollItem.EnchantScrollType type : types) {
         ticketSystem.addObject(type.tickets, type);
      }

      EnchantingScrollItem.EnchantScrollType type = ticketSystem.getRandomObject(random);
      return type.randomEnchantmentGetter.apply(random);
   }

   public static int getRandomEnchantmentID(GameRandom random) {
      ItemEnchantment enchantment = getRandomEnchantment(random);
      return enchantment == null ? 0 : enchantment.getID();
   }

   @Override
   public String getTranslatedTypeName() {
      return Localization.translate("item", "scroll");
   }

   static {
      types.add(
         new EnchantingScrollItem.EnchantScrollType(
            "equipment",
            200,
            enchantment -> EnchantmentRegistry.equipmentEnchantments.contains(enchantment.getID()),
            random -> random.getOneOf(
               EnchantmentRegistry.equipmentEnchantments
                  .stream()
                  .map(EnchantmentRegistry::getEnchantment)
                  .filter(Objects::nonNull)
                  .filter(e -> e.getEnchantCostMod() >= 1.0F)
                  .toArray(ItemEnchantment[]::new)
            ),
            enchantment -> new LocalMessage("itemtooltip", "enchantingscrollequipmenttip", "enchantment", enchantment.getLocalization()),
            enchantment -> new LocalMessage("ui", "enchantscrollequipment")
         )
      );
      types.add(
         new EnchantingScrollItem.EnchantScrollType(
            "tool",
            40,
            enchantment -> EnchantmentRegistry.toolDamageEnchantments.contains(enchantment.getID()),
            random -> random.getOneOf(
               EnchantmentRegistry.toolDamageEnchantments
                  .stream()
                  .map(EnchantmentRegistry::getEnchantment)
                  .filter(Objects::nonNull)
                  .filter(e -> e.getEnchantCostMod() >= 1.0F)
                  .toArray(ItemEnchantment[]::new)
            ),
            enchantment -> new LocalMessage("itemtooltip", "enchantingscrolltooltip", "enchantment", enchantment.getLocalization()),
            enchantment -> new LocalMessage("ui", "enchantscrolltool")
         )
      );
      types.add(
         new EnchantingScrollItem.EnchantScrollType(
            "melee",
            100,
            enchantment -> EnchantmentRegistry.meleeItemEnchantments.contains(enchantment.getID()),
            random -> random.getOneOf(
               EnchantmentRegistry.meleeItemEnchantments
                  .stream()
                  .map(EnchantmentRegistry::getEnchantment)
                  .filter(Objects::nonNull)
                  .filter(e -> e.getEnchantCostMod() >= 1.0F)
                  .toArray(ItemEnchantment[]::new)
            ),
            enchantment -> new LocalMessage("itemtooltip", "enchantingscrollmeleetip", "enchantment", enchantment.getLocalization()),
            enchantment -> new LocalMessage("ui", "enchantscrollmelee")
         )
      );
      types.add(
         new EnchantingScrollItem.EnchantScrollType(
            "ranged",
            100,
            enchantment -> EnchantmentRegistry.rangedItemEnchantments.contains(enchantment.getID()),
            random -> random.getOneOf(
               EnchantmentRegistry.rangedItemEnchantments
                  .stream()
                  .map(EnchantmentRegistry::getEnchantment)
                  .filter(Objects::nonNull)
                  .filter(e -> e.getEnchantCostMod() >= 1.0F)
                  .toArray(ItemEnchantment[]::new)
            ),
            enchantment -> new LocalMessage("itemtooltip", "enchantingscrollrangedtip", "enchantment", enchantment.getLocalization()),
            enchantment -> new LocalMessage("ui", "enchantscrollranged")
         )
      );
      types.add(
         new EnchantingScrollItem.EnchantScrollType(
            "magic",
            100,
            enchantment -> EnchantmentRegistry.magicItemEnchantments.contains(enchantment.getID()),
            random -> random.getOneOf(
               EnchantmentRegistry.magicItemEnchantments
                  .stream()
                  .map(EnchantmentRegistry::getEnchantment)
                  .filter(Objects::nonNull)
                  .filter(e -> e.getEnchantCostMod() >= 1.0F)
                  .toArray(ItemEnchantment[]::new)
            ),
            enchantment -> new LocalMessage("itemtooltip", "enchantingscrollmagictip", "enchantment", enchantment.getLocalization()),
            enchantment -> new LocalMessage("ui", "enchantscrollmagic")
         )
      );
      types.add(
         new EnchantingScrollItem.EnchantScrollType(
            "summon",
            100,
            enchantment -> EnchantmentRegistry.summonItemEnchantments.contains(enchantment.getID()),
            random -> random.getOneOf(
               EnchantmentRegistry.summonItemEnchantments
                  .stream()
                  .map(EnchantmentRegistry::getEnchantment)
                  .filter(Objects::nonNull)
                  .filter(e -> e.getEnchantCostMod() >= 1.0F)
                  .toArray(ItemEnchantment[]::new)
            ),
            enchantment -> new LocalMessage("itemtooltip", "enchantingscrollsummontip", "enchantment", enchantment.getLocalization()),
            enchantment -> new LocalMessage("ui", "enchantscrollsummon")
         )
      );
   }

   public static class EnchantScrollType {
      public String stringID;
      public int tickets;
      public Predicate<ItemEnchantment> isPartOfThis;
      public Function<GameRandom, ItemEnchantment> randomEnchantmentGetter;
      public Function<ItemEnchantment, GameMessage> itemTooltip;
      public Function<ItemEnchantment, GameMessage> enchantTip;

      public EnchantScrollType(
         String stringID,
         int tickets,
         Predicate<ItemEnchantment> isPartOfThis,
         Function<GameRandom, ItemEnchantment> randomEnchantmentGetter,
         Function<ItemEnchantment, GameMessage> itemTooltip,
         Function<ItemEnchantment, GameMessage> enchantTip
      ) {
         this.stringID = stringID;
         this.tickets = tickets;
         this.isPartOfThis = isPartOfThis;
         this.randomEnchantmentGetter = randomEnchantmentGetter;
         this.itemTooltip = itemTooltip;
         this.enchantTip = enchantTip;
      }
   }
}
