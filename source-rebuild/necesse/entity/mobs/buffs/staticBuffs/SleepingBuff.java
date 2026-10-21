package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;

public class SleepingBuff extends Buff {
   public SleepingBuff() {
      this.isVisible = false;
      this.shouldSave = false;
      this.canCancel = false;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      GNDItemMap gndData = buff.getGndData();
      float regen = gndData.getFloat("regen");
      if (regen == 0.0F) {
         regen = Math.max(buff.owner.getMaxHealth() / 20.0F, 5.0F);
         gndData.setFloat("regen", regen);
      }

      float manaRegen = gndData.getFloat("manaRegen");
      if (manaRegen == 0.0F) {
         manaRegen = Math.max(buff.owner.getMaxMana() / 20.0F, 5.0F);
         gndData.setFloat("manaRegen", manaRegen);
      }

      buff.setModifier(BuffModifiers.HEALTH_REGEN_FLAT, regen);
      buff.setModifier(BuffModifiers.MANA_REGEN_FLAT, manaRegen);
   }

   @Override
   public void serverTick(ActiveBuff buff) {
      super.serverTick(buff);
      if (buff.owner.isPlayer && buff.owner.isServer()) {
         ServerClient serverClient = ((PlayerMob)buff.owner).getServerClient();
         if (serverClient != null) {
            serverClient.refreshAFKTimer();
         }
      }
   }

   @Override
   public void onWasHit(ActiveBuff buff, MobWasHitEvent event) {
      super.onWasHit(buff, event);
      if (!event.wasPrevented) {
         if (buff.owner.isPlayer && buff.owner.isServer()) {
            PlayerMob owner = (PlayerMob)buff.owner;
            ServerClient serverClient = owner.getServerClient();
            if (serverClient != null) {
               serverClient.closeContainer(true);
            }
         }

         buff.remove();
      }
   }
}
