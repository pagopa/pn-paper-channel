package it.pagopa.pn.paperchannel.utils.costutils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class that contains static methods related to cost calculus and adjustment.
 * */
public final class CostUtils {
    private CostUtils(){}

    /**
     * This method add the percentage vat to cost value.
     * It returns the cost with VAT if vat is not null, cost otherwise.
     * Percentage formula is: cost + (cost * vat) / 100 mathematically equivalent to cost * (1 + vat/100)
     *
     * @param vat   the percentage vat to add to cost
     * @param cost  the cost to calculate with vat
     *
     * @return      the cost with vat if vat is not null, cost itself otherwise
     * */
    public static Integer getCostWithVat(@Nullable Integer vat, @NotNull Integer cost) {

        if (vat == null) return cost;

        double costWithVat = cost.doubleValue() * (1 + vat.doubleValue() / 100);
        return Math.toIntExact(Math.round(costWithVat));
    }
}