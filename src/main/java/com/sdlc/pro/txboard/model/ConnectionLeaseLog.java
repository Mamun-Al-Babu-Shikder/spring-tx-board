package com.sdlc.pro.txboard.model;

import java.time.Instant;

public record ConnectionLeaseLog(Instant acquiredTime, Instant releasedTime) {
}
