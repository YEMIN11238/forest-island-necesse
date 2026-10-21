package necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D.Float;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.SoundWaveProjectile;
import necesse.entity.projectile.modifiers.ResilienceOnHitProjectileModifier;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;

public class FrenchHornProjectileToolItem extends MagicProjectileToolItem {
   public FrenchHornProjectileToolItem() {
      super(500, null);
      this.rarity = Item.Rarity.UNCOMMON;
      this.attackAnimTime.setBaseValue(550);
      this.attackDamage.setBaseValue(32.0F).setUpgradedValue(1.0F, 109.200035F);
      this.velocity.setBaseValue(50);
      this.attackXOffset = 30;
      this.attackYOffset = 26;
      this.attackRange.setBaseValue(600).setUpgradedValue(1.0F, 800).setUpgradedValue(10.0F, 1200);
      this.knockback.setBaseValue(50);
      this.manaCost.setBaseValue(2.5F).setUpgradedValue(1.0F, 2.5F);
      this.itemAttackerProjectileCanHitWidth = 5.0F;
      this.itemAttackerPredictionDistanceOffset = -40.0F;
      this.canBeUsedForRaids = false;
      this.useForRaidsOnlyIfObtained = true;
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "frenchhorntip"), 350);
      tooltips.add(GameUtils.getDevQuoteTooltip(new LocalMessage("itemtooltip", "frenchhornquote"), "RRBE", 300));
      return tooltips;
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      drawOptions.pointRotation(attackDirX, attackDirY).forEachItemSprite(i -> i.itemRotateOffset(-45.0F));
   }

   @Override
   public boolean animDrawBehindHand(InventoryItem item) {
      return false;
   }

   @Override
   public Point getItemAttackerAttackPosition(Level level, ItemAttackerMob attackerMob, Mob target, int seed, InventoryItem item) {
      Point pos = super.getItemAttackerAttackPosition(level, attackerMob, target, seed, item);
      pos.x = attackerMob.getX() + (attackerMob.getX() - pos.x);
      pos.y = attackerMob.getY() + (attackerMob.getY() - pos.y);
      return pos;
   }

   @Override
   public InventoryItem onAttack(
      Level level,
      int x,
      int y,
      ItemAttackerMob attackerMob,
      int attackHeight,
      InventoryItem item,
      ItemAttackSlot slot,
      int animAttack,
      int seed,
      GNDItemMap mapContent
   ) {
      x = attackerMob.getX() + (attackerMob.getX() - x);
      y = attackerMob.getY() + (attackerMob.getY() - y);
      GameRandom random = new GameRandom(seed);
      Projectile projectile = new SoundWaveProjectile(
         level,
         attackerMob,
         attackerMob.x,
         attackerMob.y,
         x,
         y,
         this.getProjectileVelocity(item, attackerMob),
         this.getAttackRange(item),
         this.getAttackDamage(item),
         this.getKnockback(item, attackerMob)
      );
      projectile.setModifier(new ResilienceOnHitProjectileModifier(this.getResilienceGain(item)));
      projectile.resetUniqueID(random);
      attackerMob.addAndSendAttackerProjectile(projectile, 40);
      this.consumeMana(attackerMob, item);
      return item;
   }

   @Override
   public void showAttack(
      Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, int animAttack, int seed, GNDItemMap mapContent
   ) {
      super.showAttack(level, x, y, attackerMob, attackHeight, item, animAttack, seed, mapContent);
      if (level.isClient()) {
         Float attackDir = GameMath.normalize(attackerMob.x - x, attackerMob.y - y);

         for (int i = 0; i < 5; i++) {
            int sprite = GameRandom.globalRandom.nextInt(4);
            level.entityManager
               .addParticle(
                  attackerMob.x + attackDir.x * 12.0F + GameRandom.globalRandom.getIntBetween(-5, 5),
                  attackerMob.y + attackDir.y * 12.0F + GameRandom.globalRandom.getIntBetween(-5, 5),
                  Particle.GType.IMPORTANT_COSMETIC
               )
               .lifeTime(1500)
               .sprite(GameResources.musicNotesParticle.sprite(sprite % 2, sprite / 2, 10))
               .sizeFades(14, 22)
               .dontRotate()
               .movesConstant(
                  attackDir.x * 20.0F + GameRandom.globalRandom.getIntBetween(-10, 10), attackDir.y * 20.0F + GameRandom.globalRandom.getIntBetween(-10, 10)
               )
               .color(new Color(255, 255, 255));
         }

         GameRandom random = new GameRandom(seed);
         int previousNotePlayedID = item.getGndData().getInt("previousNotePlayedID", -1);
         int nextMusicNoteIndex = previousNotePlayedID;

         while (GameResources.cMajorScaleShortNotes.length > 1 && nextMusicNoteIndex == previousNotePlayedID) {
            nextMusicNoteIndex = random.nextInt(GameResources.cMajorScaleShortNotes.length);
         }

         item.getGndData().setInt("previousNotePlayedID", nextMusicNoteIndex);
         SoundManager.playSound(GameResources.cMajorScaleShortNotes[nextMusicNoteIndex], SoundEffect.effect(attackerMob));
      }
   }

   @Override
   protected SoundSettings getSwingSound() {
      return null;
   }
}
