package de.geeksfactory.opacclient.objects;

import android.graphics.Bitmap;

/**
 * Any object which can hold a cover should implement this interface. This might
 * be a {@link SearchResult} or {@link DetailledItem}.
 */
public interface CoverHolder {
	/**
	 * Get the cover bitmap
	 */
	public Bitmap getCoverBitmap();

	/**
	 * Set the cover as a bitmap
	 */
	public void setCoverBitmap(Bitmap coverBitmap);

	/**
	 * Get the cover URL
	 */
	public String getCover();

	/**
	 * Set the cover by URL
	 */
	public void setCover(String cover);
}
