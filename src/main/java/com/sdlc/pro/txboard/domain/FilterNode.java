package com.sdlc.pro.txboard.domain;

public sealed interface FilterNode permits Filter, FilterGroup, FilterNode.UnFilter {
    FilterNode UNFILTERED = new UnFilter();

    final class UnFilter implements FilterNode {}
}
