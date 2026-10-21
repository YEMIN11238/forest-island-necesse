package necesse.entity.mobs.friendly.critters.caveling;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.MobDrawable;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.TreeObject;
import necesse.level.maps.light.GameLight;

public class DryadCaveling extends CavelingMob {
   public DryadCaveling() {
      super(250, 40);
   }

   @Override
   public void init() {
      super.init();
      this.texture = MobRegistry.Textures.dryadCaveling;
      this.popParticleColor = new Color(200, 200, 200);
      this.singleRockSmallStringID = "dryadtree";
      if (this.item == null) {
         if (GameRandom.globalRandom.getChance(0.02F)) {
            this.item = new InventoryItem("cavelingscollection");
         } else {
            this.item = GameRandom.globalRandom
               .getOneOf(
                  new InventoryItem("amber", GameRandom.globalRandom.getIntBetween(8, 12)),
                  new InventoryItem("lifequartz", GameRandom.globalRandom.getIntBetween(8, 12))
               );
         }
      }
   }

   @Override
   public LootTable getLootTable() {
      return super.getLootTable();
   }

   @Override
   public LootTable getCavelingDropsAsLootTable() {
      return new LootTable(new LootItem("amber", 1), new LootItem("lifequartz", 1));
   }

   @Override
   protected boolean objectDrawOptions(List<MobDrawable> list, GameLight light, boolean hasSpelunker, int drawX, int drawY) {
      if (this.dx == 0.0F && this.dy == 0.0F && this.singleRockSmallStringID != null) {
         GameObject gameObject = ObjectRegistry.getObject(this.singleRockSmallStringID);
         if (gameObject instanceof TreeObject) {
            GameTexture treeTexture = ((TreeObject)gameObject).texture;
            int treeSprite = new GameRandom(this.getUniqueID()).nextInt(treeTexture.getHeight() / 128);
            final TextureDrawOptionsEnd drawOptions = treeTexture.initDraw()
               .sprite(0, treeSprite, 128, 128)
               .startGlowOptions(this, this.getID())
               .light(light)
               .apply(hasSpelunker)
               .pos(drawX - 32, drawY - 64);
            list.add(new MobDrawable() {
               @Override
               public void draw(TickManager tickManager) {
                  drawOptions.draw();
               }
            });
            return true;
         }
      }

      return false;
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return null;
   }
}
