package org.figuramc.figura.gui.widgets;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EnumButton extends AbstractContainerElement {
    private final Button button;
    private int value;
    private final ContextMenu contextMenu;
    private final Consumer<Integer> onSelect;
    private final ArrayList<ITextComponent> names = new ArrayList<>();

    public EnumButton(int x, int y, int width, int height, String translationString, int defaultValue, int length, Consumer<Integer> onSelect) {
        super(x, y, width, height);
        value = defaultValue;
        ArrayList<ITextComponent> tooltips = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            names.add(new FiguraText(String.format("%s.%s", translationString, i)));
            tooltips.add(new FiguraText(String.format("%s.%s.tooltip", translationString, i)));
        }
        children.add(button = new Button(x,y,width,height, names.get(value % length), tooltips.get(value % length),this::onClick));
        contextMenu = new ContextMenu();
        contextMenu.setX(button.getX());
        contextMenu.setY(button.getY()+button.getHeight());

        this.onSelect = onSelect;
        for (int i = 0; i < length; i++) {
            ITextComponent c = names.get(i);
            ITextComponent t = tooltips.get(i);
            int finalI = i;
            contextMenu.addAction(c, t, b -> {
                button.setMessage(c);;
                button.setTooltip(t);
                onSelect.accept(finalI);
                updateContextText();
                value = finalI;
            });
        }
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        button.setX(x);
        contextMenu.setX(button.getX());
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        button.setY(y);
        contextMenu.setY(button.getY()+button.getHeight());
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        button.setWidth(width);
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        button.setHeight(height);
        contextMenu.setY(button.getY()+button.getHeight());
    }

    private void updateContextText() {
        // cache entries
        List<? extends AbstractFiguraWidget> entries = contextMenu.getEntries();

        // entries should have the same size as names
        // otherwise something went really wrong
        for (int i = 0; i < names.size(); i++) {
            // get text
            ITextComponent text = names.get(i);

            // selected entry
            if (i == (int) this.value % this.names.size())
                text = new TextComponentString("").setStyle(FiguraMod.getAccentColor()).setStyle(new Style().setUnderlined(true)).appendSibling(text);

            // apply text
            entries.get(i).setMessage(text);
        }
    }

    private void onClick(Button button) {
        contextMenu.setVisible(!contextMenu.isVisible());

        if (contextMenu.isVisible()) {
            UIHelper.setContext(this.contextMenu);
        }
    }
}
