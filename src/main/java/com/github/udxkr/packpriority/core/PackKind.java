package com.github.udxkr.packpriority.core;

import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;

public enum PackKind {

	SERVER,

	LOCAL,

	BUILTIN;

	public static final String SERVER_ID_PREFIX = "server/";

	public static PackKind of(ResourcePackProfile profile) {
		ResourcePackSource source = profile.getInfo().source();

		if (source == ResourcePackSource.SERVER || profile.getId().startsWith(SERVER_ID_PREFIX)) {
			return SERVER;
		}
		if (source == ResourcePackSource.NONE) {
			return LOCAL;
		}
		return BUILTIN;
	}
}
