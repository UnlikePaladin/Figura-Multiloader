package org.figuramc.figura.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.gui.widgets.Button;
import org.figuramc.figura.gui.widgets.FiguraRenderable;
import org.figuramc.figura.gui.widgets.Label;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.ui.UIHelper;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;

public class GameScreen extends AbstractPanelScreen {

    private static final int[][] RULES = {
            {0, 0, 0, 1, 0, 0, 0, 0, 0}, // dead
            {0, 0, 1, 1, 0, 0, 0, 0, 0} // alive
    };

    private Label keys, stats;
    private Grid grid;
    private boolean paused = false;
    private static int scale = 5;

    protected GameScreen(GuiScreen parentScreen) {
        super(parentScreen, new TextComponentString(""));
    }

    @Override
    public Class<? extends GuiScreen> getSelectedPanel() {
        return parentScreen.getClass();
    }

    public void initGui() {
        super.initGui();
        this.removeWidget(panels);

        addRenderableOnly(grid = new Grid(width, height));

        // back button
        addRenderableWidget(new Button(this.width - 20, 4, 16, 16, 0, 0, 16, new FiguraIdentifier("textures/gui/search_clear.png"), 48, 16, new FiguraText("gui.done"), bx -> onGuiClosed()));

        // text
        addRenderableWidget(keys = new Label(
                new TextComponentString("")
                        .appendSibling(new TextComponentString("[R]").setStyle(FiguraMod.getAccentColor()))
                        .appendText(" restart, ")
                        .appendSibling(new TextComponentString("[P]").setStyle(FiguraMod.getAccentColor()))
                        .appendText(" pause, ")
                        .appendSibling(new TextComponentString("[SPACE]").setStyle(FiguraMod.getAccentColor()))
                        .appendText(" step")
                        .appendText("\n")
                        .appendSibling(new TextComponentString("[F1]").setStyle(FiguraMod.getAccentColor()))
                        .appendText(" hide text, ")
                        .appendSibling(new TextComponentString("[Scroll]").setStyle(FiguraMod.getAccentColor()))
                        .appendText(" scale (restarts)"),
                4, 4, 0)
        );
        addRenderableWidget(stats = new Label("", 4, keys.getRawY() + keys.getHeight(), 0));
    }

    @Override
    public void tick() {
        super.tick();
        if (!paused) grid.tick();
        stats.setText(
                new TextComponentString("Generation")
                        .appendSibling(new TextComponentString(" " + grid.gen).setStyle(FiguraMod.getAccentColor()))
                        .appendText(", Scale")
                        .appendSibling(new TextComponentString(" " + scale).setStyle(FiguraMod.getAccentColor()))
        );
    }

    @Override
    public void keyTyped(char keyCode, int scanCode) {
        switch (scanCode) {
            case Keyboard.KEY_R:
                grid.init();
                break;
            case Keyboard.KEY_P:
                paused = !paused;
                break;
            case Keyboard.KEY_SPACE:
                grid.tick();
                break;
            case Keyboard.KEY_F1:
                keys.setVisible(!keys.isVisible());
                stats.setVisible(!stats.isVisible());
                break;
            default:
                super.keyTyped(keyCode, scanCode);
        }

    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        scale = (int) Math.max(1, scale + Math.signum(amount));
        grid.init();
        return true;
    }

    private static class Grid implements FiguraRenderable {

        private Cell[][] grid;
        private final int width, height;
        private long gen = 0;

        private Grid(int width, int height) {
            this.width = width;
            this.height = height;
            init();
        }

        private void init() {
            gen = 0;
            int width = this.width / scale;
            int height = this.height / scale;

            // create grid
            grid = new Cell[width][height];

            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    // create cell :D
                    Cell cell = new Cell(i, j, (int) Math.round(Math.random()));

                    // neighbours
                    if (i > 0) {
                        if (j > 0) cell.addNeighbor(grid[i - 1][j - 1]); // top left
                        cell.addNeighbor(grid[i - 1][j]); // top middle
                        if (j < height - 1) cell.addNeighbor(grid[i - 1][j + 1]); // top right
                    }
                    if (j > 0) cell.addNeighbor(grid[i][j - 1]); // left

                    grid[i][j] = cell;
                }
            }
        }

        private void tick() {
            gen++;
            for (Cell[] cells : grid)
                for (Cell cell : cells)
                    cell.update();
        }

        @Override
        public void draw(Minecraft mc, int mouseX, int mouseY, float delta) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(scale, scale, scale);

            for (Cell[] cells : grid) {
                for (Cell cell : cells) {
                    cell.draw(mc);
                }
            }

            GlStateManager.popMatrix();
        }
    }

    private static class Cell {
        private final int x, y;
        private int alive, future;
        private final ArrayList<Cell> neighbors = new ArrayList<>();
        private int color = 0xFFFFFFFF;

        private Cell(int x, int y, int alive) {
            this.x = x;
            this.y = y;
            this.alive = alive;
            this.future = alive;
        }

        private void addNeighbor(Cell cell) {
            this.neighbors.add(cell);
            cell.neighbors.add(this);
        }

        private void update() {
            int neigh = 0;
            for (Cell cell : neighbors)
                neigh += cell.alive;
            this.future = RULES[this.alive][neigh];
        }

        private void draw(Minecraft mc) {
            this.alive = this.future;
            if (this.alive == 1)
                UIHelper.fill(this.x, this.y, this.x + 1, this.y + 1, color);
        }
    }
}
