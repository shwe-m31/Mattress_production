package com.peps.production.dto;

import java.time.LocalDateTime;

public record RecentProduction(String type, String size, LocalDateTime time) { }
