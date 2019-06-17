package net.kemitix.s3thorp.domain

import java.time.Instant
import java.time.temporal.ChronoField

import net.kemitix.thorp.domain.LastModified
import org.scalatest.FunSpec

class LastModifiedTest extends FunSpec {

  describe("match against a file last modified") {
    val fileLastModified = 1560464353000L
    describe("when time stamps are identical") {
      val lastModified = LastModified(Instant.ofEpochMilli(fileLastModified))
      it("how to get epochMillis from an Instant") {
        val when = lastModified.when
        val epochSecond = when.getEpochSecond
        val millis = when.getLong(ChronoField.MILLI_OF_SECOND)
        val epochMillis = (epochSecond * 1000) + millis
        assertResult(fileLastModified)(epochMillis)
      }
      it("should match") {
        assertResult(true)(lastModified.matches(fileLastModified))
      }
    }
    describe("when time stamps are out by 1 millisecond") {
      val lastModified = LastModified(Instant.ofEpochMilli(fileLastModified + 1))
      it("should not match") {
        assertResult(false)(lastModified.matches(fileLastModified))
      }
    }
  }
}
