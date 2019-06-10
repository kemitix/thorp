package net.kemitix.s3thorp.domain

object Terminal {

  /**
    * Clears the whole terminal line.
    */
  val clearLine = "\u001B[2K\r"
  /**
    * Moves the cursor up one line and back to the start of the line.
    */
  val returnToPreviousLine = "\u001B[1A\r"

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

}
