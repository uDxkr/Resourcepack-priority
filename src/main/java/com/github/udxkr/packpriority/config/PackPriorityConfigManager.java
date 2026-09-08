package com.github.udxkr.packpriority.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PackPriorityConfigManager {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "resourcepack-priorizer.json";

	private static volatile PackPriorityConfig current = new PackPriorityConfig();
	private static Logger logger;

	private PackPriorityConfigManager() {
	}

	public static PackPriorityConfig get() {
		return current;
	}

	public static void init(Logger log) {
		logger = log;
		load();
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	public static void load() {
		Path path = path();
		if (!Files.isRegularFile(path)) {
			save(current);
			return;
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			PackPriorityConfig parsed = GSON.fromJson(reader, PackPriorityConfig.class);
			if (parsed != null) {
				current = parsed;
			}
		} catch (IOException | JsonSyntaxException e) {

			if (logger != null) {
				logger.warn("Could not read {}, using defaults", path, e);
			}
			current = new PackPriorityConfig();
		}
	}

	public static void save(PackPriorityConfig config) {
		current = config;
		Path path = path();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException e) {
			if (logger != null) {
				logger.error("Could not write {}", path, e);
			}
		}
	}
}
