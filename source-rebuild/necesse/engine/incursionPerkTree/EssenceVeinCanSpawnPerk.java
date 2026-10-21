package necesse.engine.incursionPerkTree;

import necesse.engine.localization.Localization;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.generationModules.CaveGeneration;
import necesse.level.maps.generationModules.GenerationTools;

public class EssenceVeinCanSpawnPerk extends IncursionPerk {
   public EssenceVeinCanSpawnPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, true, prerequisitePerkRequired);
   }

   @Override
   public void onGenerateOres(CaveGeneration cg, IncursionLevel incursionLevel, Biome biome, GameRandom random) {
      super.onGenerateOres(cg, incursionLevel, biome, random);
      if (biome == BiomeRegistry.GRAVEYARD) {
         int cryptAshID = TileRegistry.getTileID("cryptash");
         GameObject essenceRockObject = ObjectRegistry.getObject(incursionLevel.essenceRockID);
         GenerationTools.generateGuaranteedRandomVeins(
            incursionLevel,
            random,
            22,
            1,
            1,
            (level, tileX, tileY) -> level.getTileID(tileX, tileY) == cryptAshID && essenceRockObject.canPlace(level, tileX, tileY, 0, false) == null,
            (level, tileX, tileY) -> essenceRockObject.placeObject(level, tileX, tileY, 0, false)
         );
      } else if (cg != null) {
         int essenceRockID = -1;
         if (incursionLevel.essenceRockID != null) {
            essenceRockID = ObjectRegistry.getObjectID(incursionLevel.essenceRockID);
         }

         if (essenceRockID != -1) {
            cg.generateGuaranteedOreVeins(35, 12, 24, essenceRockID);
         }
      }
   }

   @Override
   public String getCustomTooltipLocalization() {
      return Localization.translate("incursion", this.getStringID() + "newdesc");
   }
}
