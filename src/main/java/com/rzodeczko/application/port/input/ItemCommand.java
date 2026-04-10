package com.rzodeczko.application.port.input;

import java.math.BigDecimal;

public record ItemCommand(String name, int quantity, BigDecimal price) {
}
