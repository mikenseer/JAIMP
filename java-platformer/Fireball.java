import java.awt.Color;

class Fireball {
    double x, y, radius;    
    double speed;           

    public Fireball(double startX, double startY, double radius, double speed) {
        this.x = startX;
        this.y = startY;
        this.radius = radius; // This is the actual hit collider radius
        this.speed = speed; 
    }

    public void update(double dt) {
        x -= speed * dt; 
    }

    public void draw(GameEngine ge, double cameraX) {
        double screenCenterX = this.x - cameraX;

        // All visual layers will be within or at this.radius

        // Layer 1: Outer "casing" or "exhaust glow" - matches collider radius
        // For a bullet/rocket, this could be a slightly darker, less intense part of the effect, or a faint glow.
        Color colorOuter = new Color(200, 80, 0, 100); // Darker orange, semi-transparent
        double radiusOuter = this.radius; // Visual matches collider
        ge.changeColor(colorOuter);
        ge.drawSolidCircle(screenCenterX, this.y, radiusOuter);

        // Layer 2: Main body / brighter flame
        Color colorMid = new Color(255, 140, 0, 200); // Bright orange
        double radiusMid = this.radius * 0.75; // Smaller than collider
        ge.changeColor(colorMid);
        ge.drawSolidCircle(screenCenterX, this.y, radiusMid);

        // Layer 3: Hot core / "bullet tip"
        Color colorCore = new Color(255, 220, 150, 255); // Bright yellow/white, opaque
        double radiusCore = this.radius * 0.4; // Smallest, brightest part
        ge.changeColor(colorCore);
        ge.drawSolidCircle(screenCenterX, this.y, radiusCore);

        // Optional: Add a small rectangular "fin" or "trail" element if desired for more rocket look,
        // but this would deviate from purely circular. For now, concentric circles give a projectile feel.
    }

    public boolean collidesWith(Player player) {
        double closestX = Math.max(player.x, Math.min(this.x, player.x + player.collisionWidth));
        double closestY = Math.max(player.y, Math.min(this.y, player.y + player.collisionHeight));
        double distanceX = this.x - closestX;
        double distanceY = this.y - closestY;
        double distanceSquared = (distanceX * distanceX) + (distanceY * distanceY);
        return distanceSquared < (this.radius * this.radius);
    }
}
