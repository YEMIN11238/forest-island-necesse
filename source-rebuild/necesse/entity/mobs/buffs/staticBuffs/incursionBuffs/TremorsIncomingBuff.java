package necesse.entity.mobs.buffs.staticBuffs.incursionBuffs;

import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.incursionModifiers.TremorsModifierLevelEvent;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.staticBuffs.Buff;

public class TremorsIncomingBuff extends Buff {
   public TremorsIncomingBuff() {
      this.isImportant = true;
      this.canCancel = false;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   @Override
   public String getDurationText(ActiveBuff buff) {
      int eventUniqueID = buff.getGndData().getInt("levelEventUniqueID");
      if (buff.owner.getLevel() != null) {
         LevelEvent event = buff.owner.getLevel().entityManager.events.get(eventUniqueID, false);
         if (event instanceof TremorsModifierLevelEvent) {
            int timeToNext = ((TremorsModifierLevelEvent)event).getTimeToNextTremor();
            return ActiveBuff.convertSecondsToText(timeToNext / 1000.0F);
         }
      }

      return super.getDurationText(buff);
   }
}
