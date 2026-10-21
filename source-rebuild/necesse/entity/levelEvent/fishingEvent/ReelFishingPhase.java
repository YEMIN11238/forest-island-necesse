package necesse.entity.levelEvent.fishingEvent;

import java.awt.Point;
import java.util.ArrayList;
import necesse.engine.journal.JournalChallenge;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.JournalChallengeRegistry;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.sound.SoundSettings;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.projectile.FishingHookProjectile;
import necesse.entity.trails.FishingTrail;
import necesse.gfx.GameResources;
import necesse.inventory.InventoryItem;

public class ReelFishingPhase extends FishingPhase {
   private final ArrayList<ReelFishingPhase.ReelParticles> lines;
   private SoundPlayer reelSoundPlayer;

   public ReelFishingPhase(FishingEvent event, ArrayList<WaitFishingPhase.FishingLure> hooks) {
      super(event);
      this.lines = new ArrayList<>(hooks.size());
      int totalCatches = 0;
      boolean isPlayer = event.getMob().isPlayer;

      for (WaitFishingPhase.FishingLure line : hooks) {
         InventoryItem caught = line.getCatch();
         if (isPlayer && caught != null && event.level.isServer() && caught.item.getStringID().equals("icefish")) {
            ServerClient serverClient = ((PlayerMob)event.getMob()).getServerClient();
            if (serverClient != null) {
               JournalChallenge challenge = JournalChallengeRegistry.getChallenge(JournalChallengeRegistry.CATCH_ICEFISH_ID);
               if (!challenge.isCompleted(serverClient) && challenge.isJournalEntryDiscovered(serverClient)) {
                  challenge.markCompleted(serverClient);
                  serverClient.forceCombineNewStats();
               }
            }
         }

         this.lines.add(new ReelFishingPhase.ReelParticles(line.hookPosition, caught));
         if (caught != null) {
            totalCatches++;
         }
      }

      if (event.level.isServer() && totalCatches >= 2 && isPlayer) {
         ServerClient client = ((PlayerMob)event.getMob()).getServerClient();
         if (client != null && client.achievementsLoaded()) {
            client.achievements().DOUBLE_CATCH.markCompleted(client);
         }
      }
   }

   @Override
   public void tickMovement(float delta) {
      boolean isOver = true;

      for (ReelFishingPhase.ReelParticles line : this.lines) {
         line.tickMovement();
         if (!line.isOver) {
            isOver = false;
         }
      }

      if (isOver) {
         this.event.over();
      }
   }

   @Override
   public void clientTick() {
      if (this.reelSoundPlayer == null || this.reelSoundPlayer.isDone()) {
         this.reelSoundPlayer = SoundManager.playSound(new SoundSettings(GameResources.fishingRodReel).volume(0.6F).basePitch(1.1F), this.event.getMob());
      }

      if (this.reelSoundPlayer != null) {
         this.reelSoundPlayer.refreshLooping(0.2F);
      }
   }

   @Override
   public void serverTick() {
   }

   @Override
   public void end() {
      this.lines.forEach(ReelFishingPhase.ReelParticles::remove);
   }

   @Override
   public void over() {
      if (!this.event.isClient()) {
         boolean giveBaitBack = true;

         for (ReelFishingPhase.ReelParticles line : this.lines) {
            if (!line.spawnedItem) {
               line.spawnItem();
            }

            if (line.spawnedItem) {
               giveBaitBack = false;
            }
         }

         if (giveBaitBack) {
            this.event.giveBaitBack();
         }
      }

      this.event.getFishingMob().stopFishing();
      this.lines.forEach(ReelFishingPhase.ReelParticles::remove);
   }

   private class ReelParticles {
      public FishingTrail line;
      public FishingHookProjectile hook;
      public final InventoryItem caught;
      public boolean spawnedItem;
      public boolean isOver;

      public ReelParticles(Point hookPoint, InventoryItem caught) {
         this.caught = caught;
         this.hook = new FishingHookProjectile(
            ReelFishingPhase.this.event.level, ReelFishingPhase.this.event, ReelFishingPhase.this.event.getMob(), caught == null ? null : caught.item
         );
         this.hook
            .applyData(
               hookPoint.x,
               hookPoint.y,
               ReelFishingPhase.this.event.getMob().getX(),
               ReelFishingPhase.this.event.getMob().getY(),
               ReelFishingPhase.this.event.fishingRod.hookSpeed * 1.25F,
               ReelFishingPhase.this.event.fishingRod.lineLength * 5,
               new GameDamage(0.0F),
               ReelFishingPhase.this.event.getMob()
            );
         ReelFishingPhase.this.event.level.entityManager.projectiles.addHidden(this.hook);
         if (!ReelFishingPhase.this.event.level.isServer()) {
            ReelFishingPhase.this.event
               .level
               .entityManager
               .addTrail(
                  this.line = new FishingTrail(
                     ReelFishingPhase.this.event.getMob(), ReelFishingPhase.this.event.level, this.hook, ReelFishingPhase.this.event.fishingRod
                  )
               );
         }
      }

      public void tickMovement() {
         if (this.line != null) {
            this.line.update();
         }

         if (!this.isOver && this.hook.removed()) {
            if (!ReelFishingPhase.this.event.isClient() && !this.spawnedItem) {
               this.spawnItem();
            }

            if (this.line != null && !this.line.isRemoved()) {
               this.line.remove();
            }

            this.isOver = true;
         }
      }

      public void spawnItem() {
         if (this.caught != null) {
            ReelFishingPhase.this.event.getFishingMob().giveCaughtItem(ReelFishingPhase.this.event, this.caught);
            if (ReelFishingPhase.this.event.getMob().isPlayer) {
               ServerClient client = ((PlayerMob)ReelFishingPhase.this.event.getMob()).getServerClient();
               if (client != null) {
                  client.newStats.fish_caught.increment(1);
               }
            }

            this.spawnedItem = true;
         }
      }

      public void remove() {
         if (this.line != null && !this.line.isRemoved()) {
            this.line.remove();
         }

         if (!this.hook.removed()) {
            this.hook.remove();
         }
      }
   }
}
