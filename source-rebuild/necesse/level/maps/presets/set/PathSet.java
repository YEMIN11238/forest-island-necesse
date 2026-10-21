package necesse.level.maps.presets.set;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.gfx.forms.presets.PresetDebugPreviewForm;

public class PathSet extends PresetSet<PathSet> {
   public static final PathSet wood = new PathSet(TileRegistry.woodPathID)
      .surface(BiomeRegistry.FOREST, BiomeRegistry.PLAINS, BiomeRegistry.SNOW, BiomeRegistry.SWAMP, BiomeRegistry.DESERT)
      .cave(BiomeRegistry.FOREST, BiomeRegistry.PLAINS, BiomeRegistry.SNOW, BiomeRegistry.SWAMP, BiomeRegistry.DESERT);
   public static final PathSet stone = new PathSet(TileRegistry.stonePathID)
      .surface(BiomeRegistry.FOREST, BiomeRegistry.PLAINS, BiomeRegistry.SNOW, BiomeRegistry.SWAMP, BiomeRegistry.DESERT)
      .cave(BiomeRegistry.FOREST, BiomeRegistry.PLAINS, BiomeRegistry.SNOW, BiomeRegistry.SWAMP, BiomeRegistry.DESERT);
   public static final PathSet snowStone = new PathSet(TileRegistry.snowStonePathID)
      .surface(BiomeRegistry.SNOW)
      .cave(BiomeRegistry.SNOW)
      .deepCave(BiomeRegistry.SNOW);
   public static final PathSet granite = new PathSet(TileRegistry.granitePathID).surface(BiomeRegistry.PLAINS).cave(BiomeRegistry.PLAINS);
   public static final PathSet sandStone = new PathSet(TileRegistry.sandstonePathID)
      .surface(BiomeRegistry.DESERT)
      .cave(BiomeRegistry.DESERT)
      .deepCave(BiomeRegistry.DESERT);
   public static final PathSet swampStone = new PathSet(TileRegistry.swampStonePathID)
      .surface(BiomeRegistry.SWAMP)
      .cave(BiomeRegistry.SWAMP)
      .deepCave(BiomeRegistry.SWAMP);
   public static final PathSet dryad = new PathSet(TileRegistry.dryadPathID).cave(BiomeRegistry.PLAINS).deepCave(BiomeRegistry.PLAINS).incursion();
   public static final PathSet basalt = new PathSet(TileRegistry.basaltPathID).deepCave(BiomeRegistry.PLAINS).incursion();
   public static final PathSet crypt = new PathSet(TileRegistry.getTileID("cryptpath")).incursion();
   public static final PathSet lava = new PathSet(TileRegistry.getTileID("lavapath"));
   public static final PathSet dawn = new PathSet(TileRegistry.getTileID("dawnpath"));
   public static final PathSet moon = new PathSet(TileRegistry.getTileID("moonpath"));
   public static final PathSet darkMoon = new PathSet(TileRegistry.getTileID("darkmoonpath"));
   public static final PathSet darkFullMoon = new PathSet(TileRegistry.getTileID("darkfullmoonpath"));
   public final int tile;

   protected PathSet(int tile) {
      this.tileArrays = new int[][]{{this.tile = tile}};
   }

   static {
      PresetDebugPreviewForm.registerPresetSet(PathSet.class);
   }
}
