package necesse.engine.registries;

import java.awt.Color;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import necesse.engine.GameLoadingScreen;
import necesse.engine.localization.Localization;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.modifiers.ModifierUpgradeValue;
import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.CryoImmunityBuff;
import necesse.entity.mobs.buffs.NecroticPoisonBuff;
import necesse.entity.mobs.buffs.NecroticSlowBuff;
import necesse.entity.mobs.buffs.PharaohManaSpentBuff;
import necesse.entity.mobs.buffs.staticBuffs.AblazeBuff;
import necesse.entity.mobs.buffs.staticBuffs.AgedChampionProwessBuff;
import necesse.entity.mobs.buffs.staticBuffs.ArcanicHoodActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.AscendedDarknessBuff;
import necesse.entity.mobs.buffs.staticBuffs.AscendedDashBuff;
import necesse.entity.mobs.buffs.staticBuffs.AshenBurialBuff;
import necesse.entity.mobs.buffs.staticBuffs.BannerOfDamageBuff;
import necesse.entity.mobs.buffs.staticBuffs.BannerOfDefenseBuff;
import necesse.entity.mobs.buffs.staticBuffs.BannerOfPeaceBuff;
import necesse.entity.mobs.buffs.staticBuffs.BannerOfSpeedBuff;
import necesse.entity.mobs.buffs.staticBuffs.BannerOfSummonSpeedBuff;
import necesse.entity.mobs.buffs.staticBuffs.BannerOfWarMobBuff;
import necesse.entity.mobs.buffs.staticBuffs.BarkBladeEnhancedBuff;
import necesse.entity.mobs.buffs.staticBuffs.BarkskinBuff;
import necesse.entity.mobs.buffs.staticBuffs.BashyBushFrenzyBuff;
import necesse.entity.mobs.buffs.staticBuffs.BleedDebuff;
import necesse.entity.mobs.buffs.staticBuffs.BloodClawStacksBuff;
import necesse.entity.mobs.buffs.staticBuffs.BloodGrimoireMarkedBuff;
import necesse.entity.mobs.buffs.staticBuffs.BloodplateCowlActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.BossNearbyBuff;
import necesse.entity.mobs.buffs.staticBuffs.BounceGlyphBuff;
import necesse.entity.mobs.buffs.staticBuffs.BrokenArmorBuff;
import necesse.entity.mobs.buffs.staticBuffs.Buff;
import necesse.entity.mobs.buffs.staticBuffs.BullseyeBuff;
import necesse.entity.mobs.buffs.staticBuffs.CampfireBuff;
import necesse.entity.mobs.buffs.staticBuffs.CantCancelSummonedCountBuff;
import necesse.entity.mobs.buffs.staticBuffs.ChilledBuff;
import necesse.entity.mobs.buffs.staticBuffs.CrushingDarknessBuff;
import necesse.entity.mobs.buffs.staticBuffs.CrystallizeBuff;
import necesse.entity.mobs.buffs.staticBuffs.DebugBuff;
import necesse.entity.mobs.buffs.staticBuffs.DebugInvisibilityBuff;
import necesse.entity.mobs.buffs.staticBuffs.DebugSpeedBuff;
import necesse.entity.mobs.buffs.staticBuffs.DemonicRageActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.DryadHauntedBuff;
import necesse.entity.mobs.buffs.staticBuffs.DryadPossessedBuff;
import necesse.entity.mobs.buffs.staticBuffs.EmeraldPoisonBuff;
import necesse.entity.mobs.buffs.staticBuffs.FlourCoveredSlowBuff;
import necesse.entity.mobs.buffs.staticBuffs.FoodBuff;
import necesse.entity.mobs.buffs.staticBuffs.ForceOfWindActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.FreezingBuff;
import necesse.entity.mobs.buffs.staticBuffs.FrostSlowBuff;
import necesse.entity.mobs.buffs.staticBuffs.FrostburnBuff;
import necesse.entity.mobs.buffs.staticBuffs.FrozenMobBuff;
import necesse.entity.mobs.buffs.staticBuffs.FrozenMobImmuneBuff;
import necesse.entity.mobs.buffs.staticBuffs.GhostBootsActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.GhostDashActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.GodmodeBuff;
import necesse.entity.mobs.buffs.staticBuffs.GuardianBraceletBuff;
import necesse.entity.mobs.buffs.staticBuffs.GuardianShellActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.HauntedBuff;
import necesse.entity.mobs.buffs.staticBuffs.HealthPotionFatigueBuff;
import necesse.entity.mobs.buffs.staticBuffs.HiddenCooldownBuff;
import necesse.entity.mobs.buffs.staticBuffs.HoldItemBuff;
import necesse.entity.mobs.buffs.staticBuffs.HoverBootsActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.HumanAngryBuff;
import necesse.entity.mobs.buffs.staticBuffs.HungryBuff;
import necesse.entity.mobs.buffs.staticBuffs.HydraDeafenedBuff;
import necesse.entity.mobs.buffs.staticBuffs.InvulnerableActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.IvyPoisonBuff;
import necesse.entity.mobs.buffs.staticBuffs.KatanaDashStacksBuff;
import necesse.entity.mobs.buffs.staticBuffs.KineticBootsActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.LeatherDashersActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.LifeEssenceStacksBuff;
import necesse.entity.mobs.buffs.staticBuffs.LifelineCooldownBuff;
import necesse.entity.mobs.buffs.staticBuffs.MagicianInvisBuff;
import necesse.entity.mobs.buffs.staticBuffs.MakeshiftRobotOverclockBuff;
import necesse.entity.mobs.buffs.staticBuffs.ManaExhaustionBuff;
import necesse.entity.mobs.buffs.staticBuffs.ManaPotionFatigueBuff;
import necesse.entity.mobs.buffs.staticBuffs.MoveSpeedBurstBuff;
import necesse.entity.mobs.buffs.staticBuffs.MyceliumHoodActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.MyceliumScarfActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.NettedBuff;
import necesse.entity.mobs.buffs.staticBuffs.NightsteelActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.NitroActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.OnFireBuff;
import necesse.entity.mobs.buffs.staticBuffs.OverchargedManaBuff;
import necesse.entity.mobs.buffs.staticBuffs.ParryBucklerParriedBuff;
import necesse.entity.mobs.buffs.staticBuffs.PerfectBlockBuff;
import necesse.entity.mobs.buffs.staticBuffs.PerfectStormBuff;
import necesse.entity.mobs.buffs.staticBuffs.PirateEscapeBuff;
import necesse.entity.mobs.buffs.staticBuffs.PiratePassiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.PyromancyOnFireBuff;
import necesse.entity.mobs.buffs.staticBuffs.QuadrupleChaseRangeBuff;
import necesse.entity.mobs.buffs.staticBuffs.QuickTinkerStacksBuff;
import necesse.entity.mobs.buffs.staticBuffs.QuicksandStacksBuff;
import necesse.entity.mobs.buffs.staticBuffs.RebornBuff;
import necesse.entity.mobs.buffs.staticBuffs.ReverseDamageGlyphBuff;
import necesse.entity.mobs.buffs.staticBuffs.RingOfAscensionActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.RunicEmpowermentActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.SandKnifeWoundBuff;
import necesse.entity.mobs.buffs.staticBuffs.SapphireSetActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.SecondBossWatchingBuff;
import necesse.entity.mobs.buffs.staticBuffs.SecondLifePerkBuff;
import necesse.entity.mobs.buffs.staticBuffs.SettlementFlagBuff;
import necesse.entity.mobs.buffs.staticBuffs.SettlerJogBuff;
import necesse.entity.mobs.buffs.staticBuffs.SettlerSprintBuff;
import necesse.entity.mobs.buffs.staticBuffs.ShadowHoodSetBuff;
import necesse.entity.mobs.buffs.staticBuffs.SharkBleedDebuff;
import necesse.entity.mobs.buffs.staticBuffs.SharkFrenzyBuff;
import necesse.entity.mobs.buffs.staticBuffs.ShieldActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.ShownCooldownBuff;
import necesse.entity.mobs.buffs.staticBuffs.ShownItemCooldownBuff;
import necesse.entity.mobs.buffs.staticBuffs.SimpleDebuff;
import necesse.entity.mobs.buffs.staticBuffs.SimpleModifierBuff;
import necesse.entity.mobs.buffs.staticBuffs.SimplePotionBuff;
import necesse.entity.mobs.buffs.staticBuffs.SleepingBuff;
import necesse.entity.mobs.buffs.staticBuffs.SlimeDomeActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.SlimeGreatbowSlowTargetBuff;
import necesse.entity.mobs.buffs.staticBuffs.SlimePoisonBuff;
import necesse.entity.mobs.buffs.staticBuffs.SnowCoveredBuff;
import necesse.entity.mobs.buffs.staticBuffs.SnowCoveredSlowBuff;
import necesse.entity.mobs.buffs.staticBuffs.SoldierFrenzyBuff;
import necesse.entity.mobs.buffs.staticBuffs.SoulseedConsecutiveExplosionsBuff;
import necesse.entity.mobs.buffs.staticBuffs.SoulstormSoulStacksBuff;
import necesse.entity.mobs.buffs.staticBuffs.SpawnInvincibilityBuff;
import necesse.entity.mobs.buffs.staticBuffs.SpiderCharmPoisonBuff;
import necesse.entity.mobs.buffs.staticBuffs.SpiderVenomBuff;
import necesse.entity.mobs.buffs.staticBuffs.SpiderWebSlowBuff;
import necesse.entity.mobs.buffs.staticBuffs.SpiritCorruptedBuff;
import necesse.entity.mobs.buffs.staticBuffs.SpiritHauntedBuff;
import necesse.entity.mobs.buffs.staticBuffs.SpiritPossessedBuff;
import necesse.entity.mobs.buffs.staticBuffs.StabbyBushFrenzyBuff;
import necesse.entity.mobs.buffs.staticBuffs.StaminaBuff;
import necesse.entity.mobs.buffs.staticBuffs.StarBarrierBuff;
import necesse.entity.mobs.buffs.staticBuffs.StarvingBuff;
import necesse.entity.mobs.buffs.staticBuffs.StatsBuff;
import necesse.entity.mobs.buffs.staticBuffs.StealthBasketArmorBuff;
import necesse.entity.mobs.buffs.staticBuffs.StinkFlaskBuff;
import necesse.entity.mobs.buffs.staticBuffs.StunnedDamageTakenIncreassedBuff;
import necesse.entity.mobs.buffs.staticBuffs.SummonedMobBuff;
import necesse.entity.mobs.buffs.staticBuffs.SummonedMountBuff;
import necesse.entity.mobs.buffs.staticBuffs.SummonedPetBuff;
import necesse.entity.mobs.buffs.staticBuffs.SummonersBestiaryBuff;
import necesse.entity.mobs.buffs.staticBuffs.SwampSporesBuff;
import necesse.entity.mobs.buffs.staticBuffs.TeleportSicknessBuff;
import necesse.entity.mobs.buffs.staticBuffs.ThornsPotionBuff;
import necesse.entity.mobs.buffs.staticBuffs.WebPotionBuff;
import necesse.entity.mobs.buffs.staticBuffs.WebPotionSlowBuff;
import necesse.entity.mobs.buffs.staticBuffs.WidowPoisonBuff;
import necesse.entity.mobs.buffs.staticBuffs.WindBootsActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.ZephyrBootsActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.ArmorBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.AgedChampionSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.AmethystSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.AncestorSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.AncientFossilHelmetSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.AncientFossilMaskSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.ArachnidSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.ArcanicHelmetSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.BattleChefSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.BloodplateCowlSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.CryoWitchSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.DawnSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.DeepfrostSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.DemonicSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.DryadCrownSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.DryadHatSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.DryadHelmetSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.DryadScarfSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.DuskSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.EmeraldSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.FrostSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.GlacialCircletBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.GlacialHelmetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.GunslingerSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.IvyCircletSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.IvyHatSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.IvyHelmetSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.IvyHoodSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.MakeshiftMagicSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.MakeshiftMeleeSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.MakeshiftRangedSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.MakeshiftSummonSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.MyceliumHoodSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.MyceliumScarfSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.NightSteelCircletSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.NightSteelHelmetSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.NightSteelSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.NightSteelVeilSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.NinjaSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.PharaohsSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.PhoenixSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.QuartzCrownSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.QuartzHelmetSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.RavenlordsHeaddressSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.RubySetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.RunicHatSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.RunicSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.SapphireSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.SetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.ShadowHatSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.ShadowHoodSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.SharkArmorSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.SharpshooterSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.SimpleUpgradeSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.SlimeHatSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.SlimeHelmetSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.SoldierCapSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.SoldierHelmetSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.SoulseedCrownSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.SpiderSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.SpideriteCrownSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.SpideriteHatSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.SpideriteHelmetSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.SpideriteSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.SupporterSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.ThiefSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.TungstenSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.VoidHatSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.VoidMaskSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.WidowSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.WitchRobesSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.AgedChampionScabbardBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.AmethystGlyphBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.BalancedFociBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.BlinkScepterTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.BloodstoneRingBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.BloodstoneRingRegenActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.CavelingsCollectionBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.CavelingsFootBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.ChallengersPauldronBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.ClockworkHeartBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.CompanionLocketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.DebugInvincibilityBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.EmeraldGlyphBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.ExplorerCloakBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.FinsBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.FireStoneBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.FoolsGambitTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.ForceOfWindTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.FrenzyOrbBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.FrostFlameBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.FrostStoneBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.GhostBootsTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.GuardianBraceletTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.GuardianShellTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.HoverBootsTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.IncursionEmpowermentAttackSpeedBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.IncursionEmpowermentCritChanceBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.IncursionEmpowermentRegenBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.JonasGambitTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.KineticBootsTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.LeatherDashersTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.LifelineBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.LovedBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.MagicFociBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.MeleeFociBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.MinersProstheticTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.MobilityCloakBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.MoonShrineBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.NecroticSoulSkullTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.NinjasMarkBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.PolarClawBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.RangeFociBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.RubyGlyphBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.SapphireGlyphBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.SecondWindTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.ShieldTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.ShineBeltBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.SimpleTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.SpiderCharmBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.SpiritBoardTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.SpiritGreavesTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.SummonFociBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.SummonersBestiaryTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.TopazGlyphBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.TravelerCloakBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.VoidPhasingStaffTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.VoodooShieldTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.WillOWispLanternBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.WindBootsTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.ZephyrBootsTrinketBuff;
import necesse.entity.mobs.buffs.staticBuffs.incursionBuffs.AlchemicalInterferenceBuff;
import necesse.entity.mobs.buffs.staticBuffs.incursionBuffs.FrenzyBuff;
import necesse.entity.mobs.buffs.staticBuffs.incursionBuffs.PerkFlamelingsCanDieBuff;
import necesse.entity.mobs.buffs.staticBuffs.incursionBuffs.TremorHappeningBuff;
import necesse.entity.mobs.buffs.staticBuffs.incursionBuffs.TremorsIncomingBuff;
import necesse.entity.mobs.buffs.staticBuffs.polymorph.PolymorphBuff;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;

public class BuffRegistry extends GameRegistry<BuffRegistry.BuffRegistryElement<?>> {
   public static Buff FRENZY;
   public static Buff TREMORS_INCOMING;
   public static Buff TREMOR_HAPPENING;
   public static Buff ALCHEMICAL_INTERFERENCE;
   public static Buff PERK_FLAMELINGS_CAN_DIE;
   public static Buff SECOND_LIFE_PERK;
   public static Buff DEBUG_BUFF;
   public static Buff SHADOWHOOD_SET;
   public static Buff DEBUG_INVIS;
   public static Buff DEBUG_INVINCIBILITY;
   public static Buff DEBUG_SPEED;
   public static Buff PIRATE_ESCAPE;
   public static Buff PIRATE_PASSIVE;
   public static Buff SUMMONED_PET;
   public static Buff SUMMONED_MOUNT;
   public static Buff SUMMONED_MOB;
   public static Buff HUMAN_ANGRY;
   public static Buff MOVE_SPEED_BURST;
   public static Buff BOSS_NEARBY;
   public static Buff SETTLEMENT_FLAG;
   public static Buff CAMPFIRE;
   public static Buff ANCIENT_FOSSIL_ACTIVE;
   public static Buff SPIDER_CHARGE;
   public static Buff BLOODPLATE_COWL_ACTIVE;
   public static Buff BLOOD_CLAW_STACKS_BUFF;
   public static Buff BLOODSTONE_RING_REGEN_ACTIVE_BUFF;
   public static Buff STAR_BARRIER_BUFF;
   public static Buff STABBY_BUSH_FRENZY_BUFF;
   public static Buff BASHY_BUSH_FRENZY_BUFF;
   public static Buff BANNER_OF_WAR_MOB;
   public static Buff OVERCHARGED_MANA;
   public static Buff ARCANIC_HOOD_ACTIVE;
   public static Buff LIFE_ESSENCE;
   public static Buff SAPPHIRE_GLYPH;
   public static Buff AMETHYST_GLYPH;
   public static Buff RUBY_GLYPH;
   public static Buff EMERALD_GLYPH;
   public static Buff TOPAZ_GLYPH;
   public static Buff QUADRUPLE_CHASE_RANGE;
   public static Buff SUMMONED_WILL_O_WISP;
   public static Buff QUICKSAND_STACKS;
   public static Buff BARK_SKIN;
   public static Buff AGED_CHAMPION_PROWESS;
   public static Buff BULLSEYE;
   public static Buff SUMMONERS_BESTIARY;
   public static Buff GUARDIAN_BRACELET;
   public static Buff BARKBLADE_ENHANCED;
   public static Buff PARRY_BUCKLER_PARRIED;
   public static Buff SOULSTORM_SOULS;
   public static Buff BOUNCE_GLYPH;
   public static Buff REVERSE_DAMAGE_GLYPH;
   public static Buff SOULSEED_CONSECUTIVE;
   public static Buff SUMMONED_GHOSTLYBOW;
   public static Buff SOLDIER_FRENZY;
   public static Buff SUMMONED_LOCUST;
   public static Buff PHARAOH_MANA_SPENT;
   public static Buff CRYO_IMMUNTY;
   public static Buff ASCENDED_DASH;
   public static Buff SPAWN_INVINCIBILITY;
   public static Buff SHARK_FRENZY;
   public static Buff SHARK_TORNADO;
   public static Buff MOON_SHRINE_BUFF;
   public static Buff QUICK_TINKER;
   public static Buff MAKESHIFT_ROBOT_OVERCLOCK;
   public static Buff REBORN;
   public static Buff MAGICIAN_INVIS;
   public static Buff LOVED;
   public static Buff HARDENED;
   public static Buff SETTLER_SPRINT;
   public static Buff SETTLER_JOG;
   public static Buff FROZEN_MOB_IMMUNE;
   public static Buff FROZEN_MOB;
   public static Buff THAWED_ENEMY;
   public static Buff INCURSION_EMPOWERMENT_CRIT_CHANCE;
   public static Buff INCURSION_EMPOWERMENT_ATTACK_SPEED;
   public static Buff INCURSION_EMPOWERMENT_REGEN;
   public static Buff LEATHER_DASHERS_ACTIVE;
   public static Buff ZEPHYR_BOOTS_ACTIVE;
   public static Buff WIND_BOOTS_ACTIVE;
   public static Buff HOVER_BOOTS_ACTIVE;
   public static Buff GHOST_BOOTS_ACTIVE;
   public static Buff KINETIC_BOOTS_ACTIVE;
   public static Buff NITRO_ACTIVE;
   public static Buff FOW_ACTIVE;
   public static Buff INVULNERABLE_ACTIVE;
   public static Buff GHOST_DASH_ACTIVE;
   public static Buff KATANA_DASH_STACKS;
   public static Buff GUARDIAN_SHELL_ACTIVE;
   public static Buff MYCELIUM_HOOD_ACTIVE;
   public static Buff MYCELIUM_SCARF_ACTIVE;
   public static Buff NIGHTSTEEL_ACTIVE;
   public static Buff SLIME_DOME_ACTIVE;
   public static Buff RUNIC_EMPOWERMENT;
   public static Buff SAPPHIRE_SET_ACTIVE;
   public static Buff SHIELD_ACTIVE;
   public static Buff PERFECT_BLOCK;
   public static Buff SLEEPING;
   public static Buff SWAMP_SPORES;
   public static Buff DEMONIC_RAGE;
   public static Buff PERFECT_STORM;
   public static Buff RING_OF_ASCENSION_ACTIVE;
   public static StatsBuff EQUIPMENT_BUFF;
   public static FoodBuff[] FOOD_BUFFS;
   public static FoodBuff FOOD_DEBUFF;
   public static Buff HUNGRY_BUFF;
   public static Buff STARVING_BUFF;
   public static Buff STAMINA_BUFF;
   public static Buff HOLD_ITEM_BUFF;
   public static Buff GODMODE_BUFF;
   public static final BuffRegistry instance = new BuffRegistry();
   private static int totalSetBonuses = 0;

   private BuffRegistry() {
      super("Buff", 32762);
   }

   @Override
   public void registerCore() {
      GameLoadingScreen.drawLoadingString(Localization.translate("loading", "buffs"));
      DEBUG_BUFF = registerBuff("debugbuff", new DebugBuff());
      DEBUG_INVIS = registerBuff("debuginvisibility", new DebugInvisibilityBuff());
      DEBUG_INVINCIBILITY = registerBuff("debuginvincibility", new DebugInvincibilityBuff());
      DEBUG_SPEED = registerBuff("debugspeed", new DebugSpeedBuff());
      GODMODE_BUFF = registerBuff("godmode", new GodmodeBuff());
      registerBuff("speedpotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.SPEED, 0.2F)));
      registerBuff("greaterspeedpotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.SPEED, 0.3F)));
      registerBuff("healthregenpotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 0.5F)));
      registerBuff("greaterhealthregenpotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 1.0F)));
      registerBuff("manaregenpotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.COMBAT_MANA_REGEN, 3.0F)));
      registerBuff("greatermanaregenpotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.COMBAT_MANA_REGEN, 5.0F)));
      registerBuff("attackspeedpotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.15F)));
      registerBuff("greaterattackspeedpotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.3F)));
      registerBuff("invisibilitypotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.INVISIBILITY, true)));
      registerBuff("fireresistancepotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.FIRE_DAMAGE, 0.0F).max(0.0F)));
      registerBuff(
         "fishingpotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.FISHING_POWER, 20), new ModifierValue<>(BuffModifiers.FISHING_LINES, 1))
      );
      registerBuff(
         "greaterfishingpotion",
         new SimplePotionBuff(new ModifierValue<>(BuffModifiers.FISHING_POWER, 30), new ModifierValue<>(BuffModifiers.FISHING_LINES, 2))
      );
      registerBuff("battlepotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.1F)));
      registerBuff("greaterbattlepotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.15F)));
      registerBuff("resistancepotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.ARMOR_FLAT, 8)));
      registerBuff("greaterresistancepotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.ARMOR_FLAT, 32)));
      registerBuff("spelunkerpotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.SPELUNKER, true)));
      registerBuff("treasurepotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.TREASURE_HUNTER, true)));
      registerBuff("trackerpotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.ENEMY_TRACKER, true)));
      registerBuff("passivepotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.MOB_SPAWN_RATE, 0.0F).max(0.1F)));
      registerBuff("buildingpotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.BUILDING_SPEED, 0.5F)));
      registerBuff("greaterbuildingpotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.BUILDING_SPEED, 1.0F)));
      registerBuff("miningpotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.MINING_SPEED, 0.4F)));
      registerBuff("greaterminingpotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.MINING_SPEED, 0.8F)));
      registerBuff("thornspotion", new ThornsPotionBuff());
      registerBuff("knockbackpotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.KNOCKBACK_OUT, 0.5F)));
      registerBuff("rapidpotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.PROJECTILE_VELOCITY, 0.5F)));
      registerBuff("greaterrapidpotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.PROJECTILE_VELOCITY, 1.0F)));
      registerBuff("accuracypotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.1F)));
      registerBuff("greateraccuracypotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.2F)));
      registerBuff("webpotion", new WebPotionBuff());
      registerBuff("strengthpotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.MELEE_DAMAGE, 0.1F)));
      registerBuff("rangerpotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.RANGED_DAMAGE, 0.1F)));
      registerBuff("wisdompotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.MAGIC_DAMAGE, 0.1F)));
      registerBuff("minionpotion", new SimplePotionBuff(new ModifierValue<>(BuffModifiers.MAX_SUMMONS, 1)));
      BuffRegistry.Potions.STINKFLASK = registerBuff("stinkflask", new StinkFlaskBuff());
      BuffRegistry.Debuffs.HEALTH_POT_FATIGUE = registerBuff("healthpotionfatigue", new HealthPotionFatigueBuff());
      BuffRegistry.Debuffs.MANA_POT_FATIGUE = registerBuff("manapotionfatigue", new ManaPotionFatigueBuff());
      BuffRegistry.Debuffs.MANA_EXHAUSTION = registerBuff("manaexhaustion", new ManaExhaustionBuff());
      BuffRegistry.Debuffs.ON_FIRE = registerBuff("onfire", new OnFireBuff());
      BuffRegistry.Debuffs.ABLAZE = registerBuff("ablaze", new AblazeBuff());
      BuffRegistry.Debuffs.BROKEN_ARMOR = registerBuff("brokenarmor", new BrokenArmorBuff());
      BuffRegistry.Debuffs.TELEPORT_SICKNESS = registerBuff("teleportsickness", new TeleportSicknessBuff());
      BuffRegistry.Debuffs.LIFELINE_COOLDOWN = registerBuff("lifelinecooldown", new LifelineCooldownBuff());
      BuffRegistry.Debuffs.QUARTZ_SET_COOLDOWN = registerBuff("quartzsetcooldown", new ShownCooldownBuff());
      BuffRegistry.Debuffs.GUNSLINGER_SET_COOLDOWN = registerBuff("gunslingersetcooldown", new ShownCooldownBuff());
      BuffRegistry.Debuffs.VOID_SET_COOLDOWN = registerBuff("voidsetcooldown", new ShownCooldownBuff());
      BuffRegistry.Debuffs.DASH_COOLDOWN = registerBuff("dashcooldown", new ShownCooldownBuff(250, true));
      BuffRegistry.Debuffs.SPIDER_WEB_SLOW = registerBuff("spiderwebslow", new SpiderWebSlowBuff());
      BuffRegistry.Debuffs.WEB_POTION_SLOW = registerBuff("webpotionslow", new WebPotionSlowBuff());
      BuffRegistry.Debuffs.SPIDER_VENOM = registerBuff("spidervenom", new SpiderVenomBuff());
      BuffRegistry.Debuffs.NECROTIC_POISON = registerBuff("necroticpoison", new NecroticPoisonBuff());
      BuffRegistry.Debuffs.NECROTIC_SLOW = registerBuff("necroticslow", new NecroticSlowBuff());
      BuffRegistry.Debuffs.IVY_POISON = registerBuff("ivypoison", new IvyPoisonBuff());
      BuffRegistry.Debuffs.WIDOW_POISON = registerBuff("widowpoison", new WidowPoisonBuff());
      BuffRegistry.Debuffs.CHILLED = registerBuff("chilled", new ChilledBuff());
      BuffRegistry.Debuffs.FREEZING = registerBuff("freezing", new FreezingBuff());
      BuffRegistry.Debuffs.FROSTBURN = registerBuff("frostburn", new FrostburnBuff());
      BuffRegistry.Debuffs.FROSTSLOW = registerBuff("frostslow", new FrostSlowBuff());
      BuffRegistry.Debuffs.SNOW_COVERED_DEBUFF = registerBuff("snowcovered", new SnowCoveredBuff());
      BuffRegistry.Debuffs.SNOW_COVERED_SLOW_DEBUFF = registerBuff("snowcoveredslow", new SnowCoveredSlowBuff());
      BuffRegistry.Debuffs.FLOUR_COVERED_SLOW_DEBUFF = registerBuff("flourcoveredslow", new FlourCoveredSlowBuff());
      BuffRegistry.Debuffs.HAUNTED = registerBuff("haunted", new HauntedBuff());
      BuffRegistry.Debuffs.EMERALD_POISON = registerBuff("emeraldpoison", new EmeraldPoisonBuff());
      BuffRegistry.Debuffs.ASHEN_BURIAL = registerBuff("ashenburial", new AshenBurialBuff());
      BuffRegistry.Debuffs.RUNIC_HAT_COOLDOWN = registerBuff("runichatcooldown", new ShownCooldownBuff());
      BuffRegistry.Debuffs.GUARDIAN_SHELL_COOLDOWN = registerBuff("guardianshellcooldown", new ShownCooldownBuff());
      BuffRegistry.Debuffs.NECROTIC_SOUL_SKULL_COOLDOWN = registerBuff("necroticsoulskullcooldown", new ShownCooldownBuff());
      BuffRegistry.Debuffs.ANCESTOR_SET_COOLDOWN = registerBuff("ancestorsetcooldown", new ShownCooldownBuff());
      BuffRegistry.Debuffs.ANCIENT_FOSSIL_SET_COOLDOWN = registerBuff("ancientfossilsetcooldown", new ShownCooldownBuff());
      BuffRegistry.Debuffs.SLIME_SET_COOLDOWN = registerBuff("slimesetcooldown", new ShownCooldownBuff());
      BuffRegistry.Debuffs.NIGHTSTEEL_SET_COOLDOWN = registerBuff("nightsteelsetcooldown", new ShownCooldownBuff());
      BuffRegistry.Debuffs.SPIDERITE_SET_COOLDOWN = registerBuff("spideritesetcooldown", new ShownCooldownBuff());
      BuffRegistry.Debuffs.SAPPHIRE_SET_COOLDOWN = registerBuff("sapphiresetcooldown", new ShownCooldownBuff());
      BuffRegistry.Debuffs.SPIDER_CHARM_POISON = registerBuff("spidercharmpoison", new SpiderCharmPoisonBuff());
      BuffRegistry.Debuffs.SLIME_POISON = registerBuff("slimepoison", new SlimePoisonBuff());
      BuffRegistry.Debuffs.SAND_KNIFE_WOUND_BUFF = registerBuff("sandknifewound", new SandKnifeWoundBuff());
      BuffRegistry.Debuffs.CRYSTALLIZE_BUFF = registerBuff("crystallize", new CrystallizeBuff());
      BuffRegistry.Debuffs.BLOOD_GRIMOIRE_MARKED_DEBUFF = registerBuff("bloodgrimoiredebuff", new BloodGrimoireMarkedBuff());
      BuffRegistry.Debuffs.SLIME_GREATBOW_SLOW_TARGET_DEBUFF = registerBuff("slimegreatbowdebuff", new SlimeGreatbowSlowTargetBuff());
      BuffRegistry.Debuffs.BLOOD_GRIMOIRE_COOLDOWN = registerBuff("bloodgrimoirecooldown", new ShownItemCooldownBuff(1, true, "items/bloodgrimoire"));
      BuffRegistry.Debuffs.THE_CRIMSON_SKY_COOLDOWN = registerBuff("thecrimsonskycooldown", new ShownItemCooldownBuff(1, true, "items/thecrimsonsky"));
      BuffRegistry.Debuffs.RAVENBEAK_SPEAR_COOLDOWN = registerBuff("ravenbeakspearcooldown", new ShownItemCooldownBuff(1, true, "items/ravenbeakspear"));
      BuffRegistry.Debuffs.CRUSHING_DARKNESS = registerBuff("crushingdarkness", new CrushingDarknessBuff());
      BuffRegistry.Debuffs.KATANA_DASH_COOLDOWN = registerBuff("katanadashcooldown", new ShownCooldownBuff());
      BuffRegistry.Debuffs.RUNEBOUND_SCEPTER_COOLDOWN = registerBuff("runeboundsceptercooldown", new ShownCooldownBuff());
      BuffRegistry.Debuffs.CAPTORS_SHORTBOW_COOLDOWN = registerBuff("captorsshortbowcooldown", new ShownItemCooldownBuff(1, true, "items/captorsshortbow"));
      BuffRegistry.Debuffs.NETTED_DEBUFF = registerBuff("netted", new NettedBuff());
      BuffRegistry.Debuffs.DRYAD_HAUNTED = registerBuff("dryadhaunted", new DryadHauntedBuff());
      BuffRegistry.Debuffs.DRYAD_POSSESSED = registerBuff("dryadpossessed", new DryadPossessedBuff());
      BuffRegistry.Debuffs.SPIRIT_HAUNTED = registerBuff("spirithaunted", new SpiritHauntedBuff());
      BuffRegistry.Debuffs.SPIRIT_POSSESSED = registerBuff("spiritpossessed", new SpiritPossessedBuff());
      BuffRegistry.Debuffs.STUNNED_DAMAGE_TAKEN_INCREASED = registerBuff("stunneddamagetakenincreased", new StunnedDamageTakenIncreassedBuff());
      BuffRegistry.Debuffs.SPIRIT_CORRUPTED = registerBuff("spiritcorrupted", new SpiritCorruptedBuff());
      BuffRegistry.Debuffs.DEMONIC_SET_COOLDOWN = registerBuff("demonicsetcooldown", new ShownCooldownBuff());
      BuffRegistry.Debuffs.SOULSEED_COOLDOWN = registerBuff("soulseedcooldown", new ShownCooldownBuff());
      BuffRegistry.Debuffs.THIEF_THROW_COOLDOWN = registerBuff("thiefthrowcooldown", new ShownCooldownBuff());
      BuffRegistry.Debuffs.THIEF_STEAL_COOLDOWN = registerBuff("thiefstealcooldown", new HiddenCooldownBuff());
      BuffRegistry.Debuffs.PERFECT_STORM_ONHIT_COOLDOWN = registerBuff("perfectstormonhitcooldown", new HiddenCooldownBuff());
      BuffRegistry.Debuffs.PERFECT_STORM_ABILITY_COOLDOWN = registerBuff("perfectstormabilitycooldown", new ShownCooldownBuff());
      BuffRegistry.Debuffs.CRYOWITCH_COOLDOWN = registerBuff("cryowitchcooldown", new ShownCooldownBuff());
      BuffRegistry.Debuffs.CRYOWITCH_COOLDOWN_ONHIT = registerBuff("cryowitchcooldownonhit", new HiddenCooldownBuff());
      BuffRegistry.Debuffs.ASCENDED_DARKNESS = registerBuff("ascendeddarkness", new AscendedDarknessBuff());
      BuffRegistry.Debuffs.BLEED = registerBuff("bleed", new BleedDebuff());
      BuffRegistry.Debuffs.SHARKARMOR_BLEED = registerBuff("sharkarmorbleed", new BleedDebuff());
      BuffRegistry.Debuffs.SHARK_BLEED = registerBuff("sharkbleed", new SharkBleedDebuff());
      BuffRegistry.Debuffs.PYROMANCY_ONFIRE = registerBuff("pyromancyfire", new PyromancyOnFireBuff());
      BuffRegistry.Debuffs.HYDRA_DEAFENED = registerBuff("hydradeafened", new HydraDeafenedBuff());
      BuffRegistry.Debuffs.PHOENIX_COOLDOWN = registerBuff("phoenixsetcooldown", new ShownCooldownBuff());
      BuffRegistry.Debuffs.GENERIC_ICE_SLOW = registerBuff(
         "genericiceslowdebuff", new SimpleDebuff(new Color(111, 246, 246), "genericiceslowdebuff", new ModifierValue<>(BuffModifiers.SLOW, 0.3F))
      );
      BuffRegistry.Debuffs.GENERIC_ONFIRE = registerBuff(
         "genericonfiredebuff", new SimpleDebuff(new Color(227, 72, 13), "genericonfiredebuff", new ModifierValue<>(BuffModifiers.FIRE_DAMAGE_FLAT, 5.0F))
      );
      BuffRegistry.Debuffs.GENERIC_POISON = registerBuff(
         "genericpoisondebuff", new SimpleDebuff(new Color(111, 67, 187), "genericpoisondebuff", new ModifierValue<>(BuffModifiers.POISON_DAMAGE_FLAT, 5.0F))
      );
      BuffRegistry.Debuffs.GENERIC_DAMAGE_DEBUFF = registerBuff(
         "genericdamagedebuff", new SimpleDebuff(new Color(150, 150, 150), "genericdamagedebuff", new ModifierValue<>(BuffModifiers.ALL_DAMAGE, -0.2F))
      );
      BuffRegistry.Debuffs.GENERIC_WEAKNESS = registerBuff(
         "genericweaknessdebuff",
         new SimpleDebuff(new Color(150, 150, 150), "genericweaknessdebuff", new ModifierValue<>(BuffModifiers.INCOMING_DAMAGE_MOD, 0.2F))
      );
      BuffRegistry.Debuffs.GENERIC_ATTACKSPEED_SLOW = registerBuff(
         "genericattackspeeddebuff",
         new SimpleDebuff(new Color(194, 242, 246), "genericattackspeeddebuff", new ModifierValue<>(BuffModifiers.ATTACK_SPEED, -0.3F))
      );
      BuffRegistry.Debuffs.POLYMORPH_CHICKEN = registerBuff("chickenpolymorph", new PolymorphBuff("chickenpolymorph"));
      BuffRegistry.Debuffs.POLYMORPH_FROG = registerBuff("frogpolymorph", new PolymorphBuff("frogpolymorph"));
      BuffRegistry.Debuffs.POLYMORPH_RABBIT = registerBuff("rabbitpolymorph", new PolymorphBuff("rabbitpolymorph"));
      BuffRegistry.Debuffs.POLYMORPH_DUCK = registerBuff("duckpolymorph", new PolymorphBuff("duckpolymorph"));
      BuffRegistry.Debuffs.POLYMORPH_TURTLE = registerBuff("turtlepolymorph", new PolymorphBuff("turtlepolymorph"));
      BuffRegistry.Debuffs.POLYMORPH_ROOSTER = registerBuff("roosterpolymorph", new PolymorphBuff("roosterpolymorph"));
      BuffRegistry.Debuffs.POLYMORPH_SQUIRREL = registerBuff("squirrelpolymorph", new PolymorphBuff("squirrelpolymorph"));
      BuffRegistry.Debuffs.SECOND_BOSS_WATCHING = registerBuff("secondbosswatching", new SecondBossWatchingBuff());
      BuffRegistry.SetBonuses.LEATHER = registerBuff(
         "leathersetbonus",
         new SimpleUpgradeSetBonusBuff(
            new ModifierUpgradeValue<>(BuffModifiers.ATTACK_SPEED, new FloatUpgradeValue().setBaseValue(0.1F).setUpgradedValue(1.0F, 0.25F))
         )
      );
      BuffRegistry.SetBonuses.CLOTH_ROBE = registerBuff(
         "clothrobesetbonus",
         new SimpleUpgradeSetBonusBuff(
            new ModifierUpgradeValue<>(BuffModifiers.MAGIC_DAMAGE, new FloatUpgradeValue().setBaseValue(0.15F).setUpgradedValue(1.0F, 0.25F)),
            new ModifierUpgradeValue<>(BuffModifiers.MAX_MANA_FLAT, new IntUpgradeValue(0, 0.1F).setBaseValue(30).setUpgradedValue(1.0F, 250))
         )
      );
      BuffRegistry.SetBonuses.COPPER = registerBuff(
         "coppersetbonus",
         new SimpleUpgradeSetBonusBuff(new ModifierUpgradeValue<>(BuffModifiers.ARMOR_FLAT, new IntUpgradeValue().setBaseValue(2).setUpgradedValue(1.0F, 6)))
      );
      BuffRegistry.SetBonuses.IRON = registerBuff(
         "ironsetbonus",
         new SimpleUpgradeSetBonusBuff(new ModifierUpgradeValue<>(BuffModifiers.ARMOR_FLAT, new IntUpgradeValue().setBaseValue(2).setUpgradedValue(1.0F, 6)))
      );
      BuffRegistry.SetBonuses.GOLD_CROWN = registerBuff(
         "goldcrownsetbonus",
         new SimpleUpgradeSetBonusBuff(new ModifierUpgradeValue<>(BuffModifiers.MAX_SUMMONS, new IntUpgradeValue().setBaseValue(1).setUpgradedValue(1.0F, 2)))
      );
      BuffRegistry.SetBonuses.GOLD = registerBuff(
         "goldsetbonus",
         new SimpleUpgradeSetBonusBuff(new ModifierUpgradeValue<>(BuffModifiers.ARMOR_FLAT, new IntUpgradeValue().setBaseValue(2).setUpgradedValue(1.0F, 6)))
      );
      BuffRegistry.SetBonuses.SPIDER = registerBuff("spidersetbonus", new SpiderSetBonusBuff());
      BuffRegistry.SetBonuses.FROST_HELMET = registerBuff("frostsetbonus", new FrostSetBonusBuff());
      BuffRegistry.SetBonuses.FROST_HOOD = registerBuff(
         "frosthoodsetbonus",
         new SimpleUpgradeSetBonusBuff(
            new ModifierUpgradeValue<>(BuffModifiers.CRIT_CHANCE, new FloatUpgradeValue().setBaseValue(0.1F).setUpgradedValue(1.0F, 0.2F)),
            new ModifierUpgradeValue<>(BuffModifiers.PROJECTILE_VELOCITY, new FloatUpgradeValue().setBaseValue(0.5F).setUpgradedValue(1.0F, 1.0F))
         )
      );
      BuffRegistry.SetBonuses.FROST_HAT = registerBuff(
         "frosthatsetbonus",
         new SimpleUpgradeSetBonusBuff(
            new ModifierUpgradeValue<>(BuffModifiers.MAGIC_CRIT_CHANCE, new FloatUpgradeValue().setBaseValue(0.1F).setUpgradedValue(1.0F, 0.2F)),
            new ModifierUpgradeValue<>(BuffModifiers.MAX_MANA_FLAT, new IntUpgradeValue(0, 0.1F).setBaseValue(60).setUpgradedValue(1.0F, 250))
         )
      );
      BuffRegistry.SetBonuses.DEMONIC = registerBuff("demonicsetbonus", new DemonicSetBonusBuff());
      BuffRegistry.SetBonuses.VOIDMASK = registerBuff("voidmasksetbonus", new VoidMaskSetBonusBuff());
      BuffRegistry.SetBonuses.VOIDHAT = registerBuff("voidhatsetbonus", new VoidHatSetBonusBuff());
      BuffRegistry.SetBonuses.NINJA = registerBuff("ninjasetbonus", new NinjaSetBonusBuff());
      BuffRegistry.SetBonuses.WITCH_ROBES = registerBuff("witchrobessetbonus", new WitchRobesSetBonusBuff());
      BuffRegistry.SetBonuses.CRYOWITCH = registerBuff("cryowitchsetbonus", new CryoWitchSetBonusBuff());
      BuffRegistry.SetBonuses.RUNIC = registerBuff("runicsetbonus", new RunicSetBonusBuff());
      BuffRegistry.SetBonuses.RUNICHAT = registerBuff("runichatsetbonus", new RunicHatSetBonusBuff());
      BuffRegistry.SetBonuses.IVYHELMET = registerBuff("ivyhelmetsetbonus", new IvyHelmetSetBonusBuff());
      BuffRegistry.SetBonuses.IVYHOOD = registerBuff("ivyhoodsetbonus", new IvyHoodSetBonusBuff());
      BuffRegistry.SetBonuses.IVYHAT = registerBuff("ivyhatsetbonus", new IvyHatSetBonusBuff());
      BuffRegistry.SetBonuses.IVYCIRCLET = registerBuff("ivycircletsetbonus", new IvyCircletSetBonusBuff());
      BuffRegistry.SetBonuses.QUARTZHELMET = registerBuff("quartzhelmetsetbonus", new QuartzHelmetSetBonusBuff());
      BuffRegistry.SetBonuses.QUARTZCROWN = registerBuff("quartzcrownsetbonus", new QuartzCrownSetBonusBuff());
      BuffRegistry.SetBonuses.GUNSLINGER = registerBuff("gunslingersetbonus", new GunslingerSetBonusBuff());
      BuffRegistry.SetBonuses.TUNGSTEN = registerBuff("tungstensetbonus", new TungstenSetBonusBuff());
      BuffRegistry.SetBonuses.SHADOWHOOD = registerBuff("shadowhoodsetbonus", new ShadowHoodSetBonusBuff());
      BuffRegistry.SetBonuses.SHADOWHAT = registerBuff("shadowhatsetbonus", new ShadowHatSetBonusBuff());
      BuffRegistry.SetBonuses.GLACIALHELMET = registerBuff("glacialhelmetsetbonus", new GlacialHelmetBonusBuff());
      BuffRegistry.SetBonuses.GLACIALCIRCLET = registerBuff("glacialcircletsetbonus", new GlacialCircletBonusBuff());
      BuffRegistry.SetBonuses.DRYAD_HELMET = registerBuff("dryadhelmetsetbonus", new DryadHelmetSetBonusBuff());
      BuffRegistry.SetBonuses.DRYAD_SCARF = registerBuff("dryadscarfsetbonus", new DryadScarfSetBonusBuff());
      BuffRegistry.SetBonuses.DRYAD_HAT = registerBuff("dryadhatsetbonus", new DryadHatSetBonusBuff());
      BuffRegistry.SetBonuses.DRYAD_CROWN = registerBuff("dryadcrownsetbonus", new DryadCrownSetBonusBuff());
      BuffRegistry.SetBonuses.MYCELIUMHOOD = registerBuff("myceliumhoodsetbonus", new MyceliumHoodSetBonusBuff());
      BuffRegistry.SetBonuses.MYCELIUMSCARF = registerBuff("myceliumscarfsetbonus", new MyceliumScarfSetBonusBuff());
      BuffRegistry.SetBonuses.WIDOW = registerBuff("widowsetbonus", new WidowSetBonusBuff());
      BuffRegistry.SetBonuses.AGED_CHAMPION = registerBuff("agedchampionsetbonus", new AgedChampionSetBonusBuff());
      BuffRegistry.SetBonuses.ANCESTOR = registerBuff("ancestorsetbonus", new AncestorSetBonusBuff());
      BuffRegistry.SetBonuses.ANCIENT_FOSSIL_HELMET = registerBuff("ancientfossilhelmetsetbonus", new AncientFossilHelmetSetBonusBuff());
      BuffRegistry.SetBonuses.ANCIENT_FOSSIL_MASK = registerBuff("ancientfossilmasksetbonus", new AncientFossilMaskSetBonusBuff());
      BuffRegistry.SetBonuses.SHARPSHOOTER = registerBuff("sharpshootersetbonus", new SharpshooterSetBonusBuff());
      BuffRegistry.SetBonuses.SLIMEHELMET = registerBuff("slimehelmetsetbonus", new SlimeHelmetSetBonusBuff());
      BuffRegistry.SetBonuses.SLIMEHAT = registerBuff("slimehatsetbonus", new SlimeHatSetBonusBuff());
      BuffRegistry.SetBonuses.NIGHTSTEEL = registerBuff("nightsteelsetbonus", new NightSteelSetBonusBuff());
      BuffRegistry.SetBonuses.NIGHTSTEEL_HELMET = registerBuff("nightsteelhelmetsetbonus", new NightSteelHelmetSetBonusBuff());
      BuffRegistry.SetBonuses.NIGHTSTEEL_VEIL = registerBuff("nightsteelveilsetbonus", new NightSteelVeilSetBonusBuff());
      BuffRegistry.SetBonuses.NIGHTSTEEL_CIRCLET = registerBuff("nightsteelcircletsetbonus", new NightSteelCircletSetBonusBuff());
      BuffRegistry.SetBonuses.BLOODPLATE_COWL = registerBuff("bloodplatecowlsetbonus", new BloodplateCowlSetBonusBuff());
      BuffRegistry.SetBonuses.SPIDERITE = registerBuff("spideritesetbonus", new SpideriteSetBonusBuff());
      BuffRegistry.SetBonuses.BATTLE_CHEF = registerBuff("battlechefsetbonus", new BattleChefSetBonusBuff());
      BuffRegistry.SetBonuses.SPIDERITE_HELMET = registerBuff("spideritehelmetsetbonus", new SpideriteHelmetSetBonusBuff());
      BuffRegistry.SetBonuses.SPIDERITE_HAT = registerBuff("spideritehatsetbonus", new SpideriteHatSetBonusBuff());
      BuffRegistry.SetBonuses.SPIDERITE_CROWN = registerBuff("spideritecrownsetbonus", new SpideriteCrownSetBonusBuff());
      BuffRegistry.SetBonuses.RAVENLORDS_HEADDRESS = registerBuff("ravenlordsheaddresssetbonus", new RavenlordsHeaddressSetBonusBuff());
      BuffRegistry.SetBonuses.DAWN = registerBuff("dawnsetbonus", new DawnSetBonusBuff());
      BuffRegistry.SetBonuses.DUSK = registerBuff("dusksetbonus", new DuskSetBonusBuff());
      BuffRegistry.SetBonuses.AMETHYST = registerBuff("amethystsetbonus", new AmethystSetBonusBuff());
      BuffRegistry.SetBonuses.SAPPHIRE = registerBuff("sapphiresetbonus", new SapphireSetBonusBuff());
      BuffRegistry.SetBonuses.EMERALD = registerBuff("emeraldsetbonus", new EmeraldSetBonusBuff());
      BuffRegistry.SetBonuses.RUBY = registerBuff("rubysetbonus", new RubySetBonusBuff());
      BuffRegistry.SetBonuses.MAKESHIFT_MELEE = registerBuff("makeshiftmeleesetbonus", new MakeshiftMeleeSetBonusBuff());
      BuffRegistry.SetBonuses.MAKESHIFT_RANGED = registerBuff("makeshiftrangedsetbonus", new MakeshiftRangedSetBonusBuff());
      BuffRegistry.SetBonuses.MAKESHIFT_MAGIC = registerBuff("makeshiftmagicsetbonus", new MakeshiftMagicSetBonusBuff());
      BuffRegistry.SetBonuses.MAKESHIFT_SUMMON = registerBuff("makeshiftsummonsetbonus", new MakeshiftSummonSetBonusBuff());
      BuffRegistry.SetBonuses.ARCANIC_HOOD = registerBuff("arcanichelmetsetbonus", new ArcanicHelmetSetBonusBuff());
      BuffRegistry.SetBonuses.ARACHNID = registerBuff("arachnidsetbonus", new ArachnidSetBonusBuff());
      BuffRegistry.SetBonuses.GHOSTLY_ARCHER = registerBuff("deepfrostsetbonus", new DeepfrostSetBonusBuff());
      BuffRegistry.SetBonuses.SOULSEED = registerBuff("soulseedcrownsetbonus", new SoulseedCrownSetBonusBuff());
      BuffRegistry.SetBonuses.SOLDIERHELMET = registerBuff("soldierhelmetsetbonus", new SoldierHelmetSetBonusBuff());
      BuffRegistry.SetBonuses.SOLDIERCAP = registerBuff("soldiercapsetbonus", new SoldierCapSetBonusBuff());
      BuffRegistry.SetBonuses.THIEF = registerBuff("thiefsetbonus", new ThiefSetBonusBuff());
      BuffRegistry.SetBonuses.PHARAOH = registerBuff("pharaohssetbonus", new PharaohsSetBonusBuff());
      BuffRegistry.SetBonuses.SUPPORTER = registerBuff("supportersetbonus", new SupporterSetBonusBuff());
      BuffRegistry.SetBonuses.SHARK_ARMOR = registerBuff("sharkarmorsetbonus", new SharkArmorSetBonusBuff());
      BuffRegistry.ArmorBuffs.STEALTH_BASKET = registerBuff("stealthbasketbuff", new StealthBasketArmorBuff());
      BuffRegistry.SetBonuses.PHOENIX = registerBuff("phoenixsetbonus", new PhoenixSetBonusBuff());
      registerBuff("ninjasmarktrinket", new NinjasMarkBuff());
      registerBuff("leatherglovetrinket", new SimpleTrinketBuff("leatherglovetip", new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.1F)));
      registerBuff("trackerboottrinket", new SimpleTrinketBuff("trackerboottip", new ModifierValue<>(BuffModifiers.SPEED, 0.1F)));
      registerBuff("shinebelttrinket", new ShineBeltBuff());
      registerBuff("vampiresgifttrinket", new SimpleTrinketBuff("vampiresgift", new ModifierValue<>(BuffModifiers.SPEED, 0.15F)));
      registerBuff("zephyrcharmtrinket", new SimpleTrinketBuff("zephyrcharmtip", new ModifierValue<>(BuffModifiers.STAMINA_CAPACITY, 0.5F)));
      registerBuff("demonclawtrinket", new SimpleTrinketBuff("demonclawtip", new ModifierValue<>(BuffModifiers.KNOCKBACK_INCOMING_MOD).max(0.0F)));
      registerBuff("polarclawtrinket", new PolarClawBuff());
      registerBuff("finstrinket", new FinsBuff());
      registerBuff("piratetelescopetrinket", new SimpleTrinketBuff("piratetelescopemap", new ModifierValue<>(BuffModifiers.EXTENDED_MAP_DISCOVER_RANGE, true)));
      registerBuff("regenpendanttrinket", new SimpleTrinketBuff("regenpendant", new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 0.5F)));
      registerBuff("mobilitycloaktrinket", new MobilityCloakBuff());
      registerBuff("travelercloaktrinket", new TravelerCloakBuff());
      registerBuff("explorercloaktrinket", new ExplorerCloakBuff());
      registerBuff("mesmertablettrinket", new SimpleTrinketBuff(new ModifierValue<>(BuffModifiers.MAX_SUMMONS, 1)));
      registerBuff("inducingamulettrinket", new SimpleTrinketBuff(new ModifierValue<>(BuffModifiers.SUMMONS_SPEED, 0.3F)));
      registerBuff("lifelinettrinket", new LifelineBuff());
      registerBuff("magicfocitrinket", new MagicFociBuff());
      registerBuff("rangefocitrinket", new RangeFociBuff());
      registerBuff("meleefocitrinket", new MeleeFociBuff());
      registerBuff("summonfocitrinket", new SummonFociBuff());
      registerBuff("balancedfocitrinket", new BalancedFociBuff());
      registerBuff("ancientfeathertrinket", new SimpleTrinketBuff("ancientfeather", new ModifierValue<>(BuffModifiers.PROJECTILE_VELOCITY, 0.5F)));
      registerBuff("airvesseltrinket", new SimpleTrinketBuff("airvesseltip", new ModifierValue<>(BuffModifiers.DASH_STACKS, 2)));
      registerBuff("miningcharmtrinket", new SimpleTrinketBuff("miningcharm", new ModifierValue<>(BuffModifiers.TOOL_DAMAGE, 0.4F)));
      registerBuff(
         "minersbouquettrinket",
         new SimpleTrinketBuff(
            new String[]{"miningcharm", "minersbouquettip"},
            new ModifierValue<>(BuffModifiers.TOOL_DAMAGE, 0.4F),
            new ModifierValue<>(BuffModifiers.MINING_SPEED, 0.25F)
         )
      );
      registerBuff(
         "calmingminersbouquettrinket",
         new SimpleTrinketBuff(
            new String[]{"miningcharm", "minersbouquettip", "calmingrose"},
            new ModifierValue<>(BuffModifiers.TOOL_DAMAGE, 0.4F),
            new ModifierValue<>(BuffModifiers.MINING_SPEED, 0.25F),
            new ModifierValue<>(BuffModifiers.MOB_SPAWN_RATE).max(0.4F)
         )
      );
      registerBuff("magicalquivertrinket", new SimpleTrinketBuff("magicalquiver", new ModifierValue<>(BuffModifiers.ARROW_USAGE, -0.75F)));
      registerBuff("ammoboxtrinket", new SimpleTrinketBuff("ammobox", new ModifierValue<>(BuffModifiers.BULLET_USAGE, -0.75F)));
      registerBuff("bonehilttrinket", new SimpleTrinketBuff("bonehilt", new ModifierValue<>(BuffModifiers.ARMOR_PEN_FLAT, 20)));
      registerBuff("firestonetrinket", new FireStoneBuff());
      registerBuff("lifependanttrinket", new SimpleTrinketBuff("lifependant", new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 1.0F)));
      registerBuff("constructionhammertrinket", new SimpleTrinketBuff("constructionhammer", new ModifierValue<>(BuffModifiers.BUILDING_SPEED, 0.5F)));
      registerBuff("telescopicladdertrinket", new SimpleTrinketBuff("telescopicladder", new ModifierValue<>(BuffModifiers.BUILD_RANGE, 1.0F)));
      registerBuff("toolextendertrinket", new SimpleTrinketBuff("toolextender", new ModifierValue<>(BuffModifiers.MINING_RANGE, 1.0F)));
      registerBuff("itemattractortrinket", new SimpleTrinketBuff("itemattractortip", new ModifierValue<>(BuffModifiers.ITEM_PICKUP_RANGE, 5.0F)));
      registerBuff("minersprosthetictrinket", new MinersProstheticTrinketBuff());
      registerBuff("leatherdasherstrinket", new LeatherDashersTrinketBuff());
      registerBuff("zephyrbootstrinket", new ZephyrBootsTrinketBuff());
      registerBuff("windbootstrinket", new WindBootsTrinketBuff());
      registerBuff("hoverbootstrinket", new HoverBootsTrinketBuff());
      registerBuff("ghostbootstrinket", new GhostBootsTrinketBuff());
      registerBuff("kineticbootstrinket", new KineticBootsTrinketBuff());
      registerBuff("forceofwindtrinket", new ForceOfWindTrinketBuff());
      registerBuff("shieldtrinket", new ShieldTrinketBuff());
      registerBuff("blinksceptertrinket", new BlinkScepterTrinketBuff());
      registerBuff(
         "spikedbootstrinket",
         new SimpleTrinketBuff("spikedboots", new ModifierValue<>(BuffModifiers.FRICTION, 0.0F).min(1.0F), new ModifierValue<>(BuffModifiers.SPEED, 0.15F))
      );
      registerBuff(
         "spikedbatbootstrinket",
         new SimpleTrinketBuff("spikedbatboots", new ModifierValue<>(BuffModifiers.FRICTION, 0.0F).min(1.0F), new ModifierValue<>(BuffModifiers.SPEED, 0.25F))
      );
      registerBuff("froststonetrinket", new FrostStoneBuff());
      registerBuff("frostflametrinket", new FrostFlameBuff());
      registerBuff("calmingrosetrinket", new SimpleTrinketBuff("calmingrose", new ModifierValue<>(BuffModifiers.MOB_SPAWN_RATE).max(0.4F)));
      registerBuff(
         "hysteriatablettrinket",
         new SimpleTrinketBuff(new ModifierValue<>(BuffModifiers.MAX_SUMMONS, 2), new ModifierValue<>(BuffModifiers.SUMMONS_SPEED, 0.3F))
      );
      registerBuff("frenzyorbtrinket", new FrenzyOrbBuff());
      registerBuff("frozenhearttrinket", new SimpleTrinketBuff("frozenhearttip", new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 50)));
      registerBuff("frozenwavetrinket", new SimpleTrinketBuff("frozenwavetip", new ModifierValue<>(BuffModifiers.CRIT_DAMAGE, 0.25F)));
      registerBuff("fuzzydicetrinket", new SimpleTrinketBuff("fuzzydicetip", new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.05F)));
      registerBuff("noblehorseshoetrinket", new SimpleTrinketBuff("noblehorseshoetip", new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.05F)));
      registerBuff("luckycapetrinket", new SimpleTrinketBuff("luckycapetip", new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.1F)));
      registerBuff("guardianshelltrinket", new GuardianShellTrinketBuff());
      registerBuff("necroticsoulskull", new NecroticSoulSkullTrinketBuff());
      registerBuff("scryingmirrortrinket", new SimpleTrinketBuff(new ModifierValue<>(BuffModifiers.MAX_SUMMONS, 1)));
      registerBuff(
         "diggingclawtrinket",
         new SimpleTrinketBuff(
            new ModifierValue<>(BuffModifiers.TOOL_DAMAGE, 0.5F),
            new ModifierValue<>(BuffModifiers.MINING_SPEED, 0.5F),
            new ModifierValue<>(BuffModifiers.MINING_RANGE, -2.0F)
         )
      );
      registerBuff("spidercharmtrinket", new SpiderCharmBuff());
      registerBuff("templependanttrinket", new SimpleTrinketBuff("templependanttip", new ModifierValue<>(BuffModifiers.DASH_COOLDOWN, -0.25F)));
      registerBuff("gelatincasingstrinket", new SimpleTrinketBuff("gelatincasingstip", new ModifierValue<>(BuffModifiers.PROJECTILE_BOUNCES, 10)));
      registerBuff(
         "ancientrelicstrinket",
         new SimpleTrinketBuff(
            new String[]{"airvesseltip", "templependanttip"},
            new ModifierValue<>(BuffModifiers.DASH_STACKS, 2),
            new ModifierValue<>(BuffModifiers.DASH_COOLDOWN, -0.25F)
         )
      );
      registerBuff("sparegemstonestrinket", new SimpleTrinketBuff("sparegemstonestip", new ModifierValue<>(BuffModifiers.MAX_MANA, 0.2F)));
      registerBuff("spellstonetrinket", new SimpleTrinketBuff("spellstonetip", new ModifierValue<>(BuffModifiers.MAX_MANA, 0.5F)));
      registerBuff("companionlockettrinket", new CompanionLocketBuff());
      registerBuff("willowisplanterntrinket", new WillOWispLanternBuff());
      registerBuff("essenceofperspectivetrinket", new SimpleTrinketBuff("essenceofperspectivetip", new ModifierValue<>(BuffModifiers.LIFE_ESSENCE_GAIN, 0.5F)));
      registerBuff(
         "essenceofprolongingtrinket", new SimpleTrinketBuff("essenceofprolongingtip", new ModifierValue<>(BuffModifiers.LIFE_ESSENCE_DURATION, 1.0F))
      );
      registerBuff(
         "essenceofrebirthtrinket",
         new SimpleTrinketBuff(
            "essenceofrebirthtip", new ModifierValue<>(BuffModifiers.LIFE_ESSENCE_GAIN, 1.0F), new ModifierValue<>(BuffModifiers.LIFE_ESSENCE_DURATION, 1.0F)
         )
      );
      registerBuff("dreamcatchertrinket", new SimpleTrinketBuff("dreamcatchertip", new ModifierValue<>(BuffModifiers.COMBAT_MANA_REGEN, 1.0F)));
      registerBuff("nightmaretalismantrinket", new SimpleTrinketBuff("nightmaretalismantip", new ModifierValue<>(BuffModifiers.COMBAT_MANA_REGEN, 2.5F)));
      registerBuff("prophecyslabtrinket", new SimpleTrinketBuff("prophecyslabtip", new ModifierValue<>(BuffModifiers.COMBAT_MANA_REGEN, 1.0F)));
      registerBuff("magicmanualtrinket", new SimpleTrinketBuff("magicmanualtip", new ModifierValue<>(BuffModifiers.COMBAT_MANA_REGEN, 1.0F)));
      registerBuff("scryingcardstrinket", new SimpleTrinketBuff("scryingcardstip", new ModifierValue<>(BuffModifiers.COMBAT_MANA_REGEN, 2.5F)));
      registerBuff(
         "forbiddenspellbooktrinket",
         new SimpleTrinketBuff(
            "forbiddenspellbooktip", new ModifierValue<>(BuffModifiers.MAGIC_DAMAGE, 0.5F), new ModifierValue<>(BuffModifiers.MANA_USAGE, 1.0F)
         )
      );
      registerBuff("bloodstoneringtrinket", new BloodstoneRingBuff());
      registerBuff("cavelingsfoottrinket", new CavelingsFootBuff());
      registerBuff("cavelingscollectiontrinket", new CavelingsCollectionBuff());
      registerBuff(
         "claygauntlettrinket",
         new SimpleTrinketBuff(
            "claygauntlettip", new ModifierValue<>(BuffModifiers.MAX_RESILIENCE_FLAT, 10), new ModifierValue<>(BuffModifiers.RESILIENCE_DECAY, -0.75F)
         )
      );
      registerBuff("vambracetrinket", new SimpleTrinketBuff("vambracetip", new ModifierValue<>(BuffModifiers.RESILIENCE_GAIN, 0.5F)));
      registerBuff("chainshirttrinket", new SimpleTrinketBuff("chainshirttip", new ModifierValue<>(BuffModifiers.MAX_RESILIENCE_FLAT, 50)));
      registerBuff(
         "manicatrinket",
         new SimpleTrinketBuff(
            "manicatip", new ModifierValue<>(BuffModifiers.MAX_RESILIENCE_FLAT, 50), new ModifierValue<>(BuffModifiers.RESILIENCE_GAIN, 0.5F)
         )
      );
      registerBuff("clockworkhearttrinket", new ClockworkHeartBuff());
      registerBuff("agedchampionscabbardtrinket", new AgedChampionScabbardBuff());
      registerBuff("challengerspauldrontrinket", new ChallengersPauldronBuff());
      registerBuff("foolsgambittrinket", new FoolsGambitTrinketBuff());
      registerBuff("spiritboardtrinket", new SpiritBoardTrinketBuff());
      registerBuff("guardianbracelettrinket", new GuardianBraceletTrinketBuff());
      registerBuff("secondwindcharmtrinket", new SecondWindTrinketBuff());
      registerBuff("summonersbestiarytrinket", new SummonersBestiaryTrinketBuff());
      registerBuff("spiritgreavestrinket", new SpiritGreavesTrinketBuff());
      registerBuff("voodooshieldtrinket", new VoodooShieldTrinketBuff());
      registerBuff("voidphasingstafftrinket", new VoidPhasingStaffTrinketBuff());
      registerBuff("jonasgambittrinket", new JonasGambitTrinketBuff());
      BuffRegistry.Banners.SPEED = registerBuff("bannerofspeed", new BannerOfSpeedBuff());
      BuffRegistry.Banners.DAMAGE = registerBuff("bannerofdamage", new BannerOfDamageBuff());
      BuffRegistry.Banners.SUMMON_SPEED = registerBuff("bannerofsummonspeed", new BannerOfSummonSpeedBuff());
      BuffRegistry.Banners.DEFENSE = registerBuff("bannerofdefense", new BannerOfDefenseBuff());
      BuffRegistry.Banners.PEACE = registerBuff("bannerofpeace", new BannerOfPeaceBuff());
      FRENZY = registerBuff("frenzy", new FrenzyBuff());
      TREMORS_INCOMING = registerBuff("tremorincoming", new TremorsIncomingBuff());
      TREMOR_HAPPENING = registerBuff("tremorhappening", new TremorHappeningBuff());
      ALCHEMICAL_INTERFERENCE = registerBuff("alchemicalinterference", new AlchemicalInterferenceBuff());
      PERK_FLAMELINGS_CAN_DIE = registerBuff("perkflamelingscandie", new PerkFlamelingsCanDieBuff());
      SECOND_LIFE_PERK = registerBuff("secondlifeperk", new SecondLifePerkBuff());
      PIRATE_ESCAPE = registerBuff("pirateescape", new PirateEscapeBuff());
      PIRATE_PASSIVE = registerBuff("piratepassive", new PiratePassiveBuff());
      SUMMONED_PET = registerBuff("summonedpet", new SummonedPetBuff());
      SUMMONED_MOUNT = registerBuff("summonedmount", new SummonedMountBuff());
      SUMMONED_MOB = registerBuff("summonedmob", new SummonedMobBuff());
      HUMAN_ANGRY = registerBuff("humanangry", new HumanAngryBuff());
      MOVE_SPEED_BURST = registerBuff("movespeedburst", new MoveSpeedBurstBuff());
      BOSS_NEARBY = registerBuff("bossnearby", new BossNearbyBuff());
      SETTLEMENT_FLAG = registerBuff("settlementflag", new SettlementFlagBuff());
      CAMPFIRE = registerBuff("campfire", new CampfireBuff());
      SHADOWHOOD_SET = registerBuff("shadowhoodset", new ShadowHoodSetBuff());
      SPIDER_CHARGE = registerBuff("spidercharge", new SimpleModifierBuff(false, true, false, true, new ModifierValue<>(BuffModifiers.SPEED, 0.75F)));
      HARDENED = registerBuff("hardened", new SimpleModifierBuff(false, true, false, true, new ModifierValue<>(BuffModifiers.INCOMING_DAMAGE_MOD, 0.1F)));
      ANCIENT_FOSSIL_ACTIVE = registerBuff(
         "ancientfossilactive", new SimpleModifierBuff(true, true, true, true, new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 1.0F))
      );
      SETTLER_SPRINT = registerBuff("settlersprint", new SettlerSprintBuff());
      SETTLER_JOG = registerBuff("settlerjog", new SettlerJogBuff());
      FROZEN_MOB_IMMUNE = registerBuff("frozenmobimmune", new FrozenMobImmuneBuff());
      FROZEN_MOB = registerBuff("frozenmob", new FrozenMobBuff());
      THAWED_ENEMY = registerBuff("thawedenemy", new SimpleModifierBuff(false, true, false, true, new ModifierValue<>(BuffModifiers.CHASER_RANGE, 3.0F)));
      LEATHER_DASHERS_ACTIVE = registerBuff("leatherdashersactive", new LeatherDashersActiveBuff());
      ZEPHYR_BOOTS_ACTIVE = registerBuff("zephyrbootsactive", new ZephyrBootsActiveBuff());
      WIND_BOOTS_ACTIVE = registerBuff("windbootsactive", new WindBootsActiveBuff());
      HOVER_BOOTS_ACTIVE = registerBuff("hoverbootsactive", new HoverBootsActiveBuff());
      GHOST_BOOTS_ACTIVE = registerBuff("ghostbootsactive", new GhostBootsActiveBuff());
      KINETIC_BOOTS_ACTIVE = registerBuff("kineticbootsactive", new KineticBootsActiveBuff());
      NITRO_ACTIVE = registerBuff("nitroactive", new NitroActiveBuff());
      FOW_ACTIVE = registerBuff("fowactive", new ForceOfWindActiveBuff());
      INVULNERABLE_ACTIVE = registerBuff("ivulnerableactive", new InvulnerableActiveBuff());
      KATANA_DASH_STACKS = registerBuff("katanadashstacks", new KatanaDashStacksBuff());
      GHOST_DASH_ACTIVE = registerBuff("ghostdashactive", new GhostDashActiveBuff());
      RUNIC_EMPOWERMENT = registerBuff("runicempowerment", new RunicEmpowermentActiveBuff());
      GUARDIAN_SHELL_ACTIVE = registerBuff("guardianshellactive", new GuardianShellActiveBuff());
      SLIME_DOME_ACTIVE = registerBuff("slimedomeactive", new SlimeDomeActiveBuff());
      MYCELIUM_HOOD_ACTIVE = registerBuff("myceliumhoodactive", new MyceliumHoodActiveBuff());
      MYCELIUM_SCARF_ACTIVE = registerBuff("myceliumscarfactive", new MyceliumScarfActiveBuff());
      NIGHTSTEEL_ACTIVE = registerBuff("nightsteelactive", new NightsteelActiveBuff());
      SAPPHIRE_SET_ACTIVE = registerBuff("sapphireactive", new SapphireSetActiveBuff());
      SHIELD_ACTIVE = registerBuff("shieldActive", new ShieldActiveBuff());
      PERFECT_BLOCK = registerBuff("perfectblock", new PerfectBlockBuff());
      SLEEPING = registerBuff("sleeping", new SleepingBuff());
      SWAMP_SPORES = registerBuff("swampspores", new SwampSporesBuff());
      EQUIPMENT_BUFF = registerBuff("equipmentbuff", new StatsBuff());
      FOOD_BUFFS = new FoodBuff[10];

      for (int i = 0; i < FOOD_BUFFS.length; i++) {
         FOOD_BUFFS[i] = registerBuff("foodbuff" + i, new FoodBuff(true));
      }

      FOOD_DEBUFF = registerBuff("fooddebuff", new FoodBuff(false));
      HUNGRY_BUFF = registerBuff("hungry", new HungryBuff());
      STARVING_BUFF = registerBuff("starving", new StarvingBuff());
      STAMINA_BUFF = registerBuff("stamina", new StaminaBuff());
      HOLD_ITEM_BUFF = registerBuff("holditem", new HoldItemBuff());
      BLOODPLATE_COWL_ACTIVE = registerBuff("bloodplatecowlactive", new BloodplateCowlActiveBuff());
      BLOOD_CLAW_STACKS_BUFF = registerBuff("bloodclaw", new BloodClawStacksBuff());
      BLOODSTONE_RING_REGEN_ACTIVE_BUFF = registerBuff("bloodstonering", new BloodstoneRingRegenActiveBuff());
      STAR_BARRIER_BUFF = registerBuff("starbarrier", new StarBarrierBuff());
      STABBY_BUSH_FRENZY_BUFF = registerBuff("stabbybushfrenzy", new StabbyBushFrenzyBuff());
      BASHY_BUSH_FRENZY_BUFF = registerBuff("bashybushfrenzy", new BashyBushFrenzyBuff());
      BANNER_OF_WAR_MOB = registerBuff("bannerofwarmob", new BannerOfWarMobBuff());
      ARCANIC_HOOD_ACTIVE = registerBuff("arcanichoodactive", new ArcanicHoodActiveBuff());
      OVERCHARGED_MANA = registerBuff("overchargedmana", new OverchargedManaBuff());
      QUICKSAND_STACKS = registerBuff("quicksandstacks", new QuicksandStacksBuff());
      LIFE_ESSENCE = registerBuff("lifeessence", new LifeEssenceStacksBuff());
      SAPPHIRE_GLYPH = registerBuff("sapphireglyph", new SapphireGlyphBuff());
      AMETHYST_GLYPH = registerBuff("amethystglyph", new AmethystGlyphBuff());
      RUBY_GLYPH = registerBuff("rubyglyph", new RubyGlyphBuff());
      EMERALD_GLYPH = registerBuff("emeraldglyph", new EmeraldGlyphBuff());
      TOPAZ_GLYPH = registerBuff("topazglyph", new TopazGlyphBuff());
      QUADRUPLE_CHASE_RANGE = registerBuff("quadruplechaserange", new QuadrupleChaseRangeBuff());
      SUMMONED_WILL_O_WISP = registerBuff("summonedwillothewisp", new CantCancelSummonedCountBuff());
      BARK_SKIN = registerBuff("barkskin", new BarkskinBuff());
      AGED_CHAMPION_PROWESS = registerBuff("agedchampionprowess", new AgedChampionProwessBuff());
      BULLSEYE = registerBuff("bullseye", new BullseyeBuff());
      SUMMONERS_BESTIARY = registerBuff("summonersbestiary", new SummonersBestiaryBuff());
      GUARDIAN_BRACELET = registerBuff("guardianbracelet", new GuardianBraceletBuff());
      BARKBLADE_ENHANCED = registerBuff("barkbladeenhanced", new BarkBladeEnhancedBuff());
      PARRY_BUCKLER_PARRIED = registerBuff("parrybucklerparried", new ParryBucklerParriedBuff());
      SOULSTORM_SOULS = registerBuff("soulstormsouls", new SoulstormSoulStacksBuff());
      BOUNCE_GLYPH = registerBuff("bounceglyph", new BounceGlyphBuff());
      REVERSE_DAMAGE_GLYPH = registerBuff("reversedamageglyph", new ReverseDamageGlyphBuff());
      DEMONIC_RAGE = registerBuff("demonicrage", new DemonicRageActiveBuff());
      SOULSEED_CONSECUTIVE = registerBuff("soulseedconsecutive", new SoulseedConsecutiveExplosionsBuff());
      PERFECT_STORM = registerBuff("perfectstorm", new PerfectStormBuff());
      SOLDIER_FRENZY = registerBuff("soldierfrenzy", new SoldierFrenzyBuff());
      SHARK_FRENZY = registerBuff("sharkfrenzy", new SharkFrenzyBuff());
      SUMMONED_GHOSTLYBOW = registerBuff("summonedghostlybow", new CantCancelSummonedCountBuff());
      SUMMONED_LOCUST = registerBuff("summonedlocust", new CantCancelSummonedCountBuff());
      PHARAOH_MANA_SPENT = registerBuff("pharaohmanaspent", new PharaohManaSpentBuff());
      CRYO_IMMUNTY = registerBuff("cryoimmunity", new CryoImmunityBuff());
      INCURSION_EMPOWERMENT_CRIT_CHANCE = registerBuff("incursionempowermentcritchance", new IncursionEmpowermentCritChanceBuff());
      INCURSION_EMPOWERMENT_ATTACK_SPEED = registerBuff("incursionempowermentattackspeed", new IncursionEmpowermentAttackSpeedBuff());
      INCURSION_EMPOWERMENT_REGEN = registerBuff("incursionempowermentregen", new IncursionEmpowermentRegenBuff());
      ASCENDED_DASH = registerBuff("ascendeddash", new AscendedDashBuff());
      SPAWN_INVINCIBILITY = registerBuff("spawninvincibility", new SpawnInvincibilityBuff());
      SHARK_TORNADO = registerBuff("sharktornadobuff", new HiddenCooldownBuff());
      QUICK_TINKER = registerBuff("quicktinker", new QuickTinkerStacksBuff());
      MAKESHIFT_ROBOT_OVERCLOCK = registerBuff("makeshiftrobotoverclock", new MakeshiftRobotOverclockBuff());
      RING_OF_ASCENSION_ACTIVE = registerBuff("ringofascensionactive", new RingOfAscensionActiveBuff());
      MOON_SHRINE_BUFF = registerBuff("moonshrinebuff", new MoonShrineBuff());
      REBORN = registerBuff("reborn", new RebornBuff());
      MAGICIAN_INVIS = registerBuff("magicianinvis", new MagicianInvisBuff());
      LOVED = registerBuff("loved", new LovedBuff());
   }

   protected void onRegister(BuffRegistry.BuffRegistryElement<?> object, int id, String stringID, boolean isReplace) {
      if (object.buff instanceof SetBonusBuff) {
         totalSetBonuses++;
      }
   }

   @Override
   protected void onRegistryClose() {
      instance.streamElements().map(e -> e.buff).forEach(Buff::updateLocalDisplayName);

      for (BuffRegistry.BuffRegistryElement<?> element : this.getElements()) {
         element.buff.onBuffRegistryClosed();
      }
   }

   public static <T extends Buff> T registerBuff(String stringID, T buff) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register buffs");
      } else {
         return (T)instance.registerObj(stringID, new BuffRegistry.BuffRegistryElement(buff)).buff;
      }
   }

   public static List<Buff> getBuffs() {
      return instance.streamElements().map(e -> e.buff).collect(Collectors.toList());
   }

   public static Buff getBuff(int id) {
      return instance.getElement(id).buff;
   }

   public static Buff getBuff(String stringID) {
      BuffRegistry.BuffRegistryElement<?> element = instance.getElement(stringID);
      return element == null ? null : element.buff;
   }

   public static int getBuffID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static int getBuffIDRaw(String stringID) throws NoSuchElementException {
      return instance.getElementIDRaw(stringID);
   }

   public static String getBuffStringID(int id) {
      return instance.getElementStringID(id);
   }

   public static int getTotalSetBonuses() {
      return totalSetBonuses;
   }

   public static class ArmorBuffs {
      public static ArmorBuff STEALTH_BASKET;
   }

   public static class Banners {
      public static Buff SPEED;
      public static Buff DAMAGE;
      public static Buff SUMMON_SPEED;
      public static Buff DEFENSE;
      public static Buff PEACE;
   }

   protected static class BuffRegistryElement<T extends Buff> implements IDDataContainer {
      public final T buff;

      public BuffRegistryElement(T buff) {
         this.buff = buff;
      }

      @Override
      public IDData getIDData() {
         return this.buff.idData;
      }
   }

   public static class Debuffs {
      public static Buff HEALTH_POT_FATIGUE;
      public static Buff MANA_POT_FATIGUE;
      public static Buff MANA_EXHAUSTION;
      public static Buff ON_FIRE;
      public static Buff ABLAZE;
      public static Buff POLYMORPH;
      public static Buff BROKEN_ARMOR;
      public static Buff TELEPORT_SICKNESS;
      public static Buff LIFELINE_COOLDOWN;
      public static Buff QUARTZ_SET_COOLDOWN;
      public static Buff GUNSLINGER_SET_COOLDOWN;
      public static Buff VOID_SET_COOLDOWN;
      public static Buff DASH_COOLDOWN;
      public static Buff SPIDER_WEB_SLOW;
      public static Buff WEB_POTION_SLOW;
      public static Buff SPIDER_VENOM;
      public static Buff NECROTIC_POISON;
      public static Buff NECROTIC_SLOW;
      public static Buff IVY_POISON;
      public static Buff WIDOW_POISON;
      public static Buff CHILLED;
      public static Buff FREEZING;
      public static Buff FROSTBURN;
      public static Buff FROSTSLOW;
      public static Buff HAUNTED;
      public static Buff EMERALD_POISON;
      public static Buff ASHEN_BURIAL;
      public static Buff RUNIC_HAT_COOLDOWN;
      public static Buff GUARDIAN_SHELL_COOLDOWN;
      public static Buff ANCESTOR_SET_COOLDOWN;
      public static Buff ANCIENT_FOSSIL_SET_COOLDOWN;
      public static Buff SLIME_SET_COOLDOWN;
      public static Buff NECROTIC_SOUL_SKULL_COOLDOWN;
      public static Buff NIGHTSTEEL_SET_COOLDOWN;
      public static Buff SPIDERITE_SET_COOLDOWN;
      public static Buff SAPPHIRE_SET_COOLDOWN;
      public static Buff SPIDER_CHARM_POISON;
      public static Buff SLIME_POISON;
      public static Buff SAND_KNIFE_WOUND_BUFF;
      public static Buff CRYSTALLIZE_BUFF;
      public static Buff BLOOD_GRIMOIRE_MARKED_DEBUFF;
      public static Buff SLIME_GREATBOW_SLOW_TARGET_DEBUFF;
      public static Buff BLOOD_GRIMOIRE_COOLDOWN;
      public static Buff THE_CRIMSON_SKY_COOLDOWN;
      public static Buff RAVENBEAK_SPEAR_COOLDOWN;
      public static Buff CRUSHING_DARKNESS;
      public static Buff SNOW_COVERED_DEBUFF;
      public static Buff SNOW_COVERED_SLOW_DEBUFF;
      public static Buff FLOUR_COVERED_SLOW_DEBUFF;
      public static Buff NECROBOW_ROOT_DEBUFF;
      public static Buff KATANA_DASH_COOLDOWN;
      public static Buff RUNEBOUND_SCEPTER_COOLDOWN;
      public static Buff CAPTORS_SHORTBOW_COOLDOWN;
      public static Buff NETTED_DEBUFF;
      public static Buff DRYAD_HAUNTED;
      public static Buff DRYAD_POSSESSED;
      public static Buff SPIRIT_HAUNTED;
      public static Buff SPIRIT_POSSESSED;
      public static Buff STUNNED_DAMAGE_TAKEN_INCREASED;
      public static Buff SPIRIT_CORRUPTED;
      public static Buff DEMONIC_SET_COOLDOWN;
      public static Buff SOULSEED_COOLDOWN;
      public static Buff THIEF_THROW_COOLDOWN;
      public static Buff THIEF_STEAL_COOLDOWN;
      public static Buff PERFECT_STORM_ABILITY_COOLDOWN;
      public static Buff PERFECT_STORM_ONHIT_COOLDOWN;
      public static Buff CRYOWITCH_COOLDOWN;
      public static Buff CRYOWITCH_COOLDOWN_ONHIT;
      public static Buff ASCENDED_DARKNESS;
      public static Buff PYROMANCY_ONFIRE;
      public static Buff HYDRA_DEAFENED;
      public static Buff PHOENIX_COOLDOWN;
      public static Buff GENERIC_ONFIRE;
      public static Buff GENERIC_ICE_SLOW;
      public static Buff GENERIC_POISON;
      public static Buff GENERIC_DAMAGE_DEBUFF;
      public static Buff GENERIC_WEAKNESS;
      public static Buff GENERIC_ATTACKSPEED_SLOW;
      public static Buff POLYMORPH_CHICKEN;
      public static Buff POLYMORPH_FROG;
      public static Buff POLYMORPH_SQUIRREL;
      public static Buff POLYMORPH_DUCK;
      public static Buff POLYMORPH_RABBIT;
      public static Buff POLYMORPH_TURTLE;
      public static Buff POLYMORPH_ROOSTER;
      public static Buff SECOND_BOSS_WATCHING;
      public static Buff BLEED;
      public static Buff SHARKARMOR_BLEED;
      public static Buff SHARK_BLEED;
   }

   public static class Potions {
      public static Buff STINKFLASK;
   }

   public static class SetBonuses {
      public static SetBonusBuff LEATHER;
      public static SetBonusBuff CLOTH_ROBE;
      public static SetBonusBuff COPPER;
      public static SetBonusBuff IRON;
      public static SetBonusBuff GOLD_CROWN;
      public static SetBonusBuff GOLD;
      public static SetBonusBuff SPIDER;
      public static SetBonusBuff FROST_HELMET;
      public static SetBonusBuff FROST_HOOD;
      public static SetBonusBuff FROST_HAT;
      public static SetBonusBuff DEMONIC;
      public static SetBonusBuff VOIDHAT;
      public static SetBonusBuff VOIDMASK;
      public static SetBonusBuff NINJA;
      public static SetBonusBuff WITCH_ROBES;
      public static SetBonusBuff CRYOWITCH;
      public static SetBonusBuff RUNIC;
      public static SetBonusBuff RUNICHAT;
      public static SetBonusBuff IVYHELMET;
      public static SetBonusBuff IVYHOOD;
      public static SetBonusBuff IVYHAT;
      public static SetBonusBuff IVYCIRCLET;
      public static SetBonusBuff QUARTZHELMET;
      public static SetBonusBuff QUARTZCROWN;
      public static SetBonusBuff GUNSLINGER;
      public static SetBonusBuff TUNGSTEN;
      public static SetBonusBuff SHADOWHOOD;
      public static SetBonusBuff SHADOWHAT;
      public static SetBonusBuff GLACIALHELMET;
      public static SetBonusBuff GLACIALCIRCLET;
      public static SetBonusBuff DRYAD_HELMET;
      public static SetBonusBuff DRYAD_SCARF;
      public static SetBonusBuff DRYAD_HAT;
      public static SetBonusBuff DRYAD_CROWN;
      public static SetBonusBuff MYCELIUMHOOD;
      public static SetBonusBuff MYCELIUMSCARF;
      public static SetBonusBuff AGED_CHAMPION;
      public static SetBonusBuff WIDOW;
      public static SetBonusBuff PHOENIX;
      public static SetBonusBuff ANCESTOR;
      public static SetBonusBuff ANCIENT_FOSSIL_HELMET;
      public static SetBonusBuff ANCIENT_FOSSIL_MASK;
      public static SetBonusBuff SHARPSHOOTER;
      public static SetBonusBuff SLIMEHELMET;
      public static SetBonusBuff SLIMEHAT;
      public static SetBonusBuff NIGHTSTEEL;
      public static SetBonusBuff NIGHTSTEEL_HELMET;
      public static SetBonusBuff NIGHTSTEEL_VEIL;
      public static SetBonusBuff NIGHTSTEEL_CIRCLET;
      public static SetBonusBuff BLOODPLATE_COWL;
      public static SetBonusBuff SPIDERITE;
      public static SetBonusBuff BATTLE_CHEF;
      public static SetBonusBuff SPIDERITE_HELMET;
      public static SetBonusBuff SPIDERITE_HAT;
      public static SetBonusBuff SPIDERITE_CROWN;
      public static SetBonusBuff RAVENLORDS_HEADDRESS;
      public static SetBonusBuff DAWN;
      public static SetBonusBuff DUSK;
      public static SetBonusBuff AMETHYST;
      public static SetBonusBuff SAPPHIRE;
      public static SetBonusBuff EMERALD;
      public static SetBonusBuff RUBY;
      public static SetBonusBuff MAKESHIFT_MELEE;
      public static SetBonusBuff MAKESHIFT_RANGED;
      public static SetBonusBuff MAKESHIFT_MAGIC;
      public static SetBonusBuff MAKESHIFT_SUMMON;
      public static SetBonusBuff ARCANIC_HOOD;
      public static SetBonusBuff ARACHNID;
      public static SetBonusBuff GHOSTLY_ARCHER;
      public static SetBonusBuff SOULSEED;
      public static SetBonusBuff SOLDIERHELMET;
      public static SetBonusBuff SOLDIERCAP;
      public static SetBonusBuff THIEF;
      public static SetBonusBuff PHARAOH;
      public static SetBonusBuff SUPPORTER;
      public static SetBonusBuff SHARK_ARMOR;
   }
}
