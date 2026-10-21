package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.network.gameNetworkData.GNDItem;
import necesse.engine.network.gameNetworkData.GNDItemIntArrayList;
import necesse.engine.registries.MobRegistry;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.friendly.LifeEssenceFollowingMob;

public class LifeEssenceStacksBuff extends Buff {
   public static int STACKS_PER_LIFE_ESSENCE = 15;

   public LifeEssenceStacksBuff() {
      this.canCancel = false;
   }

   @Override
   public boolean isVisible(ActiveBuff buff) {
      return buff.getStacks() >= STACKS_PER_LIFE_ESSENCE;
   }

   @Override
   public boolean isImportant(ActiveBuff buff) {
      return buff.getStacks() >= STACKS_PER_LIFE_ESSENCE;
   }

   @Override
   public void onOverridden(ActiveBuff buff, ActiveBuff other) {
      super.onOverridden(buff, other);
      other.getGndData().copyKeysToTarget(buff.getGndData(), "lifeEssenceMobs");
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   @Override
   public int getStacksDisplayCount(ActiveBuff buff) {
      return buff.getStacks() / STACKS_PER_LIFE_ESSENCE;
   }

   @Override
   public int getStackSize(ActiveBuff buff) {
      return STACKS_PER_LIFE_ESSENCE * 5;
   }

   @Override
   public boolean overridesStackDuration() {
      return true;
   }

   @Override
   public boolean showsFirstStackDurationText() {
      return super.showsFirstStackDurationText();
   }

   public LifeEssenceFollowingMob addNewMob(Mob owner) {
      LifeEssenceFollowingMob mob = (LifeEssenceFollowingMob)MobRegistry.getMob("lifeessencefollower", owner.getLevel());
      mob.setPos(owner.x, owner.y, true);
      mob.followingMob = owner;
      owner.getLevel().entityManager.mobs.addHidden(mob);
      return mob;
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      GNDItem gndItem = buff.getGndData().getItem("lifeEssenceMobs");
      GNDItemIntArrayList list;
      if (gndItem instanceof GNDItemIntArrayList) {
         list = (GNDItemIntArrayList)gndItem;
      } else {
         list = new GNDItemIntArrayList();
         buff.getGndData().setItem("lifeEssenceMobs", list);
      }

      int desiredMobs = buff.getStacks() / STACKS_PER_LIFE_ESSENCE;

      for (int i = 0; i < desiredMobs; i++) {
         LifeEssenceFollowingMob mob;
         if (list.size() <= i) {
            mob = this.addNewMob(buff.owner);
            list.add(mob.getUniqueID());
         } else {
            int mobUniqueID = list.get(i);
            Mob found = buff.owner.getLevel().entityManager.mobs.get(mobUniqueID, false);
            if (found instanceof LifeEssenceFollowingMob && ((LifeEssenceFollowingMob)found).followingMob == buff.owner) {
               mob = (LifeEssenceFollowingMob)found;
            } else {
               mob = this.addNewMob(buff.owner);
               list.remove(i);
               list.add(i, mob.getUniqueID());
            }
         }

         mob.index = i;
         mob.totalOther = desiredMobs;
         mob.keepAliveBuffer = 20;
      }

      if (desiredMobs < list.size()) {
         for (int i = desiredMobs; i < list.size(); i++) {
            int mobUniqueID = list.get(i);
            Mob found = buff.owner.getLevel().entityManager.mobs.get(mobUniqueID, false);
            if (found instanceof LifeEssenceFollowingMob && ((LifeEssenceFollowingMob)found).followingMob == buff.owner) {
               found.remove();
            }
         }
      }

      buff.getGndData().setItem("lifeEssenceMobs", list);
   }
}
