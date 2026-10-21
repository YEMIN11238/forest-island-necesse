package necesse.engine.registries;

import necesse.level.maps.mapData.GameMapIcon;
import necesse.level.maps.mapData.ItemGameMapIcon;
import necesse.level.maps.mapData.MobGameMapIcon;
import necesse.level.maps.mapData.TextureGameMapIcon;

public class MapIconRegistry extends GameRegistry<GameMapIcon> {
   public static final MapIconRegistry instance = new MapIconRegistry();
   public static GameMapIcon defaultIcon;

   private MapIconRegistry() {
      super("MapIcon", 32762);
   }

   @Override
   public void registerCore() {
      defaultIcon = registerIcon("poi", new TextureGameMapIcon("ui/mapicons/poi", -14));
      registerIcon("checkmark", new TextureGameMapIcon("ui/mapicons/checkmark"));
      registerIcon("cross", new TextureGameMapIcon("ui/mapicons/cross"));
      registerIcon("village", new TextureGameMapIcon("ui/mapicons/village"));
      registerIcon("voidwizard", new MobGameMapIcon("voidwizard"));
      registerIcon("piratebanner", new TextureGameMapIcon("ui/mapicons/piratebanner"));
      registerIcon("strikebanner", new ItemGameMapIcon("strikebanner"));
      registerIcon("bannerofpeace", new ItemGameMapIcon("bannerofpeace"));
      registerIcon("bannerofwar", new ItemGameMapIcon("bannerofwar"));
      registerIcon("chieftain", new MobGameMapIcon("chieftain"));
      registerIcon("thecursedcrone", new MobGameMapIcon("thecursedcrone"));
      registerIcon("sageandgrit", new MobGameMapIcon("sageandgrit"));
   }

   protected void onRegister(GameMapIcon object, int id, String stringID, boolean isReplace) {
   }

   @Override
   protected void onRegistryClose() {
   }

   public static <T extends GameMapIcon> T registerIcon(String stringID, T preset) {
      instance.register(stringID, preset);
      return preset;
   }

   public static GameMapIcon getIcon(int id) {
      return instance.getElement(id);
   }

   public static GameMapIcon getIcon(String stringID) {
      return instance.getElement(stringID);
   }

   public static Iterable<GameMapIcon> getAllIcons() {
      return instance.getElements();
   }
}
