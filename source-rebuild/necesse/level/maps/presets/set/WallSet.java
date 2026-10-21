package necesse.level.maps.presets.set;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.gfx.forms.presets.PresetDebugPreviewForm;

public class WallSet extends PresetSet<WallSet> {
   public static final WallSet wood = new WallSet("wood")
      .surface(BiomeRegistry.FOREST, BiomeRegistry.PLAINS, BiomeRegistry.SNOW, BiomeRegistry.SWAMP, BiomeRegistry.DESERT)
      .cave(BiomeRegistry.FOREST, BiomeRegistry.PLAINS, BiomeRegistry.SWAMP, BiomeRegistry.DESERT)
      .deepCave(BiomeRegistry.FOREST, BiomeRegistry.PLAINS, BiomeRegistry.SNOW, BiomeRegistry.SWAMP, BiomeRegistry.DESERT);
   public static final WallSet pine = new WallSet("pine")
      .surface(BiomeRegistry.FOREST, BiomeRegistry.SNOW, BiomeRegistry.PLAINS)
      .cave(BiomeRegistry.FOREST, BiomeRegistry.SNOW, BiomeRegistry.PLAINS)
      .deepCave(BiomeRegistry.FOREST, BiomeRegistry.SNOW, BiomeRegistry.PLAINS)
      .incursion(BiomeRegistry.SNOW_DEEP_CAVE_INCURSION);
   public static final WallSet palm = new WallSet("palm")
      .surface(BiomeRegistry.DESERT)
      .cave(BiomeRegistry.DESERT)
      .deepCave(BiomeRegistry.DESERT)
      .incursion(BiomeRegistry.DESERT_DEEP_CAVE_INCURSION);
   public static final WallSet willow = new WallSet("willow")
      .surface(BiomeRegistry.SWAMP)
      .cave(BiomeRegistry.SWAMP)
      .deepCave(BiomeRegistry.SWAMP, BiomeRegistry.FOREST)
      .incursion(BiomeRegistry.FOREST_DEEP_CAVE_INCURSION, BiomeRegistry.PLAINS, BiomeRegistry.SLIME_CAVE, BiomeRegistry.SWAMP_DEEP_CAVE_INCURSION);
   public static final WallSet dryad = new WallSet("dryad")
      .cave(BiomeRegistry.PLAINS)
      .deepCave(BiomeRegistry.PLAINS)
      .incursion(BiomeRegistry.SNOW_DEEP_CAVE_INCURSION);
   public static final WallSet bamboo = new WallSet("bamboo")
      .cave(BiomeRegistry.SWAMP)
      .deepCave(BiomeRegistry.SWAMP)
      .incursion(BiomeRegistry.SWAMP_DEEP_CAVE_INCURSION, BiomeRegistry.DESERT_DEEP_CAVE_INCURSION, BiomeRegistry.SLIME_CAVE);
   public static final WallSet stone = new WallSet("stone").surface(BiomeRegistry.FOREST).cave(BiomeRegistry.FOREST);
   public static final WallSet sandstone = new WallSet("sandstone")
      .surface(BiomeRegistry.DESERT)
      .cave(BiomeRegistry.DESERT)
      .incursion(BiomeRegistry.DESERT_DEEP_CAVE_INCURSION);
   public static final WallSet swampStone = new WallSet("swampstone")
      .surface(BiomeRegistry.SWAMP)
      .cave(BiomeRegistry.SWAMP)
      .incursion(BiomeRegistry.SWAMP_DEEP_CAVE_INCURSION, BiomeRegistry.SLIME_CAVE);
   public static final WallSet snowStone = new WallSet("snowstone")
      .surface(BiomeRegistry.SNOW)
      .cave(BiomeRegistry.SNOW)
      .incursion(BiomeRegistry.SNOW_DEEP_CAVE_INCURSION);
   public static final WallSet granite = new WallSet("granite")
      .surface(BiomeRegistry.PLAINS)
      .cave(BiomeRegistry.PLAINS)
      .incursion(BiomeRegistry.FOREST_DEEP_CAVE_INCURSION);
   public static final WallSet ice = new WallSet("ice").surface(BiomeRegistry.SNOW).cave(BiomeRegistry.SNOW).deepCave(BiomeRegistry.SNOW);
   public static final WallSet brick = new WallSet("brick")
      .surface(BiomeRegistry.FOREST, BiomeRegistry.PLAINS, BiomeRegistry.SNOW, BiomeRegistry.SWAMP, BiomeRegistry.DESERT)
      .cave(BiomeRegistry.FOREST, BiomeRegistry.PLAINS, BiomeRegistry.SNOW, BiomeRegistry.SWAMP, BiomeRegistry.DESERT)
      .deepCave(BiomeRegistry.FOREST, BiomeRegistry.PLAINS, BiomeRegistry.SNOW, BiomeRegistry.SWAMP, BiomeRegistry.DESERT);
   public static final WallSet dungeon = new WallSet("dungeon")
      .surface(BiomeRegistry.SNOW, BiomeRegistry.SWAMP)
      .cave(BiomeRegistry.SNOW, BiomeRegistry.SWAMP)
      .deepCave(BiomeRegistry.SNOW, BiomeRegistry.SWAMP)
      .incursion(BiomeRegistry.FOREST_DEEP_CAVE_INCURSION, BiomeRegistry.SNOW_DEEP_CAVE_INCURSION);
   public static final WallSet deepStone = new WallSet("deepstone")
      .deepCave(BiomeRegistry.FOREST, BiomeRegistry.PLAINS, BiomeRegistry.SNOW, BiomeRegistry.SWAMP, BiomeRegistry.DESERT)
      .incursion(BiomeRegistry.DESERT_DEEP_CAVE_INCURSION, BiomeRegistry.FOREST_DEEP_CAVE_INCURSION, BiomeRegistry.SWAMP_DEEP_CAVE_INCURSION);
   public static final WallSet obsidian = new WallSet("obsidian")
      .deepCave(BiomeRegistry.FOREST, BiomeRegistry.SWAMP, BiomeRegistry.PLAINS)
      .incursion(BiomeRegistry.FOREST_DEEP_CAVE_INCURSION, BiomeRegistry.SNOW_DEEP_CAVE_INCURSION);
   public static final WallSet deepSnowStone = new WallSet("deepsnowstone").deepCave(BiomeRegistry.SNOW).incursion(BiomeRegistry.SNOW_DEEP_CAVE_INCURSION);
   public static final WallSet deepSwampStone = new WallSet("deepswampstone")
      .deepCave(BiomeRegistry.SWAMP)
      .incursion(BiomeRegistry.SWAMP_DEEP_CAVE_INCURSION, BiomeRegistry.SLIME_CAVE);
   public static final WallSet deepSandstone = new WallSet("deepsandstone")
      .cave(BiomeRegistry.DESERT)
      .deepCave(BiomeRegistry.DESERT)
      .incursion(BiomeRegistry.DESERT_DEEP_CAVE_INCURSION);
   public static final WallSet basalt = new WallSet("basalt")
      .deepCave(BiomeRegistry.PLAINS)
      .incursion(BiomeRegistry.FOREST_DEEP_CAVE_INCURSION, BiomeRegistry.SNOW_DEEP_CAVE_INCURSION);
   public static final WallSet crypt = new WallSet("crypt").incursion(BiomeRegistry.GRAVEYARD);
   public static final WallSet spidercastle = new WallSet("spidercastle").incursion(BiomeRegistry.SPIDER_CASTLE);
   public static final WallSet dawn = new WallSet("dawn");
   public static final WallSet dusk = new WallSet("dusk");
   public static final WallSet ancientruin = new WallSet("ancientruin").incursion(BiomeRegistry.CRYSTAL_HOLLOW);
   public final String prefixStringID;
   public final int wall;
   public final int doorClosed;
   public final int doorOpen;
   public final int flameTrap;
   public final int arrowTrap;
   public final int sawTrap;
   public final int window;

   public WallSet(String prefixStringID) {
      this.prefixStringID = prefixStringID;
      this.objectArrays = new int[][]{
         {this.wall = ObjectRegistry.getObjectID(prefixStringID + "wall")},
         {this.doorClosed = ObjectRegistry.getObjectID(prefixStringID + "door")},
         {this.arrowTrap = ObjectRegistry.getObjectID(prefixStringID + "arrowtrap")},
         {this.flameTrap = ObjectRegistry.getObjectID(prefixStringID + "flametrap")},
         {this.sawTrap = ObjectRegistry.getObjectID(prefixStringID + "sawtrap")},
         {this.doorOpen = ObjectRegistry.getObjectID(prefixStringID + "dooropen")},
         {this.window = ObjectRegistry.getObjectID(prefixStringID + "window")}
      };
   }

   static {
      PresetDebugPreviewForm.registerPresetSet(WallSet.class);
   }
}
