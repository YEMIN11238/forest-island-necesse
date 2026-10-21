package necesse.entity.levelEvent;

import java.util.stream.Stream;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.entity.levelEvent.actions.LongLevelEventAction;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.level.maps.levelBuffManager.LevelBuffsEntityComponent;
import necesse.level.maps.levelBuffManager.LevelModifiers;

public class SpiritCorruptedLevelEvent extends LevelEvent implements LevelBuffsEntityComponent {
   protected long endTime;
   public LongLevelEventAction setEndTimeAction;
   protected long nextBuffUpdateLocalTime;

   public SpiritCorruptedLevelEvent() {
      this.shouldSave = true;
      this.setEndTimeAction = this.registerAction(new LongLevelEventAction() {
         @Override
         protected void run(long value) {
            SpiritCorruptedLevelEvent.this.endTime = value;
         }
      });
   }

   public SpiritCorruptedLevelEvent(long endTime) {
      this();
      this.endTime = endTime;
   }

   @Override
   public boolean isNetworkImportant() {
      return true;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addLong("endTime", this.endTime);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.endTime = save.getLong("endTime", this.endTime);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextLong(this.endTime);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.endTime = reader.getNextLong();
   }

   public long getRemainingTime() {
      return this.endTime - this.getTime();
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (this.getRemainingTime() <= 0L) {
         this.over();
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.getRemainingTime() <= 0L) {
         this.over();
      }

      if (this.isClient()) {
         PlayerMob playerMob = this.getClient().getPlayer();
         if (playerMob != null
            && playerMob.getLevel().isSamePlace(this.level)
            && this.level.getBiome(playerMob.getTileX(), playerMob.getTileY()) == BiomeRegistry.PLAINS
            && this.nextBuffUpdateLocalTime <= this.getLocalTime()) {
            playerMob.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.SPIRIT_CORRUPTED, playerMob, 1000, null), false);
            this.nextBuffUpdateLocalTime = this.getLocalTime() + 500L;
         }
      }
   }

   @Override
   public Stream<ModifierValue<?>> getLevelModifiers() {
      return Stream.of(new ModifierValue<>(LevelModifiers.SPIRIT_CORRUPTED, true));
   }
}
