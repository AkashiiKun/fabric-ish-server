package holiday.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.server.MinecraftServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Dedicated-server administration that runs inside the game, so no RCON is needed:
 * <ul>
 *   <li>syncs whitelist.json and ops.json from GitHub (WHITELIST_URL / OPS_URL)</li>
 *   <li>watches the deployed pack (PACK_URL); on a change it warns players, snapshots the
 *       server directory into /backups and stops the server. The container's restart
 *       policy brings it back, and packwiz installs the new mods on that start.</li>
 * </ul>
 * Configured through environment variables (see .env.example).
 */
public final class ServerAdmin {
	private static final Logger LOG = LoggerFactory.getLogger("holiday/admin");
	private static final HttpClient HTTP = HttpClient.newBuilder()
		.followRedirects(HttpClient.Redirect.ALWAYS)
		.connectTimeout(Duration.ofSeconds(10))
		.build();

	private static ServerAdmin instance;

	private final MinecraftServer server;
	private final ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "holiday-admin");
		t.setDaemon(true);
		return t;
	});

	private final String packUrl = env("PACK_URL", "");
	private final String whitelistUrl = env("WHITELIST_URL", "");
	private final String opsUrl = env("OPS_URL", "");
	private final int pollSeconds = Math.max(30, envInt("UPDATE_POLL_SECONDS", 300));
	private final int warnSeconds = Math.max(0, envInt("UPDATE_WARN_SECONDS", 60));
	private final int backupKeep = Math.max(1, envInt("BACKUP_KEEP", 5));
	private final Path backups = Path.of(env("BACKUP_DIR", "/backups"));

	private String packBaseline;
	private String lastWhitelistHash;
	private String lastOpsHash;
	private volatile boolean updating;
	private volatile boolean snapshotOnStop;

	private ServerAdmin(MinecraftServer server) {
		this.server = server;
	}

	public static void start(MinecraftServer server) {
		if (instance != null) {
			return;
		}
		instance = new ServerAdmin(server);
		instance.pool.scheduleWithFixedDelay(instance::tick, 5, instance.pollSeconds, TimeUnit.SECONDS);
		LOG.info("Server admin started (poll {}s, pack={}, whitelist={}, ops={})", instance.pollSeconds,
			!instance.packUrl.isEmpty(), !instance.whitelistUrl.isEmpty(), !instance.opsUrl.isEmpty());
	}

	public static void stop() {
		if (instance != null) {
			// stop polling; the instance is kept until afterStop() has taken the snapshot
			instance.pool.shutdownNow();
		}
	}

	private void tick() {
		if (updating) {
			return;
		}
		try {
			syncWhitelist();
		} catch (Exception e) {
			LOG.warn("whitelist sync failed: {}", e.toString());
		}
		try {
			syncOps();
		} catch (Exception e) {
			LOG.warn("ops sync failed: {}", e.toString());
		}
		try {
			checkPack();
		} catch (Exception e) {
			LOG.warn("pack check failed: {}", e.toString());
		}
	}

	// ---- whitelist / ops --------------------------------------------------------------

	private void syncWhitelist() throws Exception {
		if (whitelistUrl.isEmpty()) {
			return;
		}
		String body = fetch(whitelistUrl);
		if (!isJsonArray(body)) {
			LOG.warn("whitelist: {} is not a JSON array, ignoring", whitelistUrl);
			return;
		}
		String hash = sha256(body);
		if (hash.equals(lastWhitelistHash)) {
			return;
		}
		Files.writeString(Path.of("whitelist.json"), body, StandardCharsets.UTF_8);
		run("whitelist reload");
		lastWhitelistHash = hash;
		LOG.info("whitelist: applied");
	}

	private void syncOps() throws Exception {
		if (opsUrl.isEmpty()) {
			return;
		}
		String body = fetch(opsUrl);
		if (!isJsonArray(body)) {
			LOG.warn("ops: {} is not a JSON array, ignoring", opsUrl);
			return;
		}
		String hash = sha256(body);
		if (hash.equals(lastOpsHash)) {
			return;
		}
		Set<String> wanted = names(body);
		Path current = Path.of("ops.json");
		Set<String> existing = Files.exists(current) ? names(Files.readString(current)) : Set.of();
		for (String name : wanted) {
			if (!existing.contains(name)) {
				run("op " + name);
			}
		}
		for (String name : existing) {
			if (!wanted.contains(name)) {
				run("deop " + name);
			}
		}
		lastOpsHash = hash;
		LOG.info("ops: applied");
	}

	private static Set<String> names(String json) {
		Set<String> result = new HashSet<>();
		for (JsonElement e : JsonParser.parseString(json).getAsJsonArray()) {
			if (e.isJsonObject() && e.getAsJsonObject().has("name")) {
				result.add(e.getAsJsonObject().get("name").getAsString());
			}
		}
		return result;
	}

	private static boolean isJsonArray(String body) {
		try {
			return JsonParser.parseString(body) instanceof JsonArray;
		} catch (Exception e) {
			return false;
		}
	}

	// ---- pack updates -----------------------------------------------------------------

	private void checkPack() throws Exception {
		if (packUrl.isEmpty() || Files.exists(backups.resolve("PINNED"))) {
			return;
		}
		String body = fetch(packUrl);
		String marker = "";
		try {
			marker = fetch(packUrl.substring(0, packUrl.lastIndexOf('/') + 1) + "deploy-version.txt");
		} catch (Exception ignored) {
			// optional
		}
		String hash = sha256(body + "\n" + marker);
		if (packBaseline == null) {
			packBaseline = hash;
			LOG.info("pack baseline {}", hash);
			return;
		}
		if (hash.equals(packBaseline)) {
			return;
		}
		LOG.info("pack changed ({} -> {}), restarting for update", packBaseline, hash);
		updating = true;
		run("say Mod update available - server restarting in " + warnSeconds + "s");
		pool.schedule(this::stopForUpdate, warnSeconds, TimeUnit.SECONDS);
	}

	/**
	 * The normal "stop" command saves everything and closes the world. The snapshot is taken
	 * afterwards (see {@link #afterStop()}), so it is always a clean, fully saved state.
	 */
	private void stopForUpdate() {
		snapshotOnStop = true;
		LOG.info("stopping server for update; the container restart policy starts it again");
		run("stop");
	}

	/** Called once the server has fully stopped and the world is saved and closed. */
	public static void afterStop() {
		ServerAdmin admin = instance;
		instance = null;
		if (admin == null || !admin.snapshotOnStop) {
			return;
		}
		try {
			if (Files.isDirectory(admin.backups)) {
				admin.snapshot();
				admin.prune();
			} else {
				LOG.warn("{} is not mounted, skipping snapshot", admin.backups);
			}
		} catch (Exception e) {
			LOG.warn("snapshot failed: {}", e.toString());
		}
	}

	private void snapshot() throws IOException, InterruptedException {
		String name = "backup-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".tar.gz";
		Path out = backups.resolve(name);
		LOG.info("snapshot: writing {}", out);
		Process p = new ProcessBuilder("sh", "-c",
			"tar --exclude=./logs --exclude=./crash-reports --exclude=./cache --exclude=./bluemap "
				+ "--exclude=./libraries --exclude=./.fabric -cf - . | gzip -1 > '" + out + "'")
			.directory(Path.of("").toAbsolutePath().toFile())
			.redirectErrorStream(true)
			.start();
		p.getInputStream().transferTo(OutputStream.nullOutputStream());
		if (p.waitFor() != 0) {
			LOG.warn("snapshot: failed, removing partial file");
			Files.deleteIfExists(out);
		} else {
			LOG.info("snapshot: done ({} MB)", Files.size(out) / (1024 * 1024));
		}
	}

	private void prune() throws IOException {
		List<Path> all;
		try (Stream<Path> s = Files.list(backups)) {
			all = s.filter(p -> p.getFileName().toString().matches("backup-.*\\.tar\\.gz"))
				.sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
				.toList();
		}
		for (int i = backupKeep; i < all.size(); i++) {
			Files.deleteIfExists(all.get(i));
		}
	}

	// ---- helpers ----------------------------------------------------------------------

	/** Runs a console command, from any thread, on the server thread. */
	private void run(String command) {
		server.execute(() -> exec(command));
	}

	/** Must be called on the server thread. */
	private void exec(String command) {
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
	}

	private static String fetch(String url) throws Exception {
		HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build();
		HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (res.statusCode() != 200) {
			throw new IOException("HTTP " + res.statusCode() + " for " + url);
		}
		return res.body();
	}

	private static String sha256(String s) throws Exception {
		byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
		StringBuilder sb = new StringBuilder();
		for (byte b : d) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	private static String env(String key, String fallback) {
		String v = System.getenv(key);
		return v == null ? fallback : v.trim();
	}

	private static int envInt(String key, int fallback) {
		try {
			return Integer.parseInt(env(key, ""));
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
