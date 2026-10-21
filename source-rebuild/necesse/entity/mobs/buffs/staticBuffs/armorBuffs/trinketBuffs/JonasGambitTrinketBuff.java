package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs;

import necesse.engine.localization.Localization;
import necesse.engine.network.Packet;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffAbility;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.trinketItem.TrinketItem;

public class JonasGambitTrinketBuff extends TrinketBuff implements BuffAbility {
   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.ARMOR_FLAT, 25);
      buff.setModifier(BuffModifiers.ALL_DAMAGE, 0.2F);
      buff.setModifier(BuffModifiers.CRIT_CHANCE, 0.15F);
      buff.setModifier(BuffModifiers.MAX_SUMMONS, 2);
      buff.setModifier(BuffModifiers.SPEED, -0.5F);
   }

   @Override
   public void runAbility(PlayerMob player, ActiveBuff buff, Packet content) {
      SoundManager.playSound(new SoundSettings(GameResources.jinglehit).volume(0.2F).basePitch(1.02F).pitchVariance(0.01F), player);
   }

   @Override
   public boolean canRunAbility(PlayerMob player, ActiveBuff buff, Packet content) {
      return true;
   }

   @Override
   public ListGameTooltips getTrinketTooltip(TrinketItem trinketItem, InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getTrinketTooltip(trinketItem, item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "jonasgambittip"), 400);
      tooltips.add(GameUtils.getDevQuoteTooltip(Localization.translate("itemtooltip", "jonasgambitquote"), "Jonas"));
      return tooltips;
   }
}
