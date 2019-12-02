package net.kemitix.thorp.domain

import org.scalatest.freespec.AnyFreeSpec

class SimpleLensTest extends AnyFreeSpec {

  "lens" - {
    val subject = Subject(0, "s")
    "modify" in {
      val expected = Subject(1, "s")
      val result   = Subject.anIntLens.modify(_ + 1)(subject)
      assertResult(expected)(result)
    }
    "get" in {
      val expected = "s"
      val result   = Subject.aStringLens.get(subject)
      assertResult(expected)(result)
    }
    "set" in {
      val expected = Subject(0, "k")
      val result   = Subject.aStringLens.set("k")(subject)
      assertResult(expected)(result)
    }
  }

  "lens composed" - {
    val wrapper           = Wrapper(1, Subject(2, "x"))
    val subjectStringLens = Wrapper.aSubjectLens ^|-> Subject.aStringLens
    "modify" in {
      val expected = Wrapper(1, Subject(2, "X"))
      val result   = subjectStringLens.modify(_.toUpperCase)(wrapper)
      assertResult(expected)(result)
    }
    "get" in {
      val expected = "x"
      val result   = subjectStringLens.get(wrapper)
      assertResult(expected)(result)
    }
    "set" in {
      val expected = Wrapper(1, Subject(2, "k"))
      val result   = subjectStringLens.set("k")(wrapper)
      assertResult(expected)(result)
    }
  }

  case class Subject(anInt: Int, aString: String)
  object Subject {
    val anIntLens: SimpleLens[Subject, Int] =
      SimpleLens[Subject, Int](_.anInt, subject => i => subject.copy(anInt = i))
    val aStringLens: SimpleLens[Subject, String] =
      SimpleLens[Subject, String](_.aString,
                                  subject => str => subject.copy(aString = str))
  }
  case class Wrapper(anInt: Int, aSubject: Subject)
  object Wrapper {
    val anIntLens: SimpleLens[Wrapper, Int] =
      SimpleLens[Wrapper, Int](_.anInt, wrapper => i => wrapper.copy(anInt = i))
    val aSubjectLens: SimpleLens[Wrapper, Subject] =
      SimpleLens[Wrapper, Subject](
        _.aSubject,
        wrapper => subject => wrapper.copy(aSubject = subject))
  }

}
