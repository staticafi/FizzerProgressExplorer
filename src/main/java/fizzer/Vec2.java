package fizzer;

public class Vec2 {
    public float x;
    public float y;

    public static Vec2 zero = new Vec2(0, 0);
    public static Vec2 basisX = new Vec2(1, 0);
    public static Vec2 basisY = new Vec2(0, 1);

    public Vec2(float x_, float y_) {
        x = x_;
        y = y_;
    }

    public void set(Vec2 other) {
        x = other.x;
        y = other.y;
    }

    public Vec2 copy() {
        return new Vec2(x, y);
    }

    public Vec2 add(Vec2 other) {
        return new Vec2(x + other.x, y + other.y);
    }

    public Vec2 sub(Vec2 other) {
        return new Vec2(x - other.x, y - other.y);
    }

    public Vec2 mul(float c) {
        return new Vec2(c * x, c  * y);
    }

    public Vec2 neg() {
        return new Vec2(-x, -y);
    }

    public Vec2 orthogonal() {
        return new Vec2(-y, x);
    }

    public float dot(Vec2 other) {
        return x * other.x + y * other.y;
    }

    public float length() {
        return (float)Math.sqrt(x * x + y * y);
    }
}
