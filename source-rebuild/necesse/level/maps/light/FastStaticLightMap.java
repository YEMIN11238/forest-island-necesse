package necesse.level.maps.light;

public class FastStaticLightMap extends FastLightMap {
   public FastStaticLightMap(LightManager manager, int startX, int startY, int endX, int endY) {
      super(manager, startX, startY, endX, endY, 25);
   }

   @Override
   protected GameLight getNewLight(int x, int y) {
      GameLight light = this.level.objectLayer.getCombinedLight(x, y);
      light.combine(this.level.getTile(x, y).getLight(this.level));
      return light;
   }
}
