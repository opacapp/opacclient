package de.geeksfactory.opacclient.reminder;

import java.io.IOException;
import java.net.SocketException;
import java.util.Date;
import java.util.List;

import org.acra.ACRA;
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
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.frontend.AccountActivity;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class ReminderCheckService extends Service {

	boolean notification_on = false;

	@Override
	public int onStartCommand(Intent intent, int flags, int startid) {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(ReminderCheckService.this);
		notification_on = sp.getBoolean("notification_service", false);
		long waittime = (1000 * 3600 * 5);

		if (((OpacClient) getApplication()).isOnline()) {
			new CheckTask().execute();
		} else {
			waittime = (1000 * 3600 * 1);
			stopSelf();
		}

		if (!notification_on) {
			waittime = (1000 * 3600 * 12);
		}

		Intent i = new Intent(ReminderCheckService.this,
				ReminderAlarmReceiver.class);
		PendingIntent sender = PendingIntent.getBroadcast(
				ReminderCheckService.this, OpacClient.BROADCAST_REMINDER, i,
				PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + waittime,
				sender);

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public class CheckTask extends AsyncTask<Object, Object, Long[]> {

		@Override
		protected Long[] doInBackground(Object... params) {
			AccountDataSource data = new AccountDataSource(
					ReminderCheckService.this);
			data.open();
			List<Account> accounts = data.getAccountsWithPassword();
			if (accounts.size() == 0)
				return null;

			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(ReminderCheckService.this);

			long now = new Date().getTime();
			long last = sp.getLong("notification_last", 0);
			long new_last = last;
			long warning = Long.decode(sp.getString("notification_warning",
					"367200000"));
			// long warning = 1000 * 3600 * 24 * 90;
			long expired_new = 0;
			long expired_total = 0;
			long affected_accounts = 0;
			long first = 0;
			long first_affected_account = 0;

			OpacClient app = (OpacClient) getApplication();
			for (Account account : accounts) {
				try {
					Library library = app.getLibrary(account.getLibrary());
					OpacApi api = app.getNewApi(library);

					if (!api.isAccountSupported(library))
						continue;

					AccountData res = api.account(account);

					if (res == null)
						continue;

					data.storeCachedAccountData(account, res);

					int this_account = 0;

					for (ContentValues item : res.getLent()) {
						if (item.containsKey(AccountData.KEY_LENT_DEADLINE_TIMESTAMP)) {
							long expiring = item
									.getAsLong(AccountData.KEY_LENT_DEADLINE_TIMESTAMP);
							if ((expiring - now) < warning) {
								expired_total++;
								if (expiring >= last) {
									expired_new++;
								}
								this_account++;
							}
							if (expiring > new_last) {
								new_last = expiring;
							}
							if (expiring < first || first == 0) {
								first = expiring;
							}
						}
					}

					if (this_account > 0) {
						affected_accounts++;
						if (first_affected_account == 0)
							first_affected_account = account.getId();
					}

				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					ACRA.getErrorReporter().handleException(e);
					e.printStackTrace();
				}
			}
			data.close();
			return new Long[] { expired_new, expired_total, new_last, first,
					affected_accounts, first_affected_account };
		}

		@Override
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
			long affected_accounts = result[4];
			long first_affected_account = result[5];

			if (expired_new == 0)
				return;

			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(ReminderCheckService.this);

			if (notification_on) {
				NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

				NotificationCompat.Builder nb = new NotificationCompat.Builder(
						ReminderCheckService.this);
				nb.setContentInfo(getString(R.string.notif_ticker,
						expired_total));
				nb.setContentTitle(getString(R.string.notif_title));
				nb.setContentText(getString(R.string.notif_ticker,
						expired_total));
				nb.setTicker(getString(R.string.notif_ticker, expired_total));
				nb.setSmallIcon(R.drawable.ic_stat_notification);
				nb.setWhen(first);
				nb.setNumber((int) expired_new);
				nb.setSound(null);

				Intent notificationIntent = new Intent(
						ReminderCheckService.this, AccountActivity.class);
				notificationIntent.putExtra("notif_last", last);
				if (affected_accounts > 1) {
					// If there are notifications for more than one account,
					// account
					// menu should be opened
					notificationIntent.putExtra("showmenu", true);
				}
				notificationIntent.putExtra("account", first_affected_account);
				PendingIntent contentIntent = PendingIntent.getActivity(
						ReminderCheckService.this, 0, notificationIntent, 0);
				nb.setContentIntent(contentIntent);
				nb.setAutoCancel(true);

				Notification notification = nb.build();
				mNotificationManager.notify(OpacClient.NOTIF_ID, notification);
			}

			stopSelf();
		}

	}

}
