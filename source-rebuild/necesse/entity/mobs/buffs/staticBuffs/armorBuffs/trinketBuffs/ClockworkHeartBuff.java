package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs;

import necesse.engine.localization.Localization;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.trinketItem.TrinketItem;

public class ClockworkHeartBuff extends TrinketBuff {
   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      this.updateModifiers(buff);
   }

   @Override
   public ListGameTooltips getTrinketTooltip(TrinketItem trinketItem, InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getTrinketTooltip(trinketItem, item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "clockworkhearttip"));
      return tooltips;
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      this.updateModifiers(buff);
   }

   @Override
   public void serverTick(ActiveBuff buff) {
      this.updateModifiers(buff);
   }

   private void updateModifiers(ActiveBuff buff) {
      int lastMaxHealth = buff.getGndData().getInt("lastMaxHealth");
      int nextMaxHealth = buff.owner.getMaxHealth() + lastMaxHealth / 2;
      if (lastMaxHealth != nextMaxHealth) {
         buff.setModifier(BuffModifiers.MAX_HEALTH_FLAT, -nextMaxHealth / 2);
         buff.setModifier(BuffModifiers.MAX_RESILIENCE_FLAT, nextMaxHealth / 2);
         buff.getGndData().setInt("lastMaxHealth", nextMaxHealth);
         buff.owner.buffManager.forceUpdateBuffs();
      }

      Mob owner = buff.owner;
      if (owner.getHealth() < owner.getMaxHealth()) {
         buff.setModifier(BuffModifiers.RESILIENCE_REGEN_FLAT, 0.0F);
      } else {
         buff.setModifier(BuffModifiers.RESILIENCE_REGEN_FLAT, owner.isInCombat() ? owner.getCombatRegen() : owner.getRegen() + owner.getCombatRegen());
      }
   }
}
