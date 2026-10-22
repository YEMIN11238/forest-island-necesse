package localforest;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import localforest.visual.CrownWindRenderer;
import necesse.engine.GameLaunch;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.client.Client;
import necesse.engine.state.MainGame;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.engine.window.DisplayMode;
import necesse.gfx.camera.MainGameCamera;
import necesse.level.maps.Level;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL11;

/**
 * Opt-in visual instrumentation only. The first pass has no readback or image encoding;
 * the later capture pass is explicitly excluded from frame-time statistics.
 * GL_FRONT is the previously presented native frame, not an off-screen mock-up.
 */
public final class VisualSceneProbe {
    private enum Phase { WARMUP, PERFORMANCE, CAPTURE, DRAIN, DONE }
    private static Phase phase = Phase.WARMUP;
    private static boolean initialized, failed, shutdownRequested;
    private static Path output;
    private static long phaseStart, lastPerfFrame, baseWorldMillis, startWallMillis;
    private static long captureStart, nextCapture, perfHeapStart, perfHeapEnd;
    private static long perfGcStart, perfGcEnd;
    private static double animationSeconds;
    private static final List<Double> frameMillis = new ArrayList<>();
    private static final StringBuilder frameCsv = new StringBuilder(
        "frame,file,capture_wall_seconds,previous_presented_animation_seconds,readback_ms\n");
    private static final AtomicInteger pending = new AtomicInteger(), written = new AtomicInteger();
    private static volatile String writeError;
    private static ExecutorService writer;
    private static ByteBuffer pixels;
    private static int captured, queueSkips, dueSkips, width, height, cropX, cropY;
    private static int frameWidth, frameHeight, windowWidth, windowHeight, sceneWidth, sceneHeight;
    private static int warmupFrames;
    private static boolean snapshotTaken;
    private static boolean secondHourSnapshot;
    private static volatile necesse.engine.network.server.Server pendingStop;
    public static void stopOnServerTick(necesse.engine.network.server.Server server){
        if(pendingStop!=server)return;
        pendingStop=null;
        server.stop(stopped->{System.out.println("MOSSLUME_VISUAL_PROBE_SERVER_STOPPED");System.exit(0);});
    }
    private static double performanceElapsed, captureElapsed;
    private static String renderer, vendor, version;
    private static float rainAlpha;

    public static boolean enabled() {
        return System.getProperty("mosslume.visualProbe") != null
            || (GameLaunch.launchOptions != null && GameLaunch.launchOptions.containsKey("islandvisualprobe"));
    }

    private static double option(String name, double fallback, double minimum, double maximum) {
        try { return Math.max(minimum, Math.min(maximum,
            Double.parseDouble(System.getProperty("mosslume.visualProbe." + name, "" + fallback)))); }
        catch (RuntimeException ignored) { return fallback; }
    }

    public static void tick(MainGame game) {
        if (!enabled() || failed || phase == Phase.DONE) return;
        try {
            GameWindow window = WindowManager.getWindow();
            Client client = game.getClient();
            Level level = client.getPlayer().getLevel();
            long now = System.nanoTime();
            if (!initialized) initialize(game, window, level, now);
            // Override the test client's clock only. No inventory, map, player position or
            // server save data is changed by this probe. Both comparison runs start at 21:00.
            long activeBase=baseWorldMillis;
            boolean secondHour=phase==Phase.CAPTURE&&System.getProperty("mosslume.visualProbe.compareHour")!=null
                &&captureElapsed>=option("captureSeconds",8,2,30)*.5;
            if(secondHour)activeBase=(long)(level.getWorldEntity().hourToDayTime((float)option("compareHour",12,0,23.99))*1000f);
            level.getWorldEntity().setWorldTime(activeBase + (long)(animationSeconds * 1000.0));
            if (phase == Phase.WARMUP) {
                warmupFrames++;
                animationSeconds = (now - phaseStart) / 1.0e9;
                if (warmupFrames >= 12 && animationSeconds >= option("warmupSeconds", 4, 1, 60)) {
                    phase = Phase.PERFORMANCE; phaseStart = now; lastPerfFrame = 0;
                    perfHeapStart = heapUsed(); perfGcStart = gcCount();
                    System.out.println("MOSSLUME_VISUAL_PROBE_PERFORMANCE_START readback=false");
                }
            } else if (phase == Phase.PERFORMANCE) {
                if (lastPerfFrame != 0) frameMillis.add((now - lastPerfFrame) / 1.0e6);
                lastPerfFrame = now;
                performanceElapsed = (now - phaseStart) / 1.0e9;
                animationSeconds = performanceElapsed;
                if (performanceElapsed >= option("perfSeconds", 10, 2, 120)) {
                    perfHeapEnd = heapUsed(); perfGcEnd = gcCount();
                    writePerformance();
                    phase = Phase.CAPTURE; captureStart = now; phaseStart = now;
                    nextCapture = now; warmupFrames = 0; animationSeconds = 0;
                    System.out.println("MOSSLUME_VISUAL_PROBE_CAPTURE_START performanceExcluded=true");
                }
            } else if (phase == Phase.CAPTURE) {
                warmupFrames++;
                captureElapsed = (now - captureStart) / 1.0e9;
                int target = (int)Math.round(option("captureSeconds", 8, 2, 30)
                    * option("captureFps", 30, 1, 60));
                if (captured >= target || (captureElapsed >= option("captureSeconds", 8, 2, 30) && captured >= 60)
                    || captureElapsed >= option("captureSeconds", 8, 2, 30) * 3) {
                    phase = Phase.DRAIN; phaseStart = now; writer.shutdown();
                } else if (warmupFrames >= 3 && now >= nextCapture) {
                    long interval = Math.round(1.0e9 / option("captureFps", 30, 1, 60));
                    // Advance the original deadline, not "now + interval": resetting
                    // the deadline on every slightly late 60Hz frame halves a 30Hz
                    // capture cadence and can make a smooth animation look jerky.
                    long missed = Math.max(0, (now - nextCapture) / interval);
                    dueSkips += (int)missed;
                    nextCapture += (missed + 1) * interval;
                    if (pending.get() >= 6) queueSkips++;
                    else {
                        if (!snapshotTaken) {
                            queueImage(readFrame(0, 0, frameWidth, frameHeight), output.resolve("full-frame.png"));
                            snapshotTaken = true;
                        }
                        if(secondHour&&!secondHourSnapshot&&captureElapsed>=option("captureSeconds",8,2,30)*.5+.5){
                            queueImage(readFrame(0,0,frameWidth,frameHeight),output.resolve("comparison-hour.png"));secondHourSnapshot=true;
                        }
                        long readStart = System.nanoTime();
                        BufferedImage image = readFrame(cropX, cropY, width, height);
                        double readMillis = (System.nanoTime() - readStart) / 1.0e6;
                        String file = String.format(Locale.ROOT, "frame-%06d.png", captured);
                        queueImage(image, output.resolve("frames").resolve(file));
                        frameCsv.append(String.format(Locale.ROOT, "%d,%s,%.9f,%.9f,%.6f\n",
                            captured, file, captureElapsed, animationSeconds, readMillis));
                        captured++;
                    }
                }
                // Continuous wall time, not capture index: wind remains smooth between
                // snapshots. CSV timestamps describe the actual irregular sampling.
                animationSeconds = captureElapsed;
            } else if (phase == Phase.DRAIN && (writer.isTerminated() || now - phaseStart > TimeUnit.SECONDS.toNanos(30))) {
                Files.write(output.resolve("frames.csv"), frameCsv.toString().getBytes(StandardCharsets.UTF_8));
                writeManifest(window, level);
                phase = Phase.DONE;
                CrownWindRenderer.clearProbeTime();
                System.out.println("MOSSLUME_VISUAL_PROBE_COMPLETE " + output + " captures=" + captured
                    + " written=" + written.get() + " error=" + writeError);
                if (Boolean.parseBoolean(System.getProperty("mosslume.visualProbe.stop", "true")) && !shutdownRequested) {
                    shutdownRequested = true;
                    // Server.stop sets its stop flag before installing the save handler.
                    // Queue this opt-in probe's stop onto the server tick to avoid a race
                    // where the server observes the flag and exits before the save exists.
                    if (client.getLocalServer() != null) pendingStop=client.getLocalServer();
                    else window.requestClose();
                }
            }
            if (phase != Phase.DONE) CrownWindRenderer.setProbeTimeSeconds(animationSeconds);
        } catch (Exception error) {
            failed = true; CrownWindRenderer.clearProbeTime();
            if (writer != null) writer.shutdown();
            System.err.println("MOSSLUME_VISUAL_PROBE_FAILED " + error);
            error.printStackTrace();
        }
    }

    private static void initialize(MainGame game, GameWindow window, Level level, long now) throws Exception {
        String specified = System.getProperty("mosslume.visualProbe");
        if (specified == null) specified = GameLaunch.launchOptions.get("islandvisualprobe");
        output = specified == null || specified.trim().isEmpty()
            ? Paths.get(GlobalData.appDataPath(), "visual-probe") : Paths.get(specified);
        Files.createDirectories(output.resolve("frames"));
        int requestedW = (int)option("windowWidth", 1200, 320, 2560);
        int requestedH = (int)option("windowHeight", 900, 240, 1800);
        long handle = GLFW.glfwGetCurrentContext();
        if (Boolean.getBoolean("mosslume.visualProbe.fullscreen")) {
            // The engine deliberately maps all macOS display modes to a window;
            // exercise an actual monitor-sized output only in this opt-in probe.
            long monitor = GLFW.glfwGetPrimaryMonitor();
            GLFWVidMode mode = monitor == 0 ? null : GLFW.glfwGetVideoMode(monitor);
            if (mode == null) throw new IllegalStateException("No active display for fullscreen probe");
            Settings.displayMode = DisplayMode.Fullscreen;
            GLFW.glfwSetWindowMonitor(handle, monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate());
        } else if (handle != 0L && (window.getWidth() != requestedW || window.getHeight() != requestedH))
            GLFW.glfwSetWindowSize(handle, requestedW, requestedH);
        Settings.pauseOnFocusLoss = false;
        // Do not let unrelated chat/tool focus silently turn a 60Hz test into
        // the engine's intentional 30Hz background power-saving mode.
        Settings.savePerformanceOnFocusLoss = false;
        Settings.maxFPS = (int)option("maxFps", 60, 0, 240);
        Settings.alwaysRain = false;
        // A fixed camera is reapplied by the engine in prepareDrawScene(), after frameTick.
        game.setCamera(new FixedCamera((int)option("centerX", 1536, 0, 10000),
            (int)option("centerY", 1216, 0, 10000)));
        baseWorldMillis = (long)(level.getWorldEntity().hourToDayTime(
            (float)option("hour", 21, 0, 23.99)) * 1000f);
        // Start the clear-weather transition before the fixed test clock. Otherwise
        // resetting animation time between passes would reintroduce a ten-second
        // rain fade even when the source save was already dry.
        level.getWorldEntity().setWorldTime(baseWorldMillis - 20000);
        level.weatherLayer.setRaining(false);
        level.getWorldEntity().setWorldTime(baseWorldMillis);
        writer = Executors.newSingleThreadExecutor(task -> {
            Thread t = new Thread(task, "mosslume-visual-probe-png"); t.setDaemon(true); return t;
        });
        renderer = GL11.glGetString(GL11.GL_RENDERER);
        vendor = GL11.glGetString(GL11.GL_VENDOR);
        version = GL11.glGetString(GL11.GL_VERSION);
        phaseStart = now; startWallMillis = System.currentTimeMillis(); initialized = true;
        System.out.println("MOSSLUME_VISUAL_PROBE_START " + output + " renderer=" + renderer);
    }

    private static final class FixedCamera extends MainGameCamera {
        private final int centerX, centerY;
        FixedCamera(int x, int y) { centerX = x; centerY = y; }
        @Override public void tickCamera(TickManager tick, MainGame game, Client client) {
            centerCamera(centerX, centerY);
        }
    }

    private static BufferedImage readFrame(int x, int y, int w, int h) {
        if (pixels == null || pixels.capacity() < w * h * 4) pixels = BufferUtils.createByteBuffer(w * h * 4);
        pixels.clear();
        int previous = GL11.glGetInteger(GL11.GL_READ_BUFFER);
        try {
            GL11.glReadBuffer(GL11.GL_FRONT);
            GL11.glReadPixels(x, y, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        } finally { GL11.glReadBuffer(previous); }
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] data = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        for (int row = 0; row < h; row++) for (int col = 0; col < w; col++) {
            int i = (col + row * w) * 4;
            data[col + (h - 1 - row) * w] = ((pixels.get(i) & 255) << 16)
                | ((pixels.get(i + 1) & 255) << 8) | (pixels.get(i + 2) & 255);
        }
        return image;
    }

    private static void queueImage(BufferedImage image, Path path) {
        pending.incrementAndGet();
        writer.execute(() -> {
            try {
                if (!ImageIO.write(image, "png", path.toFile())) throw new IllegalStateException("PNG encoder unavailable");
                written.incrementAndGet();
            } catch (Exception error) { writeError = error.toString(); }
            finally { pending.decrementAndGet(); }
        });
    }

    // Capture dimensions are latched after the requested window resize has settled.
    private static void latchDimensions() {
        GameWindow w = WindowManager.getWindow();
        frameWidth = w.getFrameWidth(); frameHeight = w.getFrameHeight();
        windowWidth = w.getWidth(); windowHeight = w.getHeight();
        sceneWidth = w.getSceneWidth(); sceneHeight = w.getSceneHeight();
        width = Math.min(frameWidth, (int)option("captureWidth", 1200, 64, 5120));
        height = Math.min(frameHeight, (int)option("captureHeight", 900, 64, 3200));
        cropX = (frameWidth - width) / 2; cropY = (frameHeight - height) / 2;
    }

    private static long heapUsed() {
        Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory();
    }
    private static long gcCount() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
            .mapToLong(b -> Math.max(0, b.getCollectionCount())).sum();
    }
    private static double percentile(double[] values, double p) {
        if (values.length == 0) return 0;
        return values[Math.min(values.length - 1, Math.max(0, (int)Math.ceil(values.length * p) - 1))];
    }
    private static void writePerformance() throws Exception {
        latchDimensions();
        double[] sorted = frameMillis.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        double mean = Arrays.stream(sorted).average().orElse(0);
        StringBuilder csv = new StringBuilder("frame,interval_ms\n");
        for (int i = 0; i < frameMillis.size(); i++) csv.append(String.format(Locale.ROOT, "%d,%.6f\n", i, frameMillis.get(i)));
        Files.write(output.resolve("performance-frames.csv"), csv.toString().getBytes(StandardCharsets.UTF_8));
        String json = "{\n  \"scope\":\"Wall-clock intervals between MainGame.frameTick exits; includes engine rendering, swap/vsync and scheduler wait. Not GPU or CPU render time.\","
            + "\n  \"readPixelsDuringMeasurement\":false,\n  \"pngEncodingDuringMeasurement\":false,"
            + "\n  \"frames\":" + sorted.length + ",\n  \"elapsedSeconds\":" + number(performanceElapsed)
            + ",\n  \"meanMs\":" + number(mean) + ",\n  \"medianMs\":" + number(percentile(sorted, .5))
            + ",\n  \"p95Ms\":" + number(percentile(sorted, .95)) + ",\n  \"p99Ms\":" + number(percentile(sorted, .99))
            + ",\n  \"maximumMs\":" + number(percentile(sorted, 1)) + ",\n  \"meanFps\":" + number(mean > 0 ? 1000 / mean : 0)
            + ",\n  \"over33_333ms\":" + Arrays.stream(sorted).filter(v -> v > 1000.0 / 30).count()
            + ",\n  \"over50ms\":" + Arrays.stream(sorted).filter(v -> v > 50).count()
            + ",\n  \"over100ms\":" + Arrays.stream(sorted).filter(v -> v > 100).count()
            + ",\n  \"heapUsedStartBytes\":" + perfHeapStart + ",\n  \"heapUsedEndBytes\":" + perfHeapEnd
            + ",\n  \"gcCollections\":" + (perfGcEnd - perfGcStart) + "\n}\n";
        Files.write(output.resolve("performance.json"), json.getBytes(StandardCharsets.UTF_8));
        System.out.println("MOSSLUME_VISUAL_PROBE_PERFORMANCE_DONE frames=" + sorted.length
            + " meanMs=" + number(mean) + " p95Ms=" + number(percentile(sorted, .95)));
    }
    private static String number(double n) { return String.format(Locale.ROOT, "%.6f", n); }
    private static String quote(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
    private static void writeManifest(GameWindow window, Level level) throws Exception {
        rainAlpha = level.weatherLayer.getRainAlpha();
        String json = "{\n  \"probeVersion\":1,\n  \"build\":" + quote(IslandMod.VERSION)
            + ",\n  \"profile\":" + quote(GlobalData.appDataPath()) + ",\n  \"startedAtMillis\":" + startWallMillis
            + ",\n  \"level\":" + quote(level.getIdentifier().toString())
            + ",\n  \"centerWorldX\":" + (int)option("centerX", 1536, 0, 10000)
            + ",\n  \"centerWorldY\":" + (int)option("centerY", 1216, 0, 10000)
            + ",\n  \"windowLogicalWidth\":" + windowWidth + ",\n  \"windowLogicalHeight\":" + windowHeight
            + ",\n  \"framebufferWidth\":" + frameWidth + ",\n  \"framebufferHeight\":" + frameHeight
            + ",\n  \"sceneWidth\":" + sceneWidth + ",\n  \"sceneHeight\":" + sceneHeight
            + ",\n  \"sceneSizeSetting\":" + number(Settings.sceneSize)
            + ",\n  \"interfaceSizeSetting\":" + number(Settings.interfaceSize)
            + ",\n  \"maxFpsSetting\":" + Settings.maxFPS
            + ",\n  \"savePerformanceOnFocusLoss\":" + Settings.savePerformanceOnFocusLoss
            + ",\n  \"windowFocusedAtFinish\":" + window.isFocused()
            + ",\n  \"captureCropFramebuffer\":{\"x\":" + cropX + ",\"yFromBottom\":" + cropY
            + ",\"width\":" + width + ",\"height\":" + height + "}"
            + ",\n  \"readBuffer\":\"GL_FRONT previous presented frame\""
            + ",\n  \"capturedFrames\":" + captured + ",\n  \"writtenPngsIncludingFullFrame\":" + written.get()
            + ",\n  \"pendingWrites\":" + pending.get() + ",\n  \"writerError\":" + quote(writeError)
            + ",\n  \"captureComplete\":" + (captured >= 60 && pending.get() == 0 && writeError == null)
            + ",\n  \"captureElapsedSeconds\":" + number(captureElapsed)
            + ",\n  \"requestedCaptureFps\":" + number(option("captureFps", 30, 1, 60))
            + ",\n  \"queueBackpressureSkips\":" + queueSkips + ",\n  \"missedCaptureIntervals\":" + dueSkips
            + ",\n  \"capturePerformanceExcluded\":true,\n  \"frameTimestamps\":\"frames.csv; real sampling may be irregular, do not encode as if every sample were exactly 1/requestedCaptureFps\""
            + ",\n  \"fixedEnvironmentHour\":" + number(option("hour", 21, 0, 23.99))
            + ",\n  \"actualEnvironmentHour\":" + number(level.getWorldEntity().getDayTimeHourFloat())
            + ",\n  \"comparisonHourCaptured\":" + secondHourSnapshot
            + ",\n  \"surfaceLightingSamples\":" + localforest.visual.SceneLighting.terrainSamples
            + ",\n  \"surfaceLightingSpritePasses\":" + localforest.visual.SceneLighting.spritePasses
            + ",\n  \"dayGroundBatchDraws\":" + localforest.visual.DaylightMaterials.groundBatchDraws
            + ",\n  \"companionSpriteCounters\":" + quote(localforest.visual.CompanionSprites.counters().toString())
            + ",\n  \"naturalPathDraws\":" + localforest.visual.NaturalPathRenderer.draws
            + ",\n  \"radianceShadowFrames\":" + localforest.visual.RadianceShadowRenderer.frames
            + ",\n  \"radianceShadowGPU\":" + localforest.visual.RadianceShadowRenderer.gpuActive
            + ",\n  \"shadowSubmissionMeanMillis\":" + number(localforest.visual.RadianceShadowRenderer.meanCpuMillis)
            + ",\n  \"shoreProjectionQuads\":" + localforest.visual.RadianceShadowRenderer.shoreQuads
            + ",\n  \"captureRainAlpha\":" + number(rainAlpha)
            + ",\n  \"windLegacy\":" + Boolean.getBoolean("mosslume.wind.legacy")
            + ",\n  \"windPixelCoverage\":" + CrownWindRenderer.usesPixelCoverage()
            + ",\n  \"windMeshDraws\":" + CrownWindRenderer.meshDraws
            + ",\n  \"windFallbackDraws\":" + CrownWindRenderer.fallbackDraws
            + ",\n  \"pixelLegacy\":" + Boolean.getBoolean("mosslume.pixel.legacy")
            + ",\n  \"visualLegacy\":" + Boolean.getBoolean("mosslume.visual.legacy")
            + ",\n  \"pixelScalerInstalled\":" + localforest.visual.MosslumePixelPipeline.installed
            + ",\n  \"pixelScalerFrames\":" + localforest.visual.MosslumePixelPipeline.frames
            + ",\n  \"displayMode\":" + quote(Settings.displayMode.toString())
            + ",\n  \"actualFullscreenMonitor\":" + (GLFW.glfwGetWindowMonitor(GLFW.glfwGetCurrentContext()) != 0)
            + ",\n  \"glVendor\":" + quote(vendor) + ",\n  \"glRenderer\":" + quote(renderer) + ",\n  \"glVersion\":" + quote(version)
            + ",\n  \"javaVersion\":" + quote(System.getProperty("java.version"))
            + ",\n  \"os\":" + quote(System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"))
            + ",\n  \"physicalMouseInputTested\":false,\n  \"scope\":\"Visual capture plus frame-interval measurement only; no gameplay or save round-trip validation.\"\n}\n";
        Files.write(output.resolve("manifest.json"), json.getBytes(StandardCharsets.UTF_8));
    }
    private VisualSceneProbe() { }
}
