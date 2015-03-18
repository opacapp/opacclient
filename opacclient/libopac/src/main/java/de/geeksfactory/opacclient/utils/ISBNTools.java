package de.geeksfactory.opacclient.utils;

public class ISBNTools {
    public static String isbn13to10(String isbn13) {
        isbn13 = cleanupISBN(isbn13);

        if (isbn13.length() != 13) return isbn13;

        String isbn10 = isbn13.substring(3, 12);
        int checksum = 0;
        int weight = 10;

        for (int i = 0; i < isbn10.length(); i++) {
            char c = isbn10.charAt(i);
            checksum += (int) Character.getNumericValue(c) * weight;
            weight--;
        }

        checksum = 11 - (checksum % 11);
        if (checksum == 10)
            isbn10 += "X";
        else if (checksum == 11)
            isbn10 += "0";
        else
            isbn10 += checksum;

        return isbn10;
    }

    public static boolean is_valid_isbn10(char[] digits) {
        digits = cleanupISBN(digits.toString()).toCharArray();
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
}
