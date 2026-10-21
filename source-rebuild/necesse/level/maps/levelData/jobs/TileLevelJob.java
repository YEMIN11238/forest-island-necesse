package necesse.level.maps.levelData.jobs;

import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameLinkedList;
import necesse.level.maps.Level;

public abstract class TileLevelJob extends AbstractLevelJob {
   public final int tileX;
   public final int tileY;
   private Level level;
   private GameLinkedList<TileLevelJob>.Element element;

   public TileLevelJob(int tileX, int tileY) {
      this.tileX = tileX;
      this.tileY = tileY;
   }

   public TileLevelJob(LoadData save) {
      this.tileX = save.getInt("tileX");
      this.tileY = save.getInt("tileY");
   }

   public void addSaveData(SaveData save) {
      save.addInt("tileX", this.tileX);
      save.addInt("tileY", this.tileY);
   }

   public final AbstractLevelJob init(Level level, GameLinkedList<TileLevelJob>.Element element) {
      this.level = level;
      this.element = element;
      return this;
   }

   @Override
   public Level getLevel() {
      return this.level;
   }

   @Override
   public int getTileX() {
      return this.tileX;
   }

   @Override
   public int getTileY() {
      return this.tileY;
   }

   public void remove() {
      if (this.element != null && !this.element.isRemoved()) {
         this.element.remove();
      }
   }

   public boolean isRemoved() {
      return this.element != null && this.element.isRemoved();
   }

   @Override
   public boolean isValid() {
      return !this.isRemoved() && super.isValid();
   }

   public boolean shouldSave() {
      return true;
   }

   protected boolean isSameTile(TileLevelJob other) {
      return this.tileX == other.tileX && this.tileY == other.tileY;
   }

   public boolean isSameJob(TileLevelJob other) {
      return this.isSameTile(other) && this.getID() == other.getID();
   }

   @Override
   public String toString() {
      return super.toString() + "{" + this.tileX + ", " + this.tileY + "}";
   }
}
