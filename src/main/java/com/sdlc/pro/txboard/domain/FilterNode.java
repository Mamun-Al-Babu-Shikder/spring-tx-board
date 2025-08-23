package com.sdlc.pro.txboard.domain;

public interface FilterNode {
    FilterNode UNFILTERED = new UnFilter();

    final class UnFilter implements FilterNode {}
}
