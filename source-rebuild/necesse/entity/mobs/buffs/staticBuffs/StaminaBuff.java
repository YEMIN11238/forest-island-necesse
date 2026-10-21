package necesse.entity.mobs.buffs.staticBuffs;

import java.io.FileNotFoundException;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.MovementTickBuff;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameTexture.GameTexture;

public class StaminaBuff extends Buff implements MovementTickBuff {
   protected GameTexture cooldownTexture;

   public StaminaBuff() {
      this.isPassive = true;
      this.shouldSave = true;
      this.overrideSync = true;
      this.canCancel = false;
      this.isImportant = true;
      this.sortByDuration = false;
   }

   @Override
   public void loadTextures() {
      super.loadTextures();

      try {
         this.cooldownTexture = GameTexture.fromFileRaw("buffs/" + this.getStringID() + "_cooldown");
      } catch (FileNotFoundException e) {
         this.cooldownTexture = GameTexture.fromFile("buffs/unknown");
      }
   }

   @Override
   public void drawIcon(int x, int y, ActiveBuff buff) {
      GNDItemMap gndData = buff.getGndData();
      boolean onCooldown = gndData.getBoolean("onCooldown");
      if (onCooldown) {
         this.cooldownTexture.initDraw().size(32, 32).draw(x, y);
      } else {
         this.iconTexture.initDraw().size(32, 32).draw(x, y);
      }

      float stamina = gndData.getFloat("stamina");
      stamina = GameMath.limit(stamina, 0.0F, 1.0F);
      int staminaInt = (int)(Math.abs(stamina - 1.0F) * 100.0F);
      staminaInt = Math.max(staminaInt, onCooldown ? 0 : 1);
      String text = staminaInt + "%";
      int width = FontManager.bit.getWidthCeil(text, durationFontOptions);
      FontManager.bit.drawString(x + 16 - width / 2, y + 30, text, durationFontOptions);
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   @Override
   public void tickMovement(ActiveBuff buff, float delta) {
      long time = buff.owner.getWorldEntity().getTime();
      GNDItemMap gndData = buff.getGndData();
      boolean onCooldown = gndData.getBoolean("onCooldown");
      float stamina = gndData.getFloat("stamina");
      if (stamina <= 0.0F) {
         gndData.setBoolean("onCooldown", false);
         buff.remove();
      } else {
         long lastUsageTime = gndData.getLong("lastUsageTime");
         long timeSinceLastUsage = time - lastUsageTime;
         int regenDelay = onCooldown ? 800 : 200;
         if (timeSinceLastUsage <= -1000L || timeSinceLastUsage >= regenDelay) {
            long msToReplenish = 3000L;
            float decrease = delta / (float)msToReplenish;
            decrease *= buff.owner.buffManager.getModifier(BuffModifiers.STAMINA_REGEN);
            float capacityMod = buff.owner.buffManager.getModifier(BuffModifiers.STAMINA_CAPACITY);
            if (capacityMod != 0.0F) {
               decrease *= 1.0F / capacityMod;
            } else {
               decrease = 1.0F;
            }

            stamina -= decrease;
            if (stamina <= 0.0F) {
               gndData.setFloat("stamina", 0.0F);
               gndData.setBoolean("onCooldown", false);
               buff.remove();
            } else {
               gndData.setFloat("stamina", stamina);
            }
         }
      }
   }

   @Override
   public boolean shouldDrawDuration(ActiveBuff buff) {
      return true;
   }

   public static float getCurrentStamina(Mob mob) {
      ActiveBuff buff = mob.buffManager.getBuff(BuffRegistry.STAMINA_BUFF);
      return buff != null ? buff.getGndData().getFloat("stamina") : 0.0F;
   }

   public static void setCurrentStamina(Mob mob, float stamina) {
      ActiveBuff buff = mob.buffManager.getBuff(BuffRegistry.STAMINA_BUFF);
      if (buff == null) {
         buff = mob.buffManager.addBuff(new ActiveBuff(BuffRegistry.STAMINA_BUFF, mob, 0.0F, null), false);
      }

      buff.getGndData().setFloat("stamina", stamina);
   }

   public static boolean canStartStaminaUsage(Mob mob) {
      ActiveBuff buff = mob.buffManager.getBuff(BuffRegistry.STAMINA_BUFF);
      if (buff != null) {
         GNDItemMap gndData = buff.getGndData();
         return gndData.getBoolean("onCooldown") ? false : gndData.getFloat("stamina") < 1.0F;
      } else {
         return true;
      }
   }

   public static void writeStaminaData(Mob mob, PacketWriter writer) {
      ActiveBuff buff = mob.buffManager.getBuff(BuffRegistry.STAMINA_BUFF);
      if (buff != null) {
         writer.putNextBoolean(true);
         buff.getGndData().writePacket(writer);
      } else {
         writer.putNextBoolean(false);
      }
   }

   public static void readStaminaData(Mob mob, PacketReader reader) {
      boolean active = reader.getNextBoolean();
      if (!active) {
         mob.buffManager.removeBuff(BuffRegistry.STAMINA_BUFF, false);
      } else {
         ActiveBuff buff = mob.buffManager.getBuff(BuffRegistry.STAMINA_BUFF);
         if (buff == null) {
            buff = mob.buffManager.addBuff(new ActiveBuff(BuffRegistry.STAMINA_BUFF, mob, 0.0F, null), false);
         }

         buff.getGndData().readPacket(reader);
      }
   }

   public static void keepStaminaUsage(Mob mob) {
      ActiveBuff buff = mob.buffManager.getBuff(BuffRegistry.STAMINA_BUFF);
      if (buff != null) {
         long time = mob.getWorldEntity().getTime();
         buff.getGndData().setLong("lastUsageTime", time);
      }
   }

   public static float getStamina(Mob mob) {
      ActiveBuff buff = mob.buffManager.getBuff(BuffRegistry.STAMINA_BUFF);
      if (buff == null) {
         return 0.0F;
      }

      GNDItemMap gndData = buff.getGndData();
      return gndData.getFloat("stamina");
   }

   public static void setStamina(Mob mob, float stamina, boolean setLastUsageTime, boolean onCooldown) {
      ActiveBuff buff = mob.buffManager.getBuff(BuffRegistry.STAMINA_BUFF);
      if (buff == null) {
         buff = mob.buffManager.addBuff(new ActiveBuff(BuffRegistry.STAMINA_BUFF, mob, 1.0F, null), false);
      }

      GNDItemMap gndData = buff.getGndData();
      gndData.setFloat("stamina", stamina);
      if (setLastUsageTime) {
         gndData.setLong("lastUsageTime", mob.getWorldEntity().getTime());
      }

      if (stamina >= 1.0F || onCooldown) {
         gndData.setBoolean("onCooldown", true);
      }
   }

   public static boolean useStaminaAndGetValid(Mob mob, float usage) {
      usage *= mob.buffManager.getModifier(BuffModifiers.STAMINA_USAGE);
      float capacityMod = mob.buffManager.getModifier(BuffModifiers.STAMINA_CAPACITY);
      if (capacityMod != 0.0F) {
         usage *= 1.0F / capacityMod;
      } else {
         usage = 1.0F;
      }

      if (usage > 0.0F) {
         long time = mob.getWorldEntity().getTime();
         ActiveBuff buff = mob.buffManager.getBuff(BuffRegistry.STAMINA_BUFF);
         if (buff == null) {
            buff = mob.buffManager.addBuff(new ActiveBuff(BuffRegistry.STAMINA_BUFF, mob, 1.0F, null), false);
         }

         GNDItemMap gndData = buff.getGndData();
         float stamina = gndData.getFloat("stamina");
         stamina = Math.min(stamina + usage, 1.0F);
         gndData.setFloat("stamina", stamina);
         gndData.setLong("lastUsageTime", time);
         if (stamina >= 1.0F) {
            gndData.setBoolean("onCooldown", true);
            return false;
         } else {
            return true;
         }
      } else {
         return true;
      }
   }
}
