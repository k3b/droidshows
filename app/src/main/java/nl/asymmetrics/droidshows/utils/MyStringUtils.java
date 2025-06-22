package nl.asymmetrics.droidshows.utils;

import android.os.Build;

import org.apache.commons.lang3.StringUtils;

public class MyStringUtils {

    /** Adapted from Apache Commons Text > org.apache.commons.text > CaseUtils.java
     * https://commons.apache.org/proper/commons-text/jacoco/org.apache.commons.text/CaseUtils.java.html
     *
     * Converts all the delimiter separated words in a String into camelCase,
     * that is each word is made up of a title case character and then a series of
     * lowercase characters.
     *
     * <p>The delimiters represent a set of characters understood to separate words.
     * The first non-delimiter character after a delimiter will be capitalized. The first String
     * character may or may not be capitalized and it's determined by the user input for capitalizeFirstLetter
     * variable.</p>
     *
     * <p>A {@code null} input String returns {@code null}.</p>
     *
     * <p>A input string with only delimiter characters returns {@code ""}.</p>
     *
     * Capitalization uses the Unicode title case, normally equivalent to
     * upper case and cannot perform locale-sensitive mappings.
     *
     * <pre>
     * CaseUtils.toCamelCase(null)                = null
     * CaseUtils.toCamelCase(""                   = ""
     * CaseUtils.toCamelCase("To.Camel.Case")     = "ToCamelCase"
     * CaseUtils.toCamelCase("To.CAMel.Case")     = "ToCAMelCase"
     * CaseUtils.toCamelCase("star trek IV")     = "StarTrekIV"
     * CaseUtils.toCamelCase("Kant für Anfänger") = "KantFürAnfänger"
     * </pre>
     *
     * @param str  the String to be converted to camelCase, may be null
     * @return camelCase of String, {@code null} if null String input
     */
    public static String toCamelCase(String str) {
        if (StringUtils.isEmpty(str)) {
            return str;
        }
        // keep upper/lower case
        // str = str.toLowerCase();
        final int strLen = str.length();
        final int[] newCodePoints = new int[strLen];
        int outOffset = 0;
        boolean capitalizeNext = true;
        for (int index = 0; index < strLen;) {
            final int codePoint = str.codePointAt(index);

            if (isNotAlphaNum(codePoint)) {
                // delimiter found: Skip until end of delimiter
                capitalizeNext = outOffset != 0;
                index += Character.charCount(codePoint);
            } else if (capitalizeNext || outOffset == 0) {
                final int titleCaseCodePoint = Character.toTitleCase(codePoint);
                newCodePoints[outOffset++] = titleCaseCodePoint;
                index += Character.charCount(titleCaseCodePoint);
                capitalizeNext = false;
            } else {
                newCodePoints[outOffset++] = codePoint;
                index += Character.charCount(codePoint);
            }
        }

        return new String(newCodePoints, 0, outOffset);
    }

    private static boolean isNotAlphaNum(int codePoint) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return !Character.isAlphabetic(codePoint) && !Character.isDigit(codePoint);
        } else {
            // is not implemented in older android. loosing umlauts in names (äöü....)
            return !(
                        (codePoint >= 'a' && codePoint <= 'z')
                        || (codePoint >= 'A' && codePoint <= 'Z')
                        || (codePoint >= '0' && codePoint <= '9'));
        }
    }


}
