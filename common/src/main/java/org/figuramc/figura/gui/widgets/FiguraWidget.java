package org.figuramc.figura.gui.widgets;


public interface FiguraWidget extends FiguraRenderable {
    boolean isVisible();
    void setVisible(boolean visible);
    int getX();
    void setX(int x);
    int getY();
    void setY(int y);
    int getWidth();
    void setWidth(int width);
    int getHeight();
    void setHeight(int height);
}
