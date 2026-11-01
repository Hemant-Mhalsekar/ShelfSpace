package com.shelfspace.fine.dto;

import java.math.BigDecimal;
import java.util.List;

public record FineSummaryResponse(BigDecimal totalUnpaid, List<FineResponse> fines) {
}
