package necesse.level.gameObject;

import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.BounceGlyphTrapEvent;
import necesse.entity.levelEvent.GlyphObjectTrapEvent;
import necesse.level.maps.Level;

public class BounceGlyphTrapObject extends GlyphTrapObject {
   public BounceGlyphTrapObject() {
      super("glyphtrapbounce", BounceGlyphTrapEvent.particleHue);
   }

   @Override
   protected void addLevelEvent(Level level, int x, int y) {
      GlyphObjectTrapEvent event = new BounceGlyphTrapEvent(x, y, GameRandom.globalRandom);
      level.entityManager.events.add(event);
   }
}
