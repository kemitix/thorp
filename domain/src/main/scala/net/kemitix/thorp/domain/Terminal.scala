package net.kemitix.thorp.domain

object Terminal {

  val esc: String = "\u001B"
  val csi: String = esc + "["

  /**
    * Clear from cursor to end of screen.
    */
  val eraseToEndOfScreen: String = csi + "0J"

  /**
    * Clear from cursor to beginning of screen.
    */
  val eraseToStartOfScreen: String = csi + "1J"

  /**
    * Clear screen and move cursor to top-left.
    *
    * On DOS the "2J" command also moves to 1,1, so we force that behaviour for all.
    */
  val eraseScreen: String = csi + "2J" + cursorPosition(1, 1)

  /**
    * Clear screen and scrollback buffer then move cursor to top-left.
    *
    * Anticipate that same DOS behaviour here, and to maintain consistency with {@link #eraseScreen}.
    */
  val eraseScreenAndBuffer: String = csi + "3J"

  /**
    * Clears the terminal line to the right of the cursor.
    *
    * Does not move the cursor.
    */
  val eraseLineForward: String = csi + "0K"

  /**
    * Clears the terminal line to the left of the cursor.
    *
    * Does not move the cursor.
    */
  val eraseLineBack: String = csi + "1K"

  /**
    * Clears the whole terminal line.
    *
    * Does not move the cursor.
    */
  val eraseLine: String = csi + "2K"

  /**
    * Saves the cursor position/state.
    */
  val saveCursorPosition: String = csi + "s"

  /**
    * Restores the cursor position/state.
    */
  val restoreCursorPosition: String  = csi + "u"
  val enableAlternateBuffer: String  = csi + "?1049h"
  val disableAlternateBuffer: String = csi + "?1049l"
  private val subBars = Map(0 -> " ",
                            1 -> "▏",
                            2 -> "▎",
                            3 -> "▍",
                            4 -> "▌",
                            5 -> "▋",
                            6 -> "▊",
                            7 -> "▉")

  /**
    * Move the cursor up, default 1 line.
    *
    * Stops at the edge of the screen.
    */
  def cursorUp(lines: Int = 1): String = csi + lines + "A"

  /**
    * Move the cursor down, default 1 line.
    *
    * Stops at the edge of the screen.
    */
  def cursorDown(lines: Int = 1): String = csi + lines + "B"

  /**
    * Move the cursor forward, default 1 column.
    *
    * Stops at the edge of the screen.
    */
  def cursorForward(cols: Int = 1): String = csi + cols + "C"

  /**
    * Move the cursor back, default 1 column,
    *
    * Stops at the edge of the screen.
    */
  def cursorBack(cols: Int = 1): String = csi + cols + "D"

  /**
    * Move the cursor to the beginning of the line, default 1, down.
    */
  def cursorNextLine(lines: Int = 1): String = csi + lines + "E"

  /**
    * Move the cursor to the beginning of the line, default 1, up.
    */
  def cursorPrevLine(lines: Int = 1): String = csi + lines + "F"

  /**
    * Move the cursor to the column on the current line.
    */
  def cursorHorizAbs(col: Int): String = csi + col + "G"

  /**
    * Move the cursor to the position on screen (1,1 is the top-left).
    */
  def cursorPosition(row: Int, col: Int): String = csi + row + ";" + col + "H"

  /**
    * Scroll page up, default 1, lines.
    */
  def scrollUp(lines: Int = 1): String = csi + lines + "S"

  /**
    * Scroll page down, default 1, lines.
    */
  def scrollDown(lines: Int = 1): String = csi + lines + "T"

  /**
    * The Width of the terminal, as reported by the COLUMNS environment variable.
    *
    * N.B. Not all environment will update this value when the terminal is resized.
    *
    * @return the number of columns in the terminal
    */
  def width: Int = {
    Option(System.getenv("COLUMNS"))
      .map(_.toInt)
      .map(Math.max(_, 10))
      .getOrElse(80)
  }

  def progressBar(
      pos: Double,
      max: Double,
      width: Int
  ): String = {
    val barWidth          = width - 2
    val phases            = subBars.values.size
    val pxWidth           = barWidth * phases
    val ratio             = pos / max
    val pxDone            = pxWidth * ratio
    val fullHeadSize: Int = (pxDone / phases).toInt
    val part              = (pxDone % phases).toInt
    val partial           = if (part != 0) subBars.getOrElse(part, "") else ""
    val head              = ("█" * fullHeadSize) + partial
    val tailSize          = barWidth - head.length
    val tail              = " " * tailSize
    s"[$head$tail]"
  }

}
