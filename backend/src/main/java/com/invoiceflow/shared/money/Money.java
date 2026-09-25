package com.invoiceflow.shared.money;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * An immutable monetary amount in a single currency.
 *
 * <p>Amounts are always held at the currency's minor-unit scale (two decimals for INR). Construction
 * never rounds silently: an amount with more precision than the currency allows is rejected, and
 * callers that need rounding use {@link #rounded(BigDecimal, Currency)} or {@link #multiply(BigDecimal)},
 * which apply {@link #ROUNDING} explicitly. Arithmetic across currencies is rejected.
 */
public record Money(@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount, Currency currency)
        implements Comparable<Money> {

    public static final Currency INR = Currency.getInstance("INR");
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        try {
            amount = amount.setScale(scaleOf(currency), RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "Amount %s has more precision than %s allows".formatted(amount.toPlainString(), currency), e);
        }
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    public static Money inr(String amount) {
        return new Money(new BigDecimal(amount), INR);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    /** Rounds {@code amount} to the currency's scale using {@link #ROUNDING}. */
    public static Money rounded(BigDecimal amount, Currency currency) {
        return new Money(amount.setScale(scaleOf(currency), ROUNDING), currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    /** Multiplies by a quantity or rate, rounding the result to the currency's scale. */
    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor");
        return rounded(amount.multiply(factor), currency);
    }

    public Money negate() {
        return new Money(amount.negate(), currency);
    }

    @JsonIgnore
    public boolean isZero() {
        return amount.signum() == 0;
    }

    @JsonIgnore
    public boolean isPositive() {
        return amount.signum() > 0;
    }

    @JsonIgnore
    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return currency.getCurrencyCode() + " " + amount.toPlainString();
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other");
        if (!currency.equals(other.currency)) {
            throw new CurrencyMismatchException(currency, other.currency);
        }
    }

    private static int scaleOf(Currency currency) {
        return Math.max(currency.getDefaultFractionDigits(), 0);
    }
}
