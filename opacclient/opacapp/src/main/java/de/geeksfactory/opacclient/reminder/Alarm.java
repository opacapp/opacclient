package de.geeksfactory.opacclient.reminder;

public class Alarm {
    public long id;
    public long[] media;
    public long deadlineTimestamp;
    public long notificationTimestamp;
    public boolean notified;
    public boolean finished;
}
