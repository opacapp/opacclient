package de.geeksfactory.opacclient.objects;

import android.graphics.Bitmap;

/*
 * Any object which can hold a cover
 */
public interface CoverHolder {
	public Bitmap getCoverBitmap();

	public void setCoverBitmap(Bitmap coverBitmap);

	public String getCover();

	public void setCover(String cover);
}
