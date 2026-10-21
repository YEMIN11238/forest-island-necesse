package necesse.engine.registries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import necesse.engine.GameLoadingScreen;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.sound.GameMusic;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.LevelIdentifier;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.friendly.critters.flyingbugs.ButterflyMob;
import necesse.entity.mobs.friendly.critters.flyingbugs.FireflyMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.FishingPoleHolding;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.ItemHolding;
import necesse.inventory.item.SwingSpriteAttackItem;
import necesse.inventory.item.WorkSpriteAttackItem;
import necesse.inventory.item.armorItem.agedChampion.AgedChampionChestplateArmorItem;
import necesse.inventory.item.armorItem.agedChampion.AgedChampionGreavesArmorItem;
import necesse.inventory.item.armorItem.agedChampion.AgedChampionHelmetArmorItem;
import necesse.inventory.item.armorItem.ancestor.AncestorsBootsArmorItem;
import necesse.inventory.item.armorItem.ancestor.AncestorsHatArmorItem;
import necesse.inventory.item.armorItem.ancestor.AncestorsRobeArmorItem;
import necesse.inventory.item.armorItem.ancientFossil.AncientFossilBootsArmorItem;
import necesse.inventory.item.armorItem.ancientFossil.AncientFossilChestplateArmorItem;
import necesse.inventory.item.armorItem.ancientFossil.AncientFossilHelmetArmorItem;
import necesse.inventory.item.armorItem.ancientFossil.AncientFossilMaskArmorItem;
import necesse.inventory.item.armorItem.arachnid.ArachnidChestplateArmorItem;
import necesse.inventory.item.armorItem.arachnid.ArachnidHelmetArmorItem;
import necesse.inventory.item.armorItem.arachnid.ArachnidLegsArmorItem;
import necesse.inventory.item.armorItem.arcanic.ArcanicBootsArmorItem;
import necesse.inventory.item.armorItem.arcanic.ArcanicChestplateArmorItem;
import necesse.inventory.item.armorItem.arcanic.ArcanicHelmetArmorItem;
import necesse.inventory.item.armorItem.battlechef.BattleChefBootsArmorItem;
import necesse.inventory.item.armorItem.battlechef.BattleChefChestplateArmorItem;
import necesse.inventory.item.armorItem.battlechef.BattleChefHatArmorItem;
import necesse.inventory.item.armorItem.bloodplate.BloodplateBootsArmorItem;
import necesse.inventory.item.armorItem.bloodplate.BloodplateChestplateArmorItem;
import necesse.inventory.item.armorItem.bloodplate.BloodplateCowlArmorItem;
import necesse.inventory.item.armorItem.cloth.ClothBootsArmorItem;
import necesse.inventory.item.armorItem.cloth.ClothHatArmorItem;
import necesse.inventory.item.armorItem.cloth.ClothRobeArmorItem;
import necesse.inventory.item.armorItem.copper.CopperBootsArmorItem;
import necesse.inventory.item.armorItem.copper.CopperChestplateArmorItem;
import necesse.inventory.item.armorItem.copper.CopperHelmetArmorItem;
import necesse.inventory.item.armorItem.cosmetics.alien.AlienCostumeBootsArmorItem;
import necesse.inventory.item.armorItem.cosmetics.alien.AlienCostumeShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.alien.AlienMaskArmorItem;
import necesse.inventory.item.armorItem.cosmetics.animalKeeper.AnimalKeeperHatArmorItem;
import necesse.inventory.item.armorItem.cosmetics.animalKeeper.AnimalKeeperShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.animalKeeper.AnimalKeeperShoesArmorItem;
import necesse.inventory.item.armorItem.cosmetics.chicken.ChickenCostumeBootsArmorItem;
import necesse.inventory.item.armorItem.cosmetics.chicken.ChickenCostumeShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.chicken.ChickenMaskArmorItem;
import necesse.inventory.item.armorItem.cosmetics.crimson.CrimsonHatArmorItem;
import necesse.inventory.item.armorItem.cosmetics.crimson.CrimsonRobeArmorItem;
import necesse.inventory.item.armorItem.cosmetics.crimson.CrimsonShoesArmorItem;
import necesse.inventory.item.armorItem.cosmetics.elder.ElderHatArmorItem;
import necesse.inventory.item.armorItem.cosmetics.elder.ElderShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.elder.ElderShoesArmorItem;
import necesse.inventory.item.armorItem.cosmetics.exotic.ExoticShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.exotic.ExoticShoesArmorItem;
import necesse.inventory.item.armorItem.cosmetics.exotic.TurbanArmorItem;
import necesse.inventory.item.armorItem.cosmetics.exotic.WalkingStickHoldItem;
import necesse.inventory.item.armorItem.cosmetics.farmer.FarmerHatArmorItem;
import necesse.inventory.item.armorItem.cosmetics.farmer.FarmerPitchForkHoldItem;
import necesse.inventory.item.armorItem.cosmetics.farmer.FarmerShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.farmer.FarmerShoesArmorItem;
import necesse.inventory.item.armorItem.cosmetics.frog.FrogCostumeBootsArmorItem;
import necesse.inventory.item.armorItem.cosmetics.frog.FrogCostumeShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.frog.FrogMaskArmorItem;
import necesse.inventory.item.armorItem.cosmetics.horse.HorseCostumeBootsArmorItem;
import necesse.inventory.item.armorItem.cosmetics.horse.HorseCostumeShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.horse.HorseMaskArmorItem;
import necesse.inventory.item.armorItem.cosmetics.hula.HulaHatArmorItem;
import necesse.inventory.item.armorItem.cosmetics.hula.HulaShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.hula.HulaShirtWithTopArmorItem;
import necesse.inventory.item.armorItem.cosmetics.hunter.HunterBootsArmorItem;
import necesse.inventory.item.armorItem.cosmetics.hunter.HunterHoodArmorItem;
import necesse.inventory.item.armorItem.cosmetics.hunter.HunterHoodMaskArmorItem;
import necesse.inventory.item.armorItem.cosmetics.hunter.HunterShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.jester.JesterBootsArmorItem;
import necesse.inventory.item.armorItem.cosmetics.jester.JesterHatArmorItem;
import necesse.inventory.item.armorItem.cosmetics.jester.JesterShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.lab.AlchemistGlassesArmorItem;
import necesse.inventory.item.armorItem.cosmetics.lab.EngineerGogglesArmorItem;
import necesse.inventory.item.armorItem.cosmetics.lab.JonasGlassesArmorItem;
import necesse.inventory.item.armorItem.cosmetics.lab.LabApronArmorItem;
import necesse.inventory.item.armorItem.cosmetics.lab.LabBootsArmorItem;
import necesse.inventory.item.armorItem.cosmetics.lab.LabCoatArmorItem;
import necesse.inventory.item.armorItem.cosmetics.mage.MageHatArmorItem;
import necesse.inventory.item.armorItem.cosmetics.mage.MageRobeArmorItem;
import necesse.inventory.item.armorItem.cosmetics.mage.MageShoesArmorItem;
import necesse.inventory.item.armorItem.cosmetics.merchant.MerchantBootsArmorItem;
import necesse.inventory.item.armorItem.cosmetics.merchant.MerchantShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.mime.MimeBeretArmorItem;
import necesse.inventory.item.armorItem.cosmetics.mime.MimeBootsArmorItem;
import necesse.inventory.item.armorItem.cosmetics.mime.MimeShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.miner.MinerBootsArmorItem;
import necesse.inventory.item.armorItem.cosmetics.miner.MinerHatArmorItem;
import necesse.inventory.item.armorItem.cosmetics.miner.MinerShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.misc.CheatShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.misc.CheatShoesArmorItem;
import necesse.inventory.item.armorItem.cosmetics.misc.CheatWigArmorItem;
import necesse.inventory.item.armorItem.cosmetics.misc.ChristmasHatArmorItem;
import necesse.inventory.item.armorItem.cosmetics.misc.EmpressMaskArmorItem;
import necesse.inventory.item.armorItem.cosmetics.misc.HardHatArmorItem;
import necesse.inventory.item.armorItem.cosmetics.misc.PartyHatArmorItem;
import necesse.inventory.item.armorItem.cosmetics.misc.PumpkinMaskArmorItem;
import necesse.inventory.item.armorItem.cosmetics.misc.ShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.misc.ShoesArmorItem;
import necesse.inventory.item.armorItem.cosmetics.misc.StealthBasketArmorItem;
import necesse.inventory.item.armorItem.cosmetics.misc.SunglassesArmorItem;
import necesse.inventory.item.armorItem.cosmetics.misc.SurgicalMaskArmorItem;
import necesse.inventory.item.armorItem.cosmetics.misc.TrapperHatArmorItem;
import necesse.inventory.item.armorItem.cosmetics.misc.VultureMaskArmorItem;
import necesse.inventory.item.armorItem.cosmetics.misc.WigArmorItem;
import necesse.inventory.item.armorItem.cosmetics.pawnBroker.BlazerArmorItem;
import necesse.inventory.item.armorItem.cosmetics.pawnBroker.DressShoesArmorItem;
import necesse.inventory.item.armorItem.cosmetics.pawnBroker.TopHatArmorItem;
import necesse.inventory.item.armorItem.cosmetics.pirate.CaptainsBootsArmorItem;
import necesse.inventory.item.armorItem.cosmetics.pirate.CaptainsHatArmorItem;
import necesse.inventory.item.armorItem.cosmetics.pirate.CaptainsShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.pirate.PirateBandanaArmorItem;
import necesse.inventory.item.armorItem.cosmetics.pirate.PirateBandanaWithEyePatchArmorItem;
import necesse.inventory.item.armorItem.cosmetics.pirate.PirateBootsArmorItem;
import necesse.inventory.item.armorItem.cosmetics.pirate.PirateEyePatchArmorItem;
import necesse.inventory.item.armorItem.cosmetics.pirate.PirateShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.plague.PlagueBootsArmorItem;
import necesse.inventory.item.armorItem.cosmetics.plague.PlagueMaskArmorItem;
import necesse.inventory.item.armorItem.cosmetics.plague.PlagueRobeArmorItem;
import necesse.inventory.item.armorItem.cosmetics.rain.RainBootsArmorItem;
import necesse.inventory.item.armorItem.cosmetics.rain.RainCoatArmorItem;
import necesse.inventory.item.armorItem.cosmetics.rain.RainHatArmorItem;
import necesse.inventory.item.armorItem.cosmetics.safari.SafariHatArmorItem;
import necesse.inventory.item.armorItem.cosmetics.safari.SafariShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.safari.SafariShoesArmorItem;
import necesse.inventory.item.armorItem.cosmetics.sailor.SailorHatArmorItem;
import necesse.inventory.item.armorItem.cosmetics.sailor.SailorShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.sailor.SailorShoesArmorItem;
import necesse.inventory.item.armorItem.cosmetics.shark.SharkCostumeBootsArmorItem;
import necesse.inventory.item.armorItem.cosmetics.shark.SharkCostumeShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.shark.SharkMaskArmorItem;
import necesse.inventory.item.armorItem.cosmetics.smithing.SmithingApronArmorItem;
import necesse.inventory.item.armorItem.cosmetics.smithing.SmithingShoesArmorItem;
import necesse.inventory.item.armorItem.cosmetics.snow.SnowBootsArmorItem;
import necesse.inventory.item.armorItem.cosmetics.snow.SnowCloakArmorItem;
import necesse.inventory.item.armorItem.cosmetics.snow.SnowHoodArmorItem;
import necesse.inventory.item.armorItem.cosmetics.space.SpaceBootsArmorItem;
import necesse.inventory.item.armorItem.cosmetics.space.SpaceHelmetArmorItem;
import necesse.inventory.item.armorItem.cosmetics.space.SpaceSuitArmorItem;
import necesse.inventory.item.armorItem.cosmetics.stylist.StylishFlowerArmorItem;
import necesse.inventory.item.armorItem.cosmetics.stylist.StylistShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.stylist.StylistShoesArmorItem;
import necesse.inventory.item.armorItem.cosmetics.swim.SwimSuitArmorItem;
import necesse.inventory.item.armorItem.cosmetics.swim.SwimTrunksArmorItem;
import necesse.inventory.item.armorItem.cosmetics.syndicatethug.SyndicateThugBootsArmorItem;
import necesse.inventory.item.armorItem.cosmetics.syndicatethug.SyndicateThugHoodArmorItem;
import necesse.inventory.item.armorItem.cosmetics.syndicatethug.SyndicateThugShirtArmorItem;
import necesse.inventory.item.armorItem.cryowitch.CryoWitchHatArmorItem;
import necesse.inventory.item.armorItem.cryowitch.CryoWitchRobeArmorItem;
import necesse.inventory.item.armorItem.cryowitch.CryoWitchShoesArmorItem;
import necesse.inventory.item.armorItem.crystal.AmethystHelmet;
import necesse.inventory.item.armorItem.crystal.CrystalBoots;
import necesse.inventory.item.armorItem.crystal.CrystalChestplate;
import necesse.inventory.item.armorItem.crystal.EmeraldMask;
import necesse.inventory.item.armorItem.crystal.RubyCrown;
import necesse.inventory.item.armorItem.crystal.SapphireEyepatch;
import necesse.inventory.item.armorItem.dawn.DawnBootsArmorItem;
import necesse.inventory.item.armorItem.dawn.DawnChestplateArmorItem;
import necesse.inventory.item.armorItem.dawn.DawnHelmetArmorItem;
import necesse.inventory.item.armorItem.deepfrost.DeepfrostBootsArmorItem;
import necesse.inventory.item.armorItem.deepfrost.DeepfrostChestplateArmorItem;
import necesse.inventory.item.armorItem.deepfrost.DeepfrostHelmetArmorItem;
import necesse.inventory.item.armorItem.demonic.DemonicBootsArmorItem;
import necesse.inventory.item.armorItem.demonic.DemonicChestplateArmorItem;
import necesse.inventory.item.armorItem.demonic.DemonicHelmetArmorItem;
import necesse.inventory.item.armorItem.dryad.DryadBootsArmorItem;
import necesse.inventory.item.armorItem.dryad.DryadChestplateArmorItem;
import necesse.inventory.item.armorItem.dryad.DryadCrownArmorItem;
import necesse.inventory.item.armorItem.dryad.DryadHatArmorItem;
import necesse.inventory.item.armorItem.dryad.DryadHelmetArmorItem;
import necesse.inventory.item.armorItem.dryad.DryadScarfArmorItem;
import necesse.inventory.item.armorItem.dusk.DuskBootsArmorItem;
import necesse.inventory.item.armorItem.dusk.DuskChestplateArmorItem;
import necesse.inventory.item.armorItem.dusk.DuskHelmetArmorItem;
import necesse.inventory.item.armorItem.frost.FrostBootsArmorItem;
import necesse.inventory.item.armorItem.frost.FrostChestplateArmorItem;
import necesse.inventory.item.armorItem.frost.FrostHatArmorItem;
import necesse.inventory.item.armorItem.frost.FrostHelmetArmorItem;
import necesse.inventory.item.armorItem.frost.FrostHoodArmorItem;
import necesse.inventory.item.armorItem.glacial.GlacialBootsArmorItem;
import necesse.inventory.item.armorItem.glacial.GlacialChestplateArmorItem;
import necesse.inventory.item.armorItem.glacial.GlacialCircletArmorItem;
import necesse.inventory.item.armorItem.glacial.GlacialHelmetArmorItem;
import necesse.inventory.item.armorItem.gold.GoldBootsArmorItem;
import necesse.inventory.item.armorItem.gold.GoldChestplateArmorItem;
import necesse.inventory.item.armorItem.gold.GoldCrownArmorItem;
import necesse.inventory.item.armorItem.gold.GoldHelmetArmorItem;
import necesse.inventory.item.armorItem.gunslinger.GunslingerBootsArmorItem;
import necesse.inventory.item.armorItem.gunslinger.GunslingerHatArmorItem;
import necesse.inventory.item.armorItem.gunslinger.GunslingerVestArmorItem;
import necesse.inventory.item.armorItem.iron.IronBootsArmorItem;
import necesse.inventory.item.armorItem.iron.IronChestplateArmorItem;
import necesse.inventory.item.armorItem.iron.IronHelmetArmorItem;
import necesse.inventory.item.armorItem.ivy.IvyBootsArmorItem;
import necesse.inventory.item.armorItem.ivy.IvyChestplateArmorItem;
import necesse.inventory.item.armorItem.ivy.IvyCircletArmorItem;
import necesse.inventory.item.armorItem.ivy.IvyHatArmorItem;
import necesse.inventory.item.armorItem.ivy.IvyHelmetArmorItem;
import necesse.inventory.item.armorItem.ivy.IvyHoodArmorItem;
import necesse.inventory.item.armorItem.leather.LeatherBootsArmorItem;
import necesse.inventory.item.armorItem.leather.LeatherHoodArmorItem;
import necesse.inventory.item.armorItem.leather.LeatherShirtArmorItem;
import necesse.inventory.item.armorItem.makeshift.MakeshiftBoots;
import necesse.inventory.item.armorItem.makeshift.MakeshiftChestplate;
import necesse.inventory.item.armorItem.makeshift.MakeshiftCrown;
import necesse.inventory.item.armorItem.makeshift.MakeshiftEyepiece;
import necesse.inventory.item.armorItem.makeshift.MakeshiftFacePlating;
import necesse.inventory.item.armorItem.makeshift.MakeshiftHat;
import necesse.inventory.item.armorItem.mycelium.MyceliumBootsArmorItem;
import necesse.inventory.item.armorItem.mycelium.MyceliumChestplateArmorItem;
import necesse.inventory.item.armorItem.mycelium.MyceliumHoodArmorItem;
import necesse.inventory.item.armorItem.mycelium.MyceliumScarfArmorItem;
import necesse.inventory.item.armorItem.nightsteel.NightsteelBootsArmorItem;
import necesse.inventory.item.armorItem.nightsteel.NightsteelChestplateArmorItem;
import necesse.inventory.item.armorItem.nightsteel.NightsteelCircletArmorItem;
import necesse.inventory.item.armorItem.nightsteel.NightsteelHelmetArmorItem;
import necesse.inventory.item.armorItem.nightsteel.NightsteelMaskArmorItem;
import necesse.inventory.item.armorItem.nightsteel.NightsteelVeilArmorItem;
import necesse.inventory.item.armorItem.ninja.NinjaHoodArmorItem;
import necesse.inventory.item.armorItem.ninja.NinjaRobeArmorItem;
import necesse.inventory.item.armorItem.ninja.NinjaShoesArmorItem;
import necesse.inventory.item.armorItem.pharaoh.PharaohsHeaddress;
import necesse.inventory.item.armorItem.pharaoh.PharaohsRobeArmorItem;
import necesse.inventory.item.armorItem.pharaoh.PharaohsSandalsArmorItem;
import necesse.inventory.item.armorItem.phoenix.PhoenixBootsArmorItem;
import necesse.inventory.item.armorItem.phoenix.PhoenixCloakArmorItem;
import necesse.inventory.item.armorItem.phoenix.PhoenixMaskArmorItem;
import necesse.inventory.item.armorItem.quartz.QuartzBootsArmorItem;
import necesse.inventory.item.armorItem.quartz.QuartzChestplateArmorItem;
import necesse.inventory.item.armorItem.quartz.QuartzCrownArmorItem;
import necesse.inventory.item.armorItem.quartz.QuartzHelmetArmorItem;
import necesse.inventory.item.armorItem.ravenlords.RavenlordsBootsArmorItem;
import necesse.inventory.item.armorItem.ravenlords.RavenlordsChestplateArmorItem;
import necesse.inventory.item.armorItem.ravenlords.RavenlordsHeaddressArmorItem;
import necesse.inventory.item.armorItem.runebound.RuneboundBackBonesArmorItem;
import necesse.inventory.item.armorItem.runebound.RuneboundBonesRobeChestArmorItem;
import necesse.inventory.item.armorItem.runebound.RuneboundBootsArmorItem;
import necesse.inventory.item.armorItem.runebound.RuneboundCrownArmorItem;
import necesse.inventory.item.armorItem.runebound.RuneboundCrownMaskArmorItem;
import necesse.inventory.item.armorItem.runebound.RuneboundHelmetArmorItem;
import necesse.inventory.item.armorItem.runebound.RuneboundHoodArmorItem;
import necesse.inventory.item.armorItem.runebound.RuneboundHornHelmetArmorItem;
import necesse.inventory.item.armorItem.runebound.RuneboundLeatherChestArmorItem;
import necesse.inventory.item.armorItem.runebound.RuneboundRobeArmorItem;
import necesse.inventory.item.armorItem.runebound.RuneboundSkullHelmetArmorItem;
import necesse.inventory.item.armorItem.runestone.RunicBootsArmorItem;
import necesse.inventory.item.armorItem.runestone.RunicChestplateArmorItem;
import necesse.inventory.item.armorItem.runestone.RunicCrownArmorItem;
import necesse.inventory.item.armorItem.runestone.RunicHatArmorItem;
import necesse.inventory.item.armorItem.runestone.RunicHelmetArmorItem;
import necesse.inventory.item.armorItem.runestone.RunicHoodArmorItem;
import necesse.inventory.item.armorItem.shadow.ShadowBootsArmorItem;
import necesse.inventory.item.armorItem.shadow.ShadowHatArmorItem;
import necesse.inventory.item.armorItem.shadow.ShadowHoodArmorItem;
import necesse.inventory.item.armorItem.shadow.ShadowMantleArmorItem;
import necesse.inventory.item.armorItem.sharkarmor.SharkBootsArmorItem;
import necesse.inventory.item.armorItem.sharkarmor.SharkChestplateArmorItem;
import necesse.inventory.item.armorItem.sharkarmor.SharkHelmetArmorItem;
import necesse.inventory.item.armorItem.sharpshooter.SharpshooterBootsArmorItem;
import necesse.inventory.item.armorItem.sharpshooter.SharpshooterCoatArmorItem;
import necesse.inventory.item.armorItem.sharpshooter.SharpshooterHatArmorItem;
import necesse.inventory.item.armorItem.slime.SlimeBootsArmorItem;
import necesse.inventory.item.armorItem.slime.SlimeChestplateArmorItem;
import necesse.inventory.item.armorItem.slime.SlimeHatArmorItem;
import necesse.inventory.item.armorItem.slime.SlimeHelmetArmorItem;
import necesse.inventory.item.armorItem.soldier.SoldierBootsArmorItem;
import necesse.inventory.item.armorItem.soldier.SoldierCapArmorItem;
import necesse.inventory.item.armorItem.soldier.SoldierChestplateArmorItem;
import necesse.inventory.item.armorItem.soldier.SoldierHelmetArmorItem;
import necesse.inventory.item.armorItem.soulseed.SoulseedBootsArmorItem;
import necesse.inventory.item.armorItem.soulseed.SoulseedChestplateArmorItem;
import necesse.inventory.item.armorItem.soulseed.SoulseedCrownArmorItem;
import necesse.inventory.item.armorItem.spider.SpiderBootsArmorItem;
import necesse.inventory.item.armorItem.spider.SpiderChestplateArmorItem;
import necesse.inventory.item.armorItem.spider.SpiderHelmetArmorItem;
import necesse.inventory.item.armorItem.spiderite.SpideriteChestplateArmorItem;
import necesse.inventory.item.armorItem.spiderite.SpideriteCrownArmorItem;
import necesse.inventory.item.armorItem.spiderite.SpideriteGreavesArmorItem;
import necesse.inventory.item.armorItem.spiderite.SpideriteHatArmorItem;
import necesse.inventory.item.armorItem.spiderite.SpideriteHelmetArmorItem;
import necesse.inventory.item.armorItem.spiderite.SpideriteHoodArmorItem;
import necesse.inventory.item.armorItem.supporter.SupporterBootsArmorItem;
import necesse.inventory.item.armorItem.supporter.SupporterChestplateArmorItem;
import necesse.inventory.item.armorItem.supporter.SupporterHelmetArmorItem;
import necesse.inventory.item.armorItem.thief.ThiefsBootsArmorItem;
import necesse.inventory.item.armorItem.thief.ThiefsCloakArmorItem;
import necesse.inventory.item.armorItem.thief.ThiefsCowlArmorItem;
import necesse.inventory.item.armorItem.tungsten.TungstenBootsArmorItem;
import necesse.inventory.item.armorItem.tungsten.TungstenChestplateArmorItem;
import necesse.inventory.item.armorItem.tungsten.TungstenHelmetArmorItem;
import necesse.inventory.item.armorItem.voixd.VoidBootsArmorItem;
import necesse.inventory.item.armorItem.voixd.VoidHatArmorItem;
import necesse.inventory.item.armorItem.voixd.VoidMaskArmorItem;
import necesse.inventory.item.armorItem.voixd.VoidRobeArmorItem;
import necesse.inventory.item.armorItem.widow.WidowBootsArmorItem;
import necesse.inventory.item.armorItem.widow.WidowChestplateArmorItem;
import necesse.inventory.item.armorItem.widow.WidowHelmetArmorItem;
import necesse.inventory.item.armorItem.witch.WitchHatArmorItem;
import necesse.inventory.item.armorItem.witch.WitchRobeArmorItem;
import necesse.inventory.item.armorItem.witch.WitchShoesArmorItem;
import necesse.inventory.item.arrowItem.BoneArrowItem;
import necesse.inventory.item.arrowItem.BouncingArrowItem;
import necesse.inventory.item.arrowItem.FireArrowItem;
import necesse.inventory.item.arrowItem.FrostArrowItem;
import necesse.inventory.item.arrowItem.IronArrowItem;
import necesse.inventory.item.arrowItem.PoisonArrowItem;
import necesse.inventory.item.arrowItem.SpideriteArrowItem;
import necesse.inventory.item.arrowItem.StoneArrowItem;
import necesse.inventory.item.baitItem.BaitItem;
import necesse.inventory.item.bulletItem.BouncingBulletItem;
import necesse.inventory.item.bulletItem.CannonballAmmoItem;
import necesse.inventory.item.bulletItem.CrystalBulletItem;
import necesse.inventory.item.bulletItem.FrostBulletItem;
import necesse.inventory.item.bulletItem.SeedBulletItem;
import necesse.inventory.item.bulletItem.SharkToothBulletItem;
import necesse.inventory.item.bulletItem.SimpleBulletItem;
import necesse.inventory.item.bulletItem.VoidBulletItem;
import necesse.inventory.item.matItem.BookMatItem;
import necesse.inventory.item.matItem.EssenceMatItem;
import necesse.inventory.item.matItem.FishItem;
import necesse.inventory.item.matItem.MatItem;
import necesse.inventory.item.matItem.MultiTextureMatItem;
import necesse.inventory.item.matItem.RevivalPotion;
import necesse.inventory.item.miscItem.AmmoBag;
import necesse.inventory.item.miscItem.AmmoPouch;
import necesse.inventory.item.miscItem.AscendedShardItem;
import necesse.inventory.item.miscItem.BannerItem;
import necesse.inventory.item.miscItem.BinocularsItem;
import necesse.inventory.item.miscItem.BoneOfferingItem;
import necesse.inventory.item.miscItem.ChristmasPresentItem;
import necesse.inventory.item.miscItem.CoinPouch;
import necesse.inventory.item.miscItem.CraftingGuideBookItem;
import necesse.inventory.item.miscItem.DivorcePapersItem;
import necesse.inventory.item.miscItem.DragonSoulsItem;
import necesse.inventory.item.miscItem.EnchantingScrollItem;
import necesse.inventory.item.miscItem.GatewayTabletItem;
import necesse.inventory.item.miscItem.InfiniteRopeItem;
import necesse.inventory.item.miscItem.Lunchbox;
import necesse.inventory.item.miscItem.PortableMusicPlayerItem;
import necesse.inventory.item.miscItem.PotionBag;
import necesse.inventory.item.miscItem.PotionPouch;
import necesse.inventory.item.miscItem.PresentItem;
import necesse.inventory.item.miscItem.PrettyBouquetItem;
import necesse.inventory.item.miscItem.PrettyFlowerItem;
import necesse.inventory.item.miscItem.RecipeBookItem;
import necesse.inventory.item.miscItem.RopeItem;
import necesse.inventory.item.miscItem.SeedPouch;
import necesse.inventory.item.miscItem.ShearsItem;
import necesse.inventory.item.miscItem.StrikeBannerItem;
import necesse.inventory.item.miscItem.TabletBox;
import necesse.inventory.item.miscItem.TelescopeItem;
import necesse.inventory.item.miscItem.VinylItem;
import necesse.inventory.item.miscItem.VoidBagItem;
import necesse.inventory.item.miscItem.VoidPouchItem;
import necesse.inventory.item.miscItem.WorkInProgressItem;
import necesse.inventory.item.miscItem.WrappingPaperItem;
import necesse.inventory.item.mountItem.BumperCarMountItem;
import necesse.inventory.item.mountItem.HoverBoardMountItem;
import necesse.inventory.item.mountItem.JumpingBallMountItem;
import necesse.inventory.item.mountItem.MinecartMountItem;
import necesse.inventory.item.mountItem.MountItem;
import necesse.inventory.item.mountItem.RingOfAscensionMountItem;
import necesse.inventory.item.mountItem.RuneboundBoatMountItem;
import necesse.inventory.item.mountItem.SeahorseMountItem;
import necesse.inventory.item.mountItem.SteelBoatMountItem;
import necesse.inventory.item.mountItem.WitchBroomMountItem;
import necesse.inventory.item.mountItem.WoodBoatMountItem;
import necesse.inventory.item.placeableItem.ApiaryFramePlaceableItem;
import necesse.inventory.item.placeableItem.ButterflySpawnItem;
import necesse.inventory.item.placeableItem.CutterPlaceableItem;
import necesse.inventory.item.placeableItem.FertilizerPlaceableItem;
import necesse.inventory.item.placeableItem.FireflySpawnItem;
import necesse.inventory.item.placeableItem.FireworkPlaceableItem;
import necesse.inventory.item.placeableItem.HoneyBeePlaceableItem;
import necesse.inventory.item.placeableItem.ImportedAnimalSpawnItem;
import necesse.inventory.item.placeableItem.StonePlaceableItem;
import necesse.inventory.item.placeableItem.WrenchPlaceableItem;
import necesse.inventory.item.placeableItem.bucketItem.BucketItem;
import necesse.inventory.item.placeableItem.bucketItem.InfiniteWaterBucketItem;
import necesse.inventory.item.placeableItem.consumableItem.CryoHeartItem;
import necesse.inventory.item.placeableItem.consumableItem.DemonHeartItem;
import necesse.inventory.item.placeableItem.consumableItem.GreaterLifeElixirItem;
import necesse.inventory.item.placeableItem.consumableItem.GuardianHeartItem;
import necesse.inventory.item.placeableItem.consumableItem.ItemSetsIncreaseItem;
import necesse.inventory.item.placeableItem.consumableItem.LifeElixirItem;
import necesse.inventory.item.placeableItem.consumableItem.PortalFlaskItem;
import necesse.inventory.item.placeableItem.consumableItem.RecallFlaskItem;
import necesse.inventory.item.placeableItem.consumableItem.RecallScrollItem;
import necesse.inventory.item.placeableItem.consumableItem.RunicHeartItem;
import necesse.inventory.item.placeableItem.consumableItem.SpiderHeartItem;
import necesse.inventory.item.placeableItem.consumableItem.StinkFlaskItem;
import necesse.inventory.item.placeableItem.consumableItem.TeleportationScrollItem;
import necesse.inventory.item.placeableItem.consumableItem.TestChangeItemSetsItem;
import necesse.inventory.item.placeableItem.consumableItem.TestChangeTrinketSlotsItem;
import necesse.inventory.item.placeableItem.consumableItem.TrinketSlotsIncreaseItem;
import necesse.inventory.item.placeableItem.consumableItem.WardenHeartItem;
import necesse.inventory.item.placeableItem.consumableItem.food.EggFoodConsumableItem;
import necesse.inventory.item.placeableItem.consumableItem.food.FoodConsumableItem;
import necesse.inventory.item.placeableItem.consumableItem.food.FoodMatItem;
import necesse.inventory.item.placeableItem.consumableItem.food.GrainItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.buffPotions.AccuracyPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.buffPotions.AttackSpeedPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.buffPotions.BattlePotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.buffPotions.FireResistancePotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.buffPotions.HealthRegenPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.buffPotions.KnockbackPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.buffPotions.ManaRegenPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.buffPotions.RapidPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.buffPotions.ResistancePotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.buffPotions.SpeedPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.buffPotions.ThornsPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.greaterBuffPotions.GreaterAccuracyPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.greaterBuffPotions.GreaterAttackSpeedPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.greaterBuffPotions.GreaterBattlePotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.greaterBuffPotions.GreaterHealthRegenPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.greaterBuffPotions.GreaterManaRegenPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.greaterBuffPotions.GreaterRapidPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.greaterBuffPotions.GreaterResistancePotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.greaterBuffPotions.GreaterSpeedPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.greaterBuffPotions.MinionPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.greaterBuffPotions.RangerPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.greaterBuffPotions.StrengthPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.greaterBuffPotions.WebPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.greaterBuffPotions.WisdomPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.resourcePotions.HealthPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.resourcePotions.ManaPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.utilityBuffPotions.BuildingPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.utilityBuffPotions.FishingPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.utilityBuffPotions.GreaterBuildingPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.utilityBuffPotions.GreaterFishingPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.utilityBuffPotions.GreaterMiningPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.utilityBuffPotions.InvisibilityPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.utilityBuffPotions.MiningPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.utilityBuffPotions.PassivePotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.utilityBuffPotions.SpelunkerPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.utilityBuffPotions.TrackerPotionItem;
import necesse.inventory.item.placeableItem.consumableItem.potionConsumableItem.utilityBuffPotions.TreasurePotionItem;
import necesse.inventory.item.placeableItem.consumableItem.spawnItems.AncientVultureSpawnItem;
import necesse.inventory.item.placeableItem.consumableItem.spawnItems.CryoQueenSpawnItem;
import necesse.inventory.item.placeableItem.consumableItem.spawnItems.CursedCroneSpawnItem;
import necesse.inventory.item.placeableItem.consumableItem.spawnItems.EvilsProtectorSpawnItem;
import necesse.inventory.item.placeableItem.consumableItem.spawnItems.MotherSlimeSpawnItem;
import necesse.inventory.item.placeableItem.consumableItem.spawnItems.NightSwarmSpawnItem;
import necesse.inventory.item.placeableItem.consumableItem.spawnItems.PestWardenSpawnItem;
import necesse.inventory.item.placeableItem.consumableItem.spawnItems.PirateCaptainSpawnItem;
import necesse.inventory.item.placeableItem.consumableItem.spawnItems.QueenSpiderSpawnItem;
import necesse.inventory.item.placeableItem.consumableItem.spawnItems.ReaperSpawnItem;
import necesse.inventory.item.placeableItem.consumableItem.spawnItems.SpiderEmpressSpawnItem;
import necesse.inventory.item.placeableItem.consumableItem.spawnItems.SwampGuardianSpawnItem;
import necesse.inventory.item.placeableItem.consumableItem.spawnItems.VoidWizardSpawnItem;
import necesse.inventory.item.placeableItem.fishingRodItem.DepthsCatcherRodItem;
import necesse.inventory.item.placeableItem.fishingRodItem.FishingRodItem;
import necesse.inventory.item.placeableItem.followerSummonItem.petFollowerPlaceableItem.PetFollowerPlaceableItem;
import necesse.inventory.item.placeableItem.followerSummonItem.petFollowerPlaceableItem.ThrumboHornPlaceableItem;
import necesse.inventory.item.placeableItem.mapItem.WorldPresetMapItem;
import necesse.inventory.item.placeableItem.tileItem.GrassSeedItem;
import necesse.inventory.item.placeableItem.tileItem.LandfillItem;
import necesse.inventory.item.questItem.ApprenticeScrollQuestItem;
import necesse.inventory.item.questItem.BabySharkQuestItem;
import necesse.inventory.item.questItem.BabySwordfishQuestItem;
import necesse.inventory.item.questItem.BrokenLimbQuestItem;
import necesse.inventory.item.questItem.CapturedSpiritQuestItem;
import necesse.inventory.item.questItem.CaveOysterQuestItem;
import necesse.inventory.item.questItem.CrabClawQuestItem;
import necesse.inventory.item.questItem.CrawlersFootQuestItem;
import necesse.inventory.item.questItem.DarkGemQuestItem;
import necesse.inventory.item.questItem.DeepSpiritSwabQuestItem;
import necesse.inventory.item.questItem.EnchantedCollarQuestItem;
import necesse.inventory.item.questItem.EyePatchQuestItem;
import necesse.inventory.item.questItem.FakeFangsQuestItem;
import necesse.inventory.item.questItem.FeralTailQuestItem;
import necesse.inventory.item.questItem.FrozenBeardQuestItem;
import necesse.inventory.item.questItem.GoblinRingQuestItem;
import necesse.inventory.item.questItem.MagicSandQuestItem;
import necesse.inventory.item.questItem.MummysBandageQuestItem;
import necesse.inventory.item.questItem.PegLegQuestItem;
import necesse.inventory.item.questItem.RazorIcicleQuestItem;
import necesse.inventory.item.questItem.RumBottleQuestItem;
import necesse.inventory.item.questItem.SandRayQuestItem;
import necesse.inventory.item.questItem.SlimeChunkQuestItem;
import necesse.inventory.item.questItem.SlimeSampleQuestItem;
import necesse.inventory.item.questItem.SlimyLauncherQuestItem;
import necesse.inventory.item.questItem.SoakedBowQuestItem;
import necesse.inventory.item.questItem.SpiderLegQuestItem;
import necesse.inventory.item.questItem.SwampEelQuestItem;
import necesse.inventory.item.questItem.WormToothQuestItem;
import necesse.inventory.item.questItem.ZombieArmQuestItem;
import necesse.inventory.item.toolItem.MultiToolItem;
import necesse.inventory.item.toolItem.axeToolItem.CustomAxeToolItem;
import necesse.inventory.item.toolItem.glaiveToolItem.CryoGlaiveToolItem;
import necesse.inventory.item.toolItem.glaiveToolItem.FrostGlaiveToolItem;
import necesse.inventory.item.toolItem.glaiveToolItem.GoldGlaiveToolItem;
import necesse.inventory.item.toolItem.glaiveToolItem.QuartzGlaiveToolItem;
import necesse.inventory.item.toolItem.glaiveToolItem.SlimeGlaiveToolItem;
import necesse.inventory.item.toolItem.miscToolItem.EraserToolItem;
import necesse.inventory.item.toolItem.miscToolItem.FarmingScytheToolItem;
import necesse.inventory.item.toolItem.miscToolItem.NetToolItem;
import necesse.inventory.item.toolItem.miscToolItem.PipetteItem;
import necesse.inventory.item.toolItem.miscToolItem.SickleToolItem;
import necesse.inventory.item.toolItem.miscToolItem.TestToolItem;
import necesse.inventory.item.toolItem.pickaxeToolItem.CustomPickaxeToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.AntiqueBowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.ArachnidWebBowToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.AscendedBowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.BowOfDualismProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.CaptorsShortbowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.CopperBowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.DemonicBowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.DryadBowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.FrostBowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.GlacialBowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.GoldBowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.GoldenArachnidWebBowToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.IronBowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.IvyBowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.NecroticBowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.ReanimationBowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.TheCrimsonSkyProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.TungstenBowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.VulturesBurstProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.WoodBowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.greatbowProjectileToolItem.DruidsGreatBowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.greatbowProjectileToolItem.GoldGreatbowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.greatbowProjectileToolItem.IvyGreatbowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.greatbowProjectileToolItem.MyceliumGreatbowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.greatbowProjectileToolItem.NightPiercerGreatBowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.greatbowProjectileToolItem.SlimeGreatbowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.greatbowProjectileToolItem.TheRavensNestProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.greatbowProjectileToolItem.TungstenGreatbowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.greatbowProjectileToolItem.VoidGreatbowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem.AntiqueRifleProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem.CryoBlasterProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem.DeathRipperProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem.FlintlockProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem.HandCannonProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem.HandGunProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem.LivingShottyProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem.MachineGunProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem.PatchworkGrenadierProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem.SapphireRevolverProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem.SeedGunProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem.ShardCannonProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem.ShotgunProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem.SixShooterProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem.SniperProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem.SnowLauncherProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem.WebbedGunProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.AmethystStaffProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.AncientDredgingStaffProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.AscendedStaffToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.BloodBoltProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.BloodGrimoireProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.BloodVolleyProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.BoulderStaffProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.ChargeBeamProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.ChargeShowerProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.ChromaticSpellbookProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.CryoQuakeProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.DragonLanceProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.DredgingStaffProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.DryadBarrageToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.ElderlyWandProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.EmeraldStaffProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.EmeraldWandProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.FishianHealerStaffToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.FlamelingOrbProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.FrenchHornProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.FrostStaffProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.GenieLampProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.GoldenWebWeaverToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.IcicleStaffProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.MouseBeamProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.MouseTestProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.NecroticFlaskProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.PhantomPopperProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.PyromancyProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.QuartzStaffProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.RefractorProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.RubyStaffProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.RuneboundScepterProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.SapphireStaffProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.ShadowBeamProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.ShadowBoltProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.SlimeStaffProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.SparklerProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.SprinklerProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.SwampDwellerStaffProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.SwampTomeProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.TheSoulstormProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.TopazStaffProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.UnlabeledPotionProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.VampiricLampProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.VenomShowerProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.VenomStaffProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.VoidMissileProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.VoidStaffProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.WebWeaverToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.WoodStaffProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.meleeProjectileToolItem.AnchorAndChainToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.meleeProjectileToolItem.BoxingGloveGunToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.meleeProjectileToolItem.HeavyHammerProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.meleeProjectileToolItem.ReaperScytheProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.CarapaceDaggerToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.DynamiteStickToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.IceJavelinToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.IronBombToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.NinjaStarToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.SnowBallToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.TileBombToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.boomerangToolItem.ButchersCleaverBoomerangToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.boomerangToolItem.ChefsSpecialBoomerangToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.boomerangToolItem.DragonsReboundToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.boomerangToolItem.FrostBoomerangToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.boomerangToolItem.GlacialBoomerangToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.boomerangToolItem.HookBoomerangToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.boomerangToolItem.NightRazorBoomerangToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.boomerangToolItem.RazorBladeBoomerangToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.boomerangToolItem.RollingPinBoomerangToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.boomerangToolItem.SpiderBoomerangToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.boomerangToolItem.TungstenBoomerangToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.boomerangToolItem.VoidBoomerangToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.boomerangToolItem.WoodBoomerangToolItem;
import necesse.inventory.item.toolItem.shovelToolItem.CustomShovelToolItem;
import necesse.inventory.item.toolItem.spearToolItem.CopperPitchforkToolItem;
import necesse.inventory.item.toolItem.spearToolItem.CopperSpearToolItem;
import necesse.inventory.item.toolItem.spearToolItem.CryoSpearToolItem;
import necesse.inventory.item.toolItem.spearToolItem.DemonicSpearToolItem;
import necesse.inventory.item.toolItem.spearToolItem.FrostSpearToolItem;
import necesse.inventory.item.toolItem.spearToolItem.GoldSpearToolItem;
import necesse.inventory.item.toolItem.spearToolItem.IronSpearToolItem;
import necesse.inventory.item.toolItem.spearToolItem.IvySpearToolItem;
import necesse.inventory.item.toolItem.spearToolItem.RavenBeakSpearToolItem;
import necesse.inventory.item.toolItem.spearToolItem.TungstenSpearToolItem;
import necesse.inventory.item.toolItem.spearToolItem.VoidSpearToolItem;
import necesse.inventory.item.toolItem.spearToolItem.VulturesTalonToolItem;
import necesse.inventory.item.toolItem.spearToolItem.WoodSpearToolItem;
import necesse.inventory.item.toolItem.summonToolItem.BashyBushSummonToolItem;
import necesse.inventory.item.toolItem.summonToolItem.BatCageSummonToolItem;
import necesse.inventory.item.toolItem.summonToolItem.BrainOnAStickToolItem;
import necesse.inventory.item.toolItem.summonToolItem.CryoStaffSummonToolItem;
import necesse.inventory.item.toolItem.summonToolItem.CrystallizedSkullSummonToolItem;
import necesse.inventory.item.toolItem.summonToolItem.DryadBranchSummonToolItem;
import necesse.inventory.item.toolItem.summonToolItem.EmpressCommandToolItem;
import necesse.inventory.item.toolItem.summonToolItem.EyeOfTheVoidSummonToolItem;
import necesse.inventory.item.toolItem.summonToolItem.FrostPiercerSummonToolItem;
import necesse.inventory.item.toolItem.summonToolItem.GrizzboltSphereSummonToolItem;
import necesse.inventory.item.toolItem.summonToolItem.HiveScepterSummonToolItem;
import necesse.inventory.item.toolItem.summonToolItem.IgnitionKeySummonToolItem;
import necesse.inventory.item.toolItem.summonToolItem.MagicBranchSummonToolItem;
import necesse.inventory.item.toolItem.summonToolItem.OrbOfSlimesToolItem;
import necesse.inventory.item.toolItem.summonToolItem.PhantomCallerSummonToolItem;
import necesse.inventory.item.toolItem.summonToolItem.PirateHookSummonToolItem;
import necesse.inventory.item.toolItem.summonToolItem.ReapersCallSummonToolItem;
import necesse.inventory.item.toolItem.summonToolItem.RubyShieldsToolItem;
import necesse.inventory.item.toolItem.summonToolItem.SentientSwordSummonToolItem;
import necesse.inventory.item.toolItem.summonToolItem.SkeletonStaffToolItem;
import necesse.inventory.item.toolItem.summonToolItem.SlimeCanisterSummonToolItem;
import necesse.inventory.item.toolItem.summonToolItem.SpiderStaffSummonToolItem;
import necesse.inventory.item.toolItem.summonToolItem.StabbyBushSummonToolItem;
import necesse.inventory.item.toolItem.summonToolItem.SwampsGraspToolItem;
import necesse.inventory.item.toolItem.summonToolItem.VultureStaffSummonToolItem;
import necesse.inventory.item.toolItem.swordToolItem.AgedChampionSwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.AmethystSwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.AncestorSwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.AncestorWandToolItem;
import necesse.inventory.item.toolItem.swordToolItem.AntiqueSwordSwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.BarkBladeSwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.BloodClawToolItem;
import necesse.inventory.item.toolItem.swordToolItem.CausticExecutionerToolItem;
import necesse.inventory.item.toolItem.swordToolItem.CopperSwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.CutlassSwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.DemonicSwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.FishianWarriorHookSwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.FrostSwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.GalvanicHammerToolItem;
import necesse.inventory.item.toolItem.swordToolItem.GemstoneLongswordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.GoldSwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.GoldenCausticExecutionerToolItem;
import necesse.inventory.item.toolItem.swordToolItem.IronSwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.IvySwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.KatanaToolItem;
import necesse.inventory.item.toolItem.swordToolItem.KineticChargerToolItem;
import necesse.inventory.item.toolItem.swordToolItem.LightningHammerToolItem;
import necesse.inventory.item.toolItem.swordToolItem.MLG1SwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.MLG2SwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.NunchucksToolItem;
import necesse.inventory.item.toolItem.swordToolItem.PerfectStormSwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.ReinforcedKatanaToolItem;
import necesse.inventory.item.toolItem.swordToolItem.RevSwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.SandKnifeToolItem;
import necesse.inventory.item.toolItem.swordToolItem.SpiderClawSwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.StopSignSwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.SurvivorWhipToolItem;
import necesse.inventory.item.toolItem.swordToolItem.SyndicateBatSwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.TungstenSwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.VenomSlasherToolItem;
import necesse.inventory.item.toolItem.swordToolItem.VoidClawSwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.WoodSwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem.BrutesBattleaxeToolItem;
import necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem.DryadGreatHammerToolItem;
import necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem.FrostGreatswordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem.GlacialGreatswordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem.HexedBladeGreatswordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem.InvisibleSwordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem.IronGreatswordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem.IvyGreatswordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem.NecroticGreatswordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem.QuartzGreatswordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem.RavenwingGreatswordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem.SlimeGreatswordToolItem;
import necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem.WoodGreatswordToolItem;
import necesse.inventory.item.trinketItem.BlinkScepterTrinketItem;
import necesse.inventory.item.trinketItem.CactusShieldTrinketItem;
import necesse.inventory.item.trinketItem.CalmingMinersBouquetTrinketItem;
import necesse.inventory.item.trinketItem.CombinedTrinketItem;
import necesse.inventory.item.trinketItem.CrystalShieldTrinketItem;
import necesse.inventory.item.trinketItem.FoolsGambitTrinketItem;
import necesse.inventory.item.trinketItem.ForceOfWindTrinketItem;
import necesse.inventory.item.trinketItem.GhostBootsTrinketItem;
import necesse.inventory.item.trinketItem.HoverBootsTrinketItem;
import necesse.inventory.item.trinketItem.JonasGambitTrinketItem;
import necesse.inventory.item.trinketItem.KineticBootsTrinketItem;
import necesse.inventory.item.trinketItem.LeatherDashersTrinketItem;
import necesse.inventory.item.trinketItem.MinersBouquetTrinketItem;
import necesse.inventory.item.trinketItem.MoonShieldTrinketItem;
import necesse.inventory.item.trinketItem.ParryBucklerShieldTrinketItem;
import necesse.inventory.item.trinketItem.ShieldTrinketItem;
import necesse.inventory.item.trinketItem.SimpleTrinketItem;
import necesse.inventory.item.trinketItem.SiphonShieldTrinketItem;
import necesse.inventory.item.trinketItem.VoidPhasingStaffTrinketItem;
import necesse.inventory.item.trinketItem.WindBootsTrinketItem;
import necesse.inventory.item.trinketItem.WoodShieldTrinketItem;
import necesse.inventory.item.trinketItem.ZephyrBootsTrinketItem;
import necesse.inventory.lootTable.presets.IncursionBowWeaponsLootTable;
import necesse.inventory.lootTable.presets.IncursionGreatswordWeaponsLootTable;
import necesse.inventory.lootTable.presets.IncursionMagicWeaponsLootTable;
import necesse.inventory.lootTable.presets.IncursionTrinketsLootTable;
import necesse.inventory.lootTable.presets.ToolsLootTable;
import necesse.inventory.lootTable.presets.TrinketsLootTable;
import necesse.inventory.recipe.RecipeBrokerValueCompute;
import necesse.inventory.recipe.Recipes;
import necesse.level.maps.levelData.settlementData.settler.Settler;

public class ItemRegistry extends GameRegistry<ItemRegistry.ItemRegistryElement> {
   public static final ItemRegistry instance = new ItemRegistry();
   private static int totalItemsObtainable = 0;
   private static int totalStatItemsObtainable = 0;
   private static int totalItems = 0;
   private static int totalTrinkets = 0;
   public static final int WOOD_TOOL_DPS = 50;
   public static final int COPPER_TOOL_DPS = 65;
   public static final int IRON_TOOL_DPS = 80;
   public static final int GOLD_TOOL_DPS = 95;
   public static final int FROST_TOOL_DPS = 110;
   public static final int DEMONIC_TOOL_DPS = 125;
   public static final int RUNIC_TOOL_DPS = 140;
   public static final int IVY_TOOL_DPS = 155;
   public static final int QUARTZ_TOOL_DPS = 170;
   public static final int TUNGSTEN_TOOL_DPS = 185;
   public static final int GLACIAL_TOOLS_DPS = 200;
   public static final int DRYAD_TOOLS_DPS = 215;
   public static final int MYCELIUM_TOOLS_DPS = 230;
   public static final int ANCIENT_FOSSIl_TOOLS_DPS = 245;
   public static final int FROST_TOOL_TIER = 1;
   public static final int DEMONIC_TOOL_TIER = 2;
   public static final int RUNIC_TOOL_TIER = 3;
   public static final int IVY_TOOL_TIER = 4;
   public static final int QUARTZ_TOOL_TIER = 5;
   public static final int TUNGSTEN_TOOL_TIER = 6;
   public static final int GLACIAL_TOOL_TIER = 7;
   public static final int DRYAD_TOOL_TIER = 8;
   public static final int MYCELIUM_TOOL_TIER = 9;
   public static final int ANCIENT_FOSSIL_TOOL_TIER = 10;
   public static final int EQUIPMENT_VALUE_WOOD = 100;
   public static final int EQUIPMENT_VALUE_COPPER = 200;
   public static final int EQUIPMENT_VALUE_IRON = 300;
   public static final int EQUIPMENT_VALUE_GOLD = 350;
   public static final int EQUIPMENT_VALUE_SOLDIER = 375;
   public static final int EQUIPMENT_VALUE_EVILS_PROTECTOR = 400;
   public static final int EQUIPMENT_VALUE_FROST = 500;
   public static final int EQUIPMENT_VALUE_QUEEN_SPIDER = 550;
   public static final int EQUIPMENT_VALUE_VOID_SHARDS = 650;
   public static final int EQUIPMENT_VALUE_VOID_WIZARD = 700;
   public static final int EQUIPMENT_VALUE_RUNESTONE = 750;
   public static final int EQUIPMENT_VALUE_CHIEFTAIN = 800;
   public static final int EQUIPMENT_VALUE_IVY = 850;
   public static final int EQUIPMENT_VALUE_SWAMP_GUARDIAN = 900;
   public static final int EQUIPMENT_VALUE_QUARTZ = 1000;
   public static final int EQUIPMENT_VALUE_ANCIENT_VULTURE = 1050;
   public static final int EQUIPMENT_VALUE_PIRATE_ISLAND = 1150;
   public static final int EQUIPMENT_VALUE_PIRATE_CAPTAIN = 1200;
   public static final int EQUIPMENT_VALUE_TUNGSTEN = 1300;
   public static final int EQUIPMENT_VALUE_REAPER = 1350;
   public static final int EQUIPMENT_VALUE_GLACIAL = 1450;
   public static final int EQUIPMENT_VALUE_CRYO_QUEEN = 1500;
   public static final int EQUIPMENT_VALUE_DRYAD = 1550;
   public static final int EQUIPMENT_VALUE_CURSED_CRONE = 1575;
   public static final int EQUIPMENT_VALUE_MYCELIUM = 1600;
   public static final int EQUIPMENT_VALUE_PEST_WARDEN = 1650;
   public static final int EQUIPMENT_VALUE_ANCIENT_FOSSIL = 1750;
   public static final int EQUIPMENT_VALUE_SAGE_AND_GRIT = 1800;
   public static final int EQUIPMENT_VALUE_DESERT_TEMPLE = 1850;
   public static final int EQUIPMENT_VALUE_TIERED_ONLY = 1900;
   public static final int EQUIPMENT_VALUE_INCURSIONS = 2000;
   public static int REVIVAL_POTION_ID;

   private ItemRegistry() {
      super("Item", 32762);
   }

   @Override
   public void registerCore() {
      GameLoadingScreen.drawLoadingString(Localization.translate("loading", "items"));
      registerItem("oaklog", new MatItem(500, "anylog").setItemCategory("materials", "logs"), 2.0F, true);
      registerItem("sprucelog", new MatItem(500, "anylog").setItemCategory("materials", "logs"), 2.0F, true);
      registerItem("pinelog", new MatItem(500, "anylog").setItemCategory("materials", "logs"), 2.0F, true);
      registerItem("palmlog", new MatItem(500, "anylog").setItemCategory("materials", "logs"), 2.0F, true);
      registerItem("willowlog", new MatItem(500, "anylog").setItemCategory("materials", "logs"), 2.0F, true);
      registerItem("maplelog", new MatItem(500, "anylog").setItemCategory("materials", "logs"), 2.0F, true);
      registerItem("birchlog", new MatItem(500, "anylog").setItemCategory("materials", "logs"), 2.0F, true);
      registerItem("bamboo", new MatItem(500, "anylog").setItemCategory("materials", "logs"), 2.0F, true);
      registerItem("deadwoodlog", new MatItem(500, "anylog").setItemCategory("materials", "logs"), 2.0F, true);
      registerItem("dryadlog", new MatItem(500, "anylog").setItemCategory("materials", "logs"), 2.0F, true);
      registerItem("charredbark", new MatItem(500).setItemCategory("materials"), 2.0F, true);
      registerItem("stone", new StonePlaceableItem(5000), 0.1F, true);
      registerItem("sandstone", new StonePlaceableItem(5000), 0.1F, true);
      registerItem("swampstone", new StonePlaceableItem(5000), 0.1F, true);
      registerItem("snowstone", new StonePlaceableItem(5000), 0.1F, true);
      registerItem("granite", new StonePlaceableItem(5000), 0.1F, true);
      registerItem("deepstone", new StonePlaceableItem(5000), 0.1F, true);
      registerItem("deepsnowstone", new StonePlaceableItem(5000), 0.1F, true);
      registerItem("basalt", new StonePlaceableItem(5000), 0.1F, true);
      registerItem("deepswampstone", new StonePlaceableItem(5000), 0.1F, true);
      registerItem("deepsandstone", new StonePlaceableItem(5000), 0.1F, true);
      registerItem("slimestone", new StonePlaceableItem(5000), 0.1F, true);
      registerItem("cryptstone", new StonePlaceableItem(5000), 0.1F, true);
      registerItem("spiderstone", new StonePlaceableItem(5000), 0.1F, true);
      registerItem("crystalstone", new StonePlaceableItem(5000), 0.1F, true);
      registerItem("junk", new StonePlaceableItem(5000), 0.1F, true);
      registerItem("batwing", new MatItem(500).setItemCategory("materials", "mobdrops"), 10.0F, true);
      registerItem("wool", new MatItem(500).setItemCategory("materials", "mobdrops"), 7.0F, true);
      registerItem("leather", new MatItem(500).setItemCategory("materials", "mobdrops"), 7.0F, true);
      registerItem("clothscraps", new MatItem(500, Item.Rarity.COMMON).setItemCategory("materials", "mobdrops"), 10.0F, true);
      registerItem("clay", new MatItem(500).setItemCategory("materials", "minerals"), 4.0F, true);
      registerItem("copperore", new MatItem(500).setItemCategory("materials", "ore"), 1.0F, true);
      registerItem("copperbar", new MatItem(250).setItemCategory("materials", "bars"), 4.0F, true);
      registerItem("ironore", new MatItem(500).setItemCategory("materials", "ore"), 1.5F, true);
      registerItem("ironbar", new MatItem(250).setItemCategory("materials", "bars"), 6.0F, true);
      registerItem("goldore", new MatItem(500).setItemCategory("materials", "ore"), 2.5F, true);
      registerItem("goldbar", new MatItem(250).setItemCategory("materials", "bars"), 10.0F, true);
      registerItem("frostshard", new MatItem(250, Item.Rarity.COMMON).setItemCategory("materials", "minerals"), 10.0F, true);
      registerItem("runestone", new MatItem(250, Item.Rarity.COMMON).setItemCategory("materials", "minerals"), 10.0F, true);
      registerItem("ivyore", new MatItem(500, Item.Rarity.COMMON).setItemCategory("materials", "ore"), 3.0F, true);
      registerItem("ivybar", new MatItem(250, Item.Rarity.COMMON).setItemCategory("materials", "bars"), 12.0F, true);
      registerItem("quartz", new MatItem(250, Item.Rarity.COMMON).setItemCategory("materials", "minerals"), 15.0F, true);
      registerItem("lifequartz", new MatItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "minerals"), 20.0F, true);
      registerItem("demonicbar", new MatItem(250, Item.Rarity.COMMON).setItemCategory("materials", "bars"), 10.0F, true);
      registerItem("tungstenore", new MatItem(500, Item.Rarity.UNCOMMON).setItemCategory("materials", "ore"), 6.0F, true);
      registerItem("tungstenbar", new MatItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "bars"), 20.0F, true);
      registerItem("glacialore", new MatItem(500, Item.Rarity.UNCOMMON).setItemCategory("materials", "ore"), 6.0F, true);
      registerItem("glacialbar", new MatItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "bars"), 20.0F, true);
      registerItem("amber", new MatItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "minerals"), 20.0F, true);
      registerItem("myceliumore", new MatItem(500, Item.Rarity.UNCOMMON).setItemCategory("materials", "ore"), 6.0F, true);
      registerItem("myceliumbar", new MatItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "bars"), 20.0F, true);
      registerItem("ancientfossilore", new MatItem(500, Item.Rarity.UNCOMMON).setItemCategory("materials", "ore"), 6.0F, true);
      registerItem("ancientfossilbar", new MatItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "bars"), 20.0F, true);
      registerItem("obsidian", new MatItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "minerals"), 2.0F, true);
      registerItem("amethyst", new MatItem(250, Item.Rarity.RARE).setItemCategory("materials", "minerals").addGlobalIngredient("anygem"), 25.0F, true);
      registerItem("sapphire", new MatItem(250, Item.Rarity.RARE).setItemCategory("materials", "minerals").addGlobalIngredient("anygem"), 25.0F, true);
      registerItem("emerald", new MatItem(250, Item.Rarity.RARE).setItemCategory("materials", "minerals").addGlobalIngredient("anygem"), 25.0F, true);
      registerItem("ruby", new MatItem(250, Item.Rarity.RARE).setItemCategory("materials", "minerals").addGlobalIngredient("anygem"), 25.0F, true);
      registerItem("topaz", new MatItem(250, Item.Rarity.RARE).setItemCategory("materials", "minerals").addGlobalIngredient("anygem"), 25.0F, true);
      registerItem("slimeum", new MatItem(500, Item.Rarity.RARE).setItemCategory("materials", "minerals"), 10.0F, true);
      registerItem("nightsteelore", new MatItem(500, Item.Rarity.RARE).setItemCategory("materials", "ore"), 8.0F, true);
      registerItem("nightsteelbar", new MatItem(250, Item.Rarity.RARE).setItemCategory("materials", "bars"), 24.0F, true);
      registerItem("spideriteore", new MatItem(500, Item.Rarity.RARE).setItemCategory("materials", "ore"), 10.0F, true);
      registerItem("spideritebar", new MatItem(250, Item.Rarity.RARE).setItemCategory("materials", "bars"), 28.0F, true);
      registerItem("pearlescentdiamond", new MatItem(250, Item.Rarity.RARE).setItemCategory("materials", "bars"), 35.0F, true);
      registerItem("fuelskull", new MatItem(250, Item.Rarity.RARE).setItemCategory("materials", "bars"), 42.0F, true);
      registerItem("prettyflower", new PrettyFlowerItem(), 250.0F, true);
      registerItem("prettybouquet", new PrettyBouquetItem(), 1000.0F, true);
      registerItem("divorcepapers", new DivorcePapersItem(), 500.0F, true);
      registerItem("cavespidergland", new MultiTextureMatItem(3, 100, Item.Rarity.COMMON).setItemCategory("materials", "mobdrops"), 15.0F, true);
      registerItem("voidshard", new MatItem(500, Item.Rarity.UNCOMMON, "voidshardtip").setItemCategory("materials", "mobdrops"), 10.0F, true);
      registerItem("swampsludge", new MatItem(500, Item.Rarity.UNCOMMON).setItemCategory("materials", "mobdrops"), 10.0F, true);
      registerItem("bone", new MatItem(500, Item.Rarity.UNCOMMON).setItemCategory("materials", "mobdrops"), 8.0F, true);
      registerItem("ectoplasm", new MatItem(500, Item.Rarity.UNCOMMON).setItemCategory("materials", "mobdrops"), 12.0F, true);
      registerItem("glacialshard", new MatItem(500, Item.Rarity.UNCOMMON).setItemCategory("materials", "mobdrops"), 12.0F, true);
      registerItem("silk", new MatItem(500, Item.Rarity.UNCOMMON).setItemCategory("materials", "mobdrops"), 12.0F, true);
      registerItem("wormcarapace", new MatItem(500, Item.Rarity.UNCOMMON).setItemCategory("materials", "mobdrops"), 12.0F, true);
      registerItem("phantomdust", new MatItem(500, Item.Rarity.RARE).setItemCategory("materials", "mobdrops"), 14.0F, true);
      registerItem("glass", new MatItem(500), 1.0F, true);
      registerItem("glassbottle", new MatItem(500, Item.Rarity.NORMAL, "glassbottletip"), 1.0F, true);
      registerItem("book", new BookMatItem(), 15.0F, true);
      registerItem("slimematter", new MatItem(500, Item.Rarity.RARE).setItemCategory("materials", "mobdrops"), 15.0F, true);
      registerItem("spidervenom", new MatItem(500, Item.Rarity.RARE).setItemCategory("materials", "mobdrops"), 15.0F, true);
      registerItem("omnicrystal", new MatItem(500, Item.Rarity.RARE).setItemCategory("materials", "mobdrops"), 15.0F, true);
      registerItem("pristinegear", new MatItem(500, Item.Rarity.EPIC).setItemCategory("materials", "mobdrops"), 15.0F, true);
      registerItem("ravenfeather", new MatItem(500, Item.Rarity.EPIC).setItemCategory("materials", "mobdrops"), 15.0F, true);
      registerItem("electrifiedmana", new MatItem(500, Item.Rarity.EPIC).setItemCategory("materials", "mobdrops"), 15.0F, true);
      registerItem("sharktooth", new MatItem(500, Item.Rarity.UNCOMMON).setItemCategory("materials", "mobdrops"), 15.0F, true);
      registerItem("sharkscales", new MatItem(500, Item.Rarity.UNCOMMON).setItemCategory("materials", "mobdrops"), 15.0F, true);
      registerItem("phoenixfeather", new MatItem(500, Item.Rarity.RARE).setItemCategory("materials", "mobdrops"), 20.0F, true);
      registerItem("livingash", new MatItem(500, Item.Rarity.RARE).setItemCategory("materials", "mobdrops"), 20.0F, true);
      String[] tierOneEssenceCraftingCat = new String[]{"incursions", "tier1essences"};
      String[] tierTwoEssenceCraftingCat = new String[]{"incursions", "tier2essences"};
      String[] tierThreeEssenceCraftingCat = new String[]{"incursions", "tier3essences"};
      String[] dustAndShardsCraftingCat = new String[]{"incursions", "dustandshards"};
      registerItem(
         "altardust",
         new MatItem(5000, Item.Rarity.COMMON, "altardusttip")
            .setItemCategory("materials", "mobdrops")
            .setItemCategory(ItemCategory.craftingManager, dustAndShardsCraftingCat),
         5.0F,
         true
      );
      registerItem(
         "alchemyshard",
         new MatItem(1000, Item.Rarity.COMMON, "alchemyshardtip")
            .setItemCategory("materials", "minerals")
            .setItemCategory(ItemCategory.craftingManager, dustAndShardsCraftingCat),
         8.0F,
         true
      );
      registerItem(
         "upgradeshard",
         new MatItem(1000, Item.Rarity.COMMON, "upgradeshardtip")
            .setItemCategory("materials", "minerals")
            .setItemCategory(ItemCategory.craftingManager, dustAndShardsCraftingCat),
         8.0F,
         true
      );
      registerItem("ascendedshard", new AscendedShardItem(), 100.0F, true);
      registerItem(
         "shadowessence",
         new EssenceMatItem(250, Item.Rarity.UNCOMMON, 1).setItemCategory(ItemCategory.craftingManager, tierOneEssenceCraftingCat),
         25.0F,
         true
      );
      registerItem(
         "cryoessence", new EssenceMatItem(250, Item.Rarity.UNCOMMON, 1).setItemCategory(ItemCategory.craftingManager, tierOneEssenceCraftingCat), 25.0F, true
      );
      registerItem(
         "bioessence", new EssenceMatItem(250, Item.Rarity.UNCOMMON, 1).setItemCategory(ItemCategory.craftingManager, tierOneEssenceCraftingCat), 25.0F, true
      );
      registerItem(
         "primordialessence",
         new EssenceMatItem(250, Item.Rarity.UNCOMMON, 1).setItemCategory(ItemCategory.craftingManager, tierOneEssenceCraftingCat),
         25.0F,
         true
      );
      registerItem(
         "slimeessence", new EssenceMatItem(250, Item.Rarity.RARE, 2).setItemCategory(ItemCategory.craftingManager, tierTwoEssenceCraftingCat), 30.0F, true
      );
      registerItem(
         "bloodessence", new EssenceMatItem(250, Item.Rarity.RARE, 2).setItemCategory(ItemCategory.craftingManager, tierTwoEssenceCraftingCat), 30.0F, true
      );
      registerItem(
         "spideressence", new EssenceMatItem(250, Item.Rarity.RARE, 2).setItemCategory(ItemCategory.craftingManager, tierTwoEssenceCraftingCat), 30.0F, true
      );
      registerItem(
         "crystalessence", new EssenceMatItem(250, Item.Rarity.EPIC, 3).setItemCategory(ItemCategory.craftingManager, tierThreeEssenceCraftingCat), 35.0F, true
      );
      registerItem(
         "radiatedessence",
         new EssenceMatItem(250, Item.Rarity.EPIC, 3).setItemCategory(ItemCategory.craftingManager, tierThreeEssenceCraftingCat),
         35.0F,
         true
      );
      registerItem("mapfragment", new MatItem(250, Item.Rarity.UNCOMMON, "mapfragmenttip").setItemCategory("materials", "mobdrops"), 30.0F, true);
      registerItem("spareboatparts", new MatItem(1, Item.Rarity.RARE, "sparepartstip"), 250.0F, true);
      registerItem(
         "brokencoppertool", new MultiTextureMatItem(4, 100, Item.Rarity.COMMON, "brokentooltip").setItemCategory("materials", "mobdrops"), 20.0F, true
      );
      registerItem("brokenirontool", new MultiTextureMatItem(4, 100, Item.Rarity.COMMON, "brokentooltip").setItemCategory("materials", "mobdrops"), 30.0F, true);
      registerItem("woodpickaxe", new CustomPickaxeToolItem(500, 50, 0.0F, 10, 50, 50, 100, ToolsLootTable.tools), 8.0F, true);
      registerItem("copperpickaxe", new CustomPickaxeToolItem(500, 65, 0.0F, 12, 50, 50, 200, ToolsLootTable.tools), 17.0F, true);
      registerItem("ironpickaxe", new CustomPickaxeToolItem(500, 80, 0.0F, 14, 50, 50, 400, ToolsLootTable.tools), 25.0F, true);
      registerItem("goldpickaxe", new CustomPickaxeToolItem(500, 95, 0.0F, 16, 50, 50, 450, ToolsLootTable.tools), 40.0F, true);
      registerItem("frostpickaxe", new CustomPickaxeToolItem(500, 110, 1.0F, 18, 50, 50, 500, ToolsLootTable.tools) {
         @Override
         public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
            ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
            tooltips.add(Localization.translate("itemtooltip", "frosttooltip"), 350);
            return tooltips;
         }
      }, 60.0F, true);
      registerItem("demonicpickaxe", new CustomPickaxeToolItem(500, 125, 2.0F, 20, 50, 50, 600, ToolsLootTable.tools, Item.Rarity.COMMON) {
         @Override
         public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
            ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
            tooltips.add(Localization.translate("itemtooltip", "demonictooltipnew"), 350);
            return tooltips;
         }
      }, 80.0F, true);
      registerItem("runicpickaxe", new CustomPickaxeToolItem(500, 140, 3.0F, 22, 50, 50, 700, ToolsLootTable.tools, Item.Rarity.COMMON) {
         @Override
         public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
            ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
            tooltips.add(Localization.translate("itemtooltip", "runictooltip"), 350);
            return tooltips;
         }
      }, 100.0F, true);
      registerItem("ivypickaxe", new CustomPickaxeToolItem(450, 155, 4.0F, 24, 50, 50, 700, ToolsLootTable.tools, Item.Rarity.COMMON) {
         @Override
         public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
            ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
            tooltips.add(Localization.translate("itemtooltip", "ivytooltip"), 350);
            return tooltips;
         }
      }, 100.0F, true);
      registerItem("quartzpickaxe", new CustomPickaxeToolItem(450, 170, 5.0F, 26, 50, 50, 700, ToolsLootTable.tools, Item.Rarity.COMMON) {
         @Override
         public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
            ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
            tooltips.add(Localization.translate("itemtooltip", "quartztooltip"), 350);
            return tooltips;
         }
      }, 100.0F, true);
      registerItem("tungstenpickaxe", new CustomPickaxeToolItem(400, 185, 6.0F, 28, 50, 50, 800, ToolsLootTable.tools, Item.Rarity.UNCOMMON) {
         @Override
         public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
            ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
            tooltips.add(Localization.translate("itemtooltip", "tungstentooltip"), 350);
            return tooltips;
         }
      }, 160.0F, true);
      registerItem("glacialpickaxe", new CustomPickaxeToolItem(400, 200, 7.0F, 30, 50, 50, 900, ToolsLootTable.tools, Item.Rarity.UNCOMMON) {
         @Override
         public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
            ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
            tooltips.add(Localization.translate("itemtooltip", "glacialtooltipnew"), 350);
            return tooltips;
         }
      }, 160.0F, true);
      registerItem("icepickaxe", new CustomPickaxeToolItem(400, 245, 10.0F, 24, 50, 50, 1000, ToolsLootTable.tools, Item.Rarity.RARE, 1), 400.0F, true);
      registerItem("dryadpickaxe", new CustomPickaxeToolItem(400, 215, 8.0F, 32, 50, 50, 950, ToolsLootTable.tools, Item.Rarity.UNCOMMON) {
         @Override
         public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
            ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
            tooltips.add(Localization.translate("itemtooltip", "dryadtooltip"), 350);
            return tooltips;
         }
      }, 160.0F, true);
      registerItem("myceliumpickaxe", new CustomPickaxeToolItem(400, 230, 9.0F, 34, 50, 50, 1000, ToolsLootTable.tools, Item.Rarity.UNCOMMON) {
         @Override
         public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
            ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
            tooltips.add(Localization.translate("itemtooltip", "myceliumtooltip"), 350);
            return tooltips;
         }
      }, 160.0F, true);
      registerItem(
         "ancientfossilpickaxe", new CustomPickaxeToolItem(400, 245, 10.0F, 36, 50, 50, 1000, ToolsLootTable.tools, Item.Rarity.UNCOMMON), 160.0F, true
      );
      registerItem("woodaxe", new CustomAxeToolItem(500, 50, 0.0F, 10, 50, 50, 100, ToolsLootTable.tools), 8.0F, true);
      registerItem("copperaxe", new CustomAxeToolItem(500, 65, 0.0F, 12, 50, 50, 200, ToolsLootTable.tools), 17.0F, true);
      registerItem("ironaxe", new CustomAxeToolItem(500, 80, 0.0F, 14, 50, 50, 300, ToolsLootTable.tools), 25.0F, true);
      registerItem("goldaxe", new CustomAxeToolItem(500, 95, 0.0F, 15, 50, 50, 400, ToolsLootTable.tools), 40.0F, true);
      registerItem("frostaxe", new CustomAxeToolItem(500, 110, 0.0F, 16, 50, 50, 500, ToolsLootTable.tools), 60.0F, true);
      registerItem("demonicaxe", new CustomAxeToolItem(500, 125, 2.0F, 17, 50, 50, 600, ToolsLootTable.tools, Item.Rarity.COMMON), 80.0F, true);
      registerItem("runicaxe", new CustomAxeToolItem(500, 140, 3.0F, 18, 50, 50, 700, ToolsLootTable.tools, Item.Rarity.COMMON), 80.0F, true);
      registerItem("ivyaxe", new CustomAxeToolItem(450, 155, 2.0F, 19, 50, 50, 800, ToolsLootTable.tools, Item.Rarity.COMMON), 100.0F, true);
      registerItem("quartzaxe", new CustomAxeToolItem(450, 170, 5.0F, 20, 50, 50, 900, ToolsLootTable.tools, Item.Rarity.COMMON), 100.0F, true);
      registerItem("tungstenaxe", new CustomAxeToolItem(400, 185, 6.0F, 22, 50, 50, 1000, ToolsLootTable.tools, Item.Rarity.UNCOMMON), 160.0F, true);
      registerItem("glacialaxe", new CustomAxeToolItem(400, 200, 7.0F, 24, 50, 50, 1100, ToolsLootTable.tools, Item.Rarity.UNCOMMON), 160.0F, true);
      registerItem("dryadaxe", new CustomAxeToolItem(400, 215, 8.0F, 26, 50, 50, 1200, ToolsLootTable.tools, Item.Rarity.UNCOMMON), 160.0F, true);
      registerItem("myceliumaxe", new CustomAxeToolItem(400, 230, 9.0F, 28, 50, 50, 1300, ToolsLootTable.tools, Item.Rarity.UNCOMMON), 160.0F, true);
      registerItem("ancientfossilaxe", new CustomAxeToolItem(400, 245, 10.0F, 30, 50, 50, 1400, ToolsLootTable.tools, Item.Rarity.UNCOMMON), 160.0F, true);
      registerItem("woodshovel", new CustomShovelToolItem(500, 50, 0.0F, 10, 50, 50, 100), 8.0F, true);
      registerItem("coppershovel", new CustomShovelToolItem(500, 65, 0.0F, 12, 50, 50, 200), 17.0F, true);
      registerItem("ironshovel", new CustomShovelToolItem(500, 80, 0.0F, 14, 50, 50, 300), 25.0F, true);
      registerItem("goldshovel", new CustomShovelToolItem(500, 95, 0.0F, 15, 50, 50, 400), 40.0F, true);
      registerItem("frostshovel", new CustomShovelToolItem(500, 110, 0.0F, 16, 50, 50, 500), 60.0F, true);
      registerItem("demonicshovel", new CustomShovelToolItem(500, 125, 2.0F, 17, 50, 50, 600, Item.Rarity.COMMON), 80.0F, true);
      registerItem("runicshovel", new CustomShovelToolItem(500, 140, 3.0F, 18, 50, 50, 700, Item.Rarity.COMMON), 80.0F, true);
      registerItem("ivyshovel", new CustomShovelToolItem(450, 155, 2.0F, 19, 50, 50, 800, Item.Rarity.COMMON), 100.0F, true);
      registerItem("quartzshovel", new CustomShovelToolItem(450, 170, 5.0F, 20, 50, 50, 900, Item.Rarity.COMMON), 100.0F, true);
      registerItem("tungstenshovel", new CustomShovelToolItem(400, 185, 6.0F, 22, 50, 50, 1000, Item.Rarity.UNCOMMON), 160.0F, true);
      registerItem("glacialshovel", new CustomShovelToolItem(400, 200, 7.0F, 24, 50, 50, 1100, Item.Rarity.UNCOMMON), 160.0F, true);
      registerItem("dryadshovel", new CustomShovelToolItem(400, 215, 8.0F, 26, 50, 50, 1200, Item.Rarity.UNCOMMON), 160.0F, true);
      registerItem("myceliumshovel", new CustomShovelToolItem(400, 230, 9.0F, 28, 50, 50, 1300, Item.Rarity.UNCOMMON), 160.0F, true);
      registerItem("ancientfossilshovel", new CustomShovelToolItem(400, 245, 10.0F, 30, 50, 50, 1400, Item.Rarity.UNCOMMON), 160.0F, true);
      registerItem("godrod", new FishingRodItem(100, 40, 35, 400, 5000, 5, 40, 0, Item.Rarity.LEGENDARY), 0.0F, false);
      registerItem("woodfishingrod", new FishingRodItem(5, 37, 30, Item.Rarity.NORMAL), 20.0F, true);
      registerItem("ironfishingrod", new FishingRodItem(15, 37, 30, Item.Rarity.NORMAL), 150.0F, true);
      registerItem("overgrownfishingrod", new FishingRodItem(25, 38, 14, Item.Rarity.COMMON), 200.0F, true);
      registerItem("goldfishingrod", new FishingRodItem(30, 37, 30, Item.Rarity.COMMON), 250.0F, true);
      registerItem("depthscatcher", new DepthsCatcherRodItem(), 400.0F, true);
      registerItem("godbait", new BaitItem(false, 100), 0.0F, false);
      registerItem("wormbait", new BaitItem(true, 10), 2.0F, true);
      registerItem("swamplarva", new BaitItem(true, 20), 3.0F, true);
      registerItem("anglersbait", new BaitItem(false, 25), 5.0F, true);
      registerItem("cheattool", new TestToolItem(), 0.0F, false);
      registerItem("sickle", new SickleToolItem(), 50.0F, true);
      registerItem("farmingscythe", new FarmingScytheToolItem(), 550.0F, true);
      registerItem("net", new NetToolItem(), 40.0F, true);
      registerItem("wrench", new WrenchPlaceableItem(), 50.0F, true);
      registerItem("cutter", new CutterPlaceableItem(), 50.0F, true);
      registerItem(
         "wire", new MatItem(1000, Item.Rarity.NORMAL, "wiretip").setItemCategory("wiring").setItemCategory(ItemCategory.craftingManager, "wiring"), 0.1F, true
      );
      registerItem("multitool", new MultiToolItem(1000), 50.0F, false);
      registerItem("eraser", new EraserToolItem(), 50.0F, false, false, true);
      registerItem("pipette", new PipetteItem(), 50.0F, false, false, true);
      registerItem("woodsword", new WoodSwordToolItem(), 10.0F, true);
      registerItem("coppersword", new CopperSwordToolItem(), 25.0F, true);
      registerItem("ironsword", new IronSwordToolItem(), 40.0F, true);
      registerItem("goldsword", new GoldSwordToolItem(), 50.0F, true);
      registerItem("nunchucks", new NunchucksToolItem(), 50.0F, true);
      registerItem("katana", new KatanaToolItem(), 50.0F, true);
      registerItem("heavyhammer", new HeavyHammerProjectileToolItem(), 120.0F, true);
      registerItem("frostsword", new FrostSwordToolItem(), 80.0F, true);
      registerItem("demonicsword", new DemonicSwordToolItem(), 120.0F, true);
      registerItem("lightninghammer", new LightningHammerToolItem(), 150.0F, true);
      registerItem("survivorwhip", new SurvivorWhipToolItem(), 200.0F, true);
      registerItem("galvanichammer", new GalvanicHammerToolItem(), 250.0F, true);
      registerItem("spiderclaw", new SpiderClawSwordToolItem(), 300.0F, true);
      registerItem("ivysword", new IvySwordToolItem(), 110.0F, true);
      registerItem("amethystsword", new AmethystSwordToolItem(), 350.0F, true);
      registerItem("cutlass", new CutlassSwordToolItem(), 500.0F, true);
      registerItem("reinforcedkatana", new ReinforcedKatanaToolItem(), 300.0F, true);
      registerItem("tungstensword", new TungstenSwordToolItem(), 200.0F, true);
      registerItem("reaperscythe", new ReaperScytheProjectileToolItem(), 650.0F, true);
      registerItem("barkblade", new BarkBladeSwordToolItem(), 700.0F, true);
      registerItem("agedchampionsword", new AgedChampionSwordToolItem(), 800.0F, true);
      registerItem("venomslasher", new VenomSlasherToolItem(), 750.0F, true);
      registerItem("sandknife", new SandKnifeToolItem(), 450.0F, true);
      registerItem("antiquesword", new AntiqueSwordSwordToolItem(), 800.0F, true);
      registerItem("bloodclaw", new BloodClawToolItem(), 750.0F, true);
      registerItem("causticexecutioner", new CausticExecutionerToolItem(IncursionGreatswordWeaponsLootTable.incursionGreatswordWeapons), 850.0F, true);
      registerItem("gemstonelongsword", new GemstoneLongswordToolItem(), 1100.0F, true);
      registerItem("perfectstorm", new PerfectStormSwordToolItem(), 450.0F, true);
      registerItem("syndicatebat", new SyndicateBatSwordToolItem(), 0.0F, false);
      registerItem("revsword", new RevSwordToolItem(), 800.0F, true);
      registerItem("irongreatsword", new IronGreatswordToolItem(), 60.0F, true);
      registerItem("frostgreatsword", new FrostGreatswordToolItem(), 120.0F, true);
      registerItem("brutesbattleaxe", new BrutesBattleaxeToolItem(), 350.0F, true);
      registerItem("ivygreatsword", new IvyGreatswordToolItem(), 160.0F, true);
      registerItem("necroticgreatsword", new NecroticGreatswordToolItem(), 500.0F, true);
      registerItem("hexedbladegreatsword", new HexedBladeGreatswordToolItem(), 800.0F, true);
      registerItem("quartzgreatsword", new QuartzGreatswordToolItem(), 220.0F, true);
      registerItem("glacialgreatsword", new GlacialGreatswordToolItem(), 280.0F, true);
      registerItem("dryadgreathammer", new DryadGreatHammerToolItem(), 280.0F, true);
      registerItem("slimegreatsword", new SlimeGreatswordToolItem(), 400.0F, true);
      registerItem("ravenwinggreatsword", new RavenwingGreatswordToolItem(), 450.0F, true);
      registerItem("voidclaw", new VoidClawSwordToolItem(), 2000.0F, true);
      registerItem("woodspear", new WoodSpearToolItem(), 15.0F, true);
      registerItem("copperpitchfork", new CopperPitchforkToolItem(), 60.0F, true);
      registerItem("copperspear", new CopperSpearToolItem(), 45.0F, true);
      registerItem("ironspear", new IronSpearToolItem(), 75.0F, true);
      registerItem("goldspear", new GoldSpearToolItem(), 90.0F, true);
      registerItem("frostspear", new FrostSpearToolItem(), 100.0F, true);
      registerItem("demonicspear", new DemonicSpearToolItem(), 120.0F, true);
      registerItem("voidspear", new VoidSpearToolItem(), 150.0F, true);
      registerItem("ivyspear", new IvySpearToolItem(), 130.0F, true);
      registerItem("vulturestalon", new VulturesTalonToolItem(), 300.0F, true);
      registerItem("tungstenspear", new TungstenSpearToolItem(), 200.0F, true);
      registerItem("cryospear", new CryoSpearToolItem(), 650.0F, true);
      registerItem("ravenbeakspear", new RavenBeakSpearToolItem(), 800.0F, true);
      registerItem("goldglaive", new GoldGlaiveToolItem(), 100.0F, true);
      registerItem("frostglaive", new FrostGlaiveToolItem(), 120.0F, true);
      registerItem("quartzglaive", new QuartzGlaiveToolItem(), 200.0F, true);
      registerItem("cryoglaive", new CryoGlaiveToolItem(), 300.0F, true);
      registerItem("slimeglaive", new SlimeGlaiveToolItem(), 400.0F, true);
      registerItem("woodbow", new WoodBowProjectileToolItem(), 12.0F, true);
      registerItem("copperbow", new CopperBowProjectileToolItem(), 25.0F, true);
      registerItem("ironbow", new IronBowProjectileToolItem(), 40.0F, true);
      registerItem("goldbow", new GoldBowProjectileToolItem(), 60.0F, true);
      registerItem("frostbow", new FrostBowProjectileToolItem(), 80.0F, true);
      registerItem("demonicbow", new DemonicBowProjectileToolItem(), 100.0F, true);
      registerItem("captorsshortbow", new CaptorsShortbowProjectileToolItem(), 350.0F, true);
      registerItem("ivybow", new IvyBowProjectileToolItem(), 110.0F, true);
      registerItem("vulturesburst", new VulturesBurstProjectileToolItem(), 300.0F, true);
      registerItem("tungstenbow", new TungstenBowProjectileToolItem(), 200.0F, true);
      registerItem("glacialbow", new GlacialBowProjectileToolItem(), 200.0F, true);
      registerItem("dryadbow", new DryadBowProjectileToolItem(), 340.0F, true);
      registerItem("bowofdualism", new BowOfDualismProjectileToolItem(), 700.0F, true);
      registerItem("antiquebow", new AntiqueBowProjectileToolItem(), 800.0F, true);
      registerItem("thecrimsonsky", new TheCrimsonSkyProjectileToolItem(), 400.0F, true);
      registerItem("arachnidwebbow", new ArachnidWebBowToolItem(IncursionBowWeaponsLootTable.incursionBowWeapons), 850.0F, true);
      registerItem("ascendedbow", new AscendedBowProjectileToolItem(), 2000.0F, true);
      registerItem("goldgreatbow", new GoldGreatbowProjectileToolItem(), 80.0F, true);
      registerItem("voidgreatbow", new VoidGreatbowProjectileToolItem(), 120.0F, true);
      registerItem("ivygreatbow", new IvyGreatbowProjectileToolItem(), 140.0F, true);
      registerItem("tungstengreatbow", new TungstenGreatbowProjectileToolItem(), 250.0F, true);
      registerItem("myceliumgreatbow", new MyceliumGreatbowProjectileToolItem(), 300.0F, true);
      registerItem("druidsgreatbow", new DruidsGreatBowProjectileToolItem(), 800.0F, true);
      registerItem("slimegreatbow", new SlimeGreatbowProjectileToolItem(), 400.0F, true);
      registerItem("nightpiercer", new NightPiercerGreatBowProjectileToolItem(), 400.0F, true);
      registerItem("theravensnest", new TheRavensNestProjectileToolItem(), 475.0F, true);
      registerItem("stonearrow", new StoneArrowItem(), 0.2F, true);
      registerItem("firearrow", new FireArrowItem(), 0.2F, true);
      registerItem("frostarrow", new FrostArrowItem(), 0.2F, true);
      registerItem("poisonarrow", new PoisonArrowItem(), 0.2F, true);
      registerItem("bouncingarrow", new BouncingArrowItem(), 0.5F, true);
      registerItem("ironarrow", new IronArrowItem(), 0.8F, true);
      registerItem("bonearrow", new BoneArrowItem(), 1.2F, true);
      registerItem("spideritearrow", new SpideriteArrowItem(), 2.0F, true);
      registerItem("handgun", new HandGunProjectileToolItem(), 150.0F, true);
      registerItem("sapphirerevolver", new SapphireRevolverProjectileToolItem(), 350.0F, true);
      registerItem("webbedgun", new WebbedGunProjectileToolItem(), 300.0F, true);
      registerItem("machinegun", new MachineGunProjectileToolItem(), 400.0F, true);
      registerItem("shotgun", new ShotgunProjectileToolItem(), 450.0F, true);
      registerItem("sniperrifle", new SniperProjectileToolItem(), 450.0F, true);
      registerItem("sixshooter", new SixShooterProjectileToolItem(), 450.0F, true);
      registerItem("flintlock", new FlintlockProjectileToolItem(), 500.0F, true);
      registerItem("handcannon", new HandCannonProjectileToolItem(), 500.0F, true);
      registerItem("seedgun", new SeedGunProjectileToolItem(), 600.0F, true);
      registerItem("deathripper", new DeathRipperProjectileToolItem(), 600.0F, true);
      registerItem("cryoblaster", new CryoBlasterProjectileToolItem(), 650.0F, true);
      registerItem("livingshotty", new LivingShottyProjectileToolItem(), 750.0F, true);
      registerItem("antiquerifle", new AntiqueRifleProjectileToolItem(), 800.0F, true);
      registerItem("shardcannon", new ShardCannonProjectileToolItem(), 1500.0F, true);
      registerItem("snowlauncher", new SnowLauncherProjectileToolItem(), 425.0F, true, false);
      registerItem("patchworkgrenadier", new PatchworkGrenadierProjectileToolItem(), 800.0F, true);
      registerItem("simplebullet", new SimpleBulletItem(), 0.1F, true);
      registerItem("frostbullet", new FrostBulletItem(), 0.1F, true);
      registerItem("bouncingbullet", new BouncingBulletItem(), 0.1F, true);
      registerItem("voidbullet", new VoidBulletItem(), 0.1F, true);
      registerItem("cannonball", new CannonballAmmoItem(), 2.0F, true);
      registerItem("crystalbullet", new CrystalBulletItem(), 0.1F, true);
      registerItem("sharktoothbullet", new SharkToothBulletItem(), 0.1F, true);
      registerItem("seedbullet", new SeedBulletItem(), 0.0F, false);
      registerItem("woodstaff", new WoodStaffProjectileToolItem(), 30.0F, true);
      registerItem("sparkler", new SparklerProjectileToolItem(), 40.0F, true);
      registerItem("sprinkler", new SprinklerProjectileToolItem(), 60.0F, true);
      registerItem("bloodbolt", new BloodBoltProjectileToolItem(), 60.0F, true);
      registerItem("venomstaff", new VenomStaffProjectileToolItem(), 100.0F, true);
      registerItem("froststaff", new FrostStaffProjectileToolItem(), 150.0F, true);
      registerItem("frenchhorn", new FrenchHornProjectileToolItem(), 250.0F, true);
      registerItem("sapphirestaff", new SapphireStaffProjectileToolItem(), 150.0F, true);
      registerItem("bloodvolley", new BloodVolleyProjectileToolItem(), 150.0F, true);
      registerItem("voidstaff", new VoidStaffProjectileToolItem(), 200.0F, true);
      registerItem("voidmissile", new VoidMissileProjectileToolItem(), 400.0F, true);
      registerItem("runeboundscepter", new RuneboundScepterProjectileToolItem(), 425.0F, true);
      registerItem("swamptome", new SwampTomeProjectileToolItem(), 500.0F, true);
      registerItem("necroticflask", new NecroticFlaskProjectileToolItem(), 600.0F, true);
      registerItem("unlabeledpotion", new UnlabeledPotionProjectileToolItem(), 600.0F, true);
      registerItem("boulderstaff", new BoulderStaffProjectileToolItem(), 400.0F, true);
      registerItem("dredgingstaff", new DredgingStaffProjectileToolItem(), 500.0F, true);
      registerItem("quartzstaff", new QuartzStaffProjectileToolItem(), 200.0F, true);
      registerItem("amethyststaff", new AmethystStaffProjectileToolItem(), 325.0F, true);
      registerItem("genielamp", new GenieLampProjectileToolItem(), 600.0F, true);
      registerItem("vampiriclamp", new VampiricLampProjectileToolItem(), 600.0F, true);
      registerItem("elderlywand", new ElderlyWandProjectileToolItem(), 600.0F, true);
      registerItem("shadowbolt", new ShadowBoltProjectileToolItem(), 100.0F, true);
      registerItem("chromaticspellbook", new ChromaticSpellbookProjectileToolItem(), 100.0F, true);
      registerItem("rubystaff", new RubyStaffProjectileToolItem(), 380.0F, true);
      registerItem("shadowbeam", new ShadowBeamProjectileToolItem(), 650.0F, true);
      registerItem("iciclestaff", new IcicleStaffProjectileToolItem(), 260.0F, true);
      registerItem("cryoquake", new CryoQuakeProjectileToolItem(), 700.0F, true);
      registerItem("dryadbarrage", new DryadBarrageToolItem(), 340.0F, true);
      registerItem("thesoulstorm", new TheSoulstormProjectileToolItem(), 725.0F, true);
      registerItem("swampdwellerstaff", new SwampDwellerStaffProjectileToolItem(), 800.0F, true);
      registerItem("emeraldstaff", new EmeraldStaffProjectileToolItem(), 420.0F, true);
      registerItem("venomshower", new VenomShowerProjectileToolItem(), 800.0F, true);
      registerItem("ancientdredgingstaff", new AncientDredgingStaffProjectileToolItem(), 600.0F, true);
      registerItem("dragonlance", new DragonLanceProjectileToolItem(), 700.0F, true);
      registerItem("slimestaff", new SlimeStaffProjectileToolItem(), 400.0F, true);
      registerItem("phantompopper", new PhantomPopperProjectileToolItem(), 400.0F, true);
      registerItem("bloodgrimoire", new BloodGrimoireProjectileToolItem(), 700.0F, true);
      registerItem("webweaver", new WebWeaverToolItem(IncursionMagicWeaponsLootTable.incursionMagicWeapons), 1000.0F, true);
      registerItem("emeraldwand", new EmeraldWandProjectileToolItem(), 350.0F, true);
      registerItem("refractor", new RefractorProjectileToolItem(), 1100.0F, true);
      registerItem("topazstaff", new TopazStaffProjectileToolItem(), 350.0F, true);
      registerItem("ascendedstaff", new AscendedStaffToolItem(), 2000.0F, true);
      registerItem("pyromancy", new PyromancyProjectileToolItem(), 800.0F, true);
      registerItem("ninjastar", new NinjaStarToolItem(), 0.5F, true);
      registerItem("icejavelin", new IceJavelinToolItem(), 1.0F, true);
      registerItem("woodboomerang", new WoodBoomerangToolItem(), 20.0F, true);
      registerItem("spiderboomerang", new SpiderBoomerangToolItem(), 80.0F, true);
      registerItem("boxingglovegun", new BoxingGloveGunToolItem(), 150.0F, true);
      registerItem("frostboomerang", new FrostBoomerangToolItem(), 90.0F, true);
      registerItem("hook", new HookBoomerangToolItem(), 200.0F, false);
      registerItem("rollingpin", new RollingPinBoomerangToolItem(), 100.0F, true);
      registerItem("voidboomerang", new VoidBoomerangToolItem(), 100.0F, true);
      registerItem("razorbladeboomerang", new RazorBladeBoomerangToolItem(), 150.0F, true);
      registerItem("tungstenboomerang", new TungstenBoomerangToolItem(), 60.0F, true);
      registerItem("glacialboomerang", new GlacialBoomerangToolItem(), 180.0F, true);
      registerItem("butcherscleaver", new ButchersCleaverBoomerangToolItem(), 600.0F, true);
      registerItem("chefsspecial", new ChefsSpecialBoomerangToolItem(), 800.0F, true);
      registerItem("anchorandchain", new AnchorAndChainToolItem(), 800.0F, true);
      registerItem("carapacedagger", new CarapaceDaggerToolItem(), 220.0F, true);
      registerItem("dragonsrebound", new DragonsReboundToolItem(), 600.0F, true);
      registerItem("nightrazorboomerang", new NightRazorBoomerangToolItem(), 150.0F, true);
      registerItem("snowball", new SnowBallToolItem(), 0.1F, true);
      registerItem("brainonastick", new BrainOnAStickToolItem(), 400.0F, true);
      registerItem("spiderstaff", new SpiderStaffSummonToolItem(), 120.0F, true);
      registerItem("magicbranch", new MagicBranchSummonToolItem(), 250.0F, true);
      registerItem("batcage", new BatCageSummonToolItem(), 260.0F, true);
      registerItem("hivescepter", new HiveScepterSummonToolItem(), 300.0F, true);
      registerItem("frostpiercer", new FrostPiercerSummonToolItem(), 200.0F, true);
      registerItem("sentientsword", new SentientSwordSummonToolItem(), 250.0F, true);
      registerItem("slimecanister", new SlimeCanisterSummonToolItem(), 400.0F, true);
      registerItem("necroticbow", new NecroticBowProjectileToolItem(), 500.0F, true);
      registerItem("reanimationbow", new ReanimationBowProjectileToolItem(), 900.0F, true);
      registerItem("stabbybush", new StabbyBushSummonToolItem(), 400.0F, true);
      registerItem("bashybush", new BashyBushSummonToolItem(), 550.0F, true);
      registerItem("vulturestaff", new VultureStaffSummonToolItem(), 400.0F, true);
      registerItem("piratehook", new PirateHookSummonToolItem(), 500.0F, false);
      registerItem("rubyshields", new RubyShieldsToolItem(), 350.0F, true);
      registerItem("reaperscall", new ReapersCallSummonToolItem(), 700.0F, true);
      registerItem("cryostaff", new CryoStaffSummonToolItem(), 350.0F, true);
      registerItem("dryadbranch", new DryadBranchSummonToolItem(), 280.0F, true);
      registerItem("swampsgrasp", new SwampsGraspToolItem(), 800.0F, true);
      registerItem("grizzboltsphere", new GrizzboltSphereSummonToolItem(), 1000.0F, true);
      registerItem("skeletonstaff", new SkeletonStaffToolItem(), 750.0F, true);
      registerItem("orbofslimes", new OrbOfSlimesToolItem(), 350.0F, true);
      registerItem("phantomcaller", new PhantomCallerSummonToolItem(), 400.0F, true);
      registerItem("empresscommand", new EmpressCommandToolItem(), 750.0F, true);
      registerItem("crystallizedskull", new CrystallizedSkullSummonToolItem(), 1100.0F, true);
      registerItem("eyeofthevoid", new EyeOfTheVoidSummonToolItem(), 2000.0F, true);
      registerItem("ignitionkey", new IgnitionKeySummonToolItem(), 800.0F, true);
      registerItem("ironbomb", new IronBombToolItem(), 20.0F, true);
      registerItem("dynamitestick", new DynamiteStickToolItem(), 60.0F, true);
      registerItem("tilebomb", new TileBombToolItem(), 20.0F, true);
      registerItem("bannerofspeed", new BannerItem(Item.Rarity.COMMON, 480, m -> BuffRegistry.Banners.SPEED), 200.0F, true);
      registerItem("bannerofdamage", new BannerItem(Item.Rarity.COMMON, 480, m -> BuffRegistry.Banners.DAMAGE), 200.0F, true);
      registerItem("bannerofsummonspeed", new BannerItem(Item.Rarity.COMMON, 480, m -> BuffRegistry.Banners.SUMMON_SPEED), 200.0F, true);
      registerItem("bannerofdefense", new BannerItem(Item.Rarity.COMMON, 480, m -> BuffRegistry.Banners.DEFENSE), 200.0F, true);
      registerItem("mlg1", new MLG1SwordToolItem(), 1000.0F, false);
      registerItem("mlg2", new MLG2SwordToolItem(), 1000.0F, false);
      registerItem("mousetest", new MouseTestProjectileToolItem(), 0.0F, false);
      registerItem("mousebeam", new MouseBeamProjectileToolItem(), 0.0F, false);
      registerItem("woodgreatsword", new WoodGreatswordToolItem(), 0.0F, false);
      registerItem("goldencausticexecutioner", new GoldenCausticExecutionerToolItem(), 0.0F, false);
      registerItem("goldenarachnidwebbow", new GoldenArachnidWebBowToolItem(), 0.0F, false);
      registerItem("goldenwebweaver", new GoldenWebWeaverToolItem(), 0.0F, false);
      registerItem("flamelingorb", new FlamelingOrbProjectileToolItem(), 0.0F, false);
      registerItem("fishianwarriorhook", new FishianWarriorHookSwordToolItem(), 0.0F, false);
      registerItem("fishianhealerstaff", new FishianHealerStaffToolItem(), 0.0F, false);
      registerItem("ancestorsword", new AncestorSwordToolItem(), 0.0F, false);
      registerItem("ancestorwand", new AncestorWandToolItem(), 0.0F, false);
      registerItem("chargebeam", new ChargeBeamProjectileToolItem(), 0.0F, false);
      registerItem("chargeshower", new ChargeShowerProjectileToolItem(), 0.0F, false);
      registerItem("invisiblesword", new InvisibleSwordToolItem(), 0.0F, false);
      registerItem("kineticcharger", new KineticChargerToolItem(), 0.0F, false);
      registerItem("stopsignsword", new StopSignSwordToolItem(), 0.0F, false);
      registerItem("steelboat", new SteelBoatMountItem(), 500.0F, true);
      registerItem("runeboundboat", new RuneboundBoatMountItem(), 500.0F, false);
      registerItem("inefficientfeather", new MountItem("tameostrich"), 100.0F, true);
      registerItem("callofthesea", new SeahorseMountItem(), 1000.0F, true);
      registerItem("jumpingball", new JumpingBallMountItem(), 400.0F, true);
      registerItem("hoverboard", new HoverBoardMountItem(), 800.0F, true);
      registerItem("witchbroom", new WitchBroomMountItem(), 1000.0F, true);
      registerItem("ringofascension", new RingOfAscensionMountItem(), 1500.0F, true);
      registerItem("bumpercar", new BumperCarMountItem(), 300.0F, true);
      registerItem("leatherglove", new SimpleTrinketItem(Item.Rarity.COMMON, "leatherglovetrinket", 200, TrinketsLootTable.trinkets), 50.0F, true);
      registerItem("trackerboot", new SimpleTrinketItem(Item.Rarity.COMMON, "trackerboottrinket", 200, TrinketsLootTable.trinkets), 60.0F, true);
      registerItem("shinebelt", new SimpleTrinketItem(Item.Rarity.COMMON, "shinebelttrinket", 200, TrinketsLootTable.trinkets), 100.0F, true);
      registerItem("dreamcatcher", new SimpleTrinketItem(Item.Rarity.COMMON, "dreamcatchertrinket", 200, TrinketsLootTable.trinkets), 50.0F, true);
      registerItem(
         "nightmaretalisman",
         new SimpleTrinketItem(Item.Rarity.COMMON, "nightmaretalismantrinket", 500, TrinketsLootTable.trinkets).addDisables("dreamcatcher"),
         100.0F,
         true
      );
      registerItem("prophecyslab", new SimpleTrinketItem(Item.Rarity.COMMON, "prophecyslabtrinket", 200, TrinketsLootTable.trinkets), 100.0F, true);
      registerItem("magicmanual", new SimpleTrinketItem(Item.Rarity.COMMON, "magicmanualtrinket", 200, TrinketsLootTable.trinkets), 100.0F, true);
      registerItem(
         "scryingcards",
         new SimpleTrinketItem(Item.Rarity.COMMON, "scryingcardstrinket", 200, TrinketsLootTable.trinkets).addDisables("magicmanual", "prophecyslab"),
         100.0F,
         true
      );
      registerItem("forbiddenspellbook", new SimpleTrinketItem(Item.Rarity.COMMON, "forbiddenspellbooktrinket", 200, TrinketsLootTable.trinkets), 100.0F, true);
      registerItem("explorersatchel", new CombinedTrinketItem(Item.Rarity.UNCOMMON, 500, "leatherglove", "trackerboot", "shinebelt"), 200.0F, true);
      registerItem("vampiresgift", new SimpleTrinketItem(Item.Rarity.COMMON, "vampiresgifttrinket", 300, TrinketsLootTable.trinkets), 150.0F, true);
      registerItem("leatherdashers", new LeatherDashersTrinketItem(), 50.0F, true);
      registerItem("zephyrcharm", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "zephyrcharmtrinket", 200, TrinketsLootTable.trinkets), 50.0F, true);
      registerItem("zephyrboots", new ZephyrBootsTrinketItem().addDisables("leatherdashers"), 200.0F, true);
      registerItem("windboots", new WindBootsTrinketItem().addDisables("leatherdashers", "zephyrboots"), 300.0F, true);
      registerItem("hoverboots", new HoverBootsTrinketItem().addDisables("leatherdashers", "zephyrboots", "windboots"), 400.0F, true);
      registerItem(
         "ghostboots", new GhostBootsTrinketItem().addDisables("leatherdashers", "zephyrboots", "hoverboots", "hoverboots", "windboots"), 500.0F, true
      );
      registerItem("kineticboots", new KineticBootsTrinketItem().addDisables("zephyrboots", "leatherdashers"), 1000.0F, true);
      registerItem("forceofwind", new ForceOfWindTrinketItem(), 200.0F, true);
      registerItem("woodshield", new WoodShieldTrinketItem(Item.Rarity.COMMON, 2, 0.5F, 10000, 0.25F, 60, 360.0F, 200), 100.0F, true);
      registerItem("hardenedshield", new ShieldTrinketItem(Item.Rarity.COMMON, 4, 0.5F, 9000, 0.25F, 50, 360.0F, 400, TrinketsLootTable.trinkets), 200.0F, true);
      registerItem("cactusshield", new CactusShieldTrinketItem(Item.Rarity.UNCOMMON, 400), 300.0F, true);
      registerItem("shellofretribution", new CactusShieldTrinketItem(Item.Rarity.RARE, 600).addCombinedTrinkets("spidercharm"), 600.0F, true);
      registerItem(
         "tungstenshield", new ShieldTrinketItem(Item.Rarity.UNCOMMON, 6, 0.5F, 7000, 0.25F, 30, 360.0F, 800, TrinketsLootTable.trinkets), 250.0F, true
      );
      registerItem("parrybuckler", new ParryBucklerShieldTrinketItem(Item.Rarity.RARE, 900), 500.0F, true);
      registerItem(
         "agedchampionshield", new ShieldTrinketItem(Item.Rarity.RARE, 9, 0.5F, 2000, 100.0F, 0, 360.0F, 900, TrinketsLootTable.trinkets), 600.0F, true
      );
      registerItem("siphonshield", new SiphonShieldTrinketItem(Item.Rarity.RARE, 900), 500.0F, true);
      registerItem("carapaceshield", new ShieldTrinketItem(Item.Rarity.RARE, 12, 1.0F, 5000, 0.1F, 10, 360.0F, 1000, TrinketsLootTable.trinkets), 500.0F, true);
      registerItem("crystalshield", new CrystalShieldTrinketItem(Item.Rarity.RARE, 900), 700.0F, true);
      registerItem("moonshield", new MoonShieldTrinketItem(Item.Rarity.EPIC, 500), 500.0F, true);
      registerItem("sparegemstones", new SimpleTrinketItem(Item.Rarity.COMMON, "sparegemstonestrinket", 200, TrinketsLootTable.trinkets), 100.0F, true);
      registerItem(
         "spellstone",
         new SimpleTrinketItem(Item.Rarity.UNCOMMON, "spellstonetrinket", 200, TrinketsLootTable.trinkets).addDisables("sparegemstones"),
         300.0F,
         true
      );
      registerItem("companionlocket", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "companionlockettrinket", 200, TrinketsLootTable.trinkets), 200.0F, true);
      registerItem("willowisplantern", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "willowisplanterntrinket", 200, TrinketsLootTable.trinkets), 200.0F, true);
      registerItem(
         "essenceofperspective", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "essenceofperspectivetrinket", 200, TrinketsLootTable.trinkets), 200.0F, true
      );
      registerItem(
         "essenceofprolonging", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "essenceofprolongingtrinket", 200, TrinketsLootTable.trinkets), 200.0F, true
      );
      registerItem(
         "essenceofrebirth",
         new SimpleTrinketItem(Item.Rarity.UNCOMMON, "essenceofrebirthtrinket", 200, TrinketsLootTable.trinkets)
            .addDisables("essenceofperspectivetrinket", "essenceofprolongingtrinket"),
         200.0F,
         true
      );
      registerItem("blinkscepter", new BlinkScepterTrinketItem(), 800.0F, true);
      registerItem(
         "magicfoci",
         new SimpleTrinketItem(Item.Rarity.COMMON, "magicfocitrinket", 500, TrinketsLootTable.trinkets).addDisables("rangefoci", "meleefoci", "summonfoci"),
         200.0F,
         true
      );
      registerItem(
         "rangefoci",
         new SimpleTrinketItem(Item.Rarity.COMMON, "rangefocitrinket", 500, TrinketsLootTable.trinkets).addDisables("magicfoci", "meleefoci", "summonfoci"),
         200.0F,
         true
      );
      registerItem(
         "meleefoci",
         new SimpleTrinketItem(Item.Rarity.COMMON, "meleefocitrinket", 500, TrinketsLootTable.trinkets).addDisables("magicfoci", "rangefoci", "summonfoci"),
         200.0F,
         true
      );
      registerItem(
         "summonfoci",
         new SimpleTrinketItem(Item.Rarity.COMMON, "summonfocitrinket", 500, TrinketsLootTable.trinkets).addDisables("magicfoci", "rangefoci", "meleefoci"),
         200.0F,
         true
      );
      registerItem(
         "balancedfoci",
         new SimpleTrinketItem(Item.Rarity.UNCOMMON, "balancedfocitrinket", 600, TrinketsLootTable.trinkets)
            .addDisables("magicfoci", "rangefoci", "meleefoci", "summonfoci"),
         600.0F,
         true
      );
      registerItem("demonclaw", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "demonclawtrinket", 500, TrinketsLootTable.trinkets), 40.0F, true);
      registerItem("fins", new SimpleTrinketItem(Item.Rarity.COMMON, "finstrinket", 200, TrinketsLootTable.trinkets), 150.0F, true);
      registerItem("fuzzydice", new SimpleTrinketItem(Item.Rarity.COMMON, "fuzzydicetrinket", 200, TrinketsLootTable.trinkets), 100.0F, true);
      registerItem("noblehorseshoe", new SimpleTrinketItem(Item.Rarity.COMMON, "noblehorseshoetrinket", 250, TrinketsLootTable.trinkets), 150.0F, true);
      registerItem(
         "luckycape",
         new SimpleTrinketItem(Item.Rarity.UNCOMMON, "luckycapetrinket", 350, TrinketsLootTable.trinkets).addDisables("fuzzydice", "noblehorseshoe"),
         200.0F,
         true
      );
      registerItem("miningcharm", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "miningcharmtrinket", 300, TrinketsLootTable.trinkets), 250.0F, true);
      registerItem("ancientfeather", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "ancientfeathertrinket", 500, TrinketsLootTable.trinkets), 300.0F, true);
      registerItem("assassinscowl", new CombinedTrinketItem(Item.Rarity.RARE, 600, "luckycape", "ancientfeather"), 400.0F, true);
      registerItem("airvessel", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "airvesseltrinket", 500, TrinketsLootTable.trinkets), 300.0F, true);
      registerItem("regenpendant", new SimpleTrinketItem(Item.Rarity.COMMON, "regenpendanttrinket", 400, TrinketsLootTable.trinkets), 75.0F, true);
      registerItem("calmingrose", new SimpleTrinketItem(Item.Rarity.RARE, "calmingrosetrinket", 400, TrinketsLootTable.trinkets), 300.0F, true);
      registerItem("calmingminersbouquet", new CalmingMinersBouquetTrinketItem().addDisables("miningcharm", "calmingrose"), 500.0F, true);
      registerItem(
         "minersbouquet", new MinersBouquetTrinketItem().addDisables("miningcharm", "calmingrose").addDisabledBy("calmingminersbouquet"), 500.0F, false
      );
      registerItem("mobilitycloak", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "mobilitycloaktrinket", 400, TrinketsLootTable.trinkets), 200.0F, true);
      registerItem(
         "travelercloak",
         new SimpleTrinketItem(Item.Rarity.UNCOMMON, "travelercloaktrinket", 600, TrinketsLootTable.trinkets).addDisables("mobilitycloak", "fins"),
         320.0F,
         true
      );
      registerItem("mesmertablet", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "mesmertablettrinket", 600, TrinketsLootTable.trinkets), 200.0F, true);
      registerItem("magicalquiver", new SimpleTrinketItem(Item.Rarity.RARE, "magicalquivertrinket", 500, TrinketsLootTable.trinkets), 500.0F, true);
      registerItem("ammobox", new SimpleTrinketItem(Item.Rarity.RARE, "ammoboxtrinket", 700, TrinketsLootTable.trinkets), 700.0F, true);
      registerItem("frozenheart", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "frozenhearttrinket", 500, TrinketsLootTable.trinkets), 300.0F, true);
      registerItem("frozenwave", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "frozenwavetrinket", 500, TrinketsLootTable.trinkets), 300.0F, true);
      registerItem("spidercharm", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "spidercharmtrinket", 600, TrinketsLootTable.trinkets), 400.0F, true);
      registerItem("guardianshell", new SimpleTrinketItem(Item.Rarity.RARE, "guardianshelltrinket", 700, TrinketsLootTable.trinkets), 500.0F, true);
      registerItem("inducingamulet", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "inducingamulettrinket", 700, TrinketsLootTable.trinkets), 300.0F, true);
      registerItem("polarclaw", new SimpleTrinketItem(Item.Rarity.RARE, "polarclawtrinket", 800, TrinketsLootTable.trinkets), 200.0F, true);
      registerItem("piratetelescope", new SimpleTrinketItem(Item.Rarity.RARE, "piratetelescopetrinket", 800, TrinketsLootTable.trinkets), 500.0F, true);
      registerItem("lifeline", new SimpleTrinketItem(Item.Rarity.RARE, "lifelinettrinket", 800, TrinketsLootTable.trinkets), 350.0F, true);
      registerItem(
         "explorercloak",
         new SimpleTrinketItem(Item.Rarity.RARE, "explorercloaktrinket", 800, TrinketsLootTable.trinkets).addDisables("travelercloak", "mobilitycloak", "fins"),
         700.0F,
         true
      );
      registerItem("frozensoul", new CombinedTrinketItem(Item.Rarity.RARE, 800, "lifeline", "frozenheart"), 600.0F, true);
      registerItem("ninjasmark", new SimpleTrinketItem(Item.Rarity.RARE, "ninjasmarktrinket", 800, TrinketsLootTable.trinkets), 350.0F, true);
      registerItem("bonehilt", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "bonehilttrinket", 500, TrinketsLootTable.trinkets), 160.0F, true);
      registerItem("firestone", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "firestonetrinket", 600, TrinketsLootTable.trinkets), 400.0F, true);
      registerItem(
         "lifependant",
         new SimpleTrinketItem(Item.Rarity.UNCOMMON, "lifependanttrinket", 600, TrinketsLootTable.trinkets).addDisables("regenpendant"),
         200.0F,
         true
      );
      registerItem("spikedboots", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "spikedbootstrinket", 600, TrinketsLootTable.trinkets), 350.0F, true);
      registerItem(
         "spikedbatboots",
         new SimpleTrinketItem(Item.Rarity.RARE, "spikedbatbootstrinket", 1000, TrinketsLootTable.trinkets).addDisables("spikedboots", "vampiresgift"),
         450.0F,
         true
      );
      registerItem("froststone", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "froststonetrinket", 600, TrinketsLootTable.trinkets), 400.0F, true);
      registerItem(
         "frostflame",
         new SimpleTrinketItem(Item.Rarity.RARE, "frostflametrinket", 1000, TrinketsLootTable.trinkets).addDisables("froststone", "firestone"),
         600.0F,
         true
      );
      registerItem("balancedfrostfirefoci", new CombinedTrinketItem(Item.Rarity.EPIC, 1200, "balancedfoci", "frostflame"), 900.0F, true);
      registerItem(
         "hysteriatablet",
         new SimpleTrinketItem(Item.Rarity.RARE, "hysteriatablettrinket", 800, TrinketsLootTable.trinkets).addDisables("mesmertablet", "inducingamulet"),
         400.0F,
         true
      );
      registerItem("frenzyorb", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "frenzyorbtrinket", 1000, TrinketsLootTable.trinkets), 200.0F, true);
      registerItem("scryingmirror", new SimpleTrinketItem(Item.Rarity.RARE, "scryingmirrortrinket", 1100, TrinketsLootTable.trinkets), 600.0F, true);
      registerItem("diggingclaw", new SimpleTrinketItem(Item.Rarity.RARE, "diggingclawtrinket", 1000, TrinketsLootTable.trinkets), 600.0F, true);
      registerItem("templependant", new SimpleTrinketItem(Item.Rarity.RARE, "templependanttrinket", 1000, TrinketsLootTable.trinkets), 700.0F, true);
      registerItem(
         "ancientrelics",
         new SimpleTrinketItem(Item.Rarity.RARE, "ancientrelicstrinket", 1200, TrinketsLootTable.trinkets).addDisables("airvessel", "templependant"),
         800.0F,
         true
      );
      registerItem(
         "gelatincasings", new SimpleTrinketItem(Item.Rarity.EPIC, "gelatincasingstrinket", 1000, IncursionTrinketsLootTable.incursionTrinkets), 250.0F, true
      );
      registerItem(
         "bloodstonering", new SimpleTrinketItem(Item.Rarity.EPIC, "bloodstoneringtrinket", 1000, IncursionTrinketsLootTable.incursionTrinkets), 250.0F, true
      );
      registerItem("claygauntlet", new SimpleTrinketItem(Item.Rarity.COMMON, "claygauntlettrinket", 300, TrinketsLootTable.trinkets), 100.0F, true);
      registerItem("chainshirt", new SimpleTrinketItem(Item.Rarity.COMMON, "chainshirttrinket", 300, TrinketsLootTable.trinkets), 100.0F, true);
      registerItem("vambrace", new SimpleTrinketItem(Item.Rarity.RARE, "vambracetrinket", 300, TrinketsLootTable.trinkets), 100.0F, true);
      registerItem(
         "manica",
         new SimpleTrinketItem(Item.Rarity.EPIC, "manicatrinket", 800, TrinketsLootTable.trinkets).addDisables("vambrace", "chainshirt"),
         200.0F,
         true
      );
      registerItem(
         "agedchampionscabbard", new SimpleTrinketItem(Item.Rarity.RARE, "agedchampionscabbardtrinket", 600, TrinketsLootTable.trinkets), 350.0F, true
      );
      registerItem("challengerspauldron", new SimpleTrinketItem(Item.Rarity.RARE, "challengerspauldrontrinket", 800, TrinketsLootTable.trinkets), 350.0F, true);
      registerItem("clockworkheart", new SimpleTrinketItem(Item.Rarity.EPIC, "clockworkhearttrinket", 1000, TrinketsLootTable.trinkets), 800.0F, true);
      registerItem("foolsgambit", new FoolsGambitTrinketItem(Item.Rarity.EPIC, "foolsgambittrinket", 1100), 850.0F, true);
      registerItem("necroticsoulskull", new SimpleTrinketItem(Item.Rarity.EPIC, "necroticsoulskull", 1100, TrinketsLootTable.trinkets), 600.0F, true);
      registerItem("spiritboard", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "spiritboardtrinket", 600, TrinketsLootTable.trinkets), 450.0F, true);
      registerItem("secondwindcharm", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "secondwindcharmtrinket", 600, TrinketsLootTable.trinkets), 450.0F, true);
      registerItem("guardianbracelet", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "guardianbracelettrinket", 600, TrinketsLootTable.trinkets), 450.0F, true);
      registerItem("summonersbestiary", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "summonersbestiarytrinket", 600, TrinketsLootTable.trinkets), 450.0F, true);
      registerItem("spiritgreaves", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "spiritgreavestrinket", 600, TrinketsLootTable.trinkets), 450.0F, true);
      registerItem(
         "constructionhammer", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "constructionhammertrinket", 400, TrinketsLootTable.trinkets), 200.0F, true
      );
      registerItem("telescopicladder", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "telescopicladdertrinket", 400, TrinketsLootTable.trinkets), 200.0F, true);
      registerItem("toolextender", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "toolextendertrinket", 400, TrinketsLootTable.trinkets), 200.0F, true);
      registerItem("itemattractor", new SimpleTrinketItem(Item.Rarity.UNCOMMON, "itemattractortrinket", 400, TrinketsLootTable.trinkets), 200.0F, true);
      registerItem(
         "toolbox", new CombinedTrinketItem(Item.Rarity.RARE, 800, "constructionhammer", "telescopicladder", "toolextender", "itemattractor"), 600.0F, true
      );
      registerItem("minersprosthetic", new SimpleTrinketItem(Item.Rarity.RARE, "minersprosthetictrinket", 800, TrinketsLootTable.trinkets), 500.0F, true);
      registerItem("cavelingsfoot", new SimpleTrinketItem(Item.Rarity.RARE, "cavelingsfoottrinket", 300, TrinketsLootTable.trinkets), 200.0F, true);
      registerItem("cavelingscollection", new SimpleTrinketItem(Item.Rarity.RARE, "cavelingscollectiontrinket", 600, TrinketsLootTable.trinkets), 300.0F, true);
      registerItem("voidphasingstaff", new VoidPhasingStaffTrinketItem(), 1500.0F, true);
      registerItem("jonasgambit", new JonasGambitTrinketItem(Item.Rarity.EPIC, "jonasgambittrinket", 1400), 950.0F, true);
      registerItem("leatherhood", new LeatherHoodArmorItem(), 25.0F, true);
      registerItem("leathershirt", new LeatherShirtArmorItem(), 55.0F, true);
      registerItem("leatherboots", new LeatherBootsArmorItem(), 10.0F, true);
      registerItem("clothhat", new ClothHatArmorItem(), 25.0F, true);
      registerItem("clothrobe", new ClothRobeArmorItem(), 55.0F, true);
      registerItem("clothboots", new ClothBootsArmorItem(), 10.0F, true);
      registerItem("copperhelmet", new CopperHelmetArmorItem(), 15.0F, true);
      registerItem("copperchestplate", new CopperChestplateArmorItem(), 30.0F, true);
      registerItem("copperboots", new CopperBootsArmorItem(), 5.0F, true);
      registerItem("ironhelmet", new IronHelmetArmorItem(), 20.0F, true);
      registerItem("ironchestplate", new IronChestplateArmorItem(), 45.0F, true);
      registerItem("ironboots", new IronBootsArmorItem(), 8.0F, true);
      registerItem("goldcrown", new GoldCrownArmorItem(), 50.0F, true);
      registerItem("goldhelmet", new GoldHelmetArmorItem(), 50.0F, true);
      registerItem("goldchestplate", new GoldChestplateArmorItem(), 80.0F, true);
      registerItem("goldboots", new GoldBootsArmorItem(), 30.0F, true);
      registerItem("soldiercap", new SoldierCapArmorItem(), 60.0F, true);
      registerItem("soldierhelmet", new SoldierHelmetArmorItem(), 60.0F, true);
      registerItem("soldierchestplate", new SoldierChestplateArmorItem(), 100.0F, true);
      registerItem("soldierboots", new SoldierBootsArmorItem(), 20.0F, true);
      registerItem("spiderhelmet", new SpiderHelmetArmorItem(), 50.0F, true);
      registerItem("spiderchestplate", new SpiderChestplateArmorItem(), 75.0F, true);
      registerItem("spiderboots", new SpiderBootsArmorItem(), 40.0F, true);
      registerItem("bloodplatecowl", new BloodplateCowlArmorItem(), 55.0F, true);
      registerItem("bloodplatechestplate", new BloodplateChestplateArmorItem(), 65.0F, true);
      registerItem("bloodplateboots", new BloodplateBootsArmorItem(), 50.0F, true);
      registerItem("sharkarmorhelmet", new SharkHelmetArmorItem(), 55.0F, true);
      registerItem("sharkarmorchestplate", new SharkChestplateArmorItem(), 80.0F, true);
      registerItem("sharkarmorboots", new SharkBootsArmorItem(), 60.0F, true);
      registerItem("frosthelmet", new FrostHelmetArmorItem(), 60.0F, true);
      registerItem("frosthood", new FrostHoodArmorItem(), 60.0F, true);
      registerItem("frosthat", new FrostHatArmorItem(), 60.0F, true);
      registerItem("frostchestplate", new FrostChestplateArmorItem(), 80.0F, true);
      registerItem("frostboots", new FrostBootsArmorItem(), 40.0F, true);
      registerItem("demonichelmet", new DemonicHelmetArmorItem(), 45.0F, true);
      registerItem("demonicchestplate", new DemonicChestplateArmorItem(), 80.0F, true);
      registerItem("demonicboots", new DemonicBootsArmorItem(), 20.0F, true);
      registerItem("arachnidhelmet", new ArachnidHelmetArmorItem(), 70.0F, true);
      registerItem("arachnidchestplate", new ArachnidChestplateArmorItem(), 130.0F, true);
      registerItem("arachnidlegs", new ArachnidLegsArmorItem(), 70.0F, true);
      registerItem("voidmask", new VoidMaskArmorItem(), 100.0F, true);
      registerItem("voidhat", new VoidHatArmorItem(), 100.0F, true);
      registerItem("voidrobe", new VoidRobeArmorItem(), 140.0F, true);
      registerItem("voidboots", new VoidBootsArmorItem(), 60.0F, true);
      registerItem("thiefscowl", new ThiefsCowlArmorItem(), 100.0F, true);
      registerItem("thiefscloak", new ThiefsCloakArmorItem(), 200.0F, true);
      registerItem("thiefsboots", new ThiefsBootsArmorItem(), 100.0F, true);
      registerItem("runichelmet", new RunicHelmetArmorItem(), 55.0F, true);
      registerItem("runichood", new RunicHoodArmorItem(), 55.0F, true);
      registerItem("runichat", new RunicHatArmorItem(), 55.0F, true);
      registerItem("runiccrown", new RunicCrownArmorItem(), 55.0F, true);
      registerItem("runicchestplate", new RunicChestplateArmorItem(), 65.0F, true);
      registerItem("runicboots", new RunicBootsArmorItem(), 50.0F, true);
      registerItem("pharaohsheaddress", new PharaohsHeaddress(), 100.0F, true);
      registerItem("pharaohsrobe", new PharaohsRobeArmorItem(), 120.0F, true);
      registerItem("pharaohssandals", new PharaohsSandalsArmorItem(), 60.0F, true);
      registerItem("witchhat", new WitchHatArmorItem(), 55.0F, true);
      registerItem("witchrobe", new WitchRobeArmorItem(), 65.0F, true);
      registerItem("witchshoes", new WitchShoesArmorItem(), 50.0F, true);
      registerItem("ivyhelmet", new IvyHelmetArmorItem(), 75.0F, true);
      registerItem("ivyhood", new IvyHoodArmorItem(), 75.0F, true);
      registerItem("ivyhat", new IvyHatArmorItem(), 75.0F, true);
      registerItem("ivycirclet", new IvyCircletArmorItem(), 75.0F, true);
      registerItem("ivychestplate", new IvyChestplateArmorItem(), 125.0F, true);
      registerItem("ivyboots", new IvyBootsArmorItem(), 50.0F, true);
      registerItem("quartzhelmet", new QuartzHelmetArmorItem(), 100.0F, true);
      registerItem("quartzcrown", new QuartzCrownArmorItem(), 100.0F, true);
      registerItem("quartzchestplate", new QuartzChestplateArmorItem(), 150.0F, true);
      registerItem("quartzboots", new QuartzBootsArmorItem(), 50.0F, true);
      registerItem("gunslingerhat", new GunslingerHatArmorItem(), 100.0F, true);
      registerItem("gunslingervest", new GunslingerVestArmorItem(), 150.0F, true);
      registerItem("gunslingerboots", new GunslingerBootsArmorItem(), 50.0F, true);
      registerItem("tungstenhelmet", new TungstenHelmetArmorItem(), 110.0F, true);
      registerItem("tungstenchestplate", new TungstenChestplateArmorItem(), 160.0F, true);
      registerItem("tungstenboots", new TungstenBootsArmorItem(), 80.0F, true);
      registerItem("shadowhat", new ShadowHatArmorItem(), 110.0F, true);
      registerItem("shadowhood", new ShadowHoodArmorItem(), 110.0F, true);
      registerItem("shadowmantle", new ShadowMantleArmorItem(), 160.0F, true);
      registerItem("shadowboots", new ShadowBootsArmorItem(), 80.0F, true);
      registerItem("ninjahood", new NinjaHoodArmorItem(), 120.0F, true);
      registerItem("ninjarobe", new NinjaRobeArmorItem(), 200.0F, true);
      registerItem("ninjashoes", new NinjaShoesArmorItem(), 80.0F, true);
      registerItem("soulseedcrown", new SoulseedCrownArmorItem(), 200.0F, true);
      registerItem("soulseedchestplate", new SoulseedChestplateArmorItem(), 165.0F, true);
      registerItem("soulseedboots", new SoulseedBootsArmorItem(), 190.0F, true);
      registerItem("glacialcirclet", new GlacialCircletArmorItem(), 110.0F, true);
      registerItem("glacialhelmet", new GlacialHelmetArmorItem(), 110.0F, true);
      registerItem("glacialchestplate", new GlacialChestplateArmorItem(), 160.0F, true);
      registerItem("glacialboots", new GlacialBootsArmorItem(), 80.0F, true);
      registerItem("deepfrosthood", new DeepfrostHelmetArmorItem(), 180.0F, true);
      registerItem("deepfrostchestplate", new DeepfrostChestplateArmorItem(), 190.0F, true);
      registerItem("deepfrostboots", new DeepfrostBootsArmorItem(), 160.0F, true);
      registerItem("cryowitchhat", new CryoWitchHatArmorItem(), 310.0F, true);
      registerItem("cryowitchrobe", new CryoWitchRobeArmorItem(), 230.0F, true);
      registerItem("cryowitchshoes", new CryoWitchShoesArmorItem(), 180.0F, true);
      registerItem("dryadcrown", new DryadCrownArmorItem(), 240.0F, true);
      registerItem("dryadscarf", new DryadScarfArmorItem(), 240.0F, true);
      registerItem("dryadhat", new DryadHatArmorItem(), 240.0F, true);
      registerItem("dryadhelmet", new DryadHelmetArmorItem(), 240.0F, true);
      registerItem("dryadchestplate", new DryadChestplateArmorItem(), 330.0F, true);
      registerItem("dryadboots", new DryadBootsArmorItem(), 200.0F, true);
      registerItem("myceliumhood", new MyceliumHoodArmorItem(), 110.0F, true);
      registerItem("myceliumscarf", new MyceliumScarfArmorItem(), 110.0F, true);
      registerItem("myceliumchestplate", new MyceliumChestplateArmorItem(), 160.0F, true);
      registerItem("myceliumboots", new MyceliumBootsArmorItem(), 80.0F, true);
      registerItem("agedchampionhelmet", new AgedChampionHelmetArmorItem(), 120.0F, true);
      registerItem("agedchampionchestplate", new AgedChampionChestplateArmorItem(), 160.0F, true);
      registerItem("agedchampiongreaves", new AgedChampionGreavesArmorItem(), 80.0F, true);
      registerItem("widowhelmet", new WidowHelmetArmorItem(), 120.0F, true);
      registerItem("widowchestplate", new WidowChestplateArmorItem(), 160.0F, true);
      registerItem("widowboots", new WidowBootsArmorItem(), 80.0F, true);
      registerItem("ancestorshat", new AncestorsHatArmorItem(), 110.0F, true);
      registerItem("ancestorsrobe", new AncestorsRobeArmorItem(), 110.0F, true);
      registerItem("ancestorsboots", new AncestorsBootsArmorItem(), 110.0F, true);
      registerItem("ancientfossilmask", new AncientFossilMaskArmorItem(), 110.0F, true);
      registerItem("ancientfossilhelmet", new AncientFossilHelmetArmorItem(), 110.0F, true);
      registerItem("ancientfossilchestplate", new AncientFossilChestplateArmorItem(), 160.0F, true);
      registerItem("ancientfossilboots", new AncientFossilBootsArmorItem(), 80.0F, true);
      registerItem("sharpshooterhat", new SharpshooterHatArmorItem(), 110.0F, true);
      registerItem("sharpshootercoat", new SharpshooterCoatArmorItem(), 160.0F, true);
      registerItem("sharpshooterboots", new SharpshooterBootsArmorItem(), 80.0F, true);
      registerItem("phoenixmask", new PhoenixMaskArmorItem(), 400.0F, true);
      registerItem("phoenixcloak", new PhoenixCloakArmorItem(), 480.0F, true);
      registerItem("phoenixboots", new PhoenixBootsArmorItem(), 320.0F, true);
      registerItem("slimehat", new SlimeHatArmorItem(), 300.0F, true);
      registerItem("slimehelmet", new SlimeHelmetArmorItem(), 300.0F, true);
      registerItem("slimechestplate", new SlimeChestplateArmorItem(), 400.0F, true);
      registerItem("slimeboots", new SlimeBootsArmorItem(), 200.0F, true);
      registerItem("nightsteelhelmet", new NightsteelHelmetArmorItem(), 150.0F, true);
      registerItem("nightsteelmask", new NightsteelMaskArmorItem(), 150.0F, true);
      registerItem("nightsteelveil", new NightsteelVeilArmorItem(), 150.0F, true);
      registerItem("nightsteelcirclet", new NightsteelCircletArmorItem(), 150.0F, true);
      registerItem("nightsteelchestplate", new NightsteelChestplateArmorItem(), 200.0F, true);
      registerItem("nightsteelboots", new NightsteelBootsArmorItem(), 120.0F, true);
      registerItem("spideritehelmet", new SpideriteHelmetArmorItem(), 200.0F, true);
      registerItem("spideritehat", new SpideriteHatArmorItem(), 200.0F, true);
      registerItem("spideritehood", new SpideriteHoodArmorItem(), 200.0F, true);
      registerItem("spideritecrown", new SpideriteCrownArmorItem(), 200.0F, true);
      registerItem("spideritechestplate", new SpideriteChestplateArmorItem(), 300.0F, true);
      registerItem("spideritegreaves", new SpideriteGreavesArmorItem(), 150.0F, true);
      registerItem("ravenlordsheaddress", new RavenlordsHeaddressArmorItem(), 150.0F, true);
      registerItem("ravenlordschestplate", new RavenlordsChestplateArmorItem(), 200.0F, true);
      registerItem("ravenlordsboots", new RavenlordsBootsArmorItem(), 120.0F, true);
      registerItem("battlechefhat", new BattleChefHatArmorItem(), 150.0F, true);
      registerItem("battlechefchestplate", new BattleChefChestplateArmorItem(), 200.0F, true);
      registerItem("battlechefboots", new BattleChefBootsArmorItem(), 120.0F, true);
      registerItem("dawnhelmet", new DawnHelmetArmorItem(), 150.0F, true);
      registerItem("dawnchestplate", new DawnChestplateArmorItem(), 150.0F, true);
      registerItem("dawnboots", new DawnBootsArmorItem(), 150.0F, true);
      registerItem("duskhelmet", new DuskHelmetArmorItem(), 150.0F, true);
      registerItem("duskchestplate", new DuskChestplateArmorItem(), 150.0F, true);
      registerItem("duskboots", new DuskBootsArmorItem(), 150.0F, true);
      registerItem("sapphireeyepatch", new SapphireEyepatch(), 1000.0F, true);
      registerItem("amethysthelmet", new AmethystHelmet(), 1000.0F, true);
      registerItem("emeraldmask", new EmeraldMask(), 1000.0F, true);
      registerItem("rubycrown", new RubyCrown(), 1000.0F, true);
      registerItem("crystalchestplate", new CrystalChestplate(), 1000.0F, true);
      registerItem("crystalboots", new CrystalBoots(), 500.0F, true);
      registerItem("makeshiftfaceplating", new MakeshiftFacePlating(), 550.0F, true);
      registerItem("makeshifthat", new MakeshiftHat(), 550.0F, true);
      registerItem("makeshifteyepiece", new MakeshiftEyepiece(), 550.0F, true);
      registerItem("makeshiftcrown", new MakeshiftCrown(), 550.0F, true);
      registerItem("makeshiftchestplate", new MakeshiftChestplate(), 1100.0F, true);
      registerItem("makeshiftboots", new MakeshiftBoots(), 550.0F, true);
      registerItem("arcanichelmet", new ArcanicHelmetArmorItem(), 1000.0F, true);
      registerItem("arcanicchestplate", new ArcanicChestplateArmorItem(), 1000.0F, true);
      registerItem("arcanicboots", new ArcanicBootsArmorItem(), 1000.0F, true);
      registerItem("runeboundcrown", new RuneboundCrownArmorItem(), 0.0F, true);
      registerItem("runeboundcrownmask", new RuneboundCrownMaskArmorItem(), 0.0F, true);
      registerItem("runeboundskullhelmet", new RuneboundSkullHelmetArmorItem(), 0.0F, true);
      registerItem("runeboundhelmet", new RuneboundHelmetArmorItem(), 0.0F, true);
      registerItem("runeboundhornhelmet", new RuneboundHornHelmetArmorItem(), 0.0F, true);
      registerItem("runeboundhood", new RuneboundHoodArmorItem(), 0.0F, true);
      registerItem("runeboundbackbones", new RuneboundBackBonesArmorItem(), 0.0F, true);
      registerItem("runeboundrobe", new RuneboundRobeArmorItem(), 0.0F, true);
      registerItem("runeboundbonesrobe", new RuneboundBonesRobeChestArmorItem(), 0.0F, true);
      registerItem("runeboundleatherchest", new RuneboundLeatherChestArmorItem(), 0.0F, true);
      registerItem("runeboundboots", new RuneboundBootsArmorItem(), 0.0F, true);
      registerItem("shoes", new ShoesArmorItem(0), 100.0F, true);
      registerItem("shirt", new ShirtArmorItem(0), 100.0F, true);
      registerItem("wig", new WigArmorItem(0), 100.0F, true);
      registerItem("cheatshoes", new CheatShoesArmorItem(), 0.0F, false);
      registerItem("cheatshirt", new CheatShirtArmorItem(), 0.0F, false);
      registerItem("cheatwig", new CheatWigArmorItem(), 0.0F, false);
      registerItem("farmerhat", new FarmerHatArmorItem(), 15.0F, true);
      registerItem("farmershirt", new FarmerShirtArmorItem(), 35.0F, true);
      registerItem("farmershoes", new FarmerShoesArmorItem(), 10.0F, true);
      registerItem("farmerpitchfork", new FarmerPitchForkHoldItem(), 50.0F, false);
      registerItem("alchemistglasses", new AlchemistGlassesArmorItem(), 50.0F, true);
      registerItem("labcoat", new LabCoatArmorItem(), 50.0F, true);
      registerItem("labapron", new LabApronArmorItem(), 50.0F, true);
      registerItem("labboots", new LabBootsArmorItem(), 50.0F, true);
      registerItem("rainhat", new RainHatArmorItem(), 50.0F, true);
      registerItem("raincoat", new RainCoatArmorItem(), 50.0F, true);
      registerItem("rainboots", new RainBootsArmorItem(), 50.0F, true);
      registerItem("animalkeepershat", new AnimalKeeperHatArmorItem(), 50.0F, true);
      registerItem("animalkeepershirt", new AnimalKeeperShirtArmorItem(), 50.0F, true);
      registerItem("animalkeepershoes", new AnimalKeeperShoesArmorItem(), 50.0F, true);
      registerItem("engineergoggles", new EngineerGogglesArmorItem(), 50.0F, true);
      registerItem("smithingapron", new SmithingApronArmorItem(), 50.0F, true);
      registerItem("smithingshoes", new SmithingShoesArmorItem(), 50.0F, true);
      registerItem("turban", new TurbanArmorItem(), 50.0F, true);
      registerItem("exoticshirt", new ExoticShirtArmorItem(), 50.0F, true);
      registerItem("exoticshoes", new ExoticShoesArmorItem(), 50.0F, true);
      registerItem("walkingstick", new WalkingStickHoldItem(), 50.0F, false, false, false);
      registerItem("hardhat", new HardHatArmorItem(), 50.0F, true);
      registerItem("elderhat", new ElderHatArmorItem(), 50.0F, true);
      registerItem("eldershirt", new ElderShirtArmorItem(), 50.0F, true);
      registerItem("eldershoes", new ElderShoesArmorItem(), 50.0F, true);
      registerItem("safarihat", new SafariHatArmorItem(), 50.0F, true);
      registerItem("safarishirt", new SafariShirtArmorItem(), 50.0F, true);
      registerItem("safarishoes", new SafariShoesArmorItem(), 50.0F, true);
      registerItem("minerhat", new MinerHatArmorItem(), 50.0F, true);
      registerItem("minershirt", new MinerShirtArmorItem(), 50.0F, true);
      registerItem("minerboots", new MinerBootsArmorItem(), 50.0F, true);
      registerItem("hunterhood", new HunterHoodArmorItem(), 50.0F, true);
      registerItem("hunterhoodmask", new HunterHoodMaskArmorItem(), 50.0F, true);
      registerItem("huntershirt", new HunterShirtArmorItem(), 50.0F, true);
      registerItem("hunterboots", new HunterBootsArmorItem(), 50.0F, true);
      registerItem("magehat", new MageHatArmorItem(), 50.0F, true);
      registerItem("magerobe", new MageRobeArmorItem(), 50.0F, true);
      registerItem("mageshoes", new MageShoesArmorItem(), 50.0F, true);
      registerItem("crimsonhat", new CrimsonHatArmorItem(), 50.0F, true);
      registerItem("crimsonrobe", new CrimsonRobeArmorItem(), 50.0F, true);
      registerItem("crimsonshoes", new CrimsonShoesArmorItem(), 50.0F, true);
      registerItem("tophat", new TopHatArmorItem(), 50.0F, true);
      registerItem("blazer", new BlazerArmorItem(), 50.0F, true);
      registerItem("dressshoes", new DressShoesArmorItem(), 50.0F, true);
      registerItem("stylistshirt", new StylistShirtArmorItem(), 50.0F, true);
      registerItem("stylistshoes", new StylistShoesArmorItem(), 50.0F, true);
      registerItem("merchantshirt", new MerchantShirtArmorItem(), 50.0F, true);
      registerItem("merchantboots", new MerchantBootsArmorItem(), 50.0F, true);
      registerItem("captainshat", new CaptainsHatArmorItem(), 200.0F, true);
      registerItem("captainsshirt", new CaptainsShirtArmorItem(), 200.0F, true);
      registerItem("captainsboots", new CaptainsBootsArmorItem(), 200.0F, true);
      registerItem("pirateeyepatch", new PirateEyePatchArmorItem(), 200.0F, true);
      registerItem("piratebandana", new PirateBandanaArmorItem(), 150.0F, true);
      registerItem("piratebandanawitheyepatch", new PirateBandanaWithEyePatchArmorItem(), 150.0F, true);
      registerItem("pirateshirt", new PirateShirtArmorItem(), 150.0F, true);
      registerItem("pirateboots", new PirateBootsArmorItem(), 150.0F, true);
      registerItem("vulturemask", new VultureMaskArmorItem(), 200.0F, true);
      registerItem("plaguemask", new PlagueMaskArmorItem(), 150.0F, true);
      registerItem("plaguerobe", new PlagueRobeArmorItem(), 150.0F, true);
      registerItem("plagueboots", new PlagueBootsArmorItem(), 150.0F, true);
      registerItem("surgicalmask", new SurgicalMaskArmorItem(), 50.0F, true);
      registerItem("trapperhat", new TrapperHatArmorItem(), 20.0F, true);
      registerItem("horsemask", new HorseMaskArmorItem(), 50.0F, true);
      registerItem("horsecostumeshirt", new HorseCostumeShirtArmorItem(), 50.0F, true);
      registerItem("horsecostumeboots", new HorseCostumeBootsArmorItem(), 50.0F, true);
      registerItem("christmashat", new ChristmasHatArmorItem(), 10.0F, true, false);
      registerItem("partyhat", new PartyHatArmorItem(), 10.0F, true, false);
      registerItem("pumpkinmask", new PumpkinMaskArmorItem(), 10.0F, true, false);
      registerItem("chickenmask", new ChickenMaskArmorItem(), 50.0F, true);
      registerItem("chickencostumeshirt", new ChickenCostumeShirtArmorItem(), 50.0F, true);
      registerItem("chickencostumeboots", new ChickenCostumeBootsArmorItem(), 50.0F, true);
      registerItem("frogmask", new FrogMaskArmorItem(), 50.0F, true);
      registerItem("frogcostumeshirt", new FrogCostumeShirtArmorItem(), 50.0F, true);
      registerItem("frogcostumeboots", new FrogCostumeBootsArmorItem(), 50.0F, true);
      registerItem("alienmask", new AlienMaskArmorItem(), 50.0F, true);
      registerItem("aliencostumeshirt", new AlienCostumeShirtArmorItem(), 50.0F, true);
      registerItem("aliencostumeboots", new AlienCostumeBootsArmorItem(), 50.0F, true);
      registerItem("sharkmask", new SharkMaskArmorItem(), 50.0F, true);
      registerItem("sharkcostumeshirt", new SharkCostumeShirtArmorItem(), 50.0F, true);
      registerItem("sharkcostumeboots", new SharkCostumeBootsArmorItem(), 50.0F, true);
      registerItem("sunglasses", new SunglassesArmorItem(), 50.0F, true);
      registerItem("jesterhat", new JesterHatArmorItem(), 50.0F, true);
      registerItem("jestershirt", new JesterShirtArmorItem(), 50.0F, true);
      registerItem("jesterboots", new JesterBootsArmorItem(), 50.0F, true);
      registerItem("hulahat", new HulaHatArmorItem(), 50.0F, true);
      registerItem("hulaskirtwithtop", new HulaShirtWithTopArmorItem(), 50.0F, true);
      registerItem("hulaskirt", new HulaShirtArmorItem(), 50.0F, true);
      registerItem("swimsuit", new SwimSuitArmorItem(), 50.0F, true);
      registerItem("swimtrunks", new SwimTrunksArmorItem(), 50.0F, true);
      registerItem("snowhood", new SnowHoodArmorItem(), 50.0F, true);
      registerItem("snowcloak", new SnowCloakArmorItem(), 50.0F, true);
      registerItem("snowboots", new SnowBootsArmorItem(), 50.0F, true);
      registerItem("sailorhat", new SailorHatArmorItem(), 50.0F, true);
      registerItem("sailorshirt", new SailorShirtArmorItem(), 50.0F, true);
      registerItem("sailorshoes", new SailorShoesArmorItem(), 50.0F, true);
      registerItem("spacehelmet", new SpaceHelmetArmorItem(), 50.0F, true);
      registerItem("spacesuit", new SpaceSuitArmorItem(), 50.0F, true);
      registerItem("spaceboots", new SpaceBootsArmorItem(), 50.0F, true);
      registerItem("empressmask", new EmpressMaskArmorItem(), 50.0F, true);
      registerItem("stylishflower", new StylishFlowerArmorItem(), 50.0F, true);
      registerItem("stealthbasket", new StealthBasketArmorItem(), 100.0F, true, false);
      registerItem("syndicatethughood", new SyndicateThugHoodArmorItem(), 0.0F, false);
      registerItem("syndicatethugshirt", new SyndicateThugShirtArmorItem(), 0.0F, false);
      registerItem("syndicatethugboots", new SyndicateThugBootsArmorItem(), 0.0F, false);
      registerItem("mimeberet", new MimeBeretArmorItem(), 50.0F, true);
      registerItem("mimeshirt", new MimeShirtArmorItem(), 50.0F, true);
      registerItem("mimeboots", new MimeBootsArmorItem(), 50.0F, true);
      registerItem("supporterhelmet", new SupporterHelmetArmorItem(), 0.0F, false);
      registerItem("supporterchestplate", new SupporterChestplateArmorItem(), 0.0F, false);
      registerItem("supporterboots", new SupporterBootsArmorItem(), 0.0F, false);
      registerItem("jonasglasses", new JonasGlassesArmorItem(), 0.0F, true, false);
      registerItem(
         "carp",
         new FishItem(250, Item.Rarity.COMMON, new String[]{"anycommonfish"}).spoilDuration(240).setItemCategory("consumable", "commonfish"),
         12.0F,
         true
      );
      registerItem(
         "cod",
         new FishItem(250, Item.Rarity.COMMON, new String[]{"anycommonfish"}).spoilDuration(240).setItemCategory("consumable", "commonfish"),
         12.0F,
         true
      );
      registerItem(
         "herring",
         new FishItem(250, Item.Rarity.COMMON, new String[]{"anycommonfish"}).spoilDuration(240).setItemCategory("consumable", "commonfish"),
         12.0F,
         true
      );
      registerItem(
         "mackerel",
         new FishItem(250, Item.Rarity.COMMON, new String[]{"anycommonfish"}).spoilDuration(240).setItemCategory("consumable", "commonfish"),
         12.0F,
         true
      );
      registerItem(
         "salmon",
         new FishItem(250, Item.Rarity.COMMON, new String[]{"anycommonfish"}).spoilDuration(240).setItemCategory("consumable", "commonfish"),
         12.0F,
         true
      );
      registerItem(
         "trout",
         new FishItem(250, Item.Rarity.COMMON, new String[]{"anycommonfish"}).spoilDuration(240).setItemCategory("consumable", "commonfish"),
         12.0F,
         true
      );
      registerItem(
         "tuna",
         new FishItem(250, Item.Rarity.COMMON, new String[]{"anycommonfish"}).spoilDuration(240).setItemCategory("consumable", "commonfish"),
         12.0F,
         true
      );
      registerItem("gobfish", new FishItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "specialfish"), 16.0F, true);
      registerItem("terrorfish", new FishItem(250, Item.Rarity.RARE).setItemCategory("materials", "specialfish"), 30.0F, true);
      registerItem("halffish", new FishItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "specialfish"), 16.0F, true);
      registerItem("rockfish", new FishItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "specialfish"), 16.0F, true);
      registerItem("furfish", new FishItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "specialfish"), 16.0F, true);
      registerItem("icefish", new FishItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "specialfish"), 16.0F, true);
      registerItem("swampfish", new FishItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "specialfish"), 16.0F, true);
      registerItem("spoiledfood", new MatItem(1000, Item.Rarity.NORMAL, "compostabletip", new String[]{"anycompostable"}).setItemCategory("materials"), 0.1F, true);
      getItem("mushroom").setItemCategory("materials", "flowers");
      registerItem("wheat", new GrainItem(250, Item.Rarity.NORMAL).cropTexture("wheat").spoilDuration(960).addGlobalIngredient("anycompostable"), 2.0F, true);
      registerItem(
         "sugarbeet", new FoodMatItem(250, Item.Rarity.NORMAL).cropTexture("sugarbeet").spoilDuration(480).addGlobalIngredient("anycompostable"), 2.0F, true
      );
      registerItem("flour", new FoodMatItem(250, Item.Rarity.NORMAL).spoilDuration(960), 3.0F, true);
      registerItem("sugar", new FoodMatItem(250, Item.Rarity.NORMAL).spoilDuration(960), 3.0F, true);
      registerItem("honey", new FoodMatItem(250, Item.Rarity.NORMAL), 5.0F, true);
      registerItem("rice", new FoodMatItem(250, Item.Rarity.NORMAL).cropTexture("rice").spoilDuration(960), 3.0F, true);
      registerItem("groundcoffee", new FoodMatItem(250, Item.Rarity.NORMAL).cropTexture("coffee").spoilDuration(480), 6.0F, true);
      registerItem(
         "beef",
         new FoodConsumableItem(250, Item.Rarity.NORMAL, Settler.FOOD_SIMPLE, 10, 240, new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, -10))
            .debuff()
            .spoilDuration(240)
            .addGlobalIngredient("anyrawmeat")
            .setItemCategory("consumable", "rawfood"),
         4.0F,
         true
      );
      registerItem(
         "rawmutton",
         new FoodConsumableItem(250, Item.Rarity.NORMAL, Settler.FOOD_SIMPLE, 10, 240, new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, -10))
            .debuff()
            .spoilDuration(240)
            .addGlobalIngredient("anyrawmeat")
            .setItemCategory("consumable", "rawfood"),
         4.0F,
         true
      );
      registerItem(
         "rawpork",
         new FoodConsumableItem(250, Item.Rarity.NORMAL, Settler.FOOD_SIMPLE, 10, 240, new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, -10))
            .debuff()
            .spoilDuration(240)
            .addGlobalIngredient("anyrawmeat")
            .setItemCategory("consumable", "rawfood"),
         6.0F,
         true
      );
      registerItem(
         "rawchickenleg",
         new FoodConsumableItem(100, Item.Rarity.NORMAL, Settler.FOOD_SIMPLE, 10, 240, new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, -10))
            .debuff()
            .spoilDuration(240)
            .addGlobalIngredient("anyrawmeat")
            .setItemCategory("consumable", "rawfood"),
         6.0F,
         true
      );
      registerItem(
         "rabbitleg",
         new FoodConsumableItem(250, Item.Rarity.COMMON, Settler.FOOD_SIMPLE, 10, 240, new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, -10))
            .debuff()
            .spoilDuration(240)
            .addGlobalIngredient("anyrawmeat")
            .setItemCategory("consumable", "rawfood"),
         6.0F,
         true
      );
      registerItem(
         "duckbreast",
         new FoodConsumableItem(250, Item.Rarity.COMMON, Settler.FOOD_SIMPLE, 10, 240, new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, -10))
            .debuff()
            .spoilDuration(240)
            .addGlobalIngredient("anyrawmeat")
            .setItemCategory("consumable", "rawfood"),
         6.0F,
         true
      );
      registerItem(
         "frogleg",
         new FoodConsumableItem(250, Item.Rarity.COMMON, Settler.FOOD_SIMPLE, 10, 240, new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, -10))
            .debuff()
            .spoilDuration(240)
            .addGlobalIngredient("anyrawmeat")
            .setItemCategory("consumable", "rawfood"),
         6.0F,
         true
      );
      registerItem(
         "sharkmeat",
         new FoodConsumableItem(250, Item.Rarity.COMMON, Settler.FOOD_SIMPLE, 10, 240, new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, -10))
            .debuff()
            .spoilDuration(240)
            .addGlobalIngredient("anyrawmeat", "anycommonfish")
            .setItemCategory("consumable", "rawfood"),
         6.0F,
         true
      );
      registerItem(
         "corn",
         new FoodConsumableItem(250, Item.Rarity.NORMAL, Settler.FOOD_SIMPLE, 15, 240, new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 10))
            .cropTexture("corn")
            .spoilDuration(480)
            .addGlobalIngredient("anycompostable")
            .setItemCategory("consumable", "rawfood"),
         2.0F,
         true
      );
      registerItem(
         "tomato",
         new FoodConsumableItem(250, Item.Rarity.NORMAL, Settler.FOOD_SIMPLE, 15, 240, new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 0.25F))
            .cropTexture("tomato")
            .spoilDuration(480)
            .addGlobalIngredient("anycompostable")
            .setItemCategory("consumable", "rawfood"),
         2.0F,
         true
      );
      registerItem(
         "cabbage",
         new FoodConsumableItem(250, Item.Rarity.NORMAL, Settler.FOOD_SIMPLE, 15, 240, new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 0.25F))
            .cropTexture("cabbage")
            .spoilDuration(480)
            .addGlobalIngredient("anycompostable")
            .setItemCategory("consumable", "rawfood"),
         2.0F,
         true
      );
      registerItem(
         "chilipepper",
         new FoodConsumableItem(250, Item.Rarity.NORMAL, Settler.FOOD_SIMPLE, 15, 240, new ModifierValue<>(BuffModifiers.SPEED, 0.02F))
            .cropTexture("chilipepper")
            .spoilDuration(480)
            .addGlobalIngredient("anycompostable")
            .setItemCategory("consumable", "rawfood"),
         2.0F,
         true
      );
      registerItem(
         "eggplant",
         new FoodConsumableItem(250, Item.Rarity.NORMAL, Settler.FOOD_SIMPLE, 15, 240, new ModifierValue<>(BuffModifiers.SPEED, 0.02F))
            .cropTexture("eggplant")
            .spoilDuration(480)
            .addGlobalIngredient("anycompostable")
            .setItemCategory("consumable", "rawfood"),
         2.0F,
         true
      );
      registerItem(
         "potato",
         new FoodConsumableItem(250, Item.Rarity.NORMAL, Settler.FOOD_SIMPLE, 15, 240, new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 0.25F))
            .cropTexture("potato")
            .spoilDuration(480)
            .addGlobalIngredient("anycompostable")
            .setItemCategory("consumable", "rawfood"),
         2.0F,
         true
      );
      registerItem(
         "carrot",
         new FoodConsumableItem(250, Item.Rarity.NORMAL, Settler.FOOD_SIMPLE, 15, 240, new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.05F))
            .cropTexture("carrot")
            .spoilDuration(480)
            .addGlobalIngredient("anycompostable")
            .setItemCategory("consumable", "rawfood"),
         2.0F,
         true
      );
      registerItem(
         "onion",
         new FoodConsumableItem(250, Item.Rarity.COMMON, Settler.FOOD_SIMPLE, 20, 240, new ModifierValue<>(BuffModifiers.SPEED, 0.1F))
            .cropTexture("onion")
            .spoilDuration(480)
            .addGlobalIngredient("anycompostable")
            .setItemCategory("consumable", "rawfood"),
         3.0F,
         true
      );
      registerItem(
         "pumpkin",
         new FoodConsumableItem(250, Item.Rarity.COMMON, Settler.FOOD_SIMPLE, 30, 240, new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 20))
            .cropTexture("pumpkin")
            .spoilDuration(480)
            .addGlobalIngredient("anycompostable")
            .setItemCategory("consumable", "rawfood"),
         5.0F,
         true
      );
      registerItem(
         "strawberry",
         new FoodConsumableItem(250, Item.Rarity.COMMON, Settler.FOOD_SIMPLE, 15, 240, new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 0.5F))
            .cropTexture("strawberry")
            .spoilDuration(480)
            .addGlobalIngredient("anycompostable", "anyfruit")
            .setItemCategory("consumable", "rawfood"),
         3.0F,
         true
      );
      registerItem(
         "apple",
         new FoodConsumableItem(250, Item.Rarity.NORMAL, Settler.FOOD_SIMPLE, 15, 240, new ModifierValue<>(BuffModifiers.SPEED, 0.02F))
            .spoilDuration(480)
            .addGlobalIngredient("anycompostable", "anyfruit")
            .setItemCategory("consumable", "rawfood"),
         14.0F,
         true
      );
      registerItem(
         "lemon",
         new FoodConsumableItem(250, Item.Rarity.COMMON, Settler.FOOD_SIMPLE, 20, 240, new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 20))
            .spoilDuration(480)
            .addGlobalIngredient("anycompostable", "anyfruit")
            .setItemCategory("consumable", "rawfood"),
         16.0F,
         true
      );
      registerItem(
         "coconut",
         new FoodConsumableItem(250, Item.Rarity.NORMAL, Settler.FOOD_SIMPLE, 10, 240, new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 0.25F))
            .spoilDuration(480)
            .addGlobalIngredient("anycompostable", "anyfruit")
            .setItemCategory("consumable", "rawfood"),
         14.0F,
         true
      );
      registerItem(
         "banana",
         new FoodConsumableItem(250, Item.Rarity.COMMON, Settler.FOOD_SIMPLE, 20, 240, new ModifierValue<>(BuffModifiers.SPEED, 0.05F))
            .spoilDuration(480)
            .addGlobalIngredient("anycompostable", "anyfruit")
            .setItemCategory("consumable", "rawfood"),
         16.0F,
         true
      );
      registerItem(
         "blueberry",
         new FoodConsumableItem(250, Item.Rarity.NORMAL, Settler.FOOD_SIMPLE, 10, 240, new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.02F))
            .spoilDuration(480)
            .addGlobalIngredient("anycompostable", "anyfruit")
            .setItemCategory("consumable", "rawfood"),
         4.0F,
         true
      );
      registerItem(
         "raspberry",
         new FoodConsumableItem(250, Item.Rarity.NORMAL, Settler.FOOD_SIMPLE, 10, 240, new ModifierValue<>(BuffModifiers.MAX_MANA_FLAT, 10))
            .spoilDuration(480)
            .addGlobalIngredient("anycompostable", "anyfruit")
            .setItemCategory("consumable", "rawfood"),
         4.0F,
         true
      );
      registerItem(
         "blackberry",
         new FoodConsumableItem(250, Item.Rarity.NORMAL, Settler.FOOD_SIMPLE, 10, 240, new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.02F))
            .spoilDuration(480)
            .addGlobalIngredient("anycompostable", "anyfruit")
            .setItemCategory("consumable", "rawfood"),
         4.0F,
         true
      );
      registerItem(
         "milk",
         new FoodConsumableItem(250, Item.Rarity.COMMON, Settler.FOOD_SIMPLE, 20, 480, true, new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 10))
            .spoilDuration(240)
            .setItemCategory("consumable", "rawfood"),
         2.0F,
         true
      );
      registerItem(
         "egg",
         new EggFoodConsumableItem(250, Item.Rarity.COMMON, Settler.FOOD_SIMPLE, 10, 480, new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, -10))
            .debuff()
            .spoilDuration(240)
            .setItemCategory("consumable", "rawfood"),
         2.0F,
         true
      );
      registerItem(
         "beet",
         new FoodConsumableItem(250, Item.Rarity.COMMON, Settler.FOOD_SIMPLE, 15, 240, new ModifierValue<>(BuffModifiers.MINING_SPEED, 0.1F))
            .cropTexture("beet")
            .spoilDuration(480)
            .addGlobalIngredient("anycompostable")
            .setItemCategory("consumable", "rawfood"),
         2.0F,
         true
      );
      registerItem(
         "steak",
         new FoodConsumableItem(250, Item.Rarity.NORMAL, Settler.FOOD_FINE, 25, 240, new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.02F))
            .spoilDuration(240)
            .addGlobalIngredient("anycookedfood"),
         5.0F,
         true
      );
      registerItem(
         "roastedmutton",
         new FoodConsumableItem(250, Item.Rarity.NORMAL, Settler.FOOD_FINE, 25, 240, new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.02F))
            .spoilDuration(240)
            .addGlobalIngredient("anycookedfood"),
         5.0F,
         true
      );
      registerItem(
         "roastedpork",
         new FoodConsumableItem(250, Item.Rarity.NORMAL, Settler.FOOD_FINE, 25, 480, new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.02F))
            .spoilDuration(240)
            .addGlobalIngredient("anycookedfood"),
         12.0F,
         true
      );
      registerItem(
         "roastedrabbitleg",
         new FoodConsumableItem(250, Item.Rarity.COMMON, Settler.FOOD_FINE, 25, 480, new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.05F))
            .spoilDuration(240)
            .addGlobalIngredient("anycookedfood"),
         7.0F,
         true
      );
      registerItem(
         "roastedduckbreast",
         new FoodConsumableItem(250, Item.Rarity.COMMON, Settler.FOOD_FINE, 25, 480, new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 20))
            .spoilDuration(240)
            .addGlobalIngredient("anycookedfood"),
         7.0F,
         true
      );
      registerItem(
         "roastedfrogleg",
         new FoodConsumableItem(250, Item.Rarity.COMMON, Settler.FOOD_FINE, 25, 480, new ModifierValue<>(BuffModifiers.SPEED, 0.05F))
            .spoilDuration(240)
            .addGlobalIngredient("anycookedfood"),
         7.0F,
         true
      );
      registerItem(
         "roastedfish",
         new FoodConsumableItem(250, Item.Rarity.COMMON, Settler.FOOD_FINE, 25, 480, new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.05F))
            .spoilDuration(240)
            .addGlobalIngredient("anycookedfood"),
         14.0F,
         true
      );
      registerItem(
         "chickendrumstick",
         new FoodConsumableItem(
               250,
               Item.Rarity.COMMON,
               Settler.FOOD_FINE,
               25,
               1200,
               new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 10),
               new ModifierValue<>(BuffModifiers.MAX_RESILIENCE_FLAT, 10)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         8.0F,
         true
      );
      registerItem(
         "cheese",
         new FoodConsumableItem(250, Item.Rarity.NORMAL, Settler.FOOD_FINE, 25, 480, new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 20))
            .spoilDuration(480)
            .addGlobalIngredient("anycookedfood"),
         5.0F,
         true
      );
      registerItem(
         "candyapple",
         new FoodConsumableItem(
               250,
               Item.Rarity.COMMON,
               Settler.FOOD_FINE,
               30,
               480,
               new ModifierValue<>(BuffModifiers.SPEED, 0.05F),
               new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.02F)
            )
            .spoilDuration(240)
            .addGlobalIngredient("anycookedfood"),
         22.0F,
         true
      );
      registerItem(
         "bread",
         new FoodConsumableItem(250, Item.Rarity.COMMON, Settler.FOOD_FINE, 20, 480, new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 20))
            .spoilDuration(480)
            .addGlobalIngredient("anycookedfood"),
         8.0F,
         true
      );
      registerItem(
         "hardboiledegg",
         new FoodConsumableItem(250, Item.Rarity.COMMON, Settler.FOOD_FINE, 10, 480, new ModifierValue<>(BuffModifiers.MAX_RESILIENCE_FLAT, 20))
            .spoilDuration(240)
            .addGlobalIngredient("anycookedfood"),
         9.0F,
         true
      );
      registerItem(
         "friedegg",
         new FoodConsumableItem(
               250,
               Item.Rarity.COMMON,
               Settler.FOOD_FINE,
               10,
               480,
               new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 10),
               new ModifierValue<>(BuffModifiers.MAX_MANA_FLAT, 10)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         6.0F,
         true
      );
      registerItem(
         "candycane",
         new FoodConsumableItem(250, Item.Rarity.UNCOMMON, Settler.FOOD_FINE, 10, 300, new ModifierValue<>(BuffModifiers.SPEED, 0.15F))
            .spoilDuration(1080)
            .addGlobalIngredient("anycookedfood"),
         10.0F,
         true,
         false
      );
      registerItem(
         "popcorn",
         new FoodConsumableItem(250, Item.Rarity.COMMON, Settler.FOOD_FINE, 10, 480, new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 20))
            .spoilDuration(240)
            .addGlobalIngredient("anycookedfood"),
         10.0F,
         true
      );
      registerItem(
         "donut",
         new FoodConsumableItem(
               250,
               Item.Rarity.COMMON,
               Settler.FOOD_FINE,
               20,
               480,
               new ModifierValue<>(BuffModifiers.SPEED, 0.1F),
               new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.05F)
            )
            .spoilDuration(240)
            .addGlobalIngredient("anycookedfood"),
         10.0F,
         true
      );
      registerItem(
         "cookies",
         new FoodConsumableItem(
               250,
               Item.Rarity.COMMON,
               Settler.FOOD_FINE,
               25,
               480,
               new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.05F),
               new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 10)
            )
            .spoilDuration(360)
            .addGlobalIngredient("anycookedfood"),
         10.0F,
         true
      );
      registerItem(
         "raspberryjuice",
         new FoodConsumableItem(250, Item.Rarity.COMMON, Settler.FOOD_FINE, 15, 480, true, new ModifierValue<>(BuffModifiers.COMBAT_MANA_REGEN, 1.0F))
            .spoilDuration(240),
         10.0F,
         true
      );
      registerItem(
         "meatballs",
         new FoodConsumableItem(
               250,
               Item.Rarity.UNCOMMON,
               Settler.FOOD_GOURMET,
               30,
               720,
               new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.05F),
               new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 0.5F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         14.0F,
         true
      );
      registerItem(
         "smokedfillet",
         new FoodConsumableItem(
               250,
               Item.Rarity.UNCOMMON,
               Settler.FOOD_GOURMET,
               30,
               720,
               new ModifierValue<>(BuffModifiers.SPEED, 0.07F),
               new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.05F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         18.0F,
         true
      );
      registerItem(
         "sharkfillet",
         new FoodConsumableItem(250, Item.Rarity.UNCOMMON, Settler.FOOD_GOURMET, 30, 720, new ModifierValue<>(BuffModifiers.SWIM_SPEED, 2.0F))
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         18.0F,
         true
      );
      registerItem(
         "blueberrycake",
         new FoodConsumableItem(
               250,
               Item.Rarity.UNCOMMON,
               Settler.FOOD_GOURMET,
               30,
               720,
               new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.05F),
               new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.05F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         26.0F,
         true
      );
      registerItem(
         "scrambledeggs",
         new FoodConsumableItem(
               250,
               Item.Rarity.UNCOMMON,
               Settler.FOOD_FINE,
               20,
               720,
               new ModifierValue<>(BuffModifiers.MAX_RESILIENCE_FLAT, 20),
               new ModifierValue<>(BuffModifiers.RESILIENCE_GAIN, 0.25F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         15.0F,
         true
      );
      registerItem(
         "cheesybeetbowl",
         new FoodConsumableItem(
               250,
               Item.Rarity.COMMON,
               Settler.FOOD_FINE,
               25,
               960,
               new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 30),
               new ModifierValue<>(BuffModifiers.MINING_SPEED, 0.5F)
            )
            .spoilDuration(240)
            .addGlobalIngredient("anycookedfood"),
         8.0F,
         true
      );
      registerItem(
         "blackberryicecream",
         new FoodConsumableItem(
               250,
               Item.Rarity.UNCOMMON,
               Settler.FOOD_GOURMET,
               30,
               720,
               new ModifierValue<>(BuffModifiers.SPEED, 0.1F),
               new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.05F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         24.0F,
         true
      );
      registerItem(
         "fruitsmoothie",
         new FoodConsumableItem(
               250,
               Item.Rarity.UNCOMMON,
               Settler.FOOD_GOURMET,
               30,
               720,
               true,
               new ModifierValue<>(BuffModifiers.SPEED, 0.1F),
               new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.05F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         36.0F,
         true
      );
      registerItem(
         "fishtaco",
         new FoodConsumableItem(
               250,
               Item.Rarity.UNCOMMON,
               Settler.FOOD_GOURMET,
               30,
               720,
               new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.05F),
               new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.1F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         24.0F,
         true
      );
      registerItem(
         "juniorburger",
         new FoodConsumableItem(
               250,
               Item.Rarity.UNCOMMON,
               Settler.FOOD_GOURMET,
               30,
               720,
               new ModifierValue<>(BuffModifiers.SPEED, 0.1F),
               new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 20),
               new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 0.5F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         22.0F,
         true
      );
      registerItem(
         "cheeseburger",
         new FoodConsumableItem(
               250,
               Item.Rarity.UNCOMMON,
               Settler.FOOD_GOURMET,
               30,
               720,
               new ModifierValue<>(BuffModifiers.SPEED, 0.1F),
               new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 50),
               new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 0.5F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         26.0F,
         true
      );
      registerItem(
         "omelette",
         new FoodConsumableItem(
               250,
               Item.Rarity.UNCOMMON,
               Settler.FOOD_GOURMET,
               25,
               1200,
               new ModifierValue<>(BuffModifiers.SPEED, 0.2F),
               new ModifierValue<>(BuffModifiers.STAMINA_REGEN, 0.2F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         28.0F,
         true
      );
      registerItem(
         "nachos",
         new FoodConsumableItem(
               250,
               Item.Rarity.UNCOMMON,
               Settler.FOOD_GOURMET,
               30,
               720,
               new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.1F),
               new ModifierValue<>(BuffModifiers.SPEED, 0.1F),
               new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 0.25F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         20.0F,
         true
      );
      registerItem(
         "eggplantparmesan",
         new FoodConsumableItem(
               250,
               Item.Rarity.UNCOMMON,
               Settler.FOOD_GOURMET,
               30,
               720,
               new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.1F),
               new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.05F),
               new ModifierValue<>(BuffModifiers.SPEED, 0.1F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         20.0F,
         true
      );
      registerItem(
         "raspberrymousse",
         new FoodConsumableItem(
               250,
               Item.Rarity.RARE,
               Settler.FOOD_GOURMET,
               25,
               960,
               new ModifierValue<>(BuffModifiers.MAX_MANA_FLAT, 50),
               new ModifierValue<>(BuffModifiers.MANA_USAGE, -0.25F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         21.0F,
         true
      );
      registerItem(
         "tropicalstew",
         new FoodConsumableItem(
               250,
               Item.Rarity.RARE,
               Settler.FOOD_GOURMET,
               40,
               960,
               new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.05F),
               new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 50),
               new ModifierValue<>(BuffModifiers.ARMOR_FLAT, 4)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         40.0F,
         true
      );
      registerItem(
         "fishandchips",
         new FoodConsumableItem(
               250,
               Item.Rarity.RARE,
               Settler.FOOD_GOURMET,
               40,
               960,
               new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.1F),
               new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.1F),
               new ModifierValue<>(BuffModifiers.SPEED, 0.1F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         30.0F,
         true
      );
      registerItem(
         "freshpotatosalad",
         new FoodConsumableItem(
               250,
               Item.Rarity.RARE,
               Settler.FOOD_GOURMET,
               40,
               960,
               new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.1F),
               new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 30),
               new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 0.5F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         45.0F,
         true
      );
      registerItem(
         "rootsalad",
         new FoodConsumableItem(
               250,
               Item.Rarity.RARE,
               Settler.FOOD_GOURMET,
               40,
               720,
               new ModifierValue<>(BuffModifiers.MINING_SPEED, 0.3F),
               new ModifierValue<>(BuffModifiers.MINING_RANGE, 1.0F),
               new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 0.3F)
            )
            .spoilDuration(240)
            .addGlobalIngredient("anycookedfood"),
         12.0F,
         true
      );
      registerItem(
         "hotdog",
         new FoodConsumableItem(
               250,
               Item.Rarity.RARE,
               Settler.FOOD_GOURMET,
               40,
               960,
               new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 50),
               new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 1.0F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         45.0F,
         true
      );
      registerItem(
         "ricepudding",
         new FoodConsumableItem(
               250,
               Item.Rarity.RARE,
               Settler.FOOD_GOURMET,
               40,
               960,
               new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.1F),
               new ModifierValue<>(BuffModifiers.SPEED, 0.15F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         22.0F,
         true
      );
      registerItem(
         "minersstew",
         new FoodConsumableItem(
               250,
               Item.Rarity.RARE,
               Settler.FOOD_GOURMET,
               50,
               960,
               new ModifierValue<>(BuffModifiers.MINING_SPEED, 0.5F),
               new ModifierValue<>(BuffModifiers.MINING_RANGE, 1.0F),
               new ModifierValue<>(BuffModifiers.SPEED, 0.15F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         24.0F,
         true
      );
      registerItem(
         "chickencutletdish",
         new FoodConsumableItem(
               250,
               Item.Rarity.EPIC,
               Settler.FOOD_GOURMET,
               50,
               1200,
               new ModifierValue<>(BuffModifiers.ARMOR_FLAT, 25),
               new ModifierValue<>(BuffModifiers.SPEED, -0.1F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         40.0F,
         true
      );
      registerItem(
         "sushirolls",
         new FoodConsumableItem(
               250,
               Item.Rarity.EPIC,
               Settler.FOOD_GOURMET,
               50,
               1200,
               new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.1F),
               new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.15F),
               new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 50)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         32.0F,
         true
      );
      registerItem(
         "friedpork",
         new FoodConsumableItem(
               250,
               Item.Rarity.EPIC,
               Settler.FOOD_GOURMET,
               50,
               1200,
               new ModifierValue<>(BuffModifiers.SPEED, 0.1F),
               new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.15F),
               new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 0.5F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         29.0F,
         true
      );
      registerItem(
         "dessertpancakes",
         new FoodConsumableItem(
               250,
               Item.Rarity.EPIC,
               Settler.FOOD_GOURMET,
               50,
               1200,
               new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 65),
               new ModifierValue<>(BuffModifiers.MAX_RESILIENCE_FLAT, 65)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         28.0F,
         true
      );
      registerItem(
         "bananapudding",
         new FoodConsumableItem(
               250,
               Item.Rarity.EPIC,
               Settler.FOOD_GOURMET,
               50,
               1200,
               new ModifierValue<>(BuffModifiers.SPEED, 0.15F),
               new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.1F),
               new ModifierValue<>(BuffModifiers.ARMOR_FLAT, 6)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         84.0F,
         true
      );
      registerItem(
         "lemontart",
         new FoodConsumableItem(
               250,
               Item.Rarity.EPIC,
               Settler.FOOD_GOURMET,
               50,
               1200,
               new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.1F),
               new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.1F),
               new ModifierValue<>(BuffModifiers.ARMOR_PEN_FLAT, 8)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         90.0F,
         true
      );
      registerItem(
         "parisiansteak",
         new FoodConsumableItem(
               250,
               Item.Rarity.EPIC,
               Settler.FOOD_GOURMET,
               50,
               1200,
               new ModifierValue<>(BuffModifiers.MAX_RESILIENCE_FLAT, 10),
               new ModifierValue<>(BuffModifiers.RESILIENCE_REGEN_FLAT, 1.0F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         25.0F,
         true
      );
      registerItem(
         "deepfriedchicken",
         new FoodConsumableItem(
               250,
               Item.Rarity.EPIC,
               Settler.FOOD_GOURMET,
               50,
               1200,
               new ModifierValue<>(BuffModifiers.SPEED, 0.15F),
               new ModifierValue<>(BuffModifiers.MAX_RESILIENCE_FLAT, 25),
               new ModifierValue<>(BuffModifiers.RESILIENCE_GAIN, 0.25F),
               new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.15F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         35.0F,
         true
      );
      registerItem(
         "spaghettibolognese",
         new FoodConsumableItem(
               250,
               Item.Rarity.EPIC,
               Settler.FOOD_GOURMET,
               50,
               1200,
               new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.1F),
               new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 0.75F),
               new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 50)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         30.0F,
         true
      );
      registerItem(
         "porktenderloin",
         new FoodConsumableItem(
               250,
               Item.Rarity.EPIC,
               Settler.FOOD_GOURMET,
               50,
               1200,
               new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 80),
               new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 1.0F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         35.0F,
         true
      );
      registerItem(
         "beefgoulash",
         new FoodConsumableItem(
               250,
               Item.Rarity.EPIC,
               Settler.FOOD_GOURMET,
               50,
               1200,
               new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.2F),
               new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.15F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         30.0F,
         true
      );
      registerItem(
         "shishkebab",
         new FoodConsumableItem(
               250,
               Item.Rarity.EPIC,
               Settler.FOOD_GOURMET,
               50,
               1200,
               new ModifierValue<>(BuffModifiers.SPEED, 0.1F),
               new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.2F),
               new ModifierValue<>(BuffModifiers.ARMOR_FLAT, 8)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         28.0F,
         true
      );
      registerItem(
         "pumpkinpie",
         new FoodConsumableItem(
               250,
               Item.Rarity.EPIC,
               Settler.FOOD_GOURMET,
               50,
               1200,
               new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.1F),
               new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.15F),
               new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 0.75F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         30.0F,
         true
      );
      registerItem(
         "sweetlemonade",
         new FoodConsumableItem(
               250,
               Item.Rarity.RARE,
               Settler.FOOD_GOURMET,
               25,
               960,
               true,
               new ModifierValue<>(BuffModifiers.SPEED, 0.15F),
               new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.15F)
            )
            .spoilDuration(120),
         100.0F,
         true
      );
      registerItem(
         "raspberryjam",
         new FoodConsumableItem(
               250,
               Item.Rarity.EPIC,
               Settler.FOOD_GOURMET,
               30,
               1200,
               new ModifierValue<>(BuffModifiers.MAGIC_CRIT_DAMAGE, 0.2F),
               new ModifierValue<>(BuffModifiers.MAX_MANA_FLAT, 200),
               new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, -50)
            )
            .spoilDuration(480)
            .addGlobalIngredient("anycookedfood"),
         32.0F,
         true
      );
      registerItem(
         "raspberrypie",
         new FoodConsumableItem(
               250,
               Item.Rarity.EPIC,
               Settler.FOOD_GOURMET,
               45,
               1200,
               new ModifierValue<>(BuffModifiers.MAGIC_DAMAGE, 0.2F),
               new ModifierValue<>(BuffModifiers.MAX_MANA_FLAT, 75),
               new ModifierValue<>(BuffModifiers.COMBAT_MANA_REGEN, 1.5F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         34.0F,
         true
      );
      registerItem(
         "strawberrypie",
         new FoodConsumableItem(
               250,
               Item.Rarity.EPIC,
               Settler.FOOD_GOURMET,
               50,
               1200,
               new ModifierValue<>(BuffModifiers.SPEED, 0.1F),
               new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.2F),
               new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 50)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         35.0F,
         true
      );
      registerItem(
         "wildsalad",
         new FoodConsumableItem(
               250,
               Item.Rarity.EPIC,
               Settler.FOOD_GOURMET,
               50,
               1200,
               new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.1F),
               new ModifierValue<>(BuffModifiers.SPEED, 0.15F),
               new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 0.5F),
               new ModifierValue<>(BuffModifiers.MAX_MANA_FLAT, 40),
               new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.2F)
            )
            .spoilDuration(120)
            .addGlobalIngredient("anycookedfood"),
         28.0F,
         true
      );
      registerItem(
         "blackcoffee",
         new FoodConsumableItem(250, Item.Rarity.RARE, Settler.FOOD_FINE, 20, 1800, true, new ModifierValue<>(BuffModifiers.SPEED, 0.25F)).spoilDuration(120),
         40.0F,
         true
      );
      registerItem(
         "cappuccino",
         new FoodConsumableItem(250, Item.Rarity.EPIC, Settler.FOOD_FINE, 25, 2700, true, new ModifierValue<>(BuffModifiers.SPEED, 0.3F)).spoilDuration(120),
         58.0F,
         true
      );
      registerItem("healthpotion", new HealthPotionItem(Item.Rarity.COMMON, 50, 0.1F), 5.0F, true);
      registerItem("greaterhealthpotion", new HealthPotionItem(Item.Rarity.UNCOMMON, 100, 0.2F), 6.0F, true);
      registerItem("superiorhealthpotion", new HealthPotionItem(Item.Rarity.RARE, 150, 0.3F), 10.0F, true);
      registerItem("manapotion", new ManaPotionItem(Item.Rarity.COMMON, 50), 5.0F, true);
      registerItem("greatermanapotion", new ManaPotionItem(Item.Rarity.UNCOMMON, 150), 6.0F, true);
      registerItem("superiormanapotion", new ManaPotionItem(Item.Rarity.RARE, 300), 10.0F, true);
      REVIVAL_POTION_ID = registerItem("revivalpotion", new RevivalPotion(), 50.0F, true);
      registerItem("speedpotion", new SpeedPotionItem(), 10.0F, true);
      registerItem("greaterspeedpotion", new GreaterSpeedPotionItem(), 20.0F, true);
      registerItem("healthregenpotion", new HealthRegenPotionItem(), 10.0F, true);
      registerItem("greaterhealthregenpotion", new GreaterHealthRegenPotionItem(), 20.0F, true);
      registerItem("resistancepotion", new ResistancePotionItem(), 15.0F, true);
      registerItem("greaterresistancepotion", new GreaterResistancePotionItem(), 30.0F, true);
      registerItem("battlepotion", new BattlePotionItem(), 35.0F, true);
      registerItem("greaterbattlepotion", new GreaterBattlePotionItem(), 50.0F, true);
      registerItem("attackspeedpotion", new AttackSpeedPotionItem(), 10.0F, true);
      registerItem("greaterattackspeedpotion", new GreaterAttackSpeedPotionItem(), 20.0F, true);
      registerItem("manaregenpotion", new ManaRegenPotionItem(), 10.0F, true);
      registerItem("greatermanaregenpotion", new GreaterManaRegenPotionItem(), 20.0F, true);
      registerItem("accuracypotion", new AccuracyPotionItem(), 10.0F, true);
      registerItem("greateraccuracypotion", new GreaterAccuracyPotionItem(), 20.0F, true);
      registerItem("rapidpotion", new RapidPotionItem(), 10.0F, true);
      registerItem("greaterrapidpotion", new GreaterRapidPotionItem(), 20.0F, true);
      registerItem("knockbackpotion", new KnockbackPotionItem(), 10.0F, true);
      registerItem("thornspotion", new ThornsPotionItem(), 10.0F, true);
      registerItem("fireresistancepotion", new FireResistancePotionItem(), 15.0F, true);
      registerItem("invisibilitypotion", new InvisibilityPotionItem(), 40.0F, true);
      registerItem("fishingpotion", new FishingPotionItem(), 20.0F, true);
      registerItem("greaterfishingpotion", new GreaterFishingPotionItem(), 40.0F, true);
      registerItem("miningpotion", new MiningPotionItem(), 20.0F, true);
      registerItem("greaterminingpotion", new GreaterMiningPotionItem(), 40.0F, true);
      registerItem("spelunkerpotion", new SpelunkerPotionItem(), 30.0F, true);
      registerItem("treasurepotion", new TreasurePotionItem(), 30.0F, true);
      registerItem("trackerpotion", new TrackerPotionItem(), 40.0F, true);
      registerItem("passivepotion", new PassivePotionItem(), 10.0F, true);
      registerItem("buildingpotion", new BuildingPotionItem(), 15.0F, true);
      registerItem("greaterbuildingpotion", new GreaterBuildingPotionItem(), 30.0F, true);
      registerItem("strengthpotion", new StrengthPotionItem(), 20.0F, true);
      registerItem("rangerpotion", new RangerPotionItem(), 20.0F, true);
      registerItem("wisdompotion", new WisdomPotionItem(), 20.0F, true);
      registerItem("minionpotion", new MinionPotionItem(), 20.0F, true);
      registerItem("webpotion", new WebPotionItem(), 20.0F, true);
      registerItem("recallscroll", new RecallScrollItem(), 25.0F, true);
      registerItem("teleportationscroll", new TeleportationScrollItem(), 100.0F, true);
      registerItem("recallflask", new RecallFlaskItem(), 400.0F, true);
      registerItem("stinkflask", new StinkFlaskItem(), 200.0F, true);
      registerItem("portalflask", new PortalFlaskItem(), 800.0F, true);
      registerItem("enchantingscroll", new EnchantingScrollItem(), 200.0F, true);
      registerItem("gatewaytablet", new GatewayTabletItem(), 500.0F, true);
      registerItem("craftingguide", new CraftingGuideBookItem(), 20.0F, true);
      registerItem("recipebook", new RecipeBookItem(), 400.0F, true);
      registerItem("voidpouch", new VoidPouchItem(), 400.0F, true);
      registerItem("voidbag", new VoidBagItem(), 1200.0F, true);
      registerItem("coinpouch", new CoinPouch(), 600.0F, true);
      registerItem("ammopouch", new AmmoPouch(), 200.0F, true);
      registerItem("ammobag", new AmmoBag(), 400.0F, true);
      registerItem("potionpouch", new PotionPouch(), 800.0F, true);
      registerItem("potionbag", new PotionBag(), 1000.0F, true);
      registerItem("lunchbox", new Lunchbox(), 400.0F, true);
      registerItem("seedpouch", new SeedPouch(), 500.0F, true);
      registerItem("tabletbox", new TabletBox(), 1000.0F, true);
      registerItem("portablemusicplayer", new PortableMusicPlayerItem(), 800.0F, true);
      registerItem("fireworkrocket", new FireworkPlaceableItem(), 5.0F, true);
      registerItem("woodboat", new WoodBoatMountItem(), 10.0F, true);
      registerItem("minecart", new MinecartMountItem(), 50.0F, true);
      registerItem("shears", new ShearsItem(), 50.0F, true);
      registerItem("bucket", new BucketItem(), 10.0F, true);
      registerItem("infinitewaterbucket", new InfiniteWaterBucketItem(), 250.0F, true);
      registerItem("rope", new RopeItem(), 50.0F, true);
      registerItem("infiniterope", new InfiniteRopeItem(), 250.0F, true);
      registerItem("binoculars", new BinocularsItem(), 100.0F, true);
      registerItem("telescope", new TelescopeItem(), 1000.0F, false);
      registerItem("strikebanner", new StrikeBannerItem(), 25.0F, true);
      registerItem("greenpresent", new PresentItem(), 10.0F, true, false);
      registerItem("bluepresent", new PresentItem(), 10.0F, true, false);
      registerItem("redpresent", new PresentItem(), 10.0F, true, false);
      registerItem("yellowpresent", new PresentItem(), 10.0F, true, false);
      registerItem("christmaspresent", new ChristmasPresentItem(), 10.0F, true, false);
      registerItem("greenwrappingpaper", new WrappingPaperItem("greenpresent"), 10.0F, true, false);
      registerItem("bluewrappingpaper", new WrappingPaperItem("bluepresent"), 10.0F, true, false);
      registerItem("redwrappingpaper", new WrappingPaperItem("redpresent"), 10.0F, true, false);
      registerItem("yellowwrappingpaper", new WrappingPaperItem("yellowpresent"), 10.0F, true, false);
      registerItem("landfill", new LandfillItem(), 0.1F, true);
      registerItem("grassseed", new GrassSeedItem("grasstile"), 2.0F, true);
      registerItem("overgrowngrassseed", new GrassSeedItem("overgrowngrasstile"), 2.0F, true);
      registerItem("swampgrassseed", new GrassSeedItem("swampgrasstile"), 2.0F, true);
      registerItem("overgrownswampgrassseed", new GrassSeedItem("overgrownswampgrasstile"), 2.0F, true);
      registerItem("plainsgrassseed", new GrassSeedItem("plainsgrasstile"), 2.0F, true);
      registerItem("overgrownplainsgrassseed", new GrassSeedItem("overgrownplainsgrasstile"), 2.0F, true);
      registerItem("fertilizer", new FertilizerPlaceableItem(), 4.0F, true);
      registerItem("mysteriousportal", new EvilsProtectorSpawnItem(), 50.0F, true);
      registerItem("royalegg", new QueenSpiderSpawnItem(), 50.0F, true);
      registerItem("voidcaller", new VoidWizardSpawnItem(), 50.0F, true);
      registerItem("boneoffering", new BoneOfferingItem(), 60.0F, true);
      registerItem("spikedfossil", new SwampGuardianSpawnItem(), 70.0F, true);
      registerItem("ancientstatue", new AncientVultureSpawnItem(), 100.0F, true);
      registerItem("messageinabottle", new PirateCaptainSpawnItem(), 120.0F, true);
      registerItem("shadowgate", new ReaperSpawnItem(), 140.0F, true);
      registerItem("icecrown", new CryoQueenSpawnItem(), 140.0F, true);
      registerItem("spiriturn", new CursedCroneSpawnItem(), 140.0F, true);
      registerItem("decayingleaf", new PestWardenSpawnItem(), 150.0F, true);
      registerItem("dragonsouls", new DragonSoulsItem(), 160.0F, true);
      registerItem("slimeeggs", new MotherSlimeSpawnItem(), 170.0F, false);
      registerItem("swarmsignal", new NightSwarmSpawnItem(), 180.0F, false);
      registerItem("crownofspiderkin", new SpiderEmpressSpawnItem(), 190.0F, false);
      registerItem("fishinghold", new FishingPoleHolding(), 0.0F, false);
      registerItem("itemhold", new ItemHolding(), 0.0F, false);
      registerItem(
         "villagemap",
         new WorldPresetMapItem(
               Item.Rarity.RARE, LevelIdentifier.SURFACE_IDENTIFIER, 800, "village", new LocalMessage("biome", "npcvillage"), "surfacevillages"
            )
            .setLocalizationKey("villagemapnew"),
         40.0F,
         true
      );
      registerItem(
         "dungeonmap",
         new WorldPresetMapItem(
               Item.Rarity.RARE, LevelIdentifier.SURFACE_IDENTIFIER, 800, "voidwizard", new LocalMessage("biome", "dungeon"), "dungeonentrance"
            )
            .setLocalizationKey("dungeonmapnew"),
         40.0F,
         true
      );
      registerItem(
         "piratemap",
         new WorldPresetMapItem(
               Item.Rarity.RARE, LevelIdentifier.SURFACE_IDENTIFIER, 800, "piratebanner", new LocalMessage("biome", "piratevillage"), "piratevillages"
            )
            .setLocalizationKey("piratemapnew"),
         120.0F,
         true
      );
      registerItem(
         "chieftainmap",
         new WorldPresetMapItem(
            Item.Rarity.RARE, LevelIdentifier.CAVE_IDENTIFIER, 800, "chieftain", new LocalMessage("biome", "chieftain"), "foundchieftainarena"
         ),
         120.0F,
         true
      );
      registerItem(
         "cronemap",
         new WorldPresetMapItem(
            Item.Rarity.RARE, LevelIdentifier.DEEP_CAVE_IDENTIFIER, 800, "thecursedcrone", new LocalMessage("biome", "thecursedcrone"), "foundcursedcronearena"
         ),
         120.0F,
         true
      );
      registerItem(
         "sagegritmap",
         new WorldPresetMapItem(
            Item.Rarity.RARE, LevelIdentifier.DEEP_CAVE_IDENTIFIER, 800, "sageandgrit", new LocalMessage("biome", "sageandgrit"), "founddeserttemple"
         ),
         120.0F,
         true
      );
      registerItem("honeybee", new HoneyBeePlaceableItem(false), 10.0F, true);
      registerItem("queenbee", new HoneyBeePlaceableItem(true), 100.0F, true);
      registerItem("apiaryframe", new ApiaryFramePlaceableItem(), 30.0F, true);
      registerItem("importedcow", new ImportedAnimalSpawnItem(1, true, "cow"), 150.0F, false);
      registerItem("importedsheep", new ImportedAnimalSpawnItem(1, true, "sheep"), 150.0F, false);
      registerItem("importedpig", new ImportedAnimalSpawnItem(1, true, "pig"), 150.0F, false);
      registerItem("eyeinaportal", new PetFollowerPlaceableItem("petevilminion", Item.Rarity.UNCOMMON), 150.0F, true);
      registerItem("weticicle", new PetFollowerPlaceableItem("petpenguin", Item.Rarity.UNCOMMON), 250.0F, true);
      registerItem("exoticseeds", new PetFollowerPlaceableItem("petparrot", Item.Rarity.UNCOMMON), 300.0F, true);
      registerItem("magicstilts", new PetFollowerPlaceableItem("petwalkingtorch", Item.Rarity.UNCOMMON), 500.0F, true);
      registerItem("petrock", new PetFollowerPlaceableItem("petcavelingelder", Item.Rarity.RARE), 5.0F, false);
      registerItem("squeakytoy", new PetFollowerPlaceableItem("petpug", Item.Rarity.UNCOMMON), 500.0F, false);
      registerItem("grizzlycub", new PetFollowerPlaceableItem("petgrizzlybearcub", Item.Rarity.RARE), 5.0F, true);
      registerItem("thrumbohorn", new ThrumboHornPlaceableItem(), 250.0F, true);
      registerItem("dragonegg", new PetFollowerPlaceableItem("petdragonwhelp", Item.Rarity.RARE), 500.0F, true, false);
      registerItem("frozenberry", new PetFollowerPlaceableItem("peticiclearmadillo", Item.Rarity.RARE), 500.0F, true, false);
      registerItem("demonheart", new DemonHeartItem(), 30.0F, true);
      registerItem("spiderheart", new SpiderHeartItem(), 30.0F, true);
      registerItem("runicheart", new RunicHeartItem(), 30.0F, true);
      registerItem("guardianheart", new GuardianHeartItem(), 30.0F, true);
      registerItem("cryoheart", new CryoHeartItem(), 30.0F, true);
      registerItem("wardenheart", new WardenHeartItem(), 30.0F, true);
      registerItem("emptypendant", new TrinketSlotsIncreaseItem(5), 40.0F, true, true, "piratesheath", "wizardsocket", "wormholelocket");
      registerItem("piratesheath", new TrinketSlotsIncreaseItem(6), 75.0F, true, true, "wizardsocket", "wormholelocket");
      registerItem("wizardsocket", new TrinketSlotsIncreaseItem(7), 100.0F, true, true, "wormholelocket");
      registerItem("wormholelocket", new TrinketSlotsIncreaseItem(8), 150.0F, true, true);
      registerItem("demoncloak", new ItemSetsIncreaseItem(2), 50.0F, true, true, "arcanearmory", "abyssalcloak");
      registerItem("arcanearmory", new ItemSetsIncreaseItem(3), 300.0F, true, true, "abyssalcloak");
      registerItem("abyssalcloak", new ItemSetsIncreaseItem(4), 200.0F, true, true);
      registerItem("lifeelixir", new LifeElixirItem(), 50.0F, true);
      registerItem("greaterlifeelixir", new GreaterLifeElixirItem(), 50.0F, true);
      registerItem("workinprogress", new WorkInProgressItem(), 1.0F, true, false);
      registerItem("inctrinkets", new TestChangeTrinketSlotsItem(1), 0.0F, false);
      registerItem("dectrinkets", new TestChangeTrinketSlotsItem(-1), 0.0F, false);
      registerItem("incitemsets", new TestChangeItemSetsItem(1), 0.0F, false);
      registerItem("decitemsets", new TestChangeItemSetsItem(-1), 0.0F, false);
      registerItem("fireflyyellow", new FireflySpawnItem(FireflyMob.FireflyType.YELLOW), 10.0F, true);
      registerItem("fireflygreen", new FireflySpawnItem(FireflyMob.FireflyType.GREEN), 10.0F, true);
      registerItem("fireflyblue", new FireflySpawnItem(FireflyMob.FireflyType.BLUE), 10.0F, true);
      registerItem("butterflyred", new ButterflySpawnItem(ButterflyMob.SmallButterflyType.RED), 10.0F, true);
      registerItem("butterflyorange", new ButterflySpawnItem(ButterflyMob.SmallButterflyType.ORANGE), 10.0F, true);
      registerItem("butterflyyellow", new ButterflySpawnItem(ButterflyMob.SmallButterflyType.YELLOW), 10.0F, true);
      registerItem("butterflygreen", new ButterflySpawnItem(ButterflyMob.SmallButterflyType.GREEN), 10.0F, true);
      registerItem("butterflyblue", new ButterflySpawnItem(ButterflyMob.SmallButterflyType.BLUE), 10.0F, true);
      registerItem("butterflypurple", new ButterflySpawnItem(ButterflyMob.SmallButterflyType.PURPLE), 10.0F, true);
      registerItem("butterflyblack", new ButterflySpawnItem(ButterflyMob.SmallButterflyType.BLACK), 10.0F, true);
      registerItem("butterflywhite", new ButterflySpawnItem(ButterflyMob.SmallButterflyType.WHITE), 10.0F, true);
      registerItem("butterflyheart", new ButterflySpawnItem(ButterflyMob.SmallButterflyType.HEART), 10.0F, true, false);
      registerItem("butterflyfrog", new ButterflySpawnItem(ButterflyMob.SmallButterflyType.FROG), 10.0F, true, false);

      for (GameMusic music : MusicRegistry.getMusic()) {
         registerItem(music.getStringID() + "vinyl", new VinylItem(music), 50.0F, true, false);
      }

      registerItem("zombiearm", new ZombieArmQuestItem(), 0.0F, true, false);
      registerItem("goblinring", new GoblinRingQuestItem(), 0.0F, true, false);
      registerItem("swampeel", new SwampEelQuestItem(), 0.0F, true, false);
      registerItem("babyshark", new BabySharkQuestItem(), 0.0F, true, false);
      registerItem("babyswordfish", new BabySwordfishQuestItem(), 0.0F, true, false);
      registerItem("frozenbeard", new FrozenBeardQuestItem(), 0.0F, true, false);
      registerItem("spiderleg", new SpiderLegQuestItem(), 0.0F, true, false);
      registerItem("crabclaw", new CrabClawQuestItem(), 0.0F, true, false);
      registerItem("sandray", new SandRayQuestItem(), 0.0F, true, false);
      registerItem("fakefangs", new FakeFangsQuestItem(), 0.0F, true, false);
      registerItem("slimechunk", new SlimeChunkQuestItem(), 0.0F, true, false);
      registerItem("enchantedcollar", new EnchantedCollarQuestItem(), 0.0F, true, false);
      registerItem("apprenticescroll", new ApprenticeScrollQuestItem(), 0.0F, true, false);
      registerItem("darkgem", new DarkGemQuestItem(), 0.0F, true, false);
      registerItem("slimylauncher", new SlimyLauncherQuestItem(), 0.0F, true, false);
      registerItem("mummysbandage", new MummysBandageQuestItem(), 0.0F, true, false);
      registerItem("magicsand", new MagicSandQuestItem(), 0.0F, true, false);
      registerItem("caveoyster", new CaveOysterQuestItem(), 0.0F, true, false);
      registerItem("capturedspirit", new CapturedSpiritQuestItem(), 0.0F, true, false);
      registerItem("pegleg", new PegLegQuestItem(), 0.0F, true, false);
      registerItem("eyepatch", new EyePatchQuestItem(), 0.0F, true, false);
      registerItem("rumbottle", new RumBottleQuestItem(), 0.0F, true, false);
      registerItem("deepspiritswab", new DeepSpiritSwabQuestItem(), 0.0F, true, false);
      registerItem("brokenlimb", new BrokenLimbQuestItem(), 0.0F, true, false);
      registerItem("feraltail", new FeralTailQuestItem(), 0.0F, true, false);
      registerItem("razoricicle", new RazorIcicleQuestItem(), 0.0F, true, false);
      registerItem("slimesample", new SlimeSampleQuestItem(), 0.0F, true, false);
      registerItem("soakedbow", new SoakedBowQuestItem(), 0.0F, true, false);
      registerItem("wormtooth", new WormToothQuestItem(), 0.0F, true, false);
      registerItem("crawlersfoot", new CrawlersFootQuestItem(), 0.0F, true, false);
      registerItem("swingspriteattack", new SwingSpriteAttackItem(), 0.0F, false);
      registerItem("workspriteattack", new WorkSpriteAttackItem(), 0.0F, false);
   }

   protected void onRegister(ItemRegistry.ItemRegistryElement object, int id, String stringID, boolean isReplace) {
      for (Integer globalIngredient : object.item.getGlobalIngredients()) {
         GlobalIngredientRegistry.getGlobalIngredient(globalIngredient).registerItemID(id);
      }

      object.item.registerItemCategory();
   }

   @Override
   protected void onRegistryClose() {
      for (ItemRegistry.ItemRegistryElement item : this.getElements()) {
         item.displayName = item.item.getNewLocalization();
      }

      for (ItemRegistry.ItemRegistryElement element : this.getElements()) {
         element.item.onItemRegistryClosed();
      }

      totalItems = 0;
      totalItemsObtainable = 0;
      totalStatItemsObtainable = 0;
      totalTrinkets = 0;

      for (ItemRegistry.ItemRegistryElement e : this.getElements()) {
         totalItems++;
         if (e.isObtainable) {
            totalItemsObtainable++;
            if (e.countInStats) {
               totalStatItemsObtainable++;
            }

            if (e.item.isTrinketItem()) {
               totalTrinkets++;
            }
         }

         for (String itemStringID : e.isObtainedByOtherItemStringIDs) {
            ItemRegistry.ItemRegistryElement element = this.getElement(itemStringID);
            if (element != null) {
               element.obtainsOtherItemStringIDs.add(e.getStringID());
            }
         }
      }
   }

   public static void calculateBrokerValues() {
      RecipeBrokerValueCompute compute = Recipes.getDefaultBrokerValueCompute();

      for (ItemRegistry.ItemRegistryElement element : instance.getElements()) {
         if (element.setBrokerValue < 0.0F) {
            compute.addItem(element.getID(), Math.abs(element.setBrokerValue));
         }
      }

      compute.calculate((itemID, brokerValue) -> instance.getElement(itemID).brokerValue = brokerValue);
   }

   public static Stream<Item> streamItems() {
      return instance.streamElements().map(e -> e.item);
   }

   public static List<Item> getItems() {
      return streamItems().collect(Collectors.toList());
   }

   public static List<Item> getCreativeItems() {
      return streamItems().filter(ItemRegistry::isValidCreativeItem).collect(Collectors.toList());
   }

   public static boolean isValidCreativeItem(Item item) {
      return isValidCreativeItem(item.getID());
   }

   public static boolean isValidCreativeItem(int id) {
      return isObtainableInCreative(id);
   }

   public static GameMessage getLocalization(int id) {
      return id == -1 ? new StaticMessage("N/A") : instance.getElement(id).displayName;
   }

   public static GameMessage getLocalization(String stringID) {
      return getLocalization(getItemID(stringID));
   }

   public static String getDisplayName(int id) {
      return id == -1 ? null : Objects.requireNonNull(getLocalization(id)).translate();
   }

   public static String getDisplayName(String stringID) {
      return getDisplayName(getItemID(stringID));
   }

   public static int registerItem(String stringID, Item item, float brokerValue, boolean isObtainable) {
      return registerItem(stringID, item, brokerValue, isObtainable, isObtainable);
   }

   public static int registerItem(
      String stringID, Item item, float brokerValue, boolean isObtainable, boolean countInStats, String... isObtainedByOtherItemStringIDs
   ) {
      return registerItem(stringID, item, brokerValue, isObtainable, countInStats, isObtainable, isObtainedByOtherItemStringIDs);
   }

   public static int registerItem(
      String stringID,
      Item item,
      float brokerValue,
      boolean isObtainable,
      boolean countInStats,
      boolean isObtainableInCreative,
      String... isObtainedByOtherItemStringIDs
   ) {
      return registerItem(stringID, item, brokerValue, isObtainable, countInStats, isObtainableInCreative, Arrays.asList(isObtainedByOtherItemStringIDs));
   }

   public static int registerItem(
      String stringID, Item item, float brokerValue, boolean isObtainable, boolean countInStats, List<String> isObtainedByOtherItemStringIDs
   ) {
      return registerItem(stringID, item, brokerValue, isObtainable, countInStats, isObtainable, isObtainedByOtherItemStringIDs);
   }

   public static int registerItem(
      String stringID,
      Item item,
      float brokerValue,
      boolean isObtainable,
      boolean countInStats,
      boolean isObtainableInCreative,
      List<String> isObtainedByOtherItemStringIDs
   ) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register items");
      } else {
         return instance.register(
            stringID,
            new ItemRegistry.ItemRegistryElement(item, brokerValue, isObtainable, isObtainableInCreative, countInStats, isObtainedByOtherItemStringIDs)
         );
      }
   }

   public static int replaceItem(String stringID, Item item, float brokerValue, boolean isObtainable) {
      return replaceItem(stringID, item, brokerValue, isObtainable, isObtainable);
   }

   public static int replaceItem(
      String stringID, Item item, float brokerValue, boolean isObtainable, boolean countInStats, String... isObtainedByOtherItemStringIDs
   ) {
      return replaceItem(stringID, item, brokerValue, isObtainable, countInStats, isObtainable, isObtainedByOtherItemStringIDs);
   }

   public static int replaceItem(
      String stringID,
      Item item,
      float brokerValue,
      boolean isObtainable,
      boolean countInStats,
      boolean isObtainableInCreative,
      String... isObtainedByOtherItemStringIDs
   ) {
      return replaceItem(stringID, item, brokerValue, isObtainable, countInStats, isObtainableInCreative, Arrays.asList(isObtainedByOtherItemStringIDs));
   }

   public static int replaceItem(
      String stringID, Item item, float brokerValue, boolean isObtainable, boolean countInStats, List<String> isObtainedByOtherItemStringIDs
   ) {
      return replaceItem(stringID, item, brokerValue, isObtainable, countInStats, isObtainable, isObtainedByOtherItemStringIDs);
   }

   public static int replaceItem(
      String stringID,
      Item item,
      float brokerValue,
      boolean isObtainable,
      boolean countInStats,
      boolean isObtainableInCreative,
      List<String> isObtainedByOtherItemStringIDs
   ) {
      return instance.replace(
         stringID, new ItemRegistry.ItemRegistryElement(item, brokerValue, isObtainable, isObtainableInCreative, countInStats, isObtainedByOtherItemStringIDs)
      );
   }

   public static Item getItem(String stringID) {
      return getItem(getItemID(stringID));
   }

   public static Item getItem(int id) {
      if (id == -1) {
         return null;
      }

      ItemRegistry.ItemRegistryElement e = instance.getElement(id);
      return e == null ? null : e.item;
   }

   public static int getItemID(String stringID) {
      try {
         return instance.getElementIDRaw(stringID);
      } catch (NoSuchElementException e) {
         return -1;
      }
   }

   public static String getItemStringID(int id) {
      return instance.getElementStringID(id);
   }

   public static boolean itemExists(String stringID) {
      try {
         return instance.getElementIDRaw(stringID) >= 0;
      } catch (NoSuchElementException e) {
         return false;
      }
   }

   public static boolean isObtainable(int id) {
      return id == -1 ? false : instance.getElement(id).isObtainable;
   }

   public static boolean isObtainableInCreative(int id) {
      return id == -1 ? false : instance.getElement(id).isObtainableInCreative;
   }

   public static List<String> getIsObtainedByOtherItemStringIDs(String itemStringID) {
      return getIsObtainedByOtherItemStringIDs(getItemID(itemStringID));
   }

   public static List<String> getIsObtainedByOtherItemStringIDs(int itemID) {
      return itemID == -1 ? Collections.emptyList() : instance.getElement(itemID).isObtainedByOtherItemStringIDs;
   }

   public static List<String> getObtainsOtherItemStringIDs(String itemStringID) {
      return getObtainsOtherItemStringIDs(getItemID(itemStringID));
   }

   public static List<String> getObtainsOtherItemStringIDs(int itemID) {
      return itemID == -1 ? Collections.emptyList() : instance.getElement(itemID).obtainsOtherItemStringIDs;
   }

   public static boolean countsInStats(int id) {
      return id == -1 ? false : instance.getElement(id).countInStats;
   }

   public static float getBrokerValue(int id) {
      return id == -1 ? 0.0F : instance.getElement(id).brokerValue;
   }

   public static LoadedMod getItemMod(int id) {
      return id == -1 ? null : instance.getElement(id).mod;
   }

   public static int getTotalItemsObtainable() {
      return totalItemsObtainable;
   }

   public static int getTotalStatItemsObtainable() {
      return totalStatItemsObtainable;
   }

   public static int getTotalItems() {
      return totalItems;
   }

   public static int getTotalTrinkets() {
      return totalTrinkets;
   }

   protected static class ItemRegistryElement implements IDDataContainer {
      public final Item item;
      public float setBrokerValue;
      public float brokerValue;
      public final boolean isObtainable;
      public final boolean isObtainableInCreative;
      public final boolean countInStats;
      public final List<String> isObtainedByOtherItemStringIDs;
      public final ArrayList<String> obtainsOtherItemStringIDs = new ArrayList<>();
      public final LoadedMod mod;
      public GameMessage displayName;

      public ItemRegistryElement(
         Item item, float brokerValue, boolean isObtainable, boolean isObtainableInCreative, boolean countInStats, List<String> isObtainedByOtherItemStringIDs
      ) {
         this.item = item;
         this.setBrokerValue = brokerValue;
         this.brokerValue = Math.max(brokerValue, 0.0F);
         this.isObtainable = isObtainable;
         this.isObtainableInCreative = isObtainableInCreative;
         this.countInStats = countInStats;
         this.isObtainedByOtherItemStringIDs = isObtainedByOtherItemStringIDs;
         this.mod = LoadedMod.getRunningMod();
      }

      @Override
      public IDData getIDData() {
         return this.item.idData;
      }
   }
}
