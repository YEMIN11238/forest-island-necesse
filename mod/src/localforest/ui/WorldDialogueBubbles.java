package localforest.ui;

import java.awt.Point;
import java.awt.Rectangle;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.client.Client;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameColor;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.ChatBubbleDrawOptions;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.fairType.FairType;
import necesse.gfx.fairType.FairTypeDrawOptions;
import necesse.gfx.gameFont.FontOptions;
import necesse.level.maps.Level;
import necesse.level.maps.hudManager.HudDrawElement;
import necesse.level.maps.hudManager.HudManager;

/**
 * Local world speech using Necesse's own HUD, font layout and chat-bubble skin.
 * Producers only enqueue; tick must be called from MainGame.frameTick even when
 * the conversation input is closed. No network/global-chat packet is emitted.
 */
public final class WorldDialogueBubbles {
    private static final int TEXT_WIDTH = 208;
    private static final int MAX_LINES = 3;
    private static final int MAX_PAGES = 6;
    private static final int MAX_SPEAKERS = 4;
    private static final FontOptions FONT = new FontOptions(16);
    private static final ConcurrentLinkedQueue<Pending> pending = new ConcurrentLinkedQueue<>();
    private static WeakReference<Client> owner = new WeakReference<>(null);
    private static Session current;

    private record Pending(WeakReference<Client> client, WeakReference<Level> level,
                           int mobId, boolean player, boolean clear, String text) {}
    private record Page(FairTypeDrawOptions text, int width, int height, long duration) {}

    /** Safe to call from a producer thread. The player's actual world avatar speaks. */
    public static void player(Client client, String text) {
        enqueue(client, 0, true, false, text);
    }

    /**
     * Accepts spoken dialogue only. Request status, validation receipts and API
     * failures belong in the input UI, not in an NPC's mouth.
     */
    public static void npc(Client client, int mobId, String spokenText) {
        enqueue(client, mobId, false, false, spokenText);
    }

    /** Clears only this client's conversation bubbles, never native chat or damage text. */
    public static void clear(Client client) {
        enqueue(client, 0, false, true, "");
    }

    private static void enqueue(Client client, int mobId, boolean player, boolean clear, String text) {
        if (client == null) return;
        String clean = clear ? "" : clean(text);
        if (!clear && clean.isBlank()) return;
        // Capture the level when the line is submitted: a delayed result cannot
        // attach to a reused mob ID after the player changes maps.
        Level level = client.getLevel();
        if (!clear && level == null) return;
        if (pending.size() >= 128) pending.poll();
        pending.add(new Pending(new WeakReference<>(client), new WeakReference<>(level),
                mobId, player, clear, clean));
    }

    /** Main/client thread only; call once per frame, independently of input visibility. */
    public static void tick(Client client) {
        Level level = client == null || client.isDisconnected() ? null : client.getLevel();
        if (owner.get() != client || current != null && current.level != level) {
            reset();
            owner = new WeakReference<>(client);
        }
        if (level == null || client.getPlayer() == null) {
            reset();
            pending.clear();
            return;
        }
        if (current == null) current = new Session(level);
        Pending next;
        while ((next = pending.poll()) != null) {
            if (next.client.get() != client) continue;
            if (next.clear) {
                current.clear();
                continue;
            }
            if (next.level.get() != level) continue;
            Mob speaker = next.player ? client.getPlayer() : GameUtils.getLevelMob(next.mobId, level);
            if (speaker == null || speaker.removed() || speaker.getLevel() != level) continue;
            current.say(speaker, next.player, next.text);
        }
        current.tick();
    }

    private static void reset() {
        if (current != null) current.clear();
        current = null;
    }

    private static String clean(String text) {
        if (text == null) return "";
        String stripped = GameColor.stripCodes(text);
        StringBuilder result = new StringBuilder();
        stripped.codePoints().limit(1000).forEach(cp -> {
            if (cp == '\t' || cp == '\r') result.append(' ');
            else if (cp == '\n' || !Character.isISOControl(cp)) result.appendCodePoint(cp);
        });
        return result.toString().replaceAll("\\n{3,}", "\n\n").strip();
    }

    private static FairTypeDrawOptions layout(String text) {
        // forceMaxWidth breaks CJK text and long unspaced words at glyph boundaries.
        // Do not apply chat-link/item parsers to model output.
        return new FairType().append(FONT, text)
                .getDrawOptions(FairType.TextAlign.LEFT, TEXT_WIDTH, true, true);
    }

    private static List<Page> pages(String text) {
        List<Page> result = new ArrayList<>();
        String rest = text;
        while (!rest.isBlank() && result.size() < MAX_PAGES) {
            int count = rest.codePointCount(0, rest.length());
            int low = 1, high = Math.min(count, 96), fits = 1;
            while (low <= high) {
                int middle = (low + high) >>> 1;
                String candidate = rest.substring(0, rest.offsetByCodePoints(0, middle));
                if (layout(candidate).getLineCount() <= MAX_LINES) {
                    fits = middle;
                    low = middle + 1;
                } else high = middle - 1;
            }
            int end = rest.offsetByCodePoints(0, fits);
            // Prefer a sentence/phrase boundary, while retaining at least half a page.
            if (fits < count) {
                int lower = rest.offsetByCodePoints(0, fits / 2);
                for (int p = end; p > lower; ) {
                    int cp = rest.codePointBefore(p);
                    if ("。！？!?；;，,、\n ".indexOf(cp) >= 0) { end = p; break; }
                    p -= Character.charCount(cp);
                }
            }
            String line = rest.substring(0, end).strip();
            rest = rest.substring(end).stripLeading();
            if (result.size() == MAX_PAGES - 1 && !rest.isBlank()) {
                while (!line.isEmpty() && layout(line + "…").getLineCount() > MAX_LINES)
                    line = line.substring(0, line.offsetByCodePoints(line.length(), -1));
                line += "…";
            }
            FairTypeDrawOptions draw = layout(line);
            Rectangle box = draw.getBoundingBox();
            int length = line.codePointCount(0, line.length());
            result.add(new Page(draw, Math.max(40, box.x + box.width), Math.max(16, box.y + box.height),
                    Math.max(3500, Math.min(7500, 2400L + length * 110L))));
        }
        return result;
    }

    private static final class Session {
        final Level level;
        final List<Speech> speakers = new ArrayList<>();
        Session(Level level) { this.level = level; }

        void say(Mob mob, boolean player, String text) {
            speakers.removeIf(s -> {
                if (s.mob == mob || s.isRemoved()) { s.remove(); return true; }
                return false;
            });
            while (speakers.size() >= MAX_SPEAKERS) speakers.remove(0).remove();
            Speech speech = new Speech(this, mob, player, pages(text));
            if (!speech.pages.isEmpty()) {
                speakers.add(speech);
                level.hudManager.addElement(speech);
            }
        }

        void clear() {
            speakers.forEach(HudDrawElement::remove);
            speakers.clear();
        }

        void tick() {
            speakers.removeIf(s -> { s.advance(); return s.isRemoved(); });
        }

        Rectangle position(Speech target, GameCamera camera) {
            List<Rectangle> occupied = new ArrayList<>();
            for (Speech speech : speakers) {
                if (speech.isRemoved()) continue;
                Rectangle bounds = speech.basePosition(camera);
                // A reply may appear before the player's bubble expires. Keep both
                // legible rather than drawing the NPC directly over the player text.
                for (int attempt = 0; attempt < MAX_SPEAKERS; attempt++) {
                    boolean changed = false;
                    for (Rectangle previous : occupied) {
                        if (bounds.intersects(previous) && previous.y - bounds.height - 12 >= 8) {
                            bounds.y = previous.y - bounds.height - 12;
                            changed = true;
                        }
                    }
                    if (!changed) break;
                }
                if (speech == target) return bounds;
                occupied.add(bounds);
            }
            return target.basePosition(camera);
        }
    }

    private static final class Speech extends HudDrawElement {
        final Session session;
        final Mob mob;
        final boolean player;
        final List<Page> pages;
        int page;
        long nextPage;

        Speech(Session session, Mob mob, boolean player, List<Page> pages) {
            this.session = session; this.mob = mob; this.player = player; this.pages = pages;
        }

        @Override public void init(HudManager manager) {
            nextPage = getTime() + pages.get(0).duration;
        }

        void advance() {
            if (isRemoved()) return;
            if (mob.removed() || mob.getLevel() != session.level) { remove(); return; }
            if (getTime() >= nextPage) {
                if (++page >= pages.size()) { remove(); return; }
                nextPage = getTime() + pages.get(page).duration;
            }
        }

        Rectangle basePosition(GameCamera camera) {
            Page p = pages.get(page);
            Point anchor = mob.getDrawPos();
            int x = camera.getDrawX(anchor.x), y = camera.getDrawY(anchor.y) - 42;
            boolean left = player;
            for (Speech other : session.speakers) {
                if (other != this && other.player != player && !other.isRemoved()) {
                    left = anchor.x <= other.mob.getDrawPos().x;
                    break;
                }
            }
            int width = p.width + 20, height = p.height + 18;
            int drawX = left ? x - width + 24 : x - 24;
            drawX = Math.max(8, Math.min(Math.max(8, camera.getWidth() - width - 8), drawX));
            return new Rectangle(drawX, Math.max(8, y - height - 12), width, height);
        }

        @Override public void addDrawables(List<SortedDrawable> list, GameCamera camera, PlayerMob perspective) {
            if (isRemoved() || page >= pages.size()) return;
            Point anchor = mob.getDrawPos();
            if (!camera.getBounds().intersects(anchor.x - 32, anchor.y - 64, 64, 80)) return;
            Page p = pages.get(page);
            Rectangle bounds = session.position(this, camera);
            int tip = Math.max(16, Math.min(bounds.width - 16, camera.getDrawX(anchor.x) - bounds.x));
            DrawOptions skin = new ChatBubbleDrawOptions(bounds.width, bounds.height)
                    .setBotIndicator(tip, false).pos(bounds.x, bounds.y);
            list.add(new SortedDrawable() {
                @Override public int getPriority() { return 1000; }
                @Override public void draw(TickManager tickManager) {
                    skin.draw();
                    p.text.draw(bounds.x + 10, bounds.y + 8, Settings.UI.chatBubbleActiveTextColor);
                }
            });
        }
    }

    private WorldDialogueBubbles() {}
}
