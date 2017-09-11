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
import android.support.annotation.DrawableRes;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.client.HttpClient;

import java.util.List;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.ApacheBaseApi;
import de.geeksfactory.opacclient.apis.BaseApi;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.networking.AndroidHttpClientFactory;
import de.geeksfactory.opacclient.networking.CoverDownloadTask;
import de.geeksfactory.opacclient.objects.CoverHolder;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.objects.SearchResult.MediaType;
import de.geeksfactory.opacclient.utils.BitmapUtils;

public class ResultsAdapter extends ArrayAdapter<SearchResult> {
    private List<SearchResult> objects;

    public ResultsAdapter(Context context, List<SearchResult> objects, OpacApi api) {
        super(context, R.layout.listitem_searchresult, objects);
        this.objects = objects;
    }

    @DrawableRes
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
            case EDOC:
                return R.drawable.type_edoc;
            case EVIDEO:
                return R.drawable.type_evideo;
            case EAUDIO:
            case MP3:
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
            ivType.setImageBitmap(BitmapUtils.bitmapFromBytes(item.getCoverBitmap()));
            ivType.setVisibility(View.VISIBLE);
        } else if (item.getCover() != null) {
            LoadCoverTask lct = new LoadCoverTask(ivType, item);
            lct.execute();
            ivType.setImageResource(R.drawable.ic_loading);
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

    public class LoadCoverTask extends CoverDownloadTask {
        protected ImageView iv;

        public LoadCoverTask(ImageView iv, SearchResult item) {
            super(getContext(), item);
            this.iv = iv;
        }

        @Override
        protected void onPostExecute(CoverHolder result) {
            if (item.getCover() != null && item.getCoverBitmap() != null) {
                iv.setImageBitmap(BitmapUtils.bitmapFromBytes(item.getCoverBitmap()));
                iv.setVisibility(View.VISIBLE);
            } else if (item instanceof SearchResult && ((SearchResult) item).getType() != null
                    && ((SearchResult) item).getType() != MediaType.NONE) {
                iv.setImageResource(getResourceByMediaType(((SearchResult) item).getType()));
                iv.setVisibility(View.VISIBLE);
            } else {
                iv.setVisibility(View.INVISIBLE);
            }
        }
    }
}