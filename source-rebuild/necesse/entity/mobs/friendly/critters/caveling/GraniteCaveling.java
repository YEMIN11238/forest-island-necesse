package necesse.entity.mobs.friendly.critters.caveling;

import java.awt.Color;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

public class GraniteCaveling extends CavelingMob {
   public GraniteCaveling() {
      super(250, 40);
   }

   @Override
   public void init() {
      super.init();
      this.texture = MobRegistry.Textures.graniteCaveling;
      this.popParticleColor = new Color(200, 200, 200);
      this.singleRockSmallStringID = "granitecaverocksmall";
      if (this.item == null) {
         if (GameRandom.globalRandom.getChance(0.02F)) {
            this.item = new InventoryItem("cavelingsfoot");
         } else {
            this.item = new InventoryItem("runestone", GameRandom.globalRandom.getIntBetween(8, 12));
         }
      }
   }

   @Override
   public LootTable getLootTable() {
      return super.getLootTable();
   }

   @Override
   public LootTable getCavelingDropsAsLootTable() {
      return new LootTable(new LootItem("runestone", 1));
   }
}
