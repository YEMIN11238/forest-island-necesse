package necesse.engine.registries;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import necesse.engine.GameLoadingScreen;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.modLoader.LoadedMod;
import necesse.entity.mobs.AscendedPylonDummyMob;
import necesse.entity.mobs.BannerOfWarDummyMob;
import necesse.entity.mobs.BoneSpikeMob;
import necesse.entity.mobs.EarthSpikeMob;
import necesse.entity.mobs.HumanTexture;
import necesse.entity.mobs.HumanTextureFull;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobTexture;
import necesse.entity.mobs.ProjectileHitboxMob;
import necesse.entity.mobs.SwampSporeDummyMob;
import necesse.entity.mobs.TrainingDummyMob;
import necesse.entity.mobs.friendly.BoarMob;
import necesse.entity.mobs.friendly.BullMob;
import necesse.entity.mobs.friendly.ChickenMob;
import necesse.entity.mobs.friendly.CowMob;
import necesse.entity.mobs.friendly.CrocodileMob;
import necesse.entity.mobs.friendly.GrizzboltMob;
import necesse.entity.mobs.friendly.GrizzlyBearMob;
import necesse.entity.mobs.friendly.HoneyBeeMob;
import necesse.entity.mobs.friendly.LifeEssenceFollowingMob;
import necesse.entity.mobs.friendly.PenguinMob;
import necesse.entity.mobs.friendly.PigMob;
import necesse.entity.mobs.friendly.PolarBearMob;
import necesse.entity.mobs.friendly.QueenBeeMob;
import necesse.entity.mobs.friendly.QuickTinkerFollowingMob;
import necesse.entity.mobs.friendly.RamMob;
import necesse.entity.mobs.friendly.RoosterMob;
import necesse.entity.mobs.friendly.SheepMob;
import necesse.entity.mobs.friendly.ThrumboMob;
import necesse.entity.mobs.friendly.WildOstrichMob;
import necesse.entity.mobs.friendly.critters.BirdMob;
import necesse.entity.mobs.friendly.critters.BluebirdMob;
import necesse.entity.mobs.friendly.critters.CanaryBirdMob;
import necesse.entity.mobs.friendly.critters.CardinalBirdMob;
import necesse.entity.mobs.friendly.critters.CrabMob;
import necesse.entity.mobs.friendly.critters.DuckMob;
import necesse.entity.mobs.friendly.critters.FrogMob;
import necesse.entity.mobs.friendly.critters.MouseMob;
import necesse.entity.mobs.friendly.critters.RabbitMob;
import necesse.entity.mobs.friendly.critters.ScorpionMob;
import necesse.entity.mobs.friendly.critters.SnowHareMob;
import necesse.entity.mobs.friendly.critters.SpiderCritterMob;
import necesse.entity.mobs.friendly.critters.SquirrelMob;
import necesse.entity.mobs.friendly.critters.StealthBasketHumanMob;
import necesse.entity.mobs.friendly.critters.SwampSlugMob;
import necesse.entity.mobs.friendly.critters.TurtleMob;
import necesse.entity.mobs.friendly.critters.caveling.DeepSandStoneCaveling;
import necesse.entity.mobs.friendly.critters.caveling.DeepSnowStoneCaveling;
import necesse.entity.mobs.friendly.critters.caveling.DeepStoneCaveling;
import necesse.entity.mobs.friendly.critters.caveling.DeepSwampStoneCaveling;
import necesse.entity.mobs.friendly.critters.caveling.DryadCaveling;
import necesse.entity.mobs.friendly.critters.caveling.GraniteCaveling;
import necesse.entity.mobs.friendly.critters.caveling.IncursionCaveling;
import necesse.entity.mobs.friendly.critters.caveling.IncursionFlamelingMob;
import necesse.entity.mobs.friendly.critters.caveling.SandStoneCaveling;
import necesse.entity.mobs.friendly.critters.caveling.SnowStoneCaveling;
import necesse.entity.mobs.friendly.critters.caveling.StoneCaveling;
import necesse.entity.mobs.friendly.critters.caveling.SwampStoneCaveling;
import necesse.entity.mobs.friendly.critters.croppler.BeetCroppler;
import necesse.entity.mobs.friendly.critters.croppler.CarrotCroppler;
import necesse.entity.mobs.friendly.critters.croppler.OnionCroppler;
import necesse.entity.mobs.friendly.critters.croppler.PotatoCroppler;
import necesse.entity.mobs.friendly.critters.croppler.PumpkinCroppler;
import necesse.entity.mobs.friendly.critters.flyingbugs.ButterflyMob;
import necesse.entity.mobs.friendly.critters.flyingbugs.FireflyMob;
import necesse.entity.mobs.friendly.human.ElderHumanMob;
import necesse.entity.mobs.friendly.human.GenericHumanMob;
import necesse.entity.mobs.friendly.human.GuardHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.AlchemistHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.AnglerHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.AnimalKeeperHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.BlacksmithHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.ExoticMerchantHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.ExplorerHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.FarmerHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.FriendlyJonasHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.FriendlyWitchHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.GunsmithHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.HunterHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.MageHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.MinerHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.PawnBrokerHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.PirateHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.StylistHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.TraderHumanMob;
import necesse.entity.mobs.hostile.AgedChampionMob;
import necesse.entity.mobs.hostile.AncientArmoredSkeletonMob;
import necesse.entity.mobs.hostile.AncientSkeletonMageMob;
import necesse.entity.mobs.hostile.AncientSkeletonMob;
import necesse.entity.mobs.hostile.AncientSkeletonRaiderMob;
import necesse.entity.mobs.hostile.AncientSkeletonThrowerMob;
import necesse.entity.mobs.hostile.ArcanicPylonMob;
import necesse.entity.mobs.hostile.AscendedBatMob;
import necesse.entity.mobs.hostile.AscendedGolemMob;
import necesse.entity.mobs.hostile.AshGolemMob;
import necesse.entity.mobs.hostile.BlackCaveSpiderMob;
import necesse.entity.mobs.hostile.BloatedSpiderMob;
import necesse.entity.mobs.hostile.BoneWalkerMob;
import necesse.entity.mobs.hostile.CaveMoleMob;
import necesse.entity.mobs.hostile.ChickenRaiderMob;
import necesse.entity.mobs.hostile.CrawlingZombieMob;
import necesse.entity.mobs.hostile.CrazedRavenMob;
import necesse.entity.mobs.hostile.CryoFlakeMob;
import necesse.entity.mobs.hostile.CryptBatMob;
import necesse.entity.mobs.hostile.CryptVampireMob;
import necesse.entity.mobs.hostile.CrystalArmadillo;
import necesse.entity.mobs.hostile.CrystalGolemMob;
import necesse.entity.mobs.hostile.DeepCaveSpiritMob;
import necesse.entity.mobs.hostile.DesertCrawlerMob;
import necesse.entity.mobs.hostile.DryadSentinelMob;
import necesse.entity.mobs.hostile.EnchantedCrawlingZombieMob;
import necesse.entity.mobs.hostile.EnchantedZombieArcherMob;
import necesse.entity.mobs.hostile.EnchantedZombieMob;
import necesse.entity.mobs.hostile.FakeSharkMob;
import necesse.entity.mobs.hostile.FishianHealerMob;
import necesse.entity.mobs.hostile.FishianHookWarriorMob;
import necesse.entity.mobs.hostile.FishianRaiderMob;
import necesse.entity.mobs.hostile.FishianShamanMob;
import necesse.entity.mobs.hostile.FlamelingShooterMob;
import necesse.entity.mobs.hostile.ForestSpectorMob;
import necesse.entity.mobs.hostile.FrostSentryMob;
import necesse.entity.mobs.hostile.FrozenDwarfMob;
import necesse.entity.mobs.hostile.FrozenDwarfRaiderMob;
import necesse.entity.mobs.hostile.FurnaceGolemMob;
import necesse.entity.mobs.hostile.GhostSlimeMob;
import necesse.entity.mobs.hostile.GiantCaveSpiderMob;
import necesse.entity.mobs.hostile.GiantScorpionMob;
import necesse.entity.mobs.hostile.GiantSwampSlimeMob;
import necesse.entity.mobs.hostile.GoblinMob;
import necesse.entity.mobs.hostile.HaywireShredderMob;
import necesse.entity.mobs.hostile.HumanRaiderMob;
import necesse.entity.mobs.hostile.IncursionCrawlingZombieMob;
import necesse.entity.mobs.hostile.JackalMob;
import necesse.entity.mobs.hostile.LeggedSlimeThrowerMob;
import necesse.entity.mobs.hostile.MageSlimeMob;
import necesse.entity.mobs.hostile.MagechanicMob;
import necesse.entity.mobs.hostile.MimeMob;
import necesse.entity.mobs.hostile.MimicMob;
import necesse.entity.mobs.hostile.MosquitoEggMob;
import necesse.entity.mobs.hostile.MosquitoMob;
import necesse.entity.mobs.hostile.MummyMageMob;
import necesse.entity.mobs.hostile.MummyMob;
import necesse.entity.mobs.hostile.MummyRaiderMob;
import necesse.entity.mobs.hostile.MutantMob;
import necesse.entity.mobs.hostile.NinjaMob;
import necesse.entity.mobs.hostile.NinjaRaiderMob;
import necesse.entity.mobs.hostile.PalworldSyndicateThugMob;
import necesse.entity.mobs.hostile.PhantomMob;
import necesse.entity.mobs.hostile.PhoenixEggMob;
import necesse.entity.mobs.hostile.PhoenixMob;
import necesse.entity.mobs.hostile.PirateRaiderMob;
import necesse.entity.mobs.hostile.PossessedSettlerMob;
import necesse.entity.mobs.hostile.RCToySoldierMob;
import necesse.entity.mobs.hostile.RogueHunterRaiderMob;
import necesse.entity.mobs.hostile.SandSpiritMob;
import necesse.entity.mobs.hostile.SandwormBody;
import necesse.entity.mobs.hostile.SandwormHead;
import necesse.entity.mobs.hostile.SandwormTail;
import necesse.entity.mobs.hostile.SharkMob;
import necesse.entity.mobs.hostile.SkeletonMageMob;
import necesse.entity.mobs.hostile.SkeletonMinerMob;
import necesse.entity.mobs.hostile.SkeletonMob;
import necesse.entity.mobs.hostile.SkeletonThrowerMob;
import necesse.entity.mobs.hostile.SlimeWormBody;
import necesse.entity.mobs.hostile.SlimeWormHead;
import necesse.entity.mobs.hostile.SmallCaveSpiderMob;
import necesse.entity.mobs.hostile.SnowWolfMob;
import necesse.entity.mobs.hostile.SpiderkinArcherMob;
import necesse.entity.mobs.hostile.SpiderkinMageMob;
import necesse.entity.mobs.hostile.SpiderkinMob;
import necesse.entity.mobs.hostile.SpiderkinWarriorMob;
import necesse.entity.mobs.hostile.SpiritGhoulMob;
import necesse.entity.mobs.hostile.StabbyBushMob;
import necesse.entity.mobs.hostile.StaticJellyfishMob;
import necesse.entity.mobs.hostile.SwampCaveSpiderMob;
import necesse.entity.mobs.hostile.SwampDwellerMob;
import necesse.entity.mobs.hostile.SwampShooterMob;
import necesse.entity.mobs.hostile.SwampSkeletonMob;
import necesse.entity.mobs.hostile.SwampSlimeMob;
import necesse.entity.mobs.hostile.SwampZombieMob;
import necesse.entity.mobs.hostile.TheMafiaRaiderMob;
import necesse.entity.mobs.hostile.TrapperZombieMob;
import necesse.entity.mobs.hostile.TrenchcoatGoblinChestplateMob;
import necesse.entity.mobs.hostile.TrenchcoatGoblinHelmetMob;
import necesse.entity.mobs.hostile.TrenchcoatGoblinShoesMob;
import necesse.entity.mobs.hostile.TrenchcoatGoblinStackedMob;
import necesse.entity.mobs.hostile.VampireMob;
import necesse.entity.mobs.hostile.VampireRaiderMob;
import necesse.entity.mobs.hostile.VoidApprentice;
import necesse.entity.mobs.hostile.VoidApprenticeRaiderMob;
import necesse.entity.mobs.hostile.WarriorSlimeMob;
import necesse.entity.mobs.hostile.WebSpinnerMob;
import necesse.entity.mobs.hostile.ZombieArcherMob;
import necesse.entity.mobs.hostile.ZombieMob;
import necesse.entity.mobs.hostile.bosses.AncientVultureEggMob;
import necesse.entity.mobs.hostile.bosses.AncientVultureMob;
import necesse.entity.mobs.hostile.bosses.ArenaEntrancePortalMob;
import necesse.entity.mobs.hostile.bosses.BossSpawnPortalMob;
import necesse.entity.mobs.hostile.bosses.ChieftainGauntletSpawnerPortalMob;
import necesse.entity.mobs.hostile.bosses.ChieftainMob;
import necesse.entity.mobs.hostile.bosses.CryoQueenMob;
import necesse.entity.mobs.hostile.bosses.CrystalDragonBody;
import necesse.entity.mobs.hostile.bosses.CrystalDragonHead;
import necesse.entity.mobs.hostile.bosses.CursedCroneSpiritGhoulMob;
import necesse.entity.mobs.hostile.bosses.EvilsPortalMob;
import necesse.entity.mobs.hostile.bosses.EvilsProtectorMob;
import necesse.entity.mobs.hostile.bosses.FallenDragonBody;
import necesse.entity.mobs.hostile.bosses.FallenDragonHead;
import necesse.entity.mobs.hostile.bosses.FallenWizardGhostMob;
import necesse.entity.mobs.hostile.bosses.FallenWizardMob;
import necesse.entity.mobs.hostile.bosses.FlyingSpiritsBody;
import necesse.entity.mobs.hostile.bosses.GritHead;
import necesse.entity.mobs.hostile.bosses.HomePortalMob;
import necesse.entity.mobs.hostile.bosses.IncursionCavelingsSpawnerPortalMob;
import necesse.entity.mobs.hostile.bosses.MoonlightDancerMob;
import necesse.entity.mobs.hostile.bosses.MotherSlimeMob;
import necesse.entity.mobs.hostile.bosses.NightSwarmBatMob;
import necesse.entity.mobs.hostile.bosses.NightSwarmStartMob;
import necesse.entity.mobs.hostile.bosses.PestWardenBody;
import necesse.entity.mobs.hostile.bosses.PestWardenHead;
import necesse.entity.mobs.hostile.bosses.PortalMinion;
import necesse.entity.mobs.hostile.bosses.QueenSpiderMob;
import necesse.entity.mobs.hostile.bosses.ReaperMob;
import necesse.entity.mobs.hostile.bosses.ReaperSpiritMob;
import necesse.entity.mobs.hostile.bosses.ReaperSpiritPortalMob;
import necesse.entity.mobs.hostile.bosses.ReturnPortalMob;
import necesse.entity.mobs.hostile.bosses.SageAndGritStartMob;
import necesse.entity.mobs.hostile.bosses.SageHead;
import necesse.entity.mobs.hostile.bosses.SpiderEmpressMob;
import necesse.entity.mobs.hostile.bosses.SpiderHatchlingMob;
import necesse.entity.mobs.hostile.bosses.SpiritTornadoMob;
import necesse.entity.mobs.hostile.bosses.SunlightChampionMob;
import necesse.entity.mobs.hostile.bosses.SwampGuardianBody;
import necesse.entity.mobs.hostile.bosses.SwampGuardianHead;
import necesse.entity.mobs.hostile.bosses.SwampGuardianTail;
import necesse.entity.mobs.hostile.bosses.TheCursedCroneMob;
import necesse.entity.mobs.hostile.bosses.TrialPortalMob;
import necesse.entity.mobs.hostile.bosses.VoidWizard;
import necesse.entity.mobs.hostile.bosses.VoidWizardClone;
import necesse.entity.mobs.hostile.bosses.VultureHatchling;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.AscendedBeamMob;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.AscendedGauntletMob;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.AscendedWizardMob;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.AscendedWizardPeripheralMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraBossMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraNeckMob;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidClawMob;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidHornMob;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidMob;
import necesse.entity.mobs.hostile.pirates.PirateCaptainMob;
import necesse.entity.mobs.hostile.pirates.PirateParrotMob;
import necesse.entity.mobs.hostile.pirates.PirateRecruit;
import necesse.entity.mobs.hostile.theRunebound.BattleChefMob;
import necesse.entity.mobs.hostile.theRunebound.CroneMob;
import necesse.entity.mobs.hostile.theRunebound.RuneboundBruteMob;
import necesse.entity.mobs.hostile.theRunebound.RuneboundShamanMob;
import necesse.entity.mobs.hostile.theRunebound.RuneboundTrapperMob;
import necesse.entity.mobs.hostile.witches.EvilWitchNecroticBowMob;
import necesse.entity.mobs.hostile.witches.EvilWitchNecroticFlaskMob;
import necesse.entity.mobs.hostile.witches.EvilWitchNecroticGreatswordMob;
import necesse.entity.mobs.hostile.witches.EvilWitchStartMob;
import necesse.entity.mobs.polymorph.ChickenPolymorphMob;
import necesse.entity.mobs.polymorph.DuckPolymorphMob;
import necesse.entity.mobs.polymorph.FrogPolymorphMob;
import necesse.entity.mobs.polymorph.RabbitPolymorphMob;
import necesse.entity.mobs.polymorph.RoosterPolymorphMob;
import necesse.entity.mobs.polymorph.SquirrelPolymorphMob;
import necesse.entity.mobs.polymorph.TurtlePolymorphMob;
import necesse.entity.mobs.summon.ExplosiveBarrelMob;
import necesse.entity.mobs.summon.MinecartMob;
import necesse.entity.mobs.summon.SawBladeMob;
import necesse.entity.mobs.summon.WoodBoatMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.AncestorKnightFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.AncestorMageFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.ArachnidSpiderMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.BabyCrawlingZombieFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.BabyDryadMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.BabySkeletonMageMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.BabySkeletonMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.BabySnowmanMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.BabySpiderMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.BabySpiderkinArcher;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.BabySpiderkinWarrior;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.BabyZombieArcherMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.BabyZombieMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.BashyBushFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.BashyBushMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.BeeFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.ChargingPhantomFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.CryoFlakeFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.DryadSpiritFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.DuskMoonDiscFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.FrostPiercerFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.GhostShipFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.GrizzboltSummonMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.LocustFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.MakeshiftArcThrowerMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.MakeshiftRobotSettlerFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.MakeshiftTurretMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.OrbOfSlimesFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.PoisonSlimeFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.PouncingSlimeFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.RavenLordFeatherFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.ReaperSpiritFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.RubyDragonBodyFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.RubyDragonHeadFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.RubyShieldFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.SentientSwordFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.SkeletonFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.SlimeGreatswordFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.StabbyBushFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.VampiricBatFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.VultureHatchlingFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.WanderbotFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob.BumperCarMob;
import necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob.HoverboardMob;
import necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob.JumpingBallMob;
import necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob.JunkMechMob;
import necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob.MinecartMountMob;
import necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob.RuneboundBoatMob;
import necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob.SeahorseMob;
import necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob.SteelBoatMob;
import necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob.TameOstrichMob;
import necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob.WitchBroomMob;
import necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob.WoodBoatMountMob;
import necesse.entity.mobs.summon.summonFollowingMob.petFollowingMob.GhostlyBowFollowingMob;
import necesse.entity.mobs.summon.summonFollowingMob.petFollowingMob.PetCavelingElder;
import necesse.entity.mobs.summon.summonFollowingMob.petFollowingMob.PetDragonWhelpMob;
import necesse.entity.mobs.summon.summonFollowingMob.petFollowingMob.PetEvilMinion;
import necesse.entity.mobs.summon.summonFollowingMob.petFollowingMob.PetGrizzlyBearCub;
import necesse.entity.mobs.summon.summonFollowingMob.petFollowingMob.PetIcicleArmadilloMob;
import necesse.entity.mobs.summon.summonFollowingMob.petFollowingMob.PetParrotMob;
import necesse.entity.mobs.summon.summonFollowingMob.petFollowingMob.PetPenguinMob;
import necesse.entity.mobs.summon.summonFollowingMob.petFollowingMob.PetPugMob;
import necesse.entity.mobs.summon.summonFollowingMob.petFollowingMob.PetThrumboMob;
import necesse.entity.mobs.summon.summonFollowingMob.petFollowingMob.PetWalkingTorchMob;
import necesse.entity.mobs.summon.summonFollowingMob.petFollowingMob.WillOWispMob;
import necesse.gfx.GameResources;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.placeableItem.MobSpawnItem;
import necesse.level.maps.Level;

public class MobRegistry extends ClassedGameRegistry<Mob, MobRegistry.MobRegistryElement> {
   public static final MobRegistry instance = new MobRegistry();
   private static final HashMap<Integer, Integer> mobIDToSpawnItemIDMap = new HashMap<>();
   public static HashSet<Integer> textColoredIcons = new HashSet<>();

   private MobRegistry() {
      super("Mob", 32762);
   }

   @Override
   public void registerCore() {
      GameLoadingScreen.drawLoadingString(Localization.translate("loading", "mobs"));
      registerMob("sheep", SheepMob.class, true);
      registerMob("ram", RamMob.class, true);
      registerMob("wildostrich", WildOstrichMob.class, true);
      registerMob("cow", CowMob.class, true);
      registerMob("bull", BullMob.class, true);
      registerMob("pig", PigMob.class, true);
      registerMob("boar", BoarMob.class, true);
      registerMob("chicken", ChickenMob.class, true);
      registerMob("rooster", RoosterMob.class, true);
      registerMob("honeybee", HoneyBeeMob.class, true, false, new LocalMessage("item", "honeybee"), null);
      registerMob("queenbee", QueenBeeMob.class, true, false, new LocalMessage("item", "queenbee"), null);
      registerMob("penguin", PenguinMob.class, true);
      registerMob("polarbear", PolarBearMob.class, true);
      registerMob("grizzlybear", GrizzlyBearMob.class, true);
      registerMob("crocodile", CrocodileMob.class, false);
      registerMob("thrumbo", ThrumboMob.class, true);
      registerMob("rabbit", RabbitMob.class, true);
      registerMob("squirrel", SquirrelMob.class, true);
      registerMob("snowhare", SnowHareMob.class, true);
      registerMob("crab", CrabMob.class, true);
      registerMob("scorpion", ScorpionMob.class, true);
      registerMob("turtle", TurtleMob.class, true);
      registerMob("swampslug", SwampSlugMob.class, true);
      registerMob("frog", FrogMob.class, true);
      registerMob("duck", DuckMob.class, true);
      registerMob("bird", BirdMob.class, true);
      registerMob("bluebird", BluebirdMob.class, true);
      registerMob("canarybird", CanaryBirdMob.class, true);
      registerMob("cardinalbird", CardinalBirdMob.class, true);
      registerMob("spider", SpiderCritterMob.class, true);
      registerMob("mouse", MouseMob.class, true);
      registerMob("butterfly", ButterflyMob.class, true);
      registerMob("firefly", FireflyMob.class, true);
      registerMob("cropplerbeet", BeetCroppler.class, true);
      registerMob("cropplercarrot", CarrotCroppler.class, true);
      registerMob("croppleronion", OnionCroppler.class, true);
      registerMob("cropplerpumpkin", PumpkinCroppler.class, true);
      registerMob("cropplerpotato", PotatoCroppler.class, true);
      registerMob("stonecaveling", StoneCaveling.class, true, false, null);
      registerMob("snowstonecaveling", SnowStoneCaveling.class, true, false, null);
      registerMob("granitecaveling", GraniteCaveling.class, true, false, null);
      registerMob("swampstonecaveling", SwampStoneCaveling.class, true, false, null);
      registerMob("sandstonecaveling", SandStoneCaveling.class, true, false, null);
      registerMob("deepstonecaveling", DeepStoneCaveling.class, true, false, null);
      registerMob("deepsnowstonecaveling", DeepSnowStoneCaveling.class, true, false, null);
      registerMob("dryadcaveling", DryadCaveling.class, true, false, null);
      registerMob("deepswampstonecaveling", DeepSwampStoneCaveling.class, true, false, null);
      registerMob("deepsandstonecaveling", DeepSandStoneCaveling.class, true, false, null);
      registerMob("flameling", IncursionFlamelingMob.class, true, false, null);
      registerMob("incursioncaveling", IncursionCaveling.class, true, false, null);
      registerMob("stealthbaskethuman", StealthBasketHumanMob.class, true, false, null);
      registerMob("human", GenericHumanMob.class, true);
      registerMob("farmerhuman", FarmerHumanMob.class, true);
      registerMob("blacksmithhuman", BlacksmithHumanMob.class, true);
      registerMob("guardhuman", GuardHumanMob.class, true);
      registerMob("magehuman", MageHumanMob.class, true);
      registerMob("gunsmithhuman", GunsmithHumanMob.class, true);
      registerMob("alchemisthuman", AlchemistHumanMob.class, true);
      registerMob("hunterhuman", HunterHumanMob.class, true);
      registerMob("elderhuman", ElderHumanMob.class, true);
      registerMob("anglerhuman", AnglerHumanMob.class, true);
      registerMob("pawnbrokerhuman", PawnBrokerHumanMob.class, true);
      registerMob("animalkeeperhuman", AnimalKeeperHumanMob.class, true);
      registerMob("stylisthuman", StylistHumanMob.class, true);
      registerMob("piratehuman", PirateHumanMob.class, true);
      registerMob("explorerhuman", ExplorerHumanMob.class, true);
      registerMob("minerhuman", MinerHumanMob.class, true);
      registerMob("traderhuman", TraderHumanMob.class, true);
      registerMob("exoticmerchanthuman", ExoticMerchantHumanMob.class, true);
      registerMob("friendlywitchhuman", FriendlyWitchHumanMob.class, true);
      registerMob("friendlyjonashuman", FriendlyJonasHumanMob.class, true);
      registerMob("evilwitch", EvilWitchStartMob.class, true, false);
      registerMob("evilwitchflask", EvilWitchNecroticFlaskMob.class, false, false, new LocalMessage("mob", "evilwitch"), null);
      registerMob("evilwitchbow", EvilWitchNecroticBowMob.class, false, false, new LocalMessage("mob", "evilwitch"), null);
      registerMob("evilwitchgreatsword", EvilWitchNecroticGreatswordMob.class, false, false, new LocalMessage("mob", "evilwitch"), null);
      registerMob("zombie", ZombieMob.class, true);
      registerMob("trapperzombie", TrapperZombieMob.class, true);
      registerMob("goblin", GoblinMob.class, true);
      registerMob("trenchcoatgoblinstacked", TrenchcoatGoblinStackedMob.class, true);
      registerMob("trenchcoatgoblinhelmet", TrenchcoatGoblinHelmetMob.class, true, false, false);
      registerMob("trenchcoatgoblinchestplate", TrenchcoatGoblinChestplateMob.class, true, false, false);
      registerMob("trenchcoatgoblinshoes", TrenchcoatGoblinShoesMob.class, true, false, false);
      registerMob("vampire", VampireMob.class, true);
      registerMob("zombiearcher", ZombieArcherMob.class, true);
      registerMob("crawlingzombie", CrawlingZombieMob.class, true);
      registerMob("giantcavespider", GiantCaveSpiderMob.class, true);
      registerMob("blackcavespider", BlackCaveSpiderMob.class, true);
      registerMob("swampcavespider", SwampCaveSpiderMob.class, true);
      registerMob("cavemole", CaveMoleMob.class, true);
      registerMob("frozendwarf", FrozenDwarfMob.class, true);
      registerMob("frostsentry", FrostSentryMob.class, true);
      registerMob("swampzombie", SwampZombieMob.class, true);
      registerMob("swampslime", SwampSlimeMob.class, true);
      registerMob("swampshooter", SwampShooterMob.class, true);
      registerMob("enchantedzombie", EnchantedZombieMob.class, true);
      registerMob("enchantedzombiearcher", EnchantedZombieArcherMob.class, true);
      registerMob("enchantedcrawlingzombie", EnchantedCrawlingZombieMob.class, true);
      registerMob("voidapprentice", VoidApprentice.class, true);
      registerMob("mummy", MummyMob.class, true);
      registerMob("mummymage", MummyMageMob.class, true);
      registerMob("sandspirit", SandSpiritMob.class, true);
      registerMob("jackal", JackalMob.class, true);
      registerMob("giantscorpion", GiantScorpionMob.class, false);
      registerMob("skeleton", SkeletonMob.class, true);
      registerMob("skeletonthrower", SkeletonThrowerMob.class, true);
      registerMob("skeletonmage", SkeletonMageMob.class, false);
      registerMob("deepcavespirit", DeepCaveSpiritMob.class, true);
      registerMob("skeletonminer", SkeletonMinerMob.class, true);
      registerMob("ninja", NinjaMob.class, true);
      registerMob("snowwolf", SnowWolfMob.class, true);
      registerMob("cryoflake", CryoFlakeMob.class, true);
      registerMob("giantswampslime", GiantSwampSlimeMob.class, true);
      registerMob("swampskeleton", SwampSkeletonMob.class, true);
      registerMob("smallswampcavespider", SmallCaveSpiderMob.class, true);
      registerMob("swampdweller", SwampDwellerMob.class, true);
      registerMob("sandworm", SandwormHead.class, true);
      registerMob("sandwormbody", SandwormBody.class, false);
      registerMob("sandwormtail", SandwormTail.class, false);
      registerMob("desertcrawler", DesertCrawlerMob.class, true);
      registerMob("ancientskeleton", AncientSkeletonMob.class, true);
      registerMob("ancientskeletonthrower", AncientSkeletonThrowerMob.class, true);
      registerMob("ancientarmoredskeleton", AncientArmoredSkeletonMob.class, true);
      registerMob("ancientskeletonmage", AncientSkeletonMageMob.class, true);
      registerMob("leggedslimethrower", LeggedSlimeThrowerMob.class, true);
      registerMob("mageslime", MageSlimeMob.class, true);
      registerMob("ghostslime", GhostSlimeMob.class, true);
      registerMob("warriorslime", WarriorSlimeMob.class, true);
      registerMob("slimeworm", SlimeWormHead.class, true);
      registerMob("slimewormbody", SlimeWormBody.class, false);
      registerMob("cryptbat", CryptBatMob.class, true);
      registerMob("phantom", PhantomMob.class, true);
      registerMob("cryptvampire", CryptVampireMob.class, true);
      registerMob("webspinner", WebSpinnerMob.class, true);
      registerMob("bloatedspider", BloatedSpiderMob.class, true);
      registerMob("spiderkin", SpiderkinMob.class, true);
      registerMob("spiderkinwarrior", SpiderkinWarriorMob.class, true);
      registerMob("spiderkinarcher", SpiderkinArcherMob.class, true);
      registerMob("spiderkinmage", SpiderkinMageMob.class, true);
      registerMob("crystalgolem", CrystalGolemMob.class, true);
      registerMob("crystalarmadillo", CrystalArmadillo.class, true);
      registerMob("fishianhookwarrior", FishianHookWarriorMob.class, true);
      registerMob("fishianhealer", FishianHealerMob.class, true);
      registerMob("fishianshaman", FishianShamanMob.class, false, false, false);
      registerMob("staticjellyfish", StaticJellyfishMob.class, true);
      registerMob("agedchampion", AgedChampionMob.class, true);
      registerMob("stabbybush", StabbyBushMob.class, true);
      registerMob("bashybush", BashyBushMob.class, true);
      registerMob("mosquitoegg", MosquitoEggMob.class, true);
      registerMob("mosquito", MosquitoMob.class, true);
      registerMob("runeboundbrute", RuneboundBruteMob.class, true);
      registerMob("runeboundshaman", RuneboundShamanMob.class, true);
      registerMob("runeboundtrapper", RuneboundTrapperMob.class, true);
      registerMob("bonewalker", BoneWalkerMob.class, true);
      registerMob("forestspector", ForestSpectorMob.class, true);
      registerMob("dryadsentinel", DryadSentinelMob.class, true);
      registerMob("spiritghoul", SpiritGhoulMob.class, true);
      registerMob("flamelingshooter", FlamelingShooterMob.class, false, false);
      registerMob("crazedraven", CrazedRavenMob.class, true);
      registerMob("arcanicpylon", ArcanicPylonMob.class, true);
      registerMob("battlechef", BattleChefMob.class, true);
      registerMob("incursioncavelingsspawnerportal", IncursionCavelingsSpawnerPortalMob.class, false);
      registerMob("possessedsettler", PossessedSettlerMob.class, true);
      registerMob("shark", SharkMob.class, true);
      registerMob("fakeshark", FakeSharkMob.class, true);
      registerMob("palworldsyndicatethug", PalworldSyndicateThugMob.class, true);
      registerMob("grizzbolt", GrizzboltMob.class, true);
      registerMob("mime", MimeMob.class, true);
      registerMob("mimic", MimicMob.class, true);
      registerMob("furnacegolem", FurnaceGolemMob.class, true);
      registerMob("magechanic", MagechanicMob.class, true);
      registerMob("rctoysoldier", RCToySoldierMob.class, false);
      registerMob("mutant", MutantMob.class, true);
      registerMob("haywireshredder", HaywireShredderMob.class, true);
      registerMob("phoenix", PhoenixMob.class, true);
      registerMob("phoenixegg", PhoenixEggMob.class, true);
      registerMob("ashgolem", AshGolemMob.class, true);
      int unknownRaiderID = registerMob("unknownraider", HumanRaiderMob.class, false);
      registerMob("humanraider", HumanRaiderMob.class, true);
      registerMob("fishianraider", FishianRaiderMob.class, true);
      registerMob("ninjaraider", NinjaRaiderMob.class, true);
      registerMob("chickenraider", ChickenRaiderMob.class, true);
      registerMob("roguehunterraider", RogueHunterRaiderMob.class, true);
      registerMob("frozendwarfraider", FrozenDwarfRaiderMob.class, true);
      registerMob("voidapprenticeraider", VoidApprenticeRaiderMob.class, true);
      registerMob("vampireraider", VampireRaiderMob.class, true);
      registerMob("mummyraider", MummyRaiderMob.class, true);
      registerMob("pirateraider", PirateRaiderMob.class, true);
      registerMob("mafiaraider", TheMafiaRaiderMob.class, true);
      registerMob("ancientskeletonraider", AncientSkeletonRaiderMob.class, true);
      registerMob("tameostrich", TameOstrichMob.class, false);
      registerMob("seahorse", SeahorseMob.class, false);
      registerMob("petpenguin", PetPenguinMob.class, false);
      registerMob("petparrot", PetParrotMob.class, false);
      registerMob("minecartmount", MinecartMountMob.class, false, false, new LocalMessage("mob", "minecart"), null);
      registerMob("woodboatmount", WoodBoatMountMob.class, false, false, new LocalMessage("mob", "woodboat"), null);
      registerMob("steelboat", SteelBoatMob.class, false);
      registerMob("runeboundboat", RuneboundBoatMob.class, false);
      registerMob("petwalkingtorch", PetWalkingTorchMob.class, false);
      registerMob("jumpingball", JumpingBallMob.class, false);
      registerMob("witchbroom", WitchBroomMob.class, false);
      registerMob("hoverboard", HoverboardMob.class, false);
      registerMob("bumpercar", BumperCarMob.class, false, false, new LocalMessage("item", "bumpercar"), null);
      registerMob("petcavelingelder", PetCavelingElder.class, false);
      registerMob("petgrizzlybearcub", PetGrizzlyBearCub.class, false);
      registerMob("petevilminion", PetEvilMinion.class, false);
      registerMob("willowisp", WillOWispMob.class, false);
      registerMob("petpug", PetPugMob.class, false);
      registerMob("ghostlybow", GhostlyBowFollowingMob.class, false);
      registerMob("petthrumbo", PetThrumboMob.class, false);
      registerMob("petdragonwhelp", PetDragonWhelpMob.class, false);
      registerMob("peticiclearmadillo", PetIcicleArmadilloMob.class, false);
      registerMob("babyzombie", BabyZombieMob.class, false);
      registerMob("babyzombiearcher", BabyZombieArcherMob.class, false);
      registerMob("babyspider", BabySpiderMob.class, false);
      registerMob("frostpiercer", FrostPiercerFollowingMob.class, false);
      registerMob("vampiricbat", VampiricBatFollowingMob.class, false);
      registerMob("bee", BeeFollowingMob.class, false);
      registerMob("sentientsword", SentientSwordFollowingMob.class, false);
      registerMob("babysnowman", BabySnowmanMob.class, false);
      registerMob("playerpoisonslime", PoisonSlimeFollowingMob.class, false);
      registerMob("babycrawlingzombie", BabyCrawlingZombieFollowingMob.class, false);
      registerMob("stabbybushfollowingmob", StabbyBushFollowingMob.class, false);
      registerMob("bashybushfollowingmob", BashyBushFollowingMob.class, false);
      registerMob("playervulturehatchling", VultureHatchlingFollowingMob.class, false);
      registerMob("playerghostship", GhostShipFollowingMob.class, false);
      registerMob("playerreaperspirit", ReaperSpiritFollowingMob.class, false);
      registerMob("playercryoflake", CryoFlakeFollowingMob.class, false);
      registerMob("dryadspirit", DryadSpiritFollowingMob.class, false);
      registerMob("babydryad", BabyDryadMob.class, false);
      registerMob("playerpouncingslime", PouncingSlimeFollowingMob.class, false);
      registerMob("slimegreatswordslime", SlimeGreatswordFollowingMob.class, false);
      registerMob("skeletonfollowing", SkeletonFollowingMob.class, false);
      registerMob("ancestorknight", AncestorKnightFollowingMob.class, false);
      registerMob("ancestormage", AncestorMageFollowingMob.class, false);
      registerMob("babyskeleton", BabySkeletonMob.class, false);
      registerMob("babyskeletonmage", BabySkeletonMageMob.class, false);
      registerMob("playerchargingphantom", ChargingPhantomFollowingMob.class, false);
      registerMob("orbofslimesslime", OrbOfSlimesFollowingMob.class, false);
      registerMob("babyspiderkinwarrior", BabySpiderkinWarrior.class, false);
      registerMob("babyspiderkinarcher", BabySpiderkinArcher.class, false);
      registerMob("ravenlordfeather", RavenLordFeatherFollowingMob.class, false);
      registerMob("duskmoondisc", DuskMoonDiscFollowingMob.class, false);
      registerMob("rubyshield", RubyShieldFollowingMob.class, false);
      registerMob("rubydragonhead", RubyDragonHeadFollowingMob.class, false);
      registerMob("rubydragonbody", RubyDragonBodyFollowingMob.class, false);
      registerMob("arachnidspider", ArachnidSpiderMob.class, false);
      registerMob("locust", LocustFollowingMob.class, false);
      registerMob("grizzboltsummon", GrizzboltSummonMob.class, false, false, new LocalMessage("mob", "grizzbolt"), null);
      registerMob("junkmech", JunkMechMob.class, false);
      registerMob("makeshiftturret", MakeshiftTurretMob.class, false);
      registerMob("makeshiftarcthrower", MakeshiftArcThrowerMob.class, false);
      registerMob("makeshiftrobotsettler", MakeshiftRobotSettlerFollowingMob.class, false);
      registerMob("evilsprotector", EvilsProtectorMob.class, true, true);
      registerMob("evilsportal", EvilsPortalMob.class, true);
      registerMob("portalminion", PortalMinion.class, true);
      registerMob("queenspider", QueenSpiderMob.class, true, true);
      registerMob("spiderhatchling", SpiderHatchlingMob.class, true);
      registerMob("voidwizard", VoidWizard.class, true, true);
      registerMob("voidwizardclone", VoidWizardClone.class, false, false, new LocalMessage("mob", "voidwizard"), null);
      registerMob("chieftain", ChieftainMob.class, true, true);
      registerMob("chieftaingauntletspawnerportal", ChieftainGauntletSpawnerPortalMob.class, false);
      registerMob("crone", CroneMob.class, false);
      registerMob("swampguardian", SwampGuardianHead.class, true, true);
      registerMob("swampguardianbody", SwampGuardianBody.class, false, true);
      registerMob("swampguardiantail", SwampGuardianTail.class, false, true);
      registerMob("ancientvulture", AncientVultureMob.class, true, true);
      registerMob("ancientvultureegg", AncientVultureEggMob.class, true);
      registerMob("vulturehatchling", VultureHatchling.class, true);
      registerMob("piratecaptain", PirateCaptainMob.class, true, true);
      registerMob("piraterecruit", PirateRecruit.class, true);
      registerMob("pirateparrot", PirateParrotMob.class, true);
      registerMob("reaper", ReaperMob.class, true, true);
      registerMob("reaperspiritportal", ReaperSpiritPortalMob.class, true, false, false);
      registerMob("reaperspirit", ReaperSpiritMob.class, true);
      registerMob("cryoqueen", CryoQueenMob.class, true, true);
      registerMob("thecursedcrone", TheCursedCroneMob.class, true, true);
      registerMob("cursedcronespiritghoul", CursedCroneSpiritGhoulMob.class, true, false, false);
      registerMob("spirittornado", SpiritTornadoMob.class, false);
      registerMob("pestwarden", PestWardenHead.class, true, true);
      registerMob("pestwardenbody", PestWardenBody.class, true, true, false);
      registerMob("grit", GritHead.class, true, true);
      registerMob("sage", SageHead.class, true, true);
      registerMob("flyingspiritsbody", FlyingSpiritsBody.class, false, true);
      registerMob("sageandgrit", SageAndGritStartMob.class, true, true);
      registerMob("fallenwizard", FallenWizardMob.class, true, true);
      registerMob("fallenwizardghost", FallenWizardGhostMob.class, false, false, new LocalMessage("mob", "fallenwizard"), null);
      registerMob("fallendragon", FallenDragonHead.class, true);
      registerMob("fallendragonbody", FallenDragonBody.class, false);
      registerMob("motherslime", MotherSlimeMob.class, true, true);
      registerMob("nightswarm", NightSwarmStartMob.class, true, true);
      registerMob("nightswarmbat", NightSwarmBatMob.class, true, false, false);
      registerMob("spiderempress", SpiderEmpressMob.class, true, true);
      registerMob("sunlightchampion", SunlightChampionMob.class, true, true);
      registerMob("sunlightgauntlet", SunlightChampionMob.SunlightGauntletMob.class, false, false);
      registerMob("moonlightdancer", MoonlightDancerMob.class, true, true);
      registerMob("crystaldragon", CrystalDragonHead.class, true, true);
      registerMob("crystaldragonbody", CrystalDragonBody.class, true, true, false);
      registerMob("mutanthydra", MutantHydraBossMob.class, true, true);
      registerMob("mutanthydraneck", MutantHydraNeckMob.class, true, true, false);
      registerMob("mutanthydrahead", MutantHydraHeadMob.class, true, true, false);
      registerMob("ascendedwizard", AscendedWizardMob.class, true, true);
      registerMob("ascendedgauntlet", AscendedGauntletMob.class, false, false);
      registerMob("ascendedgolem", AscendedGolemMob.class, false, false, false);
      registerMob("ascendedbat", AscendedBatMob.class, false, false, false);
      registerMob("ascendedbeam", AscendedBeamMob.class, false);
      registerMob("ascendedpylondummy", AscendedPylonDummyMob.class, false, false, new LocalMessage("object", "ascendedpylon"), null);
      registerMob("ascendedwizardperipheral", AscendedWizardPeripheralMob.class, false, false, new LocalMessage("mob", "ascendedwizard"), null);
      registerMob("thevoid", TheVoidMob.class, true, true);
      registerMob("thevoidhorn", TheVoidHornMob.class, false, false);
      registerMob("thevoidclaw", TheVoidClawMob.class, false, false);
      registerMob("wanderbotfollowingmob", WanderbotFollowingMob.class, false, false);
      registerMob("incursioncrawlingzombie", IncursionCrawlingZombieMob.class, true);
      registerMob("woodboat", WoodBoatMob.class, false);
      registerMob("minecart", MinecartMob.class, false);
      registerMob("sawblade", SawBladeMob.class, false);
      registerMob("explosivebarrel", ExplosiveBarrelMob.class, false);
      registerMob("trainingdummy", TrainingDummyMob.class, false, false, new LocalMessage("object", "trainingdummy"), null);
      registerMob("bannerofwardummy", BannerOfWarDummyMob.class, false, false, new StaticMessage("BANNER_OF_WAR_DUMMY"), null);
      registerMob("swampsporedummy", SwampSporeDummyMob.class, false, false, new StaticMessage("SWAMP_SPORE_DUMMY"), null);
      registerMob("projectilehitbox", ProjectileHitboxMob.class, false, false, new StaticMessage("PROJECTILE_HITBOX"), null);
      registerMob("lifeessencefollower", LifeEssenceFollowingMob.class, false);
      registerMob("quicktinkerfollower", QuickTinkerFollowingMob.class, false);
      registerMob("homeportal", HomePortalMob.class, false);
      registerMob("returnportal", ReturnPortalMob.class, false);
      registerMob("bossspawnportal", BossSpawnPortalMob.class, false);
      registerMob("arenaentranceportal", ArenaEntrancePortalMob.class, false);
      registerMob("trialportal", TrialPortalMob.class, false);
      registerMob("bonespike", BoneSpikeMob.class, false);
      registerMob("earthspike", EarthSpikeMob.class, false);
      registerMob("chickenpolymorph", ChickenPolymorphMob.class, false, false, new LocalMessage("mob", "chicken"), null);
      registerMob("frogpolymorph", FrogPolymorphMob.class, false, false, new LocalMessage("mob", "frog"), null);
      registerMob("rabbitpolymorph", RabbitPolymorphMob.class, false, false, new LocalMessage("mob", "rabbit"), null);
      registerMob("duckpolymorph", DuckPolymorphMob.class, false, false, new LocalMessage("mob", "duck"), null);
      registerMob("roosterpolymorph", RoosterPolymorphMob.class, false, false, new LocalMessage("mob", "rooster"), null);
      registerMob("turtlepolymorph", TurtlePolymorphMob.class, false, false, new LocalMessage("mob", "turtle"), null);
      registerMob("squirrelpolymorph", SquirrelPolymorphMob.class, false, false, new LocalMessage("mob", "squirrel"), null);
   }

   protected void onRegister(MobRegistry.MobRegistryElement object, int id, String stringID, boolean isReplace) {
      super.onRegister(object, id, stringID, isReplace);
      if (object.createSpawnItem) {
         MobSpawnItem spawnItem = new MobSpawnItem(1, true, stringID);
         int spawnItemID = ItemRegistry.registerItem(stringID + "spawnitem", spawnItem, 50.0F, false, false, true);
         mobIDToSpawnItemIDMap.put(id, spawnItemID);
      }
   }

   @Override
   protected void onRegistryClose() {
      setKillHint(
         "evilsprotector", new LocalMessage("objectives", "evilsprotectortip", "mysteriousportal", TypeParsers.getItemAndNameParseString("mysteriousportal"))
      );
      setKillHint("queenspider", new LocalMessage("objectives", "queenspidertip", "royalegg", TypeParsers.getItemAndNameParseString("royalegg")));
      setKillHint("voidwizard", new LocalMessage("objectives", "voidwizardtip", "dungeonmap", TypeParsers.getItemAndNameParseString("dungeonmap")));
      setKillHint(
         "chieftain",
         new LocalMessage(
            "objectives",
            "chieftaintip",
            "boneoffering",
            TypeParsers.getItemAndNameParseString("boneoffering"),
            "chieftainspedestal",
            TypeParsers.getItemAndNameParseString("chieftainspedestal")
         )
      );
      setKillHint("swampguardian", new LocalMessage("objectives", "swampguardiantip", "spikedfossil", TypeParsers.getItemAndNameParseString("spikedfossil")));
      setKillHint(
         "ancientvulture", new LocalMessage("objectives", "ancientvulturetip", "ancientstatue", TypeParsers.getItemAndNameParseString("ancientstatue"))
      );
      setKillHint("piratecaptain", new LocalMessage("objectives", "piratecaptaintip", "piratemap", TypeParsers.getItemAndNameParseString("piratemap")));
      setKillHint("reaper", new LocalMessage("objectives", "reapertip", "shadowgate", TypeParsers.getItemAndNameParseString("shadowgate")));
      setKillHint("cryoqueen", new LocalMessage("objectives", "cryoqueentip", "icecrown", TypeParsers.getItemAndNameParseString("icecrown")));
      setKillHint("thecursedcrone", new LocalMessage("objectives", "thecursedcronetip"));
      setKillHint("pestwarden", new LocalMessage("objectives", "pestwardentip", "decayingleaf", TypeParsers.getItemAndNameParseString("decayingleaf")));
      setKillHint(
         "sageandgrit",
         new LocalMessage(
            "objectives",
            "sageandgrittip",
            "dragonsouls",
            TypeParsers.getItemAndNameParseString("dragonsouls"),
            "templepedestal",
            TypeParsers.getItemAndNameParseString("templepedestal")
         )
      );
      setKillHint("fallenwizard", new LocalMessage("objectives", "fallenwizardtip"));
      setKillHint(
         "ascendedwizard",
         new LocalMessage(
            "objectives",
            "ascendedwizardtip",
            "ascendedshard",
            TypeParsers.getItemAndNameParseString("ascendedshard"),
            "fallenworkstation",
            TypeParsers.getItemAndNameParseString("fallenworkstation")
         )
      );
   }

   public static GameMessage getLocalization(int id) {
      return id == -1 ? new StaticMessage("N/A") : instance.getElement(id).displayName;
   }

   public static GameMessage getLocalization(String stringID) {
      return getLocalization(getMobID(stringID));
   }

   public static String getDisplayName(int id) {
      return id == -1 ? null : getLocalization(id).translate();
   }

   public static String getDisplayName(String stringID) {
      return stringID == null ? null : getDisplayName(getMobID(stringID));
   }

   public static GameMessage getKillHintLocalization(int id) {
      return id == -1 ? null : instance.getElement(id).killHint;
   }

   public static GameMessage getKillHintLocalization(String stringID) {
      return getKillHintLocalization(getMobID(stringID));
   }

   public static String getKillHint(int id) {
      GameMessage localization = getKillHintLocalization(id);
      return localization != null ? localization.translate() : null;
   }

   public static String getKillHint(String stringID) {
      return getKillHint(getMobID(stringID));
   }

   public static void setKillHint(int id, GameMessage message) {
      instance.getElement(id).killHint = message;
   }

   public static void setKillHint(String stringID, GameMessage message) {
      int mobID = getMobID(stringID);
      if (mobID == -1) {
         throw new IllegalArgumentException("Mob with string ID " + stringID + " does not exist");
      }

      setKillHint(mobID, message);
   }

   public static int registerMob(String stringID, Class<? extends Mob> mobClass, boolean countKillStat) {
      return registerMob(stringID, mobClass, countKillStat, false, countKillStat);
   }

   public static int registerMob(String stringID, Class<? extends Mob> mobClass, boolean countKillStat, boolean isBossMob, boolean createSpawnItem) {
      return registerMob(stringID, mobClass, countKillStat, isBossMob, new LocalMessage("mob", stringID), null, createSpawnItem);
   }

   public static int registerMob(String stringID, Class<? extends Mob> mobClass, boolean countKillStat, boolean isBossMob) {
      return registerMob(stringID, mobClass, countKillStat, isBossMob, new LocalMessage("mob", stringID), null);
   }

   public static int registerMob(String stringID, Class<? extends Mob> mobClass, boolean countKillStat, boolean isBossMob, GameMessage killHint) {
      return registerMob(stringID, mobClass, countKillStat, isBossMob, new LocalMessage("mob", stringID), killHint);
   }

   public static int registerMob(
      String stringID, Class<? extends Mob> mobClass, boolean countKillStat, boolean isBossMob, GameMessage displayName, GameMessage killHint
   ) {
      return registerMob(stringID, mobClass, countKillStat, isBossMob, displayName, killHint, countKillStat);
   }

   public static int registerMob(
      String stringID,
      Class<? extends Mob> mobClass,
      boolean countKillStat,
      boolean isBossMob,
      GameMessage displayName,
      GameMessage killHint,
      boolean createSpawnItem
   ) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register mobs");
      }

      try {
         return instance.register(stringID, new MobRegistry.MobRegistryElement(mobClass, countKillStat, isBossMob, createSpawnItem, displayName, killHint));
      } catch (NoSuchMethodException e) {
         System.err.println("Could not register mob " + mobClass.getSimpleName() + ": Missing constructor with no parameters");
         return -1;
      }
   }

   @Deprecated
   public static Mob getMob(int id) {
      try {
         return instance.getElement(id).newInstance();
      } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
         e.printStackTrace();
         return null;
      }
   }

   public static Mob getMob(int id, Level level) {
      try {
         if (id == -1) {
            return null;
         }

         Mob out = instance.getElement(id).newInstance();
         if (out != null) {
            out.onConstructed(level);
         }

         return out;
      } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
         e.printStackTrace();
         return null;
      }
   }

   @Deprecated
   public static Mob getMob(String stringID) {
      return getMob(getMobID(stringID));
   }

   public static Mob getMob(String stringID, Level level) {
      return getMob(getMobID(stringID), level);
   }

   public static int getMobID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static int getMobID(Class<? extends Mob> clazz) {
      return instance.getElementID(clazz);
   }

   public static GameTexture getMobIcon(String stringID) {
      return getMobIcon(getMobID(stringID));
   }

   public static GameTexture getMobIcon(int id) {
      return id == -1 ? GameResources.error : instance.getElement(id).icon;
   }

   public static boolean mobExists(String stringID) {
      try {
         return instance.getElementIDRaw(stringID) >= 0;
      } catch (NoSuchElementException e) {
         return false;
      }
   }

   public static String getMobStringID(int id) {
      return instance.getElementStringID(id);
   }

   public static boolean countMobKillStat(int id) {
      return id >= 0 && id < instance.size() ? instance.getElement(id).countKillStat : false;
   }

   public static boolean isBossMob(int id) {
      return id >= 0 && id < instance.size() ? instance.getElement(id).isBossMob : false;
   }

   public static Stream<ClassIDDataContainer<Mob>> streamMobs() {
      return instance.streamElements().map(e -> (ClassIDDataContainer<Mob>)e);
   }

   public static List<ClassIDDataContainer<Mob>> getMobs() {
      return streamMobs().collect(Collectors.toList());
   }

   public static void loadMobIcons() {
      for (MobRegistry.MobRegistryElement element : instance.getElements()) {
         element.loadIcon();
      }
   }

   public static MobSpawnItem getMobSpawnItemID(String stringID) {
      return getMobSpawnItemID(getMobID(stringID));
   }

   public static MobSpawnItem getMobSpawnItemID(int id) {
      Integer spawnItemID = mobIDToSpawnItemIDMap.get(id);
      return spawnItemID == null ? null : (MobSpawnItem)ItemRegistry.getItem(spawnItemID);
   }

   protected static class MobRegistryElement extends ClassIDDataContainer<Mob> {
      public boolean countKillStat;
      public boolean isBossMob;
      public boolean createSpawnItem;
      public GameMessage displayName;
      public GameMessage killHint;
      public GameTexture icon;

      public MobRegistryElement(
         Class<? extends Mob> mobClass, boolean countKillStat, boolean isBossMob, boolean createSpawnItem, GameMessage displayName, GameMessage killHint
      ) throws NoSuchMethodException {
         super(mobClass);
         this.countKillStat = countKillStat;
         this.isBossMob = isBossMob;
         this.createSpawnItem = createSpawnItem;
         this.displayName = displayName;
         this.killHint = killHint;
      }

      public void loadIcon() {
         this.icon = GameTexture.fromFile("mobs/icons/" + this.getStringID());
      }
   }

   public static class Textures {
      public static GameTexture human_shadow;
      public static GameTexture human_baby_shadow;
      public static GameTexture human_big_shadow;
      public static GameTexture small_shadow;
      public static GameTexture human_enchained_iron;
      public static GameTexture sheep;
      public static GameTexture sheep_sheared;
      public static GameTexture sheep_shadow;
      public static GameTexture ram;
      public static GameTexture ram_sheared;
      public static GameTexture lamb;
      public static GameTexture lamb_shadow;
      public static GameTexture ostrich;
      public static GameTexture ostrichMount;
      public static GameTexture ostrich_shadow;
      public static GameTexture seahorse_front;
      public static GameTexture seahorse_back;
      public static GameTexture seahorse_shadow;
      public static GameTexture cow;
      public static GameTexture cow_shadow;
      public static GameTexture bull;
      public static GameTexture calf;
      public static GameTexture calf_shadow;
      public static GameTexture pig;
      public static GameTexture pig_shadow;
      public static GameTexture boar;
      public static GameTexture piglet;
      public static GameTexture piglet_shadow;
      public static GameTexture penguin;
      public static GameTexture chicken;
      public static GameTexture chicken_shadow;
      public static GameTexture rooster;
      public static GameTexture rooster_shadow;
      public static GameTexture chick;
      public static GameTexture chick_shadow;
      public static MobTexture honeyBee;
      public static MobTexture queenBee;
      public static GameTexture junkMech;
      public static GameTexture junkMech_shadow;
      public static GameTexture junkMech_legs;
      public static GameTexture junkMech_guns;
      public static GameTexture rabbit;
      public static GameTexture squirrel;
      public static GameTexture snowHare;
      public static GameTexture swampSlug;
      public static GameTexture bird_shadow;
      public static GameTexture bird;
      public static GameTexture bluebird;
      public static GameTexture canaryBird;
      public static GameTexture cardinalBird;
      public static MobTexture crab;
      public static MobTexture scorpion;
      public static MobTexture turtle;
      public static MobTexture duck;
      public static MobTexture frog;
      public static MobTexture spider;
      public static MobTexture mouse;
      public static MobTexture butterfly;
      public static MobTexture firefly;
      public static MobTexture beetCroppler;
      public static MobTexture carrotCroppler;
      public static MobTexture onionCroppler;
      public static MobTexture potatoCroppler;
      public static MobTexture pumpkinCroppler;
      public static HumanTexture stoneCaveling;
      public static HumanTexture snowStoneCaveling;
      public static HumanTexture graniteCaveling;
      public static HumanTexture swampStoneCaveling;
      public static HumanTexture sandStoneCaveling;
      public static HumanTexture flameling;
      public static HumanTexture deepStoneCaveling;
      public static HumanTexture deepSnowStoneCaveling;
      public static HumanTexture dryadCaveling;
      public static HumanTexture deepSwampStoneCaveling;
      public static HumanTexture deepSandStoneCaveling;
      public static HumanTexture incursionCaveling;
      public static GameTexture caveling_shadow;
      public static HumanTexture zombie;
      public static HumanTexture zombieArcher;
      public static HumanTexture zombieArcherWithBow;
      public static HumanTexture trapperZombie;
      public static HumanTexture swampZombie;
      public static HumanTexture trenchcoatgoblin_stacked;
      public static GameTexture goblin;
      public static GameTexture trenchcoatgoblin_shoes;
      public static GameTexture trenchcoatgoblin_chestplate;
      public static GameTexture trenchcoatgoblin_helmet;
      public static HumanTexture enchantedZombie;
      public static HumanTexture enchantedZombieArcher;
      public static HumanTexture enchantedZombieArcherWithBow;
      public static HumanTexture ninja;
      public static HumanTexture vampire;
      public static HumanTexture frozenDwarf;
      public static HumanTexture voidApprentice;
      public static HumanTexture mummy;
      public static HumanTexture mummyMage;
      public static HumanTexture skeleton;
      public static HumanTexture skeletonMiner;
      public static HumanTexture skeletonMage;
      public static HumanTexture fishianHookWarrior;
      public static HumanTexture fishianHealer;
      public static HumanTexture fishianShaman;
      public static GameTexture voidApprentice_shadow;
      public static GameTexture frozenDwarfHair;
      public static GameTexture sandSpirit;
      public static GameTexture deepCaveSpirit;
      public static GameTexture swampSlime;
      public static GameTexture swampSlime_shadow;
      public static GameTexture cryoFlake;
      public static GameTexture cryoFlakePet;
      public static MobTexture crawlingZombie;
      public static MobTexture enchantedCrawlingZombie;
      public static MobTexture giantCaveSpider;
      public static MobTexture giantSnowCaveSpider;
      public static MobTexture giantSwampCaveSpider;
      public static MobTexture smallSwampCaveSpider;
      public static MobTexture jackal;
      public static MobTexture snowWolf;
      public static MobTexture giantScorpion;
      public static GameTexture swampShooter;
      public static GameTexture mole;
      public static GameTexture frostSentry;
      public static GameTexture giantSwampSlime;
      public static GameTexture giantSwampSlime_shadow;
      public static GameTexture sandWorm;
      public static GameTexture sandWorm_mask;
      public static GameTexture sandWorm_shadow;
      public static GameTexture desertCrawler;
      public static HumanTexture swampSkeleton;
      public static HumanTexture swampDweller;
      public static HumanTexture ancientSkeleton;
      public static HumanTexture ancientArmoredSkeleton;
      public static HumanTexture ancientSkeletonMage;
      public static MobTexture leggedSlime;
      public static MobTexture mageSlime;
      public static MobTexture ghostSlime;
      public static MobTexture warriorSlime;
      public static MobTexture slimeWorm;
      public static GameTexture slimeWorm_mask;
      public static GameTexture cryptBat;
      public static MobTexture phantom;
      public static HumanTexture cryptVampire;
      public static HumanTexture spiderkin;
      public static HumanTexture spiderkinWarrior;
      public static HumanTexture spiderkinArcher;
      public static HumanTexture spiderkinMage;
      public static GameTexture spiderkin_light;
      public static GameTexture spiderkinWarrior_light;
      public static GameTexture spiderkinArcher_light;
      public static GameTexture spiderkinMage_light;
      public static MobTexture webSpinner;
      public static MobTexture webSpinner_shadow;
      public static MobTexture bloatedSpider;
      public static MobTexture bloatedSpider_shadow;
      public static MobTexture staticJellyfish;
      public static GameTexture mosquitoEgg;
      public static GameTexture mosquito;
      public static GameTexture mosquito_shadow;
      public static HumanTexture crone;
      public static HumanTexture boneWalker;
      public static GameTexture forestSpector;
      public static GameTexture forestSpector_shadow;
      public static GameTexture dryadSentinel;
      public static GameTexture dryadSentinel_shadow;
      public static GameTexture flamelingShooter;
      public static HumanTexture crazedRaven;
      public static GameTexture arcanicPylon;
      public static GameTexture shark;
      public static GameTexture fakeShark;
      public static GameTexture mimic;
      public static GameTexture furnaceGolem;
      public static GameTexture furnaceGolem_light;
      public static GameTexture furnaceGolem_overheat;
      public static GameTexture furnaceGolem_overheat_light;
      public static GameTexture furnaceGolem_shadow;
      public static HumanTexture magechanic;
      public static GameTexture rcToySoldier;
      public static HumanTexture mutant1;
      public static HumanTexture mutant2;
      public static HumanTexture mutant3;
      public static GameTexture haywireShredder;
      public static GameTexture phoenix;
      public static GameTexture phoenix_shadow;
      public static GameTexture phoenixEgg;
      public static GameTexture phoenixEgg_shadow;
      public static GameTexture ashGolem;
      public static GameTexture ashGolem_shadow;
      public static GameTexture frostPiercer;
      public static GameTexture vampiricBat;
      public static GameTexture bee;
      public static GameTexture sentientSword;
      public static GameTexture sentientSword_shadow;
      public static GameTexture steelBoat;
      public static GameTexture runeboundBoat;
      public static GameTexture walkingTorch;
      public static GameTexture reaperSpiritPet;
      public static GameTexture jumpingBall;
      public static GameTexture jumpingBall_shadow;
      public static GameTexture cavelingElder;
      public static GameTexture poisonSlime;
      public static GameTexture poisonSlime_shadow;
      public static GameTexture dryadSpirit;
      public static GameTexture pouncingSlime;
      public static GameTexture pouncingSlime_shadow;
      public static GameTexture greatswordSlime;
      public static GameTexture greatswordSlime_shadow;
      public static MobTexture chargingPhantom;
      public static MobTexture orbOfSlimesSlime;
      public static MobTexture babySpider;
      public static MobTexture arachnidSpider;
      public static MobTexture babyCrawlingZombie;
      public static MobTexture babyDryad;
      public static MobTexture hoverBoard;
      public static MobTexture witchBroom;
      public static HumanTexture babyZombie;
      public static HumanTexture babyZombieArcher;
      public static HumanTexture babySnowman;
      public static HumanTexture babySkeleton;
      public static HumanTexture babySkeletonMage;
      public static HumanTexture babySpiderkinWarrior;
      public static HumanTexture babySpiderkinArcher;
      public static HumanTexture stabbyBush;
      public static HumanTexture bashyBush;
      public static HumanTexture ancestorKnight;
      public static HumanTexture ancestorMage;
      public static GameTexture duskMoonDisc;
      public static GameTexture rubyShield;
      public static GameTexture rubyShield_shadow;
      public static GameTexture rubyDragon;
      public static GameTexture emeraldPillar;
      public static GameTexture stabbyBush_shadow;
      public static GameTexture lifeEssence;
      public static GameTexture quickTinker;
      public static GameTexture willOWisp;
      public static GameTexture ghostlyBow;
      public static GameTexture locust;
      public static GameTexture babyThrumbo;
      public static GameTexture babyThrumbo_shadow;
      public static GameTexture dragonWhelp;
      public static GameTexture dragonWhelp_shadow;
      public static GameTexture icicleArmadillo;
      public static GameTexture icicleArmadillo_shadow;
      public static GameTexture wanderBot;
      public static GameTexture wanderBot_front;
      public static GameTexture wanderBot_shadow;
      public static GameTexture makeshiftTurret;
      public static GameTexture makeshiftArcThrower;
      public static HumanTexture makeshiftRobotSettler;
      public static GameTexture evilsProtector;
      public static GameTexture evilsProtector_shadow;
      public static GameTexture evilsProtector2;
      public static GameTexture evilsProtectorBomb;
      public static GameTexture evilsProtectorBomb_shadow;
      public static GameTexture portalMinion;
      public static GameTexture evilMinion;
      public static GameTexture queenSpiderBody;
      public static GameTexture queenSpiderHead;
      public static GameTexture queenSpiderLeg;
      public static GameTexture queenSpiderDebris;
      public static GameTexture queenSpider_shadow;
      public static GameTexture queenSpiderLeg_shadow;
      public static GameTexture queenSpider_spit;
      public static MobTexture spiderHatchling;
      public static GameTexture voidWizard2;
      public static GameTexture voidWizard3;
      public static GameTexture voidWizard_shadow;
      public static GameTexture parrot;
      public static HumanTexture voidWizard;
      public static HumanTexture pirateCaptain;
      public static HumanTexture pirateRecruit1;
      public static HumanTexture pirateRecruit2;
      public static HumanTexture pirateRecruit3;
      public static GameTexture pirateCaptainShip;
      public static GameTexture pirateCaptainShip_shadow;
      public static GameTexture pirateCaptainShip_mask;
      public static GameTexture ghostShip;
      public static GameTexture ancientVulture;
      public static GameTexture ancientVulture_shadow;
      public static GameTexture ancientVultureEgg;
      public static GameTexture ancientVultureEgg_shadow;
      public static GameTexture vultureHatchling;
      public static GameTexture vultureHatchling_shadow;
      public static GameTexture reaper;
      public static GameTexture reaper_shadow;
      public static GameTexture reaperGlow;
      public static GameTexture reaperSpirit;
      public static GameTexture reaperSpiritPortal;
      public static GameTexture cryoQueen;
      public static GameTexture swampGuardian;
      public static GameTexture swampGuardian_shadow;
      public static GameTexture swampGuardian_mask;
      public static GameTexture pestWarden;
      public static GameTexture pestWarden_shadow;
      public static GameTexture pestWarden_mask;
      public static GameTexture flyingSpirits;
      public static HumanTexture fallenWizard;
      public static GameTexture fallenWizardDragon;
      public static GameTexture nightSwarmBat;
      public static MobTexture motherSlime;
      public static GameTexture spiderEmpressHead;
      public static GameTexture spiderEmpressTorso;
      public static GameTexture spiderEmpressDress;
      public static GameTexture spiderEmpressLegTop;
      public static GameTexture spiderEmpressLegBottom;
      public static GameTexture spiderEmpressArmTop;
      public static GameTexture spiderEmpressArmBottom;
      public static GameTexture spiderEmpressHand;
      public static GameTexture spiderEmpressDebris;
      public static GameTexture spiderEmpressRageHead;
      public static GameTexture spiderEmpressRageTorso;
      public static GameTexture spiderEmpressRageDress;
      public static GameTexture spiderEmpressRageLegTop;
      public static GameTexture spiderEmpressRageLegBottom;
      public static GameTexture spiderEmpressRageArmTop;
      public static GameTexture spiderEmpressRageArmBottom;
      public static GameTexture spiderEmpressRageHand;
      public static GameTexture sunlightChampionEye;
      public static GameTexture sunlightChampionChestplate;
      public static GameTexture sunlightChampionJet;
      public static GameTexture sunlightGauntlet;
      public static GameTexture sunlightGauntletFire;
      public static GameTexture sunlightGauntletJet;
      public static GameTexture moonlightDancer;
      public static GameTexture moonlightDancerInvincible;
      public static GameTexture moonlightDancerDebris;
      public static GameTexture moonlightDancerHead;
      public static GameTexture crystalGolem;
      public static GameTexture crystalArmadillo;
      public static GameTexture crystalArmadillo_light;
      public static GameTexture crystalDragon;
      public static GameTexture crystalDragon_shadow;
      public static GameTexture crystalDragonHead;
      public static HumanTexture chieftain;
      public static GameTexture theCursedCrone;
      public static GameTexture theCursedCroneFrontEffects;
      public static GameTexture theCursedCroneBackEffects;
      public static GameTexture theCursedCrone_shadow;
      public static MobTexture spiritGhoul;
      public static GameTexture mutantHydraBody;
      public static GameTexture mutantHydraNecks;
      public static GameTexture mutantHydraHeads;
      public static GameTexture mutantHydraMap;
      public static GameTexture ascendedWizard_stage1;
      public static GameTexture ascendedWizard_stage2;
      public static GameTexture ascendedGauntlet;
      public static GameTexture ascendedGauntletJet;
      public static GameTexture ascendedGolem;
      public static GameTexture ascendedBat;
      public static GameTexture theVoidMapIcon;
      public static GameTexture theVoidHead;
      public static GameTexture theVoidClaw;
      public static GameTexture theVoidDebris;
      public static GameTexture mound1;
      public static GameTexture mound2;
      public static GameTexture mound3;
      public static GameTexture mounds32;
      public static GameTexture mountmask;
      public static GameTexture swimmask;
      public static GameTexture boat_shadow;
      public static GameTexture woodBoat;
      public static GameTexture minecart;
      public static GameTexture minecart_shadow;
      public static GameTexture bumperCar;
      public static GameTexture bumperCarOverlay;
      public static GameTexture bumperCar_shadow;
      public static GameTexture sawblade;
      public static GameTexture explosiveBarrel;
      public static GameTexture[] boat_mask;
      public static GameTexture[] runeboundboat_mask;
      public static GameTexture[] minecart_mask;
      public static GameTexture polarBear;
      public static GameTexture polarBear_shadow;
      public static GameTexture grizzlyBear;
      public static GameTexture grizzlyBear_shadow;
      public static GameTexture grizzlyBearCub;
      public static GameTexture pug;
      public static GameTexture pug_shadow;
      public static GameTexture portalSphere;
      public static GameTexture spawnSphere;
      public static GameTexture bossPortal;
      public static GameTexture ravenlords_set_feather;
      public static GameTexture crocodile;
      public static GameTexture crocodile_shadow;
      public static GameTexture necroticflaskdebris;
      public static GameTexture unlabeledpotiondebris;
      public static GameTexture bigBoneSpike;
      public static GameTexture bigEarthSpike;
      public static GameTexture unknownRaid;
      public static GameTexture grizzbolt_body;
      public static GameTexture grizzbolt_arms;
      public static GameTexture grizzbolt_minigun;
      public static GameTexture grizzbolt_shadow;
      public static GameTexture thrumbo;
      public static GameTexture thrumbo_shadow;

      public static void load() {
         human_shadow = fromFile("human_shadow");
         human_baby_shadow = fromFile("human_baby_shadow");
         human_big_shadow = fromFile("human_big_shadow");
         small_shadow = fromFile("small_shadow");
         human_enchained_iron = GameTexture.fromFile("player/armor/enchained_iron");
         sheep = fromFile("sheep");
         sheep_sheared = fromFile("sheep_sheared");
         sheep_shadow = fromFile("sheep_shadow");
         ram = fromFile("ram");
         ram_sheared = fromFile("ram_sheared");
         lamb = fromFile("lamb");
         lamb_shadow = fromFile("lamb_shadow");
         ostrich = fromFile("ostrich");
         ostrichMount = fromFile("ostrichmount");
         ostrich_shadow = fromFile("ostrich_shadow");
         seahorse_front = fromFile("seahorse_front");
         seahorse_back = fromFile("seahorse_back");
         seahorse_shadow = fromFile("seahorse_shadow");
         cow = fromFile("cow");
         cow_shadow = fromFile("cow_shadow");
         bull = fromFile("bull");
         calf = fromFile("calf");
         calf_shadow = fromFile("calf_shadow");
         pig = fromFile("pig");
         boar = fromFile("boar");
         pig_shadow = fromFile("pig_shadow");
         piglet = fromFile("piglet");
         piglet_shadow = fromFile("piglet_shadow");
         penguin = fromFile("penguin");
         chicken = fromFile("chicken");
         chicken_shadow = fromFile("chicken_shadow");
         rooster = fromFile("rooster");
         rooster_shadow = fromFile("rooster_shadow");
         chick = fromFile("chick");
         chick_shadow = fromFile("chick_shadow");
         honeyBee = fromFiles("honeybee");
         queenBee = fromFiles("queenbee");
         junkMech = fromFile("junkmech");
         junkMech_shadow = fromFile("junkmech_shadow");
         junkMech_legs = fromFile("junkmech_legs");
         junkMech_guns = fromFile("junkmech_guns");
         rabbit = fromFile("rabbit");
         squirrel = fromFile("squirrel");
         snowHare = fromFile("snowhare");
         crab = fromFiles("crab");
         scorpion = fromFiles("scorpion");
         turtle = fromFiles("turtle");
         swampSlug = fromFile("swampslug");
         frog = fromFiles("frog");
         duck = fromFiles("duck");
         bird_shadow = fromFile("bird_shadow");
         bird = fromFile("bird");
         bluebird = fromFile("bluebird");
         canaryBird = fromFile("canary");
         cardinalBird = fromFile("cardinal");
         spider = fromFiles("spider");
         mouse = fromFiles("mouse");
         butterfly = fromFiles("butterfly");
         firefly = fromFiles("firefly");
         beetCroppler = fromFiles("cropplerbeet");
         carrotCroppler = fromFiles("cropplercarrot");
         onionCroppler = fromFiles("croppleronion");
         potatoCroppler = fromFiles("cropplerpotato");
         pumpkinCroppler = fromFiles("cropplerpumpkin");
         stoneCaveling = new HumanTexture(fromFile("stonecaveling"), fromFile("stonecavelingarms_front"), fromFile("stonecavelingarms_back"));
         snowStoneCaveling = new HumanTexture(fromFile("snowstonecaveling"), fromFile("snowstonecavelingarms_front"), fromFile("snowstonecavelingarms_back"));
         graniteCaveling = new HumanTexture(fromFile("granitecaveling"), fromFile("granitecavelingarms_front"), fromFile("granitecavelingarms_back"));
         swampStoneCaveling = new HumanTexture(
            fromFile("swampstonecaveling"), fromFile("swampstonecavelingarms_front"), fromFile("swampstonecavelingarms_back")
         );
         sandStoneCaveling = new HumanTexture(fromFile("sandstonecaveling"), fromFile("sandstonecavelingarms_front"), fromFile("sandstonecavelingarms_back"));
         deepStoneCaveling = new HumanTexture(fromFile("deepstonecaveling"), fromFile("deepstonecavelingarms_front"), fromFile("deepstonecavelingarms_back"));
         deepSnowStoneCaveling = new HumanTexture(
            fromFile("deepsnowstonecaveling"), fromFile("deepsnowstonecavelingarms_front"), fromFile("deepsnowstonecavelingarms_back")
         );
         dryadCaveling = new HumanTexture(fromFile("dryadcaveling"), fromFile("dryadcavelingarms_front"), fromFile("dryadcavelingarms_back"));
         deepSwampStoneCaveling = new HumanTexture(
            fromFile("deepswampstonecaveling"), fromFile("deepswampstonecavelingarms_front"), fromFile("deepswampstonecavelingarms_back")
         );
         deepSandStoneCaveling = new HumanTexture(
            fromFile("deepsandstonecaveling"), fromFile("deepsandstonecavelingarms_front"), fromFile("deepsandstonecavelingarms_back")
         );
         flameling = new HumanTexture(fromFile("flameling"), fromFile("flamelingarms_front"), fromFile("flamelingarms_back"));
         incursionCaveling = new HumanTexture(fromFile("incursioncaveling"), fromFile("incursioncavelingarms_front"), fromFile("incursioncavelingarms_back"));
         caveling_shadow = fromFile("caveling_shadow");
         zombie = humanTexture("zombie");
         zombieArcher = humanTexture("zombiearcher");
         zombieArcherWithBow = humanTexture("zombiearcherbow", "zombiearcherarms");
         trapperZombie = humanTexture("trapperzombie", "zombiearms");
         swampZombie = humanTexture("swampzombie", "swampzombiearms");
         goblin = fromFile("goblin");
         trenchcoatgoblin_stacked = humanTexture("trenchcoatgoblin_stacked", "trenchcoatgoblin_stacked_arms");
         trenchcoatgoblin_helmet = fromFile("trenchcoatgoblin_helmet");
         trenchcoatgoblin_chestplate = fromFile("trenchcoatgoblin_chestplate");
         trenchcoatgoblin_shoes = fromFile("trenchcoatgoblin_shoes");
         mummy = humanTexture("mummy");
         mummyMage = humanTexture("mummymage");
         vampire = humanTexture("vampire");
         frozenDwarf = humanTexture("frozendwarf");
         frozenDwarfHair = fromFile("frozendwarfhair1");
         enchantedZombie = humanTexture("enchantedzombie");
         enchantedZombieArcher = humanTexture("enchantedzombiearcher");
         enchantedZombieArcherWithBow = humanTexture("enchantedzombiearcherbow", "enchantedzombiearms");
         ninja = humanTexture("ninja");
         voidApprentice = humanTexture("voidapprentice");
         voidApprentice_shadow = fromFile("voidapprentice_shadow");
         sandSpirit = fromFile("sandspirit");
         skeleton = humanTexture("skeleton");
         deepCaveSpirit = fromFile("deepcavespirit");
         skeletonMiner = humanTexture("skeletonminer");
         skeletonMage = humanTexture("skeletonmage");
         fishianHookWarrior = humanTexture("fishianhookwarrior");
         fishianHealer = humanTexture("fishianhealer");
         fishianShaman = humanTexture("fishianshaman");
         swampSlime = fromFile("swampslime");
         swampSlime_shadow = fromFile("swampslime_shadow");
         crawlingZombie = fromFiles("crawlingzombie");
         enchantedCrawlingZombie = fromFiles("enchantedcrawlingzombie", "crawlingzombie_shadow");
         giantCaveSpider = fromFiles("giantcavespider", "giantspider_shadow");
         giantSnowCaveSpider = fromFiles("giantsnowcavespider", "giantspider_shadow");
         giantSwampCaveSpider = fromFiles("giantswampcavespider", "giantspider_shadow");
         smallSwampCaveSpider = fromFiles("smallswampcavespider", "smallswampcavespider_shadow");
         jackal = fromFiles("jackal", "wolf_shadow");
         snowWolf = fromFiles("snowwolf", "wolf_shadow");
         giantScorpion = fromFiles("giantscorpion", "giantscorpion_shadow");
         cryoFlake = fromFile("cryoflake");
         swampShooter = fromFile("swampshooter");
         mole = fromFile("mole");
         frostSentry = fromFile("frostsentry");
         giantSwampSlime = fromFile("giantswampslime");
         giantSwampSlime_shadow = fromFile("giantswampslime_shadow");
         swampDweller = humanTexture("swampdweller");
         sandWorm = fromFile("sandworm");
         sandWorm_shadow = fromFile("sandworm_shadow");
         sandWorm_mask = fromFile("sandworm_mask");
         desertCrawler = fromFile("desertcrawler");
         swampSkeleton = humanTexture("swampskeleton");
         ancientSkeleton = humanTexture("ancientskeleton");
         ancientArmoredSkeleton = humanTexture("ancientarmoredskeleton");
         ancientSkeletonMage = humanTexture("ancientskeletonmage");
         leggedSlime = fromFiles("leggedslime");
         mageSlime = fromFiles("mageslime");
         ghostSlime = fromFiles("ghostslime");
         warriorSlime = fromFiles("warriorslime");
         slimeWorm = fromFiles("slimeworm");
         slimeWorm_mask = fromFile("slimeworm_mask");
         cryptBat = fromFile("cryptbat");
         phantom = fromFiles("phantom");
         cryptVampire = humanTexture("cryptvampire");
         spiderkin = humanTexture("spiderkin/spiderkin");
         spiderkinWarrior = humanTexture("spiderkin/spiderkinwarrior");
         spiderkinArcher = humanTexture("spiderkin/spiderkinarcher");
         spiderkinMage = humanTexture("spiderkin/spiderkinmage");
         spiderkin_light = fromFile("spiderkin/spiderkin_light");
         spiderkinWarrior_light = fromFile("spiderkin/spiderkinwarrior_light");
         spiderkinArcher_light = fromFile("spiderkin/spiderkinarcher_light");
         spiderkinMage_light = fromFile("spiderkin/spiderkinmage_light");
         webSpinner = fromFiles("webspinner");
         webSpinner_shadow = fromFiles("webspinner_shadow");
         bloatedSpider = fromFiles("bloatedspider");
         bloatedSpider_shadow = fromFiles("bloatedspider_shadow");
         staticJellyfish = fromFiles("staticjellyfish");
         mosquitoEgg = fromFile("mosquitoegg");
         mosquito = fromFile("mosquito");
         mosquito_shadow = fromFile("mosquito_shadow");
         crone = humanTexture("crone");
         boneWalker = humanTexture("bonewalker");
         forestSpector = fromFile("forestspector");
         forestSpector_shadow = fromFile("forestspector_shadow");
         dryadSentinel = fromFile("dryadsentinel");
         dryadSentinel_shadow = fromFile("dryadsentinel_shadow");
         flamelingShooter = fromFile("flamelingshooter");
         crazedRaven = humanTexture("crazedraven");
         arcanicPylon = fromFile("arcanicpylon");
         shark = fromFile("shark");
         fakeShark = fromFile("fakeshark");
         mimic = fromFile("mimic");
         furnaceGolem = fromFile("furnacegolem");
         furnaceGolem_light = fromFile("furnacegolem_light");
         furnaceGolem_overheat = fromFile("furnacegolem_overheat");
         furnaceGolem_overheat_light = fromFile("furnacegolem_overheat_light");
         furnaceGolem_shadow = fromFile("furnacegolem_shadow");
         magechanic = humanTexture("magechanic");
         rcToySoldier = fromFile("rctoysoldier");
         mutant1 = humanTexture("mutant1");
         mutant2 = humanTexture("mutant2");
         mutant3 = humanTexture("mutant3");
         haywireShredder = fromFile("haywireshredder");
         phoenix = fromFile("phoenix");
         phoenix_shadow = fromFile("phoenix_shadow");
         phoenixEgg = fromFile("phoenixegg");
         phoenixEgg_shadow = fromFile("phoenixegg_shadow");
         ashGolem = fromFile("ashgolem");
         frostPiercer = fromFile("frostpiercer");
         vampiricBat = fromFile("vampiricbat");
         bee = fromFile("bee");
         sentientSword = fromFile("sentientsword");
         sentientSword_shadow = fromFile("sentientsword_shadow");
         steelBoat = fromFile("steelboat");
         runeboundBoat = fromFile("runeboundboat");
         walkingTorch = fromFile("walkingtorch");
         babyZombie = humanTexture("babyzombie");
         babyZombieArcher = humanTexture("babyzombiearcher", "babyzombiearms");
         babySnowman = humanTexture("babysnowman");
         poisonSlime = fromFile("poisonslime");
         poisonSlime_shadow = fromFile("poisonslime_shadow");
         dryadSpirit = fromFile("dryadspirit");
         pouncingSlime = fromFile("pouncingslime");
         pouncingSlime_shadow = fromFile("pouncingslime_shadow");
         greatswordSlime = fromFile("greatswordslime");
         greatswordSlime_shadow = fromFile("greatswordslime_shadow");
         chargingPhantom = fromFiles("playerchargingphantom");
         orbOfSlimesSlime = fromFiles("orbofslimesslime");
         reaperSpiritPet = fromFile("playerreaperspirit");
         jumpingBall = fromFile("jumpingball");
         jumpingBall_shadow = fromFile("jumpingball_shadow");
         babySpider = fromFiles("babyspider");
         babyCrawlingZombie = fromFiles("babycrawlingzombie");
         babyDryad = fromFiles("babydryad");
         cryoFlakePet = fromFile("playercryoflake");
         hoverBoard = fromFiles("hoverboard");
         witchBroom = fromFiles("witchbroom");
         ancestorKnight = humanTexture("ancestorknight");
         ancestorMage = humanTexture("ancestormage");
         babySkeleton = humanTexture("babyskeleton");
         babySkeletonMage = humanTexture("babyskeletonmage");
         babySpiderkinWarrior = humanTexture("babyspiderkinwarrior");
         babySpiderkinArcher = humanTexture("babyspiderkinarcher");
         babyThrumbo = fromFile("babythrumbo");
         babyThrumbo_shadow = fromFile("babythrumbo_shadow");
         dragonWhelp = fromFile("dragonwhelp");
         dragonWhelp_shadow = fromFile("dragonwhelp_shadow");
         icicleArmadillo = fromFile("iciclearmadillo");
         icicleArmadillo_shadow = fromFile("iciclearmadillo_shadow");
         stabbyBush = humanTexture("stabbybush");
         bashyBush = humanTexture("bashybush");
         stabbyBush_shadow = fromFile("stabbybush_shadow");
         duskMoonDisc = fromFile("duskmoondisc");
         cavelingElder = fromFile("cavelingelder");
         grizzlyBearCub = fromFile("grizzlycub");
         pug = fromFile("pug");
         pug_shadow = fromFile("pug_shadow");
         crystalGolem = fromFile("crystalgolem");
         crystalArmadillo = fromFile("crystalarmadillo");
         crystalArmadillo_light = fromFile("crystalarmadillo_light");
         crystalDragon = fromFile("crystaldragon");
         crystalDragon_shadow = fromFile("crystaldragon_shadow");
         crystalDragonHead = fromFile("crystaldragonhead");
         rubyShield = fromFile("rubyshield");
         rubyShield_shadow = fromFile("rubyshield_shadow");
         rubyDragon = fromFile("rubydragon");
         emeraldPillar = fromFile("emeraldpillar");
         lifeEssence = fromFile("lifeessence");
         quickTinker = fromFile("quicktinker");
         willOWisp = fromFile("willowisp");
         ghostlyBow = fromFile("ghostlybow");
         locust = fromFile("locust");
         wanderBot = fromFile("wanderbot");
         wanderBot_front = fromFile("wanderbot_front");
         wanderBot_shadow = fromFile("wanderbot_shadow");
         makeshiftTurret = fromFile("makeshiftturret");
         makeshiftArcThrower = fromFile("makeshiftarcthrower");
         makeshiftRobotSettler = humanTexture("makeshiftrobotsettler");
         evilsProtector = fromFile("evilsprotector");
         evilsProtector_shadow = fromFile("evilsprotector_shadow");
         evilsProtector2 = fromFile("evilsprotector2");
         evilsProtectorBomb = fromFile("evilsprotectorbomb");
         evilsProtectorBomb_shadow = fromFile("evilsprotectorbomb_shadow");
         portalMinion = fromFile("portalminion");
         evilMinion = fromFile("evilminion");
         queenSpiderBody = fromFile("queenspiderbody");
         queenSpiderHead = fromFile("queenspiderhead");
         queenSpiderLeg = fromFile("queenspiderleg");
         queenSpiderDebris = fromFile("queenspiderdebris");
         queenSpider_shadow = fromFile("queenspider_shadow");
         queenSpiderLeg_shadow = fromFile("queenspiderleg_shadow");
         queenSpider_spit = fromFile("queenspider_spit");
         spiderHatchling = fromFiles("spiderhatchling");
         arachnidSpider = fromFiles("arachnidspider");
         voidWizard = humanTexture("voidwizard");
         voidWizard2 = fromFile("voidwizard2");
         voidWizard3 = fromFile("voidwizard3");
         voidWizard_shadow = fromFile("voidwizard_shadow");
         pirateCaptain = humanTexture("pirates/piratecaptain");
         pirateCaptainShip = fromFile("pirates/piratecaptainship");
         pirateCaptainShip_shadow = fromFile("pirates/piratecaptainship_shadow");
         pirateCaptainShip_mask = fromFile("pirates/piratecaptainship_mask");
         ghostShip = fromFile("ghostship");
         pirateRecruit1 = humanTexture("pirates/pirate1");
         pirateRecruit2 = humanTexture("pirates/pirate2");
         pirateRecruit3 = humanTexture("pirates/pirate3");
         parrot = fromFile("pirates/parrot");
         ancientVulture = fromFile("ancientvulture");
         ancientVulture_shadow = fromFile("ancientvulture_shadow");
         ancientVultureEgg = fromFile("ancientvultureegg");
         ancientVultureEgg_shadow = fromFile("ancientvultureegg_shadow");
         vultureHatchling = fromFile("vulturehatchling");
         vultureHatchling_shadow = fromFile("vulturehatchling_shadow");
         reaper = fromFile("reaper");
         reaperGlow = fromFile("reaperglow");
         reaper_shadow = fromFile("reaper_shadow");
         reaperSpirit = fromFile("reaperspirit");
         reaperSpiritPortal = fromFile("reaperspiritportal");
         cryoQueen = fromFile("cryoqueen");
         swampGuardian = fromFile("swampguardian");
         swampGuardian_shadow = fromFile("swampguardian_shadow");
         swampGuardian_mask = fromFile("swampguardian_mask");
         pestWarden = fromFile("pestwarden");
         pestWarden_shadow = fromFile("pestwarden_shadow");
         pestWarden_mask = fromFile("pestwarden_mask");
         flyingSpirits = fromFile("flyingspirits");
         fallenWizard = humanTexture("fallenwizard");
         fallenWizardDragon = fromFile("fallenwizarddragon");
         nightSwarmBat = fromFile("nightswarmbat");
         motherSlime = fromFiles("motherslime");
         spiderEmpressHead = fromFile("spiderempress_head");
         spiderEmpressTorso = fromFile("spiderempress_torso");
         spiderEmpressDress = fromFile("spiderempress_dress");
         spiderEmpressLegTop = fromFile("spiderempress_legtop");
         spiderEmpressLegBottom = fromFile("spiderempress_legbottom");
         spiderEmpressArmTop = fromFile("spiderempress_armtop");
         spiderEmpressArmBottom = fromFile("spiderempress_armbottom");
         spiderEmpressHand = fromFile("spiderempress_hand");
         spiderEmpressRageHead = fromFile("spiderempressrage_head");
         spiderEmpressRageTorso = fromFile("spiderempressrage_torso");
         spiderEmpressRageDress = fromFile("spiderempressrage_dress");
         spiderEmpressRageLegTop = fromFile("spiderempressrage_legtop");
         spiderEmpressRageLegBottom = fromFile("spiderempressrage_legbottom");
         spiderEmpressRageArmTop = fromFile("spiderempressrage_armtop");
         spiderEmpressRageArmBottom = fromFile("spiderempressrage_armbottom");
         spiderEmpressRageHand = fromFile("spiderempressrage_hand");
         spiderEmpressDebris = fromFile("spiderempress_debris");
         sunlightChampionEye = fromFile("sunlightchampioneye");
         sunlightChampionChestplate = fromFile("sunlightchampionchestplate");
         sunlightChampionJet = fromFile("sunlightchampionjet");
         sunlightGauntlet = fromFile("sunlightgauntlet");
         sunlightGauntletFire = fromFile("sunlightgauntletfire");
         sunlightGauntletJet = fromFile("sunlightgauntletjet");
         moonlightDancer = fromFile("moonlightdancer");
         moonlightDancerInvincible = fromFile("moonlightdancerinvincible");
         moonlightDancerDebris = fromFile("moonlightdancerdebris");
         moonlightDancerHead = fromFile("moonlightdancerhead");
         chieftain = humanTexture("chieftain");
         theCursedCrone = fromFile("thecursedcrone");
         theCursedCroneFrontEffects = fromFile("thecursedcronefronteffects");
         theCursedCroneBackEffects = fromFile("thecursedcronebackeffects");
         theCursedCrone_shadow = fromFile("thecursedcrone_shadow");
         spiritGhoul = fromFiles("spiritghoul");
         mutantHydraBody = fromFile("mutanthydrabody");
         mutantHydraNecks = fromFile("mutanthydranecks");
         mutantHydraHeads = fromFile("mutanthydraheads");
         mutantHydraMap = fromFile("mutanthydramap");
         ascendedWizard_stage1 = fromFile("ascendedwizard_stage1");
         ascendedWizard_stage2 = fromFile("ascendedwizard_stage2");
         ascendedGauntlet = fromFile("ascendedgauntlet");
         ascendedGauntletJet = fromFile("ascendedgauntletjet");
         ascendedGolem = fromFile("ascendedgolem");
         ascendedBat = fromFile("ascendedbat");
         theVoidMapIcon = fromFile("thevoid_mapicon");
         theVoidHead = fromFile("thevoid_head");
         theVoidClaw = fromFile("thevoid_claw");
         theVoidDebris = fromFile("thevoid_debris");
         mound1 = fromFile("mound1");
         mound2 = fromFile("mound2");
         mound3 = fromFile("mound3");
         mounds32 = fromFile("mounds32");
         mountmask = GameTexture.fromFile("mobs/mountmask");
         swimmask = GameTexture.fromFile("mobs/swimmask");
         boat_shadow = fromFile("boat_shadow");
         woodBoat = fromFile("woodboat");
         GameTexture boat_mask_sprites = fromFile("boatmask");
         int boatSprites = boat_mask_sprites.getHeight() / 64;
         boat_mask = new GameTexture[boatSprites];

         for (int i = 0; i < boatSprites; i++) {
            boat_mask[i] = new GameTexture(boat_mask_sprites, 0, i, 64);
         }

         GameTexture runeboundBoat_mask_sprites = fromFile("runeboundboatmask");
         int runeboundBoatSprites = runeboundBoat_mask_sprites.getHeight() / 64;
         runeboundboat_mask = new GameTexture[runeboundBoatSprites];

         for (int i = 0; i < runeboundBoatSprites; i++) {
            runeboundboat_mask[i] = new GameTexture(runeboundBoat_mask_sprites, 0, i, 64);
         }

         minecart = fromFile("minecart");
         minecart_shadow = fromFile("minecart_shadow");
         GameTexture minecart_mask_sprites = fromFile("minecart_mask");
         int minecartSprites = minecart_mask_sprites.getHeight() / 64;
         minecart_mask = new GameTexture[minecartSprites];

         for (int i = 0; i < minecartSprites; i++) {
            minecart_mask[i] = new GameTexture(minecart_mask_sprites, 0, i, 64);
         }

         bumperCar = fromFile("bumpercar");
         bumperCarOverlay = fromFile("bumpercar_overlay");
         bumperCar_shadow = fromFile("bumpercar_shadow");
         sawblade = fromFile("sawblade");
         explosiveBarrel = fromFile("explosivebarrel");
         polarBear = fromFile("polarbear");
         polarBear_shadow = fromFile("polarbear_shadow");
         grizzlyBear = fromFile("grizzly");
         grizzlyBear_shadow = fromFile("grizzly_shadow");
         crocodile = fromFile("crocodile");
         portalSphere = fromFile("portalsphere");
         spawnSphere = fromFile("spawnsphere");
         bossPortal = fromFile("bossportal");
         ravenlords_set_feather = fromFile("ravenlordssetfeather");
         necroticflaskdebris = GameTexture.fromFile("particles/necroticflaskdebris");
         unlabeledpotiondebris = GameTexture.fromFile("particles/unlabeledpotiondebris");
         bigBoneSpike = GameTexture.fromFile("particles/bigbonespike");
         bigEarthSpike = GameTexture.fromFile("particles/bigearthspike");
         unknownRaid = GameTexture.fromFile("mobs/icons/unknownraid");
         grizzbolt_body = fromFile("grizzbolt_body");
         grizzbolt_arms = fromFile("grizzbolt_arms");
         grizzbolt_minigun = fromFile("grizzbolt_minigun");
         grizzbolt_shadow = fromFile("grizzbolt_shadow");
         thrumbo = fromFile("thrumbo");
         thrumbo_shadow = fromFile("thrumbo_shadow");
         GameTexture overlay = GameTexture.fromFile("objects/breakobjectoverlay", true);
      }

      public static GameTexture fromFile(String path) {
         return GameTexture.fromFile("mobs/" + path);
      }

      public static GameTexture fromFile(String path, GameTexture defaultNotFound) {
         return GameTexture.fromFile("mobs/" + path, defaultNotFound);
      }

      public static MobTexture fromFiles(String path, String shadowPath) {
         return new MobTexture(fromFile(path), fromFile(shadowPath));
      }

      public static MobTexture fromFiles(String path) {
         return fromFiles(path, path + "_shadow");
      }

      public static HumanTexture humanTexture(String path, String armsPath) {
         return new HumanTexture(fromFile(path), fromFile(armsPath + "_left"), fromFile(armsPath + "_right"));
      }

      public static HumanTexture humanTexture(String path) {
         return humanTexture(path, path + "arms");
      }

      public static HumanTextureFull humanTextureFull(
         String headPath,
         String backHeadPath,
         String eyelidsPath,
         String hairPath,
         String backHairPath,
         String bodyPath,
         String backBodyPath,
         String armsPath,
         String backArmsPath,
         String feetPath,
         String backFeetPath
      ) {
         return new HumanTextureFull(
            fromFile(headPath, null),
            fromFile(backHeadPath, null),
            fromFile(eyelidsPath, null),
            fromFile(hairPath, null),
            fromFile(backHairPath, null),
            fromFile(bodyPath),
            fromFile(backBodyPath, null),
            fromFile(armsPath + "_left"),
            fromFile(backArmsPath + "_left", null),
            fromFile(armsPath + "_right"),
            fromFile(backArmsPath + "_right", null),
            fromFile(feetPath),
            fromFile(backFeetPath, null)
         );
      }

      public static HumanTextureFull humanTextureFull(String path) {
         return humanTextureFull(
            path + "head",
            path + "head_back",
            path + "eyelids",
            path + "hair",
            path + "hair_back",
            path + "body",
            path + "body_back",
            path + "arms",
            path + "arms_back",
            path + "feet",
            path + "feet_back"
         );
      }
   }
}
