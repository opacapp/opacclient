package de.geeksfactory.opacclient.reminder;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public class Alarm {
    public long id;
    public long[] media;
    public LocalDate deadline;
    public DateTime notificationTime;
    public boolean notified;
    public boolean finished;
}
