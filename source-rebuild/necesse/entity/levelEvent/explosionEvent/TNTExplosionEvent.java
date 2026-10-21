package necesse.entity.levelEvent.explosionEvent;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.entity.mobs.GameDamage;
import necesse.gfx.GameResources;

public class TNTExplosionEvent extends ExplosionEvent {
   public TNTExplosionEvent() {
      this(0.0F, 0.0F);
   }

   public TNTExplosionEvent(float tileX, float tileY) {
      super(tileX * 32.0F + 16.0F, tileY * 32.0F + 16.0F, 300, new GameDamage(400.0F, 1000.0F), true, 10.0F);
      this.sendCustomData = false;
   }

   @Override
   public void serverTick() {
      this.level.sendObjectChangePacket(this.level.getServer(), this.tileX, this.tileY, 0);
      super.serverTick();
   }

   @Override
   public GameMessage getAttackerName() {
      return new LocalMessage("deaths", "tntname");
   }

   @Override
   protected void playExplosionEffects() {
      SoundManager.playSound(GameResources.explosionHeavy, SoundEffect.effect(this.x, this.y).volume(3.5F).falloffDistance(2000));
      this.level.getClient().startCameraShake(this.x, this.y, 500, 40, 5.0F, 5.0F, true);
   }
}
