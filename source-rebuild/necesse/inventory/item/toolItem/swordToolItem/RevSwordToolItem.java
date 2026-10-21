package necesse.inventory.item.toolItem.swordToolItem;

import java.awt.Shape;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketPlayObjectDamageSound;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.LineHitbox;
import necesse.entity.ObjectDamageResult;
import necesse.entity.levelEvent.mobAbilityLevelEvent.ToolItemMobAbilityEvent;
import necesse.entity.mobs.AttackAnimMob;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.attackHandler.RevSwordAttackHandler;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.spearToolItem.SpearToolItem;
import necesse.inventory.lootTable.presets.CloseRangeWeaponsLootTable;
import necesse.level.gameObject.TreeObject;
import necesse.level.gameObject.TreeStumpObject;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;

public class RevSwordToolItem extends SpearToolItem {
   public RevSwordToolItem() {
      super(2000, CloseRangeWeaponsLootTable.closeRangeWeapons);
      this.rarity = Item.Rarity.EPIC;
      this.attackAnimTime.setBaseValue(100);
      this.attackDamage.setBaseValue(55.0F).setUpgradedValue(1.0F, 64.16669F);
      this.attackRange.setBaseValue(160);
      this.knockback.setBaseValue(100);
      this.attackXOffset = 12;
      this.attackYOffset = 6;
      this.canBeUsedForRaids = true;
      this.raidTicketsModifier = 0.5F;
      this.resilienceGain.setBaseValue(1.2F).setUpgradedValue(1.0F, 1.2F).setUpgradedValue(10.0F, 2.4F);
   }

   @Override
   public boolean getConstantUse(InventoryItem item) {
      return false;
   }

   @Override
   public void showAttack(
      Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, int animAttack, int seed, GNDItemMap mapContent
   ) {
      if (item.getGndData().getBoolean("charged")) {
         if (level.isClient()) {
            this.playAttackSound(attackerMob);
         }
      }
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      drawOptions.pointRotation(attackDirX, attackDirY);
      drawOptions.thrustOffsets(
         attackDirX + GameRandom.globalRandom.getFloatBetween(-0.3F, 0.3F),
         attackDirY + GameRandom.globalRandom.getFloatBetween(-0.3F, 0.3F),
         attackProgress / 8.0F
      );
   }

   @Override
   public ArrayList<Shape> getHitboxes(InventoryItem item, AttackAnimMob mob, int aimX, int aimY, ToolItemMobAbilityEvent event, boolean forDebug) {
      ArrayList<Shape> out = new ArrayList<>();
      float attackRange = this.getAttackRange(item);
      Float dir = GameMath.normalize(aimX, aimY);
      float yOffset = Math.min(mob.getCurrentAttackDrawYOffset() + mob.getStartAttackHeight(), 0);
      java.awt.geom.Line2D.Float attackLine = new java.awt.geom.Line2D.Float(
         mob.x + dir.x * attackRange / 2.0F, mob.y + dir.y * attackRange / 2.0F + yOffset, mob.x + dir.x / attackRange, mob.y + dir.y / attackRange + yOffset
      );
      if (this.width > 0.0F) {
         out.add(new LineHitbox(attackLine, this.width));
      } else {
         out.add(attackLine);
      }

      return out;
   }

   @Override
   public void addSpearTooltip(ListGameTooltips tooltips, InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "revswordtip"));
      return tooltips;
   }

   @Override
   public int getItemAttackerMinimumAttackRange(ItemAttackerMob attackerMob, InventoryItem item) {
      return this.getAttackRange(item) / 4;
   }

   @Override
   public int getItemAttackerStoppingDistance(ItemAttackerMob mob, InventoryItem item, int attackRange) {
      return super.getItemAttackerStoppingDistance(mob, item, attackRange / 2);
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
      attackerMob.startAttackHandler(new RevSwordAttackHandler(attackerMob, slot, item, this, seed, x, y));
      return item;
   }

   public InventoryItem superOnAttack(
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
      return super.onAttack(level, x, y, attackerMob, attackHeight, item, slot, animAttack, seed, mapContent);
   }

   @Override
   public boolean canHitObject(LevelObject levelObject) {
      return !(levelObject.object instanceof TreeObject) && !(levelObject.object instanceof TreeStumpObject) ? super.canHitObject(levelObject) : true;
   }

   @Override
   public void hitObject(InventoryItem item, LevelObject levelObject, Mob mob) {
      if (!(levelObject.object instanceof TreeObject) && !(levelObject.object instanceof TreeStumpObject)) {
         super.hitObject(item, levelObject, mob);
      } else {
         ServerClient client = mob.isPlayer ? ((PlayerMob)mob).getServerClient() : null;
         ObjectDamageResult result = levelObject.level
            .entityManager
            .doObjectDamage(0, levelObject.tileX, levelObject.tileY, (int)(this.getAttackDamage(item).damage * 0.25F), this.getUpgradeTier(item), mob, client);
         if (result != null) {
            levelObject.level
               .getServer()
               .network
               .sendToClientsWithTile(
                  new PacketPlayObjectDamageSound(levelObject.tileX, levelObject.tileY, levelObject.object.getID()),
                  levelObject.level,
                  levelObject.tileX,
                  levelObject.tileY
               );
         }
      }
   }

   @Override
   public GameSprite getAttackSprite(InventoryItem item, PlayerMob player) {
      int attackCooldown = item.getGndData().getInt("currentAttackCooldown");
      int timePerFrame = attackCooldown != 0 ? attackCooldown : 100;
      int spriteRes = this.attackTexture.getHeight();
      int sprites = this.attackTexture.getWidth() / spriteRes;
      int sprite = GameUtils.getAnim(player == null ? System.currentTimeMillis() : player.getLocalTime(), sprites, sprites * timePerFrame);
      return new GameSprite(this.attackTexture, sprite, 0, spriteRes);
   }
}
