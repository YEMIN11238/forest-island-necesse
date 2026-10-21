package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs;

import java.awt.geom.Point2D.Float;
import necesse.engine.localization.Localization;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MakeshiftDashLevelEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;

public class MakeshiftMeleeSetBonusBuff extends MakeshiftSetBonusBuff {
   public FloatUpgradeValue dashDamage = new FloatUpgradeValue(0.0F, 0.2F).setBaseValue(200.0F).setUpgradedValue(1.0F, 200.0F);

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "makeshiftmeleeset"), 400);
      return tooltips;
   }

   @Override
   public void runAbility(PlayerMob player, ActiveBuff buff, Packet content) {
      super.runAbility(player, buff, content);
      player.constantAttack = true;
      PacketReader reader = new PacketReader(content);
      GameDamage damage = new GameDamage(this.dashDamage.getValue(buff.getUpgradeTier()));
      Float dir = GameMath.normalize(reader.getNextInt() - player.x, reader.getNextInt() - player.y);
      MakeshiftDashLevelEvent event = new MakeshiftDashLevelEvent(player, reader.getNextInt(), dir.x, dir.y, 600.0F, 500, damage);
      player.addAndSendAttackerLevelEvent(event);
      if (player.isClient()) {
         SoundManager.playSound(new SoundSettings(GameResources.makeshiftDash), player);
      }
   }

   @Override
   public Packet getAbilityContent(PlayerMob player, ActiveBuff buff, GameCamera camera) {
      Packet content = new Packet();
      PacketWriter writer = new PacketWriter(content);
      writer.putNextInt(camera.getMouseLevelPosX());
      writer.putNextInt(camera.getMouseLevelPosY());
      writer.putNextInt(GameRandom.globalRandom.nextInt());
      return content;
   }
}
