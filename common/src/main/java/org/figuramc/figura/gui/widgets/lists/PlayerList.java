package org.figuramc.figura.gui.widgets.lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.gui.screens.PermissionsScreen;
import org.figuramc.figura.gui.widgets.FiguraGuiEventListener;
import org.figuramc.figura.gui.widgets.SearchBar;
import org.figuramc.figura.gui.widgets.SwitchButton;
import org.figuramc.figura.gui.widgets.permissions.AbstractPermPackElement;
import org.figuramc.figura.gui.widgets.permissions.CategoryPermPackElement;
import org.figuramc.figura.gui.widgets.permissions.PlayerPermPackElement;
import org.figuramc.figura.permissions.PermissionManager;
import org.figuramc.figura.permissions.PermissionPack;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.MathUtils;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerList extends AbstractList {

    private final HashMap<UUID, PlayerPermPackElement> players = new HashMap<>();
    private final HashSet<UUID> missingPlayers = new HashSet<>();

    private final ArrayList<AbstractPermPackElement> permissionsList = new ArrayList<>();

    public final PermissionsScreen parent;
    private final SearchBar searchBar;
    private final SwitchButton showFigura, showDisconnected;
    private static boolean showFiguraBl, showDisconnectedBl;
    private final int entryWidth;

    private int totalHeight = 0;
    private AbstractPermPackElement maxCategory;
    public AbstractPermPackElement selectedEntry;
    private String filter = "";

    public PlayerList(int x, int y, int width, int height, PermissionsScreen parent) {
        super(x, y, width, height);
        updateScissors(1, 24, -2, -25);

        this.parent = parent;
        this.entryWidth = Math.min(width - scrollBar.getWidth() - 12, 174);

        // fix scrollbar y and height
        scrollBar.setY(y + 28);
        scrollBar.setHeight(height - 32);

        // search bar
        children.add(searchBar = new SearchBar(x + 4, y + 4, width - 56, 20, new GuiPageButtonList.GuiResponder() {
            @Override
            public void setEntryValue(int i, boolean value) {}
            @Override
            public void setEntryValue(int i, float value) {}
            @Override
            public void setEntryValue(int i, String value) {
                if (!filter.equals(value))
                    scrollBar.setScrollProgress(0f);
                filter = value;
            }
        }));

        // show figura only button
        children.add(showFigura = new SwitchButton(x + width - 48, y + 4, 20, 20, 0, 0, 20, new FiguraIdentifier("textures/gui/show_figura.png"), 60, 40, new FiguraText("gui.permissions.figura_only.tooltip"), button -> showFiguraBl = ((SwitchButton) button).isToggled()));
        showFigura.setToggled(showFiguraBl);

        // show disconnected button
        children.add(showDisconnected = new SwitchButton(x + width - 24, y + 4, 20, 20, 0, 0, 20, new FiguraIdentifier("textures/gui/show_disconnected.png"), 60, 40, new FiguraText("gui.permissions.disconnected.tooltip"), button -> showDisconnectedBl = ((SwitchButton) button).isToggled()));
        showDisconnected.setToggled(showDisconnectedBl);

        // initial load
        loadGroups();
        loadPlayers();

        // select self
        selectLocalPlayer();
    }

    @Override
    public void tick() {
        // update players
        loadPlayers();
        super.tick();
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();

        // background
        UIHelper.renderSliced(x, y, width, height, UIHelper.OUTLINE_FILL);

        totalHeight = 0;
        for (AbstractPermPackElement pack : permissionsList) {
            if (pack.isVisible())
                totalHeight += pack.getHeight() + 8;
        }

        // scrollbar visible
        boolean hasScrollbar = totalHeight > height - 32;
        scrollBar.setVisible(hasScrollbar);
        scrollBar.setScrollRatio(permissionsList.isEmpty() ? 0f : (float) totalHeight / permissionsList.size(), totalHeight - (height - 32));

        // scissors
        this.scissorsWidth = hasScrollbar ? -scrollBar.getWidth() - 5 : -2;
        UIHelper.setupScissor(x + scissorsX, y + scissorsY, width + scissorsWidth, height + scissorsHeight);

        // render stuff
        int xOffset = (width - entryWidth - (scrollBar.isVisible() ? 13 : 0)) / 2;
        int playerY = scrollBar.isVisible() ? (int) -(MathUtils.lerp(scrollBar.getScrollProgress(), -32, totalHeight - height)) : 32;

        int minY = y + scissorsY;
        int maxY = minY + height + scissorsHeight;
        for (AbstractPermPackElement pack : permissionsList) {
            if (!pack.isVisible())
                continue;

            pack.setX(x + xOffset);
            pack.setY(y + playerY);

            if (pack.getY() + pack.getHeight() > minY && pack.getY() < maxY)
                pack.draw(mc, mouseX, mouseY, delta);

            playerY += pack.getHeight() + 8;
        }

        // reset scissor
        UIHelper.disableScissor();

        // render children
        super.draw(mc, mouseX, mouseY, delta);
    }

    @Override
    public List<? extends FiguraGuiEventListener> contents() {
        return permissionsList;
    }

    private void loadGroups() {
        for (PermissionPack container : PermissionManager.CATEGORIES.values()) {
            CategoryPermPackElement group = new CategoryPermPackElement(entryWidth, container, this);
            permissionsList.add(group);
            children.add(group);
            maxCategory = group;
        }
    }

    private void loadPlayers() {
        // reset missing players
        missingPlayers.clear();
        missingPlayers.addAll(players.keySet());

        // for all players
        NetHandlerPlayClient connection = Minecraft.getMinecraft().getConnection();
        List<UUID> playerList = connection == null ? new ArrayList<>() : new ArrayList<>(connection.getPlayerInfoMap().stream().map(networkPlayerInfo -> networkPlayerInfo.getGameProfile().getId()).collect(Collectors.toList()));
        for (UUID uuid : playerList) {
            // get player
            NetworkPlayerInfo player = connection.getPlayerInfo(uuid);
            if (player == null)
                continue;

            // get player data
            String name = player.getGameProfile().getName();
            ResourceLocation skin = player.getLocationSkin();
            Avatar avatar = AvatarManager.getAvatarForPlayer(uuid);

            // filter check
            if ((!name.toLowerCase().contains(filter.toLowerCase()) && !uuid.toString().contains(filter.toLowerCase())) || (showFigura.isToggled() && !FiguraMod.isLocal(uuid) && (avatar == null || avatar.nbt == null)))
                continue;

            // player is not missing
            missingPlayers.remove(uuid);

            PlayerPermPackElement element = players.computeIfAbsent(uuid, uuid1 -> {
                PlayerPermPackElement entry = new PlayerPermPackElement(entryWidth, name, PermissionManager.get(uuid1), skin, uuid1, this);

                permissionsList.add(entry);
                children.add(entry);

                return entry;
            });
            element.disconnected = false;
        }

        if (filter.isEmpty() && showDisconnected.isToggled()) {
            for (Avatar avatar : AvatarManager.getLoadedAvatars()) {
                UUID id = avatar.owner;

                if (playerList.contains(id))
                    continue;

                missingPlayers.remove(id);

                PlayerPermPackElement element = players.computeIfAbsent(id, uuid -> {
                    PlayerPermPackElement entry = new PlayerPermPackElement(entryWidth, avatar.entityName, PermissionManager.get(uuid), null, uuid, this);

                    permissionsList.add(entry);
                    children.add(entry);

                    return entry;
                });
                element.disconnected = true;
            }
        }

        // remove missing players
        for (UUID missingID : missingPlayers) {
            PlayerPermPackElement entry = players.remove(missingID);
            permissionsList.remove(entry);
            children.remove(entry);
        }

        sortList();

        // select local if current selected is missing
        if (selectedEntry instanceof PlayerPermPackElement && missingPlayers.contains(((PlayerPermPackElement) selectedEntry).getOwner())) {
            PlayerPermPackElement player = (PlayerPermPackElement) selectedEntry;
            selectLocalPlayer();
        }
    }

    private void sortList() {
        permissionsList.sort(AbstractPermPackElement::compareTo);
        children.sort((element1, element2) -> {
            if (element1 instanceof AbstractPermPackElement && element2 instanceof AbstractPermPackElement) {
                AbstractPermPackElement container1 = (AbstractPermPackElement) element1;
                AbstractPermPackElement container2 = (AbstractPermPackElement) element2;
                return container1.compareTo(container2);
            }
            return 0;
        });
    }

    private void selectLocalPlayer() {
        PlayerPermPackElement local = Minecraft.getMinecraft().player != null ? players.get(Minecraft.getMinecraft().player.getUniqueID()) : null;
        if (local != null) {
            local.widgetPressed(0,0);
        } else {
            maxCategory.widgetPressed(0,0);
        }

        scrollToSelected();
    }

    public void updateScroll() {
        // store old scroll pos
        double pastScroll = (totalHeight - getHeight()) * scrollBar.getScrollProgress();

        // get new height
        totalHeight = 0;
        for (AbstractPermPackElement pack : permissionsList) {
            if (pack.isVisible())
                totalHeight += pack.getHeight() + 8;
        }

        // set new scroll percentage
        scrollBar.setScrollProgress(pastScroll / (totalHeight - getHeight()));
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        scrollBar.setY(y + 28);
        searchBar.setY(y + 4);
        showFigura.setY(y + 4);
        showDisconnected.setY(y + 4);
    }

    public int getCategoryAt(double y) {
        int ret = -1;
        for (AbstractPermPackElement element : permissionsList)
            if (element instanceof CategoryPermPackElement && ((CategoryPermPackElement) element).isVisible() && y >= ((CategoryPermPackElement) element).getY()) {
                CategoryPermPackElement group = (CategoryPermPackElement) element;
                ret++;
            }
        return Math.max(ret, 0);
    }

    public void scrollToSelected() {
        double y = 0;

        // get height
        totalHeight = 0;
        for (AbstractPermPackElement pack : permissionsList) {
            if (pack instanceof PlayerPermPackElement && !pack.isVisible())
                continue;

            if (pack == selectedEntry)
                y = totalHeight;
            else
                totalHeight += pack.getHeight() + 8;
        }

        // set scroll
        scrollBar.setScrollProgressNoAnim(y / totalHeight);
    }

}
