package de.geeksfactory.opacclient.frontend;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.utils.Utils;

public class DrawerAccountsAdapter extends RecyclerView.Adapter<DrawerAccountsAdapter.ViewHolder> {
    private List<Account> accounts;
    private List<Account> accountsWithoutCurrent;
    private Context context;
    private Map<Account, Integer> expiring;
    private Account currentAccount;
    private Listener listener;

    public DrawerAccountsAdapter(Context context, List<Account> accounts, Account currentAccount) {
        this.accounts = accounts;
        this.context = context;
        this.expiring = new HashMap<>();
        this.currentAccount = currentAccount;
        this.accountsWithoutCurrent = new ArrayList<>();

        SharedPreferences sp =
                PreferenceManager.getDefaultSharedPreferences(context);
        long tolerance = Long.decode(sp.getString("notification_warning", "367200000"));

        AccountDataSource adata = new AccountDataSource(context);
        adata.open();
        for (Account account : accounts) {
            expiring.put(account, adata.getExpiring(account, tolerance));
            if (account.getId() != currentAccount.getId()) {
                accountsWithoutCurrent.add(account);
            }
        }
        adata.close();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                                  .inflate(R.layout.navigation_drawer_item_account, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Account account = accountsWithoutCurrent.get(position);
        holder.setData(account, expiring.get(account));
    }

    @Override
    public int getItemCount() {
        return accountsWithoutCurrent.size();
    }

    public void setCurrentAccount(Account account) {
        if (currentAccount == null || account == currentAccount) return;

        this.accountsWithoutCurrent.add(accounts.indexOf(currentAccount), currentAccount);
        notifyItemInserted(accounts.indexOf(currentAccount));

        this.currentAccount = account;
        this.accountsWithoutCurrent.remove(currentAccount);
        notifyItemRemoved(accounts.indexOf(currentAccount));
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView title;
        private TextView subtitle;
        private TextView warning;
        private View view;
        private Account account;

        public ViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.account_title);
            subtitle = (TextView) itemView.findViewById(R.id.account_subtitle);
            warning = (TextView) itemView.findViewById(R.id.account_warning);
            view = itemView;
        }

        public void setData(Account account, int expiring) {
            this.account = account;
            title.setText(Utils.getAccountTitle(account, context));
            subtitle.setText(Utils.getAccountSubtitle(account, context));
            if (expiring > 0) {
                warning.setText(String.valueOf(expiring));
                warning.setVisibility(View.VISIBLE);
            } else {
                warning.setVisibility(View.INVISIBLE);
            }
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) listener.onAccountClicked(ViewHolder.this.account);
                }
            });
        }
    }

    public interface Listener {
        void onAccountClicked(Account account);
    }
}
