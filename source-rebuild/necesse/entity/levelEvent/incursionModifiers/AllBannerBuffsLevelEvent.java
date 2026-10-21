package necesse.entity.levelEvent.incursionModifiers;

import necesse.engine.network.NetworkClient;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameUtils;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.mobs.buffs.ActiveBuff;

public class AllBannerBuffsLevelEvent extends LevelEvent {
   public AllBannerBuffsLevelEvent() {
      super(true);
      this.shouldSave = true;
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.updateClientBuffs();
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.updateClientBuffs();
   }

   public void updateClientBuffs() {
      GameUtils.streamNetworkClients(this.level).filter(NetworkClient::hasSpawned).forEach(client -> {
         client.playerMob.buffManager.addBuff(new ActiveBuff(BuffRegistry.Banners.DAMAGE, client.playerMob, 1, null), false);
         client.playerMob.buffManager.addBuff(new ActiveBuff(BuffRegistry.Banners.SPEED, client.playerMob, 1, null), false);
         client.playerMob.buffManager.addBuff(new ActiveBuff(BuffRegistry.Banners.DEFENSE, client.playerMob, 1, null), false);
         client.playerMob.buffManager.addBuff(new ActiveBuff(BuffRegistry.Banners.SUMMON_SPEED, client.playerMob, 1, null), false);
      });
   }

   @Override
   public boolean isNetworkImportant() {
      return true;
   }
}
