package necesse.level.gameObject;

import java.awt.Color;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.TrainingDummyObjectEntity;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;

public class SnowManTrainingDummyObject extends TrainingDummyObject {
   public SnowManTrainingDummyObject() {
      this.mapColor = new Color(223, 244, 255);
   }

   @Override
   public void loadTextures() {
      this.base = GameTexture.fromFile("objects/snowmantrainingdummy_base");
      this.body = GameTexture.fromFile("objects/snowmantrainingdummy_body");
   }

   @Override
   public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
      return new TrainingDummyObjectEntity(level, x, y, true);
   }
}
