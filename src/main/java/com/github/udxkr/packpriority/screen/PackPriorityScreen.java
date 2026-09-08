package com.github.udxkr.packpriority.screen;

import com.github.udxkr.packpriority.PackPriorityMod;
import com.github.udxkr.packpriority.config.PackPriorityConfig;
import com.github.udxkr.packpriority.config.PackPriorityConfigManager;
import com.github.udxkr.packpriority.core.PackKind;
import com.github.udxkr.packpriority.core.PackPriorityEngine;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PackPriorityScreen extends Screen {

	private static final int COLOR_TEXT = 0xFFE6E6E6;
	private static final int COLOR_DIM = 0xFF9A9A9A;
	private static final int COLOR_LOCAL = 0xFF6FD37A;
	private static final int COLOR_SERVER = 0xFFE0954A;
	private static final int COLOR_BUILTIN = 0xFF8AA6D6;
	private static final int COLOR_ROW = 0x50000000;
	private static final int COLOR_ROW_HOVER = 0x80202020;
	private static final int COLOR_PANEL = 0x66000000;
	private static final int COLOR_SEPARATOR = 0xFFE0954A;

	private static final int ROW_HEIGHT = 22;
	private static final int ARROW_W = 14;

	private final Screen parent;
	private final PackPriorityConfig working;

	private List<PackRow> rows = List.of();
	private int listTop;
	private int listBottom;
	private int listLeft;
	private int listRight;
	private double scroll;

	private ButtonWidget overrideButton;
	private ButtonWidget keepButton;
	private ButtonWidget modeButton;
	private ButtonWidget builtinButton;

	public PackPriorityScreen(Screen parent) {
		super(Text.translatable("screen.resourcepack-priorizer.title"));
		this.parent = parent;
		this.working = PackPriorityConfigManager.get().copy();
	}

	@Override
	protected void init() {
		int cx = this.width / 2;

		overrideButton = addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
			working.setLocalOverridesServer(!working.localOverridesServer());
			working.setEnabled(true);
			refresh();
		}).dimensions(cx - 205, 32, 200, 20).build());

		keepButton = addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
			working.setKeepPriorityOnServerChange(!working.keepPriorityOnServerChange());
			refresh();
		}).dimensions(cx + 5, 32, 200, 20).build());

		modeButton = addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
			working.setMode(working.mode() == PackPriorityConfig.Mode.ALL_LOCAL
					? PackPriorityConfig.Mode.SELECTED
					: PackPriorityConfig.Mode.ALL_LOCAL);
			refresh();
		}).dimensions(cx - 205, 56, 200, 20).build());

		builtinButton = addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
			working.setLiftBuiltinPacks(!working.liftBuiltinPacks());
			refresh();
		}).dimensions(cx + 5, 56, 200, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.translatable("screen.resourcepack-priorizer.apply"),
				b -> applyAndClose()).dimensions(cx - 205, this.height - 28, 200, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.translatable("screen.resourcepack-priorizer.cancel"),
				b -> close()).dimensions(cx + 5, this.height - 28, 200, 20).build());

		listLeft = cx - 205;
		listRight = cx + 205;
		listTop = 96;
		listBottom = this.height - 36;

		refresh();
	}

	private void refresh() {
		overrideButton.setMessage(toggleLabel("screen.resourcepack-priorizer.override", working.localOverridesServer()));
		keepButton.setMessage(toggleLabel("screen.resourcepack-priorizer.keep", working.keepPriorityOnServerChange()));
		builtinButton.setMessage(toggleLabel("screen.resourcepack-priorizer.builtin", working.liftBuiltinPacks()));
		modeButton.setMessage(Text.translatable("screen.resourcepack-priorizer.mode",
				Text.translatable(working.mode() == PackPriorityConfig.Mode.ALL_LOCAL
						? "screen.resourcepack-priorizer.mode.all"
						: "screen.resourcepack-priorizer.mode.selected")));

		rows = buildPreview();
		clampScroll();
	}

	private Text toggleLabel(String key, boolean on) {
		return Text.translatable(key, Text.translatable(on
				? "screen.resourcepack-priorizer.on"
				: "screen.resourcepack-priorizer.off"));
	}

	private List<PackRow> buildPreview() {
		if (this.client == null) {
			return List.of();
		}
		List<ResourcePackProfile> enabled = new ArrayList<>(this.client.getResourcePackManager().getEnabledProfiles());
		List<ResourcePackProfile> ordered = PackPriorityEngine.reorder(enabled, working).order();

		List<String> lifted = liftedIds(ordered);
		List<PackRow> out = new ArrayList<>(ordered.size());

		for (int i = ordered.size() - 1; i >= 0; i--) {
			ResourcePackProfile profile = ordered.get(i);
			out.add(PackRow.of(profile, lifted.contains(profile.getId())));
		}
		return out;
	}

	private List<String> liftedIds(List<ResourcePackProfile> ordered) {
		int lastServer = -1;
		for (int i = 0; i < ordered.size(); i++) {
			if (PackKind.of(ordered.get(i)) == PackKind.SERVER) {
				lastServer = i;
			}
		}
		if (lastServer < 0) {
			return List.of();
		}
		List<String> ids = new ArrayList<>();
		for (int i = lastServer + 1; i < ordered.size(); i++) {
			ids.add(ordered.get(i).getId());
		}
		return ids;
	}

	private void seedPriorityOrder() {
		if (!working.priorityOrder().isEmpty()) {
			return;
		}
		List<String> seeded = new ArrayList<>();
		for (PackRow row : rows) {
			if (row.kind() == PackKind.LOCAL || (row.kind() == PackKind.BUILTIN && working.liftBuiltinPacks())) {
				seeded.add(row.id());
			}
		}
		working.setPriorityOrder(seeded);
	}

	private void move(String id, int delta) {
		seedPriorityOrder();
		List<String> order = new ArrayList<>(working.priorityOrder());
		int index = order.indexOf(id);
		if (index < 0) {
			order.add(id);
			index = order.size() - 1;
		}
		int target = index + delta;
		if (target < 0 || target >= order.size()) {
			return;
		}
		Collections.swap(order, index, target);
		working.setPriorityOrder(order);
		refresh();
	}

	private void toggleSelected(String id) {
		List<String> order = new ArrayList<>(working.priorityOrder());
		if (!order.remove(id)) {
			order.add(id);
		}
		working.setPriorityOrder(order);
		refresh();
	}

	private void applyAndClose() {
		PackPriorityConfigManager.save(working);
		if (this.client != null) {
			boolean reloaded = PackPriorityMod.applyAndReload(this.client);
			PackPriorityMod.LOGGER.info("Pack priority saved (resource reload {})",
					reloaded ? "started" : "not needed");
		}
		close();
	}

	@Override
	public void close() {
		if (this.client != null) {
			this.client.setScreen(parent);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 14, COLOR_TEXT);

		context.fill(listLeft, listTop, listRight, listBottom, COLOR_PANEL);
		context.enableScissor(listLeft, listTop, listRight, listBottom);

		int y = listTop - (int) scroll;
		boolean separatorDrawn = false;
		for (PackRow row : rows) {

			if (!separatorDrawn && row.kind() == PackKind.SERVER) {
				separatorDrawn = true;
				context.fill(listLeft + 4, y - 1, listRight - 4, y, COLOR_SEPARATOR);
			}
			renderRow(context, row, y, mouseX, mouseY);
			y += ROW_HEIGHT;
		}

		context.disableScissor();

		if (rows.isEmpty()) {
			context.drawCenteredTextWithShadow(this.textRenderer,
					Text.translatable("screen.resourcepack-priorizer.empty"),
					this.width / 2, listTop + 12, COLOR_DIM);
		}

		context.drawCenteredTextWithShadow(this.textRenderer,
				Text.translatable("screen.resourcepack-priorizer.hint"),
				this.width / 2, listTop - 12, COLOR_DIM);
	}

	private void renderRow(DrawContext context, PackRow row, int y, int mouseX, int mouseY) {
		if (y + ROW_HEIGHT < listTop || y > listBottom) {
			return;
		}
		boolean hovered = mouseX >= listLeft && mouseX <= listRight
				&& mouseY >= y && mouseY < y + ROW_HEIGHT
				&& mouseY >= listTop && mouseY < listBottom;

		context.fill(listLeft + 2, y + 1, listRight - 2, y + ROW_HEIGHT - 1,
				hovered ? COLOR_ROW_HOVER : COLOR_ROW);

		int tagColor = switch (row.kind()) {
			case LOCAL -> COLOR_LOCAL;
			case SERVER -> COLOR_SERVER;
			case BUILTIN -> COLOR_BUILTIN;
		};
		String tag = "[" + row.kind().name() + "]";
		context.drawTextWithShadow(this.textRenderer, Text.literal(tag), listLeft + 8, y + 7, tagColor);

		int nameX = listLeft + 8 + this.textRenderer.getWidth(tag) + 6;
		int nameWidth = listRight - ARROW_W * 2 - 16 - nameX;
		String name = this.textRenderer.trimToWidth(row.name().getString(), Math.max(nameWidth, 16));
		context.drawTextWithShadow(this.textRenderer, Text.literal(name), nameX, y + 7, COLOR_TEXT);

		if (working.mode() == PackPriorityConfig.Mode.SELECTED && row.kind() != PackKind.SERVER) {
			context.drawTextWithShadow(this.textRenderer,
					Text.literal(row.lifted() ? "✔" : "✘"),
					listRight - ARROW_W * 2 - 26, y + 7, row.lifted() ? COLOR_LOCAL : COLOR_DIM);
		}

		if (row.movable()) {
			context.drawTextWithShadow(this.textRenderer, Text.literal("▲"),
					listRight - ARROW_W * 2 - 6, y + 7, COLOR_DIM);
			context.drawTextWithShadow(this.textRenderer, Text.literal("▼"),
					listRight - ARROW_W - 6, y + 7, COLOR_DIM);
		}
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (super.mouseClicked(click, doubled)) {
			return true;
		}
		double mx = click.x();
		double my = click.y();
		if (mx < listLeft || mx > listRight || my < listTop || my > listBottom) {
			return false;
		}

		int index = (int) ((my - listTop + scroll) / ROW_HEIGHT);
		if (index < 0 || index >= rows.size()) {
			return false;
		}
		PackRow row = rows.get(index);

		if (row.movable()) {
			if (mx >= listRight - ARROW_W * 2 - 8 && mx < listRight - ARROW_W - 8) {
				move(row.id(), -1);
				return true;
			}
			if (mx >= listRight - ARROW_W - 8 && mx < listRight - 4) {
				move(row.id(), 1);
				return true;
			}
			if (working.mode() == PackPriorityConfig.Mode.SELECTED) {
				toggleSelected(row.id());
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (mouseX >= listLeft && mouseX <= listRight && mouseY >= listTop && mouseY <= listBottom) {
			scroll -= vertical * ROW_HEIGHT;
			clampScroll();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	private void clampScroll() {
		double max = Math.max(0, rows.size() * ROW_HEIGHT - (listBottom - listTop));
		scroll = Math.max(0, Math.min(scroll, max));
	}
}
