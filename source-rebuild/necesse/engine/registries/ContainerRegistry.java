package necesse.engine.registries;

import necesse.engine.modLoader.LoadedMod;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.ElderHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.AlchemistHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.entity.mobs.friendly.human.humanShop.MageHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.ShopContainerData;
import necesse.entity.mobs.friendly.human.humanShop.StylistHumanMob;
import necesse.entity.objectEntity.CartographerTableObjectEntity;
import necesse.entity.objectEntity.FallenAltarObjectEntity;
import necesse.entity.objectEntity.FueledIncineratorObjectEntity;
import necesse.entity.objectEntity.FueledInventoryObjectEntity;
import necesse.entity.objectEntity.FueledProcessingInventoryObjectEntity;
import necesse.entity.objectEntity.FueledRefrigeratorObjectEntity;
import necesse.entity.objectEntity.GlyphTrapObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.SalvageStationObjectEntity;
import necesse.entity.objectEntity.ShippingChestObjectEntity;
import necesse.entity.objectEntity.SignObjectEntity;
import necesse.entity.objectEntity.UpgradeStationObjectEntity;
import necesse.entity.objectEntity.interfaces.OEInventory;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.presets.containerComponent.EndPlayerRelationshipContainerForm;
import necesse.gfx.forms.presets.containerComponent.PartyConfigContainerForm;
import necesse.gfx.forms.presets.containerComponent.PvPTeamsContainerForm;
import necesse.gfx.forms.presets.containerComponent.QuestsContainerForm;
import necesse.gfx.forms.presets.containerComponent.SettlementNameContainerForm;
import necesse.gfx.forms.presets.containerComponent.SleepContainerForm;
import necesse.gfx.forms.presets.containerComponent.StartPlayerRelationshipContainerForm;
import necesse.gfx.forms.presets.containerComponent.TeleportToTeamContainerForm;
import necesse.gfx.forms.presets.containerComponent.TravelContainerComponent;
import necesse.gfx.forms.presets.containerComponent.creative.CreativeTeleportToPlayerContainerForm;
import necesse.gfx.forms.presets.containerComponent.item.CloudItemContainerForm;
import necesse.gfx.forms.presets.containerComponent.item.CraftingGuideContainerForm;
import necesse.gfx.forms.presets.containerComponent.item.EnchantingScrollContainerForm;
import necesse.gfx.forms.presets.containerComponent.item.ItemInventoryContainerForm;
import necesse.gfx.forms.presets.containerComponent.item.PortableMusicPlayerContainerForm;
import necesse.gfx.forms.presets.containerComponent.item.RecipeBookContainerForm;
import necesse.gfx.forms.presets.containerComponent.item.RenameItemContainerForm;
import necesse.gfx.forms.presets.containerComponent.item.WrappingPaperContainerForm;
import necesse.gfx.forms.presets.containerComponent.journal.JournalContainerForm;
import necesse.gfx.forms.presets.containerComponent.logicGate.BufferLogicGateContainerForm;
import necesse.gfx.forms.presets.containerComponent.logicGate.CountdownLogicGateContainerForm;
import necesse.gfx.forms.presets.containerComponent.logicGate.CountdownRelayLogicGateContainerForm;
import necesse.gfx.forms.presets.containerComponent.logicGate.CounterLogicGateContainerForm;
import necesse.gfx.forms.presets.containerComponent.logicGate.DelayLogicGateContainerForm;
import necesse.gfx.forms.presets.containerComponent.logicGate.SRLatchLogicGateContainerForm;
import necesse.gfx.forms.presets.containerComponent.logicGate.SensorLogicGateContainerForm;
import necesse.gfx.forms.presets.containerComponent.logicGate.SimpleLogicGateContainerForm;
import necesse.gfx.forms.presets.containerComponent.logicGate.SoundLogicGateContainerForm;
import necesse.gfx.forms.presets.containerComponent.logicGate.TFlipFlopLogicGateContainerForm;
import necesse.gfx.forms.presets.containerComponent.logicGate.TimerLogicGateContainerForm;
import necesse.gfx.forms.presets.containerComponent.mob.AlchemistContainerForm;
import necesse.gfx.forms.presets.containerComponent.mob.BuyAnimalContainerForm;
import necesse.gfx.forms.presets.containerComponent.mob.ElderContainerForm;
import necesse.gfx.forms.presets.containerComponent.mob.JonasContainerForm;
import necesse.gfx.forms.presets.containerComponent.mob.MageContainerForm;
import necesse.gfx.forms.presets.containerComponent.mob.PawnbrokerContainerForm;
import necesse.gfx.forms.presets.containerComponent.mob.ShopContainerForm;
import necesse.gfx.forms.presets.containerComponent.mob.StylistContainerForm;
import necesse.gfx.forms.presets.containerComponent.mob.TraderHumanContainerForm;
import necesse.gfx.forms.presets.containerComponent.object.ArmorStandContainerForm;
import necesse.gfx.forms.presets.containerComponent.object.CartographersTableForm;
import necesse.gfx.forms.presets.containerComponent.object.CraftingStationContainerForm;
import necesse.gfx.forms.presets.containerComponent.object.DresserContainerForm;
import necesse.gfx.forms.presets.containerComponent.object.FallenAltarContainerForm;
import necesse.gfx.forms.presets.containerComponent.object.FueledCraftingStationContainerForm;
import necesse.gfx.forms.presets.containerComponent.object.FueledIncineratorInventoryContainerForm;
import necesse.gfx.forms.presets.containerComponent.object.FueledOEInventoryContainerForm;
import necesse.gfx.forms.presets.containerComponent.object.FueledProcessingInventoryContainerForm;
import necesse.gfx.forms.presets.containerComponent.object.FueledRefrigeratorInventoryContainerForm;
import necesse.gfx.forms.presets.containerComponent.object.GlyphContainerForm;
import necesse.gfx.forms.presets.containerComponent.object.HomestoneContainerForm;
import necesse.gfx.forms.presets.containerComponent.object.MissionBoardContainerForm;
import necesse.gfx.forms.presets.containerComponent.object.MusicPlayerContainerForm;
import necesse.gfx.forms.presets.containerComponent.object.OEInventoryContainerForm;
import necesse.gfx.forms.presets.containerComponent.object.ProcessingInventoryContainerForm;
import necesse.gfx.forms.presets.containerComponent.object.SalvageStationContainerForm;
import necesse.gfx.forms.presets.containerComponent.object.ShippingChestContainerForm;
import necesse.gfx.forms.presets.containerComponent.object.SignContainerForm;
import necesse.gfx.forms.presets.containerComponent.object.UpgradeStationContainerForm;
import necesse.gfx.forms.presets.containerComponent.settlement.SettlementContainerForm;
import necesse.inventory.container.AdventureJournalContainer;
import necesse.inventory.container.AdventurePartyConfigContainer;
import necesse.inventory.container.BedContainer;
import necesse.inventory.container.Container;
import necesse.inventory.container.EndPlayerRelationshipContainer;
import necesse.inventory.container.SettlementNameContainer;
import necesse.inventory.container.StartPlayerRelationshipContainer;
import necesse.inventory.container.creative.CreativeTeleportToPlayerContainer;
import necesse.inventory.container.item.CloudItemContainer;
import necesse.inventory.container.item.CraftingGuideContainer;
import necesse.inventory.container.item.EnchantingScrollContainer;
import necesse.inventory.container.item.ItemInventoryContainer;
import necesse.inventory.container.item.PortableMusicPlayerContainer;
import necesse.inventory.container.item.RecipeBookContainer;
import necesse.inventory.container.item.RenameItemContainer;
import necesse.inventory.container.item.TeleportationScrollContainer;
import necesse.inventory.container.item.WrappingPaperContainer;
import necesse.inventory.container.logicGate.BufferLogicGateContainer;
import necesse.inventory.container.logicGate.CountdownLogicGateContainer;
import necesse.inventory.container.logicGate.CountdownRelayLogicGateContainer;
import necesse.inventory.container.logicGate.CounterLogicGateContainer;
import necesse.inventory.container.logicGate.DelayLogicGateContainer;
import necesse.inventory.container.logicGate.SRLatchLogicGateContainer;
import necesse.inventory.container.logicGate.SensorLogicGateContainer;
import necesse.inventory.container.logicGate.SimpleLogicGateContainer;
import necesse.inventory.container.logicGate.SoundLogicGateContainer;
import necesse.inventory.container.logicGate.TFlipFlopLogicGateContainer;
import necesse.inventory.container.logicGate.TimerLogicGateContainer;
import necesse.inventory.container.mob.AlchemistContainer;
import necesse.inventory.container.mob.BuyAnimalContainer;
import necesse.inventory.container.mob.ElderContainer;
import necesse.inventory.container.mob.MageContainer;
import necesse.inventory.container.mob.PawnbrokerContainer;
import necesse.inventory.container.mob.ShopContainer;
import necesse.inventory.container.mob.StylistContainer;
import necesse.inventory.container.mob.TraderHumanContainer;
import necesse.inventory.container.object.CartographersTableContainer;
import necesse.inventory.container.object.CraftingStationContainer;
import necesse.inventory.container.object.FueledCraftingStationContainer;
import necesse.inventory.container.object.FueledIncineratorInventoryContainer;
import necesse.inventory.container.object.FueledOEInventoryContainer;
import necesse.inventory.container.object.FueledProcessingOEInventoryContainer;
import necesse.inventory.container.object.FueledRefrigeratorInventoryContainer;
import necesse.inventory.container.object.GlyphTrapContainer;
import necesse.inventory.container.object.HomestoneContainer;
import necesse.inventory.container.object.MusicPlayerContainer;
import necesse.inventory.container.object.OEInventoryContainer;
import necesse.inventory.container.object.SalvageStationContainer;
import necesse.inventory.container.object.ShippingChestInventoryContainer;
import necesse.inventory.container.object.SignContainer;
import necesse.inventory.container.object.TeleportationStoneContainer;
import necesse.inventory.container.object.UpgradeStationContainer;
import necesse.inventory.container.object.fallenAltar.FallenAltarContainer;
import necesse.inventory.container.object.missionBoard.MissionBoardContainer;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.inventory.container.settlement.events.SettlementDataEvent;
import necesse.inventory.container.teams.PvPTeamsContainer;
import necesse.inventory.container.travel.TravelContainer;
import necesse.inventory.container.travel.TravelScrollContainer;
import necesse.inventory.container.travel.TravelStoneContainer;
import necesse.level.gameLogicGate.entities.BufferLogicGateEntity;
import necesse.level.gameLogicGate.entities.CountdownLogicGateEntity;
import necesse.level.gameLogicGate.entities.CountdownRelayLogicGateEntity;
import necesse.level.gameLogicGate.entities.CounterLogicGateEntity;
import necesse.level.gameLogicGate.entities.DelayLogicGateEntity;
import necesse.level.gameLogicGate.entities.LogicGateEntity;
import necesse.level.gameLogicGate.entities.SRLatchLogicGateEntity;
import necesse.level.gameLogicGate.entities.SensorLogicGateEntity;
import necesse.level.gameLogicGate.entities.SimpleLogicGateEntity;
import necesse.level.gameLogicGate.entities.SoundLogicGateEntity;
import necesse.level.gameLogicGate.entities.TFlipFlopLogicGateEntity;
import necesse.level.gameLogicGate.entities.TimerLogicGateEntity;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;

public class ContainerRegistry extends GameRegistry<ContainerRegistry.ContainerRegistryElement> {
   public static int OE_INVENTORY_CONTAINER;
   public static int ARMOR_STAND_CONTAINER;
   public static int DRESSER_CONTAINER;
   public static int PROCESSING_INVENTORY_CONTAINER;
   public static int MUSIC_PLAYER_CONTAINER;
   public static int CRAFTING_STATION_CONTAINER;
   public static int FUELED_CRAFTING_STATION_CONTAINER;
   public static int FUELED_OE_INVENTORY_CONTAINER;
   public static int FUELED_PROCESSING_STATION_CONTAINER;
   public static int FUELED_REFRIGERATOR_INVENTORY_CONTAINER;
   public static int INCINERATOR_INVENTORY_CONTAINER;
   public static int SIMPLE_LOGIC_GATE_CONTAINER;
   public static int BUFFER_LOGIC_GATE_CONTAINER;
   public static int DELAY_LOGIC_GATE_CONTAINER;
   public static int SRLATCH_LOGIC_GATE_CONTAINER;
   public static int COUNTER_LOGIC_GATE_CONTAINER;
   public static int SENSOR_LOGIC_GATE_CONTAINER;
   public static int SOUND_LOGIC_GATE_CONTAINER;
   public static int TFLIPFLOP_LOGIC_GATE_CONTAINER;
   public static int TIMER_LOGIC_GATE_CONTAINER;
   public static int COUNTDOWN_LOGIC_GATE_CONTAINER;
   public static int COUNTDOWN_RELAY_LOGIC_GATE_CONTAINER;
   public static int ELDER_CONTAINER;
   public static int SHOP_CONTAINER;
   public static int MAGE_CONTAINER;
   public static int STYLIST_CONTAINER;
   public static int PAWNBROKER_CONTAINER;
   public static int ALCHEMIST_CONTAINER;
   public static int BUY_ANIMAL_CONTAINER;
   public static int JONAS_CONTAINER;
   public static int SIGN_CONTAINER;
   public static int SETTLEMENT_NAME_CONTAINER;
   public static int SETTLEMENT_CONTAINER;
   public static int TRAVEL_CONTAINER;
   public static int TRAVEL_SCROLL_CONTAINER;
   public static int TRAVEL_STONE_CONTAINER;
   public static int TELEPORTATION_SCROLL_CONTAINER;
   public static int TELEPORTATION_STONE_CONTAINER;
   public static int CRAFTING_GUIDE_CONTAINER;
   public static int RECIPE_BOOK_CONTAINER;
   public static int ENCHANTING_SCROLL_CONTAINER;
   public static int PARTY_CONFIG_CONTAINER;
   public static int PVP_TEAMS_CONTAINER;
   public static int QUESTS_CONTAINER;
   public static int JOURNAL_CONTAINER;
   public static int GLYPH_TRAP_CONTAINER;
   public static int CARTOGRAPHER_TABLE_CONTAINER;
   public static int TRAVELING_MERCHANT_CONTAINER;
   public static int MISSION_BOARD_CONTAINER;
   public static int SHIPPING_CHEST_CONTAINER;
   public static int WRAPPING_PAPER_CONTAINER;
   public static int BED_CONTAINER;
   public static int START_PLAYER_RELATIONSHIP_CONTAINER;
   public static int END_PLAYER_RELATIONSHIP_CONTAINER;
   public static int CLOUD_INVENTORY_CONTAINER;
   public static int HOMESTONE_CONTAINER;
   public static int ITEM_INVENTORY_CONTAINER;
   public static int ITEM_MUSIC_PLAYER_CONTAINER;
   public static int RENAME_ITEM_CONTAINER;
   public static int FALLEN_ALTAR_CONTAINER;
   public static int UPGRADE_STATION_CONTAINER;
   public static int SALVAGE_STATION_CONTAINER;
   public static int CREATIVE_TELEPORT_TO_PLAYER_CONTAINER;
   public static final ContainerRegistry instance = new ContainerRegistry();

   private ContainerRegistry() {
      super("Container", 65535, false);
   }

   @Override
   public void registerCore() {
      OE_INVENTORY_CONTAINER = registerSettlementDependantOEContainer(
         (client, uniqueSeed, settlement, oe, content) -> new OEInventoryContainerForm<>(
            client, new OEInventoryContainer(client.getClient(), uniqueSeed, settlement, (OEInventory)oe, new PacketReader(content))
         ),
         (client, uniqueSeed, settlement, oe, content, serverObject) -> new OEInventoryContainer(
            client, uniqueSeed, settlement, (OEInventory)oe, new PacketReader(content)
         )
      );
      ARMOR_STAND_CONTAINER = registerSettlementDependantOEContainer(
         (client, uniqueSeed, settlement, oe, content) -> new ArmorStandContainerForm(
            client, new OEInventoryContainer(client.getClient(), uniqueSeed, settlement, (OEInventory)oe, new PacketReader(content))
         ),
         (client, uniqueSeed, settlement, oe, content, serverObject) -> new OEInventoryContainer(
            client, uniqueSeed, settlement, (OEInventory)oe, new PacketReader(content)
         )
      );
      DRESSER_CONTAINER = registerSettlementDependantOEContainer(
         (client, uniqueSeed, settlement, oe, content) -> new DresserContainerForm(
            client, new OEInventoryContainer(client.getClient(), uniqueSeed, settlement, (OEInventory)oe, new PacketReader(content))
         ),
         (client, uniqueSeed, settlement, oe, content, serverObject) -> new OEInventoryContainer(
            client, uniqueSeed, settlement, (OEInventory)oe, new PacketReader(content)
         )
      );
      PROCESSING_INVENTORY_CONTAINER = registerSettlementDependantOEContainer(
         (client, uniqueSeed, settlement, oe, content) -> new ProcessingInventoryContainerForm(
            client, new OEInventoryContainer(client.getClient(), uniqueSeed, settlement, (OEInventory)oe, new PacketReader(content))
         ),
         (client, uniqueSeed, settlement, oe, content, serverObject) -> new OEInventoryContainer(
            client, uniqueSeed, settlement, (OEInventory)oe, new PacketReader(content)
         )
      );
      MUSIC_PLAYER_CONTAINER = registerSettlementDependantOEContainer(
         (client, uniqueSeed, settlement, oe, content) -> new MusicPlayerContainerForm(
            client, new MusicPlayerContainer(client.getClient(), uniqueSeed, settlement, (OEInventory)oe, new PacketReader(content))
         ),
         (client, uniqueSeed, settlement, oe, content, serverObject) -> new MusicPlayerContainer(
            client, uniqueSeed, settlement, (OEInventory)oe, new PacketReader(content)
         )
      );
      CRAFTING_STATION_CONTAINER = registerSettlementDependantLOContainer(
         (client, uniqueSeed, settlement, lo, content) -> new CraftingStationContainerForm<>(
            client, new CraftingStationContainer(client.getClient(), uniqueSeed, settlement, lo, new PacketReader(content))
         ),
         (client, uniqueSeed, settlement, lo, content, serverObject) -> new CraftingStationContainer(
            client, uniqueSeed, settlement, lo, new PacketReader(content)
         )
      );
      FUELED_CRAFTING_STATION_CONTAINER = registerSettlementDependantOEContainer(
         (client, uniqueSeed, settlement, oe, content) -> new FueledCraftingStationContainerForm<>(
            client, new FueledCraftingStationContainer(client.getClient(), uniqueSeed, settlement, (FueledInventoryObjectEntity)oe, new PacketReader(content))
         ),
         (client, uniqueSeed, settlement, oe, content, serverObject) -> new FueledCraftingStationContainer(
            client, uniqueSeed, settlement, (FueledInventoryObjectEntity)oe, new PacketReader(content)
         )
      );
      FUELED_OE_INVENTORY_CONTAINER = registerSettlementDependantOEContainer(
         (client, uniqueSeed, settlement, oe, content) -> new FueledOEInventoryContainerForm<>(
            client, new FueledOEInventoryContainer(client.getClient(), uniqueSeed, settlement, (OEInventory)oe, new PacketReader(content))
         ),
         (client, uniqueSeed, settlement, oe, content, serverObject) -> new FueledOEInventoryContainer(
            client, uniqueSeed, settlement, (OEInventory)oe, new PacketReader(content)
         )
      );
      FUELED_PROCESSING_STATION_CONTAINER = registerSettlementDependantOEContainer(
         (client, uniqueSeed, settlement, oe, content) -> new FueledProcessingInventoryContainerForm(
            client,
            new FueledProcessingOEInventoryContainer(
               client.getClient(), uniqueSeed, settlement, (FueledProcessingInventoryObjectEntity)oe, new PacketReader(content)
            )
         ),
         (client, uniqueSeed, settlement, oe, content, serverObject) -> new FueledProcessingOEInventoryContainer(
            client, uniqueSeed, settlement, (FueledProcessingInventoryObjectEntity)oe, new PacketReader(content)
         )
      );
      FUELED_REFRIGERATOR_INVENTORY_CONTAINER = registerSettlementDependantOEContainer(
         (client, uniqueSeed, settlement, oe, content) -> new FueledRefrigeratorInventoryContainerForm<>(
            client,
            new FueledRefrigeratorInventoryContainer(client.getClient(), uniqueSeed, settlement, (FueledRefrigeratorObjectEntity)oe, new PacketReader(content))
         ),
         (client, uniqueSeed, settlement, oe, content, serverObject) -> new FueledRefrigeratorInventoryContainer(
            client, uniqueSeed, settlement, (FueledRefrigeratorObjectEntity)oe, new PacketReader(content)
         )
      );
      INCINERATOR_INVENTORY_CONTAINER = registerSettlementDependantOEContainer(
         (client, uniqueSeed, settlement, oe, content) -> new FueledIncineratorInventoryContainerForm<>(
            client,
            new FueledIncineratorInventoryContainer(client.getClient(), uniqueSeed, settlement, (FueledIncineratorObjectEntity)oe, new PacketReader(content))
         ),
         (client, uniqueSeed, settlement, oe, content, serverObject) -> new FueledIncineratorInventoryContainer(
            client, uniqueSeed, settlement, (FueledIncineratorObjectEntity)oe, new PacketReader(content)
         )
      );
      GLYPH_TRAP_CONTAINER = registerOEContainer(
         (client, uniqueSeed, oe, content) -> new GlyphContainerForm<>(
            client, new GlyphTrapContainer(client.getClient(), uniqueSeed, (GlyphTrapObjectEntity)oe)
         ),
         (client, uniqueSeed, oe, content, serverObject) -> new GlyphTrapContainer(client, uniqueSeed, (GlyphTrapObjectEntity)oe)
      );
      CARTOGRAPHER_TABLE_CONTAINER = registerOEContainer(
         (client, uniqueSeed, oe, content) -> new CartographersTableForm<>(
            client, new CartographersTableContainer(client.getClient(), uniqueSeed, (CartographerTableObjectEntity)oe)
         ),
         (client, uniqueSeed, oe, content, serverObject) -> new CartographersTableContainer(client, uniqueSeed, (CartographerTableObjectEntity)oe)
      );
      SIMPLE_LOGIC_GATE_CONTAINER = registerLogicGateContainer(
         (client, uniqueSeed, lge, content) -> new SimpleLogicGateContainerForm<>(
            client, new SimpleLogicGateContainer(client.getClient(), uniqueSeed, (SimpleLogicGateEntity)lge)
         ),
         (client, uniqueSeed, lge, content, serverObject) -> new SimpleLogicGateContainer(client, uniqueSeed, (SimpleLogicGateEntity)lge)
      );
      BUFFER_LOGIC_GATE_CONTAINER = registerLogicGateContainer(
         (client, uniqueSeed, lge, content) -> new BufferLogicGateContainerForm<>(
            client, new BufferLogicGateContainer(client.getClient(), uniqueSeed, (BufferLogicGateEntity)lge)
         ),
         (client, uniqueSeed, lge, content, serverObject) -> new BufferLogicGateContainer(client, uniqueSeed, (BufferLogicGateEntity)lge)
      );
      DELAY_LOGIC_GATE_CONTAINER = registerLogicGateContainer(
         (client, uniqueSeed, lge, content) -> new DelayLogicGateContainerForm<>(
            client, new DelayLogicGateContainer(client.getClient(), uniqueSeed, (DelayLogicGateEntity)lge)
         ),
         (client, uniqueSeed, lge, content, serverObject) -> new DelayLogicGateContainer(client, uniqueSeed, (DelayLogicGateEntity)lge)
      );
      SRLATCH_LOGIC_GATE_CONTAINER = registerLogicGateContainer(
         (client, uniqueSeed, lge, content) -> new SRLatchLogicGateContainerForm<>(
            client, new SRLatchLogicGateContainer(client.getClient(), uniqueSeed, (SRLatchLogicGateEntity)lge)
         ),
         (client, uniqueSeed, lge, content, serverObject) -> new SRLatchLogicGateContainer(client, uniqueSeed, (SRLatchLogicGateEntity)lge)
      );
      COUNTER_LOGIC_GATE_CONTAINER = registerLogicGateContainer(
         (client, uniqueSeed, lge, content) -> new CounterLogicGateContainerForm<>(
            client, new CounterLogicGateContainer(client.getClient(), uniqueSeed, (CounterLogicGateEntity)lge)
         ),
         (client, uniqueSeed, lge, content, serverObject) -> new CounterLogicGateContainer(client, uniqueSeed, (CounterLogicGateEntity)lge)
      );
      SENSOR_LOGIC_GATE_CONTAINER = registerLogicGateContainer(
         (client, uniqueSeed, lge, content) -> new SensorLogicGateContainerForm<>(
            client, new SensorLogicGateContainer(client.getClient(), uniqueSeed, (SensorLogicGateEntity)lge)
         ),
         (client, uniqueSeed, lge, content, serverObject) -> new SensorLogicGateContainer(client, uniqueSeed, (SensorLogicGateEntity)lge)
      );
      SOUND_LOGIC_GATE_CONTAINER = registerLogicGateContainer(
         (client, uniqueSeed, lge, content) -> new SoundLogicGateContainerForm<>(
            client, new SoundLogicGateContainer(client.getClient(), uniqueSeed, (SoundLogicGateEntity)lge)
         ),
         (client, uniqueSeed, lge, content, serverObject) -> new SoundLogicGateContainer(client, uniqueSeed, (SoundLogicGateEntity)lge)
      );
      TFLIPFLOP_LOGIC_GATE_CONTAINER = registerLogicGateContainer(
         (client, uniqueSeed, lge, content) -> new TFlipFlopLogicGateContainerForm<>(
            client, new TFlipFlopLogicGateContainer(client.getClient(), uniqueSeed, (TFlipFlopLogicGateEntity)lge)
         ),
         (client, uniqueSeed, lge, content, serverObject) -> new TFlipFlopLogicGateContainer(client, uniqueSeed, (TFlipFlopLogicGateEntity)lge)
      );
      TIMER_LOGIC_GATE_CONTAINER = registerLogicGateContainer(
         (client, uniqueSeed, lge, content) -> new TimerLogicGateContainerForm<>(
            client, new TimerLogicGateContainer(client.getClient(), uniqueSeed, (TimerLogicGateEntity)lge)
         ),
         (client, uniqueSeed, lge, content, serverObject) -> new TimerLogicGateContainer(client, uniqueSeed, (TimerLogicGateEntity)lge)
      );
      COUNTDOWN_LOGIC_GATE_CONTAINER = registerLogicGateContainer(
         (client, uniqueSeed, lge, content) -> new CountdownLogicGateContainerForm<>(
            client, new CountdownLogicGateContainer(client.getClient(), uniqueSeed, (CountdownLogicGateEntity)lge)
         ),
         (client, uniqueSeed, lge, content, serverObject) -> new CountdownLogicGateContainer(client, uniqueSeed, (CountdownLogicGateEntity)lge)
      );
      COUNTDOWN_RELAY_LOGIC_GATE_CONTAINER = registerLogicGateContainer(
         (client, uniqueSeed, lge, content) -> new CountdownRelayLogicGateContainerForm<>(
            client, new CountdownRelayLogicGateContainer(client.getClient(), uniqueSeed, (CountdownRelayLogicGateEntity)lge)
         ),
         (client, uniqueSeed, lge, content, serverObject) -> new CountdownRelayLogicGateContainer(client, uniqueSeed, (CountdownRelayLogicGateEntity)lge)
      );
      ELDER_CONTAINER = registerMobContainer(
         (client, uniqueSeed, mob, content) -> new ElderContainerForm<>(
            client, new ElderContainer(client.getClient(), uniqueSeed, (ElderHumanMob)mob, new PacketReader(content), null)
         ),
         (client, uniqueSeed, mob, content, serverObject) -> new ElderContainer(
            client, uniqueSeed, (ElderHumanMob)mob, new PacketReader(content), (ShopContainerData)serverObject
         )
      );
      SHOP_CONTAINER = registerMobContainer(
         (client, uniqueSeed, mob, content) -> new ShopContainerForm<>(client, new ShopContainer(client.getClient(), uniqueSeed, (HumanShop)mob, content, null)),
         (client, uniqueSeed, mob, content, serverObject) -> new ShopContainer(client, uniqueSeed, (HumanShop)mob, content, (ShopContainerData)serverObject)
      );
      MAGE_CONTAINER = registerMobContainer(
         (client, uniqueSeed, mob, content) -> new MageContainerForm<>(
            client, new MageContainer(client.getClient(), uniqueSeed, (MageHumanMob)mob, new PacketReader(content), null)
         ),
         (client, uniqueSeed, mob, content, serverObject) -> new MageContainer(
            client, uniqueSeed, (MageHumanMob)mob, new PacketReader(content), (ShopContainerData)serverObject
         )
      );
      STYLIST_CONTAINER = registerMobContainer(
         (client, uniqueSeed, mob, content) -> new StylistContainerForm<>(
            client, new StylistContainer(client.getClient(), uniqueSeed, (StylistHumanMob)mob, new PacketReader(content), null)
         ),
         (client, uniqueSeed, mob, content, serverObject) -> new StylistContainer(
            client, uniqueSeed, (StylistHumanMob)mob, new PacketReader(content), (ShopContainerData)serverObject
         )
      );
      PAWNBROKER_CONTAINER = registerMobContainer(
         (client, uniqueSeed, mob, content) -> new PawnbrokerContainerForm<>(
            client, new PawnbrokerContainer(client.getClient(), uniqueSeed, (HumanShop)mob, new PacketReader(content), null)
         ),
         (client, uniqueSeed, mob, content, serverObject) -> new PawnbrokerContainer(
            client, uniqueSeed, (HumanShop)mob, new PacketReader(content), (ShopContainerData)serverObject
         )
      );
      TRAVELING_MERCHANT_CONTAINER = registerMobContainer(
         (client, uniqueSeed, mob, content) -> new TraderHumanContainerForm<>(
            client, new TraderHumanContainer(client.getClient(), uniqueSeed, (HumanShop)mob, new PacketReader(content), null)
         ),
         (client, uniqueSeed, mob, content, serverObject) -> new TraderHumanContainer(
            client, uniqueSeed, (HumanShop)mob, new PacketReader(content), (ShopContainerData)serverObject
         )
      );
      ALCHEMIST_CONTAINER = registerMobContainer(
         (client, uniqueSeed, mob, content) -> new AlchemistContainerForm<>(
            client, new AlchemistContainer(client.getClient(), uniqueSeed, (AlchemistHumanMob)mob, content, null)
         ),
         (client, uniqueSeed, mob, content, serverObject) -> new AlchemistContainer(
            client, uniqueSeed, (AlchemistHumanMob)mob, content, (ShopContainerData)serverObject
         )
      );
      BUY_ANIMAL_CONTAINER = registerMobContainer(
         (client, uniqueSeed, mob, content) -> new BuyAnimalContainerForm<>(client, new BuyAnimalContainer(client.getClient(), uniqueSeed, mob, content)),
         (client, uniqueSeed, mob, content, serverObject) -> new BuyAnimalContainer(client, uniqueSeed, mob, content)
      );
      JONAS_CONTAINER = registerMobContainer(
         (client, uniqueSeed, mob, content) -> new JonasContainerForm<>(
            client, new ShopContainer(client.getClient(), uniqueSeed, (HumanShop)mob, content, null)
         ),
         (client, uniqueSeed, mob, content, serverObject) -> new ShopContainer(client, uniqueSeed, (HumanShop)mob, content, (ShopContainerData)serverObject)
      );
      SIGN_CONTAINER = registerOEContainer(
         (client, uniqueSeed, oe, content) -> new SignContainerForm<>(client, new SignContainer(client.getClient(), uniqueSeed, (SignObjectEntity)oe)),
         (client, uniqueSeed, oe, content, serverObject) -> new SignContainer(client, uniqueSeed, (SignObjectEntity)oe)
      );
      SETTLEMENT_NAME_CONTAINER = registerContainer(
         (client, uniqueSeed, packet) -> new SettlementNameContainerForm(
            client, new SettlementNameContainer(client.getClient(), uniqueSeed, new PacketReader(packet))
         ),
         (client, uniqueSeed, packet, serverObject) -> new SettlementNameContainer(client, uniqueSeed, new PacketReader(packet))
      );
      SETTLEMENT_CONTAINER = registerSettlementDependantContainer(
         (client, uniqueSeed, settlement, content) -> new SettlementContainerForm<>(
            client, new SettlementContainer(client.getClient(), uniqueSeed, settlement, content)
         ),
         (client, uniqueSeed, settlement, content, serverObject) -> new SettlementContainer(client, uniqueSeed, settlement, content)
      );
      TRAVEL_CONTAINER = registerContainer(
         (client, uniqueSeed, content) -> new TravelContainerComponent<>(client, new TravelContainer(client.getClient(), uniqueSeed, content)),
         (client, uniqueSeed, content, serverObject) -> new TravelContainer(client, uniqueSeed, content)
      );
      TRAVEL_SCROLL_CONTAINER = registerContainer(
         (client, uniqueSeed, content) -> new TravelContainerComponent<>(client, new TravelScrollContainer(client.getClient(), uniqueSeed, content)),
         (client, uniqueSeed, content, serverObject) -> new TravelScrollContainer(client, uniqueSeed, content)
      );
      TRAVEL_STONE_CONTAINER = registerContainer(
         (client, uniqueSeed, content) -> new TravelContainerComponent<>(client, new TravelStoneContainer(client.getClient(), uniqueSeed, content)),
         (client, uniqueSeed, content, serverObject) -> new TravelStoneContainer(client, uniqueSeed, content)
      );
      TELEPORTATION_SCROLL_CONTAINER = registerContainer(
         (client, uniqueSeed, content) -> new TeleportToTeamContainerForm<>(client, new TeleportationScrollContainer(client.getClient(), uniqueSeed, content)),
         (client, uniqueSeed, content, serverObject) -> new TeleportationScrollContainer(client, uniqueSeed, content)
      );
      TELEPORTATION_STONE_CONTAINER = registerLOContainer(
         (client, uniqueSeed, levelObject, content) -> new TeleportToTeamContainerForm<>(
            client, new TeleportationStoneContainer(client.getClient(), uniqueSeed, levelObject, content)
         ),
         (client, uniqueSeed, levelObject, content, serverObject) -> new TeleportationStoneContainer(client, uniqueSeed, levelObject, content)
      );
      CRAFTING_GUIDE_CONTAINER = registerContainer(
         (client, uniqueSeed, content) -> new CraftingGuideContainerForm<>(client, new CraftingGuideContainer(client.getClient(), uniqueSeed, content)),
         (client, uniqueSeed, content, serverObject) -> new CraftingGuideContainer(client, uniqueSeed, content)
      );
      RECIPE_BOOK_CONTAINER = registerContainer(
         (client, uniqueSeed, content) -> new RecipeBookContainerForm<>(client, new RecipeBookContainer(client.getClient(), uniqueSeed, content)),
         (client, uniqueSeed, content, serverObject) -> new RecipeBookContainer(client, uniqueSeed, content)
      );
      ENCHANTING_SCROLL_CONTAINER = registerContainer(
         (client, uniqueSeed, content) -> new EnchantingScrollContainerForm<>(client, new EnchantingScrollContainer(client.getClient(), uniqueSeed, content)),
         (client, uniqueSeed, content, serverObject) -> new EnchantingScrollContainer(client, uniqueSeed, content)
      );
      PARTY_CONFIG_CONTAINER = registerContainer(
         (client, uniqueSeed, content) -> new PartyConfigContainerForm(client, new AdventurePartyConfigContainer(client.getClient(), uniqueSeed, content)),
         (client, uniqueSeed, content, serverObject) -> new AdventurePartyConfigContainer(client, uniqueSeed, content)
      );
      PVP_TEAMS_CONTAINER = registerContainer(
         (client, uniqueSeed, content) -> new PvPTeamsContainerForm(client, new PvPTeamsContainer(client.getClient(), uniqueSeed, content)),
         (client, uniqueSeed, content, serverObject) -> new PvPTeamsContainer(client, uniqueSeed, content)
      );
      QUESTS_CONTAINER = registerContainer(
         (client, uniqueSeed, content) -> new QuestsContainerForm(client, new Container(client.getClient(), uniqueSeed)),
         (client, uniqueSeed, content, serverObject) -> new Container(client, uniqueSeed)
      );
      JOURNAL_CONTAINER = registerContainer(
         (client, uniqueSeed, content) -> new JournalContainerForm(client, new AdventureJournalContainer(client.getClient(), uniqueSeed)),
         (client, uniqueSeed, content, serverObject) -> new AdventureJournalContainer(client, uniqueSeed)
      );
      MISSION_BOARD_CONTAINER = registerSettlementDependantContainer(
         (client, uniqueSeed, settlement, content) -> new MissionBoardContainerForm<>(
            client, new MissionBoardContainer(client.getClient(), uniqueSeed, settlement, content)
         ),
         (client, uniqueSeed, settlement, content, serverObject) -> new MissionBoardContainer(client, uniqueSeed, settlement, content)
      );
      SHIPPING_CHEST_CONTAINER = registerSettlementDependantOEContainer(
         (client, uniqueSeed, settlement, oe, content) -> new ShippingChestContainerForm<>(
            client, new ShippingChestInventoryContainer(client.getClient(), uniqueSeed, settlement, (ShippingChestObjectEntity)oe, new PacketReader(content))
         ),
         (client, uniqueSeed, settlement, oe, content, serverObject) -> new ShippingChestInventoryContainer(
            client, uniqueSeed, settlement, (ShippingChestObjectEntity)oe, new PacketReader(content)
         )
      );
      WRAPPING_PAPER_CONTAINER = registerContainer(
         (client, uniqueSeed, content) -> new WrappingPaperContainerForm<>(client, new WrappingPaperContainer(client.getClient(), uniqueSeed, content)),
         (client, uniqueSeed, content, serverObject) -> new WrappingPaperContainer(client, uniqueSeed, content)
      );
      BED_CONTAINER = registerOEContainer(
         (client, uniqueSeed, objectEntity, content) -> new SleepContainerForm(client, new BedContainer(client.getClient(), uniqueSeed, objectEntity, content)),
         (client, uniqueSeed, objectEntity, content, serverObject) -> new BedContainer(client, uniqueSeed, objectEntity, content)
      );
      START_PLAYER_RELATIONSHIP_CONTAINER = registerContainer(
         (client, uniqueSeed, content) -> new StartPlayerRelationshipContainerForm(
            client, new StartPlayerRelationshipContainer(client.getClient(), uniqueSeed, new PacketReader(content))
         ),
         (client, uniqueSeed, content, serverObject) -> new StartPlayerRelationshipContainer(client, uniqueSeed, new PacketReader(content))
      );
      END_PLAYER_RELATIONSHIP_CONTAINER = registerContainer(
         (client, uniqueSeed, content) -> new EndPlayerRelationshipContainerForm(
            client, new EndPlayerRelationshipContainer(client.getClient(), uniqueSeed, new PacketReader(content))
         ),
         (client, uniqueSeed, content, serverObject) -> new EndPlayerRelationshipContainer(client, uniqueSeed, new PacketReader(content))
      );
      CLOUD_INVENTORY_CONTAINER = registerContainer(
         (client, uniqueSeed, content) -> new CloudItemContainerForm<>(client, new CloudItemContainer(client.getClient(), uniqueSeed, content)),
         (client, uniqueSeed, content, serverObject) -> new CloudItemContainer(client, uniqueSeed, content)
      );
      HOMESTONE_CONTAINER = registerSettlementDependantLOContainer(
         (client, uniqueSeed, settlement, levelObject, content) -> new HomestoneContainerForm<>(
            client, new HomestoneContainer(client.getClient(), uniqueSeed, settlement, levelObject, content)
         ),
         (client, uniqueSeed, settlement, levelObject, content, serverObject) -> new HomestoneContainer(client, uniqueSeed, settlement, levelObject, content)
      );
      ITEM_INVENTORY_CONTAINER = registerContainer(
         (client, uniqueSeed, packet) -> new ItemInventoryContainerForm<>(client, new ItemInventoryContainer(client.getClient(), uniqueSeed, packet)),
         (client, uniqueSeed, packet, serverObject) -> new ItemInventoryContainer(client, uniqueSeed, packet)
      );
      ITEM_MUSIC_PLAYER_CONTAINER = registerContainer(
         (client, uniqueSeed, packet) -> new PortableMusicPlayerContainerForm(client, new PortableMusicPlayerContainer(client.getClient(), uniqueSeed, packet)),
         (client, uniqueSeed, packet, serverObject) -> new PortableMusicPlayerContainer(client, uniqueSeed, packet)
      );
      RENAME_ITEM_CONTAINER = registerContainer(
         (client, uniqueSeed, packet) -> new RenameItemContainerForm<>(client, new RenameItemContainer(client.getClient(), uniqueSeed, packet)),
         (client, uniqueSeed, packet, serverObject) -> new RenameItemContainer(client, uniqueSeed, packet)
      );
      FALLEN_ALTAR_CONTAINER = registerOEContainer(
         (client, uniqueSeed, oe, content) -> new FallenAltarContainerForm(
            client, new FallenAltarContainer(client.getClient(), uniqueSeed, (FallenAltarObjectEntity)oe, content)
         ),
         (client, uniqueSeed, oe, content, serverObject) -> new FallenAltarContainer(client, uniqueSeed, (FallenAltarObjectEntity)oe, content)
      );
      UPGRADE_STATION_CONTAINER = registerOEContainer(
         (client, uniqueSeed, oe, content) -> new UpgradeStationContainerForm<>(
            client, new UpgradeStationContainer(client.getClient(), uniqueSeed, (UpgradeStationObjectEntity)oe, new PacketReader(content))
         ),
         (client, uniqueSeed, oe, content, serverObject) -> new UpgradeStationContainer(
            client, uniqueSeed, (UpgradeStationObjectEntity)oe, new PacketReader(content)
         )
      );
      SALVAGE_STATION_CONTAINER = registerOEContainer(
         (client, uniqueSeed, oe, content) -> new SalvageStationContainerForm<>(
            client, new SalvageStationContainer(client.getClient(), uniqueSeed, (SalvageStationObjectEntity)oe, new PacketReader(content))
         ),
         (client, uniqueSeed, oe, content, serverObject) -> new SalvageStationContainer(
            client, uniqueSeed, (SalvageStationObjectEntity)oe, new PacketReader(content)
         )
      );
      CREATIVE_TELEPORT_TO_PLAYER_CONTAINER = registerContainer(
         (client, uniqueSeed, content) -> new CreativeTeleportToPlayerContainerForm<>(
            client, new CreativeTeleportToPlayerContainer(client.getClient(), uniqueSeed)
         ),
         (client, uniqueSeed, packet, serverObject) -> new CreativeTeleportToPlayerContainer(client, uniqueSeed)
      );
   }

   protected void onRegister(ContainerRegistry.ContainerRegistryElement object, int id, String stringID, boolean isReplace) {
   }

   @Override
   protected void onRegistryClose() {
   }

   public static int registerSettlementDependantOEContainer(
      ContainerRegistry.ClientBiExtraContainerHandler<SettlementDataEvent, ObjectEntity> clientHandler,
      ContainerRegistry.ServerBiExtraContainerHandler<SettlementDataEvent, ObjectEntity> serverHandler
   ) {
      return registerSettlementDependantLOContainer(
         (client, uniqueSeed, settlement, lo, content) -> clientHandler.handle(client, uniqueSeed, settlement, lo.getObjectEntity(), content),
         (client, uniqueSeed, settlement, lo, content, serverObject) -> serverHandler.handle(
            client, uniqueSeed, settlement, lo.getObjectEntity(), content, serverObject
         )
      );
   }

   public static int registerSettlementDependantLOContainer(
      ContainerRegistry.ClientBiExtraContainerHandler<SettlementDataEvent, LevelObject> clientHandler,
      ContainerRegistry.ServerBiExtraContainerHandler<SettlementDataEvent, LevelObject> serverHandler
   ) {
      return registerLevelContainer((client, uniqueSeed, level, content) -> {
         PacketReader reader = new PacketReader(content);
         SettlementDataEvent basicsEvent;
         if (reader.getNextBoolean()) {
            basicsEvent = new SettlementDataEvent(reader);
         } else {
            basicsEvent = null;
         }

         LevelObject lo = level.getLevelObject(reader.getNextInt(), reader.getNextInt());
         return clientHandler.handle(client, uniqueSeed, basicsEvent, lo, reader.getNextContentPacket());
      }, (client, uniqueSeed, level, content, serverObject) -> {
         PacketReader reader = new PacketReader(content);
         SettlementDataEvent basicsEvent;
         if (reader.getNextBoolean()) {
            basicsEvent = new SettlementDataEvent(reader);
         } else {
            basicsEvent = null;
         }

         LevelObject lo = level.getLevelObject(reader.getNextInt(), reader.getNextInt());
         return serverHandler.handle(client, uniqueSeed, basicsEvent, lo, reader.getNextContentPacket(), serverObject);
      });
   }

   public static int registerSettlementDependantMobContainer(
      ContainerRegistry.ClientBiExtraContainerHandler<SettlementDataEvent, Mob> clientHandler,
      ContainerRegistry.ServerBiExtraContainerHandler<SettlementDataEvent, Mob> serverHandler
   ) {
      return registerLevelContainer((client, uniqueSeed, level, content) -> {
         PacketReader reader = new PacketReader(content);
         SettlementDataEvent basicsEvent;
         if (reader.getNextBoolean()) {
            basicsEvent = new SettlementDataEvent(reader);
         } else {
            basicsEvent = null;
         }

         Mob mob = level.entityManager.mobs.get(reader.getNextInt(), false);
         return clientHandler.handle(client, uniqueSeed, basicsEvent, mob, reader.getNextContentPacket());
      }, (client, uniqueSeed, level, content, serverObject) -> {
         PacketReader reader = new PacketReader(content);
         SettlementDataEvent basicsEvent;
         if (reader.getNextBoolean()) {
            basicsEvent = new SettlementDataEvent(reader);
         } else {
            basicsEvent = null;
         }

         Mob mob = level.entityManager.mobs.get(reader.getNextInt(), false);
         return serverHandler.handle(client, uniqueSeed, basicsEvent, mob, reader.getNextContentPacket(), serverObject);
      });
   }

   public static int registerSettlementDependantContainer(
      ContainerRegistry.ClientExtraContainerHandler<SettlementDataEvent> clientHandler,
      ContainerRegistry.ServerExtraContainerHandler<SettlementDataEvent> serverHandler
   ) {
      return registerContainer((client, uniqueSeed, content) -> {
         PacketReader reader = new PacketReader(content);
         SettlementDataEvent basicsEvent;
         if (reader.getNextBoolean()) {
            basicsEvent = new SettlementDataEvent(reader);
         } else {
            basicsEvent = null;
         }

         return clientHandler.handle(client, uniqueSeed, basicsEvent, reader.getNextContentPacket());
      }, (client, uniqueSeed, content, serverObject) -> {
         PacketReader reader = new PacketReader(content);
         SettlementDataEvent basicsEvent;
         if (reader.getNextBoolean()) {
            basicsEvent = new SettlementDataEvent(reader);
         } else {
            basicsEvent = null;
         }

         return serverHandler.handle(client, uniqueSeed, basicsEvent, reader.getNextContentPacket(), serverObject);
      });
   }

   public static int registerLogicGateContainer(
      ContainerRegistry.ClientExtraContainerHandler<LogicGateEntity> clientHandler,
      ContainerRegistry.ServerExtraContainerHandler<LogicGateEntity> serverHandler
   ) {
      return registerLevelContainer((client, uniqueSeed, level, content) -> {
         PacketReader reader = new PacketReader(content);
         LogicGateEntity lge = level.logicLayer.getEntity(reader.getNextInt(), reader.getNextInt());
         return clientHandler.handle(client, uniqueSeed, lge, reader.getNextContentPacket());
      }, (client, uniqueSeed, level, content, serverObject) -> {
         PacketReader reader = new PacketReader(content);
         LogicGateEntity lge = level.logicLayer.getEntity(reader.getNextInt(), reader.getNextInt());
         return serverHandler.handle(client, uniqueSeed, lge, reader.getNextContentPacket(), serverObject);
      });
   }

   public static int registerOEContainer(
      ContainerRegistry.ClientExtraContainerHandler<ObjectEntity> clientHandler, ContainerRegistry.ServerExtraContainerHandler<ObjectEntity> serverHandler
   ) {
      return registerLevelContainer((client, uniqueSeed, level, content) -> {
         PacketReader reader = new PacketReader(content);
         ObjectEntity oe = level.entityManager.getObjectEntity(reader.getNextInt(), reader.getNextInt());
         return clientHandler.handle(client, uniqueSeed, oe, reader.getNextContentPacket());
      }, (client, uniqueSeed, level, content, serverObject) -> {
         PacketReader reader = new PacketReader(content);
         ObjectEntity oe = level.entityManager.getObjectEntity(reader.getNextInt(), reader.getNextInt());
         return serverHandler.handle(client, uniqueSeed, oe, reader.getNextContentPacket(), serverObject);
      });
   }

   public static int registerLOContainer(
      ContainerRegistry.ClientExtraContainerHandler<LevelObject> clientHandler, ContainerRegistry.ServerExtraContainerHandler<LevelObject> serverHandler
   ) {
      return registerLevelContainer((client, uniqueSeed, level, content) -> {
         PacketReader reader = new PacketReader(content);
         LevelObject lo = level.getLevelObject(reader.getNextInt(), reader.getNextInt());
         return clientHandler.handle(client, uniqueSeed, lo, reader.getNextContentPacket());
      }, (client, uniqueSeed, level, content, serverObject) -> {
         PacketReader reader = new PacketReader(content);
         LevelObject lo = level.getLevelObject(reader.getNextInt(), reader.getNextInt());
         return serverHandler.handle(client, uniqueSeed, lo, reader.getNextContentPacket(), serverObject);
      });
   }

   public static int registerMobContainer(
      ContainerRegistry.ClientExtraContainerHandler<Mob> clientHandler, ContainerRegistry.ServerExtraContainerHandler<Mob> serverHandler
   ) {
      return registerLevelContainer((client, uniqueSeed, level, content) -> {
         PacketReader reader = new PacketReader(content);
         Mob mob = level.entityManager.mobs.get(reader.getNextInt(), false);
         return clientHandler.handle(client, uniqueSeed, mob, reader.getNextContentPacket());
      }, (client, uniqueSeed, level, content, serverObject) -> {
         PacketReader reader = new PacketReader(content);
         Mob mob = level.entityManager.mobs.get(reader.getNextInt(), false);
         return serverHandler.handle(client, uniqueSeed, mob, reader.getNextContentPacket(), serverObject);
      });
   }

   public static int registerLevelContainer(
      ContainerRegistry.ClientExtraContainerHandler<Level> clientHandler, ContainerRegistry.ServerExtraContainerHandler<Level> serverHandler
   ) {
      return registerContainer((client, uniqueSeed, content) -> {
         Level level = client.getLevel();
         return clientHandler.handle(client, uniqueSeed, level, content);
      }, (client, uniqueSeed, content, serverObject) -> {
         Level level = client.getServer().world.getLevel(client);
         return serverHandler.handle(client, uniqueSeed, level, content, serverObject);
      });
   }

   public static int registerContainer(ContainerRegistry.ClientContainerHandler clientHandler, ContainerRegistry.ServerContainerHandler serverHandler) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register containers");
      } else {
         return instance.register("container", new ContainerRegistry.ContainerRegistryElement(clientHandler, serverHandler));
      }
   }

   public static void openContainer(int containerID, Client client, int uniqueSeed, Packet content) {
      try {
         ContainerRegistry.ContainerRegistryElement e = instance.getElement(containerID);
         if (e != null) {
            client.openContainerForm(e.clientHandler.handle(client, uniqueSeed, content));
         }
      } catch (Exception e) {
         System.err.println("Error trying to open " + containerID + " client side:");
         e.printStackTrace();
      }
   }

   public static void openContainer(int containerID, ServerClient client, int uniqueSeed, Packet content, Object serverObject) {
      try {
         ContainerRegistry.ContainerRegistryElement e = instance.getElement(containerID);
         if (e != null) {
            client.openContainer(e.serverHandler.handle(client, uniqueSeed, content, serverObject));
         }
      } catch (Exception e) {
         System.err.println("Error trying to open " + containerID + " server side:");
         e.printStackTrace();
      }
   }

   public static void openAndSendContainer(ServerClient client, PacketOpenContainer packet) {
      client.getServer().network.sendPacket(packet, client);
      openContainer(packet.containerID, client, packet.uniqueSeed, packet.content, packet.serverObject);
   }

   @FunctionalInterface
   public interface ClientBiExtraContainerHandler<T, C> {
      ContainerComponent<? extends Container> handle(Client var1, int var2, T var3, C var4, Packet var5);
   }

   @FunctionalInterface
   public interface ClientContainerHandler {
      ContainerComponent<? extends Container> handle(Client var1, int var2, Packet var3);
   }

   @FunctionalInterface
   public interface ClientExtraContainerHandler<T> {
      ContainerComponent<? extends Container> handle(Client var1, int var2, T var3, Packet var4);
   }

   protected static class ContainerRegistryElement implements IDDataContainer {
      public final IDData data = new IDData();
      public final ContainerRegistry.ClientContainerHandler clientHandler;
      public final ContainerRegistry.ServerContainerHandler serverHandler;

      public ContainerRegistryElement(ContainerRegistry.ClientContainerHandler clientHandler, ContainerRegistry.ServerContainerHandler serverHandler) {
         this.clientHandler = clientHandler;
         this.serverHandler = serverHandler;
      }

      @Override
      public IDData getIDData() {
         return this.data;
      }
   }

   @FunctionalInterface
   public interface ServerBiExtraContainerHandler<T, C> {
      Container handle(ServerClient var1, int var2, T var3, C var4, Packet var5, Object var6);
   }

   @FunctionalInterface
   public interface ServerContainerHandler {
      Container handle(ServerClient var1, int var2, Packet var3, Object var4);
   }

   @FunctionalInterface
   public interface ServerExtraContainerHandler<T> {
      Container handle(ServerClient var1, int var2, T var3, Packet var4, Object var5);
   }
}
