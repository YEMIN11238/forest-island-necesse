package necesse.level.gameObject;

import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.ChickenGlyphTrapEvent;
import necesse.entity.levelEvent.GlyphObjectTrapEvent;
import necesse.level.maps.Level;

public class ChickenGlyphTrapObject extends GlyphTrapObject {
   public ChickenGlyphTrapObject() {
      super("glyphtrapchicken", ChickenGlyphTrapEvent.particleHue);
   }

   @Override
   protected void addLevelEvent(Level level, int x, int y) {
      GlyphObjectTrapEvent event = new ChickenGlyphTrapEvent(x, y, GameRandom.globalRandom);
      level.entityManager.events.add(event);
   }
}
