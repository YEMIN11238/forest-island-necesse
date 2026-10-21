package necesse.level.maps.presets.set;

import java.util.ArrayList;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.gfx.forms.presets.PresetDebugPreviewForm;
import necesse.level.maps.biomes.Biome;

public class RockAndOreSet extends PresetSet<RockAndOreSet> {
   public static final RockAndOreSet forest = new RockAndOreSet(
         "rock", "ironorerock", "copperorerock", "goldorerock", "surfacerock", "surfacerocksmall", "rocktile"
      )
      .surface(BiomeRegistry.FOREST);
   public static final RockAndOreSet forestcave = new RockAndOreSet(
         "rock", "ironorerock", "copperorerock", "goldorerock", "caverock", "caverocksmall", "rocktile"
      )
      .cave(BiomeRegistry.FOREST);
   public static final RockAndOreSet deepforestcave = new RockAndOreSet(
         "deeprock", "ironoredeeprock", "copperoredeeprock", "goldoredeeprock", "deepcaverock", "deepcaverocksmall", "deeprocktile"
      )
      .deepCave(BiomeRegistry.FOREST);
   public static final RockAndOreSet snow = new RockAndOreSet(
         "snowrock", "ironoresnow", "copperoresnow", "goldoresnow", "snowsurfacerock", "snowsurfacerocksmall", "snowrocktile"
      )
      .surface(BiomeRegistry.SNOW);
   public static final RockAndOreSet snowcave = new RockAndOreSet(
         "snowrock", "ironoresnow", "copperoresnow", "goldoresnow", "snowcaverock", "snowcaverocksmall", "snowrocktile"
      )
      .cave(BiomeRegistry.SNOW);
   public static final RockAndOreSet deepsnowcave = new RockAndOreSet(
         "deepsnowrock", "ironoredeepsnowrock", "copperoredeepsnowrock", "goldoredeepsnowrock", "deepsnowcaverock", "deepsnowcaverocksmall", "deepsnowrocktile"
      )
      .deepCave(BiomeRegistry.SNOW);
   public static final RockAndOreSet plains = new RockAndOreSet(
         "graniterock", "ironoregraniterock", "copperoregraniterock", "goldoregraniterock", "surfacerock", "surfacerocksmall", "graniterocktile"
      )
      .surface(BiomeRegistry.PLAINS);
   public static final RockAndOreSet plainscave = new RockAndOreSet(
         "graniterock", "ironoregraniterock", "copperoregraniterock", "goldoregraniterock", "granitecaverock", "granitecaverocksmall", "graniterocktile"
      )
      .cave(BiomeRegistry.PLAINS);
   public static final RockAndOreSet deepplainscave = new RockAndOreSet(
         "basaltrock", "ironorebasaltrock", "copperorebasaltrock", "goldorebasaltrock", "basaltcaverock", "basaltcaverocksmall", "basaltrocktile"
      )
      .deepCave(BiomeRegistry.PLAINS);
   public static final RockAndOreSet swamp = new RockAndOreSet(
         "swamprock", "ironoreswamp", "copperoreswamp", "goldoreswamp", "swampsurfacerock", "swampsurfacerocksmall", "swamprocktile"
      )
      .surface(BiomeRegistry.SWAMP);
   public static final RockAndOreSet swampcave = new RockAndOreSet(
         "swamprock", "ironoreswamp", "copperoreswamp", "goldoreswamp", "swampcaverock", "swampcaverocksmall", "swamprocktile"
      )
      .cave(BiomeRegistry.SWAMP);
   public static final RockAndOreSet deepswampcave = new RockAndOreSet(
         "deepswamprock",
         "ironoredeepswamprock",
         "copperoredeepswamprock",
         "goldoredeepswamprock",
         "deepswampcaverock",
         "deepswampcaverocksmall",
         "deepswamprocktile"
      )
      .deepCave(BiomeRegistry.SWAMP);
   public static final RockAndOreSet desert = new RockAndOreSet(
         "sandstonerock", "ironoresandstone", "copperoresandstone", "goldoresandstone", "sandsurfacerock", "sandsurfacerocksmall", "sandstonetile"
      )
      .surface(BiomeRegistry.DESERT);
   public static final RockAndOreSet desertcave = new RockAndOreSet(
         "sandstonerock", "ironoresandstone", "copperoresandstone", "goldoresandstone", "sandcaverock", "sandcaverocksmall", "sandstonetile"
      )
      .cave(BiomeRegistry.DESERT);
   public static final RockAndOreSet deepdesertcave = new RockAndOreSet(
         "deepsandstonerock",
         "ironoredeepsandstone",
         "copperoredeepsandstone",
         "goldoredeepsandstone",
         "deepsandcaverock",
         "deepsandcaverocksmall",
         "deepsandstonetile"
      )
      .deepCave(BiomeRegistry.DESERT);
   public final int rock;
   public final int ironOre;
   public final int copperOre;
   public final int goldOre;
   public final int surfaceRock;
   public final int surfaceRockR;
   public final int surfaceRockSmall;
   public final int floorTile;

   public RockAndOreSet(String rock, String ironOre, String copperOre, String goldOre, String surfaceRock, String surfaceRockSmall, String floorTile) {
      this.objectArrays = new int[][]{
         {this.rock = ObjectRegistry.getObjectID(rock)},
         {this.ironOre = ObjectRegistry.getObjectID(ironOre)},
         {this.copperOre = ObjectRegistry.getObjectID(copperOre)},
         {this.goldOre = ObjectRegistry.getObjectID(goldOre)},
         {this.surfaceRock = ObjectRegistry.getObjectID(surfaceRock)},
         {this.surfaceRockR = this.surfaceRock + 1},
         {this.surfaceRockSmall = ObjectRegistry.getObjectID(surfaceRockSmall)}
      };
      this.tileArrays = new int[][]{{this.floorTile = TileRegistry.getTileID(floorTile)}};
   }

   public static String getRandomSignatureOreStringID(Biome biome, LevelIdentifier levelIdentifier, GameRandom seededRandom) {
      if (levelIdentifier.equals(LevelIdentifier.SURFACE_IDENTIFIER)) {
         return null;
      }

      ArrayList<String> possibleOres = new ArrayList<>();
      possibleOres.add("ironore");
      possibleOres.add("goldore");
      possibleOres.add("copperore");
      if (levelIdentifier.equals(LevelIdentifier.CAVE_IDENTIFIER)) {
         if (biome.equals(BiomeRegistry.SWAMP)) {
            possibleOres.add("ivyore");
         } else if (biome.equals(BiomeRegistry.DESERT)) {
            possibleOres.add("quartz");
         }
      } else if (levelIdentifier.equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER)) {
         possibleOres.add("tungstenore");
         possibleOres.add("lifequartz");
         if (biome.equals(BiomeRegistry.SWAMP)) {
            possibleOres.add("myceliumore");
         } else if (biome.equals(BiomeRegistry.SNOW)) {
            possibleOres.add("glacialore");
         } else if (biome.equals(BiomeRegistry.DESERT)) {
            possibleOres.add("ancientfossilore");
         }
      }

      return seededRandom.getOneOf(possibleOres) + getOreSuffix(biome, levelIdentifier);
   }

   public static String getOreSuffix(Biome biome, LevelIdentifier levelIdentifier) {
      if (levelIdentifier.equals(LevelIdentifier.SURFACE_IDENTIFIER)) {
         return null;
      }

      if (levelIdentifier.equals(LevelIdentifier.CAVE_IDENTIFIER)) {
         if (biome.equals(BiomeRegistry.SWAMP)) {
            return "swamp";
         }

         if (biome.equals(BiomeRegistry.SNOW)) {
            return "snow";
         }

         if (biome.equals(BiomeRegistry.DESERT)) {
            return "sandstone";
         }

         if (biome.equals(BiomeRegistry.PLAINS)) {
            return "graniterock";
         }

         if (biome.equals(BiomeRegistry.FOREST)) {
            return "rock";
         }
      }

      if (biome.equals(BiomeRegistry.SWAMP)) {
         return "deepswamprock";
      } else if (biome.equals(BiomeRegistry.SNOW)) {
         return "deepsnowrock";
      } else if (biome.equals(BiomeRegistry.DESERT)) {
         return "deepsandstone";
      } else if (biome.equals(BiomeRegistry.PLAINS)) {
         return "basaltrock";
      } else {
         return biome.equals(BiomeRegistry.FOREST) ? "deeprock" : null;
      }
   }

   static {
      PresetDebugPreviewForm.registerPresetSet(RockAndOreSet.class);
   }
}
