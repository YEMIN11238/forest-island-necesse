package necesse.level.maps.presets.set;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.gfx.forms.presets.PresetDebugPreviewForm;

public class ColumnSet extends PresetSet<ColumnSet> {
   public static final ColumnSet wood = new ColumnSet("woodcolumn").all(BiomeRegistry.FOREST, BiomeRegistry.SWAMP, BiomeRegistry.PLAINS);
   public static final ColumnSet granite = new ColumnSet("granitecolumn").all(BiomeRegistry.PLAINS, BiomeRegistry.DESERT);
   public static final ColumnSet basalt = new ColumnSet("basaltcolumn").all(BiomeRegistry.PLAINS);
   public static final ColumnSet crypt = new ColumnSet("cryptcolumn").incursion(BiomeRegistry.GRAVEYARD);
   public static final ColumnSet obsidian = new ColumnSet("obsidiancolumn").deepCave(BiomeRegistry.FOREST);
   public static final ColumnSet spidercastle = new ColumnSet("spidercastlecolumn").incursion(BiomeRegistry.SPIDER_CASTLE);
   public static final ColumnSet stone = new ColumnSet("stonecolumn").surface(BiomeRegistry.FOREST).cave(BiomeRegistry.FOREST).incursion();
   public static final ColumnSet deepstone = new ColumnSet("deepstonecolumn").deepCave(BiomeRegistry.FOREST, BiomeRegistry.PLAINS);
   public static final ColumnSet snowstone = new ColumnSet("snowstonecolumn").surface(BiomeRegistry.SNOW).cave(BiomeRegistry.SNOW).incursion();
   public static final ColumnSet deepsnowstone = new ColumnSet("deepsnowstonecolumn").deepCave(BiomeRegistry.SNOW);
   public static final ColumnSet sandstone = new ColumnSet("sandstonecolumn").surface(BiomeRegistry.DESERT).cave(BiomeRegistry.DESERT).incursion();
   public static final ColumnSet deepsandstone = new ColumnSet("deepsandstonecolumn").deepCave(BiomeRegistry.DESERT);
   public static final ColumnSet swampstone = new ColumnSet("swampstonecolumn").surface(BiomeRegistry.SWAMP).cave(BiomeRegistry.SWAMP).incursion();
   public static final ColumnSet deepswampstone = new ColumnSet("deepswampstonecolumn").deepCave(BiomeRegistry.SWAMP);
   public final int column;

   protected ColumnSet(String column) {
      this.objectArrays = new int[][]{{this.column = ObjectRegistry.getObjectID(column)}};
   }

   static {
      PresetDebugPreviewForm.registerPresetSet(ColumnSet.class);
   }
}
