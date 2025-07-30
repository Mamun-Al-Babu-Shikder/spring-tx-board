package com.sdlc.pro.txboard.domain;

import java.util.List;

public final class FilterGroup implements FilterNode {
    public enum Logic {
        AND, OR
    }

    private final List<FilterNode> filterNodes;
    private final Logic logic;


    private FilterGroup(List<FilterNode> filterNodes, Logic logic) {
        this.filterNodes = filterNodes;
        this.logic = logic;
    }

    public static FilterGroup of(List<FilterNode> filterNodes, Logic logic) {
        return new FilterGroup(filterNodes, logic);
    }

    public List<FilterNode> getFilterNodes() {
        return filterNodes;
    }

    public Logic getLogic() {
        return logic;
    }
}
