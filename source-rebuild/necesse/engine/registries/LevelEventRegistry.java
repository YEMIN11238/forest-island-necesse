package necesse.engine.registries;

import java.lang.reflect.InvocationTargetException;
import necesse.engine.GameLoadingScreen;
import necesse.engine.GameLog;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modLoader.LoadedMod;
import necesse.entity.levelEvent.ArcanicPylonLightningLevelEvent;
import necesse.entity.levelEvent.AscendedBatJailLevelEvent;
import necesse.entity.levelEvent.AscendedBlackHoleEvent;
import necesse.entity.levelEvent.AscendedFractureGroundEvent;
import necesse.entity.levelEvent.AscendedIncursionEvent;
import necesse.entity.levelEvent.BloodGrimoireParticleLevelEvent;
import necesse.entity.levelEvent.BounceGlyphTrapEvent;
import necesse.entity.levelEvent.ChainLightningEffectLevelEvent;
import necesse.entity.levelEvent.ChickenGlyphTrapEvent;
import necesse.entity.levelEvent.ChieftainGauntletEvent;
import necesse.entity.levelEvent.EmpressAcidGroundEvent;
import necesse.entity.levelEvent.ExtractionIncursionEvent;
import necesse.entity.levelEvent.FallenWizardRespawnEvent;
import necesse.entity.levelEvent.FallingIcicleEvent;
import necesse.entity.levelEvent.FlameTrapEvent;
import necesse.entity.levelEvent.FurnaceGolemOverheatEvent;
import necesse.entity.levelEvent.HuntIncursionEvent;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.OverChargedManaHitsLevelEvent;
import necesse.entity.levelEvent.ReverseDamageGlyphTrapEvent;
import necesse.entity.levelEvent.ShowAttackMobTickEvent;
import necesse.entity.levelEvent.SmokePuffCloudLevelEvent;
import necesse.entity.levelEvent.SmokePuffLevelEvent;
import necesse.entity.levelEvent.SpikeTrapEvent;
import necesse.entity.levelEvent.SpiritCorruptedLevelEvent;
import necesse.entity.levelEvent.SwordCleanSliceAttackMobEvent;
import necesse.entity.levelEvent.TeleportEvent;
import necesse.entity.levelEvent.TeleportFailEvent;
import necesse.entity.levelEvent.TempleEntranceEvent;
import necesse.entity.levelEvent.TheVoidBlackHoleGroundEvent;
import necesse.entity.levelEvent.TheVoidClawGroundShatterGroundEvent;
import necesse.entity.levelEvent.TheVoidMovingRainLevelEvent;
import necesse.entity.levelEvent.TicTacToeLevelEvent;
import necesse.entity.levelEvent.TrialIncursionEvent;
import necesse.entity.levelEvent.WaitForSecondsEvent;
import necesse.entity.levelEvent.IncursionPerkModifiers.FlamelingsCanDiePerkLevelEvent;
import necesse.entity.levelEvent.IncursionPerkModifiers.MobsDropAlchemyShardsPerkLevelEvent;
import necesse.entity.levelEvent.IncursionPerkModifiers.MobsDropAltarDustPerkLevelEvent;
import necesse.entity.levelEvent.IncursionPerkModifiers.MobsDropMoreAltarDustPerkLevelEvent;
import necesse.entity.levelEvent.IncursionPerkModifiers.MobsDropUpgradeShardsPerkLevelEvent;
import necesse.entity.levelEvent.IncursionPerkModifiers.MobsExplodeUpgradeShardsPerkLevelEvent;
import necesse.entity.levelEvent.explosionEvent.ArcanicPylonExplosionLevelEvent;
import necesse.entity.levelEvent.explosionEvent.AscendedBombExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.AscendedLightningExplosionLevelEvent;
import necesse.entity.levelEvent.explosionEvent.AscendedPushExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.BloatedSpiderExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.BombExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.BoneSpikeMobExplosionLevelEvent;
import necesse.entity.levelEvent.explosionEvent.BoulderHitExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.CannonBallExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.CaptainCannonBallExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.CursedCroneSpiritBeamsExplosionLevelEvent;
import necesse.entity.levelEvent.explosionEvent.DynamiteExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.ElectricOrbExplosionLevelEvent;
import necesse.entity.levelEvent.explosionEvent.ExplosiveModifierChargeUpLevelEvent;
import necesse.entity.levelEvent.explosionEvent.ExplosiveModifierExplosionLevelEvent;
import necesse.entity.levelEvent.explosionEvent.FlamelingsModifierSmokePuffLevelEvent;
import necesse.entity.levelEvent.explosionEvent.GhostSkullExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.MiniRocketExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.MobsExplodeUpgradeShardsPerkExplosionLevelEvent;
import necesse.entity.levelEvent.explosionEvent.PatchworkGrenadeExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.RebornExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.RubyStaffExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.SoulseedExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.StabbyBushExplosionLevelEvent;
import necesse.entity.levelEvent.explosionEvent.StormingModifierExplosionLevelEvent;
import necesse.entity.levelEvent.explosionEvent.SupernovaExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.TNTExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.VoidRainExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.VoidWizardExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.splashEvent.DamageSplashEvent;
import necesse.entity.levelEvent.explosionEvent.splashEvent.FreezeSplashEvent;
import necesse.entity.levelEvent.explosionEvent.splashEvent.HealSplashEvent;
import necesse.entity.levelEvent.explosionEvent.splashEvent.LocustDeathSplashEvent;
import necesse.entity.levelEvent.explosionEvent.splashEvent.NecroPoisonSplashEvent;
import necesse.entity.levelEvent.explosionEvent.splashEvent.PolymorphSplashEvent;
import necesse.entity.levelEvent.explosionEvent.splashEvent.SimpleSplashEvent;
import necesse.entity.levelEvent.explosionEvent.splashEvent.SlimeSplashEvent;
import necesse.entity.levelEvent.explosionEvent.splashEvent.SmiteSplashEvent;
import necesse.entity.levelEvent.fishingEvent.FishingEvent;
import necesse.entity.levelEvent.incursionModifiers.AlchemicalInterferenceModifierLevelEvent;
import necesse.entity.levelEvent.incursionModifiers.AlchemyShardsDropPotionsLevelEvent;
import necesse.entity.levelEvent.incursionModifiers.AllBannerBuffsLevelEvent;
import necesse.entity.levelEvent.incursionModifiers.AscendedShardsBossDropLevelEvent;
import necesse.entity.levelEvent.incursionModifiers.ChanceToMineFullClusterLevelEvent;
import necesse.entity.levelEvent.incursionModifiers.CrawlmageddonModifierLevelEvent;
import necesse.entity.levelEvent.incursionModifiers.DoubleBossChanceLevelEvent;
import necesse.entity.levelEvent.incursionModifiers.EmpowermentBuffsModifierLevelEvent;
import necesse.entity.levelEvent.incursionModifiers.EnableBannerOfWarLevelEvent;
import necesse.entity.levelEvent.incursionModifiers.ExplosiveModifierLevelEvent;
import necesse.entity.levelEvent.incursionModifiers.FlamelingsModifierLevelEvent;
import necesse.entity.levelEvent.incursionModifiers.FrenzyModifierLevelEvent;
import necesse.entity.levelEvent.incursionModifiers.ModifiersAffectEnemiesLevelEvent;
import necesse.entity.levelEvent.incursionModifiers.SecondLifePerkLevelEvent;
import necesse.entity.levelEvent.incursionModifiers.StormingModifierLevelEvent;
import necesse.entity.levelEvent.incursionModifiers.TremorsModifierLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.AmethystGlyphEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.AncientDredgingStaffEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.AscendedLightningLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.AscendedPylonChargeUpAttackLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.AscendedSlimeQuakeEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.AscendedSlimeQuakeWarningEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.BrutesBattleaxeDashLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.CaveSpiderSpitEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.CaveSpiderWebEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.ChargeBeamWarningLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.ChargeShowerLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.ChieftainDashLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.CoreKeeperCoreChatLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.CryoStormLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.CrystalBombEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.CrystalDragonLaserLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.CrystallizeShatterEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.CursedCroneSpiritSkullsEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.DawnSwirlEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.DredgingStaffEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.ElectricOrbEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.EmeraldGlyphEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.EvilsProtectorBombAttackEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.FallenWizardBeamLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.FireDanceLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.ForestSpectorDrainSoulLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.FrozenEnemyHitLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.FrozenEnemyShatterLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.GalvanicTrailEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.HydroPumpLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.KatanaDashLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.KineticChargeDashLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.LightningTrailEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.LinedUpSpiritBeamsLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MakeshiftDashLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MobDashLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MobHealthChangeEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MobManaChangeEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MoundShockWaveLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MouseBeamLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MutantHydraHeadScreamLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MutantHydraSpitEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MutantSpitEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.NecroticSoulSkullPushEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.PhoenixFeatherGroundFireEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.PyromancyBurningGroundEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.QueenSpiderSpitEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.ReturnLifeOnHitLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.RubyGlyphEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.RuneSpiritPoolEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.SapphireGlyphEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.ScrapBallEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.SingleSpiritBeamLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.SlimeGreatbowEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.SlimeQuakeEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.SlimeQuakeWarningEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.SmallGroundWebEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.SmiteLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.SpideriteWaveGroundWebEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.SunlightOrbEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.TheCrimsonSkyEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.TheVoidBreathBeamLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.TheVoidClawBeamLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.TheVoidHornBeamLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.ToolItemMobAbilityEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.TopazGlyphEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.VenomStaffEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.VoidClawDashLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.VoidRainAttackEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.WeaponChargeSmiteLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.WebWeaverWebEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.voidWizard.VoidWizardGooEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.voidWizard.VoidWizardHomingEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.voidWizard.VoidWizardMissileEvent;
import necesse.entity.levelEvent.nightSwarmEvent.NightSwarmLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.AncientSkeletonRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.BasicSettlementRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.ChickenPeopleSettlementRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.FishianSettlementRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.FrozenDwarvesSettlementRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.HumanSettlementRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.MummiesSettlementRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.NinjaSettlementRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.PiratesSettlementRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.RogueHuntersSettlementRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.TheMafiaRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.VampiresSettlementRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.VoidApprenticesSettlementRaidLevelEvent;
import necesse.gfx.forms.presets.creative.CreativeToolsTab;

public class LevelEventRegistry extends ClassedGameRegistry<LevelEvent, LevelEventRegistry.LevelEventRegistryElement> {
   public static final LevelEventRegistry instance = new LevelEventRegistry();

   private LevelEventRegistry() {
      super("LevelEvent", 32762);
   }

   @Override
   public void registerCore() {
      GameLoadingScreen.drawLoadingString(Localization.translate("loading", "levelevents"));
      registerEvent("toolitem", ToolItemMobAbilityEvent.class);
      registerEvent("lightningtrail", LightningTrailEvent.class);
      registerEvent("galvanictrail", GalvanicTrailEvent.class);
      registerEvent("evilsprotectorbombattack", EvilsProtectorBombAttackEvent.class);
      registerEvent("voidwizardgoo", VoidWizardGooEvent.class);
      registerEvent("voidwizardhoming", VoidWizardHomingEvent.class);
      registerEvent("voidwizardexplosion", VoidWizardExplosionEvent.class);
      registerEvent("voidwizardmissile", VoidWizardMissileEvent.class);
      registerEvent("mobhealthinc", MobHealthChangeEvent.class);
      registerEvent("mobmanainc", MobManaChangeEvent.class);
      registerEvent("cavespiderweb", CaveSpiderWebEvent.class);
      registerEvent("cavespiderspit", CaveSpiderSpitEvent.class);
      registerEvent("venomstaff", VenomStaffEvent.class);
      registerEvent("dredgingstaff", DredgingStaffEvent.class);
      registerEvent("firedance", FireDanceLevelEvent.class);
      registerEvent("mousebeam", MouseBeamLevelEvent.class);
      registerEvent("queenspiderspit", QueenSpiderSpitEvent.class);
      registerEvent("fallenwizardbeam", FallenWizardBeamLevelEvent.class);
      registerEvent("webspinnerweb", SmallGroundWebEvent.class);
      registerEvent("mobdash", MobDashLevelEvent.class);
      registerEvent("katanadash", KatanaDashLevelEvent.class);
      registerEvent("crystaldragonlaser", CrystalDragonLaserLevelEvent.class);
      registerEvent("stabbybushexplosion", StabbyBushExplosionLevelEvent.class);
      registerEvent("necropoisonexplosion", NecroPoisonSplashEvent.class);
      registerEvent("polymorphsplash", PolymorphSplashEvent.class);
      registerEvent("necropoisonaura", NecroticSoulSkullPushEvent.class);
      registerEvent("frozenenemyhit", FrozenEnemyHitLevelEvent.class);
      registerEvent("frozenenemyshatter", FrozenEnemyShatterLevelEvent.class);
      registerEvent("runicspiritpool", RuneSpiritPoolEvent.class);
      registerEvent("singlespiritbeams", SingleSpiritBeamLevelEvent.class);
      registerEvent("linedupspiritbeams", LinedUpSpiritBeamsLevelEvent.class);
      registerEvent("cursedcronespiritbeamsexplosion", CursedCroneSpiritBeamsExplosionLevelEvent.class);
      registerEvent("forestspectordrainsoul", ForestSpectorDrainSoulLevelEvent.class);
      registerEvent("brutesbattleaxedash", BrutesBattleaxeDashLevelEvent.class);
      registerEvent("arcanicpylonlightning", ArcanicPylonLightningLevelEvent.class);
      registerEvent("arcanicpylonexplosion", ArcanicPylonExplosionLevelEvent.class);
      registerEvent("chargebeamwarning", ChargeBeamWarningLevelEvent.class);
      registerEvent("chargeshower", ChargeShowerLevelEvent.class);
      registerEvent("ascendedpylonattack", ChargeShowerLevelEvent.class);
      registerEvent("ascendedbombexplosion", AscendedBombExplosionEvent.class);
      registerEvent("ascendedpylonchargeupattack", AscendedPylonChargeUpAttackLevelEvent.class);
      registerEvent("ascendedpushexplosion", AscendedPushExplosionEvent.class);
      registerEvent("ascendedfractureevent", AscendedFractureGroundEvent.class);
      registerEvent("ascendedslimequakewarning", AscendedSlimeQuakeWarningEvent.class);
      registerEvent("ascendedslimequake", AscendedSlimeQuakeEvent.class);
      registerEvent("ascendedlightning", AscendedLightningLevelEvent.class);
      registerEvent("ascendedlightningexplosion", AscendedLightningExplosionLevelEvent.class);
      registerEvent("ascendedbatjail", AscendedBatJailLevelEvent.class);
      registerEvent("voidblackhole", TheVoidBlackHoleGroundEvent.class);
      registerEvent("voidclawgroundshatter", TheVoidClawGroundShatterGroundEvent.class);
      registerEvent("ascendedblackhole", AscendedBlackHoleEvent.class);
      registerEvent("voidhornbeam", TheVoidHornBeamLevelEvent.class);
      registerEvent("voidbreathbeam", TheVoidBreathBeamLevelEvent.class);
      registerEvent("voidrain", VoidRainAttackEvent.class);
      registerEvent("voidrainexplosion", VoidRainExplosionEvent.class);
      registerEvent("voidclawbeam", TheVoidClawBeamLevelEvent.class);
      registerEvent("voidmovingrain", TheVoidMovingRainLevelEvent.class);
      registerEvent("voidclawdash", VoidClawDashLevelEvent.class);
      registerEvent("kineticchargedash", KineticChargeDashLevelEvent.class);
      registerEvent("minirocketexplosion", MiniRocketExplosionEvent.class);
      registerEvent("furnacegolemoverheat", FurnaceGolemOverheatEvent.class);
      registerEvent("scrapball", ScrapBallEvent.class);
      registerEvent("mutantspit", MutantSpitEvent.class);
      registerEvent("makeshiftdash", MakeshiftDashLevelEvent.class);
      registerEvent("patchworkgrenadeexplosion", PatchworkGrenadeExplosionEvent.class);
      registerEvent("mutanthydraheadscream", MutantHydraHeadScreamLevelEvent.class);
      registerEvent("mutanthydraspit", MutantHydraSpitEvent.class);
      registerEvent("phoenixfeathergroundfire", PhoenixFeatherGroundFireEvent.class);
      registerEvent("rebornexplosion", RebornExplosionEvent.class);
      registerEvent("teleport", TeleportEvent.class);
      registerEvent("teleportfail", TeleportFailEvent.class);
      registerEvent("smokepuff", SmokePuffLevelEvent.class);
      registerEvent("smokepuffcloud", SmokePuffCloudLevelEvent.class);
      registerEvent("crystallizeshatter", CrystallizeShatterEvent.class);
      registerEvent("huntincursion", HuntIncursionEvent.class);
      registerEvent("extractionincursion", ExtractionIncursionEvent.class);
      registerEvent("trialincursion", TrialIncursionEvent.class);
      registerEvent("ascendedincursion", AscendedIncursionEvent.class);
      registerEvent("frenzymodifier", FrenzyModifierLevelEvent.class);
      registerEvent("tremorsmodifier", TremorsModifierLevelEvent.class);
      registerEvent("alchemicalinterferencemodifier", AlchemicalInterferenceModifierLevelEvent.class);
      registerEvent("crawlmageddonmodifier", CrawlmageddonModifierLevelEvent.class);
      registerEvent("explosivemodifier", ExplosiveModifierLevelEvent.class);
      registerEvent("explosivemodifierexplosion", ExplosiveModifierExplosionLevelEvent.class);
      registerEvent("explosivemodifierchargeup", ExplosiveModifierChargeUpLevelEvent.class);
      registerEvent("flamelingsmodifier", FlamelingsModifierLevelEvent.class);
      registerEvent("flamelingsmodifiersmokepuff", FlamelingsModifierSmokePuffLevelEvent.class);
      registerEvent("stormingmodifier", StormingModifierLevelEvent.class);
      registerEvent("stormingmodifierexplosion", StormingModifierExplosionLevelEvent.class);
      registerEvent("flamelingscandieperk", FlamelingsCanDiePerkLevelEvent.class);
      registerEvent("mobsdropaltardust", MobsDropAltarDustPerkLevelEvent.class);
      registerEvent("mobsdropmorealtardust", MobsDropMoreAltarDustPerkLevelEvent.class);
      registerEvent("mobsdropalchemyshards", MobsDropAlchemyShardsPerkLevelEvent.class);
      registerEvent("mobsdropupgradeshards", MobsDropUpgradeShardsPerkLevelEvent.class);
      registerEvent("empowermentbuffs", EmpowermentBuffsModifierLevelEvent.class);
      registerEvent("enablebannerofwar", EnableBannerOfWarLevelEvent.class);
      registerEvent("alchemyshardsdroppotions", AlchemyShardsDropPotionsLevelEvent.class);
      registerEvent("ascendedsharddrops", AscendedShardsBossDropLevelEvent.class);
      registerEvent("chancetominefullcluster", ChanceToMineFullClusterLevelEvent.class);
      registerEvent("doublebosschance", DoubleBossChanceLevelEvent.class);
      registerEvent("secondlife", SecondLifePerkLevelEvent.class);
      registerEvent("modifiersaffectenemies", ModifiersAffectEnemiesLevelEvent.class);
      registerEvent("mobsexplodeupgradeshards", MobsExplodeUpgradeShardsPerkLevelEvent.class);
      registerEvent("mobsexplodeupgradeshardsexplosion", MobsExplodeUpgradeShardsPerkExplosionLevelEvent.class);
      registerEvent("allbannerbuffs", AllBannerBuffsLevelEvent.class);
      registerEvent("humanraid", HumanSettlementRaidLevelEvent.class);
      registerEvent("fishianraid", FishianSettlementRaidLevelEvent.class);
      registerEvent("chickenpeopleraid", ChickenPeopleSettlementRaidLevelEvent.class);
      registerEvent("ninjasraid", NinjaSettlementRaidLevelEvent.class);
      registerEvent("roguehuntersraid", RogueHuntersSettlementRaidLevelEvent.class);
      registerEvent("frozendwarvesraid", FrozenDwarvesSettlementRaidLevelEvent.class);
      registerEvent("voidapprenticeraid", VoidApprenticesSettlementRaidLevelEvent.class);
      registerEvent("vampireraid", VampiresSettlementRaidLevelEvent.class);
      registerEvent("mummyraid", MummiesSettlementRaidLevelEvent.class);
      registerEvent("pirateraid", PiratesSettlementRaidLevelEvent.class);
      registerEvent("ancientskeletonraid", AncientSkeletonRaidLevelEvent.class);
      registerEvent("themafiaraid", TheMafiaRaidLevelEvent.class);
      registerEvent("flametrap", FlameTrapEvent.class);
      registerEvent("spiketrap", SpikeTrapEvent.class);
      registerEvent("tntexplosion", TNTExplosionEvent.class);
      registerEvent("fishing", FishingEvent.class);
      registerEvent("cannonballexplosion", CannonBallExplosionEvent.class);
      registerEvent("ghostskullexplosion", GhostSkullExplosionEvent.class);
      registerEvent("captaincannonballexplosion", CaptainCannonBallExplosionEvent.class);
      registerEvent("bombexplosion", BombExplosionEvent.class);
      registerEvent("dynamiteexplosion", DynamiteExplosionEvent.class);
      registerEvent("boulderhit", BoulderHitExplosionEvent.class);
      registerEvent("hydropump", HydroPumpLevelEvent.class);
      registerEvent("moundshockwave", MoundShockWaveLevelEvent.class);
      registerEvent("ancientdredgingstaff", AncientDredgingStaffEvent.class);
      registerEvent("templeentrance", TempleEntranceEvent.class);
      registerEvent("fallenwizardrespawn", FallenWizardRespawnEvent.class);
      registerEvent("slimequakewarning", SlimeQuakeWarningEvent.class);
      registerEvent("slimequake", SlimeQuakeEvent.class);
      registerEvent("nightswarm", NightSwarmLevelEvent.class);
      registerEvent("returnlifeonhit", ReturnLifeOnHitLevelEvent.class);
      registerEvent("bloodgrimoireparticle", BloodGrimoireParticleLevelEvent.class);
      registerEvent("showattacktick", ShowAttackMobTickEvent.class);
      registerEvent("swordcleansliceattack", SwordCleanSliceAttackMobEvent.class);
      registerEvent("thecrimsonsky", TheCrimsonSkyEvent.class);
      registerEvent("slimegreatbowevent", SlimeGreatbowEvent.class);
      registerEvent("waitforseconds", WaitForSecondsEvent.class);
      registerEvent("bloatedspiderexplosion", BloatedSpiderExplosionEvent.class);
      registerEvent("webweaverweb", WebWeaverWebEvent.class);
      registerEvent("spideritewaveweb", SpideriteWaveGroundWebEvent.class);
      registerEvent("empressacidwave", EmpressAcidGroundEvent.class);
      registerEvent("dawnswirl", DawnSwirlEvent.class);
      registerEvent("supernovaexplosion", SupernovaExplosionEvent.class);
      registerEvent("sunlightorb", SunlightOrbEvent.class);
      registerEvent("crystalbomb", CrystalBombEvent.class);
      registerEvent("chainlightningeffect", ChainLightningEffectLevelEvent.class);
      registerEvent("overchargedmanahit", OverChargedManaHitsLevelEvent.class);
      registerEvent("electricorb", ElectricOrbEvent.class);
      registerEvent("electricorbexplosion", ElectricOrbExplosionLevelEvent.class);
      registerEvent("sapphireglyph", SapphireGlyphEvent.class);
      registerEvent("amethystglyph", AmethystGlyphEvent.class);
      registerEvent("rubyglyph", RubyGlyphEvent.class);
      registerEvent("rubyexplosion", RubyStaffExplosionEvent.class);
      registerEvent("emeraldglyph", EmeraldGlyphEvent.class);
      registerEvent("topazglyph", TopazGlyphEvent.class);
      registerEvent("bonespikemobexplosion", BoneSpikeMobExplosionLevelEvent.class);
      registerEvent("chieftaingauntlet", ChieftainGauntletEvent.class);
      registerEvent("chieftaindash", ChieftainDashLevelEvent.class);
      registerEvent("tictactoe", TicTacToeLevelEvent.class);
      registerEvent("cursedcronespiritskulls", CursedCroneSpiritSkullsEvent.class);
      registerEvent("spritcorrupted", SpiritCorruptedLevelEvent.class);
      registerEvent("fallingicicle", FallingIcicleEvent.class);
      registerEvent("chickenglyphtrap", ChickenGlyphTrapEvent.class);
      registerEvent("bounceglyphtrap", BounceGlyphTrapEvent.class);
      registerEvent("reversedamageglyphtrap", ReverseDamageGlyphTrapEvent.class);
      registerEvent("soulseedexplosion", SoulseedExplosionEvent.class);
      registerEvent("simplesplashevent", SimpleSplashEvent.class);
      registerEvent("healsplashevent", HealSplashEvent.class);
      registerEvent("freezesplashevent", FreezeSplashEvent.class);
      registerEvent("smitesplashevent", SmiteSplashEvent.class);
      registerEvent("smitelevelevent", SmiteLevelEvent.class);
      registerEvent("weaponchargesmitelevelevent", WeaponChargeSmiteLevelEvent.class);
      registerEvent("damagesplashevent", DamageSplashEvent.class);
      registerEvent("slimesplashevent", SlimeSplashEvent.class);
      registerEvent("locustdeathsplashevent", LocustDeathSplashEvent.class);
      registerEvent("cryostorm", CryoStormLevelEvent.class);
      registerEvent("corekeepercorechat", CoreKeeperCoreChatLevelEvent.class);
      registerEvent("pyromancyburningground", PyromancyBurningGroundEvent.class);
   }

   protected void onRegister(LevelEventRegistry.LevelEventRegistryElement object, int id, String stringID, boolean isReplace) {
      super.onRegister(object, id, stringID, isReplace);
      if (BasicSettlementRaidLevelEvent.class.isAssignableFrom(object.data.aClass)) {
         try {
            BasicSettlementRaidLevelEvent raidEvent = (BasicSettlementRaidLevelEvent)object.data.newInstance();
            CreativeToolsTab.addRaidType(id, raidEvent.getMobStringID(), new LocalMessage("misc", raidEvent.getRaidTypeStringID()));
         } catch (IllegalAccessException | InvocationTargetException | InstantiationException ignored) {
            GameLog.warn.println("Failed to register raid type: " + stringID + " for creative mode.");
         }
      }
   }

   @Override
   protected void onRegistryClose() {
   }

   public static int registerEvent(String stringID, Class<? extends LevelEvent> levelEvent) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register level events");
      }

      try {
         return instance.register(stringID, new LevelEventRegistry.LevelEventRegistryElement(levelEvent));
      } catch (NoSuchMethodException e) {
         System.err.println("Could not register LevelEvent " + levelEvent.getSimpleName() + ": Missing constructor with no parameters");
         return -1;
      }
   }

   public static LevelEvent getEvent(int id) {
      try {
         return instance.getElement(id).newInstance();
      } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
         e.printStackTrace();
         return null;
      }
   }

   public static LevelEvent getEvent(String stringID) {
      return getEvent(getEventID(stringID));
   }

   public static int getEventID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static int getEventID(Class<? extends LevelEvent> clazz) {
      return instance.getElementID(clazz);
   }

   public static String getEventStringID(int id) {
      return instance.getElementStringID(id);
   }

   protected static class LevelEventRegistryElement extends ClassIDDataContainer<LevelEvent> {
      public LevelEventRegistryElement(Class<? extends LevelEvent> levelEventClass) throws NoSuchMethodException {
         super(levelEventClass);
      }
   }
}
