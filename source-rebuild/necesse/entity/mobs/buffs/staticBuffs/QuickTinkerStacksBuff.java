package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.network.gameNetworkData.GNDItem;
import necesse.engine.network.gameNetworkData.GNDItemIntArrayList;
import necesse.engine.registries.MobRegistry;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.friendly.QuickTinkerFollowingMob;

public class QuickTinkerStacksBuff extends Buff {
   public QuickTinkerStacksBuff() {
      this.canCancel = false;
      this.isImportant = true;
      this.isVisible = true;
   }

   @Override
   public void onOverridden(ActiveBuff buff, ActiveBuff other) {
      super.onOverridden(buff, other);
      other.getGndData().copyKeysToTarget(buff.getGndData(), "quickTinkerMobs");
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   @Override
   public int getStackSize(ActiveBuff buff) {
      return 3;
   }

   @Override
   public boolean overridesStackDuration() {
      return true;
   }

   @Override
   public boolean showsFirstStackDurationText() {
      return super.showsFirstStackDurationText();
   }

   public QuickTinkerFollowingMob addNewMob(Mob owner) {
      QuickTinkerFollowingMob mob = (QuickTinkerFollowingMob)MobRegistry.getMob("quicktinkerfollower", owner.getLevel());
      mob.setPos(owner.x, owner.y, true);
      mob.followingMob = owner;
      owner.getLevel().entityManager.mobs.addHidden(mob);
      return mob;
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      GNDItem gndItem = buff.getGndData().getItem("quickTinkerMobs");
      GNDItemIntArrayList list;
      if (gndItem instanceof GNDItemIntArrayList) {
         list = (GNDItemIntArrayList)gndItem;
      } else {
         list = new GNDItemIntArrayList();
         buff.getGndData().setItem("quickTinkerMobs", list);
      }

      int desiredMobs = buff.getStacks();

      for (int i = 0; i < desiredMobs; i++) {
         QuickTinkerFollowingMob mob;
         if (list.size() <= i) {
            mob = this.addNewMob(buff.owner);
            list.add(mob.getUniqueID());
         } else {
            int mobUniqueID = list.get(i);
            Mob found = buff.owner.getLevel().entityManager.mobs.get(mobUniqueID, false);
            if (found instanceof QuickTinkerFollowingMob && ((QuickTinkerFollowingMob)found).followingMob == buff.owner) {
               mob = (QuickTinkerFollowingMob)found;
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
            if (found instanceof QuickTinkerFollowingMob && ((QuickTinkerFollowingMob)found).followingMob == buff.owner) {
               found.remove();
            }
         }
      }

      buff.getGndData().setItem("quickTinkerMobs", list);
   }
}
