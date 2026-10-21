package necesse.level.maps.presets.set;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.gfx.forms.presets.PresetDebugPreviewForm;

public class CrystalSet extends PresetSet<CrystalSet> {
   public static final CrystalSet sapphire = new CrystalSet("sapphire").cave(BiomeRegistry.FOREST);
   public static final CrystalSet amethyst = new CrystalSet("amethyst").cave(BiomeRegistry.DESERT);
   public static final CrystalSet ruby = new CrystalSet("ruby").deepCave(BiomeRegistry.FOREST);
   public static final CrystalSet topaz = new CrystalSet("topaz").deepCave(BiomeRegistry.PLAINS);
   public static final CrystalSet emerald = new CrystalSet("emerald").deepCave(BiomeRegistry.SWAMP);
   public final int cluster;
   public final int clusterR;
   public final int pureCluster;
   public final int pureClusterR;
   public final int clusterSmall;
   public final int crystalGravelTile;

   public CrystalSet(String prefixStringID) {
      this.objectArrays = new int[][]{
         {this.cluster = ObjectRegistry.getObjectID(prefixStringID + "cluster")},
         {this.clusterR = ObjectRegistry.getObjectID(prefixStringID + "clusterr")},
         {this.pureCluster = ObjectRegistry.getObjectID(prefixStringID + "clusterpure")},
         {this.pureClusterR = ObjectRegistry.getObjectID(prefixStringID + "clusterpurer")},
         {this.clusterSmall = ObjectRegistry.getObjectID(prefixStringID + "clustersmall")}
      };
      this.tileArrays = new int[][]{{this.crystalGravelTile = TileRegistry.getTileID(prefixStringID + "gravel")}};
   }

   static {
      PresetDebugPreviewForm.registerPresetSet(CrystalSet.class);
   }
}
