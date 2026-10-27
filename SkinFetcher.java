package com.demo16.auctionhud;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import net.minecraft.client.gui.DrawContext;

/**
 * Fallback for players the server did not put in our tab list: downloads their skin by username
 * (public skin service) and keeps just the 8x8 face. Drawn with plain fill() calls, so no texture code is needed.
 * Only the username is sent. Runs in the background; face() returns null until the download is done.
 */
final class SkinFetcher {
    private SkinFetcher() {}

    private static final String[] URLS = {
            "https://minotar.net/skin/%s",
            "https://mc-heads.net/skin/%s"
    };
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final ConcurrentHashMap<String, int[]> DONE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> PENDING = new ConcurrentHashMap<>(); // name -> time started / failed

    /** 64 ARGB pixels (8x8 face with hat layer), or null while loading / if it failed. */
    static int[] face(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_]{1,16}")) return null;
        String key = name.toLowerCase();
        int[] got = DONE.get(key);
        if (got != null) return got;
        long now = System.currentTimeMillis();
        Long started = PENDING.get(key);
        if (started != null && now - started < 30_000) return null;   // already loading, or failed recently
        PENDING.put(key, now);
        fetch(name, key, 0);
        return null;
    }

    private static void fetch(String name, String key, int urlIdx) {
        if (urlIdx >= URLS.length) { PENDING.put(key, System.currentTimeMillis()); return; }
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(String.format(URLS[urlIdx], name)))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "AuctionHud/1.6")
                    .GET().build();
            HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray()).thenAccept(resp -> {
                try {
                    if (resp.statusCode() != 200) { fetch(name, key, urlIdx + 1); return; }
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(resp.body()));
                    if (img == null || img.getWidth() < 64 || img.getHeight() < 32) { fetch(name, key, urlIdx + 1); return; }
                    int[] px = new int[64];
                    for (int y = 0; y < 8; y++) {
                        for (int x = 0; x < 8; x++) {
                            int base = img.getRGB(8 + x, 8 + y);       // face
                            int hat = img.getRGB(40 + x, 8 + y);       // hat layer
                            px[y * 8 + x] = ((hat >>> 24) > 127) ? (0xFF000000 | hat) : (0xFF000000 | base);
                        }
                    }
                    DONE.put(key, px);
                } catch (Exception e) {
                    fetch(name, key, urlIdx + 1);
                }
            }).exceptionally(t -> { fetch(name, key, urlIdx + 1); return null; });
        } catch (Exception e) {
            fetch(name, key, urlIdx + 1);
        }
    }

    /** Draws the 8x8 face scaled to size x size with its top-left at x,y. alpha is 0..1. */
    static void draw(DrawContext ctx, int[] px, int x, int y, int size, float alpha) {
        for (int j = 0; j < 8; j++) {
            int y0 = y + j * size / 8, y1 = y + (j + 1) * size / 8;
            for (int i = 0; i < 8; i++) {
                int x0 = x + i * size / 8, x1 = x + (i + 1) * size / 8;
                ctx.fill(x0, y0, x1, y1, Gfx.alpha(px[j * 8 + i], alpha));
            }
        }
    }
}
