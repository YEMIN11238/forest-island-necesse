package localforest.branding;

import java.io.File;
import java.util.function.Consumer;
import necesse.engine.GameDifficulty;
import necesse.engine.Settings;
import necesse.engine.input.InputEvent;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.server.ServerCreationSettings;
import necesse.engine.save.WorldSave;
import necesse.engine.util.GameUtils;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.engine.world.World;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormDropdownSelectionButton;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.forms.presets.NewSaveForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;
import localforest.world.WorldDefinition;

/** Native form controls and native WorldSave; only the supported creation choices change. */
public final class MosslumeNewWorldForm extends NewSaveForm {
    private Form setup;
    private FormTextInput name, seed;
    private FormDropdownSelectionButton<String> preset, rules;
    private Consumer<ServerCreationSettings> create;
    private Consumer<String> errors;
    private Runnable back;

    public MosslumeNewWorldForm(Consumer<ServerCreationSettings> create, Consumer<String> errors, Runnable back) {
        super();
        this.create = create; this.errors = errors; this.back = back;
        setup = addComponent(new Form("mosslumeNewWorld", 480, 442));
        setup.addComponent(new FormLabel("新的苔光世界", new FontOptions(24), 0, 240, 20));
        label("世界名称", 62);
        name = setup.addComponent(new FormTextInput(24, 87, FormInputSize.SIZE_32_TO_40, 432, 40));
        name.setRegexMatchFull(GameUtils.validFileNamePattern);
        name.setText("苔光营地");
        label("世界种子 · 相同种子生成相同地形", 132);
        seed = setup.addComponent(new FormTextInput(24, 157, FormInputSize.SIZE_32_TO_40, 432, 64));
        seed.setText(ServerCreationSettings.getNewRandomSpawnSeed());
        label("地图形态", 202);
        preset = setup.addComponent(new FormDropdownSelectionButton<String>(24, 227, FormInputSize.SIZE_32_TO_40, ButtonColor.BASE, 208));
        preset.options.add("forest_island", new StaticMessage("森林海岛"));
        preset.options.add("woodland", new StaticMessage("林地"));
        preset.setSelected("forest_island", new StaticMessage("森林海岛"));
        setup.addComponent(new FormLabel("生存规则", new FontOptions(16), -1, 248, 202));
        rules = setup.addComponent(new FormDropdownSelectionButton<String>(248, 227, FormInputSize.SIZE_32_TO_40, ButtonColor.BASE, 208));
        rules.options.add("standard", new StaticMessage("标准生存"));
        rules.options.add("relaxed", new StaticMessage("宽松生存"));
        rules.setSelected("standard", new StaticMessage("标准生存"));
        setup.addComponent(new FormLabel("与伙伴建设营地，探索有限的随机世界。\n伙伴默认重伤可救援。模型服务尚未连接。",
            new FontOptions(16), -1, 24, 282, 432));
        FormTextButton begin = setup.addComponent(new FormTextButton("创建世界", 24, 372, 208));
        begin.setCooldown(1500); begin.onClicked(e -> createSelected());
        setup.addComponent(new FormTextButton("返回", 248, 372, 208)).onClicked(e -> backPressed());
        makeCurrent(setup); onWindowResized(WindowManager.getWindow());
    }

    private void label(String text, int y) { setup.addComponent(new FormLabel(text, new FontOptions(16), -1, 24, y)); }

    private void createSelected() {
        String requestedName = name.getText().trim(), userSeed = seed.getText().trim();
        if (requestedName.isEmpty() || !GameUtils.validFileNamePattern.matcher(requestedName).matches()
            || WorldSave.isLatestBackup(requestedName)) { error("请填写有效的世界名称。"); return; }
        if (userSeed.isEmpty()) { error("请填写世界种子。"); return; }
        try { createPressed(settings(requestedName, userSeed, preset.getSelected(), rules.getSelected())); }
        catch (Exception e) { error("世界暂时未能创建：" + e.getMessage()); }
    }

    /** Shared with the opt-in native integration probe, never rewrites an existing save. */
    public static ServerCreationSettings settings(String name, String seed, String preset, String rules) throws Exception {
        String safe = WorldSave.addNumberSuffixIfExists(name);
        File file = new File(World.getWorldsPath(), safe + (Settings.zipSaves ? ".zip" : ""));
        String encoded = WorldDefinition.encodeSeed(seed, preset, rules);
        World temporary = World.getSaveDataWorld(file, false);
        try {
            temporary.settings.difficulty = "relaxed".equals(rules) ? GameDifficulty.CASUAL : GameDifficulty.CLASSIC;
            temporary.settings.canSettlersDie = false;
            temporary.settings.playerHunger = true;
            temporary.settings.survivalMode = true;
            temporary.settings.unloadSettlements = false;
            temporary.settings.saveSettings();
        } finally { temporary.closeFileSystem(); }
        return new ServerCreationSettings(file, encoded, false);
    }

    @Override public void createPressed(ServerCreationSettings settings) { create.accept(settings); }
    @Override public void error(String message) { if (errors != null) errors.accept(message); }
    @Override public void backPressed() { if (back != null) back.run(); }
    @Override public void onStarted() { if (setup != null) { makeCurrent(setup); name.setTyping(true); } }
    @Override public void reset() { if (seed != null) seed.setText(ServerCreationSettings.getNewRandomSpawnSeed()); }
    @Override public void submitEscapeEvent(InputEvent event) { backPressed(); event.use(); }
    @Override public void onWindowResized(GameWindow window) {
        super.onWindowResized(window);
        if (setup != null && window != null) setup.setPosMiddle(window.getHudWidth()/2, window.getHudHeight()/2);
    }
}
