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

public class SpiderCastleIncursionBiome extends IncursionBiome {
   public SpiderCastleIncursionBiome() {
      super("spiderempress");
      this.requiredPerkStringID = "spidercastletabletcandrop";
   }

   @Override
   public Collection<Item> getExtractionItems(IncursionData incursionData) {
      return Collections.singleton(ItemRegistry.getItem("spideriteore"));
   }

   @Override
   public LootTable getHuntDrop(IncursionData incursionData) {
      return new LootTable(new ChanceLootItem(0.66F, "spidervenom"));
   }

   @Override
   public LootTable getBossDrop(IncursionData incursionData) {
      return new LootTable(super.getBossDrop(incursionData), LootItem.between("spideressence", 20, 25));
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
      return new SpiderCastleIncursionLevel(identifier, incursion, worldEntity, altarData);
   }

   @Override
   public ArrayList<Color> getFallenAltarGatewayColorsForBiome() {
      ArrayList<Color> gatewayColors = new ArrayList<>();
      gatewayColors.add(new Color(53, 140, 50));
      gatewayColors.add(new Color(3, 255, 104));
      gatewayColors.add(new Color(13, 150, 61));
      gatewayColors.add(new Color(3, 255, 62));
      gatewayColors.add(new Color(152, 244, 178));
      gatewayColors.add(new Color(253, 243, 236));
      return gatewayColors;
   }

   @Override
   public LootTable getExtraIncursionDrops(Mob mob) {
      LootTable mobDrops = super.getExtraIncursionDrops(mob);
      return mob.isBoss() ? new LootTable(mobDrops, new LootItem("spideressence", 1)) : mobDrops;
   }
}
