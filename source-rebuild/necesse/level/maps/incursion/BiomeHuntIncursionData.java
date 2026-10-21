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
import necesse.entity.levelEvent.HuntIncursionEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.objectEntity.FallenAltarObjectEntity;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;
import necesse.inventory.lootTable.LootList;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.IncursionLevel;

public class BiomeHuntIncursionData extends BiomeMissionIncursionData {
   public BiomeHuntIncursionData() {
   }

   public BiomeHuntIncursionData(float difficulty, IncursionBiome biome, int tabletTier) {
      super(difficulty, biome, tabletTier);
   }

   @Override
   public GameMessage getDisplayName() {
      return new LocalMessage("biome", "incursionhunt", "incursion", this.biome.getLocalization());
   }

   @Override
   public GameMessage getIncursionMissionTypeName() {
      return new LocalMessage("biome", "hunt");
   }

   @Override
   public Collection<FairType> getObjectives(IncursionData incursionData, FontOptions fontOptions) {
      return Collections.singleton(new FairType().append(fontOptions, Localization.translate("ui", "incursionhuntobjective")));
   }

   @Override
   protected Iterable<FairType> getLoot(IncursionData incursionData, FontOptions fontOptions) {
      LootList list = new LootList();
      LootTable huntDrop = this.biome.getHuntDrop(incursionData);
      if (huntDrop != null) {
         huntDrop.addPossibleLoot(list);
      }

      LootTable bossDrop = this.biome.getBossDrop(incursionData);
      if (bossDrop != null) {
         bossDrop.addPossibleLoot(list);
      }

      return list.streamItemsAndCustomItems().map(item -> this.getItemMessage(item, fontOptions)).collect(Collectors.toList());
   }

   @Override
   public LootTable getExtraMobDrops(Mob mob) {
      if (!mob.isSummoned) {
         if (mob.isBoss()) {
            LootTable bossDrop = this.biome.getBossDrop(this);
            if (bossDrop != null) {
               return new LootTable(bossDrop, this.getBossShardDrops());
            }

            return this.getBossShardDrops();
         }

         if (mob.isHostile) {
            LootTable huntDrop = this.biome.getHuntDrop(this);
            if (huntDrop != null) {
               return huntDrop;
            }
         }
      }

      return super.getExtraMobDrops(mob);
   }

   @Override
   public IncursionLevel getNewIncursionLevel(
      FallenAltarObjectEntity altar, LevelIdentifier identifier, Server server, WorldEntity worldEntity, AltarData altarData
   ) {
      IncursionLevel level = super.getNewIncursionLevel(altar, identifier, server, worldEntity, altarData);
      level.makeServerLevel(server);
      HuntIncursionEvent event = new HuntIncursionEvent(this.biome.bossMobStringID, 0, 150);
      level.entityManager.events.addHidden(event);
      level.incursionEventUniqueID = event.getUniqueID();
      return level;
   }
}
