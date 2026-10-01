package com.kerem.morphmod.client;

import com.kerem.morphmod.network.MorphRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The Morph selection screen.
 * Displays a searchable, scrollable grid of all collected morphs, each rendered
 * as a miniature animated entity. Clicking on a morph activates the transformation.
 */
public class MorphScreen extends Screen {
    private static final int CELL_SIZE = 64;
    private static final int CELL_PADDING = 8;
    private static final int COLUMNS = 6;
    private static final int HEADER_HEIGHT = 58;
    private static final int FOOTER_HEIGHT = 42;

    private final List<ResourceLocation> allMorphList = new ArrayList<>();
    private final List<ResourceLocation> displayedMorphList = new ArrayList<>();

    private EditBox searchBox;
    private String currentSearch = "";
    private int scrollOffset = 0;
    private int maxScroll = 0;

    public MorphScreen() {
        super(Component.translatable("morphmod.screen.title"));
    }

    @Override
    protected void init() {
        super.init();

        // Load all collected morphs
        allMorphList.clear();
        Set<ResourceLocation> morphs = ClientMorphData.getCollectedMorphs();
        allMorphList.addAll(morphs);

        // Search box in header
        int searchWidth = 170;
        int searchHeight = 16;
        int searchX = (this.width - searchWidth) / 2;
        int searchY = 20;

        searchBox = new EditBox(this.font, searchX, searchY, searchWidth, searchHeight, Component.translatable("morphmod.screen.search"));
        searchBox.setHint(Component.translatable("morphmod.screen.search_hint"));
        searchBox.setMaxLength(32);
        searchBox.setValue(currentSearch);
        searchBox.setResponder(this::onSearchQueryChanged);
        this.addRenderableWidget(searchBox);

        // Filter morph list based on existing or initial query
        updateFilteredList();

        // "Return to Normal" button centered at the bottom
        int buttonWidth = 200;
        int buttonX = (this.width - buttonWidth) / 2;
        int buttonY = this.height - 30;

        this.addRenderableWidget(Button.builder(
                Component.translatable("morphmod.screen.return_normal"),
                button -> {
                    // Send un-morph request to server
                    ClientPlayNetworking.send(new MorphRequestPayload(""));
                    this.onClose();
                }
        ).bounds(buttonX, buttonY, buttonWidth, 20).build());
    }

    private void onSearchQueryChanged(String query) {
        this.currentSearch = query;
        this.scrollOffset = 0;
        updateFilteredList();
    }

    private void updateFilteredList() {
        displayedMorphList.clear();
        for (ResourceLocation id : allMorphList) {
            if (matchesSearch(id, currentSearch)) {
                displayedMorphList.add(id);
            }
        }

        // Calculate scrolling bounds
        int totalRows = (int) Math.ceil((double) displayedMorphList.size() / COLUMNS);
        int gridHeight = totalRows * (CELL_SIZE + CELL_PADDING);
        int availableHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT;
        maxScroll = Math.max(0, gridHeight - availableHeight);
        scrollOffset = Math.min(scrollOffset, maxScroll);
    }

    private boolean matchesSearch(ResourceLocation morphId, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);

        // Match entity registry id path (e.g. "creeper", "iron_golem")
        String path = morphId.getPath().toLowerCase(Locale.ROOT);
        if (path.contains(q)) {
            return true;
        }

        // Match localized name (e.g. "Creeper", "Demir Golem", "İnek")
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(morphId);
        if (entityType != null) {
            String name = entityType.getDescription().getString().toLowerCase(Locale.ROOT);
            if (name.contains(q)) {
                return true;
            }

            // Normalization for Turkish/English character variations
            String normName = normalize(name);
            String normQ = normalize(q);
            if (normName.contains(normQ)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String s) {
        return s.replace('ı', 'i')
                .replace('İ', 'i')
                .replace('ş', 's')
                .replace('Ş', 's')
                .replace('ç', 'c')
                .replace('Ç', 'c')
                .replace('ğ', 'g')
                .replace('Ğ', 'g')
                .replace('ö', 'o')
                .replace('Ö', 'o')
                .replace('ü', 'u')
                .replace('Ü', 'u');
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Crisp, sleek dark gradient background without blur shader
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xD808080C, 0xF0030305);

        // ─── Header ──────────────────────────────────────────────
        // Title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 7, 0xFFD700);

        // Count info
        if (currentSearch.isEmpty()) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("morphmod.screen.count", allMorphList.size()),
                    this.width / 2, 40,
                    0xAAAAAA
            );
        } else {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("morphmod.screen.count_filtered", displayedMorphList.size(), allMorphList.size()),
                    this.width / 2, 40,
                    0xAAAAAA
            );
        }

        // Decorative separator line
        int gridStartX = (this.width - (COLUMNS * (CELL_SIZE + CELL_PADDING))) / 2;
        int gridEndX = gridStartX + (COLUMNS * (CELL_SIZE + CELL_PADDING));
        guiGraphics.fill(gridStartX, HEADER_HEIGHT - 2, gridEndX, HEADER_HEIGHT - 1, 0x80FFD700);

        // ─── Morph Grid ──────────────────────────────────────────
        int gridStartY = HEADER_HEIGHT;
        int availableHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT;

        if (displayedMorphList.isEmpty()) {
            Component emptyMsg = allMorphList.isEmpty()
                    ? Component.translatable("morphmod.screen.empty", "Henüz mob ruhu toplamadın!")
                    : Component.translatable("morphmod.screen.no_results");
            guiGraphics.drawCenteredString(this.font, emptyMsg, this.width / 2, gridStartY + 35, 0xFFAAAA);
        }

        // Enable scissor to clip scrolling content
        guiGraphics.enableScissor(0, gridStartY, this.width, gridStartY + availableHeight);

        for (int i = 0; i < displayedMorphList.size(); i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;

            int cellX = gridStartX + col * (CELL_SIZE + CELL_PADDING);
            int cellY = gridStartY + row * (CELL_SIZE + CELL_PADDING) - scrollOffset;

            // Skip cells that aren't visible
            if (cellY + CELL_SIZE < gridStartY || cellY > gridStartY + availableHeight) {
                continue;
            }

            ResourceLocation morphId = displayedMorphList.get(i);
            boolean isActive = morphId.equals(ClientMorphData.getActiveMorph());
            boolean isHovered = mouseX >= cellX && mouseX < cellX + CELL_SIZE
                    && mouseY >= cellY && mouseY < cellY + CELL_SIZE
                    && mouseY >= gridStartY && mouseY < gridStartY + availableHeight;

            // Cell background
            int bgColor;
            if (isActive) {
                bgColor = 0x6000FF00;  // Green tint for active morph
            } else if (isHovered) {
                bgColor = 0x50FFFFFF;  // White highlight on hover
            } else {
                bgColor = 0x40000000;  // Dark default
            }
            guiGraphics.fill(cellX, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, bgColor);

            // Cell border
            int borderColor;
            if (isActive) {
                borderColor = 0xFF00FF00;  // Green for active
            } else if (isHovered) {
                borderColor = 0xFFFFD700;  // Gold on hover
            } else {
                borderColor = 0xFF555555;  // Dark gray default
            }
            drawBorder(guiGraphics, cellX, cellY, CELL_SIZE, CELL_SIZE, borderColor);

            // Active morph indicator glow
            if (isActive) {
                drawBorder(guiGraphics, cellX - 1, cellY - 1, CELL_SIZE + 2, CELL_SIZE + 2, 0x8000FF00);
            }

            // Render entity model inside the cell
            Entity fakeEntity = ClientMorphData.getOrCreateFakeEntity(morphId);
            if (fakeEntity instanceof LivingEntity livingEntity) {
                int entityCenterX = cellX + CELL_SIZE / 2;
                int entityBottomY = cellY + CELL_SIZE - 12;

                float entityHeight = livingEntity.getBbHeight();
                int scale = (int) Math.min(22, 38 / Math.max(entityHeight, 0.5f));
                scale = Math.max(scale, 6);

                try {
                    InventoryScreen.renderEntityInInventoryFollowsMouse(
                            guiGraphics,
                            entityCenterX - 18, cellY + 4,
                            entityCenterX + 18, entityBottomY,
                            scale, 0.0625F,
                            (float) mouseX, (float) mouseY,
                            livingEntity
                    );
                } catch (Exception ignored) {
                    // Some entities may fail to render in GUI context
                }
            }

            // Entity name below the model
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(morphId);
            if (entityType != null) {
                String name = entityType.getDescription().getString();
                // Truncate long names
                if (name.length() > 9) {
                    name = name.substring(0, 8) + "…";
                }
                int nameWidth = this.font.width(name);
                int nameColor = isActive ? 0xFF00FF00 : (isHovered ? 0xFFFFD700 : 0xFFCCCCCC);
                guiGraphics.drawString(
                        this.font, name,
                        cellX + (CELL_SIZE - nameWidth) / 2,
                        cellY + CELL_SIZE - 11,
                        nameColor
                );
            }
        }

        guiGraphics.disableScissor();

        // ─── Scroll Bar ──────────────────────────────────────────
        if (maxScroll > 0) {
            int scrollBarX = this.width - 8;
            int scrollBarHeight = availableHeight;
            float scrollPercent = (float) scrollOffset / maxScroll;
            int thumbHeight = Math.max(20, scrollBarHeight * availableHeight / (maxScroll + availableHeight));
            int thumbY = gridStartY + (int) ((scrollBarHeight - thumbHeight) * scrollPercent);

            // Track
            guiGraphics.fill(scrollBarX, gridStartY, scrollBarX + 4, gridStartY + scrollBarHeight, 0x30FFFFFF);
            // Thumb
            guiGraphics.fill(scrollBarX, thumbY, scrollBarX + 4, thumbY + thumbHeight, 0xAAFFD700);
        }

        // ─── Render Widgets / Buttons & SearchBox ─────────────────
        // Calls Screen.render which only renders widgets because renderBackground is overridden to no-op
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // ─── Tooltip on hover (rendered last so it's always on top) ─────────
        for (int i = 0; i < displayedMorphList.size(); i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            int cellX = gridStartX + col * (CELL_SIZE + CELL_PADDING);
            int cellY = gridStartY + row * (CELL_SIZE + CELL_PADDING) - scrollOffset;

            if (mouseX >= cellX && mouseX < cellX + CELL_SIZE
                    && mouseY >= cellY && mouseY < cellY + CELL_SIZE
                    && mouseY >= gridStartY && mouseY < gridStartY + availableHeight) {

                ResourceLocation morphId = displayedMorphList.get(i);
                EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(morphId);
                if (entityType != null) {
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(entityType.getDescription().copy().withStyle(style ->
                            style.withColor(0xFFD700).withBold(true)));

                    var entry = com.kerem.morphmod.morph.MorphRegistry.getEntryByLocation(morphId);
                    if (entry != null) {
                        tooltip.add(Component.literal("❤ " + (entry.health() / 2) + " Hearts")
                                .withStyle(style -> style.withColor(0xFF5555)));
                        if (entry.activeAbility() != null && entry.activeAbility() != com.kerem.morphmod.morph.ActiveAbility.NONE) {
                            String activeName = entry.activeAbility().getId().replace("_", " ");
                            String activeStr = "✦ [G] " + activeName
                                    + " (" + String.format("%.1fs", entry.activeAbility().getCooldownSeconds()) + ")";
                            tooltip.add(Component.literal(activeStr)
                                    .withStyle(style -> style.withColor(0xFFAA00)));
                        }
                        if (!entry.passiveAbilities().isEmpty()) {
                            StringBuilder abilitiesStr = new StringBuilder("⚡ ");
                            for (var ability : entry.passiveAbilities()) {
                                abilitiesStr.append(ability.getId().replace("_", " ")).append(", ");
                            }
                            String abStr = abilitiesStr.substring(0, abilitiesStr.length() - 2);
                            tooltip.add(Component.literal(abStr)
                                    .withStyle(style -> style.withColor(0x55FFFF)));
                        }
                    }
                    guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
                }
                break;
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Intentionally empty: prevents Minecraft from applying blur shader or dirt background
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
        // Overridden to be empty: strictly disable Minecraft post-processing blur shader
    }

    @Override
    protected void renderMenuBackground(GuiGraphics guiGraphics) {
        // Overridden to be empty: disable dirt/tint background
    }

    @Override
    protected void renderMenuBackground(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // Overridden to be empty: disable dirt/tint background
    }

    @Override
    public void renderTransparentBackground(GuiGraphics guiGraphics) {
        // Overridden to be empty
    }

    /**
     * Draws a 1-pixel border rectangle.
     */
    private void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        guiGraphics.fill(x, y, x + width, y + 1, color);           // Top
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color); // Bottom
        guiGraphics.fill(x, y, x + 1, y + height, color);           // Left
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color);  // Right
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            int gridStartX = (this.width - (COLUMNS * (CELL_SIZE + CELL_PADDING))) / 2;
            int gridStartY = HEADER_HEIGHT;
            int availableHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT;

            if (mouseY >= gridStartY && mouseY < gridStartY + availableHeight) {
                for (int i = 0; i < displayedMorphList.size(); i++) {
                    int col = i % COLUMNS;
                    int row = i / COLUMNS;

                    int cellX = gridStartX + col * (CELL_SIZE + CELL_PADDING);
                    int cellY = gridStartY + row * (CELL_SIZE + CELL_PADDING) - scrollOffset;

                    if (mouseX >= cellX && mouseX < cellX + CELL_SIZE
                            && mouseY >= cellY && mouseY < cellY + CELL_SIZE) {

                        // Send morph request to server
                        ResourceLocation morphId = displayedMorphList.get(i);
                        ClientPlayNetworking.send(new MorphRequestPayload(morphId.toString()));
                        this.onClose();
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox != null && this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (keyCode == 256) { // ESC clears focus
                this.searchBox.setFocused(false);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox != null && this.searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount * 24));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause the game when the morph menu is open
    }
}
