package localforest.branding;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import necesse.engine.GameLaunch;
import necesse.engine.GameLoadingScreen;
import necesse.engine.GlobalData;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.platforms.sharedOnPC.window.GLFWGameWindow;
import necesse.engine.save.WorldSave;
import necesse.engine.network.server.ServerCreationSettings;
import necesse.engine.state.MainMenu;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.Renderer;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.forms.presets.MainMenuForm;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.ui.ButtonColor;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** Independent title presentation over the native menu, connection and save systems. */
public final class MosslumeBranding {
    private static GameTexture title;
    private static final Map<String, GameTexture> buttons = new HashMap<>();
    private static volatile boolean ready;
    private static boolean loggedTitle;
    private static long qaTitleStarted;
    private static int qaTitleFrames;
    private static boolean qaCaptured;
    private static boolean qaEntered;
    private static boolean qaFailed;
    private static int qaWidth, qaHeight;

    public static boolean enabled() {
        return GameLaunch.launchOptions != null && GameLaunch.launchOptions.containsKey("islandprofile") && (Boolean.getBoolean("mosslume")
            || GameLaunch.launchOptions.containsKey("mosslume"));
    }

    public static void initResources() {
        if (!enabled()) return;
        title = GameTexture.fromFile("mosslume/branding/title");
        for (String state : new String[]{"normal", "hover", "pressed", "disabled"}) {
            buttons.put(state, GameTexture.fromFile("mosslume/branding/button-" + state));
        }
        GameLoadingScreen.keyArt = title;
        // The approved title art already contains the Mosslume logo.
        GameLoadingScreen.logo = null;
        ready = true;
        setWindowTitle(WindowManager.getWindow());
        System.out.println("MOSSLUME_TITLE_ART_READY source=title-screen-v1 nativeMenu=true");
    }

    public static void postInit() {
        if (enabled()) setWindowTitle(WindowManager.getWindow());
    }

    public static void setWindowTitle(GameWindow window) {
        if (!enabled() || !(window instanceof GLFWGameWindow)) return;
        long handle = ((GLFWGameWindow) window).getGlfwWindow();
        if (handle != 0L) GLFW.glfwSetWindowTitle(handle, "Mosslume · 苔光之岛");
    }

    public static void install(MainMenuForm menu) {
        if (!enabled()) return;
        Form form = menu.mainForm;
        form.clearComponents();
        form.setWidth(330);
        form.setHeight(268);
        form.drawBase = false;
        form.drawEdge = false;
        menu.mainSideButtons.setHidden(true);
        menu.dlcSideButtons.setHidden(true);
        menu.subscribeNewsletter.setHidden(true);

        MosslumeNewWorldForm newWorld = menu.addComponent(new MosslumeNewWorldForm(settings -> {
            GameLaunch.useCharacter = "";
            menu.mainMenu.startSingleplayer(null, settings, null);
        }, menu.mainMenu::addNotice, () -> menu.makeCurrent(menu.main)));
        MosslumeWorldSelect worlds = menu.addComponent(new MosslumeWorldSelect(menu.mainMenu, () -> menu.makeCurrent(menu.main)));
        form.addComponent(new MosslumeButton("新的世界", 15, 6, 300))
            .onClicked(event -> { menu.makeCurrent(newWorld); newWorld.onStarted(); });
        form.addComponent(new MosslumeButton("继续 · 世界存档", 15, 58, 300))
            .onClicked(event -> { worlds.loadWorlds(); menu.makeCurrent(worlds); });
        FormTextButton enter = form.addComponent(new MosslumeButton("原有小岛 · 画面对照", 15, 110, 300));
        enter.setCooldown(1200);
        enter.prioritizeControllerFocus();
        enter.onClicked(event -> enterIsland(menu.mainMenu));
        form.addComponent(new MosslumeButton("设置", 15, 162, 300))
            .onClicked(event -> menu.makeCurrent(menu.settings));
        FormTextButton exit = form.addComponent(new MosslumeButton("退出游戏", 15, 214, 300));
        exit.onClicked(event -> WindowManager.getWindow().requestClose());
        exit.controllerDownFocus = exit;
        position(menu, WindowManager.getWindow());
        System.out.println("MOSSLUME_TITLE_MENU_READY buttons=newWorld,saves,baseline,settings,exit");
    }

    public static void position(MainMenuForm menu, GameWindow window) {
        if (!enabled() || window == null) return;
        int h = menu.mainForm.getHeight();
        int centerY = Math.min(window.getHudHeight() - h / 2 - 42,
            Math.max(h / 2 + 12, (int)(window.getHudHeight() * 0.76f)));
        menu.mainForm.setPosMiddle(window.getHudWidth() / 2, centerY);
    }

    /** Same find/create + startSingleplayer route used by the engine's -load launch option. */
    public static void enterIsland(MainMenu menu) {
        if (!enabled()) return;
        if (menu.getClient() != null && !menu.getClient().isDisconnected()) return;
        try {
            WorldSave save = WorldSave.findOrCreateNewWorld("ForestIsland");
            MosslumeSaveUpgrade.prepare(save);
            // Reuse the first local character, preferring this authentication's last character.
            // IslandMod ensures a starter character for a fresh profile.
            GameLaunch.useCharacter = "";
            menu.startSingleplayer(save, new ServerCreationSettings(save.filePath), null);
            System.out.println("MOSSLUME_ENTER_ISLAND nativeSave=" + save.filePath);
        } catch (Exception error) {
            menu.addNotice("小岛暂时未能载入，请重试。\n" + error.getMessage());
            System.err.println("MOSSLUME_ENTER_ISLAND_FAILED " + error);
            error.printStackTrace();
        }
    }

    /** Runs before native HUD/forms, keeping warnings and connection dialogs fully functional. */
    public static void drawBackdrop() {
        if (!enabled() || !ready) return;
        GameWindow window = WindowManager.getWindow();
        float scale = Math.max((float)window.getHudWidth() / title.getWidth(),
            (float)window.getHudHeight() / title.getHeight());
        int w = Math.round(title.getWidth() * scale), h = Math.round(title.getHeight() * scale);
        title.initDraw().size(w, h).draw((window.getHudWidth() - w) / 2,
            (window.getHudHeight() - h) / 2);
        // Quiet, slowly rising warm motes; the source artwork remains unchanged.
        double time = System.currentTimeMillis() / 1000.0;
        for (int i = 0; i < 14; i++) {
            double phase = (time * 0.055 + i * 0.173) % 1.0;
            float alpha = (float)(Math.sin(phase * Math.PI) * 0.55);
            int x = (int)((0.08 + ((i * 0.273) % 0.84)) * window.getHudWidth()
                + Math.sin(time * 0.25 + i) * 8);
            int y = (int)(window.getHudHeight() * (0.95 - phase * 0.55));
            int size = i % 3 == 0 ? 3 : 2;
            Renderer.initQuadDraw(size, size).color(1.0F, 0.85F, 0.36F, alpha).draw(x, y);
        }
        if (!loggedTitle) {
            loggedTitle = true;
            System.out.println("MOSSLUME_TITLE_DRAWN hud=" + window.getHudWidth() + "x" + window.getHudHeight());
        }
        if (qaEnabled()) {
            if (qaTitleStarted == 0L) qaTitleStarted = System.currentTimeMillis();
            qaTitleFrames++;
        }
    }

    private static boolean qaEnabled() {
        return enabled() && GameLaunch.launchOptions.containsKey("mosslumeqa");
    }

    /**
     * Opt-in application diagnostic: samples our own displayed framebuffer, then calls
     * the same entry callback as the title button on the following native frame.
     * It neither synthesizes OS input nor claims a physical mouse-click test.
     */
    public static void qaTick(MainMenu menu) {
        if (!qaEnabled() || !ready || qaFailed || qaEntered || qaTitleStarted == 0L) return;
        if (qaCaptured) {
            qaEntered = true;
            if (System.getProperty("mosslume.refactorProbe") != null) enterRefactorProbe(menu);
            else enterIsland(menu);
            try { writeQaEvidence("native_entry_callback_invoked"); }
            catch (Exception error) { System.err.println("MOSSLUME_QA_EVIDENCE_FAILED " + error); }
            return;
        }
        if (qaTitleFrames < 3 || System.currentTimeMillis() - qaTitleStarted < 2000L) return;
        try {
            GameWindow window = WindowManager.getWindow();
            qaWidth = window.getFrameWidth(); qaHeight = window.getFrameHeight();
            if (qaWidth <= 0 || qaHeight <= 0) throw new IllegalStateException("Empty application framebuffer");
            ByteBuffer bytes = BufferUtils.createByteBuffer(qaWidth * qaHeight * 4);
            int previousReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
            try {
                GL11.glReadBuffer(GL11.GL_FRONT);
                GL11.glReadPixels(0, 0, qaWidth, qaHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, bytes);
            } finally {
                GL11.glReadBuffer(previousReadBuffer);
            }
            BufferedImage frame = new BufferedImage(qaWidth, qaHeight, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < qaHeight; y++) for (int x = 0; x < qaWidth; x++) {
                int i = (x + y * qaWidth) * 4;
                int rgb = 0xff000000 | ((bytes.get(i) & 255) << 16)
                    | ((bytes.get(i + 1) & 255) << 8) | (bytes.get(i + 2) & 255);
                frame.setRGB(x, qaHeight - 1 - y, rgb);
            }
            Path folder = Paths.get(GlobalData.appDataPath(), "evidence");
            Files.createDirectories(folder);
            ImageIO.write(frame, "png", folder.resolve("mosslume-title.png").toFile());
            qaCaptured = true;
            writeQaEvidence("title_captured_entry_scheduled_next_frame");
            System.out.println("MOSSLUME_QA_TITLE_CAPTURED " + qaWidth + "x" + qaHeight
                + " frames=" + qaTitleFrames + " enter=next_native_frame");
        } catch (Exception error) {
            qaFailed = true;
            System.err.println("MOSSLUME_QA_TITLE_CAPTURE_FAILED " + error);
            error.printStackTrace();
        }
    }

    /** Uses the creation/load callbacks above, only in an explicitly isolated integration profile. */
    private static void enterRefactorProbe(MainMenu menu) {
        if (!GlobalData.appDataPath().contains("Mosslume-QA-Refactor"))
            throw new IllegalStateException("Refactor probes require an isolated QA profile");
        try {
            String name="MosslumeRefactor";
            java.io.File existing=necesse.engine.world.World.worldExistsWithName(name);
            GameLaunch.useCharacter="";
            if(existing!=null) {
                WorldSave save=new WorldSave(existing,true,true,true);
                localforest.world.WorldDefinition.forWorld(save.getWorldEntity());
                menu.startSingleplayer(save,new ServerCreationSettings(existing),null);
            } else {
                ServerCreationSettings settings=MosslumeNewWorldForm.settings(name,
                    System.getProperty("mosslume.refactorProbe.seed","4212026"),
                    System.getProperty("mosslume.refactorProbe.preset","forest_island"),"relaxed");
                menu.startSingleplayer(null,settings,null);
            }
            System.out.println("MOSSLUME_REFACTOR_PROBE_ENTER existing="+(existing!=null));
        } catch(Exception error) {
            qaFailed=true;System.err.println("MOSSLUME_REFACTOR_PROBE_ENTER_FAILED "+error);error.printStackTrace();
            menu.addNotice("测试世界无法载入："+error.getMessage());
        }
    }

    private static void writeQaEvidence(String stage) throws Exception {
        Path folder = Paths.get(GlobalData.appDataPath(), "evidence");
        Files.createDirectories(folder);
        String json = "{\"optInFlag\":\"mosslumeqa\",\"stage\":\"" + stage
            + "\",\"framebufferWidth\":" + qaWidth + ",\"framebufferHeight\":" + qaHeight
            + ",\"titleDrawFrames\":" + qaTitleFrames + ",\"titleVisibleMillis\":"
            + (System.currentTimeMillis() - qaTitleStarted) + ",\"titleCaptured\":" + qaCaptured
            + ",\"entryCallbackInvoked\":" + qaEntered
            + ",\"physicalMouseInputTested\":false,\"scope\":\"Own native GLFW framebuffer and same enterIsland callback as title button; no OS input synthesis\"}";
        Files.write(folder.resolve("mosslume-title-qa.json"), json.getBytes(StandardCharsets.UTF_8));
    }

    /** New art, native FormButton input and controller focus behavior. */
    private static final class MosslumeButton extends FormTextButton {
        MosslumeButton(String text, int x, int y, int width) {
            super(text, x, y, width, FormInputSize.empty(44, 22), ButtonColor.BASE);
        }

        @Override
        public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
            GameTexture texture = buttons.get(!isActive() || isOnCooldown() ? "disabled"
                : isDown() && isHovering() ? "pressed" : isHovering() ? "hover" : "normal");
            int x = getX(), y = getY(), h = 44, width = getWidth();
            if (texture == null) {
                super.draw(tickManager, perspective, renderBox);
                return;
            }
            int cap = Math.round(51.0F * h / texture.getHeight());
            texture.initDraw().section(0, 51, 0, texture.getHeight()).size(cap, h).draw(x, y);
            texture.initDraw().section(51, texture.getWidth() - 51, 0, texture.getHeight())
                .size(width - 2 * cap, h).draw(x + cap, y);
            texture.initDraw().section(texture.getWidth() - 51, texture.getWidth(), 0, texture.getHeight())
                .size(cap, h).draw(x + width - cap, y);
            FontOptions font = new FontOptions(22).outline().color(new Color(250, 234, 191));
            String text = getText();
            int tx = x + (width - FontManager.bit.getWidthCeil(text, font)) / 2;
            int ty = y + 10 + (isDown() && isHovering() ? 1 : 0);
            FontManager.bit.drawString(tx, ty, text, font);
            if (isHovering()) addTooltips(perspective);
        }
    }
}
