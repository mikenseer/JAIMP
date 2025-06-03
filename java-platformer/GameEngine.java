import java.awt.*;
import java.awt.geom.*;
import java.awt.Polygon;

import javax.swing.*;

import java.awt.event.*;
import java.awt.image.*;
import java.io.*;

import java.util.Stack;
import java.util.Random;

import javax.imageio.*;
import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ExecutorService; // For asynchronous audio
import java.util.concurrent.Executors;     // For creating thread pool
import java.util.concurrent.TimeUnit;     // For shutting down executor

public abstract class GameEngine implements KeyListener, MouseListener, MouseMotionListener {
    //-------------------------------------------------------
    // Game Engine Frame and Panel
    //-------------------------------------------------------
    JFrame mFrame;
    GamePanel mPanel;
    int mWidth, mHeight;
    Graphics2D mGraphics;
    boolean initialised = false;
    // Add this field inside your GameEngine class, e.g., near other fields
// Inside GameEngine class
private final ExecutorService audioExecutor = Executors.newFixedThreadPool(12); // User specified 7 threads
private final Random audioRandom = new Random(); // For sound effects (if not already present)
    //-------------------------------------------------------
    // Time-Related functions
    //-------------------------------------------------------

    // Returns the time in milliseconds
    public long getTime() {
        // Get the current time from the system
        return System.currentTimeMillis();
    }

    // Waits for ms milliseconds
    public void sleep(double ms) {
        try {
            // Sleep
            Thread.sleep((long)ms);
        } catch(Exception e) {
            // Do Nothing
        }
    }

    //-------------------------------------------------------
    // Functions to control the framerate
    //-------------------------------------------------------
    // Two variables to keep track of how much time has passed between frames
    long time = 0, oldTime = 0;

    // Returns the time passed since this function was last called.
    public long measureTime() {
        time = getTime();
        if(oldTime == 0) {
            oldTime = time;
        }
        long passed = time - oldTime;
        oldTime = time;
        return passed;
    }

    //-------------------------------------------------------
    // Functions for setting up the window
    //-------------------------------------------------------
    // Function to create the window and display it
    public void setupWindow(int width, int height) {
        mFrame = new JFrame();
        mPanel = new GamePanel();

        mWidth = width;
        mHeight = height;

        mFrame.setSize(width, height);
        mFrame.setLocation(200,200);
        mFrame.setTitle("Window");
        mFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mFrame.add(mPanel);
        mFrame.setVisible(true);

        mPanel.setDoubleBuffered(true);
        mPanel.addMouseListener(this);
        mPanel.addMouseMotionListener(this);

        // Register a key event dispatcher to get a turn in handling all
        // key events, independent of which component currently has the focus
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(new KeyEventDispatcher() {
                    @Override
                    public boolean dispatchKeyEvent(KeyEvent e) {
                        switch (e.getID()) {
                        case KeyEvent.KEY_PRESSED:
                            GameEngine.this.keyPressed(e);
                            return false;
                        case KeyEvent.KEY_RELEASED:
                            GameEngine.this.keyReleased(e);
                            return false;
                        case KeyEvent.KEY_TYPED:
                            GameEngine.this.keyTyped(e);
                            return false;
                        default:
                            return false; // do not consume the event
                        }
                    }
                });

        // Resize the window
        mPanel.setPreferredSize(new Dimension(width, height));
        mFrame.setResizable(false);
        mFrame.pack();
    }

    public void setWindowSize(final int width, final int height) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                mWidth = width;
                mHeight = height;
                // Resize the window
                mPanel.setPreferredSize(new Dimension(width, height));
                mPanel.invalidate();
                mFrame.pack();
            }
        });
    }

    // Return the width of the window
    public int width() {
        return mWidth;
    }

    // Return the height of the window
    public int height() {
        return mHeight;
    }

    //-------------------------------------------------------
    // Main Game function
    //-------------------------------------------------------

    public GameEngine() {
        this(500, 500);
    }

    // GameEngine Constructor
    public GameEngine(int width, int height) {
        // Create graphics transform stack
        mTransforms = new Stack<AffineTransform>();

        // Set default width, height
        mWidth = width;
        mHeight = height;

        // Create window
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Create the window
                setupWindow(width, height);
            }
        });
    }

    // Create Game Function
    public static void createGame(GameEngine game, int framerate) {
        // Initialise Game
        game.init();

        // Start the Game
        game.gameLoop(framerate);
    }

    public static void createGame(GameEngine game) {
        // Call CreateGame
        createGame(game, 244);
    }

    // Game Timer
    protected class GameTimer extends Timer {
        private static final long serialVersionUID = 1L;
        private int framerate;

        protected GameTimer(int framerate, ActionListener listener) {
            super(1000/framerate, listener);
            this.framerate = framerate;
        }

        protected void setFramerate(int framerate) {
            if (framerate < 1) framerate = 1;
            this.framerate = framerate;

            int delay = 1000 / framerate;
            setInitialDelay(0);
            setDelay(delay);
        }

        protected int getFramerate() {
            return framerate;
        }
    }

    // Main Loop of the game. Runs continuously
    // and calls all the updates of the game and
    // tells the game to display a new frame.
    GameTimer timer = new GameTimer(30, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Determine the time step
            double passedTime = measureTime();
            double dt = passedTime / 1000.;

            // Update the Game
            update(dt);

            // Tell the Game to draw
            mPanel.repaint();
        }
    });

    // The GameEngine main Panel
    protected class GamePanel extends JPanel {
        private static final long serialVersionUID = 1L;

        // This gets called any time the Operating System
        // tells the program to paint itself
        public void paintComponent(Graphics graphics) {
            // Get the graphics object
            mGraphics = (Graphics2D)graphics;

            // Reset all transforms
            mTransforms.clear();
            mTransforms.push(mGraphics.getTransform());

            // Rendering settings
            mGraphics.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));

            // Paint the game
            if (initialised) {
                GameEngine.this.paintComponent();
            }
        }
    }

    public void drawSolidPolygon(int[] xPoints, int[] yPoints, int nPoints) {
    if (mGraphics != null) { // mGraphics is your Graphics/Graphics2D instance
        // If mGraphics is just Graphics, fillPolygon is directly available.
        // If it's Graphics2D, fillPolygon is also available.
        mGraphics.fillPolygon(xPoints, yPoints, nPoints);

        // Alternatively, if you want to ensure it's Graphics2D and use Polygon object:
        // if (mGraphics instanceof java.awt.Graphics2D) {
        //     java.awt.Graphics2D g2d = (java.awt.Graphics2D) mGraphics;
        //     java.awt.Polygon poly = new java.awt.Polygon(xPoints, yPoints, nPoints);
        //     g2d.fill(poly);
        // } else {
        //     mGraphics.fillPolygon(xPoints, yPoints, nPoints); // Fallback for basic Graphics
        // }
    } else {
        System.err.println("Graphics context (mGraphics) is null in drawSolidPolygon.");
    }
}

  
// Frequencies for notes (A minor scale and related, for synthwave)
    // Octave 1
    private static final double A1 = 55.00;
    // Octave 2
    private static final double E2 = 82.41;
    private static final double F2 = 87.31;
    private static final double G2 = 98.00;
    private static final double Gsharp2_Aflat2 = 103.83; 
    private static final double A2 = 110.00; 
    private static final double B2 = 123.47; 
    private static final double C3 = 130.81;
    // Octave 3
    private static final double D3 = 146.83;
    private static final double E3 = 164.81;
    private static final double F3 = 174.61; 
    private static final double G3 = 196.00; 
    private static final double Gsharp3_Aflat3 = 207.65;
    private static final double A3 = 220.00; 
    private static final double B3 = 246.94; 
    private static final double C4 = 261.63;
    // Octave 4
    private static final double D4 = 293.66;
    private static final double E4 = 329.63;
    private static final double F4 = 349.23;
    private static final double G4 = 392.00;
    private static final double A4 = 440.00;
    private static final double B4 = 493.88;
    private static final double C5 = 523.25;


    // Define 11 chords in/related to A minor, voiced for synthwave
    // Ordered from "highest sounding" (for highest platforms) to "lowest sounding"
    private static final double[][] CHORDS = {
        { A3, C4, E4 },           // 0: Am (high)
        { G3, B3, D4 },           // 1: G major (high)
        { F3, A3, C4 },           // 2: F major (mid-high)
        { E3, Gsharp3_Aflat3, B3 },// 3: E major (mid-high dominant)
        { D3, F3, A3 },           // 4: Dm (mid)
        { C3, E3, G3 },           // 5: C major (mid, relative major)
        { A2, C3, E3 },           // 6: Am (mid-low)
        { G2, B2, D3 },           // 7: G major (low)
        { F2, A2, C3 },           // 8: F major (low)
        { E2, Gsharp2_Aflat2, B2 },// 9: E major (very low dominant)
        { A1, C3, E3 }            // 10: Am (lowest root) - A1 is 55Hz
    };


    /**
     * Plays a synthwave-style chord asynchronously.
     * @param platformFrequency The frequency derived from platform height (used to select chord).
     * This value is expected to be already halved by Player.java (e.g., 55Hz to 440Hz).
     * @param durationMs The total duration of the tone in milliseconds.
     */
    public void playTone(double platformFrequency, int durationMs) {
        if (platformFrequency <= 0 || durationMs <= 0) return;
        
        // Map the incoming platformFrequency (approx 55Hz to 440Hz) to one of the 11 chords.
        // Higher platformFrequency (from higher on-screen platforms) -> lower chordIndex (higher pitched chord set)
        // Lower platformFrequency (from lower on-screen platforms) -> higher chordIndex (lower pitched chord set)
        
        // The platformFrequency range from Player.java is minFreqPlayer/2 to maxFreqPlayer/2
        // minFreqPlayer = 110, maxFreqPlayer = 880. So range is 55 to 440.
        double minInputFreq = 55.0;
        double maxInputFreq = 440.0;
        double inputFreqRange = maxInputFreq - minInputFreq;

        int chordIndex;
        if (inputFreqRange <= 0) { // Avoid division by zero if range is bad
            chordIndex = CHORDS.length / 2;
        } else {
            // Normalize the frequency: 0.0 for minInputFreq, 1.0 for maxInputFreq
            double normalizedInputFreq = (platformFrequency - minInputFreq) / inputFreqRange;
            normalizedInputFreq = Math.max(0.0, Math.min(1.0, normalizedInputFreq)); // Clamp

            // Map so high platform (high input freq) -> CHORDS[0] (highest pitch set)
            // and low platform (low input freq) -> CHORDS[CHORDS.length-1] (lowest pitch set)
            chordIndex = (int) (normalizedInputFreq * (CHORDS.length -1) ); // This maps 0..1 to 0..10
                                                                         // We want high freq to be low index
            chordIndex = (CHORDS.length - 1) - chordIndex; // Invert the index
        }
        chordIndex = Math.max(0, Math.min(CHORDS.length - 1, chordIndex)); // Clamp index
        
        final double[] currentChordFreqs = CHORDS[chordIndex];

        audioExecutor.submit(() -> {
            SourceDataLine sdl = null;
            try {
                float sampleRate = 44100;
                int bitsPerSample = 8; 
                AudioFormat af = new AudioFormat(sampleRate, bitsPerSample, 1, true, false);
                sdl = AudioSystem.getSourceDataLine(af);
                
                final SourceDataLine finalSdl = sdl;
                sdl.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        if (finalSdl.isOpen()) finalSdl.close();
                    }
                });

                sdl.open(af);
                sdl.start();

                int numSamples = (int) ((durationMs / 1000.0) * sampleRate);
                byte[] buf = new byte[numSamples];

                float attackTimeSec = 0.008f * (durationMs / 1000.0f); 
                float decayTimeSec = 0.15f * (durationMs / 1000.0f);   
                float sustainLevel = 0.70f;                    
                float releaseTimeSec = 0.25f * (durationMs / 1000.0f); 

                double detuneFactor = 1.006; 
                double harmonicNormalization = 1.0 + 1.0/2.0 + 1.0/3.0 + 1.0/4.0 + 1.0/5.0; // Sum of 1/k for k=1 to 5

                for (int i = 0; i < numSamples; i++) {
                    double currentTimeSec = (double) i / sampleRate;
                    double combinedSampleValue = 0.0;

                    for (int noteIndex = 0; noteIndex < currentChordFreqs.length; noteIndex++) {
                        double noteFreq1 = currentChordFreqs[noteIndex];
                        double noteFreq2 = noteFreq1 * detuneFactor; 

                        double angle1 = currentTimeSec * noteFreq1 * 2.0 * Math.PI;
                        double angle2 = currentTimeSec * noteFreq2 * 2.0 * Math.PI;
                        
                        double osc1Saw = 0.0;
                        double osc2Saw = 0.0;
                        for (int k=1; k <= 5; k++) { // Sum first 5 harmonics
                            osc1Saw += (1.0/k) * Math.sin(k * angle1);
                            osc2Saw += (1.0/k) * Math.sin(k * angle2);
                        }
                        osc1Saw /= harmonicNormalization; 
                        osc2Saw /= harmonicNormalization;
                        
                        combinedSampleValue += (osc1Saw + osc2Saw) * 0.5; // Average the two detuned oscillators for this note
                    }
                    
                    double amplitudeMultiplier = 0.0;
                    if (currentTimeSec < attackTimeSec) { 
                        amplitudeMultiplier = (attackTimeSec > 0.0001) ? currentTimeSec / attackTimeSec : 1.0;
                    } else if (currentTimeSec < attackTimeSec + decayTimeSec) { 
                        amplitudeMultiplier = 1.0 - ((currentTimeSec - attackTimeSec) / decayTimeSec) * (1.0 - sustainLevel);
                    } else if (currentTimeSec < (durationMs / 1000.0f) - releaseTimeSec) { 
                        amplitudeMultiplier = sustainLevel;
                    } else { 
                        double timeIntoRelease = currentTimeSec - ((durationMs / 1000.0f) - releaseTimeSec);
                        if (releaseTimeSec > 0.001) { 
                            amplitudeMultiplier = sustainLevel * (1.0 - (timeIntoRelease / releaseTimeSec));
                        } else {
                            amplitudeMultiplier = 0; 
                        }
                    }
                    
                    amplitudeMultiplier = Math.max(0.0, Math.min(1.0, amplitudeMultiplier));
                    // Scale final sample. Max combinedSampleValue is approx number of notes (3 or 4).
                    // Let's use a general scaling factor, e.g., 20-25 for 8-bit.
                    buf[i] = (byte) (combinedSampleValue * amplitudeMultiplier * 22); 
                }
                sdl.write(buf, 0, numSamples);
            } catch (LineUnavailableException e) {
                System.err.println("Audio line unavailable for synth chord: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Unexpected error playing synth chord: " + e.getMessage());
            } finally {
                if (sdl != null && sdl.isOpen() && !sdl.isRunning()) { 
                    sdl.close();
                }
            }
        });
    }

    public void playMidAirJumpSound() { 
        audioExecutor.submit(() -> {
            SourceDataLine sdl = null;
            try {
                float sampleRate = 44100; int bitsPerSample = 8;
                AudioFormat af = new AudioFormat(sampleRate, bitsPerSample, 1, true, false);
                sdl = AudioSystem.getSourceDataLine(af);
                final SourceDataLine finalSdl = sdl;
                sdl.addLineListener(event -> { if (event.getType() == LineEvent.Type.STOP && finalSdl.isOpen()) finalSdl.close(); });
                sdl.open(af); sdl.start();
                int durationMs = 180; 
                int numSamples = (int) ((durationMs / 1000.0) * sampleRate);
                byte[] buf = new byte[numSamples];
                double startFreq = 380; double endFreq = 250;
                for (int i = 0; i < numSamples; i++) {
                    double progress = (double)i / numSamples;
                    double currentFreq = startFreq - Math.pow(progress, 0.6) * (startFreq - endFreq); 
                    double angle = (i / (sampleRate / currentFreq)) * 2.0 * Math.PI;
                    double sampleValue = 0.7 * Math.sin(angle) + 0.3 * Math.sin(1.8 * angle + 0.15);
                    double envelope;
                    if (progress < 0.1) { envelope = progress / 0.1; } 
                    else { envelope = Math.exp(-(progress - 0.1) * 12.0); }
                    envelope = Math.max(0, Math.min(1, envelope));
                    buf[i] = (byte) (sampleValue * 60 * envelope);
                }
                sdl.write(buf, 0, numSamples);
            } catch (Exception e) { if (sdl != null && sdl.isOpen()) sdl.close(); }
        });
    }
    public void playGroundJumpPuffSound() { 
        audioExecutor.submit(() -> {
            SourceDataLine sdl = null;
            try {
                float sampleRate = 44100; int bitsPerSample = 8;
                AudioFormat af = new AudioFormat(sampleRate, bitsPerSample, 1, true, false);
                sdl = AudioSystem.getSourceDataLine(af);
                final SourceDataLine finalSdl = sdl;
                sdl.addLineListener(event -> { if (event.getType() == LineEvent.Type.STOP && finalSdl.isOpen()) finalSdl.close(); });
                sdl.open(af); sdl.start();
                int durationMs = 120;
                int numSamples = (int) ((durationMs / 1000.0) * sampleRate);
                byte[] buf = new byte[numSamples];
                for (int i = 0; i < numSamples; i++) {
                    buf[i] = (byte) (audioRandom.nextInt(80) - 40);
                }
                for (int i = 0; i < numSamples; i++) {
                    double progress = (double)i / numSamples;
                    double amplitudeMultiplier = Math.sin(progress * Math.PI * 0.9 + 0.05 * Math.PI);
                    amplitudeMultiplier *= Math.exp(-progress * 7.0); 
                    buf[i] = (byte) (buf[i] * amplitudeMultiplier * 0.30); 
                }
                sdl.write(buf, 0, numSamples);
            } catch (Exception e) { if (sdl != null && sdl.isOpen()) sdl.close(); }
        });
    }
    public void playGroundLandingPfftSound() { 
        audioExecutor.submit(() -> {
            SourceDataLine sdl = null;
            try {
                float sampleRate = 44100; int bitsPerSample = 8;
                AudioFormat af = new AudioFormat(sampleRate, bitsPerSample, 1, true, false);
                sdl = AudioSystem.getSourceDataLine(af);
                final SourceDataLine finalSdl = sdl;
                sdl.addLineListener(event -> { if (event.getType() == LineEvent.Type.STOP && finalSdl.isOpen()) finalSdl.close(); });
                sdl.open(af); sdl.start();
                int durationMs = 90;
                int numSamples = (int) ((durationMs / 1000.0) * sampleRate);
                byte[] buf = new byte[numSamples];
                for (int i = 0; i < numSamples; i++) {
                    buf[i] = (byte) (audioRandom.nextInt(80) - 40);
                }
                for (int i = 0; i < numSamples; i++) {
                    double progress = (double)i / numSamples;
                    double amplitudeMultiplier;
                    if (progress < 0.05) { amplitudeMultiplier = progress / 0.05; } 
                    else { amplitudeMultiplier = Math.exp(-(progress - 0.05) * 25.0); }
                    buf[i] = (byte) (buf[i] * amplitudeMultiplier * 0.20); 
                }
                sdl.write(buf, 0, numSamples);
            } catch (Exception e) { if (sdl != null && sdl.isOpen()) sdl.close(); }
        });
    }
    public void playShieldCollectSound() { 
        audioExecutor.submit(() -> {
            SourceDataLine sdl = null;
            try {
                float sampleRate = 44100; int bitsPerSample = 8;
                AudioFormat af = new AudioFormat(sampleRate, bitsPerSample, 1, true, false);
                sdl = AudioSystem.getSourceDataLine(af);
                final SourceDataLine finalSdl = sdl;
                sdl.addLineListener(event -> { if (event.getType() == LineEvent.Type.STOP && finalSdl.isOpen()) finalSdl.close(); });
                sdl.open(af); sdl.start();
                int durationMs = 200;
                int numSamples = (int) ((durationMs / 1000.0) * sampleRate);
                byte[] buf = new byte[numSamples];
                double startFreq = 350; double endFreq = 120; double sweepPower = 0.6; 
                for (int i = 0; i < numSamples; i++) {
                    double progress = (double) i / numSamples; 
                    double currentFreq = startFreq - Math.pow(progress, sweepPower) * (startFreq - endFreq);
                    double angle = (i / (sampleRate / currentFreq)) * 2.0 * Math.PI;
                    double modulator = Math.sin(2 * Math.PI * progress * 12); 
                    double sampleValue = Math.sin(angle + modulator * 0.08) * (1.0 - progress * 0.4); 
                    double envelope;
                    if (progress < 0.08) { envelope = progress / 0.08; } 
                    else if (progress < 0.6) { envelope = 1.0 - (progress - 0.08) * 0.4; } 
                    else { envelope = (1.0 - (progress - 0.6) / 0.4) * 0.6; }
                    envelope = Math.max(0, Math.min(1, envelope));
                    buf[i] = (byte) (sampleValue * 75 * envelope);
                }
                sdl.write(buf, 0, numSamples);
            } catch (Exception e) { if (sdl != null && sdl.isOpen()) sdl.close(); }
        });
    }
    public void playBoingSound() { 
        audioExecutor.submit(() -> {
            SourceDataLine sdl = null;
            try {
                float sampleRate = 44100; int bitsPerSample = 8;
                AudioFormat af = new AudioFormat(sampleRate, bitsPerSample, 1, true, false);
                sdl = AudioSystem.getSourceDataLine(af);
                final SourceDataLine finalSdl = sdl;
                sdl.addLineListener(event -> { if (event.getType() == LineEvent.Type.STOP && finalSdl.isOpen()) finalSdl.close(); });
                sdl.open(af); sdl.start();
                int durationMs = 250;
                int numSamples = (int) ((durationMs / 1000.0) * sampleRate);
                byte[] buf = new byte[numSamples];
                double startFreq = 120; double peakFreq = 550; double endFreq = 120;   
                for (int i = 0; i < numSamples; i++) {
                    double progress = (double)i / numSamples; 
                    double currentFreq;
                    if (progress < 0.25) { currentFreq = startFreq + (peakFreq - startFreq) * (progress / 0.25); } 
                    else { currentFreq = peakFreq - (peakFreq - endFreq) * ((progress - 0.25) / 0.75); }
                    currentFreq = Math.max(20, currentFreq); 
                    double angle = (i / (sampleRate / currentFreq)) * 2.0 * Math.PI;
                    double sampleValue = 0.6 * Math.sin(angle) + 0.25 * Math.sin(1.9 * angle) + 0.15 * Math.sin(0.55 * angle + 0.1);
                    double envelope;
                    if (progress < 0.03) { envelope = progress / 0.03; } 
                    else { envelope = Math.exp(-(progress - 0.03) * 9.0); }
                    envelope = Math.max(0, Math.min(1, envelope));
                    buf[i] = (byte) (sampleValue * 80 * envelope);
                }
                sdl.write(buf, 0, numSamples);
            } catch (Exception e) { if (sdl != null && sdl.isOpen()) sdl.close(); }
        });
    }
    public void playHitSound() { 
        audioExecutor.submit(() -> {
            SourceDataLine sdl = null;
            try {
                float sampleRate = 44100; int bitsPerSample = 8;
                AudioFormat af = new AudioFormat(sampleRate, bitsPerSample, 1, true, false);
                sdl = AudioSystem.getSourceDataLine(af);
                final SourceDataLine finalSdl = sdl;
                sdl.addLineListener(event -> { if (event.getType() == LineEvent.Type.STOP && finalSdl.isOpen()) finalSdl.close(); });
                sdl.open(af); sdl.start();
                int durationMs = 180; 
                int numSamples = (int) ((durationMs / 1000.0) * sampleRate);
                byte[] buf = new byte[numSamples];
                double baseFreq = 90; double noiseFactor = 0.4;
                for (int i = 0; i < numSamples; i++) {
                    double time = (double) i / sampleRate;
                    double envelope = Math.exp(-time * 30.0); 
                    double sineWave = Math.sin(2 * Math.PI * baseFreq * time * (1 + 0.3 * Math.sin(2 * Math.PI * 5 * time + 0.1)));
                    double squareWaveIsh = (Math.sin(2 * Math.PI * baseFreq * 0.5 * time) > 0 ? 1 : -1) * 0.3; 
                    double noise = (audioRandom.nextDouble() * 2.0 - 1.0) * noiseFactor;
                    buf[i] = (byte) ((sineWave * (1 - noiseFactor - 0.1) + squareWaveIsh + noise) * 85 * envelope);
                }
                sdl.write(buf, 0, numSamples);
            } catch (Exception e) { if (sdl != null && sdl.isOpen()) sdl.close(); }
        });
    }
    public void playDeathSound() { 
        audioExecutor.submit(() -> {
            SourceDataLine sdl = null;
            try {
                float sampleRate = 44100; int bitsPerSample = 8;
                AudioFormat af = new AudioFormat(sampleRate, bitsPerSample, 1, true, false);
                sdl = AudioSystem.getSourceDataLine(af);
                final SourceDataLine finalSdl = sdl;
                sdl.addLineListener(event -> { if (event.getType() == LineEvent.Type.STOP && finalSdl.isOpen()) finalSdl.close(); });
                sdl.open(af); sdl.start();
                int durationMs = 800;
                int numSamples = (int) ((durationMs / 1000.0) * sampleRate);
                byte[] buf = new byte[numSamples];
                double startFreq = 700; double endFreq = 60;
                double accumulatedPhase = 0; 

                for (int i = 0; i < numSamples; i++) {
                    double progress = (double) i / numSamples;
                    double currentFreq = startFreq * Math.pow(endFreq / startFreq, progress * progress);
                    
                    accumulatedPhase += (currentFreq / sampleRate) * 2.0 * Math.PI;
                    if (accumulatedPhase > 2.0 * Math.PI) accumulatedPhase -= 2.0 * Math.PI;
                    
                    double sampleValue = 0.4 * Math.sin(accumulatedPhase) 
                                       + 0.2 * Math.sin(2 * accumulatedPhase + 0.5) 
                                       + 0.15 * Math.sin(3 * accumulatedPhase + 1.0)
                                       + 0.1 * (audioRandom.nextDouble() * 0.5 - 0.25); 
                    double envelope = Math.pow(1.0 - progress, 0.75); 
                    buf[i] = (byte) (sampleValue * 90 * envelope);
                }
                sdl.write(buf, 0, numSamples);
            } catch (Exception e) { if (sdl != null && sdl.isOpen()) sdl.close(); }
        });
    }
    public void shutdownAudio() { 
        System.out.println("Shutting down audio executor...");
        audioExecutor.shutdown();
        try {
            if (!audioExecutor.awaitTermination(1, TimeUnit.SECONDS)) { 
                audioExecutor.shutdownNow();
                if (!audioExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    System.err.println("Audio executor did not terminate.");
                }
            }
        } catch (InterruptedException ie) {
            audioExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Audio executor shut down.");
    }

    // Initialises and starts the game loop with the given framerate.
    public void gameLoop(int framerate) {
        initialised = true; // assume init has been called or won't be called

        timer.setFramerate(framerate);
        timer.setRepeats(true);

        // Main loop runs until program is closed
        timer.start();
    }

    //-------------------------------------------------------
    // Initialise function
    //-------------------------------------------------------
    public void init() {}

    //-------------------------------------------------------
    // Update function
    //-------------------------------------------------------
    public abstract void update(double dt);

    //-------------------------------------------------------
    // Paint function
    //-------------------------------------------------------
    public abstract void paintComponent();

    //-------------------------------------------------------
    // Keyboard functions
    //-------------------------------------------------------

    // Called whenever a key is pressed
    public void keyPressed(KeyEvent event) {}

    // Called whenever a key is released
    public void keyReleased(KeyEvent event) {}

    // Called whenever a key is pressed and immediately released
    public void keyTyped(KeyEvent event) {}

    //-------------------------------------------------------
    // Mouse functions
    //-------------------------------------------------------

    // Called whenever a mouse button is clicked
    // (pressed and released in the same position)
    public void mouseClicked(MouseEvent event) {}

    // Called whenever a mouse button is pressed
    public void mousePressed(MouseEvent event) {}

    // Called whenever a mouse button is released
    public void mouseReleased(MouseEvent event) {}

    // Called whenever the mouse cursor enters the game panel
    public void mouseEntered(MouseEvent event) {}

    // Called whenever the mouse cursor leaves the game panel
    public void mouseExited(MouseEvent event) {}

    // Called whenever the mouse is moved
    public void mouseMoved(MouseEvent event) {}

    // Called whenever the mouse is moved with the mouse button held down
    public void mouseDragged(MouseEvent event) {}

    //-------------------------------------------------------
    // Graphics Functions
    //-------------------------------------------------------

    // My Definition of some colors
    Color black = Color.BLACK;
    Color orange = Color.ORANGE;
    Color pink = Color.PINK;
    Color red = Color.RED;
    Color purple = new Color(128, 0, 128);
    Color blue = Color.BLUE;
    Color green = Color.GREEN;
    Color yellow = Color.YELLOW;
    Color white = Color.WHITE;

    // Changes the background Color to the color c
    public void changeBackgroundColor(Color c) {
        // Set background colour
        mGraphics.setBackground(c);
    }

    // Changes the background Color to the color (red,green,blue)
    public void changeBackgroundColor(int red, int green, int blue) {
        // Clamp values
        if(red < 0)   {red = 0;}
        if(red > 255) {red = 255;}

        if(green < 0)   {green = 0;}
        if(green > 255) {green = 255;}

        if(blue < 0)   {blue = 0;}
        if(blue > 255) {blue = 255;}

        // Set background colour
        mGraphics.setBackground(new Color(red,green,blue));
    }

    // Clears the background, makes the whole window whatever the background color is
    public void clearBackground(int width, int height) {
        // Clear background
        mGraphics.clearRect(0, 0, width, height);
    }

    // Changes the drawing Color to the color c
    public void changeColor(Color c) {
        // Set colour
        mGraphics.setColor(c);
    }

    // Changes the drawing Color to the color (red,green,blue)
    public void changeColor(int red, int green, int blue) {
        // Clamp values
        if(red < 0)   {red = 0;}
        if(red > 255) {red = 255;}

        if(green < 0)   {green = 0;}
        if(green > 255) {green = 255;}

        if(blue < 0)   {blue = 0;}
        if(blue > 255) {blue = 255;}

        // Set colour
        mGraphics.setColor(new Color(red,green,blue));
    }

    // Draws a line from (x1,y2) to (x2,y2)
    void drawLine(double x1, double y1, double x2, double y2) {
        // Draw a Line
        mGraphics.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    // Draws a line from (x1,y2) to (x2,y2) with width l
    void drawLine(double x1, double y1, double x2, double y2, double l) {
        // Set the stroke
        mGraphics.setStroke(new BasicStroke((float)l));

        // Draw a Line
        mGraphics.draw(new Line2D.Double(x1, y1, x2, y2));

        // Reset the stroke
        mGraphics.setStroke(new BasicStroke(1.0f));
    }

    // This function draws a rectangle at (x,y) with width and height (w,h)
    void drawRectangle(double x, double y, double w, double h) {
        // Draw a Rectangle
        mGraphics.draw(new Rectangle2D.Double(x, y, w, h));
    }

    // This function draws a rectangle at (x,y) with width and height (w,h)
    // with a line of width l
    void drawRectangle(double x, double y, double w, double h, double l) {
        // Set the stroke
        mGraphics.setStroke(new BasicStroke((float)l));

        // Draw a Rectangle
        mGraphics.draw(new Rectangle2D.Double(x, y, w, h));

        // Reset the stroke
        mGraphics.setStroke(new BasicStroke(1.0f));
    }

    // This function fills in a rectangle at (x,y) with width and height (w,h)
    void drawSolidRectangle(double x, double y, double w, double h) {
        // Fill a Rectangle
        mGraphics.fill(new Rectangle2D.Double(x, y, w, h));
    }

    // This function draws a circle at (x,y) with radius
    void drawCircle(double x, double y, double radius) {
        // Draw a Circle
        mGraphics.draw(new Ellipse2D.Double(x-radius, y-radius, radius*2, radius*2));
    }

    // This function draws a circle at (x,y) with radius
    // with a line of width l
    void drawCircle(double x, double y, double radius, double l) {
        // Set the stroke
        mGraphics.setStroke(new BasicStroke((float)l));

        // Draw a Circle
        mGraphics.draw(new Ellipse2D.Double(x-radius, y-radius, radius*2, radius*2));

        // Reset the stroke
        mGraphics.setStroke(new BasicStroke(1.0f));
    }

    // This function draws a circle at (x,y) with radius
    void drawSolidCircle(double x, double y, double radius) {
        // Fill a Circle
        mGraphics.fill(new Ellipse2D.Double(x-radius, y-radius, radius*2, radius*2));
    }

    // This function draws text on the screen at (x,y)
    public void drawText(double x, double y, String s) {
        // Draw text on the screen
        mGraphics.setFont(new Font("Arial", Font.PLAIN, 40));
        mGraphics.drawString(s, (int)x, (int)y);
    }

    // This function draws bold text on the screen at (x,y)
    public void drawBoldText(double x, double y, String s) {
        // Draw text on the screen
        mGraphics.setFont(new Font("Arial", Font.BOLD, 40));
        mGraphics.drawString(s, (int)x, (int)y);
    }

    // This function draws text on the screen at (x,y)
    // with Font (font,size)
    public void drawText(double x, double y, String s, String font, int size) {
        // Draw text on the screen
        mGraphics.setFont(new Font(font, Font.PLAIN, size));
        mGraphics.drawString(s, (int)x, (int)y);
    }

    // This function draws bold text on the screen at (x,y)
    // with Font (font,size)
    public void drawBoldText(double x, double y, String s, String font, int size) {
        // Draw text on the screen
        mGraphics.setFont(new Font(font, Font.BOLD, size));
        mGraphics.drawString(s, (int)x, (int)y);
    }

    //-------------------------------------------------------
    // Image Functions
    //-------------------------------------------------------

    // Loads an image from file
    public static Image loadImage(String filename) {
        try {
            // Load Image
            Image image = ImageIO.read(new File(filename));

            // Return Image
            return image;
        } catch (IOException e) {
            // Show Error Message
            System.out.println("Error: could not load image " + filename);
            System.exit(1);
        }

        // Return null
        return null;
    }

    // Loads a sub-image out of an image
    public static Image subImage(Image source, int x, int y, int w, int h) {
        // Check if image is null
        if(source == null) {
            // Print Error message
            System.out.println("Error: cannot extract a subImage from a null image.\n");

            // Return null
            return null;
        }

        // Convert to a buffered image
        BufferedImage buffered = (BufferedImage)source;

        // Extract sub image
        Image image = buffered.getSubimage(x, y, w, h);

        // Return image
        return image;
    }

    // Draws an image on the screen at position (x,y)
    public void drawImage(Image image, double x, double y) {
        // Check if image is null
        if(image == null) {
            // Print Error message
            System.out.println("Error: cannot draw null image.\n");
            return;
        }

        // Draw image on screen at (x,y)
        mGraphics.drawImage(image, (int)x, (int)y, null);
    }

    // Draws an image on the screen at position (x,y)
    public void drawImage(Image image, double x, double y, double w, double h) {
        // Check if image is null
        if(image == null) {
            // Print Error message
            System.out.println("Error: cannot draw null image.\n");
            return;
        }
        // Draw image on screen at (x,y) with size (w,h)
        mGraphics.drawImage(image, (int)x, (int)y, (int)w, (int)h, null);
    }

    //-------------------------------------------------------
    // Transform Functions
    //-------------------------------------------------------

    // Stack of transforms
    Stack<AffineTransform> mTransforms;

    // Save the current transform
    public void saveCurrentTransform() {
        // Push transform onto the stack
        mTransforms.push(mGraphics.getTransform());
    }

    // Restores the last transform
    public void restoreLastTransform() {
        // Set current transform to the top of the stack.
        mGraphics.setTransform(mTransforms.peek());

        // If there is more than one transform on the stack
        if(mTransforms.size() > 1) {
            // Pop a transform off the stack
            mTransforms.pop();
        }
    }

    // This function translates the drawing context by (x,y)
    void translate(double x, double y) {
        // Translate the drawing context
        mGraphics.translate(x,y);
    }

    // This function rotates the drawing context by a degrees
    void rotate(double a) {
        // Rotate the drawing context
        mGraphics.rotate(Math.toRadians(a));
    }

    // This function scales the drawing context by (x,y)
    void scale(double x, double y) {
        // Scale the drawing context
        mGraphics.scale(x, y);
    }

    // This function shears the drawing context by (x,y)
    void shear(double x, double y) {
        // Shear the drawing context
        mGraphics.shear(x, y);
    }

    //-------------------------------------------------------
    // Sound Functions
    //-------------------------------------------------------

    // Class used to store an audio clip
    public static class AudioClip {
        // Format
        AudioFormat mFormat;

        // Audio Data
        byte[] mData;

        // Buffer Length
        long mLength;

        // Loop Clip
        Clip mLoopClip;

        public Clip getLoopClip() {
            // return mLoopClip
            return mLoopClip;
        }

        public void setLoopClip(Clip clip) {
            // Set mLoopClip to clip
            mLoopClip = clip;
        }

        public AudioFormat getAudioFormat() {
            // Return mFormat
            return mFormat;
        }

        public byte[] getData() {
            // Return mData
            return mData;
        }

        public long getBufferSize() {
            // Return mLength
            return mLength;
        }

        public AudioClip(AudioInputStream stream) {
            // Get Format
            mFormat = stream.getFormat();

            // Get length (in Frames)
            mLength = stream.getFrameLength() * mFormat.getFrameSize();

            // Allocate Buffer Data
            mData = new byte[(int)mLength];

            try {
                // Read data
                stream.read(mData);
            } catch(Exception exception) {
                // Print Error
                System.out.println("Error reading Audio File\n");

                // Exit
                System.exit(1);
            }

            // Set LoopClip to null
            mLoopClip = null;
        }

        public void setLoopVolume(float volume) {
            try {
                // Create Controls
                FloatControl control = (FloatControl)mLoopClip.getControl(FloatControl.Type.MASTER_GAIN);
                // Set Volume
                control.setValue(volume);
            } catch (Exception e) {
                System.out.println("Error setting audio loop volume: " + e);
            }
        }
    }

    // Loads the AudioClip stored in the file specified by filename
    public static AudioClip loadAudio(String filename) {
        try {
            // Open File
            File file = new File(filename);

            // Open Audio Input Stream
            AudioInputStream audio = AudioSystem.getAudioInputStream(file);

            // Create Audio Clip
            AudioClip clip = new AudioClip(audio);

            // Fix pauses and mixer issues when clip is first played
            playAudio(clip,-60);

            // Return Audio Clip
            return clip;
        } catch(Exception e) {
            // Catch Exception
            System.out.println("Error: cannot open Audio File " + filename + "\n");
        }

        // Return Null
        return null;
    }

    // Plays an AudioClip
    public static boolean playAudio(AudioClip audioClip) {
        // Check audioClip for null
        if(audioClip == null) {
            // Print error message
            System.out.println("Error: audioClip is null\n");

            // Return
            return false;
        }

        try {
            // Create a Clip
            Clip clip = AudioSystem.getClip();
            clip.addLineListener(new LineListener() {
                @Override
                public void update(LineEvent event) {
                    if(event.getType().equals(LineEvent.Type.STOP)) {
                        clip.close();
                    }
                }
            });
            // Load data
            clip.open(audioClip.getAudioFormat(), audioClip.getData(), 0, (int)audioClip.getBufferSize());

            // Play Clip
            clip.start();
        } catch(Exception exception) {
            // Display Error Message
            System.out.println("Error playing Audio Clip\n");
            return false;
        }

        return true;
    }

    // Plays an AudioClip with a volume in decibels
    public static boolean playAudio(AudioClip audioClip, float volume) {
        // Check audioClip for null
        if(audioClip == null) {
            // Print error message
            System.out.println("Error: audioClip is null\n");

            // Return
            return false;
        }

        try {
            // Create a Clip
            Clip clip = AudioSystem.getClip();
            clip.addLineListener(new LineListener() {
                @Override
                public void update(LineEvent event) {
                    if(event.getType().equals(LineEvent.Type.STOP)) {
                        clip.close();
                    }
                }
            });
            // Load data
            clip.open(audioClip.getAudioFormat(), audioClip.getData(), 0, (int)audioClip.getBufferSize());

            // Create Controls
            FloatControl control = (FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);

            // Set Volume
            control.setValue(volume);

            // Play Clip
            clip.start();
        } catch(Exception exception) {
            // Display Error Message
            System.out.println("Error: could not play Audio Clip\n");
            return false;
        }

        return true;
    }


    // Starts playing an AudioClip on loop
    public static boolean startAudioLoop(AudioClip audioClip) {
        // Check audioClip for null
        if(audioClip == null) {
            // Print error message
            System.out.println("Error: audioClip is null\n");

            // Return
            return false;
        }

        // Get Loop Clip
        Clip clip = audioClip.getLoopClip();

        // Create Loop Clip if necessary
        if(clip == null) {
            try {
                // Create a Clip
                clip = AudioSystem.getClip();

                // Load data
                clip.open(audioClip.getAudioFormat(), audioClip.getData(), 0, (int)audioClip.getBufferSize());

                // Set Clip to Loop
                clip.loop(Clip.LOOP_CONTINUOUSLY);

                // Set Loop Clip
                audioClip.setLoopClip(clip);
            } catch(Exception exception) {
                // Display Error Message
                System.out.println("Error: could not play Audio Clip\n");
                return false;
            }
        }

        // Set Frame Position to 0
        clip.setFramePosition(0);

        // Start Audio Clip playing
        clip.start();

        return true;
    }

    // Starts playing an AudioClip on loop with a volume in decibels
    public static boolean startAudioLoop(AudioClip audioClip, float volume) {
        boolean success = startAudioLoop(audioClip);
        if (success)
            audioClip.setLoopVolume(volume);
        return success;
    }

    // Stops an AudioClip playing
    public static void stopAudioLoop(AudioClip audioClip) {
        // Get Loop Clip
        Clip clip = audioClip.getLoopClip();

        // Check clip is not null
        if(clip != null){
            // Stop Clip playing
            clip.stop();
        }
    }

    //-------------------------------------------------------
    // Maths Functions
    //-------------------------------------------------------
    Random mRandom = null;

    // Function that returns a random integer between 0 and max
    public int rand(int max) {
        // Check if mRandom Exists
        if(mRandom == null) {
            // Create a new Random Object
            mRandom = new Random();
        }

        // Generate a random number
        double d = mRandom.nextDouble();

        // Convert to an integer in range [0, max) and return
        return (int)(d*max);
    }

    // Function that gives you a random number between 0 and max
    public float rand(float max) {
        // Check if mRandom Exists
        if(mRandom == null) {
            // Create a new Random Object
            mRandom = new Random();
        }

        // Generate a random number
        float d = mRandom.nextFloat();

        // Convert to range [0, max) and return
        return d*max;
    }

    // Function that gives you a random number between 0 and max
    public double rand(double max) {
        // Check if mRandom Exists
        if(mRandom == null) {
            // Create a new Random Object
            mRandom = new Random();
        }

        // Generate a random number
        double value = mRandom.nextDouble();

        // Convert to range [0, max) and return
        return value*max;
    }

    // Returns the largest integer that is less than or equal
    // to the argument value.
    public static int floor(double value) {
        // Calculate and return floor
        return (int)Math.floor(value);
    }

    // Returns the smallest integer that is greater than or equal
    // to the argument value.
    public static int ceil(double value){
        // Calculate and return ceil
        return (int)Math.ceil(value);
    }

    // Rounds the argument value to the closest integer.
    public static int round(double value) {
        // Calculate and return round
        return (int)Math.round(value);
    }

    // Returns the square root of the parameter
    public static double sqrt(double value) {
        // Calculate and return the sqrt
        return Math.sqrt(value);
    }

    // Returns the length of a vector
    public static double length(double x, double y) {
        // Calculate and return the sqrt
        return Math.sqrt(x*x + y*y);
    }

    // Returns the distance between two points (x1,y1) and (x2,y2)
    public static double distance(double x1, double y1, double x2, double y2) {
        // Calculate and return the distance
        return Math.sqrt(Math.pow(x2-x1, 2) + Math.pow(y2-y1, 2));
    }

    // Converts an angle in radians to degrees
    public static double toDegrees(double radians) {
        // Calculate and return the degrees
        return Math.toDegrees(radians);
    }

    // Converts an angle in degrees to radians
    public static double toRadians(double degrees) {
        // Calculate and return the radians
        return Math.toRadians(degrees);
    }

    // Returns the absolute value of the parameter
    public static int abs(int value) {
        // Calculate and return abs
        return Math.abs(value);
    }

    // Returns the absolute value of the parameter
    public static float abs(float value) {
        // Calculate and return abs
        return Math.abs(value);
    }

    // Returns the absolute value of the parameter
    public static double abs(double value) {
        // Calculate and return abs
        return Math.abs(value);
    }

    // Returns the cos of value
    public static double cos(double value) {
        // Calculate and return cos
        return Math.cos(Math.toRadians(value));
    }

    // Returns the acos of value
    public static double acos(double value) {
        // Calculate and return acos
        return Math.toDegrees(Math.acos(value));
    }

    // Returns the sin of value
    public static double sin(double value) {
        // Calculate and return sin
        return Math.sin(Math.toRadians(value));
    }

    // Returns the asin of value
    public static double asin(double value) {
        // Calculate and return asin
        return Math.toDegrees(Math.asin(value));
    }

    // Returns the tan of value
    public static double tan(double value) {
        // Calculate and return tan
        return Math.tan(Math.toRadians(value));
    }
    // Returns the atan of value
    public static double atan(double value) {
        // Calculate and return atan
        return Math.toDegrees(Math.atan(value));
    }
    // Returns the atan2 of value
    public static double atan2(double x, double y) {
        // Calculate and return atan2
        return Math.toDegrees(Math.atan2(x,y));
    }
}
