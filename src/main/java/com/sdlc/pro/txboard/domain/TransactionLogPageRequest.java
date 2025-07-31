package com.sdlc.pro.txboard.domain;

public class TransactionLogPageRequest {
    private final int pageNumber;
    private final int pageSize;
    private final Sort sort;
    private final FilterNode filter;

    private TransactionLogPageRequest(int pageNumber, int pageSize, Sort sort, FilterNode filter) {
        if (pageNumber < 0 || pageSize < 1 || filter == null) {
            throw new IllegalArgumentException("Found invalid argument (pageNumber or pageSize or filter)  to initialize TransactionLogPageRequest");
        }
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.sort = sort;
        this.filter = filter;
    }

    public static TransactionLogPageRequest of(int pageNumber, int pageSize, Sort sort, FilterNode filter) {
        return new TransactionLogPageRequest(pageNumber, pageSize, sort, filter);
    }

    public static TransactionLogPageRequest of(int pageNumber, int pageSize, Sort sort) {
        return of(pageNumber, pageSize, sort, FilterNode.UNFILTERED);
    }

    public static TransactionLogPageRequest of(int pageNumber, int pageSize) {
        return of(pageNumber, pageSize, Sort.UNSORTED);
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public Sort getSort() {
        return sort;
    }

    public FilterNode getFilter() {
        return filter;
    }
}
