package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs;

import java.awt.geom.Point2D.Float;
import necesse.engine.Settings;
import necesse.engine.input.Control;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketGhostBoots;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.ActiveBuffAbility;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.StaminaBuff;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.level.maps.Level;

public class GhostBootsTrinketBuff extends TrinketBuff implements ActiveBuffAbility {
   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   @Override
   public Packet getStartAbilityContent(PlayerMob player, ActiveBuff buff, GameCamera camera) {
      return this.getRunningAbilityContent(player, buff);
   }

   @Override
   public Packet getRunningAbilityContent(PlayerMob player, ActiveBuff buff) {
      Packet content = new Packet();
      PacketWriter writer = new PacketWriter(content);
      StaminaBuff.writeStaminaData(player, writer);
      return content;
   }

   @Override
   public boolean canRunAbility(PlayerMob player, ActiveBuff buff, Packet content) {
      if (buff.owner.isRiding()) {
         return false;
      } else {
         return player.isServer() && !Settings.strictServerAuthority ? true : StaminaBuff.canStartStaminaUsage(buff.owner);
      }
   }

   @Override
   public void onActiveAbilityStarted(PlayerMob player, ActiveBuff buff, Packet content) {
      PacketReader reader = new PacketReader(content);
      if (!player.isServer() || !Settings.strictServerAuthority) {
         StaminaBuff.readStaminaData(player, reader);
      }

      player.buffManager.addBuff(new ActiveBuff(BuffRegistry.GHOST_BOOTS_ACTIVE, player, 1.0F, null), false);
      Mob owner = buff.owner;
      if (owner.buffManager.hasBuff("dashcooldown")) {
         int dashStacks = owner.buffManager.getModifier(BuffModifiers.DASH_STACKS);
         if (owner.buffManager.getBuff("dashcooldown").getStacks() >= dashStacks) {
            return;
         }
      }

      Level level = owner.getLevel();
      int strength = 150;
      Float dir = PacketGhostBoots.getMobDir(owner);
      PacketGhostBoots.applyToPlayer(level, owner, dir.x, dir.y, strength);
      PacketGhostBoots.addCooldownStack(owner, 6.0F, false);
      owner.buffManager.addBuff(new ActiveBuff(BuffRegistry.GHOST_DASH_ACTIVE, owner, 0.3F, null), false);
      owner.buffManager.forceUpdateBuffs();
      if (level != null && level.isClient()) {
         SoundManager.playSound(GameResources.swoosh, SoundEffect.effect(owner).volume(0.5F).pitch(1.7F));
      }
   }

   @Override
   public boolean tickActiveAbility(PlayerMob player, ActiveBuff buff, boolean isRunningClient) {
      if (player.inLiquid()) {
         player.buffManager.removeBuff(BuffRegistry.GHOST_BOOTS_ACTIVE, false);
      } else {
         ActiveBuff speedBuff = player.buffManager.getBuff(BuffRegistry.GHOST_BOOTS_ACTIVE);
         if (speedBuff != null) {
            speedBuff.setDurationLeftSeconds(1.0F);
         } else {
            player.buffManager.addBuff(new ActiveBuff(BuffRegistry.GHOST_BOOTS_ACTIVE, player, 1.0F, null), false);
         }

         if ((player.moveX != 0.0F || player.moveY != 0.0F) && (player.dx != 0.0F || player.dy != 0.0F)) {
            long msToDeplete = 4000L;
            float usage = 50.0F / (float)msToDeplete;
            if (!StaminaBuff.useStaminaAndGetValid(player, usage)) {
               return false;
            }
         }
      }

      return !isRunningClient || Control.TRINKET_ABILITY.isDown();
   }

   @Override
   public void onActiveAbilityUpdate(PlayerMob player, ActiveBuff buff, Packet content) {
   }

   @Override
   public void onActiveAbilityStopped(PlayerMob player, ActiveBuff buff) {
      player.buffManager.removeBuff(BuffRegistry.GHOST_BOOTS_ACTIVE, false);
   }
}
