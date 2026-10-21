package necesse.level.maps.incursion;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;
import necesse.engine.localization.Localization;
import necesse.engine.network.server.Server;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.TicketSystemList;
import necesse.engine.world.WorldEntity;
import necesse.entity.mobs.Mob;
import necesse.entity.objectEntity.FallenAltarObjectEntity;
import necesse.gfx.fairType.FairItemGlyph;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.IncursionMobTierLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import necesse.level.maps.IncursionLevel;

public class GraveyardIncursionBiome extends IncursionBiome {
   public static ChanceLootItemList graveyardMobDrops = new ChanceLootItemList(
      0.01F,
      new OneOfLootItems(
         new IncursionMobTierLootItem(new LootItem("bloodclaw").preventLootMultiplier(), 2),
         new IncursionMobTierLootItem(new LootItem("thecrimsonsky").preventLootMultiplier(), 2),
         new IncursionMobTierLootItem(new LootItem("bloodgrimoire").preventLootMultiplier(), 2),
         new LootItem("bloodstonering").preventLootMultiplier()
      )
   );

   public GraveyardIncursionBiome() {
      super("nightswarm");
      this.requiredPerkStringID = "graveyardtabletcandrop";
   }

   @Override
   public Collection<Item> getExtractionItems(IncursionData incursionData) {
      return Collections.singleton(ItemRegistry.getItem("nightsteelore"));
   }

   @Override
   public LootTable getHuntDrop(IncursionData incursionData) {
      return new LootTable(new ChanceLootItem(0.66F, "phantomdust"));
   }

   @Override
   public LootTable getBossDrop(IncursionData incursionData) {
      return new LootTable(super.getBossDrop(incursionData), LootItem.between("bloodessence", 20, 25));
   }

   @Override
   public ArrayList<FairType> getPrivateDropsDisplay(FontOptions fontOptions) {
      ArrayList<FairType> out = new ArrayList<>();
      FairType fairType = new FairType();
      ArrayList<InventoryItem> inventoryItems = new ArrayList<>();
      InventoryItem bloodclaw = new InventoryItem("bloodclaw", 1);
      bloodclaw.item.setUpgradeTier(bloodclaw, 1.0F);
      inventoryItems.add(bloodclaw);
      InventoryItem thecrimsonsky = new InventoryItem("thecrimsonsky", 1);
      thecrimsonsky.item.setUpgradeTier(thecrimsonsky, 1.0F);
      inventoryItems.add(thecrimsonsky);
      InventoryItem bloodgrimoire = new InventoryItem("bloodgrimoire", 1);
      bloodgrimoire.item.setUpgradeTier(bloodgrimoire, 1.0F);
      inventoryItems.add(bloodgrimoire);
      InventoryItem bloodstonering = new InventoryItem("bloodstonering", 1);
      inventoryItems.add(bloodstonering);
      fairType.append(new FairItemGlyph(fontOptions.getSize(), inventoryItems));
      fairType.append(fontOptions, " " + Localization.translate("incursion", "bloodthemedloot"));
      out.add(fairType);
      return out;
   }

   @Override
   public TicketSystemList<Supplier<IncursionData>> getAvailableIncursions(int tabletTier, IncursionData incursionData) {
      TicketSystemList<Supplier<IncursionData>> system = new TicketSystemList<>();
      int huntTickets = 100;
      int extractionTickets = 100;
      if (incursionData != null) {
         huntTickets = (int)(huntTickets * incursionData.nextIncursionModifiers.getModifier(IncursionDataModifiers.MODIFIER_HUNT_DROPS));
         extractionTickets = (int)(extractionTickets * incursionData.nextIncursionModifiers.getModifier(IncursionDataModifiers.MODIFIER_EXTRACTION_DROPS));
      }

      system.addObject(huntTickets, () -> new BiomeHuntIncursionData(1.0F, this, tabletTier));
      system.addObject(extractionTickets, () -> new BiomeExtractionIncursionData(1.0F, this, tabletTier));
      return system;
   }

   @Override
   public IncursionLevel getNewIncursionLevel(
      FallenAltarObjectEntity altar,
      LevelIdentifier identifier,
      BiomeMissionIncursionData incursion,
      Server server,
      WorldEntity worldEntity,
      AltarData altarData
   ) {
      return new GraveyardIncursionLevel(identifier, incursion, worldEntity, altarData);
   }

   @Override
   public ArrayList<Color> getFallenAltarGatewayColorsForBiome() {
      ArrayList<Color> gatewayColors = new ArrayList<>();
      gatewayColors.add(new Color(140, 50, 50));
      gatewayColors.add(new Color(255, 3, 3));
      gatewayColors.add(new Color(150, 13, 13));
      gatewayColors.add(new Color(255, 3, 3));
      gatewayColors.add(new Color(244, 152, 152));
      gatewayColors.add(new Color(253, 236, 236));
      return gatewayColors;
   }

   @Override
   public LootTable getExtraIncursionDrops(Mob mob) {
      LootTable mobDrops = super.getExtraIncursionDrops(mob);
      return mob.isBoss() ? new LootTable(mobDrops, new LootItem("bloodessence", 1)) : mobDrops;
   }
}
