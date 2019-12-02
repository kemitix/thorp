package net.kemitix.thorp.domain

import org.scalatest.funspec.AnyFunSpec

class TerminalTest extends AnyFunSpec {

  describe("progressBar") {
    describe("width 10 - 0%") {
      it("should match") {
        val bar = Terminal.progressBar(0d, 10d, 12)
        assertResult("[          ]")(bar)
      }
    }
    describe("width 10 - 10%") {
      it("should match") {
        val bar = Terminal.progressBar(1d, 10d, 12)
        assertResult("[█         ]")(bar)
      }
    }
    describe("width 1 - 8/8th") {
      it("should match") {
        val bar = Terminal.progressBar(8d, 8d, 3)
        assertResult("[█]")(bar)
      }
    }
    describe("width 1 - 7/8th") {
      it("should match") {
        val bar = Terminal.progressBar(7d, 8d, 3)
        assertResult("[▉]")(bar)
      }
    }
    describe("width 1 - 6/8th") {
      it("should match") {
        val bar = Terminal.progressBar(6d, 8d, 3)
        assertResult("[▊]")(bar)
      }
    }
    describe("width 1 - 5/8th") {
      it("should match") {
        val bar = Terminal.progressBar(5d, 8d, 3)
        assertResult("[▋]")(bar)
      }
    }
    describe("width 1 - 4/8th") {
      it("should match") {
        val bar = Terminal.progressBar(4d, 8d, 3)
        assertResult("[▌]")(bar)
      }
    }
    describe("width 1 - 3/8th") {
      it("should match") {
        val bar = Terminal.progressBar(3d, 8d, 3)
        assertResult("[▍]")(bar)
      }
    }
    describe("width 1 - 2/8th") {
      it("should match") {
        val bar = Terminal.progressBar(2d, 8d, 3)
        assertResult("[▎]")(bar)
      }
    }
    describe("width 1 - 1/8th") {
      it("should match") {
        val bar = Terminal.progressBar(1d, 8d, 3)
        assertResult("[▏]")(bar)
      }
    }
  }
}
