import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

public class PlatformerGame extends GameEngine {
    Player player;
    LinkedList<LevelChunk> activeChunks;

    int currentChunkArrayIndex = 0;
    double cameraX = 0;

    final double CHUNK_LENGTH_IN_SCREENS = 4.0;
    double actualChunkLength;

    boolean gameOverActive = false;

    double initialPlayerSpawnX;
    double initialPlayerSpawnY;

    ArrayList<Fireball> fireballs;
    Random randomGenerator;
    double fireballSpawnTimer = 0;
    double nextFireballSpawnInterval = 2.0;

    boolean keyLeftPressed = false;
    boolean keyRightPressed = false;
    boolean keyCrouchPressed = false;
    boolean jumpKeyJustPressed = false;
    boolean jumpKeyCurrentlyHeld = false;

    private boolean titleScreenActive = true;
    private boolean gameLogicActive = false;

    private ArrayList<Particle> activeParticles;

    private ArrayList<BackgroundElement> stars;
    private ArrayList<BackgroundElement> farDistantBuildings;
    private ArrayList<BackgroundElement> distantBuildings;
    private ArrayList<BackgroundElement> midClouds;
    private ArrayList<BackgroundElement> nearClouds;

    private double starsPatternWidth;
    private double farDistantBuildingsPatternWidth;
    private double distantBuildingsPatternWidth;
    private double midCloudsPatternWidth;
    private double nearCloudsPatternWidth;

    private double lastGeneratedChunkEndX = 0;
    private int chunksCompleted = 0;
    private final int MAX_LOADED_CHUNKS_AHEAD = 2;
    private final int MAX_LOADED_CHUNKS_BEHIND = 1;
    private final int TARGET_ACTIVE_CHUNKS = 1 + MAX_LOADED_CHUNKS_BEHIND + MAX_LOADED_CHUNKS_AHEAD;
    private final int MAX_ACTIVE_CHUNKS_BUFFER = TARGET_ACTIVE_CHUNKS + 1;
    private final double CHUNK_GENERATION_TRIGGER_SCREENS_BEFORE_END = 1.5;

    private static class BackgroundElement {
        double initialXOffsetInPattern;
        double y, width, height;
        Color color;
        double parallaxFactor;

        BackgroundElement(double initialXOffset, double y, double w, double h, Color c, double pFactor) {
            this.initialXOffsetInPattern = initialXOffset;
            this.y = y; this.width = w; this.height = h;
            this.color = c; this.parallaxFactor = pFactor;
        }

        public void draw(GameEngine ge, double cameraX, double gameWidth, double layerPatternWidth) {
            if (layerPatternWidth <= 0) return;
            double parallaxShift = cameraX * this.parallaxFactor;
            double patternOffset = parallaxShift % layerPatternWidth;
            if (patternOffset < 0) {
                patternOffset += layerPatternWidth;
            }
            double firstInstanceScreenX = this.initialXOffsetInPattern - patternOffset;

            for (double currentScreenX = firstInstanceScreenX - layerPatternWidth; currentScreenX < gameWidth + this.width ; currentScreenX += layerPatternWidth) {
                 if (currentScreenX + this.width > 0 && currentScreenX < gameWidth) {
                    ge.changeColor(this.color);
                    ge.drawSolidRectangle(currentScreenX, this.y, this.width, this.height);
                    if (this instanceof BuildingBackgroundElement) {
                        ((BuildingBackgroundElement)this).drawDetails(ge, currentScreenX, this.y);
                    }
                 }
            }
        }
    }

    private static class BuildingBackgroundElement extends BackgroundElement {
        ArrayList<BuildingDetail> details;
        BuildingBackgroundElement(double initialXOffset, double y, double w, double h, Color c, double pFactor, Random rand) {
            super(initialXOffset, y, w, h, c, pFactor);
            if (rand != null) {
                details = new ArrayList<>();
                int numWindows = rand.nextInt(4) + (int)(w * h / 4500) + 2;
                for (int i = 0; i < numWindows; i++) {
                    double winWidth = 4 + rand.nextDouble() * (w * 0.04);
                    winWidth = Math.max(2, winWidth);
                    double winHeight = 5 + rand.nextDouble() * (h * 0.06);
                    winHeight = Math.max(3, winHeight);
                    double winX = rand.nextDouble() * (w - winWidth - 4) + 2;
                    double winY = rand.nextDouble() * (h - winHeight - (h*0.10)) + (h*0.05);
                    Color winColor = new Color(Math.min(255,c.getRed() + 25 + rand.nextInt(25)),
                                             Math.min(255,c.getGreen() + 25 + rand.nextInt(25)),
                                             Math.min(255,c.getBlue() + 35 + rand.nextInt(25)),
                                             60 + rand.nextInt(70));
                    if (rand.nextDouble() < 0.45) {
                        winColor = new Color(Math.max(0,c.getRed() - 10 - rand.nextInt(10)),
                                             Math.max(0,c.getGreen() - 10- rand.nextInt(10)),
                                             Math.max(0,c.getBlue() - 10- rand.nextInt(10)), 170);
                    }
                    details.add(new BuildingDetail(winX, winY, winWidth, winHeight, winColor));
                }
                if (rand.nextDouble() < 0.5) {
                    int numClutter = rand.nextInt(2) + 1;
                    for (int i = 0; i < numClutter; i++) {
                        double clutterWidth = 2 + rand.nextDouble() * Math.max(3, (w * 0.05));
                        double clutterHeight = 6 + rand.nextDouble() * Math.min(20, h * 0.08);
                        double clutterX = rand.nextDouble() * (w - clutterWidth);
                        double clutterY = -clutterHeight;
                        Color clutterColor = new Color(Math.max(0,c.getRed() - 25), Math.max(0,c.getGreen() - 25), Math.max(0,c.getBlue() - 25), 255);
                        details.add(new BuildingDetail(clutterX, clutterY, clutterWidth, clutterHeight, clutterColor));
                    }
                }
            }
        }
        public void drawDetails(GameEngine ge, double buildingScreenX, double buildingScreenY) {
            if (details != null) {
                for (BuildingDetail detail : details) {
                    ge.changeColor(detail.color);
                    ge.drawSolidRectangle(buildingScreenX + detail.relX, buildingScreenY + detail.relY, detail.width, detail.height);
                }
            }
        }
    }
    private static class BuildingDetail {
        double relX, relY, width, height; Color color;
        BuildingDetail(double rx, double ry, double w, double h, Color c) {
            relX = rx; relY = ry; width = w; height = h; color = c;
        }
    }

    private double calculatePatternWidth(ArrayList<BackgroundElement> elements, double minMultiplier, double maxMultiplier) {
        if (elements == null || elements.isEmpty()) return width() * minMultiplier;
        double maxX = 0;
        for (BackgroundElement el : elements) {
            if (el.initialXOffsetInPattern + el.width > maxX) {
                maxX = el.initialXOffsetInPattern + el.width;
            }
        }
        double patternW = Math.max(maxX + width() * 0.5, width() * minMultiplier);
        return Math.min(patternW, width() * maxMultiplier);
    }

    private void initializeBackgroundElements() {
        stars = new ArrayList<>(); farDistantBuildings = new ArrayList<>();
        distantBuildings = new ArrayList<>(); midClouds = new ArrayList<>(); nearClouds = new ArrayList<>();
        int gameW = width(); int gameH = height();
        double currentXOffset;

        currentXOffset = 0;
        for (int i = 0; i < 250; i++) {
            double sX = currentXOffset;
            double sY = randomGenerator.nextDouble() * (gameH * 0.85);
            double sSize = 0.5 + randomGenerator.nextDouble() * 1.0;
            Color starColor;
            double rVal = randomGenerator.nextDouble();
            if (rVal < 0.6) starColor = new Color(255, 255, 240, 100 + randomGenerator.nextInt(100));
            else if (rVal < 0.8) starColor = new Color(200, 220, 255, 80 + randomGenerator.nextInt(100));
            else if (rVal < 0.95) starColor = new Color(255, 200, 200, 80 + randomGenerator.nextInt(100));
            else starColor = new Color(220,220,255, 50 + randomGenerator.nextInt(50));
            stars.add(new BackgroundElement(sX, sY, sSize, sSize, starColor, 0.01 + randomGenerator.nextDouble() * 0.015));
            currentXOffset += (gameW * 0.02 + randomGenerator.nextDouble() * 20);
        }
        starsPatternWidth = calculatePatternWidth(stars, 3.0, 5.0);

        currentXOffset = 0;
        for (int i = 0; i < 15; i++) {
            double bWidth = 60 + randomGenerator.nextDouble() * 150;
            double bHeight = gameH * (0.15 + randomGenerator.nextDouble() * 0.45);
            double bY = gameH - bHeight + (gameH * (0.30 + randomGenerator.nextDouble() * 0.20));
            Color bColor = new Color(10 + randomGenerator.nextInt(8), 12 + randomGenerator.nextInt(8), 18 + randomGenerator.nextInt(12), 255);
            farDistantBuildings.add(new BuildingBackgroundElement(currentXOffset, bY, bWidth, bHeight, bColor, 0.06 + randomGenerator.nextDouble() * 0.04, randomGenerator));
            currentXOffset += bWidth + (40 + randomGenerator.nextDouble() * 80);
        }
        farDistantBuildingsPatternWidth = calculatePatternWidth(farDistantBuildings, 2.5, 4.0);

        currentXOffset = 0;
        for (int i = 0; i < 6; i++) {
            double cWidth = 160 + randomGenerator.nextDouble() * 180; double cHeight = 12 + randomGenerator.nextDouble() * 18;
            double cY = gameH * 0.15 + randomGenerator.nextDouble() * (gameH * 0.20);
            Color cColor = new Color(180 + randomGenerator.nextInt(20), 180 + randomGenerator.nextInt(20), 190 + randomGenerator.nextInt(25), 15 + randomGenerator.nextInt(20));
            midClouds.add(new BackgroundElement(currentXOffset, cY, cWidth, cHeight, cColor, 0.20 + randomGenerator.nextDouble()*0.07));
            currentXOffset += cWidth + (180 + randomGenerator.nextDouble() * 220);
        }
        midCloudsPatternWidth = calculatePatternWidth(midClouds, 2.0, 3.5);

        currentXOffset = 0;
        for (int i = 0; i < 18; i++) {
            double bWidth = 50 + randomGenerator.nextDouble() * 140;
            double bHeight = gameH * (0.25 + randomGenerator.nextDouble() * 0.55);
            double bY = gameH - bHeight + (gameH * (0.10 + randomGenerator.nextDouble() * 0.15));
            Color bColor = new Color(18 + randomGenerator.nextInt(10), 22 + randomGenerator.nextInt(10), 30 + randomGenerator.nextInt(18), 255);
            distantBuildings.add(new BuildingBackgroundElement(currentXOffset, bY, bWidth, bHeight, bColor, 0.10 + randomGenerator.nextDouble() * 0.04, randomGenerator));
            currentXOffset += bWidth + (30 + randomGenerator.nextDouble() * 70);
        }
        distantBuildingsPatternWidth = calculatePatternWidth(distantBuildings, 3.0, 4.0);

        currentXOffset = 0;
         for (int i = 0; i < 4; i++) {
            double cWidth = 250 + randomGenerator.nextDouble() * 150; double cHeight = 25 + randomGenerator.nextDouble() * 20;
            double cY = gameH * 0.30 + randomGenerator.nextDouble() * (gameH * 0.20);
            Color cColor = new Color(190 + randomGenerator.nextInt(20), 190 + randomGenerator.nextInt(20), 200 + randomGenerator.nextInt(20), 8 + randomGenerator.nextInt(12));
            nearClouds.add(new BackgroundElement(currentXOffset, cY, cWidth, cHeight, cColor, 0.35 + randomGenerator.nextDouble()*0.1));
            currentXOffset += cWidth + (220 + randomGenerator.nextDouble() * 220);
        }
        nearCloudsPatternWidth = calculatePatternWidth(nearClouds, 2.0, 3.0);
    }

    private void generateAndAddNextChunk() {
        double H = height(); double SHIELD_SIZE = 25; double PLAT_H = 20; double GAP_S = 60; double GAP_M = 110; double GAP_L = 180;
        double shieldYOffset = SHIELD_SIZE + 5; final double PLAYER_COLLISION_WIDTH_CONST = 30.0;
        LevelData.ChunkData newChunkData = LevelData.generateNextChunkData(H, actualChunkLength, randomGenerator, SHIELD_SIZE, PLAT_H, PLAYER_COLLISION_WIDTH_CONST, GAP_S, GAP_M, GAP_L, shieldYOffset);
        LevelChunk newChunk = new LevelChunk(newChunkData.platforms, newChunkData.powerUps, actualChunkLength, lastGeneratedChunkEndX);
        activeChunks.add(newChunk);
        lastGeneratedChunkEndX += actualChunkLength;
    }

    private void initializeFirstChunks() {
        activeChunks.clear(); lastGeneratedChunkEndX = 0; chunksCompleted = 0; currentChunkArrayIndex = 0;
        for (int i = 0; i < 1 + MAX_LOADED_CHUNKS_AHEAD; i++) { generateAndAddNextChunk(); }
    }

    private void resetGameVariables() {
        cameraX = 0;
        keyLeftPressed = false; keyRightPressed = false; keyCrouchPressed = false;
        jumpKeyJustPressed = false;
        jumpKeyCurrentlyHeld = false;
        if (fireballs != null) fireballs.clear(); else fireballs = new ArrayList<>();
        fireballSpawnTimer = 0;
        if(activeParticles != null) activeParticles.clear(); else activeParticles = new ArrayList<>();
        gameOverActive = false;
        gameLogicActive = true;
        titleScreenActive = false;
        initializeFirstChunks();
        chunksCompleted = 0;
    }

    private void respawnPlayer() {
        if (player == null) {
             player = new Player(initialPlayerSpawnX, initialPlayerSpawnY);
        }
        player.x = initialPlayerSpawnX; player.y = initialPlayerSpawnY; player.vx = 0; player.vy = 0;
        player.jumpsAvailable = player.MAX_STANDARD_JUMPS; player.shieldLevel = 1; player.isCrouching = false;
        player.collisionHeight = player.baseCollisionHeight; player.currentVisualState = Player.VisualState.NORMAL; player.visualEffectTimer = 0;
        resetGameVariables();
        System.out.println("Player respawned. Game restarted.");
    }

    @Override
    public void init() {
        setWindowSize(900, 550); randomGenerator = new Random();
        actualChunkLength = width() * CHUNK_LENGTH_IN_SCREENS;
        Player tempPlayerForHeight = new Player(0,0);
        initialPlayerSpawnX = 50; initialPlayerSpawnY = height() - 100 - tempPlayerForHeight.baseCollisionHeight;

        activeChunks = new LinkedList<>();
        activeParticles = new ArrayList<>();
        fireballs = new ArrayList<>();
        distantBuildings = new ArrayList<>();
        midClouds = new ArrayList<>();
        nearClouds = new ArrayList<>();
        stars = new ArrayList<>();

        initializeBackgroundElements();
        // Chunks are initialized when title screen is dismissed or on respawn

        player = new Player(initialPlayerSpawnX, initialPlayerSpawnY);

        titleScreenActive = true;
        gameLogicActive = false;
        gameOverActive = false;
        jumpKeyJustPressed = false;
        jumpKeyCurrentlyHeld = false;
        fireballSpawnTimer = 0; nextFireballSpawnInterval = 1.0 + randomGenerator.nextDouble() * 1.5;
    }

    public void spawnShieldPopParticles(double centerX, double centerY, Color baseParticleColor) {
        if (activeParticles == null) activeParticles = new ArrayList<>();
        int numberOfParticles = 20 + randomGenerator.nextInt(15);
        for (int i = 0; i < numberOfParticles; i++) {
            activeParticles.add(new Particle(centerX, centerY, baseParticleColor, ParticleType.SHIELD_POP));
        }
    }

    public void spawnJumpLandParticles(double centerX, double bottomY) {
        if (activeParticles == null) activeParticles = new ArrayList<>();
        int numberOfParticles = 7 + randomGenerator.nextInt(5);
        for (int i = 0; i < numberOfParticles; i++) {
            activeParticles.add(new Particle(centerX, bottomY - 5, Color.LIGHT_GRAY, ParticleType.JUMP_LAND));
        }
    }

    public void spawnFireballHitParticles(double centerX, double centerY) {
        if (activeParticles == null) activeParticles = new ArrayList<>();
        int numberOfParticles = 15 + randomGenerator.nextInt(10);
        for (int i = 0; i < numberOfParticles; i++) {
            activeParticles.add(new Particle(centerX, centerY, Color.ORANGE, ParticleType.FIREBALL_HIT));
        }
    }

    @Override
    public void update(double dt) {
        if (activeParticles != null) {
            Iterator<Particle> particleIterator = activeParticles.iterator();
            while (particleIterator.hasNext()) { if (!particleIterator.next().update(dt)) { particleIterator.remove(); } }
        }
        if (fireballs != null) {
            for (int i = fireballs.size() - 1; i >= 0; i--) {
                Fireball fb = fireballs.get(i); fb.update(dt);
                if (fb.x + fb.radius < cameraX - width() * 1.5) { fireballs.remove(i); }
            }
        }

        if (titleScreenActive) { return; }

        if (gameOverActive) {
            if (player != null) {
                 player.vx = 0;
                 player.vy += player.GRAVITY * dt;
                 player.y += player.vy * dt;
            }
            return;
        }

        if (!gameLogicActive) { return; }

        if (keyLeftPressed) player.vx = -player.MOVE_SPEED;
        else if (keyRightPressed) player.vx = player.MOVE_SPEED;
        else player.vx = 0;

        if (jumpKeyJustPressed) {
            if (player != null) player.jump(this);
            jumpKeyJustPressed = false;
        }

        LevelChunk currentPhysicalChunkForUpdate = null;
        if (currentChunkArrayIndex >= 0 && currentChunkArrayIndex < activeChunks.size()) {
            currentPhysicalChunkForUpdate = activeChunks.get(currentChunkArrayIndex);
        } else if (!activeChunks.isEmpty()) {
            currentPhysicalChunkForUpdate = activeChunks.getLast();
        }

        if (player != null && currentPhysicalChunkForUpdate != null) {
            if (keyCrouchPressed && !player.isCrouching) {
                player.setCrouching(true, currentPhysicalChunkForUpdate, currentPhysicalChunkForUpdate.startWorldX);
            } else if (!keyCrouchPressed && player.isCrouching) {
                player.setCrouching(false, currentPhysicalChunkForUpdate, currentPhysicalChunkForUpdate.startWorldX);
            }
        }

        double playerCenterX = player.x + player.collisionWidth / 2;
        int newChunkArrayIndex = -1;
        if (!activeChunks.isEmpty()) {
            for(int i=0; i < activeChunks.size(); i++) {
                LevelChunk chunk = activeChunks.get(i);
                if (playerCenterX >= chunk.startWorldX && playerCenterX < chunk.startWorldX + chunk.chunkWidth) {
                    newChunkArrayIndex = i; break;
                }
            }
        }
        if (newChunkArrayIndex != -1) { // No need to check if > currentChunkArrayIndex for this assignment
            currentChunkArrayIndex = newChunkArrayIndex;
        } else if (!activeChunks.isEmpty() && playerCenterX >= activeChunks.getLast().startWorldX + activeChunks.getLast().chunkWidth) {
             currentChunkArrayIndex = activeChunks.size() -1;
        }

        // Update chunksCompleted based on player passing conceptual chunk boundaries
        double playerProgressBoundary = chunksCompleted * actualChunkLength;
        if (player.x >= playerProgressBoundary + actualChunkLength) { // Player has crossed into the next conceptual chunk
            chunksCompleted++;
            System.out.println("Chunks Completed: " + chunksCompleted);
        }


        LevelChunk currentPhysicalChunk = null;
        if (currentChunkArrayIndex >= 0 && currentChunkArrayIndex < activeChunks.size()) {
            currentPhysicalChunk = activeChunks.get(currentChunkArrayIndex);
        } else if (!activeChunks.isEmpty()) {
             currentPhysicalChunk = activeChunks.getLast();
        }

        double generationLookaheadPoint = cameraX + width() + (width() * (MAX_LOADED_CHUNKS_AHEAD -1) );
        if (generationLookaheadPoint > lastGeneratedChunkEndX && activeChunks.size() < MAX_ACTIVE_CHUNKS_BUFFER ) {
            generateAndAddNextChunk();
        }

        while (activeChunks.size() > TARGET_ACTIVE_CHUNKS && currentChunkArrayIndex > MAX_LOADED_CHUNKS_BEHIND) {
            activeChunks.removeFirst();
            currentChunkArrayIndex--;
            System.out.println("Despawned chunk. Active: " + activeChunks.size() + ". New currentArrayIndex: " + currentChunkArrayIndex);
        }

        if (player != null) {
            if (currentPhysicalChunk != null) { player.update(dt, currentPhysicalChunk, currentPhysicalChunk.startWorldX, this); }
            else { player.update(dt, null, 0, this); }
        }

        if (player != null && player.y > height() + player.collisionHeight * 3) {
            if (!gameOverActive) { gameOverActive = true; this.playDeathSound(); if(player != null) player.vx = 0; }
        }

        if (currentPhysicalChunk != null && player != null && !gameOverActive) {
            for (Platform p : currentPhysicalChunk.platforms) {
                if (p.type == PlatformType.HAZARD) {
                    double platformWorldX = currentPhysicalChunk.startWorldX + p.x;
                    double platformWorldY = p.y;
                    boolean horizontalOverlap = player.x < platformWorldX + p.width && player.x + player.collisionWidth > platformWorldX;
                    boolean verticalOverlap = player.y < platformWorldY + p.height && player.y + player.collisionHeight >= platformWorldY;
                    if (horizontalOverlap && verticalOverlap) {
                        if (player.takeHit(this)) {
                            if (!gameOverActive) { gameOverActive = true; this.playDeathSound(); player.vx = 0; }
                        }
                    }
                }
            }
            if (currentPhysicalChunk.powerUps != null) {
                for (PowerUp pu : currentPhysicalChunk.powerUps) {
                    if (!pu.isCollected) {
                        double puWorldX = currentPhysicalChunk.startWorldX + pu.x;
                        boolean horizontalPOverlap = player.x < puWorldX + pu.width &&
                                                     player.x + player.collisionWidth > puWorldX;
                        boolean verticalPOverlap = player.y < pu.y + pu.height &&
                                                   player.y + player.collisionHeight > pu.y;
                        if (horizontalPOverlap && verticalPOverlap) {
                            pu.collect();
                            if (pu.type == PowerUpType.SHIELD) {
                                player.addShieldLayer();
                                this.playShieldCollectSound();
                            }
                        }
                    }
                }
            }
        }

        if (!gameOverActive && gameLogicActive) {
            fireballSpawnTimer += dt;
            if (fireballSpawnTimer >= nextFireballSpawnInterval) {
                fireballSpawnTimer = 0; double spawnY = (height() * 0.1) + randomGenerator.nextDouble() * (height() * 0.8);
                double speed = 180 + randomGenerator.nextDouble() * 220; double radius = 10 + randomGenerator.nextDouble() * 8;
                fireballs.add(new Fireball(cameraX + width() + radius + 30, spawnY, radius, speed));
            }
        }
        if (player != null && fireballs != null) {
            for (int i = fireballs.size() - 1; i >= 0; i--) {
                Fireball fb = fireballs.get(i);
                if (!gameOverActive && fb.collidesWith(player)) {
                     spawnFireballHitParticles(player.x + player.collisionWidth / 2, player.y + player.collisionHeight / 2);
                    if (player.takeHit(this)){
                        if (!gameOverActive) { gameOverActive = true; this.playDeathSound(); player.vx = 0; }
                    } else { fireballs.remove(i); }
                }
            }
        }

        if (player != null) {
            double targetCameraX = player.x - width() / 3.2;
            cameraX += (targetCameraX - cameraX) * 0.09; if (cameraX < 0) cameraX = 0;
        }
    }

    @Override
    public void keyPressed(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (titleScreenActive) {
            titleScreenActive = false;
            gameLogicActive = true;
            if (activeChunks.isEmpty()) { // Initialize chunks if this is the very first start
                initializeFirstChunks();
            }
            return;
        }
        if (gameOverActive) {
            respawnPlayer();
            return;
        }
        if (keyCode == KeyEvent.VK_A) keyLeftPressed = true;
        else if (keyCode == KeyEvent.VK_D) keyRightPressed = true;
        else if (keyCode == KeyEvent.VK_S) keyCrouchPressed = true;
        else if (keyCode == KeyEvent.VK_SPACE) {
            if (!jumpKeyCurrentlyHeld) {
                 jumpKeyJustPressed = true;
            }
            jumpKeyCurrentlyHeld = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.VK_A) keyLeftPressed = false;
        else if (keyCode == KeyEvent.VK_D) keyRightPressed = false;
        else if (keyCode == KeyEvent.VK_S) keyCrouchPressed = false;
        else if (keyCode == KeyEvent.VK_SPACE) {
            jumpKeyCurrentlyHeld = false;
        }
    }

    @Override
    public void paintComponent() {
        Color topSky = new Color(5, 8, 25);
        Color bottomSky = new Color(60, 40, 80);
        if (mGraphics instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) mGraphics;
            GradientPaint skyGradient = new GradientPaint(0, 0, topSky, 0, height(), bottomSky);
            g2d.setPaint(skyGradient);
            g2d.fillRect(0, 0, width(), height());
        } else {
            changeBackgroundColor(new Color(10, 15, 35));
            clearBackground(width(), height());
        }

        if (stars != null) { for (BackgroundElement star : stars) { star.draw(this, cameraX, width(), starsPatternWidth); } }
        if (farDistantBuildings != null) { for (BackgroundElement bgEl : farDistantBuildings) { bgEl.draw(this, cameraX, width(), farDistantBuildingsPatternWidth); } }
        if (midClouds != null) { for (BackgroundElement bgEl : midClouds) { bgEl.draw(this, cameraX, width(), midCloudsPatternWidth); } }
        if (distantBuildings != null) { for (BackgroundElement bgEl : distantBuildings) { bgEl.draw(this, cameraX, width(), distantBuildingsPatternWidth); } }
        if (nearClouds != null) { for (BackgroundElement bgEl : nearClouds) { bgEl.draw(this, cameraX, width(), nearCloudsPatternWidth); } }

        if (gameLogicActive || gameOverActive || titleScreenActive) {
            if (activeChunks != null && !activeChunks.isEmpty()) {
                for (LevelChunk chunk : activeChunks) {
                    if (chunk != null &&
                        chunk.startWorldX < cameraX + width() + chunk.chunkWidth*0.5 &&
                        chunk.startWorldX + chunk.chunkWidth > cameraX - chunk.chunkWidth*0.5) {
                        chunk.draw(this, cameraX);
                    }
                }
            }
            if (activeParticles != null) { for (Particle particle : activeParticles) { particle.draw(this, cameraX); } }
            if (fireballs != null) { for (Fireball fb : fireballs) { fb.draw(this, cameraX); } }
            if (player != null) { player.draw(this, cameraX); }

            if (player != null && gameLogicActive && !gameOverActive && !titleScreenActive) {
                changeColor(Color.WHITE);
                drawText(10, 25, "Chunks: " + chunksCompleted, "Arial", 18);
                drawText(10, 50, "Jumps: " + player.jumpsAvailable + "/" + player.MAX_STANDARD_JUMPS, "Arial", 18);
                if(player.shieldLevel > 0){ drawText(10, 75, "Shield: " + player.shieldLevel + "/" + Player.MAX_SHIELD_LEVEL, "Arial", 18); }
                if(player.isCrouching){ drawText(width() - 100, 25, "CROUCHING", "Arial", 14); }
            }
        }

        if (gameOverActive) {
            changeColor(new Color(255, 255, 220));
            Font gameOverFont = new Font("Arial", Font.BOLD, 60);
            String gameOverText = "Chunks Passed: " + chunksCompleted;
            double textWidth = getTextWidthEstimate(gameOverText, gameOverFont);
            drawText((width() - textWidth) / 2, height() * 0.4, gameOverText, gameOverFont.getName(), gameOverFont.getSize());
            changeColor(Color.WHITE);
            Font promptFont = new Font("Arial", Font.PLAIN, 30);
            String promptText = "Press any key to restart";
            double promptWidth = getTextWidthEstimate(promptText, promptFont);
            drawText((width() - promptWidth) / 2, height() * 0.6, promptText, promptFont.getName(), promptFont.getSize());
        } else if (titleScreenActive) {
            Font titleFont = new Font("Arial", Font.BOLD, 180); String titleText = "JAIMP";
            double titleWidth = getTextWidthEstimate(titleText, titleFont);
            double titleX = (width() - titleWidth) / 2.0; double titleY = height() * 0.5;
            changeColor(new Color(255, 223, 0, 230));
            drawText(titleX, titleY + titleFont.getSize() / 3, titleText, titleFont.getName(), titleFont.getSize());
        }
    }

    private double getTextWidthEstimate(String text, Font font) {
        if (this.mGraphics != null) {
            FontMetrics metrics = this.mGraphics.getFontMetrics(font);
            if (metrics != null) {
                return metrics.stringWidth(text);
            }
        }
        if (font.getSize() > 100) return text.length() * (font.getSize() * 0.50);
        if (font.getSize() > 50) return text.length() * (font.getSize() * 0.45);
        return text.length() * (font.getSize() * 0.55);
    }
    public static void main(String[] args) { createGame(new PlatformerGame()); }
}
