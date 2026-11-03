package coop;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Formato {
    private Formato() {}

    static String m(BigDecimal v) {
        BigDecimal x = v.setScale(2, RoundingMode.HALF_UP);
        return (x.signum() < 0 ? "-$" : "$") + x.abs().toPlainString();
    }

    static String linea(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append('=');
        return sb.toString();
    }
}
