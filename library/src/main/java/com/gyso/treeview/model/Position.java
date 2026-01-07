package com.gyso.treeview.model;

public class Position {
    public float x;
    public float y;
    public int viewWidth;
    public int viewHeight;

    public Position(float x, float y, int viewWidth, int viewHeight) {
        this.x = x;
        this.y = y;
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
    }

    public Position(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Position(Position position) {
        this.x = position.x;
        this.y = position.y;
        this.viewWidth = position.viewWidth;
        this.viewHeight = position.viewHeight;
    }

    public Position() {
    }

    @Override
    public String toString() {
        return "Position{" +
                "x=" + x +
                ", y=" + y +
                ", viewWidth=" + viewWidth +
                ", viewHeight=" + viewHeight +
                '}';
    }
}
