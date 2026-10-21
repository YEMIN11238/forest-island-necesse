package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs;

import java.awt.geom.Point2D.Float;
import java.util.LinkedList;
import necesse.engine.localization.Localization;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobManaChangedEvent;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.PharaohManaSpentBuff;
import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.AttackingFollowingMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.item.ItemStatTip;
import necesse.inventory.item.toolItem.summonToolItem.SummonToolItem;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;

public class PharaohsSetBonusBuff extends SetBonusBuff {
   protected static final int maxLocustCount = 10;
   protected static final String summonType = "locust";
   protected static final int locustSpawnCooldown = 15;
   public IntUpgradeValue consumedManaForSpawn = new IntUpgradeValue().setBaseValue(75).setUpgradedValue(1.0F, 50);
   public FloatUpgradeValue locustDamage = new FloatUpgradeValue(0.0F, 0.2F).setBaseValue(28.0F).setUpgradedValue(1.0F, 50.0F);

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      eventSubscriber.subscribeEvent(MobManaChangedEvent.class, event -> {
         if (!buff.owner.isClient()) {
            if (event.currentMana < event.lastMana && !event.fromUpdatePacket) {
               float manaSpent = event.lastMana - event.currentMana;
               ActiveBuff manabuff = buff.owner.buffManager.getBuff(BuffRegistry.PHARAOH_MANA_SPENT);
               int manaBuffStacks = manabuff == null ? 0 : manabuff.getStacks();
               int manaNeededForSpawn = this.consumedManaForSpawn.getValue(buff.getUpgradeTier());
               if (manaBuffStacks + manaSpent >= manaNeededForSpawn - 1) {
                  this.spawnLocustFollower(buff.owner, buff);
                  buff.owner.buffManager.removeBuff(BuffRegistry.PHARAOH_MANA_SPENT, true);
               }

               int exceeding = (int)(manaBuffStacks + manaSpent) - manaNeededForSpawn;
               int add = (int)manaSpent;
               if (exceeding > 0) {
                  add = exceeding;
               }

               float currentBelowOne = manaSpent - (int)manaSpent;
               float manaBelowOneBuffer = buff.getGndData().getFloat("manaBelowOneBuffer");
               manaBelowOneBuffer += currentBelowOne;
               if (manaBelowOneBuffer >= 1.0F) {
                  manaBelowOneBuffer--;
                  add++;
               }

               buff.getGndData().setFloat("manaBelowOneBuffer", manaBelowOneBuffer);

               for (int i = 0; i < add; i++) {
                  ActiveBuff activeBuff = new ActiveBuff(BuffRegistry.PHARAOH_MANA_SPENT, buff.owner, 60.0F, null);
                  PharaohManaSpentBuff buffbuff = (PharaohManaSpentBuff)activeBuff.buff;
                  buffbuff.setManaNeeded(manaNeededForSpawn);
                  buff.owner.buffManager.addBuff(activeBuff, true);
               }
            }
         }
      });
   }

   @Override
   public void serverTick(ActiveBuff buff) {
      super.serverTick(buff);
      long locustSpawnTimer = buff.getGndData().getLong("locustSpawnTimer");
      if (++locustSpawnTimer / 20L >= 15L && buff.owner.isItemAttacker) {
         ItemAttackerMob attackerMob = (ItemAttackerMob)buff.owner;
         float count = attackerMob.serverFollowersManager.getFollowerCount("locust");
         if (count < 10.0F) {
            this.spawnLocustFollower(buff.owner, buff);
            locustSpawnTimer = 0L;
         }
      }

      buff.getGndData().setLong("locustSpawnTimer", locustSpawnTimer);
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "pharaohssetbonustip1"), 400);
      tooltips.add(Localization.translate("itemtooltip", "pharaohssetbonustip2", "value", this.consumedManaForSpawn.getValue(ab.getUpgradeTier())), 400);
      return tooltips;
   }

   @Override
   public void addStatTooltips(LinkedList<ItemStatTip> list, ActiveBuff currentValues, ActiveBuff lastValues) {
      super.addStatTooltips(list, currentValues, lastValues);
      currentValues.getModifierTooltipsBuilder(true, true).addLastValues(lastValues).buildToStatList(list);
   }

   private void spawnLocustFollower(Mob owner, ActiveBuff buff) {
      if (!owner.isClient()) {
         GameDamage explosionDamge = new GameDamage(this.locustDamage.getValue(buff.getUpgradeTier()));
         AttackingFollowingMob locust = (AttackingFollowingMob)MobRegistry.getMob("locust", owner.getLevel());
         ((ItemAttackerMob)owner).serverFollowersManager.addFollower("locust", locust, FollowPosition.WALK_CLOSE, "summonedlocust", 1.0F, 10, null, false);
         Float spawnPoint = SummonToolItem.findSpawnLocation(locust, owner.getLevel(), owner.x, owner.y);
         locust.updateDamage(explosionDamge);
         owner.getLevel().entityManager.addMob(locust, spawnPoint.x, spawnPoint.y);
      }
   }
}
