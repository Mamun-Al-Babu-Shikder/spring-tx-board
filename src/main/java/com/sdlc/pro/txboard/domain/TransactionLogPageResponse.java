package com.sdlc.pro.txboard.domain;

import com.sdlc.pro.txboard.model.TransactionLog;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class TransactionLogPageResponse implements Serializable {
    private final List<TransactionLog> content;
    private final TransactionLogPageRequest pageRequest;
    private final long totalElements;

    public TransactionLogPageResponse(List<TransactionLog> content, TransactionLogPageRequest pageRequest, long totalElements) {
        Objects.requireNonNull(content, "Content must not be null");
        Objects.requireNonNull(pageRequest, "PageRequest must not be null");

        if (content.size() > totalElements) {
            throw new IllegalArgumentException("The value of total must be greater than content size");
        }

        this.content = content;
        this.pageRequest = pageRequest;
        this.totalElements = totalElements;
    }


    public List<TransactionLog> getContent() {
        return content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getPage() {
        return pageRequest.getPageNumber();
    }

    public int getSize() {
        return pageRequest.getPageSize();
    }

    public int getTotalPages() {
        return getSize() == 0 ? 1 : (int) Math.ceil((double) totalElements / (double) getSize());
    }

    public boolean hasPrevious() {
        return getPage() > 0;
    }

    public boolean hasNext() {
        return getPage() + 1 < getTotalPages();
    }

    public boolean isFirst() {
        return !hasPrevious();
    }

    public boolean isLast() {
        return !hasNext();
    }
}
