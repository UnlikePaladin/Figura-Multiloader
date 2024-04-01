package org.figuramc.figura.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.figuramc.figura.config.ConfigManager;
import org.figuramc.figura.config.ConfigType;
import org.figuramc.figura.gui.PaperDoll;
import org.figuramc.figura.gui.widgets.Button;
import org.figuramc.figura.gui.widgets.Label;
import org.figuramc.figura.gui.widgets.SearchBar;
import org.figuramc.figura.gui.widgets.lists.ConfigList;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.IOUtils;
import org.figuramc.figura.utils.NbtType;
import org.figuramc.figura.utils.TextUtils;
import org.figuramc.figura.utils.ui.UIHelper;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;

public class ConfigScreen extends AbstractPanelScreen {

    public static final Map<ConfigType.Category, Boolean> CATEGORY_DATA = new HashMap<>();

    private ConfigList list;
    private Button cancel;
    private final boolean hasPanels;
    public boolean renderPaperdoll;

    public ConfigScreen(GuiScreen parentScreen) {
        this(parentScreen, true);
    }

    public ConfigScreen(GuiScreen parentScreen, boolean enablePanels) {
        super(parentScreen, new FiguraText("gui.panels.title.settings"));
        this.hasPanels = enablePanels;
    }

    @Override
    public void initGui() {
        super.initGui();
        loadNbt();

        if (!hasPanels) {
            this.removeWidget(panels);
            Label l;
            this.addRenderableWidget(l = new Label(getTitle(), this.width / 2, 14, TextUtils.Alignment.CENTER));
            l.centerVertically = true;
        }

        // -- middle -- //

        int width = Math.min(this.width - 8, 420);
        list = new ConfigList((this.width - width) / 2, 52, width, height - 80, this);

        this.addRenderableWidget(new SearchBar(this.width / 2 - 122, 28, 244, 20, new GuiPageButtonList.GuiResponder() {
            @Override
            public void setEntryValue(int i, boolean value) {
                list.updateSearch(String.valueOf(value));
            }

            @Override
            public void setEntryValue(int i, float value) {
                list.updateSearch(String.valueOf(value));
            }

            @Override
            public void setEntryValue(int i, String value) {
                list.updateSearch(value.toLowerCase());
            }
        }));
        this.addRenderableWidget(list);

        // -- bottom buttons -- //

        // cancel
        this.addRenderableWidget(cancel = new Button(this.width / 2 - 122, height - 24, 120, 20, new FiguraText("gui.cancel"), null, button -> {
            ConfigManager.discardConfig();
            list.updateList();
        }));
        cancel.setActive(false);

        // done
        addRenderableWidget(new Button(this.width / 2 + 2, height - 24, 120, 20, new FiguraText("gui.done"), null, button -> onGuiClosed()));
    }

    @Override
    public void tick() {
        super.tick();
        this.cancel.setActive(list.hasChanges());
    }

    @Override
    public void onGuiClosed() {
        ConfigManager.applyConfig();
        ConfigManager.saveConfig();
        saveNbt();
        super.onGuiClosed();
    }

    @Override
    public void renderBackground(float delta) {
        super.renderBackground(delta);
        if (renderPaperdoll)
            UIHelper.renderWithoutScissors(() -> PaperDoll.render(true));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        // -100 from Mojank's GuiControls
        if (list.updateKey(-100 + button))
            return;

        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void keyTyped(char c, int scanCode) {
        // 1 is Escape here / 256 for whatever reason, 0 is unbound
        if(list.updateKey(scanCode == Keyboard.KEY_ESCAPE ? 0: scanCode != 0 ? scanCode : (c+256)))
            return;

        super.keyTyped(c, scanCode);
    }

    private static void loadNbt() {
        IOUtils.readCacheFile("settings", nbt -> {
            NBTTagList groupList = nbt.getTagList("settings", NbtType.COMPOUND.getValue());
            for (int i = 0; i < groupList.tagCount(); i++) {
                NBTTagCompound compound = groupList.getCompoundTagAt(i);

                String config = compound.getString("config");
                boolean expanded = compound.getBoolean("expanded");
                CATEGORY_DATA.put(ConfigManager.CATEGORIES_REGISTRY.get(config), expanded);
            }
        });
    }

    private static void saveNbt() {
        IOUtils.saveCacheFile("settings", nbt -> {
            NBTTagList list = new NBTTagList();

            for (Map.Entry<ConfigType.Category, Boolean> entry : CATEGORY_DATA.entrySet()) {
                NBTTagCompound compound = new NBTTagCompound();
                compound.setString("config", entry.getKey().id);
                compound.setBoolean("expanded", entry.getValue());
                list.appendTag(compound);
            }

            nbt.setTag("settings", list);
        });
    }

    public static void clearCache() {
        IOUtils.deleteCacheFile("settings");
    }
}
