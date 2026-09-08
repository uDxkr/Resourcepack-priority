package com.github.udxkr.packpriority.config;

import java.util.ArrayList;
import java.util.List;

public final class PackPriorityConfig {

	public enum Mode {

		ALL_LOCAL,

		SELECTED
	}

	private boolean enabled = true;
	private boolean localOverridesServer = true;
	private boolean keepPriorityOnServerChange = true;
	private boolean liftBuiltinPacks = false;
	private Mode mode = Mode.ALL_LOCAL;

	private List<String> priorityOrder = new ArrayList<>();

	public boolean enabled() {
		return enabled;
	}

	public boolean localOverridesServer() {
		return localOverridesServer;
	}

	public boolean keepPriorityOnServerChange() {
		return keepPriorityOnServerChange;
	}

	public boolean liftBuiltinPacks() {
		return liftBuiltinPacks;
	}

	public Mode mode() {
		return mode == null ? Mode.ALL_LOCAL : mode;
	}

	public List<String> priorityOrder() {
		return priorityOrder == null ? List.of() : priorityOrder;
	}

	public void setEnabled(boolean value) {
		this.enabled = value;
	}

	public void setLocalOverridesServer(boolean value) {
		this.localOverridesServer = value;
	}

	public void setKeepPriorityOnServerChange(boolean value) {
		this.keepPriorityOnServerChange = value;
	}

	public void setLiftBuiltinPacks(boolean value) {
		this.liftBuiltinPacks = value;
	}

	public void setMode(Mode value) {
		this.mode = value;
	}

	public void setPriorityOrder(List<String> value) {
		this.priorityOrder = new ArrayList<>(value);
	}

	public PackPriorityConfig copy() {
		PackPriorityConfig copy = new PackPriorityConfig();
		copy.enabled = enabled;
		copy.localOverridesServer = localOverridesServer;
		copy.keepPriorityOnServerChange = keepPriorityOnServerChange;
		copy.liftBuiltinPacks = liftBuiltinPacks;
		copy.mode = mode();
		copy.priorityOrder = new ArrayList<>(priorityOrder());
		return copy;
	}
}
