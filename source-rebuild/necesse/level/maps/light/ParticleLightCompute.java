package necesse.level.maps.light;

import java.awt.Point;
import java.util.LinkedList;
import necesse.engine.util.PointHashSet;

public class ParticleLightCompute {
   protected final ParticleLightMap map;
   protected final LightArea area;
   protected LinkedList<ParticleLightCompute.LightPoint> sources = new LinkedList<>();
   protected LinkedList<Point> open = new LinkedList<>();
   protected PointHashSet closed = new PointHashSet();

   public ParticleLightCompute(ParticleLightMap map, int startX, int startY, int endX, int endY) {
      this.map = map;
      this.area = new LightArea(startX, startY, endX, endY);
   }

   public void addSource(int x, int y, GameLight light) {
      this.sources.add(new ParticleLightCompute.LightPoint(x, y, light));
   }

   public int compute() {
      this.area.initLights();

      for (ParticleLightCompute.LightPoint source : this.sources) {
         this.area.lights[this.area.getIndex(source.tileX, source.tileY)] = source.light;
         this.open.add(new Point(source.tileX, source.tileY));
      }

      this.sources = null;
      int iterations = 0;

      while (!this.open.isEmpty()) {
         iterations++;
         Point current = this.open.removeFirst();
         GameLight currentLight = this.area.lights[this.area.getIndex(current.x, current.y)];
         this.closed.add(current.x, current.y);
         int mod = this.map.getLightModifier(current.x, current.y);
         this.handleTile(current.x, current.y - 1, currentLight, mod);
         this.handleTile(current.x - 1, current.y, currentLight, mod);
         this.handleTile(current.x + 1, current.y, currentLight, mod);
         this.handleTile(current.x, current.y + 1, currentLight, mod);
      }

      return iterations;
   }

   public int apply(LightArea area) {
      this.closed.forEach((tileX, tileY) -> area.lights[area.getIndex(tileX, tileY)].combine(this.area.lights[this.area.getIndex(tileX, tileY)]));
      return this.closed.size();
   }

   private void handleTile(int tileX, int tileY, GameLight lastLight, int mod) {
      if (!this.area.isOutsideArea(tileX, tileY)) {
         int index = this.area.getIndex(tileX, tileY);
         float level = Math.max(lastLight.getLevel() - mod, 0.0F);
         if (level > 0.0F && (this.area.lights[index] == null || this.area.lights[index].getLevel() < level)) {
            GameLight nextLight = lastLight.copy();
            nextLight.setLevel(level);
            this.area.lights[index] = nextLight;
            if (!this.open.contains(new Point(tileX, tileY))) {
               this.open.add(new Point(tileX, tileY));
            }
         }
      }
   }

   private static class LightPoint {
      public final int tileX;
      public final int tileY;
      public final GameLight light;

      public LightPoint(int tileX, int tileY, GameLight light) {
         this.tileX = tileX;
         this.tileY = tileY;
         this.light = light;
      }
   }
}
