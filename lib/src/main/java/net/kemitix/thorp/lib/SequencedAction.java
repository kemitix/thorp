package net.kemitix.thorp.lib;

import net.kemitix.mon.TypeAlias;
import net.kemitix.thorp.domain.Action;
import net.kemitix.thorp.domain.Tuple;

public class SequencedAction extends TypeAlias<Tuple<Action, Integer>> {
    private SequencedAction(Tuple<Action, Integer> value) {
        super(value);
    }
    public static SequencedAction create(Action action, Integer index) {
        return new SequencedAction(Tuple.create(action, index));
    }
    public Action action() {
        return getValue().a;
    }
    public int index() {
        return getValue().b;
    }
}
