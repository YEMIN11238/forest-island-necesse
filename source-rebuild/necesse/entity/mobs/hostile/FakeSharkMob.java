package necesse.entity.mobs.hostile;

import java.util.ArrayList;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.packet.PacketMobChat;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.FakeSharkAI;
import necesse.entity.mobs.ai.behaviourTree.util.AIMover;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.GameSkin;
import necesse.gfx.HumanLook;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

public class FakeSharkMob extends SharkMob {
   public static LootTable lootTable = new LootTable(new LootItem("sharkmask"), new LootItem("sharkcostumeshirt"), new LootItem("sharkcostumeboots"));

   public FakeSharkMob() {
      this.swimParticleOffset = 18;
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new FakeSharkAI<>(640), new AIMover());
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      GameSkin gameSkin = new HumanLook().getGameSkin(false);

      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), gameSkin, GameRandom.globalRandom.nextInt(5), 8, 32, this.x, this.y, 10.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   protected GameTexture getSharkTexture() {
      return MobRegistry.Textures.fakeShark;
   }

   @Override
   public MobWasHitEvent isServerHit(GameDamage damage, float x, float y, float knockback, Attacker attacker) {
      MobWasHitEvent superHit = super.isServerHit(damage, x, y, knockback, attacker);
      ArrayList<GameMessage> ouchMessage = HumanMob.getLocalMessages("mobmsg", "humanangry", 6);
      if (this.isServer()) {
         this.getServer().network.sendToClientsWithEntity(new PacketMobChat(this.getUniqueID(), GameRandom.globalRandom.getOneOf(ouchMessage)), this);
      }

      return superHit;
   }

   @Override
   protected SoundSettings getDeathSound() {
      return new SoundSettings(GameResources.hurt).volume(1.0F);
   }
}
