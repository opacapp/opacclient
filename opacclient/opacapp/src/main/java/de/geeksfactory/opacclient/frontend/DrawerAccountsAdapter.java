package de.geeksfactory.opacclient.frontend;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.storage.AccountDataSource;
import de.geeksfactory.opacclient.utils.Utils;

public class DrawerAccountsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Account> accounts;
    private List<Account> accountsWithoutCurrent;
    protected Context context;
    private Map<Account, Integer> expiring;
    private Account currentAccount;
    protected Listener listener;

    protected static final int TYPE_ACCOUNT = 0;
    protected static final int TYPE_SEPARATOR = 1;
    protected static final int TYPE_FOOTER = 2;

    protected static final int FOOTER_COUNT = 2;

    public DrawerAccountsAdapter(Context context, List<Account> accounts, Account currentAccount) {
        this.accounts = accounts;
        this.context = context;
        this.expiring = new HashMap<>();
        this.currentAccount = currentAccount;
        this.accountsWithoutCurrent = new ArrayList<>();

        SharedPreferences sp =
                PreferenceManager.getDefaultSharedPreferences(context);
        int tolerance = Integer.parseInt(sp.getString("notification_warning", "3"));

        AccountDataSource adata = new AccountDataSource(context);
        for (Account account : accounts) {
            expiring.put(account, adata.getExpiring(account, tolerance));
            if (account.getId() != currentAccount.getId()) {
                accountsWithoutCurrent.add(account);
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case TYPE_ACCOUNT:
                view = LayoutInflater.from(context).inflate(
                        R.layout.navigation_drawer_item_account, parent, false);
                return new AccountViewHolder(view);
            case TYPE_SEPARATOR:
                view = LayoutInflater.from(context).inflate(
                        R.layout.navigation_drawer_item_separator, parent, false);
                return new SeparatorViewHolder(view);
            case TYPE_FOOTER:
                view = LayoutInflater.from(context).inflate(
                        R.layout.navigation_drawer_item_footer, parent, false);
                return new FooterViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AccountViewHolder) {
            Account account = accountsWithoutCurrent.get(position);
            ((AccountViewHolder) holder).setData(account, expiring.get(account));
        } else if (holder instanceof FooterViewHolder) {
            FooterViewHolder footer = (FooterViewHolder) holder;
            if (position ==
                    accountsWithoutCurrent.size() + (accountsWithoutCurrent.size() > 0 ? 1 : 0)) {
                footer.setTitle(R.string.account_add);
                footer.setIcon(R.drawable.ic_add_24dp);
                footer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (listener != null) listener.onAddAccountClicked();
                    }
                });
            } else if (position ==
                    accountsWithoutCurrent.size() + (accountsWithoutCurrent.size() > 0 ? 2 : 1)) {
                footer.setTitle(R.string.accounts);
                footer.setIcon(R.drawable.ic_settings_24dp);
                footer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (listener != null) listener.onManageAccountsClicked();
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return accountsWithoutCurrent.size() + (accountsWithoutCurrent.size() > 0 ? 1 : 0) +
                FOOTER_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < accountsWithoutCurrent.size()) {
            return TYPE_ACCOUNT;
        } else if (position == accountsWithoutCurrent.size() && accountsWithoutCurrent.size() > 0) {
            return TYPE_SEPARATOR;
        } else {
            return TYPE_FOOTER;
        }
    }


    public void setCurrentAccount(Account currentAccount) {
        if (this.currentAccount == null || currentAccount == this.currentAccount) return;

        this.currentAccount = currentAccount;
        accountsWithoutCurrent.clear();
        for (Account account : accounts) {
            if (account.getId() != this.currentAccount.getId()) {
                accountsWithoutCurrent.add(account);
            }
        }

        notifyDataSetChanged();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public class AccountViewHolder extends RecyclerView.ViewHolder {
        protected TextView title;
        protected TextView subtitle;
        protected TextView warning;
        protected View view;
        protected  Account account;

        public AccountViewHolder(View itemView) {
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
                    if (listener != null) listener.onAccountClicked(AccountViewHolder.this.account);
                }
            });
        }
    }

    public interface Listener {
        void onAccountClicked(Account account);

        void onAddAccountClicked();

        void onManageAccountsClicked();
    }

    protected class SeparatorViewHolder extends RecyclerView.ViewHolder {
        public SeparatorViewHolder(View view) {
            super(view);
        }
    }

    protected class FooterViewHolder extends RecyclerView.ViewHolder {
        private TextView title;
        private ImageView icon;
        private View view;

        public FooterViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.footer_title);
            icon = (ImageView) itemView.findViewById(R.id.footer_icon);
            view = itemView;
        }

        public void setTitle(int string) {
            title.setText(string);
        }

        public void setIcon(int id) {
            Drawable drawable = DrawableCompat.wrap(VectorDrawableCompat
                    .create(context.getResources(), id, context.getTheme()));
            DrawableCompat.setTint(drawable, Color.argb(138, 0, 0, 0));
            icon.setImageDrawable(drawable);
        }

        public void setOnClickListener(View.OnClickListener listener) {
            view.setOnClickListener(listener);
        }
    }
}
