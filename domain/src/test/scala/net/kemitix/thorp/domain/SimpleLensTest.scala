package net.kemitix.thorp.domain

import org.scalatest.FreeSpec

class SimpleLensTest extends FreeSpec {

  import Subject._

  "lens" - {
    val subject = Subject(0, "s")
    "modify" in {
      val expected = Subject(1, "s")
      val result   = a.modify(_ + 1)(subject)
      assertResult(expected)(result)
    }
    "get" in {
      val expected = "s"
      val result   = b.get(subject)
      assertResult(expected)(result)
    }
    "set" in {
      val expected = Subject(0, "k")
      val result   = b.set("k")(subject)
      assertResult(expected)(result)
    }
  }

  "lens composed" - {
    val wrapper = Wrapper(1, Subject(2, "x"))
    val sbLens  = Wrapper.s ^|-> Subject.b
    "modify" in {
      val expected = Wrapper(1, Subject(2, "X"))
      val result   = sbLens.modify(_.toUpperCase)(wrapper)
      assertResult(expected)(result)
    }
    "get" in {
      val expected = "x"
      val result   = sbLens.get(wrapper)
      assertResult(expected)(result)
    }
    "set" in {
      val expected = Wrapper(1, Subject(2, "k"))
      val result   = sbLens.set("k")(wrapper)
      assertResult(expected)(result)
    }
  }

  case class Subject(a: Int, b: String)
  object Subject {
    val a: SimpleLens[Subject, Int] =
      SimpleLens[Subject, Int](_.a, s => a => s.copy(a = a))
    val b: SimpleLens[Subject, String] =
      SimpleLens[Subject, String](_.b, s => b => s.copy(b = b))
  }
  case class Wrapper(a: Int, s: Subject)
  object Wrapper {
    val a: SimpleLens[Wrapper, Int] =
      SimpleLens[Wrapper, Int](_.a, w => a => w.copy(a = a))
    val s: SimpleLens[Wrapper, Subject] =
      SimpleLens[Wrapper, Subject](_.s, w => s => w.copy(s = s))
  }

}
