package com.accionmfb.omnix.savings.target_saving.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TargetSavingsUtils
{
    /**
     * Clean up a string value, replacing the special characters with empty string.
     * @param value
     * @return cleanedValue : String
     */
    public static String clean(String value)
    {
        if(value == null)
            return null;
        return value.replaceAll(",", "")
                .replaceAll(" ", "")
                .replaceAll("\"", "")
                .replace("\\", "");
    }

    public static String generateRequestId() {
        String[] alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("");
        String[] nums = "1234567890".split("");
        String result = "";
        int rand = (int) (Math.random() * 10);
        for (int i = 0; i < 4; i++) {
            int randomIndex = (int) (Math.random() * 26);
            result += alpha[randomIndex];
        }
        for (int j = 0; j < 3; j++) {
            int randomIndex = (int) (Math.random() * 10);
            result += nums[randomIndex];
        }
        result += nums[rand];
        for (int k = 0; k < 4; k++) {
            int randomIndex = (int) (Math.random() * 26);
            result += alpha[randomIndex];
        }

        return result.toUpperCase();
    }

    public static String resolveTargetSavingsInterest(String interest){
        BigDecimal bigDecimal = new BigDecimal(interest);
        bigDecimal = bigDecimal.setScale(2, RoundingMode.CEILING);
        return bigDecimal.toString();
    }
}
