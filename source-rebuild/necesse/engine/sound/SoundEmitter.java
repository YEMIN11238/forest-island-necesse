package necesse.engine.sound;

import java.awt.geom.Point2D.Float;
import necesse.engine.util.GameMath;

public interface SoundEmitter extends PrimitiveSoundEmitter {
   float getSoundPositionX();

   float getSoundPositionY();

   @Override
   default float getSoundDistance(float listenerX, float listenerY) {
      return (float)new Float(this.getSoundPositionX(), this.getSoundPositionY()).distance(listenerX, listenerY);
   }

   @Override
   default Float getSoundDirection(float listenerX, float listenerY) {
      return GameMath.normalize(this.getSoundPositionX() - listenerX, this.getSoundPositionY() - listenerY);
   }
}
