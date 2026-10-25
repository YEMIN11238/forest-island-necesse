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
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;
import localforest.visual.CrownWindRenderer;
import localforest.biomes.BiomeVisualTour;
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
    @Name("mosslume.RenderMeasurement")
    @Label("Mosslume no-readback frame measurement")
    @StackTrace(false)
    public static final class MeasurementEvent extends Event { public String scene; }
    private static MeasurementEvent measurement;
    private enum Phase { WARMUP, PERFORMANCE, CAPTURE, TOUR, DRAIN, DONE }
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
    private static int frameWidth, frameHeight, windowWidth, windowHeight, sceneWidth, sceneHeight, probeCenterX, probeCenterY;
    private static int warmupFrames;
    private static int focusedPerfFrames,unfocusedPerfFrames;
    private static boolean snapshotTaken;
    private static int attackPoseAttempt=-1, attackPoseAccepted, attackPoseFrames;
    private static final boolean[] attackDirectionsSeen=new boolean[4];
    private static boolean secondHourSnapshot;
    private static volatile necesse.engine.network.server.Server pendingStop;
    private static volatile long pendingStopSince;
    private static boolean stopThreadsWritten;
    private static final java.util.List<java.util.Map<String,Object>> residentSamples=new java.util.ArrayList<>();
    private static long nextResidentSample;
    public static void stopOnServerTick(necesse.engine.network.server.Server server){
        if(enabled())WeaponMatrixProbe.serverTick(server);
        if(enabled()&&Boolean.getBoolean("mosslume.visualProbe.sceneOnly")){
            long now=System.nanoTime();
            if(now>=nextResidentSample||pendingStop==server){
                nextResidentSample=now+TimeUnit.SECONDS.toNanos(5);
                server.streamClients().filter(c->c.hasSpawned()&&c.playerMob!=null).findFirst().ifPresent(c->{
                    var sample=new java.util.LinkedHashMap<String,Object>();sample.put("atNanos",now);
                    sample.put("worldHour",c.getLevel().getWorldEntity().getDayTimeHourFloat());
                    sample.put("life",localforest.companions.NpcPopulationService.lifeSummary(c.getLevel()));residentSamples.add(sample);
                });
            }
        }
        if(pendingStop!=server)return;
        pendingStop=null;
        if(!residentSamples.isEmpty())try{
            Files.writeString(Path.of(System.getProperty("mosslume.visualProbe")).resolve("resident-life.json"),RefactorNativeChecks.json(java.util.Map.of(
                "samples",residentSamples,"creatureAnimations",localforest.biomes.BiomeCreatureSprites.animationDiagnostics(),
                "scope",WeaponMatrixProbe.enabled()?"Opt-in weapon-matrix fixture in a disposable world; read-only resident sampling, five extra non-saved animation actors and QA player equipment.":"Read-only server snapshots during natural resident activity; no command fixtures, hunger changes or model calls."))+"\n");
        }catch(Exception error){System.err.println("MOSSLUME_RESIDENT_LIFE_EVIDENCE_FAILED "+error);}
        server.stop(stopped->{System.out.println("MOSSLUME_VISUAL_PROBE_SERVER_STOPPED");System.exit(0);});
    }
    private static double performanceElapsed, captureElapsed;
    private static String renderer, vendor, version;
    private static float rainAlpha;
    private static BiomeVisualTour biomeTour;

    public static boolean enabled() {
        return System.getProperty("mosslume.visualProbe") != null
            || (GameLaunch.launchOptions != null && GameLaunch.launchOptions.containsKey("islandvisualprobe"));
    }

    /** The HUD proof is present in the first captured framebuffer only. Native
     * face construction/drawing must never contaminate the performance pass. */
    static boolean wantsPortraitSnapshot() {
        return initialized && !failed && phase == Phase.CAPTURE && !snapshotTaken;
    }

    private static double option(String name, double fallback, double minimum, double maximum) {
        try { return Math.max(minimum, Math.min(maximum,
            Double.parseDouble(System.getProperty("mosslume.visualProbe." + name, "" + fallback)))); }
        catch (RuntimeException ignored) { return fallback; }
    }

    public static void tick(MainGame game) {
        // Diagnostics belong to this disposable opt-in probe, including after DONE.
        // A pending server-tick callback cannot diagnose its own blocked tick thread.
        if(enabled()&&phase==Phase.DONE&&pendingStop!=null&&pendingStopSince>0&&!stopThreadsWritten
            &&System.nanoTime()-pendingStopSince>TimeUnit.SECONDS.toNanos(10)){
            stopThreadsWritten=true;writeThreadDiagnostics("pending-stop-threads.txt","Server.tick did not consume pendingStop within 10 seconds");
        }
        if (!enabled() || failed || phase == Phase.DONE) return;
        try {
            GameWindow window = WindowManager.getWindow();
            Client client = game.getClient();
            Level level = client.getPlayer().getLevel();
            long now = System.nanoTime();
            if (!initialized) initialize(game, window, level, now);
            // Normal visual probes override the test client's clock only. Optional
            // attackPoses also exercises native client attacks in its disposable profile.
            long activeBase=baseWorldMillis;
            boolean secondHour=phase==Phase.CAPTURE&&System.getProperty("mosslume.visualProbe.compareHour")!=null
                &&captureElapsed>=option("captureSeconds",8,2,30)*.5;
            if(secondHour)activeBase=(long)(level.getWorldEntity().hourToDayTime((float)option("compareHour",12,0,23.99))*1000f);
            if(phase==Phase.TOUR)activeBase=(long)(level.getWorldEntity().hourToDayTime((float)option("biomeTourHour",12,0,23.99))*1000f);
            level.getWorldEntity().setWorldTime(activeBase + (long)(animationSeconds * 1000.0));
            if (phase == Phase.WARMUP) {
                warmupFrames++;
                animationSeconds = (now - phaseStart) / 1.0e9;
                if (warmupFrames >= 12 && animationSeconds >= option("warmupSeconds", 4, 1, 60)) {
                    phase = Phase.PERFORMANCE; phaseStart = now; lastPerfFrame = 0;
                    perfHeapStart = heapUsed(); perfGcStart = gcCount();
                    measurement=new MeasurementEvent();
                    measurement.scene=level.getIdentifier().toString();
                    measurement.begin();
                    System.out.println("MOSSLUME_VISUAL_PROBE_PERFORMANCE_START readback=false");
                }
            } else if (phase == Phase.PERFORMANCE) {
                if(window.isFocused())focusedPerfFrames++;else unfocusedPerfFrames++;
                if (lastPerfFrame != 0) frameMillis.add((now - lastPerfFrame) / 1.0e6);
                lastPerfFrame = now;
                performanceElapsed = (now - phaseStart) / 1.0e9;
                animationSeconds = performanceElapsed;
                if (performanceElapsed >= option("perfSeconds", 10, 2, 600)) {
                    perfHeapEnd = heapUsed(); perfGcEnd = gcCount();
                    measurement.end();measurement.commit();
                    writePerformance();
                    if(Boolean.getBoolean("mosslume.visualProbe.performanceOnly")){
                        phase=Phase.DRAIN;phaseStart=now;writer.shutdown();
                    } else {
                        phase = Phase.CAPTURE; captureStart = now; phaseStart = now;
                        nextCapture = now; warmupFrames = 0; animationSeconds = 0;
                        System.out.println("MOSSLUME_VISUAL_PROBE_CAPTURE_START performanceExcluded=true");
                    }
                }
            } else if (phase == Phase.CAPTURE) {
                warmupFrames++;
                captureElapsed = (now - captureStart) / 1.0e9;
                if(WeaponMatrixProbe.enabled())WeaponMatrixProbe.clientTick(client,captureElapsed);
                else if(Boolean.getBoolean("mosslume.visualProbe.attackPoses")){
                    if(!Path.of(GlobalData.appDataPath()).getFileName().toString().startsWith("Mosslume-QA-"))
                        throw new IllegalStateException("Attack pose capture requires a disposable QA profile");
                    int attempt=(int)captureElapsed;
                    if(attempt!=attackPoseAttempt){
                        attackPoseAttempt=attempt;int direction=attempt%4;
                        int[] dx={0,96,0,-96},dy={-96,0,96,0};
                        var actor=client.getPlayer();
                        if(actor.tryAttack(actor.getSelectedItemSlot(),actor.getX()+dx[direction],actor.getY()+dy[direction]))attackPoseAccepted++;
                    }
                    var actor=client.getPlayer();
                    if(actor.isAttacking&&actor.attackDir!=null){
                        attackPoseFrames++;
                        attackDirectionsSeen[localforest.visual.CharacterAnimation.direction(actor.attackDir.x,actor.attackDir.y,actor.getDir())]=true;
                    }
                }
                int target = (int)Math.round(option("captureSeconds", 8, 2, 30)
                    * option("captureFps", 30, 1, 60));
                if (captured >= target || (captureElapsed >= option("captureSeconds", 8, 2, 30) && captured >= 60)
                    || captureElapsed >= option("captureSeconds", 8, 2, 30) * 3) {
                    if(BiomeVisualTour.enabled()){
                        biomeTour=new BiomeVisualTour(game,level,now);phase=Phase.TOUR;phaseStart=now;
                    }else{phase = Phase.DRAIN; phaseStart = now; writer.shutdown();}
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
                        BufferedImage image = readFrame(cropX, cropY, width, height,(int)option("captureScale",1,1,2));
                        double readMillis = (System.nanoTime() - readStart) / 1.0e6;
                        // Uncompressed lossless frames avoid PNG compression starving
                        // the capture queue. Exported stills remain full-resolution PNG.
                        String file = String.format(Locale.ROOT, "frame-%06d.bmp", captured);
                        queueImage(image, output.resolve("frames").resolve(file));
                        frameCsv.append(String.format(Locale.ROOT, "%d,%s,%.9f,%.9f,%.6f\n",
                            captured, file, captureElapsed, animationSeconds, readMillis));
                        captured++;
                        if(WeaponMatrixProbe.enabled()&&pending.get()<4){
                            for(var shot:WeaponMatrixProbe.shots(client)){
                                int sx=game.getCamera().getDrawX(shot.x()),sy=game.getCamera().getDrawY(shot.y());
                                float rx=frameWidth/(float)sceneWidth,ry=frameHeight/(float)sceneHeight;
                                int left=Math.max(0,Math.round((sx-80)*rx)),top=Math.max(0,Math.round((sy-136)*ry));
                                int right=Math.min(frameWidth,Math.round((sx+80)*rx)),bottom=Math.min(frameHeight,Math.round((sy+24)*ry));
                                if(right>left&&bottom>top)queueImage(readFrame(left,frameHeight-bottom,right-left,bottom-top),output.resolve(shot.name()));
                            }
                        }
                    }
                }
                // Continuous wall time, not capture index: wind remains smooth between
                // snapshots. CSV timestamps describe the actual irregular sampling.
                animationSeconds = captureElapsed;
            } else if (phase == Phase.TOUR) {
                animationSeconds=captureElapsed+(now-phaseStart)/1e9;
                if(pending.get()<6){
                    String tourFile=biomeTour.ready(game,client,level,now);
                    if(tourFile!=null){queueImage(readFrame(0,0,frameWidth,frameHeight),output.resolve(tourFile));biomeTour.captured(game,level,now);}
                }
                if(biomeTour.done()){biomeTour.writeManifest(output);phase=Phase.DRAIN;phaseStart=now;writer.shutdown();}
            } else if (phase == Phase.DRAIN && (writer.isTerminated() || now - phaseStart > TimeUnit.SECONDS.toNanos(30))) {
                Files.write(output.resolve("frames.csv"), frameCsv.toString().getBytes(StandardCharsets.UTF_8));
                writeManifest(window, level);
                if(WeaponMatrixProbe.enabled())Files.writeString(output.resolve("weapon-matrix.json"),RefactorNativeChecks.json(WeaponMatrixProbe.report())+"\n");
                phase = Phase.DONE;
                CrownWindRenderer.clearProbeTime();
                System.out.println("MOSSLUME_VISUAL_PROBE_COMPLETE " + output + " captures=" + captured
                    + " written=" + written.get() + " error=" + writeError);
                if (Boolean.parseBoolean(System.getProperty("mosslume.visualProbe.stop", "true")) && !shutdownRequested) {
                    shutdownRequested = true;
                    // Server.stop sets its stop flag before installing the save handler.
                    // Queue this opt-in probe's stop onto the server tick to avoid a race
                    // where the server observes the flag and exits before the save exists.
                    if (client.getLocalServer() != null) {pendingStopSince=now;pendingStop=client.getLocalServer();}
                    else window.requestClose();
                }
            }
            if (phase != Phase.DONE) CrownWindRenderer.setProbeTimeSeconds(
                option("fixedWindSeconds",animationSeconds,0,100000));
        } catch (Exception error) {
            failed = true; CrownWindRenderer.clearProbeTime();
            if (writer != null) writer.shutdown();
            System.err.println("MOSSLUME_VISUAL_PROBE_FAILED " + error);
            error.printStackTrace();
        }
    }

    /** Read-only snapshots for failed QA streaming/stop handoffs; never active in normal play. */
    public static void writeThreadDiagnostics(String file,String reason){
        if(!enabled()||output==null)return;
        try{
            StringBuilder text=new StringBuilder("reason=").append(reason).append('\n');
            var stacks=Thread.getAllStackTraces();
            var threads=new java.util.ArrayList<>(stacks.keySet());threads.sort(java.util.Comparator.comparingLong(Thread::getId));
            for(Thread thread:threads){
                text.append('\n').append('"').append(thread.getName()).append("\" id=").append(thread.getId()).append(" state=").append(thread.getState()).append(" daemon=").append(thread.isDaemon()).append('\n');
                for(StackTraceElement frame:stacks.get(thread))text.append("  at ").append(frame).append('\n');
            }
            Files.writeString(output.resolve(file),text,StandardCharsets.UTF_8);
            System.err.println("MOSSLUME_VISUAL_PROBE_THREAD_DIAGNOSTICS "+output.resolve(file)+" reason="+reason);
        }catch(Exception error){System.err.println("MOSSLUME_VISUAL_PROBE_THREAD_DIAGNOSTICS_FAILED "+error);}
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
        Settings.maxFPS = 60;
        // Do not let unrelated chat/tool focus silently turn a 60Hz test into
        // the engine's intentional 30Hz background power-saving mode.
        Settings.savePerformanceOnFocusLoss = false;
        Settings.maxFPS = (int)option("maxFps", 60, 0, 240);
        Settings.alwaysRain = false;
        // A fixed camera is reapplied by the engine in prepareDrawScene(), after frameTick.
        var definition=localforest.world.WorldDefinition.forLevel(level);
        probeCenterX=(int)option("centerX",definition==null?1536:definition.campX*32,-20000,20000);
        probeCenterY=(int)option("centerY",definition==null?1216:definition.campY*32,-20000,20000);
        game.setCamera(new FixedCamera(probeCenterX,probeCenterY));
        baseWorldMillis = (long)(level.getWorldEntity().hourToDayTime(
            (float)option("hour", 21, 0, 23.99)) * 1000f);
        // Start the clear-weather transition before the fixed test clock. Otherwise
        // resetting animation time between passes would reintroduce a ten-second
        // rain fade even when the source save was already dry.
        level.getWorldEntity().setWorldTime(baseWorldMillis - 20000);
        level.weatherLayer.setRaining(false);
        level.getWorldEntity().setWorldTime(baseWorldMillis);
        writer = Executors.newFixedThreadPool(2,task -> {
            Thread t = new Thread(task, "mosslume-visual-probe-image"); t.setDaemon(true); return t;
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
        return readFrame(x,y,w,h,1);
    }
    private static BufferedImage readFrame(int x, int y, int w, int h,int scale) {
        if (pixels == null || pixels.capacity() < w * h * 4) pixels = BufferUtils.createByteBuffer(w * h * 4);
        pixels.clear();
        int previous = GL11.glGetInteger(GL11.GL_READ_BUFFER);
        try {
            GL11.glReadBuffer(GL11.GL_FRONT);
            GL11.glReadPixels(x, y, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        } finally { GL11.glReadBuffer(previous); }
        int imageWidth=w/scale,imageHeight=h/scale;
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        int[] data = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        // Exact nearest sampling removes duplicated Retina pixels for clip encoding.
        // Full-resolution stills remain unchanged; no art smoothing is introduced.
        for (int row = 0; row < imageHeight; row++) for (int col = 0; col < imageWidth; col++) {
            int i = (col*scale + row*scale * w) * 4;
            data[col + (imageHeight - 1 - row) * imageWidth] = ((pixels.get(i) & 255) << 16)
                | ((pixels.get(i + 1) & 255) << 8) | (pixels.get(i + 2) & 255);
        }
        return image;
    }

    private static void queueImage(BufferedImage image, Path path) {
        pending.incrementAndGet();
        writer.execute(() -> {
            try {
                String format=path.toString().endsWith(".bmp")?"bmp":"png";
                if (!ImageIO.write(image, format, path.toFile())) throw new IllegalStateException(format+" encoder unavailable");
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
        width = Boolean.getBoolean("mosslume.visualProbe.captureFullFrame") ? frameWidth
            : Math.min(frameWidth, (int)option("captureWidth", 1200, 64, 5120));
        height = Boolean.getBoolean("mosslume.visualProbe.captureFullFrame") ? frameHeight
            : Math.min(frameHeight, (int)option("captureHeight", 900, 64, 3200));
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
            + ",\n  \"gcCollections\":" + (perfGcEnd - perfGcStart)
            + ",\n  \"focusedFrames\":" + focusedPerfFrames + ",\n  \"unfocusedFrames\":" + unfocusedPerfFrames + "\n}\n";
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
            + ",\n  \"centerWorldX\":" + probeCenterX
            + ",\n  \"centerWorldY\":" + probeCenterY
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
            + ",\n  \"capturedFrames\":" + captured + ",\n  \"writtenImagesIncludingFullFrame\":" + written.get()
            + ",\n  \"captureFrameFormat\":\"lossless BMP; full-resolution stills PNG\""
            + ",\n  \"pendingWrites\":" + pending.get() + ",\n  \"writerError\":" + quote(writeError)
            + ",\n  \"captureRequested\":" + !Boolean.getBoolean("mosslume.visualProbe.performanceOnly")
            + ",\n  \"captureComplete\":" + (captured >= 60 && pending.get() == 0 && writeError == null)
            + ",\n  \"captureElapsedSeconds\":" + number(captureElapsed)
            + ",\n  \"requestedCaptureFps\":" + number(option("captureFps", 30, 1, 60))
            + ",\n  \"previewNearestCaptureScale\":" + (int)option("captureScale",1,1,2)
            + ",\n  \"queueBackpressureSkips\":" + queueSkips + ",\n  \"missedCaptureIntervals\":" + dueSkips
            + ",\n  \"capturePerformanceExcluded\":true,\n  \"frameTimestamps\":\"frames.csv; real sampling may be irregular, do not encode as if every sample were exactly 1/requestedCaptureFps\""
            + ",\n  \"fixedEnvironmentHour\":" + number(option("hour", 21, 0, 23.99))
            + ",\n  \"actualEnvironmentHour\":" + number(level.getWorldEntity().getDayTimeHourFloat())
            + ",\n  \"comparisonHourCaptured\":" + secondHourSnapshot
            + ",\n  \"nativeClientAttackPosesRequested\":" + Boolean.getBoolean("mosslume.visualProbe.attackPoses")
            + ",\n  \"nativeClientAttackPosesAccepted\":" + attackPoseAccepted
            + ",\n  \"nativeClientAttackFramesObserved\":" + attackPoseFrames
            + ",\n  \"nativeClientAttackDirectionsObserved\":" + Arrays.toString(attackDirectionsSeen)
            + ",\n  \"biomeTourRequested\":" + BiomeVisualTour.enabled()
            + ",\n  \"biomeTourManifest\":" + (biomeTour==null?"null":quote("biome-tour.json"))
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
            + ",\n  \"windVboDraws\":" + CrownWindRenderer.vboDraws()
            + ",\n  \"windVboEnabled\":" + CrownWindRenderer.usesVbo()
            + ",\n  \"windBatchSprites\":" + localforest.visual.CrownSpriteBatch.sprites
            + ",\n  \"windBatchDraws\":" + localforest.visual.CrownSpriteBatch.draws
            + ",\n  \"windBatchMaxSprites\":" + localforest.visual.CrownSpriteBatch.maxBatch
            + ",\n  \"windBatchAtlasMisses\":" + localforest.visual.CrownSpriteBatch.atlasMisses
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
