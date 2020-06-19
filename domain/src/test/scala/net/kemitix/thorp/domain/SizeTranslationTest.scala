package net.kemitix.thorp.domain

import org.scalatest.funspec.AnyFunSpec

class SizeTranslationTest extends AnyFunSpec {

  describe("sizeInEnglish") {
    describe("when size is less the 1Kb") {
      it("should in in bytes") {
        assertResult("512b")(SizeTranslation.sizeInEnglish(512))
      }
    }
    describe("when size is a less than 10Kb") {
      it("should still be in bytes") {
        assertResult("2000b")(SizeTranslation.sizeInEnglish(2000))
      }
    }
    describe("when size is over 10Kb and less than 10Mb") {
      it("should be in Kb with zero decimal places") {
        assertResult("5468Kb")(SizeTranslation.sizeInEnglish(5599232))
      }
    }
    describe("when size is over 10Mb and less than 10Gb") {
      it("should be in Mb with two decimal place") {
        assertResult("5468.17Mb")(SizeTranslation.sizeInEnglish(5733789833L))
      }
    }
    describe("when size is over 10Gb") {
      it("should be in Gb with three decimal place") {
        assertResult("5468.168Gb")(
          SizeTranslation.sizeInEnglish(5871400857278L))
      }
    }
  }

}
