package necesse.level.gameObject;

import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.GlyphObjectTrapEvent;
import necesse.entity.levelEvent.ReverseDamageGlyphTrapEvent;
import necesse.level.maps.Level;

public class ReverseDamageGlyphTrapObject extends GlyphTrapObject {
   public ReverseDamageGlyphTrapObject() {
      super("glyphtrapreversedamage", ReverseDamageGlyphTrapEvent.particleHue);
   }

   @Override
   protected void addLevelEvent(Level level, int x, int y) {
      GlyphObjectTrapEvent event = new ReverseDamageGlyphTrapEvent(x, y, GameRandom.globalRandom);
      level.entityManager.events.add(event);
   }
}
