package necesse.engine.incursionPerkTree;

import java.util.Objects;
import necesse.engine.registries.IncursionPerksRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.IncursionPerkModifiers.MobsDropAltarDustPerkLevelEvent;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.generationModules.PresetGeneration;
import necesse.level.maps.incursion.AltarData;
import necesse.level.maps.incursion.IncursionData;
import necesse.level.maps.presets.incursionPresets.JonasApartmentPreset;

public class MobsDropAltarDustPerk extends IncursionPerk {
   public MobsDropAltarDustPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, true, prerequisitePerkRequired);
   }

   @Override
   public void onIncursionLevelGenerated(IncursionLevel level, AltarData altarData, int modifierIndex) {
      if (!this.isPerkOverwritten(altarData)) {
         MobsDropAltarDustPerkLevelEvent event = new MobsDropAltarDustPerkLevelEvent();
         level.entityManager.events.add(event);
         level.gndData.setInt(this.getStringID() + modifierIndex, event.getUniqueID());
      }
   }

   @Override
   public void onIncursionLevelCompleted(IncursionLevel level, AltarData altarData, int modifierIndex) {
      int eventUniqueID = level.gndData.getInt(this.getStringID() + modifierIndex);
      LevelEvent event = level.entityManager.events.get(eventUniqueID, false);
      if (event != null) {
         event.over();
      }
   }

   @Override
   public boolean isPerkOverwritten(AltarData altarData) {
      return altarData.hasPerk(IncursionPerksRegistry.MOBS_DROP_MORE_ALTAR_DUST);
   }

   @Override
   public void onIncursionStructuresGenerated(AltarData altarData, IncursionData incursionData, PresetGeneration presets, GameRandom random, Biome biome) {
      if (incursionData.getTabletTier() >= 7) {
         if (Objects.equals(incursionData.getStringID(), "trial")) {
            return;
         }

         float chance = 0.1F;
         if (random.getChance(chance)) {
            presets.findRandomValidPositionAndApply(random, 50, new JonasApartmentPreset(random), 5, true, false);
         }
      }
   }
}
