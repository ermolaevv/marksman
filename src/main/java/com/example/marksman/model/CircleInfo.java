package com.example.marksman.model;

public class CircleInfo {
    public double x;
    public double y;
    public final double radius;
    public final double moveSpeed;

    public CircleInfo(double x, double y, double radius, double moveSpeed) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.moveSpeed = moveSpeed;
    }
}
