package necesse.entity.mobs.buffs.staticBuffs;

import java.io.FileNotFoundException;
import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;

public class BullseyeBuff extends Buff {
   private GameTexture lowStacksTexture;
   private GameTexture maxStacksTexture;

   public BullseyeBuff() {
      this.canCancel = false;
      this.isImportant = true;
      this.isPassive = false;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   @Override
   public int getStackSize(ActiveBuff buff) {
      return 5;
   }

   @Override
   public boolean overridesStackDuration() {
      return true;
   }

   @Override
   public void onStacksUpdated(ActiveBuff buff, ActiveBuff other) {
      super.onStacksUpdated(buff, other);
      if (buff.getStacks() == 5) {
         buff.setModifier(BuffModifiers.RANGED_CRIT_CHANCE, 0.2F);
         buff.setModifier(BuffModifiers.RANGED_CRIT_DAMAGE, 0.2F);
      }
   }

   @Override
   public GameTexture getDrawIcon(ActiveBuff buff) {
      return buff.getStacks() == 5 ? this.maxStacksTexture : this.lowStacksTexture;
   }

   @Override
   public void loadTextures() {
      super.loadTextures();

      try {
         this.lowStacksTexture = GameTexture.fromFileRaw("buffs/bullseye");
         this.maxStacksTexture = GameTexture.fromFileRaw("buffs/bullseyemaxstacks");
      } catch (FileNotFoundException e) {
         this.lowStacksTexture = GameTexture.fromFile("buffs/unknown");
         this.maxStacksTexture = GameTexture.fromFile("buffs/unknown");
      }
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      tooltips.add(Localization.translate("bufftooltip", "bullseyetipnew"), 400);
      return tooltips;
   }
}
