import java.awt.Color;
import java.util.Random;

class PowerUp {
    double x, y, width, height; 
    int type;
    Color coreColor;        
    Color midGlowBaseColor; 
    Color outerGlowBaseColor; 
    boolean isCollected;

    private double glowPhase = 0;
    private static final double GLOW_SPEED_MID = 2.8; // Slightly faster
    private static final double GLOW_SPEED_OUTER = 2.1; 
    private static Random random = new Random();


    public PowerUp(double x, double y, double w, double h, int type) {
        this.x = x; this.y = y; this.width = w; this.height = h;
        this.type = type;
        this.isCollected = false;
        this.glowPhase = random.nextDouble() * Math.PI * 2; 

        switch (type) {
            case PowerUpType.SHIELD:
                this.coreColor = new Color(255, 255, 180);    
                this.midGlowBaseColor = new Color(255, 223, 0); 
                this.outerGlowBaseColor = new Color(255, 200, 0);  
                break;
            default:
                this.coreColor = new Color(150, 255, 255); 
                this.midGlowBaseColor = new Color(100, 220, 220);
                this.outerGlowBaseColor = new Color(50, 180, 180);
        }
    }

    /**
     * Updates the power-up's animation state.
     * @param dt Time elapsed since the last update, in seconds.
     */
    public void update(double dt) {
        if (!isCollected) {
            glowPhase += dt;
            if (glowPhase > Math.PI * 200) { // Prevent overflow after a very long time
                glowPhase -= Math.PI * 200;
            }
        }
    }

    public void draw(GameEngine ge, double chunkWorldX, double cameraX) {
        if (!isCollected) {
            double absoluteItemX = chunkWorldX + this.x; // PowerUp's x is relative to chunk
            double screenX = absoluteItemX - cameraX;    // Position on screen
            double screenY = this.y;                     // PowerUp's y is world coordinate

            double baseMinDim = Math.min(this.width, this.height);
            
            // Core
            double coreSize = baseMinDim * 0.55; 
            double coreDrawX = screenX + (this.width - coreSize) / 2;
            double coreDrawY = screenY + (this.height - coreSize) / 2;

            // Mid Glow Layer - pulsating size and alpha
            double midGlowBaseSize = baseMinDim * 0.85;
            double midGlowPulseFactor = 0.08 * Math.sin(glowPhase * GLOW_SPEED_MID); 
            double currentMidGlowSize = midGlowBaseSize * (1 + midGlowPulseFactor);
            int midGlowAlpha = 100 + (int)(50 * Math.sin(glowPhase * GLOW_SPEED_MID + Math.PI/2)); 
            Color currentMidGlowColor = new Color(midGlowBaseColor.getRed(), midGlowBaseColor.getGreen(), midGlowBaseColor.getBlue(), Math.max(0, Math.min(255, midGlowAlpha)));
            double midGlowDrawX = screenX + (this.width - currentMidGlowSize) / 2;
            double midGlowDrawY = screenY + (this.height - currentMidGlowSize) / 2;

            // Outer Glow Layer - pulsating size and alpha (different speed)
            double outerGlowBaseSize = baseMinDim * 1.25;
            double outerGlowPulseFactor = 0.10 * Math.sin(glowPhase * GLOW_SPEED_OUTER); 
            double currentOuterGlowSize = outerGlowBaseSize * (1 + outerGlowPulseFactor);
            int outerGlowAlpha = 60 + (int)(40 * Math.sin(glowPhase * GLOW_SPEED_OUTER + Math.PI)); 
            Color currentOuterGlowColor = new Color(outerGlowBaseColor.getRed(), outerGlowBaseColor.getGreen(), outerGlowBaseColor.getBlue(), Math.max(0, Math.min(255, outerGlowAlpha)));
            double outerGlowDrawX = screenX + (this.width - currentOuterGlowSize) / 2;
            double outerGlowDrawY = screenY + (this.height - currentOuterGlowSize) / 2;


            // Draw from outermost to innermost
            ge.changeColor(currentOuterGlowColor);
            ge.drawSolidRectangle(outerGlowDrawX, outerGlowDrawY, currentOuterGlowSize, currentOuterGlowSize);

            ge.changeColor(currentMidGlowColor);
            ge.drawSolidRectangle(midGlowDrawX, midGlowDrawY, currentMidGlowSize, currentMidGlowSize);

            ge.changeColor(this.coreColor); 
            ge.drawSolidRectangle(coreDrawX, coreDrawY, coreSize, coreSize);
        }
    }

    public boolean collidesWith(Player player) {
        if (isCollected) return false;
        // This method expects its x,y to be comparable to player's world x,y.
        // PlatformerGame.update creates a temporary PowerUp with world coordinates for this.
        return player.x < this.x + this.width &&
               player.x + player.collisionWidth > this.x &&
               player.y < this.y + this.height &&
               player.y + player.collisionHeight > this.y;
    }

    public void collect() {
        this.isCollected = true;
    }
}