package necesse.gfx.forms.presets.debug;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import necesse.engine.GameLog;
import necesse.engine.GlobalData;
import necesse.engine.expeditions.SettlerExpedition;
import necesse.engine.gameTool.GameToolManager;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.client.ClientLevelLoading;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.network.server.ServerCreationSettings;
import necesse.engine.platforms.Platform;
import necesse.engine.platforms.PlatformManager;
import necesse.engine.quest.KillMobsQuest;
import necesse.engine.quest.KillMobsTitleQuest;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ExpeditionMissionRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.SettlerRegistry;
import necesse.engine.registries.StoryObjectiveRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.storyObjectives.StoryObjective;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.HashMapArrayList;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.ObjectValue;
import necesse.engine.util.PointHashSet;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.engine.world.SpawnTileFinder;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.BasicSettlementRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.SettlementRaidLevelEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.HumanSettlerMob;
import necesse.entity.mobs.friendly.human.humanShop.ExplorerHumanMob;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.entity.particle.RandomSpinningLightParticle;
import necesse.entity.particle.fireworks.FireworksExplosion;
import necesse.entity.particle.fireworks.FireworksPath;
import necesse.entity.particle.fireworks.FireworksRocketParticle;
import necesse.gfx.GameResources;
import necesse.gfx.TableContentPrint;
import necesse.gfx.camera.MainGameCamera;
import necesse.gfx.forms.FormManager;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.floatMenu.FloatMenu;
import necesse.gfx.forms.floatMenu.SelectionFloatMenu;
import necesse.gfx.forms.presets.debug.tools.BezierCurveTestGameTool;
import necesse.gfx.forms.presets.debug.tools.CastRayGameTool;
import necesse.gfx.forms.presets.debug.tools.ChaikinSmoothTestGameTool;
import necesse.gfx.forms.presets.debug.tools.CollisionPointGameTool;
import necesse.gfx.forms.presets.debug.tools.CollisionRectangleGameTool;
import necesse.gfx.forms.presets.debug.tools.ColorTweenTestGameTool;
import necesse.gfx.forms.presets.debug.tools.DebugGameTool;
import necesse.gfx.forms.presets.debug.tools.DelaunayTriangulatorGameTool;
import necesse.gfx.forms.presets.debug.tools.EasingsTestGameTool;
import necesse.gfx.forms.presets.debug.tools.ExpandingPolygonGameTool;
import necesse.gfx.forms.presets.debug.tools.FindClosestHeightGameTool;
import necesse.gfx.forms.presets.debug.tools.GenerationTesterGameTool;
import necesse.gfx.forms.presets.debug.tools.InverseKinematicsGameTool;
import necesse.gfx.forms.presets.debug.tools.LinePathGenerationTestGameTool;
import necesse.gfx.forms.presets.debug.tools.LootTableTestGameTool;
import necesse.gfx.forms.presets.debug.tools.MouseDebugGameTool;
import necesse.gfx.forms.presets.debug.tools.PresetCopyDebugGameTool;
import necesse.gfx.forms.presets.debug.tools.PresetPasteDebugGameTool;
import necesse.gfx.forms.presets.debug.tools.RegionLoaderGameTool;
import necesse.gfx.forms.presets.debug.tools.RegionPathFindGameTool;
import necesse.gfx.forms.presets.debug.tools.RoomAnalyzerGameTool;
import necesse.gfx.forms.presets.debug.tools.SoundTestGameTool;
import necesse.gfx.forms.presets.debug.tools.SurvivorWhipTestGameTool;
import necesse.gfx.forms.presets.debug.tools.TestThoughtBubblesGameTool;
import necesse.gfx.forms.presets.debug.tools.TileInfoGameTool;
import necesse.gfx.forms.presets.debug.tools.TilePathFindGameTool;
import necesse.gfx.forms.presets.debug.tools.TrailTestGameTool;
import necesse.gfx.forms.presets.debug.tools.UpdateBiomeBlendingGameTool;
import necesse.gfx.forms.presets.debug.tools.UpdateLightGameTool;
import necesse.gfx.forms.presets.debug.tools.ZoneSelectorDebugGameTool;
import necesse.gfx.forms.presets.sidebar.ShowPresetRegionSidebarForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.HUD;
import necesse.inventory.InventoryItem;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.RockObject;
import necesse.level.maps.CaveLevel;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.generationModules.GenerationTools;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementRaidLoadout;
import necesse.level.maps.levelData.settlementData.SettlementVisitorSpawner;
import necesse.level.maps.levelData.settlementData.settler.Settler;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;
import necesse.level.maps.regionSystem.Region;
import necesse.level.maps.regionSystem.RegionBoundsExecutor;

public class DebugToolsList {
   public final DebugForm parent;
   private final ArrayList<DebugToolsList.Tool> tools;

   public DebugToolsList(DebugForm parent) {
      this.parent = parent;
      this.tools = new ArrayList<>();
      this.addTool(new PresetCopyDebugGameTool(parent));
      this.addTool(new PresetPasteDebugGameTool(parent));
      this.addSubMenu("Loot table tests", LootTableTestGameTool.getSelectionMenu(parent), true);
      this.addTool(new TileInfoGameTool(parent));

      class SpawnTileResult {
         public final SpawnTileFinder.SpawnTileData tile;
         public final long timeTaken;

         public SpawnTileResult(SpawnTileFinder.SpawnTileData tile, long timeTaken) {
            this.tile = tile;
            this.timeTaken = timeTaken;
         }
      }

      this.addTool("Pause/resume region streaming", () -> {
         ClientLevelLoading.DEBUG_STREAMING_PAUSED = !ClientLevelLoading.DEBUG_STREAMING_PAUSED;
         if (ClientLevelLoading.DEBUG_STREAMING_PAUSED) {
            parent.client.chat.addOrModifyMessage("debugStreamingPaused", "Paused client level region streaming");
         } else {
            parent.client.chat.addOrModifyMessage("debugStreamingPaused", "Resumed client level region streaming");
         }
      });
      this.addTool(new RegionLoaderGameTool(parent));
      this.addTool("Show region bounds", () -> {
         HUD.showRegionBounds = !HUD.showRegionBounds;
         if (HUD.showRegionBounds) {
            parent.client.chat.addOrModifyMessage("debugRegionBounds", "Started showing region bounds");
         } else {
            parent.client.chat.addOrModifyMessage("debugRegionBounds", "Stopped showing region bounds");
         }
      });
      this.addTool("Show preset region bounds", () -> {
         HUD.showWorldPresetRegionBounds = !HUD.showWorldPresetRegionBounds;
         if (HUD.showWorldPresetRegionBounds) {
            parent.client.chat.addOrModifyMessage("debugRegionBounds", "Started showing preset region bounds");
            ShowPresetRegionSidebarForm.searchFilter = "";
            parent.mainGame.formManager.addSidebar(new ShowPresetRegionSidebarForm());
         } else {
            parent.client.chat.addOrModifyMessage("debugRegionBounds", "Stopped showing preset region bounds");
         }
      });
      this.addTool("Clear debug show", () -> {
         HUD.debugShow = HUD.DebugShow.NOTHING;
         parent.client.chat.addOrModifyMessage("debugShow", "Showing " + HUD.debugShow);
      });
      SelectionFloatMenu debugShowMenu = new SelectionFloatMenu(parent);

      for (HUD.DebugShow value : HUD.DebugShow.values()) {
         debugShowMenu.add("Show " + value, () -> {
            HUD.debugShow = value;
            parent.client.chat.addOrModifyMessage("debugShow", "Showing " + HUD.debugShow);
            debugShowMenu.remove();
         });
      }

      this.addSubMenu("Debug show", debugShowMenu, true);
      this.addTool(new UpdateLightGameTool(parent));
      this.addTool(new UpdateBiomeBlendingGameTool(parent));
      this.addTool(new TilePathFindGameTool(parent));
      this.addTool(new RegionPathFindGameTool(parent));
      this.addTool(new GenerationTesterGameTool(parent));
      this.addTool(new RoomAnalyzerGameTool(parent));
      this.addTool(new SoundTestGameTool(parent));
      this.addTool(
         "Update splatting",
         () -> {
            Level level = parent.client.getLevel();
            MainGameCamera camera = parent.mainGame.getCamera();
            int startTileX = GameMath.getTileCoordinate(camera.getX()) - 1;
            int startTileY = GameMath.getTileCoordinate(camera.getY()) - 1;
            int endTileX = GameMath.getTileCoordinate(camera.getX() + camera.getWidth()) + 1;
            int endTileY = GameMath.getTileCoordinate(camera.getY() + camera.getHeight()) + 1;
            new RegionBoundsExecutor(level.regionManager, startTileX, startTileY, endTileX, endTileY, false)
               .runCoordinates((region, regionTileX, regionTileY) -> region.splattingLayer.updateSplattingByRegion(regionTileX, regionTileY));
         }
      );
      this.addTool("Change music", SoundManager::forceChangeMusic);
      if (GlobalData.isDevMode()) {
         this.addTool("Apply minimum platform display", () -> PlatformManager.getPlatform().applyDefaultMinimumSupportedPlatformSpecs());
         this.addTool("Reset stats and achievements", () -> {
            GlobalData.resetStatsAndAchievements();
            Platform.getStatsProvider().resetStatsAndAchievements(true);
            Server server = this.getServer();
            if (server != null) {
               server.getLocalServerClient().resetStats();
            }
         });
      }

      if (this.getServer() != null) {
         this.addTool("Spawn settlement visitor", () -> {
            ServerSettlementData settlement = this.getServerSettlementAtPlayer();
            if (settlement != null) {
               if (settlement.spawnNextVisitor()) {
                  parent.client.chat.addMessage("Spawned visitor");
               } else {
                  parent.client.chat.addMessage("Could not spawn visitor");
               }
            } else {
               parent.client.chat.addMessage("Must be singleplayer or host and at a settlement");
            }
         });
         SelectionFloatMenu raidDifficulty = new SelectionFloatMenu(parent);

         for (int i = 50; i <= 150; i += 5) {
            float mod = i / 100.0F;
            raidDifficulty.add(i + "%", () -> {
               ServerSettlementData settlement = this.getServerSettlementAtPlayer();
               if (settlement != null) {
                  settlement.setRaidDifficultyMod(mod);
                  parent.client.chat.addMessage("Set raid difficulty modifier to " + settlement.getNextRaidDifficultyMod());
               } else {
                  parent.client.chat.addMessage("Must be singleplayer or host and at a settlement");
               }

               raidDifficulty.remove();
            });
         }

         this.addSubMenu("Set raid difficulty", raidDifficulty, true);
         SelectionFloatMenu raidDirections = new SelectionFloatMenu(parent);

         for (SettlementRaidLevelEvent.RaidDir direction : SettlementRaidLevelEvent.RaidDir.values()) {
            raidDirections.add(direction.displayName.translate(), () -> {
               ServerSettlementData settlement = this.getServerSettlementAtPlayer();
               if (settlement != null) {
                  if (!settlement.spawnRaid(direction, false)) {
                     parent.client.chat.addMessage("Could not spawn raid");
                  }
               } else {
                  parent.client.chat.addMessage("Must be singleplayer or host and at a settlement");
               }

               raidDirections.remove();
            });
         }

         raidDirections.add("Random", () -> {
            ServerSettlementData settlement = this.getServerSettlementAtPlayer();
            if (settlement != null) {
               SettlementRaidLevelEvent.RaidDir direction = GameRandom.globalRandom.getOneOf(SettlementRaidLevelEvent.RaidDir.values());
               if (!settlement.spawnRaid(direction, false)) {
                  parent.client.chat.addMessage("Could not spawn raid");
               }
            } else {
               parent.client.chat.addMessage("Must be singleplayer or host and at a settlement");
            }

            raidDirections.remove();
         });
         this.addSubMenu("Spawn raid", raidDirections, true);
         this.addTool("End raid", () -> {
            ServerClient client = this.getLocalClient();
            if (client != null) {
               Level level = client.getLevel();
               int playerRegionX = level.regionManager.getRegionCoordByTile(client.playerMob.getTileX());
               int playerRegionY = level.regionManager.getRegionCoordByTile(client.playerMob.getTileY());
               boolean found = false;

               for (LevelEvent event : level.entityManager.events.regionList.getInRegion(playerRegionX, playerRegionY)) {
                  if (event instanceof SettlementRaidLevelEvent) {
                     event.over();
                     found = true;
                  }
               }

               if (found) {
                  parent.client.chat.addMessage("Ended raid events at your location");
               } else {
                  parent.client.chat.addMessage("Could not find any raid events at your location");
               }
            }
         });
         this.addTool(
            "Generate raider loadouts",
            () -> {
               ServerSettlementData settlement = this.getServerSettlementAtPlayer();
               if (settlement != null) {
                  ArrayList<SettlementRaidLoadout> loadouts = BasicSettlementRaidLevelEvent.getRaidDebugLoadouts(
                     settlement, "humanraider", Integer.MAX_VALUE, true
                  );
                  System.out.println("FOUND LOADOUTS: " + loadouts.size());

                  for (SettlementRaidLoadout loadout : loadouts) {
                     System.out.println(loadout.getDebugString());
                  }
               } else {
                  parent.client.chat.addMessage("Must be singleplayer or host and at a settlement");
               }
            }
         );
         this.addTool(
            "Start test quest",
            () -> {
               KillMobsQuest quest = new KillMobsTitleQuest(
                  new StaticMessage("TEST QUEST"), new KillMobsQuest.KillObjective("zombie", 5), new KillMobsQuest.KillObjective("zombiearcher", 2)
               );
               this.getServer().world.getQuests().addQuest(quest, true);
               quest.makeActiveFor(this.getServer(), this.getServer().getLocalServerClient());
            }
         );
         this.addTool("Clear server quests", () -> this.getServer().world.getQuests().removeAll());
         this.addTool("Complete and claim current objective", () -> {
            ServerSettlementData settlement = this.getServerSettlementAtPlayer();
            ServerClient localClient = this.getLocalClient();
            if (settlement != null && localClient != null) {
               StoryObjective currentObjective = localClient.storyManager.getCurrentObjective();
               if (currentObjective != null) {
                  if (!currentObjective.isCompleted()) {
                     currentObjective.markCompleted();
                  }

                  currentObjective.claimRewards(settlement);
               }
            } else {
               parent.client.chat.addMessage("Must be singleplayer or host and at a settlement");
            }
         });
         SelectionFloatMenu clientStoryObjectivesMenu = new SelectionFloatMenu(parent);
         SelectionFloatMenu settlementStoryObjectivesMenu = new SelectionFloatMenu(parent);

         for (String objectiveStringID : StoryObjectiveRegistry.getSortedStringIDs()) {
            clientStoryObjectivesMenu.add(objectiveStringID, () -> {
               ServerClient localClient = this.getLocalClient();
               if (localClient != null) {
                  localClient.storyManager.debugSetCurrentStoryObjective(objectiveStringID);
                  parent.client.storyManager.debugSetCurrentStoryObjective(objectiveStringID);
                  localClient.storyManager.forceUpdateSettlements();
                  parent.client.chat.addMessage("Reset client story objectives");
               } else {
                  parent.client.chat.addMessage("Must be singleplayer or host");
               }

               clientStoryObjectivesMenu.remove();
            });
            settlementStoryObjectivesMenu.add(objectiveStringID, () -> {
               ServerSettlementData settlement = this.getServerSettlementAtPlayer();
               if (settlement != null) {
                  settlement.storyObjectives.debugSetCurrentStoryObjective(objectiveStringID);
                  parent.client.chat.addMessage("Set objective completed to " + objectiveStringID);
               } else {
                  parent.client.chat.addMessage("Must be singleplayer or host and at a settlement");
               }

               settlementStoryObjectivesMenu.remove();
            });
         }

         clientStoryObjectivesMenu.add("All", () -> {
            ServerClient localClient = this.getLocalClient();
            if (localClient != null) {
               localClient.storyManager.debugSetCurrentStoryObjective(null);
               parent.client.storyManager.debugSetCurrentStoryObjective(null);
               parent.client.chat.addMessage("Reset client story objectives");
            } else {
               parent.client.chat.addMessage("Must be singleplayer or host");
            }

            clientStoryObjectivesMenu.remove();
         });
         settlementStoryObjectivesMenu.add("All", () -> {
            ServerSettlementData settlement = this.getServerSettlementAtPlayer();
            ServerClient localClient = this.getLocalClient();
            if (settlement != null && localClient != null) {
               settlement.storyObjectives.debugSetCurrentStoryObjective(null);
               parent.client.chat.addMessage("Reset settlement story objectives");
            } else {
               parent.client.chat.addMessage("Must be singleplayer or host and at a settlement");
            }

            settlementStoryObjectivesMenu.remove();
         });
         this.addSubMenu("Set client story objectives", clientStoryObjectivesMenu, true);
         this.addSubMenu("Set settlement story objectives", settlementStoryObjectivesMenu, true);
         this.addTool(
            new MouseDebugGameTool(parent, "Debug mob AI") {
               @Override
               public void init() {
                  this.onLeftClick(
                     e -> {
                        Level serverLevel = DebugToolsList.this.getServerLevel();
                        if (serverLevel != null) {
                           int mouseX = this.getMouseX();
                           int mouseY = this.getMouseY();

                           for (Mob mob : this.parent
                              .client
                              .getLevel()
                              .entityManager
                              .mobs
                              .getInRegionRangeByTile(this.getMouseTileX(), this.getMouseTileY(), 1)) {
                              if (mob.getSelectBox().contains(mouseX, mouseY)) {
                                 Mob serverMob = GameUtils.getLevelMob(mob.getUniqueID(), serverLevel);
                                 if (serverMob != null) {
                                    this.parent
                                       .getManager()
                                       .addComponent(new DebugMobAIForm(mob.getLevel(), serverMob, c -> this.parent.getManager().removeComponent(c)));
                                 } else {
                                    this.parent.client.chat.addMessage("Could not find mob on server");
                                 }
                                 break;
                              }
                           }
                        } else {
                           this.parent.client.chat.addMessage("Must be singleplayer or host");
                        }

                        return true;
                     },
                     "Select mob"
                  );
               }
            }
         );
      }

      this.addTool(new MouseDebugGameTool(parent, "Play test sound") {
         @Override
         public void init() {
            this.onLeftClick(e -> {
               SoundManager.playSound(GameResources.tap, SoundEffect.effect(this.getMouseX(), this.getMouseY()));
               return true;
            }, "Play tap sound at position");
            this.onRightClick(e -> {
               SoundManager.playSound(GameResources.roar, SoundEffect.effect(this.getMouseX(), this.getMouseY()));
               return true;
            }, "Play roar sound at position");
         }
      });
      this.addTool(new CollisionPointGameTool(parent, "Collision point"));
      this.addTool(new CollisionRectangleGameTool(parent, "Collision rectangle"));
      this.addTool(new CastRayGameTool(parent, "Cast ray"));
      this.addTool(new LinePathGenerationTestGameTool(parent, "Line path generation"));
      this.addTool(new FindClosestHeightGameTool(parent, "Find closest height"));
      this.addTool(new ExpandingPolygonGameTool(parent, "Expanding polygon"));
      this.addTool(
         new MouseDebugGameTool(parent, "Test particle") {
            @Override
            public void init() {
               this.onMouseMove(
                  e -> {
                     GameWindow window = WindowManager.getWindow();
                     if (window.isKeyDown(-100)) {
                        for (int i = 0; i < 50; i++) {
                           this.getLevel()
                              .entityManager
                              .addParticle(this.getMouseX(), this.getMouseY(), Particle.GType.CRITICAL)
                              .movesConstant((float)GameRandom.globalRandom.nextGaussian() * 50.0F, (float)GameRandom.globalRandom.nextGaussian() * 50.0F)
                              .color(Color.getHSBColor(GameRandom.globalRandom.nextFloat(), 1.0F, 1.0F))
                              .rotates(100.0F, 200.0F)
                              .givesLight(300.0F, 1.0F)
                              .lifeTime(1000);
                        }

                        return true;
                     } else {
                        if (!window.isKeyDown(-99)) {
                           return false;
                        }

                        for (int i = 0; i < 50; i++) {
                           this.getLevel()
                              .entityManager
                              .addParticle(
                                 new RandomSpinningLightParticle(
                                    this.getLevel(),
                                    Color.getHSBColor(GameRandom.globalRandom.nextFloat(), 1.0F, 1.0F),
                                    this.getMouseX(),
                                    this.getMouseY(),
                                    (float)GameRandom.globalRandom.nextGaussian() * 50.0F,
                                    (float)GameRandom.globalRandom.nextGaussian() * 50.0F,
                                    0,
                                    1000
                                 ),
                                 Particle.GType.CRITICAL
                              );
                        }

                        return true;
                     }
                  }
               );
               this.onLeftClick(
                  e -> {
                     for (int i = 0; i < 50; i++) {
                        this.getLevel()
                           .entityManager
                           .addParticle(this.getMouseX(), this.getMouseY(), Particle.GType.CRITICAL)
                           .movesConstant((float)GameRandom.globalRandom.nextGaussian() * 50.0F, (float)GameRandom.globalRandom.nextGaussian() * 50.0F)
                           .color(Color.getHSBColor(GameRandom.globalRandom.nextFloat(), 1.0F, 1.0F))
                           .rotates(100.0F, 200.0F)
                           .givesLight(300.0F, 1.0F)
                           .lifeTime(1000);
                     }

                     return true;
                  },
                  "Spawn new particles"
               );
               this.onRightClick(
                  e -> {
                     for (int i = 0; i < 50; i++) {
                        this.getLevel()
                           .entityManager
                           .addParticle(
                              new RandomSpinningLightParticle(
                                 this.getLevel(),
                                 Color.getHSBColor(GameRandom.globalRandom.nextFloat(), 1.0F, 1.0F),
                                 this.getMouseX(),
                                 this.getMouseY(),
                                 (float)GameRandom.globalRandom.nextGaussian() * 50.0F,
                                 (float)GameRandom.globalRandom.nextGaussian() * 50.0F,
                                 0,
                                 1000
                              ),
                              Particle.GType.CRITICAL
                           );
                     }

                     return true;
                  },
                  "Spawn old particles"
               );
            }
         }
      );
      SelectionFloatMenu newSettlerMenu = new SelectionFloatMenu(parent);
      SelectionFloatMenu newRecruitMenu = new SelectionFloatMenu(parent);

      for (Settler settler : SettlerRegistry.getSettlers()) {
         newSettlerMenu.add(settler.getGenericMobName(), () -> {
            ServerSettlementData settlement = this.getServerSettlementAtPlayer();
            if (settlement != null) {
               SettlerMob mob = settler.getNewSettlerMob(settlement);
               if (mob != null) {
                  mob.setSettlerSeed(GameRandom.globalRandom.nextInt(), true);
                  settlement.getLevel().entityManager.mobs.add(mob.getMob());
                  settlement.moveIn(new LevelSettler(settlement, mob));
               } else {
                  parent.client.chat.addMessage("Could not spawn settler");
               }
            } else {
               parent.client.chat.addMessage("Must be singleplayer or host and at a settlement");
            }

            newSettlerMenu.remove();
         });
         newRecruitMenu.add(settler.getGenericMobName(), () -> {
            ServerSettlementData settlement = this.getServerSettlementAtPlayer();
            if (settlement != null) {
               SettlerMob mob = settler.getNewSettlerMob(settlement);
               if (mob != null) {
                  mob.setSettlerSeed(GameRandom.globalRandom.nextInt(), true);
                  HumanMob humanMob = (HumanMob)mob.getMob();
                  settlement.spawnVisitor(new SettlementVisitorSpawner(ServerSettlementData.visitorRecruitsOdds, humanMob));
               } else {
                  parent.client.chat.addMessage("Could not spawn settler");
               }
            } else {
               parent.client.chat.addMessage("Must be singleplayer or host and at a settlement");
            }

            newRecruitMenu.remove();
         });
      }

      this.addSubMenu("Spawn new settler", newSettlerMenu, true);
      this.addSubMenu("Spawn new recruit", newRecruitMenu, true);
      this.addTool(
         new MouseDebugGameTool(parent, "Fireworks") {
            @Override
            public void init() {
               this.onLeftClick(
                  e -> {
                     FireworksExplosion explosion = new FireworksExplosion(null);
                     GameRandom.globalRandom
                        .runOneOf(
                           () -> explosion.colorGetter = (p, progress, random) -> Color.getHSBColor(random.nextFloat(), 1.0F, 1.0F),
                           () -> explosion.colorGetter = (p, progress, random) -> Color.getHSBColor(random.nextFloat(), 1.0F, 1.0F),
                           () -> explosion.colorGetter = (p, progress, random) -> ParticleOption.randomFlameColor(random),
                           () -> explosion.colorGetter = (p, progress, random) -> ParticleOption.randomFlameColor(random, 0.0F),
                           () -> explosion.colorGetter = (p, progress, random) -> ParticleOption.randomFlameColor(random, 110.0F),
                           () -> explosion.colorGetter = (p, progress, random) -> ParticleOption.randomFlameColor(random, 240.0F),
                           () -> explosion.colorGetter = (p, progress, random) -> ParticleOption.randomFlameColor(random, 310.0F)
                        );
                     GameRandom.globalRandom.runOneOf(() -> explosion.popChance = 0.5F, () -> explosion.popChance = 0.0F);
                     GameRandom.globalRandom.runOneOf(() -> explosion.trailChance = 0.5F, () -> explosion.trailChance = 0.2F);
                     GameRandom.globalRandom
                        .runOneOf(
                           () -> explosion.pathGetter = FireworksPath.sphere(GameRandom.globalRandom.getIntBetween(150, 250)),
                           () -> explosion.pathGetter = FireworksPath.shape(
                              FireworksPath.star, GameRandom.globalRandom.getIntBetween(150, 250), random -> Math.min(1.0F, random.nextFloat() * 1.2F)
                           ),
                           () -> explosion.pathGetter = FireworksPath.shape(
                              FireworksPath.heart, GameRandom.globalRandom.getIntBetween(150, 250), random -> Math.min(1.0F, random.nextFloat() * 1.2F)
                           ),
                           () -> {
                              explosion.pathGetter = FireworksPath.disc(GameRandom.globalRandom.getIntBetween(150, 250));
                              explosion.minSize = 20;
                              explosion.maxSize = 30;
                              explosion.trailSize = 15.0F;
                              explosion.trailFadeTime = 1000;
                              explosion.particles = 50;
                              explosion.trailChance = 1.0F;
                           },
                           () -> explosion.pathGetter = FireworksPath.splash(
                              GameRandom.globalRandom.getIntBetween(0, 360), GameRandom.globalRandom.getIntBetween(150, 250)
                           )
                        );
                     this.getLevel()
                        .entityManager
                        .addParticle(
                           new FireworksRocketParticle(
                              this.getLevel(),
                              this.getMouseX(),
                              this.getMouseY(),
                              1200L,
                              GameRandom.globalRandom.getIntBetween(300, 400),
                              explosion,
                              GameRandom.globalRandom
                           ),
                           Particle.GType.CRITICAL
                        );
                     return true;
                  },
                  "Spawn fireworks"
               );
            }
         }
      );
      this.addTool(new InverseKinematicsGameTool(parent));
      this.addTool(new DelaunayTriangulatorGameTool(parent));
      this.addTool(new ZoneSelectorDebugGameTool(parent, "Zone selector"));
      this.addTool("Print room sizes", () -> this.getLevel().regionManager.debugPrintRoomSizes());
      this.addTool("Toggle controller boxes", () -> FormManager.drawControllerFocusBoxes = !FormManager.drawControllerFocusBoxes);
      this.addTool("Toggle controller areas", () -> FormManager.drawControllerAreaBoxes = !FormManager.drawControllerAreaBoxes);
      this.addTool(new MouseDebugGameTool(parent, "Test shockwaves") {
         @Override
         public void init() {
            this.onLeftClick(e -> {
               this.parent.client.addShockwaveEffect(this.getMouseX(), this.getMouseY(), 1000.0F, 100.0F, 500.0F, 100.0F, 200);
               return true;
            }, "Spawn shockwave");
         }
      });
      this.addTool(new MouseDebugGameTool(parent, "Test heat distortion") {
         @Override
         public void init() {
            this.onLeftClick(e -> {
               this.parent.client.addHeatDistortEffect(this.getMouseX(), this.getMouseY(), 100.0F, 100.0F, 1.0F, 5000);
               return true;
            }, "Spawn heat distortion");
         }
      });
      this.addTool(new TestThoughtBubblesGameTool(parent));
      if (GlobalData.isDevMode()) {
         this.addTool(new TrailTestGameTool(parent, "Trail test"));
         this.addTool(new ChaikinSmoothTestGameTool(parent, "Smooth test"));
         this.addTool(new BezierCurveTestGameTool(parent, "Bézier Curve test"));
         this.addTool("Test generation", () -> {
            ExecutorService executor = Executors.newFixedThreadPool(4, r -> new Thread(r, "test-generation"));
            Biome biome = this.getLevel().baseBiome;
            GameRandom random = new GameRandom(1234567890L);
            WorldEntity worldEntity = WorldEntity.getDebugWorldEntity();
            HashMap<Integer, Integer> tiles = new HashMap<>();
            HashMap<Integer, Integer> objects = new HashMap<>();
            HashMap<Integer, Integer> items = new HashMap<>();
            int samples = 50;

            for (int i = 0; i < samples; i++) {
               GameRandom uniqueRandom = random.nextSeeded(i);
               executor.submit(() -> {
                  Level level = biome.getNewLevel(uniqueRandom.nextInt(), uniqueRandom.nextInt(), this.getLevel().getIslandDimension(), null, worldEntity);
                  synchronized (executor) {
                     GenerationTools.collectLevelContent(level, tiles, objects, items);
                  }
               });
            }

            try {
               executor.shutdown();
               executor.awaitTermination(1L, TimeUnit.HOURS);
            } catch (InterruptedException e) {
               e.printStackTrace();
            }

            System.out.println("Biome " + biome.getStringID() + " at dimension " + this.getLevel().getIslandDimension() + " over " + samples + " samples");
            GenerationTools.printLevelContent(tiles, objects, items);
         });
         this.addTool(
            "Test ores",
            () -> {
               ArrayList<Biome> biomes = BiomeRegistry.getBiomes()
                  .stream()
                  .filter(b -> b.getBiomeGenerationWeight() > 0.0F)
                  .collect(Collectors.toCollection(ArrayList::new));
               WorldEntity worldEntity = WorldEntity.getDebugWorldEntity();
               int samples = 50;
               ExecutorService executor = Executors.newFixedThreadPool(4, r -> new Thread(r, "test-ores"));
               LinkedList<Future<ArrayList<ObjectValue<String, HashMap<Integer, Integer>>>>> futures = new LinkedList<>();

               for (Biome biome : biomes) {
                  futures.add(
                     executor.submit(
                        () -> {
                           GameRandom random = new GameRandom(1234567890L);
                           HashMap<Integer, Integer> caveObjects = new HashMap<>();
                           HashMap<Integer, Integer> deepCaveObjects = new HashMap<>();
                           ExecutorService generationExecutor = Executors.newFixedThreadPool(4, r -> new Thread(r, biome.getStringID() + "-generator"));

                           for (int i = 0; i < samples; i++) {
                              GameRandom uniqueRandom = random.nextSeeded(i);
                              generationExecutor.submit(() -> {
                                 Level caveLevel = biome.getNewCaveLevel(uniqueRandom.nextInt(), uniqueRandom.nextInt(), -1, null, worldEntity);
                                 Level deepCaveLevel = biome.getNewDeepCaveLevel(uniqueRandom.nextInt(), uniqueRandom.nextInt(), -2, null, worldEntity);
                                 synchronized (generationExecutor) {
                                    GenerationTools.collectLevelContent(caveLevel, null, caveObjects, null);
                                    GenerationTools.collectLevelContent(deepCaveLevel, null, deepCaveObjects, null);
                                 }
                              });
                           }

                           generationExecutor.shutdown();
                           generationExecutor.awaitTermination(1L, TimeUnit.HOURS);

                           for (int objectID : Stream.of(caveObjects.keySet().stream(), deepCaveObjects.keySet().stream())
                              .flatMap(s -> (Stream<? extends Integer>)s)
                              .filter(objectIDx -> {
                                 GameObject object = ObjectRegistry.getObject(objectIDx);
                                 return !(object instanceof RockObject);
                              })
                              .collect(Collectors.toList())) {
                              caveObjects.remove(objectID);
                              deepCaveObjects.remove(objectID);
                           }

                           return new ArrayList<>(
                              Arrays.asList(
                                 new ObjectValue<>("Biome " + biome.getStringID() + " cave:", caveObjects),
                                 new ObjectValue<>("Biome " + biome.getStringID() + " deep cave:", deepCaveObjects)
                              )
                           );
                        }
                     )
                  );
               }

               for (Future<ArrayList<ObjectValue<String, HashMap<Integer, Integer>>>> future : futures) {
                  try {
                     ArrayList<ObjectValue<String, HashMap<Integer, Integer>>> results = future.get();
                     Predicate<GameObject> filter = o -> o instanceof RockObject;

                     for (ObjectValue<String, HashMap<Integer, Integer>> result : results) {
                        System.out.println(result.object);
                        GenerationTools.printLevelContent(null, null, result.value, filter, null, null);
                     }
                  } catch (InterruptedException | ExecutionException e) {
                     e.printStackTrace();
                  }
               }

               executor.shutdown();
            }
         );
         this.addTool(
            "Test one world ores",
            () -> {
               HashSet<Integer> biomeIDs = BiomeRegistry.getBiomes()
                  .stream()
                  .filter(b -> b.getBiomeGenerationWeight() > 0.0F)
                  .map(Biome::getID)
                  .collect(Collectors.toCollection(HashSet::new));
               WorldEntity worldEntity = WorldEntity.getDebugWorldEntity();
               int regionDimensions = 2048;
               int regionDimensionsPerTask = 64;
               int regionTasksDimensions = (int)Math.ceil((double)regionDimensions / regionDimensionsPerTask);
               AtomicInteger completeRegionsCounter = new AtomicInteger(0);
               Object lock = new Object();
               Predicate<GameObject> filter = o -> o instanceof RockObject;
               ExecutorService executor = Executors.newFixedThreadPool(4, r -> new Thread(r, "test-one-world-ores"));
               HashMap<Integer, Integer> combinedBiomesFound = new HashMap<>();
               HashMap<Integer, HashMap<Integer, Integer>> combinedBiomeObjects = new HashMap<>();

               for (int taskX = 0; taskX < regionTasksDimensions; taskX++) {
                  int startRegionX = taskX * regionDimensionsPerTask;

                  for (int taskY = 0; taskY < regionTasksDimensions; taskY++) {
                     int startRegionY = taskY * regionDimensionsPerTask;
                     executor.submit(
                        () -> {
                           HashMap<Integer, Integer> biomesFound = new HashMap<>();
                           HashMap<Integer, HashMap<Integer, Integer>> biomeObjects = new HashMap<>();
                           CaveLevel caveLevel = new CaveLevel(LevelIdentifier.CAVE_IDENTIFIER, regionDimensions, regionDimensions, worldEntity, 1234567890);
                           GenerationTools.generateAndCollectLevelContentByBiome(
                              caveLevel,
                              startRegionX,
                              startRegionY,
                              regionDimensionsPerTask,
                              regionDimensionsPerTask,
                              biomesFound,
                              null,
                              biomeIDx -> !biomeIDs.contains(biomeIDx) ? null : biomeObjects.compute(biomeIDx, (key, last) -> {
                                 if (last == null) {
                                    last = new HashMap<>();
                                 }

                                 return last;
                              }),
                              null
                           );
                           synchronized (lock) {
                              biomesFound.forEach((biomeIDx, count) -> combinedBiomesFound.merge(biomeIDx, count, Integer::sum));
                              biomeObjects.forEach((biomeIDx, objectsx) -> combinedBiomeObjects.merge(biomeIDx, objectsx, (last, current) -> {
                                 current.forEach((objectID, count) -> {
                                    GameObject object = ObjectRegistry.getObject(objectID);
                                    if (filter.test(object)) {
                                       last.merge(objectID, count, Integer::sum);
                                    }
                                 });
                                 return last;
                              }));
                              int lastPercentProgress = (int)((double)completeRegionsCounter.get() / (regionDimensions * regionDimensions) * 100.0);
                              completeRegionsCounter.addAndGet(regionDimensionsPerTask * regionDimensionsPerTask);
                              int nextPercentProgress = (int)((double)completeRegionsCounter.get() / (regionDimensions * regionDimensions) * 100.0);
                              if (nextPercentProgress > lastPercentProgress) {
                                 System.out
                                    .println(
                                       "Completed "
                                          + completeRegionsCounter.get()
                                          + " regions out of "
                                          + regionDimensions * regionDimensions
                                          + " ("
                                          + nextPercentProgress
                                          + "%)"
                                    );
                              }
                           }
                        }
                     );
                  }
               }

               executor.shutdown();

               try {
                  executor.awaitTermination(1L, TimeUnit.HOURS);
               } catch (InterruptedException e) {
                  e.printStackTrace();
               }

               int totalBiomesFound = combinedBiomesFound.values().stream().reduce(0, Integer::sum);

               for (Entry<Integer, HashMap<Integer, Integer>> entry : combinedBiomeObjects.entrySet()) {
                  int biomeID = entry.getKey();
                  String biomeStringID = BiomeRegistry.getBiomeStringID(biomeID);
                  HashMap<Integer, Integer> objects = entry.getValue();
                  int totalBiomeTilesFound = combinedBiomesFound.getOrDefault(biomeID, 0);
                  float percentOfAllBiomes = GameMath.toDecimals((float)totalBiomeTilesFound / totalBiomesFound * 100.0F, 2);
                  System.out.println("Found " + totalBiomeTilesFound + " tiles of " + biomeStringID + " biome (" + percentOfAllBiomes + "% of all tiles)");
                  System.out.println("Biome " + biomeStringID + " cave:");
                  GenerationTools.printLevelContent(null, null, objects, filter, null, null);
               }
            }
         );
         this.addTool(
            "Test explorer expeditions",
            () -> {
               Level serverLevel = this.getServerLevel();
               if (serverLevel == null) {
                  parent.client.chat.addMessage("Must be singleplayer or host");
               } else {
                  ServerClient localClient = this.getLocalClient();
                  ServerSettlementData settlement = SettlementsWorldData.getSettlementsData(localClient.getServer())
                     .getServerDataAtTile(serverLevel.getIdentifier(), localClient.playerMob.getTileX(), localClient.playerMob.getTileY());
                  if (settlement == null) {
                     System.out.println("Could not find settlement data");
                  } else {
                     System.out.println("Using settlement: " + settlement.getSettlementName().translate());
                  }

                  ExplorerHumanMob explorer = serverLevel.entityManager
                     .mobs
                     .streamArea(localClient.playerMob.x, localClient.playerMob.y, 9600)
                     .filter(m -> m instanceof ExplorerHumanMob)
                     .map(m -> (ExplorerHumanMob)m)
                     .filter(HumanSettlerMob::isSettler)
                     .findBestDistance(0, Comparator.comparingDouble(m -> m.getDistance(localClient.playerMob)))
                     .orElse(null);
                  if (explorer == null) {
                     System.out.println("Could not find explorer settler");
                  } else {
                     System.out.println("Using explorer: " + explorer.getDisplayName());
                  }

                  for (int expeditionID : ExpeditionMissionRegistry.explorerExpeditionIDs) {
                     SettlerExpedition expedition = ExpeditionMissionRegistry.getExpedition(expeditionID);
                     int runs = 100;
                     float totalValue = 0.0F;

                     for (int i = 0; i < 100; i++) {
                        List<InventoryItem> rewards = expedition.getRewardItems(settlement, explorer);
                        totalValue += rewards.stream().reduce(0.0F, (last, item) -> last + item.getBrokerValue(), Float::sum);
                     }

                     int baseCost = expedition.getBaseCost(settlement);
                     float averageValue = GameMath.toDecimals(totalValue / runs, 2);
                     float increase = GameMath.toDecimals((averageValue - baseCost) / baseCost * 100.0F, 2);
                     System.out
                        .println(
                           expedition.getDisplayName().translate()
                              + " average "
                              + averageValue
                              + " broker value which is "
                              + increase
                              + "% above base cost of "
                              + baseCost
                        );
                  }
               }
            }
         );
         this.addTool(new EasingsTestGameTool(parent, "Test easings"));
         this.addTool(new ColorTweenTestGameTool(parent, "Test color tweens"));
         this.addTool(new SurvivorWhipTestGameTool(parent, "Test whip"));
         this.addTool("One World tests", () -> {
            final FormManager manager = parent.getManager();
            manager.addComponent(new OneWorldTestForm(parent.client) {
               @Override
               public void removeForm() {
                  manager.removeComponent(this);
               }
            });
         });
         this.addTool(
            "Test biome sizes",
            () -> {
               int testRegionSize = 2048;
               int printProgressEverySeconds = 5;
               int totalRegions = testRegionSize * testRegionSize;
               Server localServer = parent.client.getLocalServer();
               String worldSeed;
               if (localServer != null) {
                  worldSeed = localServer.world.worldEntity.worldSeed;
               } else {
                  worldSeed = ServerCreationSettings.getNewRandomSpawnSeed();
               }

               BiomeGeneratorStack generatorStack = new BiomeGeneratorStack(worldSeed.hashCode());
               System.out.println("Testing biome sizes and distribution with seed: " + worldSeed);
               int testStartRegionCoord = -testRegionSize / 2;
               Thread thread = new Thread(
                  () -> {
                     PointHashSet closedRegions = new PointHashSet();
                     HashMap<Integer, Integer> biomeCounts = new HashMap<>();
                     HashMapArrayList<Integer, Integer> biomeSizes = new HashMapArrayList<>();
                     AtomicInteger regionsProcessed = new AtomicInteger(0);
                     int skippedBiomes = 0;
                     int totalProcessed = 0;
                     long lastPrintTime = System.currentTimeMillis();
                     Rectangle regions = new Rectangle(testStartRegionCoord, testStartRegionCoord, testRegionSize, testRegionSize);

                     for (int regionX = regions.x; regionX < regions.x + regions.width; regionX++) {
                        for (int regionY = regions.y; regionY < regions.y + regions.height; regionY++) {
                           totalProcessed++;
                           if (closedRegions.contains(regionX, regionY)) {
                              skippedBiomes++;
                           } else {
                              int startTileX = GameMath.getTileCoordByRegion(regionX) + 8;
                              int startTileY = GameMath.getTileCoordByRegion(regionY) + 8;
                              int biomeID = generatorStack.getLazyBiomeID(startTileX, startTileY);
                              PointHashSet currentClosedRegions = new PointHashSet();
                              LinkedList<Point> openRegions = new LinkedList<>();
                              openRegions.add(new Point(regionX, regionY));
                              int biomeSize = 0;

                              while (!openRegions.isEmpty()) {
                                 Point currentRegion = openRegions.poll();
                                 if (!closedRegions.contains(currentRegion.x, currentRegion.y)) {
                                    int currentTileX = GameMath.getTileCoordByRegion(currentRegion.x) + 8;
                                    int currentTileY = GameMath.getTileCoordByRegion(currentRegion.y) + 8;
                                    int currentBiomeID = generatorStack.getLazyBiomeID(currentTileX, currentTileY);
                                    if (currentBiomeID == biomeID) {
                                       closedRegions.add(currentRegion.x, currentRegion.y);
                                       currentClosedRegions.add(currentRegion.x, currentRegion.y);
                                       biomeCounts.merge(currentBiomeID, 1, Integer::sum);
                                       biomeSize++;

                                       for (Point offset : Level.adjacentGetters) {
                                          int nextRegionX = currentRegion.x + offset.x;
                                          int nextRegionY = currentRegion.y + offset.y;
                                          if (!closedRegions.contains(nextRegionX, nextRegionY) && currentClosedRegions.add(nextRegionX, nextRegionY)) {
                                             openRegions.add(new Point(nextRegionX, nextRegionY));
                                          }
                                       }
                                    }
                                 }
                              }

                              biomeSizes.add(biomeID, biomeSize);
                              regionsProcessed.addAndGet(biomeSize);
                              if (System.currentTimeMillis() - lastPrintTime > printProgressEverySeconds * 1000) {
                                 float percentProgress = GameMath.toDecimals((float)totalProcessed / totalRegions * 100.0F, 2);
                                 System.out.println("Current biome sizes test progress: " + percentProgress + "%");
                                 lastPrintTime = System.currentTimeMillis();
                              }
                           }
                        }
                     }

                     System.out
                        .println("Skipped biomes: " + skippedBiomes + " (" + GameMath.toDecimals((float)skippedBiomes / totalRegions * 100.0F, 2) + "%)");
                     System.out
                        .println(
                           "Regions processed: "
                              + regionsProcessed.get()
                              + " ("
                              + GameMath.toDecimals((float)regionsProcessed.get() / totalRegions * 100.0F, 2)
                              + "%)"
                        );
                     TableContentPrint table = new TableContentPrint(4);
                     TableContentPrint.TableRow startRow = table.newRow();
                     startRow.addColumn("Biome")
                        .addColumn("Regions")
                        .addColumn("Percent")
                        .addColumn("Average size")
                        .addColumn("Bot 10% size")
                        .addColumn("Bot 25% size")
                        .addColumn("Median size")
                        .addColumn("Top 25% size")
                        .addColumn("Top 10% size")
                        .addColumn("Biomes found");

                     for (Entry<Integer, Integer> entry : biomeCounts.entrySet()) {
                        int biomeID = entry.getKey();
                        int count = entry.getValue();
                        String biomeStringID = BiomeRegistry.getBiomeStringID(biomeID);
                        float percent = GameMath.toDecimals((float)count / totalRegions * 100.0F, 2);
                        List<Integer> sizes = biomeSizes.get(biomeID);
                        sizes.sort(Integer::compareTo);
                        long averageSize = (long)sizes.stream().mapToLong(l -> l.intValue()).average().orElse(0.0);
                        long bottom10Percent = sizes.isEmpty() ? 0L : sizes.get((int)(sizes.size() * 0.1)).intValue();
                        long bottom25Percent = sizes.isEmpty() ? 0L : sizes.get((int)(sizes.size() * 0.25)).intValue();
                        long medianSize = sizes.isEmpty() ? 0L : sizes.get(sizes.size() / 2).intValue();
                        long top10Percent = sizes.isEmpty() ? 0L : sizes.get((int)(sizes.size() * 0.9)).intValue();
                        long top25Percent = sizes.isEmpty() ? 0L : sizes.get((int)(sizes.size() * 0.75)).intValue();
                        TableContentPrint.TableRow currentRow = table.newRow();
                        currentRow.addColumn(biomeStringID)
                           .addColumn(count)
                           .addColumn(percent + "%")
                           .addColumn(averageSize)
                           .addColumn(bottom10Percent)
                           .addColumn(bottom25Percent)
                           .addColumn(medianSize)
                           .addColumn(top25Percent)
                           .addColumn(top10Percent)
                           .addColumn(sizes.size());
                     }

                     table.print("\t");
                  },
                  "test-biome-sizes-submitter"
               );
               thread.start();
            }
         );
         this.addTool(
            "Test spawn tile finder",
            () -> {
               int totalTests = 512;
               int runGCEveryTest = 16;
               int printProgressEverySeconds = 5;
               Server localServer = parent.client.getLocalServer();
               if (localServer != null) {
                  System.out.println("Testing spawn tile finder " + totalTests + " times...");
                  ExecutorService executor = Executors.newFixedThreadPool(4, r -> new Thread(r, "test-spawn-tile-finder"));
                  ArrayList<Future<SpawnTileResult>> tasks = new ArrayList<>(totalTests);

                  for (int i = 0; i < totalTests; i++) {
                     Future<SpawnTileResult> task = executor.submit(() -> {
                        long startTime = System.nanoTime();
                        String worldSeed = ServerCreationSettings.getNewRandomSpawnSeed();
                        WorldEntity worldEntity = WorldEntity.getServerWorldEntity(localServer.world, worldSeed);
                        worldEntity.printPresetRegionLoads = false;
                        SpawnTileFinder.SpawnTileData foundTile = SpawnTileFinder.findSpawnTile(worldEntity);
                        worldEntity.dispose();
                        return new SpawnTileResult(foundTile, System.nanoTime() - startTime);
                     });
                     tasks.add(task);
                  }

                  Consumer<ArrayList<Long>> printTimes = timeTaken -> {
                     long averageTimeTaken = (long)timeTaken.stream().mapToLong(l -> l).average().orElse(0.0);
                     long minTimeTaken = timeTaken.stream().mapToLong(l -> l).min().orElse(0L);
                     long maxTimeTaken = timeTaken.stream().mapToLong(l -> l).max().orElse(0L);
                     long medianTimeTaken = timeTaken.stream().mapToLong(l -> l).sorted().skip(timeTaken.size() / 2).findFirst().orElse(0L);
                     System.out
                        .println(
                           "Average time taken: "
                              + GameUtils.getTimeStringNano(averageTimeTaken)
                              + ", min: "
                              + GameUtils.getTimeStringNano(minTimeTaken)
                              + ", median: "
                              + GameUtils.getTimeStringNano(medianTimeTaken)
                              + ", max: "
                              + GameUtils.getTimeStringNano(maxTimeTaken)
                        );
                  };
                  Thread waitThread = new Thread(
                     () -> {
                        long lastPrintTime = System.currentTimeMillis();
                        int totalFound = 0;
                        int totalBackups = 0;
                        int totalRandoms = 0;
                        ArrayList<Long> timeTaken = new ArrayList<>();

                        for (Future<SpawnTileResult> task : tasks) {
                           try {
                              SpawnTileResult data = task.get();
                              timeTaken.add(data.timeTaken);
                              totalFound++;
                              if (data.tile.isBackup) {
                                 totalBackups++;
                              }

                              if (data.tile.isCompletelyRandom) {
                                 totalRandoms++;
                              }

                              if (totalFound % runGCEveryTest == 0) {
                                 System.gc();
                              }

                              if (System.currentTimeMillis() - lastPrintTime > printProgressEverySeconds * 1000) {
                                 System.out.println("Current spawn tile finder test progress: " + totalFound + "/" + totalTests);
                                 printTimes.accept(timeTaken);
                                 lastPrintTime = System.currentTimeMillis();
                              }
                           } catch (InterruptedException | ExecutionException e) {
                              e.printStackTrace();
                           }
                        }

                        executor.shutdown();
                        System.out.println("Completed spawn tile finder test: " + totalFound + "/" + totalTests);
                        printTimes.accept(timeTaken);
                        System.out
                           .println(
                              "Backup spawn tile used: "
                                 + totalBackups
                                 + "/"
                                 + totalFound
                                 + " ("
                                 + GameMath.toDecimals((float)totalBackups / totalFound * 100.0F, 2)
                                 + "%)"
                           );
                        System.out
                           .println(
                              "Completely random spawn tile used: "
                                 + totalRandoms
                                 + "/"
                                 + totalFound
                                 + " ("
                                 + GameMath.toDecimals((float)totalRandoms / totalFound * 100.0F, 2)
                                 + "%)"
                           );
                        System.gc();
                     }
                  );
                  waitThread.start();
               } else {
                  GameLog.warn.println("Cannot test spawn tile finder because not hosting or playing singleplayer");
               }
            }
         );
         this.addTool("Noise Tests", () -> {
            final FormManager manager = parent.getManager();
            manager.addComponent(new NoiseTestForm() {
               @Override
               public void removeForm() {
                  manager.removeComponent(this);
               }
            });
         });
         this.addTool(new MouseDebugGameTool(parent, "Update liquid data") {
            @Override
            public void init() {
               this.onLeftClick(e -> {
                  Level level = DebugToolsList.this.getLevel();
                  int tileX = this.getMouseTileX();
                  int tileY = this.getMouseTileY();
                  level.liquidManager.updateLevel(null, tileX, tileY, tileX, tileY, true, true);
                  return true;
               }, "Update liquid data at tile");
               this.onRightClick(e -> {
                  Level level = DebugToolsList.this.getLevel();
                  int tileX = this.getMouseTileX();
                  int tileY = this.getMouseTileY();
                  Region region = level.regionManager.getRegionByTile(tileX, tileY, false);
                  if (region == null) {
                     return true;
                  }

                  region.updateLiquidManager();
                  return true;
               }, "Update liquid data at region");
               this.onKeyClick(-98, e -> {
                  Level level = DebugToolsList.this.getLevel();
                  int tileX = this.getMouseTileX();
                  int tileY = this.getMouseTileY();
                  level.liquidManager.queueTextureUpdate(tileX, tileY);
                  return true;
               }, "Update liquid data texture at tile");
            }
         });
         this.addTool(new MouseDebugGameTool(parent, "Update subregion data") {
            @Override
            public void init() {
               this.onLeftClick(e -> {
                  Level level = DebugToolsList.this.getLevel();
                  int tileX = this.getMouseTileX();
                  int tileY = this.getMouseTileY();
                  Region region = level.regionManager.getRegionByTile(tileX, tileY, false);
                  if (region != null) {
                     region.subRegionData.update();
                  }

                  return true;
               }, "Update region data at tile");
            }
         });
      }
   }

   public void addTool(String text, Runnable onClicked) {
      this.tools.add(new DebugToolsList.RunnableTool(text, () -> {
         try {
            onClicked.run();
         } catch (Exception ex) {
            System.err.println(text + " debug tool error:");
            ex.printStackTrace();
         }
      }));
   }

   public void addTool(DebugGameTool tool) {
      this.addTool(tool.name, () -> {
         GameToolManager.clearGameTools(this.parent);
         GameToolManager.setGameTool(tool, this.parent);
      });
   }

   public void addSubMenu(String text, SelectionFloatMenu floatMenu, boolean removingSubmenuRemovesParent) {
      this.tools.add(menu -> menu.add(text, floatMenu, removingSubmenuRemovesParent));
   }

   public Level getLevel() {
      return this.parent.client.getLevel();
   }

   public Server getServer() {
      return this.parent.client.getLocalServer();
   }

   public ServerClient getLocalClient() {
      return this.getServer() == null ? null : this.getServer().getLocalServerClient();
   }

   public Level getServerLevel() {
      ServerClient localClient = this.getLocalClient();
      return localClient == null ? null : this.getServer().world.getLevel(localClient.getLevelIdentifier());
   }

   public ServerSettlementData getServerSettlementAtPlayer() {
      ServerClient localClient = this.getLocalClient();
      if (localClient == null) {
         return null;
      }

      Level level = localClient.getLevel();
      return SettlementsWorldData.getSettlementsData(localClient.getServer())
         .getServerDataAtTile(level.getIdentifier(), localClient.playerMob.getTileX(), localClient.playerMob.getTileY());
   }

   public FloatMenu getFloatMenu(FormComponent button) {
      SelectionFloatMenu menu = new SelectionFloatMenu(button, SelectionFloatMenu.Solid(new FontOptions(16)));

      for (DebugToolsList.Tool tool : this.tools) {
         tool.addToMenu(menu);
      }

      return menu;
   }

   private static class RunnableTool implements DebugToolsList.Tool {
      public final String name;
      public final Runnable onClicked;

      public RunnableTool(String name, Runnable onClicked) {
         this.name = name;
         this.onClicked = onClicked;
      }

      @Override
      public void addToMenu(SelectionFloatMenu menu) {
         menu.add(this.name, () -> {
            this.onClicked.run();
            menu.remove();
         });
      }
   }

   private interface Tool {
      void addToMenu(SelectionFloatMenu var1);
   }
}
