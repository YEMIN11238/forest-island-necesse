package localforest;

import java.io.File;
import java.awt.Dimension;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;
import necesse.engine.GameLaunch;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.localization.Localization;
import necesse.engine.modLoader.annotations.ModEntry;
import necesse.engine.save.CharacterSave;
import necesse.engine.util.GameRandom;
import necesse.engine.window.DisplayMode;
import necesse.engine.platforms.Platform;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.HumanLook;
import localforest.world.IslandWorld;
import localforest.visual.IslandVisuals;

@ModEntry
public class IslandMod {
    public static final String VERSION="0.1.0";
    public static boolean enabled() { return GameLaunch.launchOptions!=null && GameLaunch.launchOptions.containsKey("islandprofile"); }
    public void init() {
        if(!enabled())return;
        IslandWorld.init();IslandNativeChecks.register();IslandVisuals.init();
        System.out.println("LOCALFOREST_INIT version="+VERSION+" kernel=Necesse-1.3.3 profile="+GlobalData.appDataPath());
    }
    public void initResources() { if(enabled())IslandVisuals.initResources(); }
    public void postInit() {
        if(!enabled())return;
        Settings.displayMode=DisplayMode.Windowed;Settings.displaySize=new Dimension(1200,900);
        Settings.sceneSize=0.85f;Settings.adjustZoomOnHighResolution=false;Settings.interfaceSize=0.75f;Settings.minimapHidden=true;
        Settings.pauseOnFocusLoss=!GameLaunch.launchOptions.containsKey("islandcapture");Settings.show1p0Notice=false;
        Settings.language=Localization.ChineseSimplified.stringID;Localization.setCurrentLang(Localization.ChineseSimplified);
        Platform.getWindowManager().updateDisplayModeAfterTick();
        ensureCharacter();IslandWorld.postInit();IslandVisuals.postInit();
        System.out.println("LOCALFOREST_READY native gameplay enabled; generated art adapter initialized");
    }
    private void ensureCharacter() {
        File folder=new File(CharacterSave.getCharacterSavesPath());folder.mkdirs();
        File[] existing=folder.listFiles((dir,name)->name.endsWith(".dat")&&!name.contains("backup"));
        if(existing!=null&&existing.length>0)return;
        PlayerMob player=new PlayerMob(0L,null);player.look=new HumanLook(new GameRandom(4212026L),false);
        player.playerName="小岛农夫";player.getInv().giveStarterItems();
        File saved=CharacterSave.saveCharacter(CharacterSave.newCharacter(player,0L),null,false);
        System.out.println("LOCALFOREST_CHARACTER_CREATED "+saved.getAbsolutePath());
    }
}
