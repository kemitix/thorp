package net.kemitix.thorp.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Terminal {

    public static String esc = "\u001B";
    public static String csi = esc + "[";

    /**
     * Clear from cursor to end of screen.
     */
    public static String eraseToEndOfScreen = csi + "0J";

    /**
     * Clear from cursor to beginning of screen.
     */
    public static String eraseToStartOfScreen = csi + "1J";

    /**
     * Clear screen and move cursor to top-left.
     *
     * On DOS the "2J" command also moves to 1,1, so we force that behaviour for all.
     */
    public static String eraseScreen = csi + "2J" + cursorPosition(1, 1);

    /**
     * Clear screen and scrollback buffer then move cursor to top-left.
     *
     * Anticipate that same DOS behaviour here, and to maintain consistency with {@link #eraseScreen}.
     */
    public static String eraseScreenAndBuffer = csi + "3J";

    /**
     * Clears the terminal line to the right of the cursor.
     *
     * Does not move the cursor.
     */
    public static String eraseLineForward = csi + "0K";

    /**
     * Clears the terminal line to the left of the cursor.
     *
     * Does not move the cursor.
     */
    public static String eraseLineBack = csi + "1K";

    /**
     * Clears the whole terminal line.
     *
     * Does not move the cursor.
     */
    public static String eraseLine = csi + "2K";

    /**
     * Saves the cursor position/state.
     */
    public static String saveCursorPosition = csi + "s";

    /**
     * Restores the cursor position/state.
     */
    public static String restoreCursorPosition  = csi + "u";
    public static String enableAlternateBuffer  = csi + "?1049h";
    public static String disableAlternateBuffer = csi + "?1049l";

    public static String reset = "\u001B[0m";
    public static String red = "\u001B[31m";
    public static String green = "\u001B[32m";
    public static String white = "\u001B[37m";

    private static Map<Integer, String> getSubBars() {
        Map<Integer, String> subBars = new HashMap<>();
        subBars.put(0, " ");
        subBars.put(1, "▏");
        subBars.put(2, "▎");
        subBars.put(3, "▍");
        subBars.put(4, "▌");
        subBars.put(5, "▋");
        subBars.put(6, "▊");
        subBars.put(7, "▉");
        return subBars;
    }

    /**
     * Move the cursor up, default 1 line.
     *
     * Stops at the edge of the screen.
     */
    public static String cursorUp(int lines) {
        return csi + lines + "A";
    }

    /**
     * Move the cursor down, default 1 line.
     *
     * Stops at the edge of the screen.
     */
    public static String cursorDown(int lines) {
        return csi + lines + "B";
    }

    /**
     * Move the cursor forward, default 1 column.
     *
     * Stops at the edge of the screen.
     */
    public static String cursorForward(int cols) {
        return csi + cols + "C";
    }

    /**
     * Move the cursor back, default 1 column,
     *
     * Stops at the edge of the screen.
     */
    public static String cursorBack(int cols) {
        return csi + cols + "D";
    }

    /**
     * Move the cursor to the beginning of the line, default 1, down.
     */
    public static String cursorNextLine(int lines) {
        return csi + lines + "E";
    }

    /**
     * Move the cursor to the beginning of the line, default 1, up.
     */
    public static String cursorPrevLine(int lines) {
        return csi + lines + "F";
    }

    /**
     * Move the cursor to the column on the current line.
     */
    public static String cursorHorizAbs(int col) {
        return csi + col + "G";
    }

    /**
     * Move the cursor to the position on screen (1,1 is the top-left).
     */
    public static String cursorPosition(int row, int col) {
        return csi + row + ";" + col + "H";
    }

    /**
     * Scroll page up, default 1, lines.
     */
    public static String scrollUp(int lines) {
        return csi + lines + "S";
    }

    /**
     * Scroll page down, default 1, lines.
     */
    public static String scrollDown(int lines) {
        return csi + lines + "T";
    }

    /**
     * The Width of the terminal, as reported by the COLUMNS environment variable.
     *
     * N.B. Not all environment will update this value when the terminal is resized.
     *
     * @return the number of columns in the terminal
     */
    public static int width() {
        return Optional.ofNullable(System.getenv("COLUMNS"))
                .map(Integer::parseInt)
                .map(x -> Math.max(x, 10))
                .orElse(80);
    }

    public static String progressBar(
            double pos,
            double max,
            int width
    ) {
        Map<Integer, String> subBars = getSubBars();
        int barWidth          = width - 2;
        int phases            = subBars.values().size();
        int pxWidth           = barWidth * phases;
        double ratio          = pos / max;
        int pxDone            = (int) (ratio * pxWidth);
        int fullHeadSize      = pxDone / phases;
        int part              = pxDone % phases;
        String partial        = part != 0 ? subBars.getOrDefault(part, "") : "";
        String head           = StringUtil.repeat("█", fullHeadSize) + partial;
        int tailSize          = barWidth - head.length();
        String tail           = StringUtil.repeat(" ", tailSize);
        return "[" + head + tail + "]";
    }

}
