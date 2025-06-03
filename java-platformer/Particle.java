import java.awt.Color;
import java.util.Random;

enum ParticleType {
    SHIELD_POP,
    JUMP_LAND,
    FIREBALL_HIT
}

class Particle {
    double x, y;        
    double vx, vy;      
    Color color;
    double lifespan;    
    double initialLifespan;
    double size;        
    double initialSize;
    // double endSize; // Not strictly needed if size goes to 0
    ParticleType type;
    double maxSizeForShieldPop; 

    private static Random random = new Random();

    public Particle(double x, double y, Color baseColor, ParticleType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        double baseSpeed, speedVariation;
        double angleSpread = 2 * Math.PI; 

        switch (type) {
            case SHIELD_POP:
                int r = Math.max(0, Math.min(255, baseColor.getRed() + random.nextInt(40) - 20));
                int g = Math.max(0, Math.min(255, baseColor.getGreen() + random.nextInt(40) - 20));
                int b = Math.max(0, Math.min(255, baseColor.getBlue() - random.nextInt(50) - 20)); 
                this.color = new Color(r, g, b);
                
                baseSpeed = 110; 
                speedVariation = 70;
                this.initialLifespan = 0.4 + random.nextDouble() * 0.35; 
                this.initialSize = 3 + random.nextDouble() * 3; 
                this.maxSizeForShieldPop = initialSize * (2.0 + random.nextDouble() * 1.0); 
                break;

            case JUMP_LAND:
                int greyTone = 180 + random.nextInt(40);
                this.color = new Color(greyTone, greyTone, greyTone, 180); 
                baseSpeed = 25; 
                speedVariation = 25;
                angleSpread = Math.PI / 1.8; 
                double baseAngle = -Math.PI / 2; 
                double angleForJumpLand = baseAngle - (angleSpread / 2) + random.nextDouble() * angleSpread;
                double speedForJumpLand = baseSpeed + random.nextDouble() * speedVariation;
                this.vx = Math.cos(angleForJumpLand) * speedForJumpLand;
                this.vy = Math.sin(angleForJumpLand) * speedForJumpLand; 
                
                this.initialLifespan = 0.35 + random.nextDouble() * 0.25; 
                this.initialSize = 6 + random.nextDouble() * 4; 
                this.size = this.initialSize; 
                this.lifespan = this.initialLifespan;
                return; 

            case FIREBALL_HIT:
                int rF = 220 + random.nextInt(36); 
                int gF = 80 + random.nextInt(100); 
                this.color = new Color(rF, gF, 0); 
                baseSpeed = 90; 
                speedVariation = 70;
                this.initialLifespan = 0.45 + random.nextDouble() * 0.35; 
                this.initialSize = 8 + random.nextDouble() * 6; 
                break;
            
            default: 
                this.color = Color.MAGENTA;
                baseSpeed = 50;
                speedVariation = 20;
                this.initialLifespan = 0.5;
                this.initialSize = 3;
                break;
        }
        
        double angle = random.nextDouble() * angleSpread;
        double speed = baseSpeed + random.nextDouble() * speedVariation;
        this.vx = Math.cos(angle) * speed;
        this.vy = Math.sin(angle) * speed;
        
        this.lifespan = this.initialLifespan;
        this.size = this.initialSize;
    }

    public boolean update(double dt) {
        this.x += this.vx * dt;
        this.y += this.vy * dt;
        this.lifespan -= dt;

        double lifeProgress = 1.0 - (this.lifespan / this.initialLifespan);

        switch (type) {
            case SHIELD_POP:
                if (lifeProgress < 0.20) { 
                    this.size = initialSize + (maxSizeForShieldPop - initialSize) * (lifeProgress / 0.20);
                } else { 
                    this.size = maxSizeForShieldPop * (1.0 - (lifeProgress - 0.20) / 0.80);
                }
                this.vy += 180 * dt; 
                break;
            case JUMP_LAND:
                this.size = initialSize * (1.0 - Math.pow(lifeProgress, 0.7)); 
                this.vy += 280 * dt; 
                this.vx *= (1 - 0.3 * dt); 
                break;
            case FIREBALL_HIT:
                 if (lifeProgress < 0.3) { 
                    this.size = initialSize + ((initialSize * 1.8) - initialSize) * (lifeProgress / 0.3); 
                } else { 
                    this.size = (initialSize * 1.8) * (1.0 - (lifeProgress - 0.3) / 0.7);
                }
                this.vy += 120 * dt; 
                break;
        }
        
        this.size = Math.max(0, this.size);
        return this.lifespan > 0;
    }

    public void draw(GameEngine ge, double cameraX) {
        if (this.lifespan <= 0 || this.size <= 0) {
            return;
        }

        int alpha = (int) (255 * (this.lifespan / this.initialLifespan));
        if (type == ParticleType.JUMP_LAND) {
            alpha = (int) (150 * (this.lifespan / this.initialLifespan)); 
        }
        alpha = Math.max(0, Math.min(255, alpha));
        
        Color drawColor = new Color(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), alpha);
        ge.changeColor(drawColor);

        ge.drawSolidRectangle(
            this.x - this.size / 2 - cameraX,
            this.y - this.size / 2,
            this.size,
            this.size
        );
    }
}
