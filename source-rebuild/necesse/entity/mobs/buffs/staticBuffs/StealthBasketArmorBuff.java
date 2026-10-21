package necesse.entity.mobs.buffs.staticBuffs;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketMobChat;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.ArmorBuff;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;

public class StealthBasketArmorBuff extends ArmorBuff {
   private GameTexture hidingIcon;
   private GameTexture hiddenIcon;
   private static final long hidingTimeInMillis = 2000L;

   public StealthBasketArmorBuff() {
      this.isPassive = true;
      this.shouldSave = false;
      this.canCancel = false;
      this.isImportant = true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.getGndData().setBoolean("standingStill", false);
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.hidingIcon = this.loadIcon("hiding");
      this.hiddenIcon = this.loadIcon("hidden");
   }

   protected GameTexture loadIcon(String id) {
      try {
         return GameTexture.fromFileRaw("buffs/stealthbasket" + id);
      } catch (FileNotFoundException e) {
         return GameTexture.fromFile("buffs/unknown");
      }
   }

   @Override
   public boolean isVisible(ActiveBuff buff) {
      return buff.getGndData().getBoolean("standingStill");
   }

   @Override
   public GameTexture getDrawIcon(ActiveBuff buff) {
      Mob owner = buff.owner;
      GNDItemMap gndData = buff.getGndData();
      return gndData.getBoolean("standingStill") && owner.getTime() - gndData.getLong("lastStillTime") > 2000L ? this.hiddenIcon : this.hidingIcon;
   }

   @Override
   public void serverTick(ActiveBuff buff) {
      super.serverTick(buff);
      Mob owner = buff.owner;
      GNDItemMap gndData = buff.getGndData();
      if (owner.dx == 0.0F && owner.dy == 0.0F && !owner.isRiding() && !owner.isAttacking) {
         if (!gndData.getBoolean("standingStill")) {
            gndData.setBoolean("standingStill", true);
            gndData.setLong("lastStillTime", owner.getTime());
         }

         if (owner.getTime() - gndData.getLong("lastStillTime") > 2000L && buff.getModifier(BuffModifiers.TARGET_RANGE) != -1.0F) {
            buff.setModifier(BuffModifiers.TARGET_RANGE, -1.0F);
            if (owner.buffManager.hasBuff(BuffRegistry.SUMMONED_MOB)) {
               owner.buffManager.removeBuff(BuffRegistry.SUMMONED_MOB, true);
            }

            List<Mob> mobs = this.lookForNearbyTargets(owner);
            float messageChance = 0.5F;

            for (Mob mob : mobs) {
               if (GameRandom.globalRandom.getChance(messageChance)) {
                  messageChance -= 0.4F;
                  if (messageChance <= 0.0F) {
                     break;
                  }

                  this.sendCantFindMessage(mob);
               }
            }
         }
      } else {
         gndData.setBoolean("standingStill", false);
         if (buff.getModifier(BuffModifiers.TARGET_RANGE) != 0.0F) {
            buff.setModifier(BuffModifiers.TARGET_RANGE, 0.0F);
         }
      }
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      Mob owner = buff.owner;
      GNDItemMap gndData = buff.getGndData();
      if (owner.dx == 0.0F && owner.dy == 0.0F && !owner.isRiding() && !owner.isAttacking) {
         if (!gndData.getBoolean("standingStill")) {
            gndData.setBoolean("standingStill", true);
            gndData.setLong("lastStillTime", owner.getTime());
         }

         if (owner.getTime() - gndData.getLong("lastStillTime") > 2000L && buff.getModifier(BuffModifiers.TARGET_RANGE) != -1.0F) {
            buff.setModifier(BuffModifiers.TARGET_RANGE, -1.0F);
            if (owner.buffManager.hasBuff(BuffRegistry.SUMMONED_MOB)) {
               owner.buffManager.removeBuff(BuffRegistry.SUMMONED_MOB, true);
            }
         }
      } else {
         gndData.setBoolean("standingStill", false);
         if (buff.getModifier(BuffModifiers.TARGET_RANGE) != 0.0F) {
            buff.setModifier(BuffModifiers.TARGET_RANGE, 0.0F);
         }
      }
   }

   private void sendCantFindMessage(Mob mob) {
      String randomCantFindMessage = "stealthbasket" + GameRandom.globalRandom.getIntBetween(1, 27);
      LocalMessage randomTranslateMessage = new LocalMessage("mobmsg", randomCantFindMessage);
      mob.getLevel().getServer().network.sendToClientsWithEntity(new PacketMobChat(mob.getUniqueID(), randomTranslateMessage, true), mob);
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      Mob owner = ab.owner;
      GNDItemMap gndData = ab.getGndData();
      if (gndData.getBoolean("standingStill") && owner.getTime() - gndData.getLong("lastStillTime") > 2000L) {
         tooltips.add(Localization.translate("bufftooltip", "stealthbaskethidden"), 400);
      } else {
         tooltips.add(Localization.translate("bufftooltip", "stealthbaskethiding"), 400);
      }

      return tooltips;
   }

   private List<Mob> lookForNearbyTargets(Mob owner) {
      int checkForMobsRange = 384;
      return GameUtils.streamTargetsRange(owner, owner.getX(), owner.getY(), checkForMobsRange)
         .filter(mob -> mob.isHostile && mob.shouldSendSpawnPacket())
         .filter(m -> m.getDistance(owner) <= checkForMobsRange)
         .collect(Collectors.toList());
   }
}
