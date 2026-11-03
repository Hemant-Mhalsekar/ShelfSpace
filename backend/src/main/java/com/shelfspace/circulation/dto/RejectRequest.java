package com.shelfspace.circulation.dto;

import jakarta.validation.constraints.Size;

public record RejectRequest(@Size(max = 255) String reason) {
}
