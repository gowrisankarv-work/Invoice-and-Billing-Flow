package com.invoiceflow.shared.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class MoneyTests {

    @Test
    void normalisesToTheCurrencyScale() {
        assertThat(Money.inr("10").amount()).isEqualTo(new BigDecimal("10.00"));
        assertThat(Money.inr("10.5")).isEqualTo(Money.inr("10.50"));
    }

    @Test
    void rejectsMorePrecisionThanTheCurrencyAllows() {
        assertThatThrownBy(() -> Money.inr("10.005")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void roundsHalfUpOnlyWhenAskedTo() {
        assertThat(Money.rounded(new BigDecimal("10.005"), Money.INR)).isEqualTo(Money.inr("10.01"));
        assertThat(Money.inr("100.00").multiply(new BigDecimal("0.18"))).isEqualTo(Money.inr("18.00"));
        assertThat(Money.inr("99.99").multiply(new BigDecimal("0.09"))).isEqualTo(Money.inr("9.00"));
    }

    @Test
    void addsAndSubtractsExactly() {
        Money total = Money.inr("0.10").plus(Money.inr("0.20"));
        assertThat(total).isEqualTo(Money.inr("0.30"));
        assertThat(total.minus(Money.inr("0.30")).isZero()).isTrue();
        assertThat(Money.inr("1.00").minus(Money.inr("2.00")).isNegative()).isTrue();
    }

    @Test
    void refusesToMixCurrencies() {
        assertThatThrownBy(() -> Money.inr("1.00").plus(Money.of("1.00", "USD")))
                .isInstanceOf(CurrencyMismatchException.class);
        assertThatThrownBy(() -> Money.inr("1.00").compareTo(Money.of("1.00", "USD")))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void serialisesTheAmountAsAStringSoClientsNeverSeeAFloat() {
        JsonMapper mapper = JsonMapper.builder().build();
        String json = mapper.writeValueAsString(Money.inr("1234.50"));
        assertThat(json).isEqualTo("{\"amount\":\"1234.50\",\"currency\":\"INR\"}");
        assertThat(mapper.readValue(json, Money.class)).isEqualTo(Money.inr("1234.50"));
    }
}
