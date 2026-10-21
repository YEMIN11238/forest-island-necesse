package necesse.level.maps.incursion;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.Server;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.entity.levelEvent.TrialIncursionEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.objectEntity.FallenAltarObjectEntity;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;
import necesse.inventory.lootTable.LootList;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.IncursionLevel;

public class BiomeTrialIncursionData extends BiomeMissionIncursionData {
   public BiomeTrialIncursionData() {
   }

   public BiomeTrialIncursionData(float difficulty, IncursionBiome biome, int tabletTier) {
      super(difficulty, biome, tabletTier);
   }

   @Override
   public GameMessage getDisplayName() {
      return new LocalMessage("biome", "incursiontrial", "incursion", this.biome.getLocalization());
   }

   @Override
   public GameMessage getIncursionMissionTypeName() {
      return new LocalMessage("biome", "trial");
   }

   @Override
   public Collection<FairType> getObjectives(IncursionData incursionData, FontOptions fontOptions) {
      return Collections.singleton(new FairType().append(fontOptions, Localization.translate("ui", "incursiontrialobjective")));
   }

   @Override
   protected Iterable<FairType> getLoot(IncursionData incursionData, FontOptions fontOptions) {
      LootList list = new LootList();
      LootTable bossDrop = this.biome.getBossDrop(incursionData);
      if (bossDrop != null) {
         bossDrop.addPossibleLoot(list);
      }

      return list.streamItemsAndCustomItems().map(item -> this.getItemMessage(item, fontOptions)).collect(Collectors.toList());
   }

   @Override
   public LootTable getExtraMobDrops(Mob mob) {
      if (!mob.isSummoned && mob.isBoss()) {
         LootTable bossDrop = this.biome.getBossDrop(this);
         return bossDrop != null ? new LootTable(bossDrop, this.getBossShardDrops()) : this.getBossShardDrops();
      } else {
         return super.getExtraMobDrops(mob);
      }
   }

   @Override
   public IncursionLevel getNewIncursionLevel(
      FallenAltarObjectEntity altar, LevelIdentifier identifier, Server server, WorldEntity worldEntity, AltarData altarData
   ) {
      IncursionLevel level = super.getNewIncursionLevel(altar, identifier, server, worldEntity, altarData);
      level.makeServerLevel(server);
      TrialIncursionEvent event = new TrialIncursionEvent(this.biome.bossMobStringID);
      level.entityManager.events.addHidden(event);
      level.incursionEventUniqueID = event.getUniqueID();
      return level;
   }
}
