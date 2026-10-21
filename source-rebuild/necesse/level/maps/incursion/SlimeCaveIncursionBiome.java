package necesse.level.maps.incursion;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;
import necesse.engine.network.server.Server;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.TicketSystemList;
import necesse.engine.world.WorldEntity;
import necesse.entity.mobs.Mob;
import necesse.entity.objectEntity.FallenAltarObjectEntity;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.IncursionLevel;

public class SlimeCaveIncursionBiome extends IncursionBiome {
   public SlimeCaveIncursionBiome() {
      super("motherslime");
      this.requiredPerkStringID = "slimecavetabletcandrop";
   }

   @Override
   public Collection<Item> getExtractionItems(IncursionData incursionData) {
      return Collections.singleton(ItemRegistry.getItem("slimeum"));
   }

   @Override
   public LootTable getHuntDrop(IncursionData incursionData) {
      return new LootTable(new ChanceLootItem(0.66F, "slimematter"));
   }

   @Override
   public LootTable getBossDrop(IncursionData incursionData) {
      return new LootTable(super.getBossDrop(incursionData), LootItem.between("slimeessence", 20, 25));
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
      return new SlimeCaveIncursionLevel(identifier, incursion, worldEntity, altarData);
   }

   @Override
   public ArrayList<Color> getFallenAltarGatewayColorsForBiome() {
      ArrayList<Color> gatewayColors = new ArrayList<>();
      gatewayColors.add(new Color(140, 139, 50));
      gatewayColors.add(new Color(247, 255, 3));
      gatewayColors.add(new Color(145, 150, 13));
      gatewayColors.add(new Color(247, 255, 3));
      gatewayColors.add(new Color(241, 244, 152));
      gatewayColors.add(new Color(253, 250, 236));
      return gatewayColors;
   }

   @Override
   public LootTable getExtraIncursionDrops(Mob mob) {
      LootTable mobDrops = super.getExtraIncursionDrops(mob);
      return mob.isBoss() ? new LootTable(mobDrops, new LootItem("slimeessence", 1)) : mobDrops;
   }
}
