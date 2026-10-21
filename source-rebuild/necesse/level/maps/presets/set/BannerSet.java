package necesse.level.maps.presets.set;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.gfx.forms.presets.PresetDebugPreviewForm;

public class BannerSet extends PresetSet<BannerSet> {
   public static final BannerSet frost = new BannerSet("frostbanner").all(BiomeRegistry.SNOW);
   public static final BannerSet dryad = new BannerSet("dryadbanner").all(BiomeRegistry.FOREST, BiomeRegistry.SWAMP);
   public static final BannerSet eggcellent = new BannerSet("eggcellentbanner").all(BiomeRegistry.PLAINS, BiomeRegistry.DESERT);
   public static final BannerSet fishian = new BannerSet("fishianbanner").all(BiomeRegistry.SWAMP);
   public static final BannerSet pirate = new BannerSet("piratebanner").all(BiomeRegistry.SWAMP, BiomeRegistry.FOREST, BiomeRegistry.PLAINS);
   public static final BannerSet vampiric = new BannerSet("vampiricbanner").all(BiomeRegistry.FOREST, BiomeRegistry.SNOW, BiomeRegistry.PLAINS);
   public final int small;
   public final int large;

   public BannerSet(String bannerID) {
      this.objectArrays = new int[][]{{this.small = ObjectRegistry.getObjectID(bannerID)}, {this.large = ObjectRegistry.getObjectID("large" + bannerID)}};
   }

   static {
      PresetDebugPreviewForm.registerPresetSet(BannerSet.class);
   }
}
