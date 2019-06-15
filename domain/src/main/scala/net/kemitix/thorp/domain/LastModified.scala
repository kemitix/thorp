package net.kemitix.thorp.domain

import java.time.Instant
import java.time.temporal.ChronoField

final case class LastModified(when: Instant) {

  def matches(epochMilliseconds: Long): Boolean = {
    val millis = when.getLong(ChronoField.MILLI_OF_SECOND)
    (when.getEpochSecond * 1000) + millis == epochMilliseconds
  }

}
