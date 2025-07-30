package com.sdlc.pro.txboard.domain;

import com.sdlc.pro.txboard.model.TransactionLog;

import java.io.Serializable;
import java.util.List;

public record TransactionLogPageResponse(List<TransactionLog> content, long totalElements, int totalPages, int number,
                                         int size, boolean first, boolean last) implements Serializable {

}
