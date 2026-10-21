package necesse.level.maps.hudManager.floatText.thoughtBubble;

import necesse.engine.Settings;
import necesse.entity.mobs.Mob;
import necesse.gfx.gameTexture.GameTexture;

public class ExcitedThoughtBubble extends TextureThoughtBubble {
   public ExcitedThoughtBubble(Mob mob, int stayTime) {
      super(mob, stayTime);
   }

   @Override
   public GameTexture getThoughtContentTexture() {
      return Settings.UI.settler_thought_excited;
   }
}
