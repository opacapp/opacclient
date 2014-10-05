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
package de.geeksfactory.opacclient.reminder;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.acra.ACRA;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.apis.OpacApi.OpacErrorException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class ReminderCheckService extends Service {

	boolean notification_on = false;
	public static final String ACTION_SNOOZE = "snooze";

	@Override
	public int onStartCommand(Intent intent, int flags, int startid) {
		if(ACTION_SNOOZE.equals(intent.getAction())) {
			Intent i = new Intent(ReminderCheckService.this,
					ReminderAlarmReceiver.class);
			PendingIntent sender = PendingIntent.getBroadcast(
					ReminderCheckService.this, OpacClient.BROADCAST_REMINDER,
					i, PendingIntent.FLAG_UPDATE_CURRENT);
			AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

			Log.i("ReminderCheckService", "Opac App Service: Quick repeat");
			// Run again in 1 day
			am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
					+ (1000 * 3600 * 24), sender);
		} else {	
			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(ReminderCheckService.this);
			notification_on = sp.getBoolean("notification_service", false);
			long waittime = (1000 * 3600 * 5);
			boolean executed = false;
	
			ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
			if (networkInfo != null) {
				if (sp.getBoolean("notification_service_wifionly", false) == false
						|| networkInfo.getType() == ConnectivityManager.TYPE_WIFI
						|| networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
					executed = true;
					new CheckTask().execute();
				} else {
					waittime = (1000 * 1800);
				}
			} else {
				waittime = (1000 * 1800);
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
	
			if (!executed)
				stopSelf();
		}
		
		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public class CheckTask extends AsyncTask<Object, Object, Object[]> {

		private boolean exception = false;

		@Override
		protected Object[] doInBackground(Object... params) {
			AccountDataSource data = new AccountDataSource(
					ReminderCheckService.this);
			data.open();
			List<Account> accounts = data.getAccountsWithPassword();
			if (accounts.size() == 0)
				return null;

			Log.i("ReminderCheckService",
					"Opac App Service: ReminderCheckService started");

			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(ReminderCheckService.this);

			long now = new Date().getTime();
			long warning = Long.decode(sp.getString("notification_warning",
					"367200000"));
			// long warning = 1000 * 3600 * 24 * 90;
			long expired_new = 0;
			long expired_total = 0;
			long affected_accounts = 0;
			long first = 0;
			long first_affected_account = 0;
			Bundle notified = new Bundle();

			OpacClient app = (OpacClient) getApplication();
			for (Account account : accounts) {
				Log.i("ReminderCheckService",
						"Opac App Service: " + account.toString());
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

					for (Map<String, String> item : res.getLent()) {
						if (item.containsKey(AccountData.KEY_LENT_DOWNLOAD)) {
							// Don't remember people of bringing back ebooks,
							// because ... uhm...
							if (item.get(AccountData.KEY_LENT_DOWNLOAD)
									.startsWith("http"))
								continue;
						}
						if (item.containsKey(AccountData.KEY_LENT_DEADLINE_TIMESTAMP)) {
							long expiring = Long
									.parseLong(item
											.get(AccountData.KEY_LENT_DEADLINE_TIMESTAMP));
							if ((expiring - now) < warning) {
								expired_total++;
								if (!data.notificationIsSent(account.getId(),
										expiring)) {
									expired_new++;
								}
								this_account++;
							}
							notified.putLongArray(account.getId() + ""
									+ expiring, new long[] { account.getId(),
									expiring });
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

				} catch (SocketException e) {
					e.printStackTrace();
					exception = true;
				} catch (InterruptedIOException e) {
					e.printStackTrace();
					exception = true;
				} catch (IOException e) {
					e.printStackTrace();
					exception = true;
				} catch (OpacErrorException e) {
					e.printStackTrace();
				} catch (Exception e) {
					ACRA.getErrorReporter().handleException(e);
				}
			}
			data.close();
			return new Object[] { expired_new, expired_total, notified, first,
					affected_accounts, first_affected_account };
		}

		@Override
		protected void onPostExecute(Object[] result) {
			Intent i = new Intent(ReminderCheckService.this,
					ReminderAlarmReceiver.class);
			PendingIntent sender = PendingIntent.getBroadcast(
					ReminderCheckService.this, OpacClient.BROADCAST_REMINDER,
					i, PendingIntent.FLAG_UPDATE_CURRENT);
			AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

			if (result == null || exception) {
				Log.i("ReminderCheckService", "Opac App Service: Quick repeat");
				// Try again in one hour
				am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
						+ (1000 * 3600), sender);
				if (result == null)
					return;
			}

			long expired_new = (Long) result[0];
			long expired_total = (Long) result[1];
			Bundle notified = (Bundle) result[2];
			long first = (Long) result[3];
			long affected_accounts = (Long) result[4];
			long first_affected_account = (Long) result[5];

			if (expired_new == 0)
				return;

			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(ReminderCheckService.this);
			notification_on = sp.getBoolean("notification_service", false);

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
				
				Intent snoozeIntent = new Intent(ReminderCheckService.this, ReminderCheckService.class);
				snoozeIntent.setAction(ACTION_SNOOZE);
				PendingIntent piSnooze = PendingIntent.getService(ReminderCheckService.this, 0, snoozeIntent, 0);
				nb.addAction(R.drawable.ic_action_alarms, getResources().getText(R.string.snooze), piSnooze);

				Intent notificationIntent = new Intent(
						ReminderCheckService.this,
						((OpacClient) getApplication()).getMainActivity());
				notificationIntent.putExtra("fragment", "account");
				notificationIntent.putExtra("notifications", notified);
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
