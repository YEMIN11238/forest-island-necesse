package necesse.level.maps.presets.set;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.gfx.forms.presets.PresetDebugPreviewForm;

public class GroundSet extends PresetSet<GroundSet> {
   public static final GroundSet forest = new GroundSet("grass", "air", "grasstile", "overgrowngrasstile", "graveltile")
      .surface(BiomeRegistry.FOREST)
      .cave(BiomeRegistry.FOREST)
      .deepCave(BiomeRegistry.FOREST)
      .incursion();
   public static final GroundSet swamp = new GroundSet("swampgrass", "deepswamptallgrass", "swampgrasstile", "overgrownswampgrasstile", "graveltile")
      .surface(BiomeRegistry.SWAMP)
      .cave(BiomeRegistry.SWAMP)
      .deepCave(BiomeRegistry.SWAMP)
      .incursion();
   public static final GroundSet plains = new GroundSet("plainsgrass", "air", "plainsgrasstile", "overgrownplainsgrasstile", "graveltile")
      .surface(BiomeRegistry.PLAINS)
      .cave(BiomeRegistry.PLAINS)
      .deepCave(BiomeRegistry.PLAINS)
      .incursion();
   public static final GroundSet snow = new GroundSet("air", "air", "snowtile", "snowtile", "graveltile")
      .surface(BiomeRegistry.SNOW)
      .cave(BiomeRegistry.SNOW)
      .deepCave(BiomeRegistry.SNOW)
      .incursion();
   public static final GroundSet desert = new GroundSet("air", "air", "sandtile", "sandtile", "sandgraveltile")
      .surface(BiomeRegistry.DESERT)
      .cave(BiomeRegistry.DESERT)
      .deepCave(BiomeRegistry.DESERT)
      .incursion();
   public final int grassObject;
   public final int tallGrassObject;
   public final int grassTile;
   public final int overgrownGrassTile;
   public final int gravelTile;

   public GroundSet(String grassObject, String tallGrassObject, String grassTile, String overgrownGrassTile, String gravelTile) {
      this.objectArrays = new int[][]{
         {this.grassObject = ObjectRegistry.getObjectID(grassObject)}, {this.tallGrassObject = ObjectRegistry.getObjectID(tallGrassObject)}
      };
      this.tileArrays = new int[][]{
         {this.grassTile = TileRegistry.getTileID(grassTile)},
         {this.overgrownGrassTile = TileRegistry.getTileID(overgrownGrassTile)},
         {this.gravelTile = TileRegistry.getTileID(gravelTile)}
      };
   }

   static {
      PresetDebugPreviewForm.registerPresetSet(GroundSet.class);
   }
}
