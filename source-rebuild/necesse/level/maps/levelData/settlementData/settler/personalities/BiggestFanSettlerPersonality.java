package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MobWasKilledEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.projectile.CoinProjectile;
import necesse.gfx.GameColor;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.SpacerGameTooltip;

public class BiggestFanSettlerPersonality extends SettlerPersonality {
   private final int rewardMinCoins = 5;
   private final int rewardMaxCoins = 15;
   private final float basicMobRewardChance = 0.1F;
   private final int bossRewardMultiplier = 10;

   public BiggestFanSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public GameTooltips getTooltip() {
      ListGameTooltips list = new ListGameTooltips();
      list.add(super.getTooltip());
      int qualityOfLifeModifier = this.mob.getSettlerQualityOfLife();
      int minNormalMobReward = 5 * qualityOfLifeModifier;
      int maxNormalMobReward = 15 * qualityOfLifeModifier;
      int minBossMobReward = 5 * qualityOfLifeModifier * 10;
      int maxBossMobReward = 15 * qualityOfLifeModifier * 10;
      GameColor normalEffectColor = maxNormalMobReward == 0 ? GameColor.YELLOW : GameColor.GREEN;
      GameColor bossEffectColor = maxBossMobReward == 0 ? GameColor.YELLOW : GameColor.GREEN;
      LocalMessage normalRewardMessage = new LocalMessage(
         "personalities",
         "biggestfan_reward",
         "value",
         minNormalMobReward == maxNormalMobReward ? minNormalMobReward : minNormalMobReward + "-" + maxNormalMobReward
      );
      LocalMessage bossRewardMessage = new LocalMessage(
         "personalities", "biggestfan_reward", "value", minBossMobReward == maxBossMobReward ? minBossMobReward : minBossMobReward + "-" + maxBossMobReward
      );
      GameMessage effectMessage = new GameMessageBuilder()
         .append(normalEffectColor.getColorCode())
         .append(new LocalMessage("personalities", "biggestfan_effect", "percent", "10%", "reward", normalRewardMessage))
         .append(GameColor.NO_COLOR.getColorCode());
      GameMessage bossEffectMessage = new GameMessageBuilder()
         .append(bossEffectColor.getColorCode())
         .append(new LocalMessage("personalities", "biggestfan_boss_effect", "reward", bossRewardMessage))
         .append(GameColor.NO_COLOR.getColorCode());
      list.add(new SpacerGameTooltip(10));
      list.add(new LocalMessage("personalities", "currentbonus"));
      list.add(effectMessage.translate());
      list.add(bossEffectMessage.translate());
      return list;
   }

   @Override
   public void onAdventurePartyHasKilledTarget(ServerClient partyOwner, MobWasKilledEvent event) {
      super.onAdventurePartyHasKilledTarget(partyOwner, event);
      if (!event.target.isSummoned) {
         boolean isBoss = event.target.isBoss();
         if (isBoss || GameRandom.globalRandom.getChance(0.1F)) {
            int rewardCoins = GameRandom.globalRandom.getIntBetween(5, 15) * (isBoss ? 10 : 1);
            PlayerMob playerMob = this.mob.adventureParty.getPlayerMob();
            int distance = Math.max(1, (int)GameMath.diamondDistance(playerMob.x, playerMob.y, this.mob.x, this.mob.y));
            int radius = (int)Math.min(distance * 0.25F, 100.0F);
            int radiusAngle = (int)(radius * 0.31F);

            for (int i = 0; i < this.mob.getSettlerQualityOfLife(); i++) {
               int angleMod = GameRandom.globalRandom.getIntBetween(-radiusAngle, radiusAngle);
               CoinProjectile coinProjectile = new CoinProjectile(
                  this.mob.x, this.mob.y, playerMob.x, playerMob.y, 200, distance, new GameDamage(0.0F), this.mob, rewardCoins
               );
               if (angleMod != 0) {
                  coinProjectile.setAngle(coinProjectile.getAngle() + angleMod);
               }

               this.mob.getLevel().entityManager.projectiles.add(coinProjectile);
            }
         }
      }
   }
}
