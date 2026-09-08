package com.github.udxkr.packpriority.core;

import com.github.udxkr.packpriority.config.PackPriorityConfig;
import com.google.common.collect.ImmutableList;
import net.minecraft.resource.ResourcePackProfile;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class PackPriorityEngine {

	private PackPriorityEngine() {
	}

	private static final AtomicReference<List<String>> BASELINE = new AtomicReference<>(List.of());

	private static final AtomicReference<Set<String>> APPLIED_SERVER_PACKS = new AtomicReference<>(null);

	public record Result(List<ResourcePackProfile> order, boolean changed) {
	}

	public static Result reorder(List<ResourcePackProfile> enabled, PackPriorityConfig config) {
		BASELINE.set(enabled.stream().map(ResourcePackProfile::getId).toList());

		if (!config.enabled() || !config.localOverridesServer()) {
			APPLIED_SERVER_PACKS.set(null);
			return new Result(enabled, false);
		}

		List<ResourcePackProfile> servers = new ArrayList<>();
		List<ResourcePackProfile> base = new ArrayList<>();
		List<ResourcePackProfile> lifted = new ArrayList<>();

		for (ResourcePackProfile profile : enabled) {
			PackKind kind = PackKind.of(profile);
			if (kind == PackKind.SERVER) {
				servers.add(profile);
			} else if (shouldLift(profile, kind, config)) {
				lifted.add(profile);
			} else {
				base.add(profile);
			}
		}

		if (servers.isEmpty()) {

			APPLIED_SERVER_PACKS.set(null);
			return new Result(enabled, false);
		}
		if (lifted.isEmpty()) {
			return new Result(enabled, false);
		}

		Set<String> serverIds = new LinkedHashSet<>(servers.stream().map(ResourcePackProfile::getId).toList());
		if (!shouldApplyForServerPacks(serverIds, config)) {
			return new Result(enabled, false);
		}

		List<ResourcePackProfile> byDisplayOrder = new ArrayList<>(lifted);
		Collections.reverse(byDisplayOrder);
		List<String> configured = config.priorityOrder();

		byDisplayOrder.sort((a, b) -> Integer.compare(rank(a.getId(), configured), rank(b.getId(), configured)));
		Collections.reverse(byDisplayOrder);

		List<ResourcePackProfile> result = new ArrayList<>(enabled.size());
		result.addAll(base);
		result.addAll(servers);
		result.addAll(byDisplayOrder);

		return new Result(ImmutableList.copyOf(result), !result.equals(enabled));
	}

	private static boolean shouldLift(ResourcePackProfile profile, PackKind kind, PackPriorityConfig config) {
		if (kind == PackKind.BUILTIN && !config.liftBuiltinPacks()) {
			return false;
		}
		return switch (config.mode()) {
			case ALL_LOCAL -> true;
			case SELECTED -> config.priorityOrder().contains(profile.getId());
		};
	}

	private static boolean shouldApplyForServerPacks(Set<String> serverIds, PackPriorityConfig config) {
		if (config.keepPriorityOnServerChange()) {
			APPLIED_SERVER_PACKS.set(serverIds);
			return true;
		}
		Set<String> applied = APPLIED_SERVER_PACKS.get();
		if (applied == null) {
			APPLIED_SERVER_PACKS.set(serverIds);
			return true;
		}
		return applied.equals(serverIds);
	}

	private static int rank(String id, List<String> configured) {
		int index = configured.indexOf(id);
		return index < 0 ? Integer.MAX_VALUE : index;
	}

	public static List<String> baselineIds() {
		return BASELINE.get();
	}

	public static void resetSessionState() {
		APPLIED_SERVER_PACKS.set(null);
	}

	public static void logOrder(Logger logger, List<ResourcePackProfile> order) {
		StringBuilder sb = new StringBuilder("Resource pack priority (highest first):");
		for (int i = order.size() - 1; i >= 0; i--) {
			ResourcePackProfile profile = order.get(i);
			sb.append("\n  #").append(order.size() - i)
					.append(" [").append(PackKind.of(profile)).append("] ")
					.append(profile.getId());
		}
		logger.info(sb.toString());
	}
}
