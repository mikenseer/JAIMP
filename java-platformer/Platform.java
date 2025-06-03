import java.awt.Color;

class Platform {
    double x, y, width, height; 
    int type;                   
    Color bodyColor; 
    Color topSurfaceColor; 
    Color bottomEdgeColor; 
    Color sideHighlightColor; // New: for a subtle side highlight

    public Platform(double x, double y, double w, double h, int type) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
        this.type = type;

        switch (type) {
            case PlatformType.SOLID: 
                this.bodyColor = new Color(50, 60, 80);     // Slightly darker base
                this.topSurfaceColor = new Color(100, 115, 140); // Brighter top
                this.bottomEdgeColor = new Color(30, 40, 60);   // Darker bottom
                this.sideHighlightColor = new Color(75, 85, 105); // Mid-tone for side
                break; 
            case PlatformType.HAZARD: 
                this.bodyColor = new Color(200, 20, 20);   
                this.topSurfaceColor = new Color(255, 60, 60); 
                this.bottomEdgeColor = new Color(160, 10, 10);  
                this.sideHighlightColor = new Color(230, 40, 40);
                break; 
            case PlatformType.GOAL: 
                this.bodyColor = new Color(30, 170, 30);   
                this.topSurfaceColor = new Color(70, 230, 70); 
                this.bottomEdgeColor = new Color(20, 130, 20);  
                this.sideHighlightColor = new Color(50, 200, 50);
                break;   
            case PlatformType.BOUNCE: 
                this.bodyColor = new Color(40, 160, 200);  
                this.topSurfaceColor = new Color(90, 200, 240); 
                this.bottomEdgeColor = new Color(30, 130, 170); 
                this.sideHighlightColor = new Color(60, 180, 220);
                break; 
            default: 
                this.bodyColor = Color.MAGENTA; 
                this.topSurfaceColor = Color.PINK;
                this.bottomEdgeColor = Color.DARK_GRAY;
                this.sideHighlightColor = Color.LIGHT_GRAY;
                break;
        }
    }

    public void draw(GameEngine ge, double chunkStartWorldX, double cameraX) {
        double absolutePlatformX = chunkStartWorldX + this.x;
        double screenX = absolutePlatformX - cameraX;
        
        double detailThickness = Math.min(this.height * 0.20, 5); // For top and side highlights
        double bottomEdgeThickness = Math.min(this.height * 0.15, 4);


        if (this.type == PlatformType.HAZARD) {
            // Flat top for hazard
            ge.changeColor(this.topSurfaceColor);
            double topSurfaceHeight = Math.min(this.height * 0.30, 8);
            if (this.height < 8) topSurfaceHeight = this.height; 
            ge.drawSolidRectangle(screenX, this.y, this.width, topSurfaceHeight);

            // Spikes
            double remainingHeightForSpikes = this.height - topSurfaceHeight;
            if (remainingHeightForSpikes > 4) { 
                ge.changeColor(this.bodyColor); 
                int spikeBaseNominalWidth = 12; 
                int numSpikes = Math.max(1, (int) (this.width / spikeBaseNominalWidth));
                double actualSpikeBaseWidth = this.width / numSpikes; 

                for (int i = 0; i < numSpikes; i++) {
                    int[] xPoints = { (int) (screenX + i * actualSpikeBaseWidth),
                                      (int) (screenX + (i + 1) * actualSpikeBaseWidth),
                                      (int) (screenX + i * actualSpikeBaseWidth + actualSpikeBaseWidth / 2) };
                    int[] yPoints = { (int) (this.y + topSurfaceHeight),
                                      (int) (this.y + topSurfaceHeight),
                                      (int) (this.y + topSurfaceHeight + remainingHeightForSpikes) };
                    ge.drawSolidPolygon(xPoints, yPoints, 3); 
                }
            } else if (remainingHeightForSpikes > 0) {
                ge.changeColor(this.bodyColor);
                ge.drawSolidRectangle(screenX, this.y + topSurfaceHeight, this.width, remainingHeightForSpikes);
            }
        } else { 
            // Main body
            ge.changeColor(this.bodyColor);
            ge.drawSolidRectangle(screenX, this.y, this.width, this.height);

            // Top surface
            if (this.height > detailThickness) {
                ge.changeColor(this.topSurfaceColor);
                ge.drawSolidRectangle(screenX, this.y, this.width, detailThickness);
            }
            
            // Bottom edge (if enough space and not overlapping top)
            if (this.bottomEdgeColor != null && this.height > detailThickness + bottomEdgeThickness + 1) { 
                 ge.changeColor(this.bottomEdgeColor);
                 ge.drawSolidRectangle(screenX, this.y + this.height - bottomEdgeThickness, this.width, bottomEdgeThickness);
            }

            // Subtle side highlight (e.g., on the left side)
            if (this.width > detailThickness && this.height > detailThickness) {
                ge.changeColor(this.sideHighlightColor);
                // Draw a thin vertical strip on the left edge, below top surface and above bottom edge
                double sideHighlightY = this.y + detailThickness;
                double sideHighlightHeight = this.height - detailThickness - (this.bottomEdgeColor != null ? bottomEdgeThickness : 0);
                if (sideHighlightHeight > 0) {
                    ge.drawSolidRectangle(screenX, sideHighlightY, detailThickness / 2, sideHighlightHeight);
                }
            }
        }
    }
}
