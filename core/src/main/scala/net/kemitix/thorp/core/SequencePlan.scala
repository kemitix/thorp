package net.kemitix.thorp.core

import net.kemitix.thorp.core.Action.{DoNothing, ToCopy, ToDelete, ToUpload}

trait SequencePlan {

  def order: Action => Int = {
    case _: DoNothing => 0
    case _: ToCopy    => 1
    case _: ToUpload  => 2
    case _: ToDelete  => 3
  }

}

object SequencePlan extends SequencePlan
