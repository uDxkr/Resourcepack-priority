package com.github.udxkr.packpriority.mixin;

import com.github.udxkr.packpriority.PackPriorityMod;
import com.github.udxkr.packpriority.config.PackPriorityConfigManager;
import com.github.udxkr.packpriority.core.PackPriorityEngine;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;

@Mixin(ResourcePackManager.class)
public class ResourcePackManagerMixin {

	@Inject(method = "buildEnabledProfiles", at = @At("RETURN"), cancellable = true)
	private void packpriority$applyLocalOverride(Collection<String> enabledNames,
			CallbackInfoReturnable<List<ResourcePackProfile>> cir) {
		List<ResourcePackProfile> vanillaOrder = cir.getReturnValue();
		if (vanillaOrder == null || vanillaOrder.isEmpty()) {
			return;
		}

		PackPriorityEngine.Result result =
				PackPriorityEngine.reorder(vanillaOrder, PackPriorityConfigManager.get());
		if (!result.changed()) {
			return;
		}

		cir.setReturnValue(result.order());
		PackPriorityEngine.logOrder(PackPriorityMod.LOGGER, result.order());
	}
}
