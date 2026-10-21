package necesse.inventory.item.toolItem.swordToolItem;

import necesse.engine.localization.Localization;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.entity.levelEvent.mobAbilityLevelEvent.CrystallizeShatterEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.ToolItemMobAbilityEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.Buff;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.CloseRangeWeaponsLootTable;
import necesse.level.maps.Level;

public class AmethystSwordToolItem extends SwordToolItem {
   public AmethystSwordToolItem() {
      super(1000, CloseRangeWeaponsLootTable.closeRangeWeapons);
      this.rarity = Item.Rarity.RARE;
      this.attackAnimTime.setBaseValue(300);
      this.attackDamage.setBaseValue(56.0F).setUpgradedValue(1.0F, 93.33336F);
      this.attackRange.setBaseValue(80);
      this.knockback.setBaseValue(100);
      this.attackXOffset = 8;
      this.attackYOffset = 8;
      this.canBeUsedForRaids = true;
      this.raidTicketsModifier = 0.5F;
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "amethystswordtip"));
      return tooltips;
   }

   @Override
   public void hitMob(InventoryItem item, ToolItemMobAbilityEvent event, Level level, Mob target, Mob attacker) {
      super.hitMob(item, event, level, target, attacker);
      if (level.isServer() && target != null) {
         GameDamage damage = this.getAttackDamage(item);
         float thresholdMod = attacker.getCritChance() + damage.type.getTypeCritChanceModifier(attacker);
         float crystallizeMod = attacker.getCritDamageModifier() + damage.type.getTypeCritDamageModifier(attacker);
         int stackThreshold = (int)GameMath.limit(10.0F - 7.0F * thresholdMod, 3.0F, 10.0F);
         float crystallizeDamageMultiplier = GameMath.limit(crystallizeMod, 2.0F, stackThreshold);
         Buff crystallizeBuff = BuffRegistry.Debuffs.CRYSTALLIZE_BUFF;
         ActiveBuff ab = new ActiveBuff(crystallizeBuff, target, 10000, attacker);
         target.buffManager.addBuff(ab, true);
         ActiveBuff buff = target.buffManager.getBuff(crystallizeBuff);
         if (buff != null && buff.getStacks() >= stackThreshold) {
            level.entityManager.events.add(new CrystallizeShatterEvent(target, CrystallizeShatterEvent.ParticleType.AMETHYST));
            target.buffManager.removeBuff(crystallizeBuff, true);
            GameDamage finalDamage = damage.modDamage(crystallizeDamageMultiplier);
            target.isServerHit(finalDamage, 0.0F, 0.0F, 0.0F, attacker);
         }
      }
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.gemstoneSwords).volume(0.4F);
   }
}
