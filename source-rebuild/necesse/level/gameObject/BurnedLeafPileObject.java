package necesse.level.gameObject;

import java.awt.Color;
import necesse.engine.util.GameRandom;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameTextureSection;

public class BurnedLeafPileObject extends LeafPileObject {
   public BurnedLeafPileObject(String textureName, Color mapColor) {
      super(textureName, mapColor);
   }

   @Override
   protected GameTextureSection getLeafParticleSprite() {
      return GameResources.burnedLeafParticles.sprite(GameRandom.globalRandom.nextInt(4), 0, 20);
   }
}
