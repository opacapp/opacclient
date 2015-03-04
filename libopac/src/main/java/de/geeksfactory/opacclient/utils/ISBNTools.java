package de.geeksfactory.opacclient.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ISBNTools {
	public static String isbn13to10(String isbn13) {		
		isbn13 = cleanupISBN(isbn13);
		
		if(isbn13.length() != 13) return isbn13;
		
		String isbn10 = isbn13.substring(3, 12);
		int checksum = 0;
		int weight = 10;
		
		for(int i = 0; i < isbn10.length(); i++)
		{
			char c = isbn10.charAt(i); 
			checksum += (int)Character.getNumericValue(c) * weight;
			weight--;
		}
		
		checksum = 11-(checksum % 11);
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
		if(large) {
			return "http://images.amazon.com/images/P/" + isbn13to10(isbn) + ".01.L";
		} else {
			return "http://images.amazon.com/images/P/" + isbn13to10(isbn) + ".01.THUMBZZZ";
		}
	}

    /**
     * Change Amazon Cover URLs (and some others) so that the image roughly matches the given size.
     * @param coverUrl Cover URL
     * @param width desired width, in pixels
     * @param height desired height, in pixels
     * @return Changed URL, or the same URL if it is not supported by this implementation
     */
    public static String bestAmazonCover(String coverUrl, int width, int height) {
        // Amazon Covers, see http://aaugh.com/imageabuse.html
        Pattern amazonRegex = Pattern.compile("(http://[^\\.]*\\.(?:images-)?amazon\\" +
                ".com/images/P/\\w*\\.\\d\\d)\\.[^\\.]*\\.jpg");
        Matcher amazonMatcher = amazonRegex.matcher(coverUrl);
        // VLB Covers, see http://info.vlb.de/files/vlb_rest_api_anwenderspezifikation.pdf, page 43
        Pattern vlbRegex = Pattern.compile("http://vlb\\.de/GetBlob\\.aspx\\?strIsbn=\\d*");
        Matcher vlbMatcher = vlbRegex.matcher(coverUrl);
        if (amazonMatcher.find()) {
            int minimum = Math.min(width, height);
            if (minimum < 75)
                return amazonMatcher.group(1) + ".THUMB.jpg";
            else if (minimum < 100)
                return amazonMatcher.group(1) + ".T.jpg";
            else if (minimum < 150)
                return amazonMatcher.group(1) + ".M.jpg";
            else
                return amazonMatcher.group(1) + ".L.jpg";
        } else if (vlbMatcher.find()) {
            if (height < 90)
                return vlbMatcher.group() + "&size=S";
            else if (height < 200)
                return vlbMatcher.group() + "&size=M";
            else if (width < 600)
                return vlbMatcher.group() + "&size=L";
            else
                return vlbMatcher.group();
        } else {
            return coverUrl;
        }
    }
}
