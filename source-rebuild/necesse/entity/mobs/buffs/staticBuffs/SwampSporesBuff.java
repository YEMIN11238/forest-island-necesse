package necesse.entity.mobs.buffs.staticBuffs;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicBoolean;
import necesse.engine.postProcessing.PostProcessingEffects;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;

public class SwampSporesBuff extends Buff {
   public SwampSporesBuff() {
      this.isImportant = true;
      this.shouldSave = true;
      this.canCancel = false;
      this.sortByDuration = false;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      this.updateBlindnessModifier(buff);
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      if (buff.owner.isClient() && buff.owner.getClient().getPlayer() == buff.owner) {
         Color color = new Color(213, 98, 232);
         float mod = (float)Math.pow(buff.getStacks() / 100.0F, 0.5);
         PostProcessingEffects.setSceneShade(
            GameMath.lerp(mod, 1.0F, color.getRed() / 255.0F),
            GameMath.lerp(mod, 1.0F, color.getGreen() / 255.0F),
            GameMath.lerp(mod, 1.0F, color.getBlue() / 255.0F)
         );
      }
   }

   @Override
   public void onStacksUpdated(ActiveBuff buff, ActiveBuff other) {
      super.onStacksUpdated(buff, other);
      this.updateBlindnessModifier(buff);
   }

   private void updateBlindnessModifier(ActiveBuff buff) {
      float blindness = (float)Math.pow(0.01F * buff.getStacks(), 0.5);
      buff.setModifier(BuffModifiers.BLINDNESS, Math.min(blindness, 0.75F));
   }

   @Override
   public int getStackSize(ActiveBuff buff) {
      return 100;
   }

   @Override
   public boolean overridesStackDuration() {
      return true;
   }

   @Override
   public int getStacksDisplayCount(ActiveBuff buff) {
      return 1;
   }

   @Override
   public String getDurationText(ActiveBuff buff) {
      return buff.getStacks() + "%";
   }

   @Override
   public int getRemainingStacksDuration(ActiveBuff buff, AtomicBoolean sendUpdatePacket) {
      return 25;
   }
}
