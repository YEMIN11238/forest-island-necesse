package localforest.branding;

import necesse.engine.GameLaunch;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.server.ServerCreationSettings;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.save.LoadData;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.World;
import necesse.engine.save.WorldSave;
import necesse.engine.state.MainMenu;
import necesse.gfx.forms.presets.WorldSelectForm;
import localforest.world.WorldDefinition;

/** Retains native load/rename/backup/delete, routes creation through our world definitions. */
public final class MosslumeWorldSelect extends WorldSelectForm {
    private final Runnable back;
    public MosslumeWorldSelect(MainMenu menu, Runnable back) {
        super(menu, new StaticMessage("返回首页"));
        this.back = back;
        removeComponent(createNewForm);
        createNewForm = addComponent(new MosslumeNewWorldForm(settings -> {
            GameLaunch.useCharacter = "";
            menu.startSingleplayer(null, settings, null);
        }, menu::addNotice, () -> makeCurrent(selectForm)));
    }
    @Override public void onSelected(WorldSave save, boolean newlyCreated) {
        try {
            if (save == null) return;
            // Parse every generated seed before opening a server, even when the user
            // named or renamed this world ForestIsland. This also checks content versions.
            WorldDefinition definition = WorldDefinition.forWorld(save.getWorldEntity());
            if (definition == null && !isLegacyIsland(save)) {
                mainMenu.addNotice("这不是苔光世界存档，请从首页创建新的世界。"); return;
            }
            GameLaunch.useCharacter = "";
            ServerCreationSettings settings = save.creationSettings == null ? new ServerCreationSettings(save.filePath) : save.creationSettings;
            mainMenu.startSingleplayer(save, settings, MainMenu.ConnectFrom.SinglePlayerLoadWorld);
        } catch (Exception e) { mainMenu.addNotice("存档无法载入：" + e.getMessage()); }
    }
    /** Inspect saved metadata only: do not construct a level or generate regions in the menu. */
    private static boolean isLegacyIsland(WorldSave save) throws Exception {
        World world = save.getWorld();
        if (world == null || save.getWorldEntity() == null) return false;
        boolean openedHere = !world.fileSystem.isOpen();
        try {
            if (openedHere) world.reloadFileSystem();
            LoadData surface = world.loadLevelScript(LevelIdentifier.SURFACE_IDENTIFIER);
            if (surface == null) return false;
            String levelType = surface.getUnsafeString("stringID", "", false);
            if ("mosslumegenerated".equals(levelType)) return false;
            LoadData data = surface.getFirstLoadDataByName("GNDData");
            GNDItemMap gnd = data == null ? new GNDItemMap() : new GNDItemMap(data);
            // A generated map with a missing/corrupt native seed must not be silently
            // reclassified as the old island and extended with the legacy generator.
            if (gnd.getBoolean("mosslumeGenerated") || !gnd.getString("mosslumeWorldDefinition", "").isEmpty()) return false;
            return "localforestisland".equals(levelType)
                || gnd.getBoolean("localforestisland");
        } finally {
            if (openedHere && world.fileSystem.isOpen()) world.closeFileSystem();
        }
    }
    @Override public void onBackPressed() { back.run(); }
}
