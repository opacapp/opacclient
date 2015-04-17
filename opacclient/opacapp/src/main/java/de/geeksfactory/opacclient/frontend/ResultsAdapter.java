/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software 
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.frontend;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.networking.HTTPClient;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.objects.SearchResult.MediaType;
import de.geeksfactory.opacclient.utils.Base64;
import de.geeksfactory.opacclient.utils.ISBNTools;

public class ResultsAdapter extends ArrayAdapter<SearchResult> {
    private List<SearchResult> objects;

    protected static HashSet<String> rejectImages = new HashSet<>();

    static {
        rejectImages.add(
                "R0lGODlhOwBLAIAAALy8vf///yH5BAEAAAEALAAAAAA7AEsAAAL/jI+py+0Po5y0" +
                        "2ouz3rz7D2rASJbmiYYGyralGrhyqrbTW4+rGeEhmeA5fCCg4sQgfowLFkLpYTaE" +
                        "O10OIJFCO9KhtYq9Zr+xbpTsDYNh5iR5y2k33/JNPUhHn9UP7T3zd+Cnx0U4xwdn" +
                        "Z3iUx7e0iIcYeDFZJgkJiCnYyKZZ9VRZUTnouDd2WVqYegjqaTHKebUa6SSLKdOJ" +
                        "5GYDY0nVWtvrqxSa61PciytMwbss+uvMjBxNXW19jZ29bHVJu/MNvqmTCK4WhvbF" +
                        "bS65EnPqXiaIJ26Eg/6HVW8+327fHg9kVpBw5xylc6eu3jeBTwh28bewIJh807RZ" +
                        "vIgxo8aNRxw7ZlNXbt04RvT+lXQjL57KciT/nRuY5iW8YzJPQjx5xKVCeCoNurTE" +
                        "0+QukBNZAsu3ECbKnhIBBnwaMWFBVx6rWr2KdUIBADs=");
    }

    public ResultsAdapter(Context context, List<SearchResult> objects) {
        super(context, R.layout.listitem_searchresult, objects);
        this.objects = objects;
    }

    public static int getResourceByMediaType(MediaType type) {
        switch (type) {
            case NONE:
                return 0;
            case BOOK:
                return R.drawable.type_book;
            case CD:
                return R.drawable.type_cd;
            case CD_SOFTWARE:
                return R.drawable.type_cd_software;
            case CD_MUSIC:
                return R.drawable.type_cd_music;
            case BLURAY:
                return R.drawable.type_bluray;
            case DVD:
                return R.drawable.type_dvd;
            case MOVIE:
                return R.drawable.type_movie;
            case AUDIOBOOK:
                return R.drawable.type_audiobook;
            case PACKAGE:
                return R.drawable.type_package;
            case GAME_CONSOLE:
            case GAME_CONSOLE_NINTENDO:
            case GAME_CONSOLE_PLAYSTATION:
            case GAME_CONSOLE_WII:
            case GAME_CONSOLE_XBOX:
                return R.drawable.type_game_console;
            case EBOOK:
                return R.drawable.type_ebook;
            case SCORE_MUSIC:
                return R.drawable.type_score_music;
            case PACKAGE_BOOKS:
                return R.drawable.type_package_books;
            case UNKNOWN:
                return R.drawable.type_unknown;
            case MAGAZINE:
            case NEWSPAPER:
                return R.drawable.type_newspaper;
            case BOARDGAME:
                return R.drawable.type_boardgame;
            case SCHOOL_VERSION:
                return R.drawable.type_school_version;
            case AUDIO_CASSETTE:
                return R.drawable.type_audio_cassette;
            case URL:
                return R.drawable.type_url;
            case MP3:
                return R.drawable.type_mp3;
            case EDOC:
                return R.drawable.type_edoc;
            case EVIDEO:
                return R.drawable.type_evideo;
            case EAUDIO:
                return R.drawable.type_eaudio;
            case ART:
                return R.drawable.type_art;
            case MAP:
                return R.drawable.type_map;
            case LP_RECORD:
                return R.drawable.type_lp_record;
        }

        return R.drawable.type_unknown;

    }

    @Override
    public View getView(int position, View contentView, ViewGroup viewGroup) {
        View view;

        // position always 0-7
        if (objects.get(position) == null) {
            LayoutInflater layoutInflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.listitem_searchresult,
                    viewGroup, false);
            return view;
        }

        SearchResult item = objects.get(position);

        if (contentView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.listitem_searchresult,
                    viewGroup, false);
        } else {
            view = contentView;
        }

        TextView tv = (TextView) view.findViewById(R.id.tvResult);
        tv.setText(Html.fromHtml(item.getInnerhtml()));

        ImageView ivType = (ImageView) view.findViewById(R.id.ivType);

        if (item.getCoverBitmap() != null) {
            ivType.setImageBitmap(item.getCoverBitmap());
            ivType.setVisibility(View.VISIBLE);
        } else if (item.getCover() != null) {
            LoadCoverTask lct = new LoadCoverTask(ivType, item);
            lct.execute();
            ivType.setImageResource(R.drawable.cover_loading);
            ivType.setVisibility(View.VISIBLE);
        } else if (item.getType() != null && item.getType() != MediaType.NONE) {
            ivType.setImageResource(getResourceByMediaType(item.getType()));
            ivType.setVisibility(View.VISIBLE);
        } else {
            ivType.setVisibility(View.INVISIBLE);
        }
        ImageView ivStatus = (ImageView) view.findViewById(R.id.ivStatus);

        if (item.getStatus() != null) {
            ivStatus.setVisibility(View.VISIBLE);
            switch (item.getStatus()) {
                case GREEN:
                    ivStatus.setImageResource(R.drawable.status_light_green);
                    break;
                case RED:
                    ivStatus.setImageResource(R.drawable.status_light_red);
                    break;
                case YELLOW:
                    ivStatus.setImageResource(R.drawable.status_light_yellow);
                    break;
                case UNKNOWN:
                    ivStatus.setVisibility(View.INVISIBLE);
                    break;
            }
        } else {
            ivStatus.setVisibility(View.GONE);
        }

        return view;
    }

    public class LoadCoverTask extends AsyncTask<Void, Integer, SearchResult> {
        protected SearchResult item;
        protected ImageView iv;

        public LoadCoverTask(ImageView iv, SearchResult item) {
            this.iv = iv;
            this.item = item;
        }

        @Override
        protected SearchResult doInBackground(Void... voids) {
            URL newurl;
            if (item.getCover() != null && item.getCoverBitmap() == null) {
                try {
                    float density = getContext().getResources().getDisplayMetrics().density;

                    HttpClient http_client = HTTPClient.getNewHttpClient(false);
                    HttpGet httpget = new HttpGet(ISBNTools.getBestSizeCoverUrl(item.getCover(),
                            (int) (56 * density), (int) (56 * density)));
                    HttpResponse response;

                    try {
                        response = http_client.execute(httpget);

                        if (response.getStatusLine().getStatusCode() >= 400) {
                            item.setCover(null);
                        }
                        HttpEntity entity = response.getEntity();
                        byte[] bytes = EntityUtils.toByteArray(entity);
                        if (rejectImages.contains(Base64.encodeBytes(bytes))) {
                            // OPACs like VuFind have a 'cover proxy' that returns a simple GIF with
                            // the text 'no image available' if no cover was found. We don't want to
                            // display this image but the media type,
                            // so we detect it. We do this here
                            // instead of in the API implementation because only this way it can be
                            // done asynchronously.
                            item.setCover(null);
                        } else {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0,
                                    bytes.length);
                            if (bitmap.getHeight() > 1 && bitmap.getWidth() > 1) {
                                item.setCoverBitmap(bitmap);
                            } else {
                                // When images embedded from Amazon aren't available, a
                                // 1x1
                                // pixel image is returned (iOPAC)
                                item.setCover(null);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return item;
        }

        @Override
        protected void onPostExecute(SearchResult result) {
            if (item.getCover() != null && item.getCoverBitmap() != null) {
                iv.setImageBitmap(item.getCoverBitmap());
                iv.setVisibility(View.VISIBLE);
            } else if (item.getType() != null
                    && item.getType() != MediaType.NONE) {
                iv.setImageResource(getResourceByMediaType(item.getType()));
                iv.setVisibility(View.VISIBLE);
            } else {
                iv.setVisibility(View.INVISIBLE);
            }
        }
    }
}