package de.geeksfactory.opacclient.reminder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

public class SyncAccountJobCreator implements JobCreator {

    public static final String PREF_SYNC_SERVICE = "notification_service";

    @Nullable
    @Override
    public Job create(@NonNull String tag) {
        switch (tag) {
            case SyncAccountJob.TAG:
            case SyncAccountJob.TAG_RETRY:
            case SyncAccountJob.TAG_IMMEDIATE:
                return new SyncAccountJob();
            default:
                return null;
        }
    }
}
