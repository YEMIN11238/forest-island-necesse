package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.HumanDrawBuff;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SlimeDomeActiveBuff extends Buff implements HumanDrawBuff {
   public GameTexture slimeDomeTexture;

   public SlimeDomeActiveBuff() {
      this.isVisible = true;
      this.isImportant = true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.INCOMING_DAMAGE_MOD, 0.0F);
      buff.setModifier(BuffModifiers.KNOCKBACK_INCOMING_MOD, 1.0F);
      buff.setMaxModifier(BuffModifiers.FRICTION, 0.25F, 1000);
      buff.setModifier(BuffModifiers.BOUNCY, true);
      buff.setModifier(BuffModifiers.SPEED, 0.5F);
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.slimeDomeTexture = GameTexture.fromFile("particles/slimedome");
   }

   @Override
   public void onWasHit(ActiveBuff buff, MobWasHitEvent event) {
      super.onWasHit(buff, event);
      if (!event.wasPrevented) {
         buff.getGndData().setLong("timeHit", buff.owner.getTime());
      }
   }

   public float getBouncyAnimation(long timeRemaining, int totalTime) {
      if (timeRemaining > totalTime) {
         float perc = GameUtils.getAnimFloat(timeRemaining - totalTime, 200);
         return (float)(Math.sin(perc * Math.PI * 2.0) + 1.0) / 2.0F;
      } else {
         float perc = Math.abs((float)timeRemaining / totalTime - 1.0F);
         return (float)Math.abs((Math.cos((Math.PI * 6) / (perc + 0.5F)) + 1.0) / 2.0);
      }
   }

   @Override
   public void addHumanDraw(ActiveBuff buff, HumanDrawOptions drawOptions) {
      if (buff.owner.buffManager.hasBuff(BuffRegistry.SLIME_DOME_ACTIVE)) {
         long timeHit = buff.getGndData().getLong("timeHit");
         long timeSinceHit = buff.owner.getTime() - timeHit;
         final float widthSize;
         final float heightSize;
         if (timeHit != 0L && timeSinceHit <= 2000L) {
            float bouncePercent = this.getBouncyAnimation(2000L - timeSinceHit, 2000);
            float bounceWidth = GameMath.lerp(bouncePercent, 0.85F, 1.15F);
            float bounceHeight = GameMath.lerp(bouncePercent, 1.15F, 0.85F);
            float animPercent = (float)timeSinceHit / 2000.0F;
            widthSize = GameMath.lerp(animPercent, bounceWidth, 1.0F);
            heightSize = GameMath.lerp(animPercent, bounceHeight, 1.0F);
         } else {
            widthSize = 1.0F;
            heightSize = 1.0F;
         }

         drawOptions.addTopDraw(
            new HumanDrawOptions.HumanDrawOptionsGetter() {
               @Override
               public DrawOptions getDrawOptions(
                  Level level,
                  PlayerMob player,
                  int dir,
                  int spriteX,
                  int spriteY,
                  int spriteRes,
                  int drawX,
                  int drawY,
                  int width,
                  int height,
                  boolean mirrorX,
                  boolean mirrorY,
                  GameLight light,
                  boolean hasGlowEffect,
                  int glowHash,
                  float alpha,
                  MaskShaderOptions mask
               ) {
                  int wiggleWidth = (int)(width * widthSize);
                  int wiggleHeight = (int)(height * heightSize);
                  int widthDiff = width - wiggleWidth;
                  int heightDiff = height - wiggleHeight;
                  return SlimeDomeActiveBuff.this.slimeDomeTexture
                     .initDraw()
                     .size(wiggleWidth, wiggleHeight)
                     .startGlowOptions(level, glowHash)
                     .light(light)
                     .apply(hasGlowEffect)
                     .addMaskShader(mask)
                     .pos(drawX + widthDiff / 2 + 2, drawY + heightDiff / 2 + 2);
               }
            }
         );
      }
   }
}
