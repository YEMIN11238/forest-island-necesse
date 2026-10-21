package necesse.engine.registries;

import java.lang.reflect.InvocationTargetException;
import necesse.engine.GameLoadingScreen;
import necesse.engine.localization.Localization;
import necesse.engine.modLoader.LoadedMod;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.projectile.AgedChampionWaveProjectile;
import necesse.entity.projectile.AmethystStaffProjectile;
import necesse.entity.projectile.AncientBoneProjectile;
import necesse.entity.projectile.AncientSkeletonMageProjectile;
import necesse.entity.projectile.AncientVultureProjectile;
import necesse.entity.projectile.AscendedBeamProjectile;
import necesse.entity.projectile.AscendedBoltProjectile;
import necesse.entity.projectile.AscendedBoltSoundProjectile;
import necesse.entity.projectile.AscendedBombProjectile;
import necesse.entity.projectile.AscendedBowBoltProjectile;
import necesse.entity.projectile.AscendedFractureProjectile;
import necesse.entity.projectile.AscendedGolemSpawnProjectile;
import necesse.entity.projectile.AscendedOrbProjectile;
import necesse.entity.projectile.AscendedShardBombProjectile;
import necesse.entity.projectile.AscendedShardProjectile;
import necesse.entity.projectile.AscendedSlashProjectile;
import necesse.entity.projectile.AscendedStaffBeamProjectile;
import necesse.entity.projectile.AshenWaveProjectile;
import necesse.entity.projectile.BabyBoneProjectile;
import necesse.entity.projectile.BarkBladeLeafProjectile;
import necesse.entity.projectile.BashyBushSpawnProjectile;
import necesse.entity.projectile.BloodClawProjectile;
import necesse.entity.projectile.BloodGrimoireRightClickProjectile;
import necesse.entity.projectile.BoneArrowProjectile;
import necesse.entity.projectile.BoneProjectile;
import necesse.entity.projectile.BoneSpikesProjectile;
import necesse.entity.projectile.BoulderStaffProjectile;
import necesse.entity.projectile.BouncingArrowProjectile;
import necesse.entity.projectile.BouncingSlimeBallProjectile;
import necesse.entity.projectile.ButchersCleaverBoomerangProjectile;
import necesse.entity.projectile.CannonBallProjectile;
import necesse.entity.projectile.CaptainCannonBallProjectile;
import necesse.entity.projectile.CarapaceDaggerProjectile;
import necesse.entity.projectile.CausticExecutionerProjectile;
import necesse.entity.projectile.CaveSpiderSpitProjectile;
import necesse.entity.projectile.CaveSpiderWebProjectile;
import necesse.entity.projectile.ChargeShowerProjectile;
import necesse.entity.projectile.ChefsSpecialRollingPinProjectile;
import necesse.entity.projectile.ChieftainShieldProjectile;
import necesse.entity.projectile.CoinProjectile;
import necesse.entity.projectile.CrazedRavenFeatherProjectile;
import necesse.entity.projectile.CrescentDiscFollowingProjectile;
import necesse.entity.projectile.CrimsonSkyArrowProjectile;
import necesse.entity.projectile.CryoMissileProjectile;
import necesse.entity.projectile.CryoQuakeProjectile;
import necesse.entity.projectile.CryoQuakeWeaponProjectile;
import necesse.entity.projectile.CryoShardProjectile;
import necesse.entity.projectile.CryoSpearShardProjectile;
import necesse.entity.projectile.CryoVolleyProjectile;
import necesse.entity.projectile.CryoWarningProjectile;
import necesse.entity.projectile.CryoWaveProjectile;
import necesse.entity.projectile.CrystalDragonShardProjectile;
import necesse.entity.projectile.CrystalGolemSpawnProjectile;
import necesse.entity.projectile.CrystalShieldRetaliationProjectile;
import necesse.entity.projectile.DawnFireballProjectile;
import necesse.entity.projectile.DruidsGreatBowPetalProjectile;
import necesse.entity.projectile.DryadBowProjectile;
import necesse.entity.projectile.DuskVolleyProjectile;
import necesse.entity.projectile.DynamiteStickProjectile;
import necesse.entity.projectile.EarthSpikesProjectile;
import necesse.entity.projectile.EmeraldStaffProjectile;
import necesse.entity.projectile.EmeraldWandProjectile;
import necesse.entity.projectile.EmpressAcidProjectile;
import necesse.entity.projectile.EmpressSlashProjectile;
import necesse.entity.projectile.EmpressSlashWarningProjectile;
import necesse.entity.projectile.EmpressWebBallProjectile;
import necesse.entity.projectile.EvilWitchGreatswordWaveProjectile;
import necesse.entity.projectile.EvilsProtectorAttack1Projectile;
import necesse.entity.projectile.EyeOfTheVoidSpawnProjectile;
import necesse.entity.projectile.FallenWizardBallProjectile;
import necesse.entity.projectile.FallenWizardWaveProjectile;
import necesse.entity.projectile.FireArrowProjectile;
import necesse.entity.projectile.FlamelingShooterProjectile;
import necesse.entity.projectile.FrostArrowProjectile;
import necesse.entity.projectile.FrostSentryProjectile;
import necesse.entity.projectile.FrostStaffProjectile;
import necesse.entity.projectile.GhostArrowProjectile;
import necesse.entity.projectile.GhostSkullProjectile;
import necesse.entity.projectile.GlacialBowProjectile;
import necesse.entity.projectile.GoldBoltProjectile;
import necesse.entity.projectile.GritArrowProjectile;
import necesse.entity.projectile.GritBoomerangProjectile;
import necesse.entity.projectile.HexedBladeGreatswordWaveProjectile;
import necesse.entity.projectile.HostileIceJavelinProjectile;
import necesse.entity.projectile.IceJavelinProjectile;
import necesse.entity.projectile.IcicleStaffProjectile;
import necesse.entity.projectile.IronArrowProjectile;
import necesse.entity.projectile.IronBombProjectile;
import necesse.entity.projectile.JunkMechMachineGunProjectile;
import necesse.entity.projectile.LivingShottyLeafProjectile;
import necesse.entity.projectile.MagneticPullProjectile;
import necesse.entity.projectile.MakeshiftArcThrowerProjectile;
import necesse.entity.projectile.MoonlightJavelinProjectile;
import necesse.entity.projectile.MutantGroundAcidProjectile;
import necesse.entity.projectile.MutantHydraGooSprayProjectile;
import necesse.entity.projectile.MutantHydraSpitProjectile;
import necesse.entity.projectile.MutantSpitProjectile;
import necesse.entity.projectile.NecroticFlaskProjectile;
import necesse.entity.projectile.NecroticGreatswordWaveProjectile;
import necesse.entity.projectile.NightPiercerArrowProjectile;
import necesse.entity.projectile.NinjaStarProjectile;
import necesse.entity.projectile.PatchworkGrenadeProjectile;
import necesse.entity.projectile.PathTestProjectile;
import necesse.entity.projectile.PhantomBobbleProjectile;
import necesse.entity.projectile.PhantomBoltProjectile;
import necesse.entity.projectile.PhoenixFeatherProjectile;
import necesse.entity.projectile.PlayerSnowballProjectile;
import necesse.entity.projectile.PoisonArrowProjectile;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.PyromancyProjectile;
import necesse.entity.projectile.QuartzBoltProjectile;
import necesse.entity.projectile.QueenSpiderEggProjectile;
import necesse.entity.projectile.QueenSpiderSpitProjectile;
import necesse.entity.projectile.RCToySoldierDynamiteProjectile;
import necesse.entity.projectile.RCToySoldierSpawnProjectile;
import necesse.entity.projectile.RavenBeakSpearProjectile;
import necesse.entity.projectile.ReaperScythePlayerProjectile;
import necesse.entity.projectile.RollingPinProjectile;
import necesse.entity.projectile.RubyStaffProjectile;
import necesse.entity.projectile.SageArrowProjectile;
import necesse.entity.projectile.SageBoomerangProjectile;
import necesse.entity.projectile.SapphireRevolverProjectile;
import necesse.entity.projectile.SapphireStaffProjectile;
import necesse.entity.projectile.SkeletonMageProjectile;
import necesse.entity.projectile.SlimeBoltProjectile;
import necesse.entity.projectile.SlimeEggProjectile;
import necesse.entity.projectile.SlimeGreatBowArrowProjectile;
import necesse.entity.projectile.SlimeGreatswordProjectile;
import necesse.entity.projectile.SmallSpiritLeafProjectile;
import necesse.entity.projectile.SmiteBeamProjectile;
import necesse.entity.projectile.SnowballProjectile;
import necesse.entity.projectile.SoundWaveProjectile;
import necesse.entity.projectile.SparklerProjectile;
import necesse.entity.projectile.SpideriteArrowProjectile;
import necesse.entity.projectile.SpideriteWaveProjectile;
import necesse.entity.projectile.SpiritBeamProjectile;
import necesse.entity.projectile.SpiritOrbProjectile;
import necesse.entity.projectile.SpiritSkullProjectile;
import necesse.entity.projectile.SprinklerProjectile;
import necesse.entity.projectile.StabbyBushSpawnProjectile;
import necesse.entity.projectile.StarVeilProjectile;
import necesse.entity.projectile.StoneArrowProjectile;
import necesse.entity.projectile.StoneProjectile;
import necesse.entity.projectile.StormingIncursionModifierProjectile;
import necesse.entity.projectile.SwampBallProjectile;
import necesse.entity.projectile.SwampBoltProjectile;
import necesse.entity.projectile.SwampBoulderProjectile;
import necesse.entity.projectile.SwampDwellerStaffFlowerProjectile;
import necesse.entity.projectile.SwampDwellerStaffPetalProjectile;
import necesse.entity.projectile.SwampRazorProjectile;
import necesse.entity.projectile.SwampTomeProjectile;
import necesse.entity.projectile.TheRavensNestProjectile;
import necesse.entity.projectile.TileBombProjectile;
import necesse.entity.projectile.TrapArrowProjectile;
import necesse.entity.projectile.TrapperNetProjectile;
import necesse.entity.projectile.TrenchcoatGoblinSpawnProjectile;
import necesse.entity.projectile.UnlabeledPotionProjectile;
import necesse.entity.projectile.VampireProjectile;
import necesse.entity.projectile.VenomShowerProjectile;
import necesse.entity.projectile.VenomSlasherWaveProjectile;
import necesse.entity.projectile.VenomStaffProjectile;
import necesse.entity.projectile.VoidApprenticeProjectile;
import necesse.entity.projectile.VoidWizardCloneProjectile;
import necesse.entity.projectile.VoidWizardMissileProjectile;
import necesse.entity.projectile.VoidWizardWaveProjectile;
import necesse.entity.projectile.VultureHatchlingProjectile;
import necesse.entity.projectile.VulturesBurstProjectile;
import necesse.entity.projectile.WaterSprayProjectile;
import necesse.entity.projectile.WaterboltProjectile;
import necesse.entity.projectile.WeaponChargeSmiteBeamProjectile;
import necesse.entity.projectile.ZombieArrowProjectile;
import necesse.entity.projectile.boomerangProjectile.AnchorBoomerangProjectile;
import necesse.entity.projectile.boomerangProjectile.BoxingGloveBoomerangProjectile;
import necesse.entity.projectile.boomerangProjectile.FishianWarriorHookBoomerangProjectile;
import necesse.entity.projectile.boomerangProjectile.FrostBoomerangProjectile;
import necesse.entity.projectile.boomerangProjectile.HookBoomerangProjectile;
import necesse.entity.projectile.boomerangProjectile.RazorBladeBoomerangProjectile;
import necesse.entity.projectile.boomerangProjectile.ReaperScytheProjectile;
import necesse.entity.projectile.boomerangProjectile.SpiderBoomerangProjectile;
import necesse.entity.projectile.boomerangProjectile.TopazSpinningProjectile;
import necesse.entity.projectile.boomerangProjectile.TungstenBoomerangProjectile;
import necesse.entity.projectile.boomerangProjectile.VoidBoomerangProjectile;
import necesse.entity.projectile.boomerangProjectile.WoodBoomerangProjectile;
import necesse.entity.projectile.bulletProjectile.BouncingBulletProjectile;
import necesse.entity.projectile.bulletProjectile.CrystalBulletProjectile;
import necesse.entity.projectile.bulletProjectile.FrostBulletProjectile;
import necesse.entity.projectile.bulletProjectile.GrizzboltBulletProjectile;
import necesse.entity.projectile.bulletProjectile.HandGunBulletProjectile;
import necesse.entity.projectile.bulletProjectile.NecroticBoltProjectile;
import necesse.entity.projectile.bulletProjectile.ReanimationBoltProjectile;
import necesse.entity.projectile.bulletProjectile.SeedBulletProjectile;
import necesse.entity.projectile.bulletProjectile.SharkToothBulletProjectile;
import necesse.entity.projectile.bulletProjectile.SniperBulletProjectile;
import necesse.entity.projectile.bulletProjectile.WebbedGunBulletProjectile;
import necesse.entity.projectile.followingProjectile.AncestorMageProjectile;
import necesse.entity.projectile.followingProjectile.BloodBoltProjectile;
import necesse.entity.projectile.followingProjectile.ChromaticBoltProjectile;
import necesse.entity.projectile.followingProjectile.CryptVampireBoltProjectile;
import necesse.entity.projectile.followingProjectile.ElderlyWandProjectile;
import necesse.entity.projectile.followingProjectile.EvilsProtectorAttack2Projectile;
import necesse.entity.projectile.followingProjectile.FallenWizardScepterProjectile;
import necesse.entity.projectile.followingProjectile.FishianHealProjectile;
import necesse.entity.projectile.followingProjectile.GlacialBoomerangProjectile;
import necesse.entity.projectile.followingProjectile.MageSlimeBoltProjectile;
import necesse.entity.projectile.followingProjectile.MiniRocketProjectile;
import necesse.entity.projectile.followingProjectile.MouseTestProjectile;
import necesse.entity.projectile.followingProjectile.NightRazorBoomerangProjectile;
import necesse.entity.projectile.followingProjectile.PhantomMissileProjectile;
import necesse.entity.projectile.followingProjectile.ShadowBoltProjectile;
import necesse.entity.projectile.followingProjectile.TicTacToePunishProjectile;
import necesse.entity.projectile.followingProjectile.VampiricBatBoltProjectile;
import necesse.entity.projectile.followingProjectile.VoidBulletProjectile;
import necesse.entity.projectile.followingProjectile.VoidMissileProjectile;
import necesse.entity.projectile.followingProjectile.VoidWizardHomingProjectile;
import necesse.entity.projectile.laserProjectile.AscendedGolemBeamProjectile;
import necesse.entity.projectile.laserProjectile.ChargeBeamProjectile;
import necesse.entity.projectile.laserProjectile.CrystalGolemBeamProjectile;
import necesse.entity.projectile.laserProjectile.ShadowBeamProjectile;
import necesse.entity.projectile.laserProjectile.VoidLaserProjectile;
import necesse.entity.projectile.laserProjectile.VoidTrapProjectile;
import necesse.entity.projectile.pathProjectile.CrimsonSkyArrowPathProjectile;
import necesse.entity.projectile.pathProjectile.CryoQuakeCirclingProjectile;
import necesse.entity.projectile.pathProjectile.CryoWarningCirclingProjectile;
import necesse.entity.projectile.pathProjectile.StaticJellyfishProjectile;
import necesse.entity.projectile.pathProjectile.SurvivorWhipProjectile;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;

public class ProjectileRegistry extends ClassedGameRegistry<Projectile, ProjectileRegistry.ProjectileRegistryElement> {
   public static final ProjectileRegistry instance = new ProjectileRegistry();

   private ProjectileRegistry() {
      super("Projectile", 32762);
   }

   @Override
   public void registerCore() {
      GameLoadingScreen.drawLoadingString(Localization.translate("loading", "projectiles"));
      registerProjectile("stonearrow", StoneArrowProjectile.class, "stonearrow", "arrow_shadow");
      registerProjectile("ironarrow", IronArrowProjectile.class, "ironarrow", "arrow_shadow");
      registerProjectile("bouncingarrow", BouncingArrowProjectile.class, "bouncingarrow", "arrow_shadow");
      registerProjectile("vampirebolt", VampireProjectile.class, "bloodbolt", "bloodbolt_shadow");
      registerProjectile("vampiricbatbolt", VampiricBatBoltProjectile.class, "bloodbolt", "bloodbolt_shadow");
      registerProjectile("ninjastar", NinjaStarProjectile.class, "ninjastar", "ninjastar_shadow");
      registerProjectile("woodboomerang", WoodBoomerangProjectile.class, "woodboomerang", "woodboomerang_shadow");
      registerProjectile("spiderboomerang", SpiderBoomerangProjectile.class, "spiderboomerang", "spiderboomerang_shadow");
      registerProjectile("frostboomerang", FrostBoomerangProjectile.class, "frostboomerang", "frostboomerang_shadow");
      registerProjectile("voidboomerang", VoidBoomerangProjectile.class, "voidboomerang", "voidboomerang_shadow");
      registerProjectile("bloodbolt", BloodBoltProjectile.class, "bloodbolt", "bloodbolt_shadow");
      registerProjectile("necroticbolt", NecroticBoltProjectile.class, "necroticbolt", "bloodbolt_shadow");
      registerProjectile("reanimationbolt", ReanimationBoltProjectile.class, "necroticbolt", "bloodbolt_shadow");
      registerProjectile("sparkler", SparklerProjectile.class, null, null);
      registerProjectile("sprinkler", SprinklerProjectile.class, null, null);
      registerProjectile("hookboomerang", HookBoomerangProjectile.class, "hookboomerang", "hookboomerang_shadow");
      registerProjectile("firearrow", FireArrowProjectile.class, "firearrow", "arrow_shadow");
      registerProjectile("frostarrow", FrostArrowProjectile.class, "frostarrow", "arrow_shadow");
      registerProjectile("voidlaser", VoidLaserProjectile.class, null, null);
      registerProjectile("handgunbullet", HandGunBulletProjectile.class, null, null);
      registerProjectile("bouncingbullet", BouncingBulletProjectile.class, null, null);
      registerProjectile("frostbullet", FrostBulletProjectile.class, null, null);
      registerProjectile("waterbolt", WaterboltProjectile.class, "waterbolt", "waterbolt_shadow");
      registerProjectile("sniperbullet", SniperBulletProjectile.class, null, null);
      registerProjectile("webbedgunbullet", WebbedGunBulletProjectile.class, null, null);
      registerProjectile("evilsprotector1", EvilsProtectorAttack1Projectile.class, "evilsprotector1", "evilsprotector1_shadow");
      registerProjectile("traparrow", TrapArrowProjectile.class, "ironarrow", "arrow_shadow");
      registerProjectile("voidtrap", VoidTrapProjectile.class, null, null);
      registerProjectile("evilsprotector2", EvilsProtectorAttack2Projectile.class, "evilsprotector2", "evilsprotector2_shadow");
      registerProjectile("zombiearrow", ZombieArrowProjectile.class, "stonearrow", "arrow_shadow");
      registerProjectile("cannonball", CannonBallProjectile.class, "cannonball", "cannonball_shadow");
      registerProjectile("captaincannonball", CaptainCannonBallProjectile.class, "cannonball", "cannonball_shadow");
      registerProjectile("voidwizardhoming", VoidWizardHomingProjectile.class, "voidwizardhoming", "voidwizardhoming_shadow");
      registerProjectile("voidwizardclone", VoidWizardCloneProjectile.class, "voidwizardclone", "voidwizardclone_shadow");
      registerProjectile("voidwizardmissile", VoidWizardMissileProjectile.class, "voidwizardmissile", "voidwizardmissile_shadow");
      registerProjectile("voidwizardwave", VoidWizardWaveProjectile.class, "voidwizardwave", null);
      registerProjectile("voidapprentice", VoidApprenticeProjectile.class, "voidapprentice", "voidapprentice_shadow");
      registerProjectile("waterspray", WaterSprayProjectile.class, null, null);
      registerProjectile("mousetest", MouseTestProjectile.class, "bloodbolt", "bloodbolt_shadow");
      registerProjectile("goldbolt", GoldBoltProjectile.class, null, null);
      registerProjectile("ironbomb", IronBombProjectile.class, "ironbomb", "ironbomb_shadow");
      registerProjectile("dynamitestick", DynamiteStickProjectile.class, "dynamitestick", "dynamitestick_shadow");
      registerProjectile("tilebomb", TileBombProjectile.class, "tilebomb", "tilebomb_shadow");
      registerProjectile("voidmissile", VoidMissileProjectile.class, null, null);
      registerProjectile("stone", StoneProjectile.class, "stone", "stone_shadow");
      registerProjectile("coin", CoinProjectile.class, "coin", "coin_shadow");
      registerProjectile("snowball", SnowballProjectile.class, "snowball", "snowball_shadow");
      registerProjectile("playersnowball", PlayerSnowballProjectile.class, "snowball", "snowball_shadow");
      registerProjectile("ancientvulture", AncientVultureProjectile.class, "ancientfeather", "ancientfeather_shadow");
      registerProjectile("vulturehatchling", VultureHatchlingProjectile.class, "hatchlingfeather", "hatchlingfeather_shadow");
      registerProjectile("quartzbolt", QuartzBoltProjectile.class, "quartzbolt", "bolt_shadow");
      registerProjectile("vulturesburst", VulturesBurstProjectile.class, "vulturesburst", null);
      registerProjectile("froststaff", FrostStaffProjectile.class, "froststaff", "froststaff_shadow");
      registerProjectile("iciclestaff", IcicleStaffProjectile.class, "icicle", "icicle_shadow");
      registerProjectile("sapphirestaff", SapphireStaffProjectile.class, "crystalbullet", "crystalbullet_shadow");
      registerProjectile("topazstaff", TopazSpinningProjectile.class, "topazswirl", "topazswirl_shadow");
      registerProjectile("amethyststaff", AmethystStaffProjectile.class, null, null);
      registerProjectile("rubystaff", RubyStaffProjectile.class, null, null);
      registerProjectile("emeraldstaff", EmeraldStaffProjectile.class, null, null);
      registerProjectile("tungstenboomerang", TungstenBoomerangProjectile.class, "tungstenboomerang", "tungstenboomerang_shadow");
      registerProjectile("bone", BoneProjectile.class, "bone", "bone_shadow");
      registerProjectile("ancientbone", AncientBoneProjectile.class, "ancientbone", "bone_shadow");
      registerProjectile("bonearrow", BoneArrowProjectile.class, "bonearrow", "arrow_shadow");
      registerProjectile("elderlywand", ElderlyWandProjectile.class, null, null);
      registerProjectile("shadowbolt", ShadowBoltProjectile.class, "shadowbolt", null);
      registerProjectile("reaperscythe", ReaperScytheProjectile.class, null, null);
      registerProjectile("shadowbeam", ShadowBeamProjectile.class, null, null);
      registerProjectile("reaperscytheplayer", ReaperScythePlayerProjectile.class, "reaperscytheplayer", null);
      registerProjectile("boxingglove", BoxingGloveBoomerangProjectile.class, "boxingglove", null);
      registerProjectile("cavespiderweb", CaveSpiderWebProjectile.class, null, null);
      registerProjectile("cavespiderspit", CaveSpiderSpitProjectile.class, null, null);
      registerProjectile("venomstaff", VenomStaffProjectile.class, null, null);
      registerProjectile("cryomissile", CryoMissileProjectile.class, "cryomissile", null);
      registerProjectile("glacialbow", GlacialBowProjectile.class, "glacialbow", "glacialbow_shadow");
      registerProjectile("glacialboomerang", GlacialBoomerangProjectile.class, "glacialboomerang", "glacialboomerang_shadow");
      registerProjectile("swampbolt", SwampBoltProjectile.class, "swampbolt", "bolt_shadow");
      registerProjectile("cryowarning", CryoWarningProjectile.class, null, null);
      registerProjectile("cryoquake", CryoQuakeProjectile.class, null, null);
      registerProjectile("cryoquakecircle", CryoQuakeCirclingProjectile.class, null, null);
      registerProjectile("cryowarningcircle", CryoWarningCirclingProjectile.class, null, null);
      registerProjectile("cryoshard", CryoShardProjectile.class, null, null);
      registerProjectile("cryowave", CryoWaveProjectile.class, null, null);
      registerProjectile("cryovolley", CryoVolleyProjectile.class, "cryomissile", null);
      registerProjectile("cryoquakeweapon", CryoQuakeWeaponProjectile.class, null, null);
      registerProjectile("cryospearshard", CryoSpearShardProjectile.class, "cryospearshard", "cryospearshard_shadow");
      registerProjectile("voidbullet", VoidBulletProjectile.class, null, null);
      registerProjectile("poisonarrow", PoisonArrowProjectile.class, "poisonarrow", "arrow_shadow");
      registerProjectile("icejavelin", IceJavelinProjectile.class, "icejavelin", "icejavelin_shadow");
      registerProjectile("frostsentry", FrostSentryProjectile.class, null, null);
      registerProjectile("hostileicejavelin", HostileIceJavelinProjectile.class, "hostileicejavelin", "hostileicejavelin_shadow");
      registerProjectile("swamprazor", SwampRazorProjectile.class, "swamprazor", "swamprazor_shadow");
      registerProjectile("swampboulder", SwampBoulderProjectile.class, "swampboulder", "swampboulder_shadow");
      registerProjectile("boulderstaff", BoulderStaffProjectile.class, "boulderstaff", "swampboulder_shadow");
      registerProjectile("razorbladeboomerang", RazorBladeBoomerangProjectile.class, "razorbladeboomerang", "swamprazor_shadow");
      registerProjectile("carapacedagger", CarapaceDaggerProjectile.class, "carapacedagger", "carapacedagger_shadow");
      registerProjectile("sageboomerang", SageBoomerangProjectile.class, "sageboomerang", "dragonsrebound_shadow");
      registerProjectile("gritboomerang", GritBoomerangProjectile.class, "gritboomerang", "dragonsrebound_shadow");
      registerProjectile("rollingpin", RollingPinProjectile.class, "rollingpinboomerang", "rollingpin_shadow");
      registerProjectile("chefsspecialrollingpin", ChefsSpecialRollingPinProjectile.class, "rollingpinboomerang", "rollingpin_shadow");
      registerProjectile("butcherscleaverboomerang", ButchersCleaverBoomerangProjectile.class, "butcherscleaverboomerang", "butcherscleaver_shadow");
      registerProjectile("gritarrow", GritArrowProjectile.class, "gritarrow", null);
      registerProjectile("sagearrow", SageArrowProjectile.class, "sagearrow", null);
      registerProjectile("babybone", BabyBoneProjectile.class, "babybone", "babybone_shadow");
      registerProjectile("queenspideregg", QueenSpiderEggProjectile.class, "queenspideregg", "queenspideregg_shadow");
      registerProjectile("queenspiderspit", QueenSpiderSpitProjectile.class, null, "queenspiderspit_shadow");
      registerProjectile("necroticflasksplash", NecroticFlaskProjectile.class, "necroticflasksplash", "necroticflasksplash_shadow");
      registerProjectile("unlabeledpotionsplash", UnlabeledPotionProjectile.class, "unlabeledpotion", "unlabeledpotion_shadow");
      registerProjectile("necroticgreatswordwave", NecroticGreatswordWaveProjectile.class, "necroticwave", null);
      registerProjectile("hexedbladegreatswordwave", HexedBladeGreatswordWaveProjectile.class, "hexedwave", null);
      registerProjectile("evilwitchgreatswordwave", EvilWitchGreatswordWaveProjectile.class, "hostilenecroticwave", null);
      registerProjectile("pathtest", PathTestProjectile.class, null, "bolt_shadow");
      registerProjectile("swamptome", SwampTomeProjectile.class, "swamptome", "swamptome_shadow");
      registerProjectile("fallenwizardscepter", FallenWizardScepterProjectile.class, "fallenwizardscepter", "fallenwizardscepter_shadow");
      registerProjectile("fallenwizardwave", FallenWizardWaveProjectile.class, "fallenwizardwave", null);
      registerProjectile("fallenwizardball", FallenWizardBallProjectile.class, "fallenwizardball", "fallenwizardball_shadow");
      registerProjectile("skeletonmage", SkeletonMageProjectile.class, "skeletonmage", "skeletonmage_shadow");
      registerProjectile("ancientskeletonmage", AncientSkeletonMageProjectile.class, "ancientskeletonmage", "ancientskeletonmage_shadow");
      registerProjectile("swampdwellerstaffflower", SwampDwellerStaffFlowerProjectile.class, "swampdwellerstaffflower", "swampdwellerstaffflower_shadow");
      registerProjectile("swampdwellerstaffpetal", SwampDwellerStaffPetalProjectile.class, "swampdwellerstaffpetal", "swampdwellerstaffpetal_shadow");
      registerProjectile("druidsgreatbowpetal", DruidsGreatBowPetalProjectile.class, "druidsgreatbowpetal", "druidsgreatbowpetal_shadow");
      registerProjectile("nightpiercerarrow", NightPiercerArrowProjectile.class, "nightpiercerarrow", "nightpiercerarrow_shadow");
      registerProjectile("spideritearrow", SpideriteArrowProjectile.class, "spideritearrow", "spideritearrow_shadow");
      registerProjectile("swampball", SwampBallProjectile.class, "swampball", "swampball_shadow");
      registerProjectile("venomshower", VenomShowerProjectile.class, null, null);
      registerProjectile("agedchampionwave", AgedChampionWaveProjectile.class, "agedchampionwave", null);
      registerProjectile("venomslasherwave", VenomSlasherWaveProjectile.class, "venomslasherwave", null);
      registerProjectile("slimegreatswordprojectile", SlimeGreatswordProjectile.class, "slimegreatswordprojectile", null);
      registerProjectile("slimebolt", SlimeBoltProjectile.class, "slimebolt", "slimebolt_shadow");
      registerProjectile("livingshotty", LivingShottyLeafProjectile.class, "livingshotty", "livingshotty_shadow");
      registerProjectile("bouncingslimeball", BouncingSlimeBallProjectile.class, "bouncingslimeball", "bouncingslimeball_shadow");
      registerProjectile("mageslimebolt", MageSlimeBoltProjectile.class, "mageslimebolt", "mageslimebolt_shadow");
      registerProjectile("phantombolt", PhantomBoltProjectile.class, "phantombolt", "phantombolt_shadow");
      registerProjectile("cryptvampirebolt", CryptVampireBoltProjectile.class, "cryptvampirebolt", "cryptvampirebolt_shadow");
      registerProjectile("slimeegg", SlimeEggProjectile.class, null, null);
      registerProjectile("phantombobble", PhantomBobbleProjectile.class, "phantombobble", "phantombobble_shadow");
      registerProjectile("phantommissile", PhantomMissileProjectile.class, "phantommissile", "phantommissile_shadow");
      registerProjectile("nightrazorboomerang", NightRazorBoomerangProjectile.class, "nightrazorboomerang", "nightrazorboomerang_shadow");
      registerProjectile("bloodgrimoire", BloodGrimoireRightClickProjectile.class, "bloodgrimoireprojectile", null);
      registerProjectile("bloodclaw", BloodClawProjectile.class, "bloodclaw", null);
      registerProjectile("thecrimsonskyarrow", CrimsonSkyArrowProjectile.class, "thecrimsonskyarrow", "nightpiercerarrow_shadow");
      registerProjectile("thecrimsonskypatharrow", CrimsonSkyArrowPathProjectile.class, "thecrimsonskyarrow", "nightpiercerarrow_shadow");
      registerProjectile("slimegreatbowarrow", SlimeGreatBowArrowProjectile.class, "slimegreatbowarrow", "nightpiercerarrow_shadow");
      registerProjectile("causticexecutioner", CausticExecutionerProjectile.class, null, null);
      registerProjectile("spideritewave", SpideriteWaveProjectile.class, null, null);
      registerProjectile("empresswebball", EmpressWebBallProjectile.class, "webball", "webball_shadow");
      registerProjectile("empressslashwarning", EmpressSlashWarningProjectile.class, null, null);
      registerProjectile("empressslash", EmpressSlashProjectile.class, null, null);
      registerProjectile("empressacid", EmpressAcidProjectile.class, null, null);
      registerProjectile("theravensnest", TheRavensNestProjectile.class, "theravensnest", "theravensnestshadow");
      registerProjectile("dawnfireball", DawnFireballProjectile.class, "dawnfireball", null);
      registerProjectile("duskvolley", DuskVolleyProjectile.class, "duskvolley", null);
      registerProjectile("starvail", StarVeilProjectile.class, "starveil", null);
      registerProjectile("crescentdisc", CrescentDiscFollowingProjectile.class, "crescentdisc", null);
      registerProjectile("ravenbeaktip", RavenBeakSpearProjectile.class, "ravenbeaktip", null);
      registerProjectile("stormingprojectile", StormingIncursionModifierProjectile.class, "stormingprojectile", null);
      registerProjectile("crystalbullet", CrystalBulletProjectile.class, "crystalbullet", "crystalbullet_shadow");
      registerProjectile("shardslinger", SapphireRevolverProjectile.class, "crystalbullet", "crystalbullet_shadow");
      registerProjectile("emeraldwand", EmeraldWandProjectile.class, "emeraldwandprojectile", null);
      registerProjectile("crystalshieldretaliation", CrystalShieldRetaliationProjectile.class, null, null);
      registerProjectile("crystalgolembeam", CrystalGolemBeamProjectile.class, null, null);
      registerProjectile("crystaldragonshard", CrystalDragonShardProjectile.class, "crystaldragonshard", "crystalbullet_shadow");
      registerProjectile("crystalgolemspawn", CrystalGolemSpawnProjectile.class, null, "queenspideregg_shadow");
      registerProjectile("trenchcoatgoblinspawn", TrenchcoatGoblinSpawnProjectile.class, null, "crystalbullet_shadow");
      registerProjectile("stabbybushspawn", StabbyBushSpawnProjectile.class, null, "queenspideregg_shadow");
      registerProjectile("bashybushspawn", BashyBushSpawnProjectile.class, null, "queenspideregg_shadow");
      registerProjectile("seedbullet", SeedBulletProjectile.class, null, null);
      registerProjectile("fishianwarriorhook", FishianWarriorHookBoomerangProjectile.class, "fishianwarriorhook", null);
      registerProjectile("anchor", AnchorBoomerangProjectile.class, "anchor", "anchor_shadow");
      registerProjectile("fishianheal", FishianHealProjectile.class, "fishianheal", null);
      registerProjectile("staticjellyfish", StaticJellyfishProjectile.class, null, null);
      registerProjectile("bonespikesprojectile", BoneSpikesProjectile.class, null, null);
      registerProjectile("earthspikesprojectile", EarthSpikesProjectile.class, null, null);
      registerProjectile("trappernet", TrapperNetProjectile.class, "trappernet", "trappernet_shadow");
      registerProjectile("chieftainshield", ChieftainShieldProjectile.class, "chieftainshield", "chieftainshield_shadow");
      registerProjectile("dryadleaf", DryadBowProjectile.class, "dryadleaf", "dryadleaf_shadow");
      registerProjectile("smallspiritleaf", SmallSpiritLeafProjectile.class, "smallspiritleaf", null);
      registerProjectile("spiritorb", SpiritOrbProjectile.class, null, null);
      registerProjectile("barkbladeleaf", BarkBladeLeafProjectile.class, "barkbladeleaf", "barkbladeleaf_shadow");
      registerProjectile("tictactoepunish", TicTacToePunishProjectile.class, null, null);
      registerProjectile("spiritbeam", SpiritBeamProjectile.class, null, null);
      registerProjectile("spiritskull", SpiritSkullProjectile.class, "spiritskull", null);
      registerProjectile("flamelingshooterprojectile", FlamelingShooterProjectile.class, "evilsprotector1", "evilsprotector1_shadow");
      registerProjectile("chromaticbolt", ChromaticBoltProjectile.class, null, null);
      registerProjectile("ghostskull", GhostSkullProjectile.class, "skulls", "cannonball_shadow");
      registerProjectile("ancestormageprojectile", AncestorMageProjectile.class, null, null);
      registerProjectile("crazedravenfeather", CrazedRavenFeatherProjectile.class, "ravenbeaktip", null);
      registerProjectile("smitebeam", SmiteBeamProjectile.class, null, null);
      registerProjectile("weaponchargesmitebeam", WeaponChargeSmiteBeamProjectile.class, null, null);
      registerProjectile("ghostarrow", GhostArrowProjectile.class, "ghostarrow", null);
      registerProjectile("chargebeam", ChargeBeamProjectile.class, null, null);
      registerProjectile("chargeshower", ChargeShowerProjectile.class, null, null);
      registerProjectile("ascendedbolt", AscendedBoltProjectile.class, "ascendedbolt", "ascendedbolt_shadow");
      registerProjectile("ascendedboltsound", AscendedBoltSoundProjectile.class, "ascendedbolt", "ascendedbolt_shadow");
      registerProjectile("ascendedbomb", AscendedBombProjectile.class, "ascendedbomb", "ascendedbomb_shadow");
      registerProjectile("ascendedorb", AscendedOrbProjectile.class, "ascendedorb", "ascendedorb_shadow");
      registerProjectile("ascendedfracture", AscendedFractureProjectile.class, null, null);
      registerProjectile("ascendedslash", AscendedSlashProjectile.class, null, null);
      registerProjectile("ascendedbeam", AscendedBeamProjectile.class, "ascendedbeam", "ascendedbeam_shadow");
      registerProjectile("ascendedshardbomb", AscendedShardBombProjectile.class, "ascendedshardbomb", "ascendedshardbomb_shadow");
      registerProjectile("ascendedshard", AscendedShardProjectile.class, "ascendedshard", "ascendedshard_shadow");
      registerProjectile("ascendedgolembeam", AscendedGolemBeamProjectile.class, null, null);
      registerProjectile("ascendedgolemspawn", AscendedGolemSpawnProjectile.class, null, "queenspideregg_shadow");
      registerProjectile("ascendedstaffbeam", AscendedStaffBeamProjectile.class, "ascendedbeam", "ascendedbeam_shadow");
      registerProjectile("eyeofthevoidspawnprojectile", EyeOfTheVoidSpawnProjectile.class, "eyeofthevoidprojectile", "ascendedbeam_shadow");
      registerProjectile("ascendedbowbolt", AscendedBowBoltProjectile.class, "ascendedbolt", "ascendedbolt_shadow");
      registerProjectile("sharktoothbullet", SharkToothBulletProjectile.class, "crystalbullet", "crystalbullet_shadow");
      registerProjectile("whipprojectile", SurvivorWhipProjectile.class, null, null);
      registerProjectile("soundwave", SoundWaveProjectile.class, "keyofg", null);
      registerProjectile("grizzboltbullet", GrizzboltBulletProjectile.class, "grizzboltbullet", "grizzboltbullet_shadow");
      registerProjectile("moonlightjavelin", MoonlightJavelinProjectile.class, "moonlightjavelin", "moonlightjavelin");
      registerProjectile("minirocket", MiniRocketProjectile.class, "minirocket", "minirocket_shadow");
      registerProjectile("junkmechmachinegun", JunkMechMachineGunProjectile.class, null, null);
      registerProjectile("pyromancy", PyromancyProjectile.class, null, null);
      registerProjectile("rctoysoldierdynamite", RCToySoldierDynamiteProjectile.class, "dynamitestick", "dynamitestick_shadow");
      registerProjectile("rctoysoldierspawn", RCToySoldierSpawnProjectile.class, null, "queenspideregg_shadow");
      registerProjectile("mutantgroundacid", MutantGroundAcidProjectile.class, "mutantspit", "mutantspit_shadow");
      registerProjectile("mutantspit", MutantSpitProjectile.class, "mutantspit", "mutantspit_shadow");
      registerProjectile("magneticpull", MagneticPullProjectile.class, null, null);
      registerProjectile("makeshiftarc", MakeshiftArcThrowerProjectile.class, null, null);
      registerProjectile("patchworkgrenade", PatchworkGrenadeProjectile.class, "patchworkgrenade", "patchworkgrenade_shadow");
      registerProjectile("mutanthydragoospray", MutantHydraGooSprayProjectile.class, null, null);
      registerProjectile("mutanthydraspit", MutantHydraSpitProjectile.class, null, "mutanthydraspit_shadow");
      registerProjectile("phoenixfeather", PhoenixFeatherProjectile.class, "phoenixfeather", "phoenixfeather_shadow");
      registerProjectile("ashenwave", AshenWaveProjectile.class, null, null);
   }

   @Override
   protected void onRegistryClose() {
   }

   public static int registerProjectile(String stringID, Class<? extends Projectile> projectileClass, String texturePath, String shadowTexturePath) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register projectiles");
      }

      try {
         return instance.register(stringID, new ProjectileRegistry.ProjectileRegistryElement(projectileClass, texturePath, shadowTexturePath));
      } catch (NoSuchMethodException e) {
         System.err.println("Could not register projectile " + projectileClass.getSimpleName() + ": Missing constructor with no parameters");
         return -1;
      }
   }

   public static Projectile getProjectile(int id) {
      try {
         return instance.getElement(id).newInstance();
      } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
         e.printStackTrace();
         return null;
      }
   }

   public static Projectile getProjectile(int id, Level level) {
      Projectile out = getProjectile(id);
      out.setLevel(level);
      return out;
   }

   public static Projectile getProjectile(String stringID) {
      return getProjectile(getProjectileID(stringID));
   }

   public static Projectile getProjectile(String stringID, Level level) {
      return getProjectile(getProjectileID(stringID), level);
   }

   public static Projectile getProjectile(
      int id, Level level, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback, Mob owner
   ) {
      Projectile out = getProjectile(id);
      out.applyData(x, y, targetX, targetY, speed, distance, damage, knockback, owner);
      out.setLevel(level);
      return out;
   }

   public static Projectile getProjectile(
      int id, Level level, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, Mob owner
   ) {
      Projectile out = getProjectile(id, level);
      out.applyData(x, y, targetX, targetY, speed, distance, damage, owner);
      return out;
   }

   public static Projectile getProjectile(
      String stringID, Level level, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback, Mob owner
   ) {
      return getProjectile(getProjectileID(stringID), level, x, y, targetX, targetY, speed, distance, damage, knockback, owner);
   }

   public static Projectile getProjectile(
      String stringID, Level level, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, Mob owner
   ) {
      return getProjectile(getProjectileID(stringID), level, x, y, targetX, targetY, speed, distance, damage, owner);
   }

   public static int getProjectileID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static int getProjectileID(Class<? extends Projectile> clazz) {
      return instance.getElementID(clazz);
   }

   public static String getProjectileStringID(int id) {
      return instance.getElementStringID(id);
   }

   protected static class ProjectileRegistryElement extends ClassIDDataContainer<Projectile> {
      public final String texturePath;
      public final String shadowTexturePath;
      public GameTexture texture;
      public GameTexture shadowTexture;

      public ProjectileRegistryElement(Class<? extends Projectile> pickupClass, String texturePath, String shadowTexturePath) throws NoSuchMethodException {
         super(pickupClass);
         this.texturePath = texturePath;
         this.shadowTexturePath = shadowTexturePath;
      }

      public void loadTexture() {
         if (this.texturePath != null) {
            this.texture = GameTexture.fromFile("projectiles/" + this.texturePath);
         } else {
            this.texture = null;
         }

         if (this.shadowTexturePath != null) {
            this.shadowTexture = GameTexture.fromFile("projectiles/" + this.shadowTexturePath);
         } else {
            this.shadowTexture = null;
         }
      }
   }

   public static class Textures {
      public static void load() {
         ProjectileRegistry.instance.streamElements().forEach(ProjectileRegistry.ProjectileRegistryElement::loadTexture);
      }

      public static GameTexture getTexture(int id) {
         if (id >= 0 && id <= ProjectileRegistry.instance.size() - 1) {
            GameTexture texture = ProjectileRegistry.instance.getElement(id).texture;
            return texture == null ? GameResources.error : texture;
         } else {
            return GameResources.error;
         }
      }

      public static GameTexture getShadowTexture(int id) {
         return id >= 0 && id <= ProjectileRegistry.instance.size() - 1 ? ProjectileRegistry.instance.getElement(id).shadowTexture : null;
      }
   }
}
