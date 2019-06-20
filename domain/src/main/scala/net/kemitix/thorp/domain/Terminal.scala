package net.kemitix.thorp.domain

import scala.io.AnsiColor._

object Terminal {

  private val esc = "\u001B"
  private val csi = esc + "["

  /**
    * Move the cursor up, default 1 line.
    *
    * Stops at the edge of the screen.
    */
  def cursorUp(lines: Int = 1) = csi + lines + "A"

  /**
    * Move the cursor down, default 1 line.
    *
    * Stops at the edge of the screen.
    */
  def cursorDown(lines: Int = 1) = csi + lines + "B"

  /**
    * Move the cursor forward, default 1 column.
    *
    * Stops at the edge of the screen.
    */
  def cursorForward(cols: Int = 1) = csi + cols + "C"

  /**
    * Move the cursor back, default 1 column,
    *
    * Stops at the edge of the screen.
    */
  def cursorBack(cols: Int = 1) = csi + cols + "D"

  /**
    * Move the cursor to the beginning of the line, default 1, down.
    */
  def cursorNextLine(lines: Int = 1) = csi + lines + "E"
  /**
    * Move the cursor to the beginning of the line, default 1, up.
    */
  def cursorPrevLine(lines: Int = 1) = csi + lines + "F"

  /**
    * Move the cursor to the column on the current line.
    */
  def cursorHorizAbs(col: Int) = csi + col + "G"

  /**
    * Move the cursor to the position on screen (1,1 is the top-left).
    */
  def cursorPosition(row: Int, col: Int) = csi + row + ";" + col + "H"

  /**
    * Clear from cursor to end of screen.
    */
  val eraseToEndOfScreen = csi + "0J"

  /**
    * Clear from cursor to beginning of screen.
    */
  val eraseToStartOfScreen = csi + "1J"

  /**
    * Clear screen and move cursor to top-left.
    *
    * On DOS the "2J" command also moves to 1,1, so we force that behaviour for all.
    */
  val eraseScreen = csi + "2J" + cursorPosition(1, 1)

  /**
    * Clear screen and scrollback buffer then move cursor to top-left.
    *
    * Anticipate that same DOS behaviour here, and to maintain consistency with {@link #eraseScreen}.
    */
  val eraseScreenAndBuffer = csi + "3J"

  /**
    * Clears the terminal line to the right of the cursor.
    *
    * Does not move the cursor.
    */
  val eraseLineForward = csi + "0K"

  /**
    * Clears the terminal line to the left of the cursor.
    *
    * Does not move the cursor.
    */
  val eraseLineBack= csi + "1K"

  /**
    * Clears the whole terminal line.
    *
    * Does not move the cursor.
    */
  val eraseLine = csi + "2K"

  /**
    * Scroll page up, default 1, lines.
    */
  def scrollUp(lines: Int = 1) = csi + lines + "S"

  /**
    * Scroll page down, default 1, lines.
    */
  def scrollDown(lines: Int = 1) = csi + lines + "T"

  /**
    * Saves the cursor position/state.
    */
  val saveCursorPosition = csi + "s"

  /**
    * Restores the cursor position/state.
    */
  val restoreCursorPosition = csi + "u"

  val enableAlternateBuffer = csi + "?1049h"

  val disableAlternateBuffer = csi + "?1049l"

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

  def progressBar(pos: Double, max: Double, width: Int): String = {
    val barWidth = width - 2
    val done = ((pos / max) * barWidth).toInt
    val head = s"$GREEN_B$GREEN#$RESET" * done
    val tail = " " * (barWidth - done)
    s"[$head$tail]"
  }

}
