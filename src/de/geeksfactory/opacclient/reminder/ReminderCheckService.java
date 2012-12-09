package de.geeksfactory.opacclient.reminder;

import java.io.IOException;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import de.geeksfactory.opacclient.AccountUnsupportedException;
import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.frontend.AccountActivity;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class ReminderCheckService extends Service {

	@Override
	public int onStartCommand(Intent intent, int flags, int startid) {

		if (((OpacClient) getApplication()).isOnline()) {
			new CheckTask().execute();
		} else {
			Intent i = new Intent(ReminderCheckService.this,
					ReminderAlarmReceiver.class);
			PendingIntent sender = PendingIntent.getBroadcast(
					ReminderCheckService.this, OpacClient.BROADCAST_REMINDER,
					i, PendingIntent.FLAG_UPDATE_CURRENT);
			AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
			am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
					+ (1000 * 3600 * 1), sender);
			stopSelf();
		}

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public class CheckTask extends AsyncTask<Object, Object, Long[]> {

		@Override
		protected Long[] doInBackground(Object... params) {
			// TODO: WIEDER EINBAUEN!
			AccountDataSource data = new AccountDataSource(
					ReminderCheckService.this);
			data.open();
			List<Account> accounts = data.getAccountsWithPassword();
			data.close();
			if (accounts.size() == 0)
				return null;

			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(ReminderCheckService.this);

			long now = new Date().getTime();
			long last = sp.getLong("notification_last", 0);
//			long warning = Long.decode(sp.getString("notification_warning",
//					"367200000"));
			long warning = 1000*3600*24*90;
			long expired_new = 0;
			long expired_total = 0;
			long first = 0;

			OpacClient app = (OpacClient) getApplication();
			for (Account account : accounts) {
				try {
					OpacApi api = app.getIndependentApi(app.getLibrary(account
							.getBib()));
					SimpleDateFormat sdf = api.getDateFormat();
					AccountData res = api.account(account);

					for (ContentValues item : res.getLent()) {
						if (item.containsKey("frist")) {
							Date expiring = sdf
									.parse(item.getAsString("frist"));
							if ((expiring.getTime() - now) < warning) {
								expired_total++;
								if (expiring.getTime() >= last) {
									expired_new++;
								}
							}
							if (expiring.getTime() > last) {
								last = expiring.getTime();
							}
							if (expiring.getTime() < first || first == 0) {
								first = expiring.getTime();
							}
						}
					}

				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SocketException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NotReachableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (AccountUnsupportedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return new Long[] { expired_new, expired_total, last, first };
		}

		protected void onPostExecute(Long[] result) {
			Intent i = new Intent(ReminderCheckService.this,
					ReminderAlarmReceiver.class);
			PendingIntent sender = PendingIntent.getBroadcast(
					ReminderCheckService.this, OpacClient.BROADCAST_REMINDER,
					i, PendingIntent.FLAG_UPDATE_CURRENT);
			AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

			if (result == null) {
				// Try again in one hour
				am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
						+ (1000 * 3600), sender);
				return;
			}

			long expired_new = result[0];
			long expired_total = result[1];
			long last = result[2];
			long first = result[3];
			if (expired_new == 0)
				return;

			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(ReminderCheckService.this);
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			int icon = android.R.drawable.stat_notify_error;

			NotificationCompat.Builder nb = new NotificationCompat.Builder(
					ReminderCheckService.this);
			nb.setContentInfo(getString(R.string.notif_ticker, expired_total));
			nb.setContentTitle(getString(R.string.notif_title));
			nb.setContentText(getString(R.string.notif_ticker, expired_total));
			nb.setTicker(getString(R.string.notif_ticker, expired_total));
			nb.setSmallIcon(R.drawable.ic_stat_notification);
			nb.setWhen(first);
			nb.setNumber((int) expired_new);

			if (!sp.getString("notification_sound", "").equals("")) {
				nb.setSound(Uri.parse(sp.getString("notification_sound", "")));
			} else {
				nb.setSound(null);
			}

			Intent notificationIntent = new Intent(ReminderCheckService.this,
					AccountActivity.class);
			notificationIntent.putExtra("notif_last", last);
			PendingIntent contentIntent = PendingIntent.getActivity(
					ReminderCheckService.this, 0, notificationIntent, 0);
			nb.setContentIntent(contentIntent);
			nb.setAutoCancel(true);

			Notification notification = nb.build();
			mNotificationManager.notify(OpacClient.NOTIF_ID, notification);

			am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
					+ (1000 * 3600 * 5), sender);
			stopSelf();
		}

	}

}
