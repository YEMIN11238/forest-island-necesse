package necesse.gfx;

import java.awt.Point;
import java.util.function.Consumer;
import necesse.engine.util.GameMath;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

public class PlayerSprite {
   public static void drawInForms(PlayerSprite.DrawInFormsLogic drawLogic, int drawX, int drawY) {
      GameWindow window = WindowManager.getWindow();
      drawInForms(drawLogic, drawX, drawY, window.getHudWidth() / 2, window.getHudHeight() / 2);
   }

   public static void drawInForms(PlayerSprite.DrawInFormsLogic drawLogic, int drawX, int drawY, int width, int height) {
      GameWindow window = WindowManager.getWindow();
      int translateX = Math.max(window.getHudWidth() / 2 - width / 2, 0);
      int translateY = Math.max(window.getHudHeight() / 2 - height / 2, 0);
      window.applyDraw(() -> {
         GameResources.formShader.stop();
         drawLogic.draw(translateX, translateY);
         GameResources.formShader.usePrevState();
      }, () -> {
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glTranslatef(-translateX + drawX, -translateY + drawY, 0.0F);
         GL14.glBlendFuncSeparate(770, 771, 1, 771);
         GameResources.formShader.usePrevState();
      }, () -> {
         GL11.glTranslatef(translateX - drawX, translateY - drawY, 0.0F);
         GameResources.formShader.stop();
      });
   }

   public static DrawOptions getIconDrawOptions(int drawX, int drawY, int width, int height, PlayerMob player, int spriteX, int dir) {
      return getIconDrawOptions(drawX, drawY, width, height, player, spriteX, dir, 1.0F, new GameLight(150.0F));
   }

   public static DrawOptions getIconDrawOptions(
      int drawX, int drawY, int width, int height, PlayerMob player, int spriteX, int dir, float alpha, GameLight light
   ) {
      return getIconDrawOptions(width, height, player, spriteX, dir, light, null).alpha(alpha).pos(drawX, drawY);
   }

   public static HumanDrawOptions getIconDrawOptions(
      int width, int height, PlayerMob player, int spriteX, int dir, GameLight light, Consumer<HumanDrawOptions> humanDrawOptionsModifier
   ) {
      InventoryItem helmet = getPlayerDisplayArmor(player, 0);
      InventoryItem chestplate = getPlayerDisplayArmor(player, 1);
      InventoryItem boots = getPlayerDisplayArmor(player, 2);
      HumanDrawOptions humanDrawOptions = new HumanDrawOptions(player.getLevel(), player.look, false)
         .player(player)
         .blinking(player.isBlinking())
         .helmet(helmet)
         .chestplate(chestplate)
         .boots(boots)
         .size(width, height)
         .invis(player.buffManager.getModifier(BuffModifiers.INVISIBILITY))
         .sprite(spriteX, dir)
         .dir(dir)
         .light(light);
      if (humanDrawOptionsModifier != null) {
         humanDrawOptionsModifier.accept(humanDrawOptions);
      }

      return humanDrawOptions;
   }

   private static InventoryItem getPlayerDisplayArmor(PlayerMob player, int slot) {
      if (player.getInv().equipment.getSelectedCosmeticSlot(slot).isSlotClear() && !player.getInv().equipment.getSelectedArmorSlot(slot).isSlotClear()) {
         if (player.getInv().equipment.getSelectedArmorSlot(slot).getItemSlot().isArmorItem()) {
            return player.getInv().equipment.getSelectedArmorSlot(slot).getItem();
         }
      } else if (!player.getInv().equipment.getSelectedCosmeticSlot(slot).isSlotClear()
         && player.getInv().equipment.getSelectedCosmeticSlot(slot).getItemSlot().isArmorItem()) {
         return player.getInv().equipment.getSelectedCosmeticSlot(slot).getItem();
      }

      return null;
   }

   public static DrawOptions getIconAnimationDrawOptions(int x, int y, int width, int height, PlayerMob player) {
      int dir = player.getDir();
      Point sprite = player.getAnimSprite(player.getX(), player.getY(), dir);
      return getIconDrawOptions(x, y, width, height, player, sprite.x, dir);
   }

   public static DrawOptions getIconDrawOptions(int x, int y, PlayerMob player) {
      return getIconDrawOptions(x, y, 32, 32, player, 0, 2);
   }

   public static DrawOptions getDrawOptions(
      PlayerMob player, int x, int y, GameLight light, GameCamera camera, Consumer<HumanDrawOptions> humanDrawOptionsModifier
   ) {
      Level level = player.getLevel();
      if (level == null) {
         return () -> {};
      }

      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 51;
      int dir = player.getDir();
      InventoryItem helmet = getPlayerDisplayArmor(player, 0);
      InventoryItem chestplate = getPlayerDisplayArmor(player, 1);
      InventoryItem boots = getPlayerDisplayArmor(player, 2);
      Mob mount = null;
      if (player.isRiding()) {
         mount = player.getMount();
      }

      if (mount != null && !player.isAttacking) {
         dir = mount.getRiderDir(dir);
      }

      Point sprite = player.getAnimSprite(x, y, dir);
      float depthPercent = player.inLiquidFloat(x, y);
      if (mount != null) {
         drawY += mount.getBobbing(x, y);
      } else {
         drawY += (int)(player.getBobbing(x, y) * depthPercent);
      }

      drawY += level.getTile(GameMath.getTileCoordinate(x), GameMath.getTileCoordinate(y)).getMobSinkingAmount(player);
      int armSpriteX = sprite.x;
      MaskShaderOptions mask;
      if (mount != null) {
         armSpriteX = mount.getRiderArmSpriteX();
         sprite.x = mount.getRiderSpriteX();
         mask = mount.getRiderMaskOptions(x, y);
      } else {
         mask = player.getSwimMaskShaderOptions(depthPercent);
      }

      float alpha = player.getInvincibilityFrameAlpha();
      HumanDrawOptions options = new HumanDrawOptions(level, player.look, false)
         .player(player)
         .helmet(helmet)
         .chestplate(chestplate)
         .boots(boots)
         .dir(dir)
         .allAlpha(alpha)
         .invis(player.buffManager.getModifier(BuffModifiers.INVISIBILITY))
         .blinking(player.isBlinking())
         .sprite(sprite)
         .armSprite(armSpriteX)
         .light(light);
      InventoryItem selectedItem = player.getSelectedItem();
      if (selectedItem != null && selectedItem.item.holdsItem(selectedItem, player)) {
         options.holdItem(selectedItem);
      }

      if (mask != null) {
         options.mask(mask);
      }

      player.setupAttackDraw(options);
      player.buffManager.addHumanDraws(options);
      player.modifyExpressionDrawOptions(options);
      if (humanDrawOptionsModifier != null) {
         humanDrawOptionsModifier.accept(options);
      }

      return options.pos(drawX, drawY);
   }

   @FunctionalInterface
   public interface DrawInFormsLogic {
      void draw(int var1, int var2);
   }
}
