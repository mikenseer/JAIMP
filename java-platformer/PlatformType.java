import java.awt.Color; // Though not used directly here, often kept for context or future use

// Defines the types of platforms available in the game
final class PlatformType {
    public static final int SOLID = 0;  // Standard solid platform
    public static final int HAZARD = 1; // A platform that harms the player
    public static final int GOAL = 2;   // The platform to reach to win the level/chunk
    public static final int BOUNCE = 3; // A platform that bounces the player higher
}
