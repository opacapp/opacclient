package de.geeksfactory.opacclient.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ISBNTools {
    public static String isbn13to10(String isbn13) {
        isbn13 = cleanupISBN(isbn13);

        if (isbn13.length() != 13) return isbn13;

        String isbn10 = isbn13.substring(3, 12);
        int checksum = 0;
        int weight = 10;

        for (int i = 0; i < isbn10.length(); i++) {
            char c = isbn10.charAt(i);
            checksum += Character.getNumericValue(c) * weight;
            weight--;
        }

        checksum = 11 - (checksum % 11);
        if (checksum == 10) {
            isbn10 += "X";
        } else if (checksum == 11) {
            isbn10 += "0";
        } else {
            isbn10 += checksum;
        }

        return isbn10;
    }

    public static boolean is_valid_isbn10(char[] digits) {
        digits = cleanupISBN(new String(digits)).toCharArray();
        int a = 0;
        for (int i = 0; i < 10; i++) {
            a += i * Integer.parseInt(String.valueOf(digits[i]));
        }
        return a % 11 == Integer.parseInt(String.valueOf(digits[9]));
    }

    private static String cleanupISBN(String isbn) {
        return isbn.replaceAll("[^\\dX]", ""); //Remove all characters that aren't digits or X
    }

    public static String getAmazonCoverURL(String isbn, boolean large) {
        if (large) {
            return "http://images.amazon.com/images/P/" + isbn13to10(isbn) + ".01.L";
        } else {
            return "http://images.amazon.com/images/P/" + isbn13to10(isbn) + ".01.THUMBZZZ";
        }
    }

    /**
     * If possible, changes a cover URL to represent the best sized image for the given display
     * size. If the URL is not supported, the original URL will be returned.
     *
     * @param url    Original cover URL
     * @param width  Desired width
     * @param height Desired height
     * @return Improved URL
     */
    public static String getBestSizeCoverUrl(String url, int width, int height) {
        Pattern vlbRegex = Pattern.compile("^https?://(?:www.)?vlb\\.de/GetBlob\\.aspx\\?" +
                ".*strIsbn=([0-9X]*)");
        Pattern amazonRegex = Pattern.compile(
                "^(https?://(:?images(?:-[^\\.]*)?\\.|[^\\.]*\\.(?:ssl-)?images-)amazon" +
                        ".com/images/[PI]/[^\\.]*\\.?(?:\\d\\d\\.)?)[^.]*\\.jpg");

        Matcher vlbMatcher = vlbRegex.matcher(url);
        Matcher amazonMatcher = amazonRegex.matcher(url);

        int max = Math.max(width, height);

        if (vlbMatcher.find()) {
            // VLB Covers, sizes accoding to documentation at page 42 of
            // http://info.vlb.de/files/vlb_rest_api_anwenderspezifikation.pdf
            if (height <= 90) {
                return "https://vlb.de/GetBlob.aspx?strIsbn=" + vlbMatcher.group(1) + "&size=S";
            } else if (height <= 200) {
                return "https://vlb.de/GetBlob.aspx?strIsbn=" + vlbMatcher.group(1) + "&size=M";
            } else if (width <= 599) {
                return "https://vlb.de/GetBlob.aspx?strIsbn=" + vlbMatcher.group(1) + "&size=L";
            } else { // Original size
                return "https://vlb.de/GetBlob.aspx?strIsbn=" + vlbMatcher.group(1);
            }
        } else if (amazonMatcher.find()) {
            // Amazon Covers, according to http://aaugh.com/imageabuse.html
            String base = amazonMatcher.group(1);
            if (!base.endsWith(".")) base = base + ".";
            return base + "_SL" + max + "_SCLZZZZZZZ_.jpg";
        } else {
            return url;
        }
    }
}
