package com.github.udxkr.packpriority.screen;

import com.github.udxkr.packpriority.core.PackKind;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.text.Text;

public record PackRow(String id, Text name, PackKind kind, boolean lifted) {

	public static PackRow of(ResourcePackProfile profile, boolean lifted) {
		return new PackRow(profile.getId(), profile.getDisplayName(), PackKind.of(profile), lifted);
	}

	public boolean movable() {
		return kind != PackKind.SERVER;
	}
}
