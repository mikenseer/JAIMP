class LevelChunk {
    Platform[] platforms;   
    PowerUp[] powerUps;     
    double chunkWidth;      
    public double startWorldX; // Absolute starting X-coordinate of this chunk in the world

    public LevelChunk(Platform[] platforms, PowerUp[] powerUps, double chunkWidth, double startWorldX) {
        this.platforms = platforms;
        this.powerUps = (powerUps != null) ? powerUps : new PowerUp[0];
        this.chunkWidth = chunkWidth;
        this.startWorldX = startWorldX;
    }

    // Draw method now uses its own startWorldX
    public void draw(GameEngine ge, double cameraX) { 
        for (Platform p : platforms) {
            // p.x is relative to the chunk's start
            double platformScreenX = this.startWorldX + p.x - cameraX;
            if (platformScreenX + p.width > 0 && platformScreenX < ge.width()) {
                 p.draw(ge, this.startWorldX, cameraX); // Pass chunk's world start X to platform draw
            }
        }

        for (PowerUp pu : powerUps) {
            if (!pu.isCollected) { 
                double powerUpScreenX = this.startWorldX + pu.x - cameraX;
                 if (powerUpScreenX + pu.width > 0 && powerUpScreenX < ge.width()) {
                    pu.draw(ge, this.startWorldX, cameraX); // Pass chunk's world start X to powerup draw
                }
            }
        }
    }
}
