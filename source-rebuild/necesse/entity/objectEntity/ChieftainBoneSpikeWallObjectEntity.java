package necesse.entity.objectEntity;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.level.maps.Level;

public class ChieftainBoneSpikeWallObjectEntity extends ObjectEntity {
   protected boolean isRising = true;
   protected long animationStartTime;
   public static int ANIMATION_TIME = 2000;

   public ChieftainBoneSpikeWallObjectEntity(Level level, int x, int y) {
      super(level, "chieftainbonespikewall", x, y);
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addLong("animationStartTime", this.animationStartTime);
      save.addBoolean("isRising", this.isRising);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.animationStartTime = save.getLong("animationStartTime", 0L);
      this.isRising = save.getBoolean("isRising", this.isRising);
   }

   @Override
   public void setupContentPacket(PacketWriter writer) {
      super.setupContentPacket(writer);
      writer.putNextLong(this.animationStartTime);
      writer.putNextBoolean(this.isRising);
   }

   @Override
   public void applyContentPacket(PacketReader reader) {
      super.applyContentPacket(reader);
      this.animationStartTime = reader.getNextLong();
      this.isRising = reader.getNextBoolean();
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (!this.isRising && this.animationStartTime + ANIMATION_TIME <= this.getTime()) {
         this.getLevel().setObject(this.tileX, this.tileY, 0);
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (!this.isRising && this.animationStartTime + ANIMATION_TIME <= this.getTime()) {
         this.getLevel().setObject(this.tileX, this.tileY, 0);
      }
   }

   public void startAnimation(boolean isRising) {
      this.isRising = isRising;
      this.animationStartTime = this.getTime();
   }

   public float getAnimationHeightProgress() {
      long progress = this.getTime() - this.animationStartTime;
      if (this.isRising) {
         return progress < ANIMATION_TIME ? (float)progress / ANIMATION_TIME : 1.0F;
      } else {
         return progress < ANIMATION_TIME ? 1.0F - (float)progress / ANIMATION_TIME : 0.0F;
      }
   }
}
