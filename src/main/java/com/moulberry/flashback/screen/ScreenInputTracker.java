package com.moulberry.flashback.screen;

/**
 * Tracks whether the render thread is currently dispatching a user mouse/keyboard event to the open screen.
 * Screens opened during that dispatch are a direct result of user interaction (e.g. clicking "Video Settings")
 * and must be allowed during replay, even when a mod (e.g. Embeddium) substitutes its own screen class.
 */
public class ScreenInputTracker {

    private static int depth = 0;

    public static void enter() {
        depth += 1;
    }

    public static void exit() {
        depth -= 1;
    }

    public static boolean isHandlingUserInput() {
        return depth > 0;
    }

}
