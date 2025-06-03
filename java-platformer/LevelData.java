import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class LevelData {

    private static double PLAYER_WIDTH_UNIT;
    private enum PlatformWidthKey {
        UNIT_X1_5, UNIT_X2, UNIT_X3, UNIT_X4, UNIT_X5, UNIT_X6, UNIT_X8, UNIT_X10 
    }
    private static double getWidth(PlatformWidthKey key) {
        switch (key) {
            case UNIT_X1_5: return PLAYER_WIDTH_UNIT * 1.5; 
            case UNIT_X2: return PLAYER_WIDTH_UNIT * 2.0; 
            case UNIT_X3: return PLAYER_WIDTH_UNIT * 3.0;
            case UNIT_X4: return PLAYER_WIDTH_UNIT * 4.0;
            case UNIT_X5: return PLAYER_WIDTH_UNIT * 5.0;
            case UNIT_X6: return PLAYER_WIDTH_UNIT * 6.0;
            case UNIT_X8: return PLAYER_WIDTH_UNIT * 7.5; 
            case UNIT_X10: return PLAYER_WIDTH_UNIT * 9.0;
            default: return PLAYER_WIDTH_UNIT * 2.5; 
        }
    }

    private static double GAME_HEIGHT;
    private enum ElevationKey {
        GROUND, LOW_A, LOW_B, MID_C, MID_D, MID_E, HIGH_F, HIGH_G, SKY_A, SKY_B 
    }
    private static double getElevation(ElevationKey key) {
        switch (key) {
            case GROUND:     return GAME_HEIGHT - 40;
            case LOW_A:      return GAME_HEIGHT - 75;
            case LOW_B:      return GAME_HEIGHT - 110;
            case MID_C:      return GAME_HEIGHT - 145; 
            case MID_D:      return GAME_HEIGHT - 180;
            case MID_E:      return GAME_HEIGHT - 215;
            case HIGH_F:     return GAME_HEIGHT - 260;
            case HIGH_G:     return GAME_HEIGHT - 305;
            case SKY_A:      return GAME_HEIGHT - 350; 
            case SKY_B:      return GAME_HEIGHT - 400; 
            default:         return GAME_HEIGHT - 40;
        }
    }
    private static final ElevationKey[] ALL_ELEVATIONS = ElevationKey.values();
    private static final ElevationKey[] NORMAL_ELEVATIONS = {
        ElevationKey.GROUND, ElevationKey.LOW_A, ElevationKey.LOW_B, ElevationKey.MID_C, 
        ElevationKey.MID_D, ElevationKey.MID_E, ElevationKey.HIGH_F, ElevationKey.HIGH_G
    };


    private static class PlatformDef {
        double xOffset; 
        ElevationKey elevation;
        PlatformWidthKey width;
        int type;
        Integer powerUpType; 

        PlatformDef(double xOffset, ElevationKey elevation, PlatformWidthKey width, int type, Integer powerUpType) {
            this.xOffset = xOffset; this.elevation = elevation; this.width = width; this.type = type; this.powerUpType = powerUpType;
        }
        PlatformDef(double xOffset, ElevationKey elevation, PlatformWidthKey width, int type) {
            this(xOffset, elevation, width, type, null);
        }
    }
    
    private static ElevationKey getRandomNormalElevation(Random rand) {
        return NORMAL_ELEVATIONS[rand.nextInt(NORMAL_ELEVATIONS.length)];
    }

    private static ElevationKey getNextStepElevation(Random rand, ElevationKey current, int maxStepMagnitude, boolean allowSky) {
        if (maxStepMagnitude < 0) maxStepMagnitude = 0; // Ensure magnitude is non-negative

        int currentOrdinalInAll = current.ordinal();
        
        int change = 0;
        if (maxStepMagnitude > 0) { // Only calculate change if magnitude allows for it
             change = rand.nextInt(maxStepMagnitude * 2 + 1) - maxStepMagnitude;
        } else if (maxStepMagnitude == 0) {
            change = 0; // No change if magnitude is 0
        }
        // If maxStepMagnitude was negative initially, it's now 0, so change will be 0.

        int newOrdinalInAll = currentOrdinalInAll + change;

        ElevationKey[] targetRangeArray = allowSky ? ALL_ELEVATIONS : NORMAL_ELEVATIONS;
        
        // Determine min and max ordinals of the *target range itself*
        int minTargetOrdinalValue = targetRangeArray[0].ordinal();
        int maxTargetOrdinalValue = targetRangeArray[targetRangeArray.length - 1].ordinal();

        // Clamp newOrdinalInAll to be within the absolute min/max of ALL_ELEVATIONS first
        newOrdinalInAll = Math.max(ALL_ELEVATIONS[0].ordinal(), Math.min(ALL_ELEVATIONS[ALL_ELEVATIONS.length - 1].ordinal(), newOrdinalInAll));
        
        // Then, if not allowing sky, further clamp to the effective range of NORMAL_ELEVATIONS
        if (!allowSky) {
            newOrdinalInAll = Math.max(minTargetOrdinalValue, Math.min(maxTargetOrdinalValue, newOrdinalInAll));
        }
        
        return ALL_ELEVATIONS[newOrdinalInAll];
    }
    
    private static PlatformWidthKey getRandomWidth(Random rand, boolean preferSmaller) {
        PlatformWidthKey[] widths = PlatformWidthKey.values();
        if (preferSmaller) {
            return widths[rand.nextInt(Math.min(3, widths.length))]; 
        }
        return widths[Math.min(2,widths.length-1) + rand.nextInt(Math.max(1, widths.length - 3))]; 
    }

    // --- Mini-Feature Generators ---

    private static void addSimpleSteps(List<PlatformDef> defs, Random rand, ElevationKey startElevation, int count) {
        ElevationKey currentElevation = startElevation;
        for (int i = 0; i < count; i++) {
            double xOff = PLAYER_WIDTH_UNIT * (0.4 + rand.nextDouble() * 0.4); 
            PlatformWidthKey width = getRandomWidth(rand, true);
            currentElevation = getNextStepElevation(rand, currentElevation, 1, false); 
            defs.add(new PlatformDef(xOff, currentElevation, width, PlatformType.SOLID));
        }
    }

    private static void addHazardPit(List<PlatformDef> defs, Random rand, ElevationKey landingElevation) {
        PlatformWidthKey pitWidthKey = PlatformWidthKey.UNIT_X3; 
        double xOffPit = PLAYER_WIDTH_UNIT * (0.5 + rand.nextDouble() * 0.2); 
        defs.add(new PlatformDef(xOffPit, ElevationKey.GROUND, pitWidthKey, PlatformType.HAZARD));
        
        double landXOff = PLAYER_WIDTH_UNIT * (0.1 + rand.nextDouble()*0.15); 
        PlatformWidthKey landWidth = getRandomWidth(rand, false); 
        defs.add(new PlatformDef(landXOff, landingElevation, landWidth, PlatformType.SOLID));
    }

    private static void addBounceSequence(List<PlatformDef> defs, Random rand, ElevationKey startBounceElev, int bounceCount, boolean placePowerUp) {
        ElevationKey currentBounceElevation = startBounceElev;
        for (int i = 0; i < bounceCount; i++) {
            double xOff = PLAYER_WIDTH_UNIT * (0.2 + rand.nextDouble() * 0.2); 
            defs.add(new PlatformDef(xOff, currentBounceElevation, PlatformWidthKey.UNIT_X1_5, PlatformType.BOUNCE));
            if (i < bounceCount - 1) {
                 currentBounceElevation = getNextStepElevation(rand, currentBounceElevation, 2, false);
            }
        }
        double landXOff = PLAYER_WIDTH_UNIT * (0.5 + rand.nextDouble() * 0.4); 
        int targetLandElevOrdinal = Math.min(ALL_ELEVATIONS.length -1, currentBounceElevation.ordinal() + 4 + rand.nextInt(2)); 
        ElevationKey landElev = ALL_ELEVATIONS[targetLandElevOrdinal];
        PlatformWidthKey landWidth = getRandomWidth(rand, false);
        defs.add(new PlatformDef(landXOff, landElev, landWidth, PlatformType.SOLID, (placePowerUp ? PowerUpType.SHIELD : null) ));
    }
    
    private static void addFloatingHazardRun(List<PlatformDef> defs, Random rand, ElevationKey runElevation, boolean placePowerUp) {
        PlatformWidthKey runWidth = PlatformWidthKey.UNIT_X6; 
        double xOffsetForRun = PLAYER_WIDTH_UNIT * (0.5 + rand.nextDouble() * 0.4);
        defs.add(new PlatformDef(xOffsetForRun, runElevation, runWidth, PlatformType.SOLID, (placePowerUp ? PowerUpType.SHIELD : null)));
        
        int hazardCount = 1 + rand.nextInt(1); 
        ElevationKey hazardElevationKey = ElevationKey.values()[Math.min(ALL_ELEVATIONS.length-1, runElevation.ordinal() + 2)]; 
        for(int i=0; i<hazardCount; i++) {
            double hazardXOffset = PLAYER_WIDTH_UNIT * (1.2 + rand.nextDouble() * 0.8); 
            defs.add(new PlatformDef(hazardXOffset, hazardElevationKey, PlatformWidthKey.UNIT_X1_5, PlatformType.HAZARD));
        }
    }
    
    private static void addShieldJumpPathOption(List<PlatformDef> defs, Random rand, ElevationKey mainPathElev, boolean placePowerUp) {
        defs.add(new PlatformDef(PLAYER_WIDTH_UNIT * (0.4 + rand.nextDouble()*0.4), mainPathElev, getRandomWidth(rand, false), PlatformType.SOLID));
        ElevationKey highOptionElev = ElevationKey.values()[Math.min(ALL_ELEVATIONS.length -1, mainPathElev.ordinal() + 5 + rand.nextInt(2))]; 
        double xOffsetForHigh = PLAYER_WIDTH_UNIT * (0.2 + rand.nextDouble()*0.2); 
        defs.add(new PlatformDef(xOffsetForHigh, highOptionElev, getRandomWidth(rand, true), PlatformType.SOLID, (placePowerUp ? PowerUpType.SHIELD : null)));
    }

    private static void addVerticalWeave(List<PlatformDef> defs, Random rand, ElevationKey startElev, boolean placePowerUp) {
        ElevationKey currentElev = startElev;
        defs.add(new PlatformDef(PLAYER_WIDTH_UNIT * 0.5, currentElev, PlatformWidthKey.UNIT_X3, PlatformType.SOLID)); 
        
        ElevationKey highElev = getNextStepElevation(rand, currentElev, 2, true); 
        defs.add(new PlatformDef(PLAYER_WIDTH_UNIT * 0.8, highElev, PlatformWidthKey.UNIT_X2, PlatformType.SOLID));
        
        // Corrected: Pass positive maxStepMagnitude
        ElevationKey lowElev = getNextStepElevation(rand, currentElev, 2, false); 
        if (rand.nextDouble() < 0.4) { 
            defs.add(new PlatformDef(PLAYER_WIDTH_UNIT * 0.7, ElevationKey.GROUND, PlatformWidthKey.UNIT_X1_5, PlatformType.HAZARD));
        }
        defs.add(new PlatformDef(PLAYER_WIDTH_UNIT * 0.7, lowElev, PlatformWidthKey.UNIT_X3, PlatformType.SOLID, (placePowerUp ? PowerUpType.SHIELD : null)));
        
        currentElev = getNextStepElevation(rand, lowElev, 2, false); 
        defs.add(new PlatformDef(PLAYER_WIDTH_UNIT * 0.8, currentElev, PlatformWidthKey.UNIT_X3, PlatformType.SOLID));
    }


    public static ChunkData generateNextChunkData(double H, double W_CHUNK_LENGTH, Random randomGenerator,
                                                 double SHIELD_SIZE, double PLAT_H,
                                                 double PLAYER_COLLISION_WIDTH,
                                                 double GAP_S_UNUSED, double GAP_M_UNUSED, double GAP_L_UNUSED, 
                                                 double shieldYOffset) {
        
        GAME_HEIGHT = H; 
        PLAYER_WIDTH_UNIT = PLAYER_COLLISION_WIDTH; 

        List<PlatformDef> platformDefs = new ArrayList<>();
        ElevationKey lastElevation = ElevationKey.GROUND;
        int powerUpsPlacedThisChunk = 0;
        final int TARGET_POWERUPS_PER_CHUNK = 2 + randomGenerator.nextInt(2); 

        PlatformWidthKey startWidth = getRandomWidth(randomGenerator, false);
        platformDefs.add(new PlatformDef(PLAYER_WIDTH_UNIT * 0.2, ElevationKey.GROUND, startWidth, PlatformType.SOLID));
        lastElevation = ElevationKey.GROUND;

        double currentEstimatedX = PLAYER_WIDTH_UNIT * 0.2 + getWidth(startWidth);
        int featuresAdded = 0;

        while (currentEstimatedX < W_CHUNK_LENGTH * 0.90 && featuresAdded < 12) { 
            int featureType = randomGenerator.nextInt(6); 
            boolean tryPlacePU = powerUpsPlacedThisChunk < TARGET_POWERUPS_PER_CHUNK && randomGenerator.nextDouble() < 0.60; 

            int prevDefCount = platformDefs.size();

            switch (featureType) {
                case 0: 
                    int stepCount = 2 + randomGenerator.nextInt(2); 
                    addSimpleSteps(platformDefs, randomGenerator, lastElevation, stepCount);
                    break;
                case 1: 
                    ElevationKey landElev = getRandomNormalElevation(randomGenerator);
                    if (Math.abs(landElev.ordinal() - lastElevation.ordinal()) > 2) {
                        landElev = getNextStepElevation(randomGenerator, lastElevation, 1, false);
                    }
                    addHazardPit(platformDefs, randomGenerator, landElev);
                    break;
                case 2: 
                    int bounceCount = 1 + randomGenerator.nextInt(1); 
                    addBounceSequence(platformDefs, randomGenerator, lastElevation, bounceCount, tryPlacePU);
                    if (tryPlacePU && !platformDefs.isEmpty() && platformDefs.get(platformDefs.size()-1).powerUpType != null) powerUpsPlacedThisChunk++;
                    break;
                case 3: 
                    ElevationKey runElev = getRandomNormalElevation(randomGenerator);
                    addFloatingHazardRun(platformDefs, randomGenerator, runElev, tryPlacePU);
                    if (tryPlacePU && !platformDefs.isEmpty() && platformDefs.get(platformDefs.size()-1).type == PlatformType.SOLID && platformDefs.get(platformDefs.size()-1).powerUpType != null) powerUpsPlacedThisChunk++;
                    break;
                case 4: 
                    ElevationKey mainPathElev = getRandomNormalElevation(randomGenerator);
                    if (mainPathElev.ordinal() > ElevationKey.HIGH_F.ordinal()) {
                        mainPathElev = ElevationKey.HIGH_F;
                    }
                    addShieldJumpPathOption(platformDefs, randomGenerator, mainPathElev, tryPlacePU);
                     if (tryPlacePU && !platformDefs.isEmpty() && platformDefs.get(platformDefs.size()-1).powerUpType != null) powerUpsPlacedThisChunk++;
                    break;
                case 5: 
                    ElevationKey weaveStartElev = getNextStepElevation(randomGenerator, lastElevation, 1, false);
                    addVerticalWeave(platformDefs, randomGenerator, weaveStartElev, tryPlacePU);
                    if (tryPlacePU && !platformDefs.isEmpty() && platformDefs.get(platformDefs.size()-1).powerUpType != null) powerUpsPlacedThisChunk++;
                    break;
            }
            
            for (int k = prevDefCount; k < platformDefs.size(); k++) {
                PlatformDef newDef = platformDefs.get(k);
                currentEstimatedX += newDef.xOffset + getWidth(newDef.width);
                lastElevation = newDef.elevation;
            }
            featuresAdded++;
        }

        while(powerUpsPlacedThisChunk < TARGET_POWERUPS_PER_CHUNK && platformDefs.size() > 1) {
            int randomIndex = 1 + randomGenerator.nextInt(platformDefs.size() -1); 
            PlatformDef targetDef = platformDefs.get(randomIndex);
            if (targetDef.type == PlatformType.SOLID && targetDef.powerUpType == null) {
                 targetDef.powerUpType = PowerUpType.SHIELD;
                 powerUpsPlacedThisChunk++;
            }
            boolean canAddMore = false;
            for(PlatformDef def : platformDefs) if(def.type == PlatformType.SOLID && def.powerUpType == null) canAddMore = true;
            if(!canAddMore || powerUpsPlacedThisChunk >= TARGET_POWERUPS_PER_CHUNK) break;
        }
        
        if (currentEstimatedX < W_CHUNK_LENGTH - getWidth(PlatformWidthKey.UNIT_X6)) {
            double finalGap = PLAYER_WIDTH_UNIT * (1.0 + randomGenerator.nextDouble() * 0.5);
            platformDefs.add(new PlatformDef(finalGap, lastElevation, 
                               PlatformWidthKey.UNIT_X8, PlatformType.SOLID)); 
        }

        List<Platform> platforms = new ArrayList<>();
        List<PowerUp> powerUpsList = new ArrayList<>();
        double xPlacementTracker = 0; 

        for (int i = 0; i < platformDefs.size(); i++) {
            PlatformDef def = platformDefs.get(i);
            
            double platformActualWidth = getWidth(def.width);
            double platformActualY = getElevation(def.elevation);
            
            xPlacementTracker += def.xOffset; 
            double platformActualX = xPlacementTracker;
            
            if (platformActualX >= W_CHUNK_LENGTH) continue; 
            
            if (platformActualX + platformActualWidth > W_CHUNK_LENGTH) {
                platformActualWidth = W_CHUNK_LENGTH - platformActualX;
                 if (platformActualWidth < PLAYER_WIDTH_UNIT * 0.5) continue; 
            }

            Platform newPlatform = new Platform(platformActualX, platformActualY, platformActualWidth, PLAT_H, def.type);
            platforms.add(newPlatform);
            
            xPlacementTracker = platformActualX + platformActualWidth;

            if (def.powerUpType != null) {
                double puX = platformActualX + platformActualWidth / 2 - SHIELD_SIZE / 2;
                double puY = platformActualY - PLAT_H - shieldYOffset;
                powerUpsList.add(new PowerUp(puX, puY, SHIELD_SIZE, SHIELD_SIZE, def.powerUpType));
            }
        }
        
        return new ChunkData(platforms.toArray(new Platform[0]), powerUpsList.toArray(new PowerUp[0]));
    }

    public static class ChunkData {
        public Platform[] platforms;
        public PowerUp[] powerUps;
        public ChunkData(Platform[] p, PowerUp[] pu) {
            this.platforms = p;
            this.powerUps = pu;
        }
    }
}
