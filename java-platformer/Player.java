import java.awt.Color;
import java.util.Random; 

class Player {
    double x, y;
    double collisionWidth = 30;
    double baseCollisionHeight = 45;
    double crouchCollisionHeight = 25;
    double collisionHeight;

    double visualWidth, visualHeight;
    double baseVisualWidth, baseVisualHeight;

    double vx = 0, vy = 0;
    boolean onGround = false;
    final double MOVE_SPEED = 240;
    final double JUMP_STRENGTH = -620;
    final double DOUBLE_JUMP_STRENGTH = -520;
    final double SHIELD_JUMP_STRENGTH = -480;
    final double BOUNCE_STRENGTH = -850;
    final double HAZARD_BOUNCE_MULTIPLIER = 1.5; 
    final double GRAVITY = 2200;
    final double MAX_FALL_SPEED = 950;

    final int MAX_STANDARD_JUMPS = 2;
    int jumpsAvailable = 0;

    int shieldLevel = 0;
    public static final int MAX_SHIELD_LEVEL = 3; 

    boolean isCrouching = false;

    Color color = new Color(70, 70, 220); 
    Color shieldOutlineColor = new Color(220, 220, 255, 150);
    Color shieldLevel2Color = new Color(180, 180, 255, 180);
    Color shieldLevel3Color = new Color(150, 150, 255, 210);
    Color eyeWhiteColor = Color.WHITE;
    Color eyePupilColor = Color.BLACK;


    enum VisualState { NORMAL, SQUASHING, STRETCHING, CROUCH_SQUASH }
    VisualState currentVisualState = VisualState.NORMAL;
    double visualEffectTimer = 0;
    final double SQUASH_DURATION = 0.12;
    final double STRETCH_DURATION = 0.15;
    final double CROUCH_SQUASH_FACTOR = 0.60;
    private static final double SIDE_FACING_SQUISH_FACTOR = 0.80; 

    private enum FacingDirection { LEFT, RIGHT, FRONT }
    private FacingDirection facing = FacingDirection.FRONT;
    private Random eyeRandom = new Random();

    private static final double EYE_BASE_WIDTH = 8;
    private static final double EYE_BASE_HEIGHT = 10;
    private static final double EYE_OFFSET_X_FRONT = 7;
    private static final double EYE_VERTICAL_POSITION_FACTOR = 0.33;
    private static final double EYE_SINGLE_OFFSET_X = 0;

    private double eyeLeftCurrentScaleY = 1.0;
    private double eyeRightCurrentScaleY = 1.0;
    private boolean eyeLeftBlinking = false;
    private boolean eyeRightBlinking = false;
    private double eyeLeftBlinkTimer = 0;
    private double eyeRightBlinkTimer = 0;
    private final double BLINK_DURATION = 0.15;
    private final double MIN_TIME_BETWEEN_BLINKS = 1.5;
    private final double MAX_TIME_BETWEEN_BLINKS = 5.0;
    private double nextBlinkTimeLeft;
    private double nextBlinkTimeRight;


    public Player(double startX, double startY) {
        this.x = startX; this.y = startY;
        this.collisionHeight = baseCollisionHeight;
        this.baseVisualWidth = collisionWidth;
        this.baseVisualHeight = baseCollisionHeight;
        this.visualWidth = baseVisualWidth; 
        this.visualHeight = baseVisualHeight; 
        this.shieldLevel = 1;
        this.jumpsAvailable = MAX_STANDARD_JUMPS;

        this.nextBlinkTimeLeft = MIN_TIME_BETWEEN_BLINKS + eyeRandom.nextDouble() * (MAX_TIME_BETWEEN_BLINKS - MIN_TIME_BETWEEN_BLINKS);
        this.nextBlinkTimeRight = MIN_TIME_BETWEEN_BLINKS + eyeRandom.nextDouble() * (MAX_TIME_BETWEEN_BLINKS - MIN_TIME_BETWEEN_BLINKS);
    }

    private void applyVisualEffect(VisualState newState, double duration) {
        currentVisualState = newState;
        visualEffectTimer = duration;
        if (newState == VisualState.SQUASHING) {
            visualHeight = baseVisualHeight * 0.70;
            visualWidth = baseVisualWidth * 1.30;
        } else if (newState == VisualState.STRETCHING) {
            visualHeight = baseVisualHeight * 1.25;
            visualWidth = baseVisualWidth * 0.75;
        } else if (newState == VisualState.CROUCH_SQUASH) {
            visualHeight = baseVisualHeight * CROUCH_SQUASH_FACTOR;
            visualWidth = baseVisualWidth * (1.0 + (1.0 - CROUCH_SQUASH_FACTOR) * 0.5);
            visualEffectTimer = 0; 
        }
    }

    public void setCrouching(boolean crouch, LevelChunk currentChunk, double currentChunkWorldStartX) {
        if (isCrouching == crouch) return;
        if (crouch) {
            if (onGround) y += (baseCollisionHeight - crouchCollisionHeight);
            collisionHeight = crouchCollisionHeight;
            isCrouching = true;
            applyVisualEffect(VisualState.CROUCH_SQUASH, 0);
        } else {
            double potentialNewY = y - (baseCollisionHeight - crouchCollisionHeight);
            boolean blocked = false;
            if (currentChunk != null) {
                for (Platform p : currentChunk.platforms) {
                    if (p.type == PlatformType.SOLID || p.type == PlatformType.BOUNCE) { 
                        double platformWorldX = currentChunkWorldStartX + p.x;
                        double platformWorldY = p.y;
                        if (x < platformWorldX + p.width && x + collisionWidth > platformWorldX &&
                            potentialNewY < platformWorldY + p.height && potentialNewY + baseCollisionHeight > platformWorldY) {
                            blocked = true;
                            break;
                        }
                    }
                }
            }
            if (!blocked) {
                y = potentialNewY;
                collisionHeight = baseCollisionHeight;
                isCrouching = false;
                currentVisualState = VisualState.NORMAL; 
            }
        }
    }

    private void updateEyeBlink(double dt, EyeIdentifier eye) {
        double blinkTimer;
        boolean isBlinking;
        double nextBlinkTime;
        double currentScaleY;

        if (eye == EyeIdentifier.LEFT) {
            blinkTimer = eyeLeftBlinkTimer; isBlinking = eyeLeftBlinking; nextBlinkTime = nextBlinkTimeLeft; currentScaleY = eyeLeftCurrentScaleY;
        } else {
            blinkTimer = eyeRightBlinkTimer; isBlinking = eyeRightBlinking; nextBlinkTime = nextBlinkTimeRight; currentScaleY = eyeRightCurrentScaleY;
        }
        if (isBlinking) {
            blinkTimer -= dt;
            if (blinkTimer <= 0) {
                isBlinking = false; currentScaleY = 1.0;
                nextBlinkTime = MIN_TIME_BETWEEN_BLINKS + eyeRandom.nextDouble() * (MAX_TIME_BETWEEN_BLINKS - MIN_TIME_BETWEEN_BLINKS);
            } else {
                double blinkProgress = 1.0 - (blinkTimer / BLINK_DURATION);
                currentScaleY = (blinkProgress < 0.5) ? (1.0 - (blinkProgress / 0.5) * 0.9) : (0.1 + ((blinkProgress - 0.5) / 0.5) * 0.9);
                currentScaleY = Math.max(0.1, currentScaleY);
            }
        } else {
            nextBlinkTime -= dt;
            if (nextBlinkTime <= 0) { isBlinking = true; blinkTimer = BLINK_DURATION; }
        }
        if (eye == EyeIdentifier.LEFT) {
            eyeLeftBlinkTimer = blinkTimer; eyeLeftBlinking = isBlinking; nextBlinkTimeLeft = nextBlinkTime; eyeLeftCurrentScaleY = currentScaleY;
        } else {
            eyeRightBlinkTimer = blinkTimer; eyeRightBlinking = isBlinking; nextBlinkTimeRight = nextBlinkTime; eyeRightCurrentScaleY = currentScaleY;
        }
    }
    private enum EyeIdentifier { LEFT, RIGHT }


    public void update(double dt, LevelChunk currentChunk, double currentChunkWorldStartX, GameEngine ge) {
        if (vx > 0.1) {
            facing = FacingDirection.RIGHT;
        } else if (vx < -0.1) {
            facing = FacingDirection.LEFT;
        } else {
            facing = FacingDirection.FRONT;
        }

        if (currentVisualState != VisualState.CROUCH_SQUASH && visualEffectTimer > 0) {
            visualEffectTimer -= dt;
            if (visualEffectTimer <= 0) { 
                currentVisualState = VisualState.NORMAL; 
                visualEffectTimer = 0;
            }
        }

        // Set visualWidth and visualHeight based on the current state
        switch (currentVisualState) {
            case NORMAL:
                if (facing == FacingDirection.LEFT || facing == FacingDirection.RIGHT) {
                    this.visualWidth = this.baseVisualWidth * SIDE_FACING_SQUISH_FACTOR;
                } else { 
                    this.visualWidth = this.baseVisualWidth;
                }
                this.visualHeight = this.baseVisualHeight; 
                break;
            case SQUASHING: // Values are set by applyVisualEffect
                break;
            case STRETCHING: // Values are set by applyVisualEffect
                break;
            case CROUCH_SQUASH: // Values are set by applyVisualEffect (called from setCrouching)
                this.visualHeight = baseVisualHeight * CROUCH_SQUASH_FACTOR;
                this.visualWidth = baseVisualWidth * (1.0 + (1.0 - CROUCH_SQUASH_FACTOR) * 0.5);
                break;
        }

        updateEyeBlink(dt, EyeIdentifier.LEFT);
        updateEyeBlink(dt, EyeIdentifier.RIGHT);

        vy += GRAVITY * dt;
        if (vy > MAX_FALL_SPEED) vy = MAX_FALL_SPEED;

        double nextX = x + vx * dt;
        double nextY = y + vy * dt;

        boolean wasOnGround = onGround;
        onGround = false;
        Platform platformLandedThisFrame = null;

        if (currentChunk != null) { 
            for (Platform p : currentChunk.platforms) {
                double platformWorldX = currentChunkWorldStartX + p.x;
                double platformWorldY = p.y;
                if (x + collisionWidth > platformWorldX && x < platformWorldX + p.width &&
                    nextY + collisionHeight > platformWorldY && nextY < platformWorldY + p.height) {
                    if (p.type == PlatformType.SOLID) { 
                        if (vy >= 0 && y + collisionHeight <= platformWorldY + 1) {
                            nextY = platformWorldY - collisionHeight; vy = 0; onGround = true; platformLandedThisFrame = p;
                        } else if (vy < 0 && y >= platformWorldY + p.height - 1) {
                            nextY = platformWorldY + p.height; vy = 0;
                        }
                    } else if (p.type == PlatformType.BOUNCE) {
                        if (vy >= 0 && y + collisionHeight <= platformWorldY + 5) {
                            if(isCrouching) { setCrouching(false, currentChunk, currentChunkWorldStartX); if (!isCrouching) nextY = platformWorldY - this.collisionHeight; }
                            if (!isCrouching) {
                                nextY = platformWorldY - this.collisionHeight; vy = BOUNCE_STRENGTH; onGround = false; jumpsAvailable = MAX_STANDARD_JUMPS;
                                applyVisualEffect(VisualState.STRETCHING, STRETCH_DURATION * 1.2); playBoingSound(ge);
                                if (ge instanceof PlatformerGame) { ((PlatformerGame)ge).spawnJumpLandParticles(this.x + this.collisionWidth/2, this.y + this.collisionHeight); }
                            } else { nextY = platformWorldY - this.collisionHeight; vy = 0; onGround = true; platformLandedThisFrame = p; }
                        }
                    } else if (p.type == PlatformType.HAZARD) {
                        if (vy >= 0 && y + collisionHeight <= platformWorldY + 5) { 
                            if(isCrouching) { setCrouching(false, currentChunk, currentChunkWorldStartX); if (!isCrouching) nextY = platformWorldY - this.collisionHeight; }
                            if(!isCrouching) {
                                nextY = platformWorldY - this.collisionHeight; vy = BOUNCE_STRENGTH * HAZARD_BOUNCE_MULTIPLIER; 
                                onGround = false; jumpsAvailable = MAX_STANDARD_JUMPS; 
                                applyVisualEffect(VisualState.STRETCHING, STRETCH_DURATION * 1.3); 
                                if (ge instanceof PlatformerGame) { ((PlatformerGame)ge).spawnJumpLandParticles(this.x + this.collisionWidth/2, this.y + this.collisionHeight); }
                            } else { nextY = platformWorldY - this.collisionHeight; vy = 0; onGround = true; platformLandedThisFrame = p; }
                        }
                    }
                }
                if (nextX + collisionWidth > platformWorldX && nextX < platformWorldX + p.width &&
                    this.y + collisionHeight > platformWorldY && this.y < platformWorldY + p.height) {
                    if (p.type == PlatformType.SOLID || p.type == PlatformType.BOUNCE) { 
                        if (vx > 0 && x + collisionWidth <= platformWorldX + 1) { nextX = platformWorldX - collisionWidth; vx = 0; } 
                        else if (vx < 0 && x >= platformWorldX + p.width - 1) { nextX = platformWorldX + p.width; vx = 0; }
                    }
                }
            }
        }

        x = nextX; y = nextY;

        if (onGround && !wasOnGround) {
            jumpsAvailable = MAX_STANDARD_JUMPS;
            if(!isCrouching && currentVisualState != VisualState.SQUASHING) { applyVisualEffect(VisualState.SQUASHING, SQUASH_DURATION); }
            if (ge instanceof PlatformerGame) { ((PlatformerGame)ge).spawnJumpLandParticles(this.x + this.collisionWidth/2, this.y + this.collisionHeight); }
            if (platformLandedThisFrame != null && (platformLandedThisFrame.type == PlatformType.SOLID)) { playPitchedPlatformSound(platformLandedThisFrame, ge); }
        }
        if (x < 0) { x = 0; if (vx < 0) vx = 0; }
    }

    private void playPitchedPlatformSound(Platform landedPlatform, GameEngine ge) {
        if (landedPlatform == null || ge == null) return;
        double gameHeight = ge.height();
        if (gameHeight <= 0) gameHeight = 550;
        double normalizedY = landedPlatform.y / gameHeight;
        double minFreq = 110.0; double maxFreq = 880.0;
        double frequency = maxFreq - (normalizedY * (maxFreq - minFreq));
        frequency = Math.max(minFreq, Math.min(maxFreq, frequency));
        double platformWidth = landedPlatform.width;
        double minPlatformWidthForSound = 20.0; double maxPlatformWidthForSound = 500.0;
        double minDurationMs = 900.0; double maxDurationMs = 2500.0;
        double normalizedWidth = (platformWidth - minPlatformWidthForSound) / (maxPlatformWidthForSound - minPlatformWidthForSound);
        normalizedWidth = Math.max(0.0, Math.min(1.0, normalizedWidth));
        int durationMs = (int) (minDurationMs + normalizedWidth * (maxDurationMs - minDurationMs));
        durationMs = Math.max((int)minDurationMs, Math.min(durationMs, (int)maxDurationMs));
        ge.playTone(frequency, durationMs);
    }
    
    private void playMidAirJumpSound(GameEngine ge) { if (ge != null) { ge.playMidAirJumpSound(); } }
    private void playBoingSound(GameEngine ge) { if (ge != null) { ge.playBoingSound(); } }

    public void draw(GameEngine ge, double cameraX) {
        double currentVisualDrawHeight = this.visualHeight;
        double currentVisualDrawWidth = this.visualWidth;

        if (currentVisualDrawWidth <= 0) currentVisualDrawWidth = baseVisualWidth;
        if (currentVisualDrawHeight <= 0) currentVisualDrawHeight = baseVisualHeight;

        double visualBaseX = this.x + (this.collisionWidth - currentVisualDrawWidth) / 2.0;
        double visualBaseY = this.y + (this.collisionHeight - currentVisualDrawHeight);
        
        if (shieldLevel > 0) {
            Color currentShieldColorToUse = shieldOutlineColor; 
            if (shieldLevel == 2) currentShieldColorToUse = shieldLevel2Color;
            else if (shieldLevel >= MAX_SHIELD_LEVEL) currentShieldColorToUse = shieldLevel3Color; 
            
            ge.changeColor(currentShieldColorToUse);
            double baseOutlineThickness = 2.0; double perLevelThickness = 2.5;
            double effectiveShieldLevelForVisuals = Math.min(shieldLevel, MAX_SHIELD_LEVEL);
            double outlineOffset = baseOutlineThickness + (effectiveShieldLevelForVisuals -1) * perLevelThickness;

            ge.drawSolidRectangle( 
                visualBaseX - cameraX - outlineOffset,
                visualBaseY - outlineOffset,
                currentVisualDrawWidth + (outlineOffset * 2),
                currentVisualDrawHeight + (outlineOffset * 2)
            );
        }

        ge.changeColor(this.color); 
        ge.drawSolidRectangle(visualBaseX - cameraX, visualBaseY, currentVisualDrawWidth, currentVisualDrawHeight);

        double playerVisualCenterX = visualBaseX + currentVisualDrawWidth / 2.0;
        double playerVisualTopY = visualBaseY;
        double eyeFinalY = playerVisualTopY + (currentVisualDrawHeight * EYE_VERTICAL_POSITION_FACTOR);

        if (facing == FacingDirection.FRONT) {
            drawOneEyeRect(ge, cameraX, playerVisualCenterX - EYE_OFFSET_X_FRONT, eyeFinalY, eyeLeftCurrentScaleY);
            drawOneEyeRect(ge, cameraX, playerVisualCenterX + EYE_OFFSET_X_FRONT, eyeFinalY, eyeRightCurrentScaleY);
        } else if (facing == FacingDirection.LEFT) {
            double sideEyeX = playerVisualCenterX - currentVisualDrawWidth * 0.25 + EYE_SINGLE_OFFSET_X;
            drawOneEyeRect(ge, cameraX, sideEyeX, eyeFinalY, eyeLeftCurrentScaleY);
        } else { 
            double sideEyeX = playerVisualCenterX + currentVisualDrawWidth * 0.25 + EYE_SINGLE_OFFSET_X;
            drawOneEyeRect(ge, cameraX, sideEyeX, eyeFinalY, eyeRightCurrentScaleY);
        }
    }

    private void drawOneEyeRect(GameEngine ge, double cameraX, double centerX, double centerY, double scaleY) {
        double actualEyeHeight = EYE_BASE_HEIGHT * scaleY;
        double actualEyeWidth = EYE_BASE_WIDTH;
        double pupilHeight = actualEyeHeight * 0.5;
        double pupilWidth = actualEyeWidth * 0.5;

        ge.changeColor(eyeWhiteColor);
        ge.drawSolidRectangle(centerX - actualEyeWidth / 2 - cameraX, centerY - actualEyeHeight / 2, actualEyeWidth, actualEyeHeight);
        ge.changeColor(eyePupilColor);
        ge.drawSolidRectangle(centerX - pupilWidth / 2 - cameraX, centerY - pupilHeight / 2, pupilWidth, pupilHeight);
    }

    public void jump(GameEngine ge) { 
        if (isCrouching) return;
        if (jumpsAvailable > 0) {
            boolean isFirstJumpFromGround = (jumpsAvailable == MAX_STANDARD_JUMPS && onGround);
            vy = isFirstJumpFromGround ? JUMP_STRENGTH : DOUBLE_JUMP_STRENGTH;
            onGround = false;
            jumpsAvailable--;
            applyVisualEffect(VisualState.STRETCHING, STRETCH_DURATION * (isFirstJumpFromGround ? 1.0 : 1.1));
            if (ge instanceof PlatformerGame) {
                double particleX = this.x + this.collisionWidth / 2;
                double particleY = this.y + this.collisionHeight; 
                ((PlatformerGame)ge).spawnJumpLandParticles(particleX, particleY);
            }
            if (!isFirstJumpFromGround) { 
                playMidAirJumpSound(ge);
            }
        } else if (shieldLevel > 0 && !onGround) {
            int shieldLevelPopped = shieldLevel; shieldLevel--;
            vy = SHIELD_JUMP_STRENGTH; onGround = false;
            applyVisualEffect(VisualState.STRETCHING, STRETCH_DURATION * 1.15);
            playMidAirJumpSound(ge); 
            if (ge instanceof PlatformerGame) { 
                Color particleColor = shieldOutlineColor; 
                if (shieldLevelPopped == 2) particleColor = shieldLevel2Color;
                else if (shieldLevelPopped >= MAX_SHIELD_LEVEL) particleColor = shieldLevel3Color;
                ((PlatformerGame)ge).spawnShieldPopParticles(x + collisionWidth/2, y + collisionHeight/2, particleColor);
            }
        }
    }

    public boolean takeHit(GameEngine ge) { 
        if (shieldLevel > 0) {
            int shieldLevelPopped = shieldLevel; shieldLevel--;
            if (ge != null) {
                ge.playHitSound();
                 if (ge instanceof PlatformerGame) {
                    Color particleColor = shieldOutlineColor; 
                    if (shieldLevelPopped == 2) particleColor = shieldLevel2Color;
                    else if (shieldLevelPopped >= MAX_SHIELD_LEVEL) particleColor = shieldLevel3Color; 
                    ((PlatformerGame)ge).spawnShieldPopParticles(x + collisionWidth/2, y + collisionHeight/2, particleColor);
                }
            }
            return false;
        }
        if (ge != null) {
            ge.playHitSound();
        }
        return true;
    }

    public void addShieldLayer() { 
        if (this.shieldLevel < MAX_SHIELD_LEVEL) {
            this.shieldLevel++;
        }
    }
}
