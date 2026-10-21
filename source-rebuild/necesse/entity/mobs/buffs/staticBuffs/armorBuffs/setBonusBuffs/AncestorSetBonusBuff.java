package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs;

import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.LinkedList;
import necesse.engine.localization.Localization;
import necesse.engine.network.Packet;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffAbility;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.itemAttacker.CheckSlotType;
import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.AncestorKnightFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.AncestorMageFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.AttackingFollowingMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.item.ItemStatTip;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;
import necesse.level.maps.Level;

public class AncestorSetBonusBuff extends SetBonusBuff implements BuffAbility {
   public IntUpgradeValue maxMana = new IntUpgradeValue(0, 0.1F).setBaseValue(275).setUpgradedValue(1.0F, 300);
   public FloatUpgradeValue ancestorKnightDamage = new FloatUpgradeValue(0.0F, 0.2F).setBaseValue(60.0F).setUpgradedValue(1.0F, 60.0F);
   public FloatUpgradeValue ancestorMageDamage = new FloatUpgradeValue(0.0F, 0.2F).setBaseValue(50.0F).setUpgradedValue(1.0F, 50.0F);

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.MAX_MANA_FLAT, this.maxMana.getValue(buff.getUpgradeTier()));
   }

   @Override
   public void runAbility(PlayerMob player, ActiveBuff buff, Packet content) {
      Mob owner = buff.owner;
      float cooldown = 75.0F;
      owner.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.ANCESTOR_SET_COOLDOWN, owner, cooldown, null), false);
      if (player.isServer()) {
         this.summonAncestorMob(buff, this.ancestorKnightDamage, new AncestorKnightFollowingMob(), "ancestorknight");
         this.summonAncestorMob(buff, this.ancestorMageDamage, new AncestorMageFollowingMob(), "ancestormage");
      }
   }

   @Override
   public boolean canRunAbility(PlayerMob player, ActiveBuff buff, Packet content) {
      return !buff.owner.buffManager.hasBuff(BuffRegistry.Debuffs.ANCESTOR_SET_COOLDOWN.getID());
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "ancestorset"), 400);
      return tooltips;
   }

   @Override
   public void addStatTooltips(LinkedList<ItemStatTip> list, ActiveBuff currentValues, ActiveBuff lastValues) {
      super.addStatTooltips(list, currentValues, lastValues);
      currentValues.getModifierTooltipsBuilder(true, true).addLastValues(lastValues).excludeLimits(BuffModifiers.SLOW).buildToStatList(list);
   }

   private void summonAncestorMob(ActiveBuff buff, FloatUpgradeValue variantDamage, AttackingFollowingMob summonMob, String summonType) {
      ItemAttackerMob attackerMob = buff.owner instanceof ItemAttackerMob ? (ItemAttackerMob)buff.owner : null;
      if (attackerMob != null) {
         String shotTimeKey = "summon" + summonType;
         buff.getGndData().setLong(shotTimeKey, attackerMob.getLevel().getWorldEntity().getTime());
         GameDamage damage = new GameDamage(variantDamage.getValue(buff.getUpgradeTier()) * GameDamage.getDamageModifier(attackerMob, DamageTypeRegistry.MAGIC));
         attackerMob.serverFollowersManager.addFollower(summonType, summonMob, FollowPosition.LARGE_PYRAMID, "summonedmob", 1.0F, 1, null, false);
         Float spawnPoint = findSpawnLocation(summonMob, attackerMob.getLevel(), attackerMob.x, attackerMob.y);
         summonMob.updateDamage(damage);
         summonMob.setRemoveWhenNotInInventory(ItemRegistry.getItem("ancestorshat"), CheckSlotType.HELMET);
         attackerMob.getLevel().entityManager.addMob(summonMob, spawnPoint.x, spawnPoint.y);
      }
   }

   public static Float findSpawnLocation(Mob mob, Level level, float centerX, float centerY) {
      ArrayList<Float> possibleSpawns = new ArrayList<>();

      for (int cX = -1; cX <= 1; cX++) {
         for (int cY = -1; cY <= 1; cY++) {
            if (cX != 0 || cY != 0) {
               float posX = centerX + cX * 32;
               float posY = centerY + cY * 32;
               if (!mob.collidesWith(level, (int)posX, (int)posY)) {
                  possibleSpawns.add(new Float(posX, posY));
               }
            }
         }
      }

      return !possibleSpawns.isEmpty() ? possibleSpawns.get(GameRandom.globalRandom.nextInt(possibleSpawns.size())) : new Float(centerX, centerY);
   }
}
