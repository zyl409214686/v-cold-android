package tech.vee.veecoldwallet.Fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import tech.vee.veecoldwallet.Activity.ColdWalletActivity;
import tech.vee.veecoldwallet.Activity.GenerateSeedActivity;
import tech.vee.veecoldwallet.Activity.ImportSeedActivity;
import tech.vee.veecoldwallet.Activity.SetPasswordActivity;
import tech.vee.veecoldwallet.R;
import tech.vee.veecoldwallet.Util.FileUtil;
import tech.vee.veecoldwallet.Util.JsonUtil;
import tech.vee.veecoldwallet.Util.UIUtil;
import tech.vee.veecoldwallet.Util.QRCodeUtil;
import tech.vee.veecoldwallet.Wallet.VEEAccount;
import tech.vee.veecoldwallet.Wallet.VEEWallet;

public class WalletFragment extends Fragment {
    private static final String TAG = "Winston";
    private static final String WALLET_FILE_NAME = "wallet.dat";

    private Activity activity;

    private RecyclerView accountCards;
    private LinearLayout linearLayout;
    private AccountAdapter adapter;

    private FloatingActionMenu menu;
    private FloatingActionButton importSeed;
    private FloatingActionButton generateSeed;
    private FloatingActionButton loadBackup;

    private VEEWallet wallet;
    private String walletFilePath;
    private ArrayList<VEEAccount> accounts;
    private VEEAccount account;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        activity = getActivity();

        accountCards = view.findViewById(R.id.cards_account);
        linearLayout = view.findViewById(R.id.start);

        menu = view.findViewById(R.id.menu);
        importSeed = view.findViewById(R.id.importSeed);
        generateSeed = view.findViewById(R.id.generateSeed);
        loadBackup = view.findViewById(R.id.loadBackup);

        walletFilePath = activity.getFilesDir().getPath() + "/" + WALLET_FILE_NAME;
        wallet = ((ColdWalletActivity) activity).getWallet();

        if (wallet != null) {
            accounts = wallet.generateAccounts();
        }

        // Display wallet if exists, otherwise display start page
        refreshAccounts(accounts);

        menu.setTag("OFF");
        menu.setOnMenuButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("OFF".equals(menu.getTag())) {
                    displayAccounts(false);
                    accountCards.setLayoutFrozen(true);
                    menu.setTag("ON");
                    menu.open(true);
                }
                else {
                    displayAccounts(true);
                    accountCards.setLayoutFrozen(false);
                    menu.setTag("OFF");
                    menu.close(true);
                }
            }
        });

        importSeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String importText = getResources().getString(R.string.import_seed);
                Intent intent;

                if (importSeed.getLabelText().equals(importText)) {
                    menu.setTag("OFF");
                    menu.close(true);
                    intent = new Intent(activity, ImportSeedActivity.class);
                    startActivity(intent);
                }
                else {
                    int i = 100 - accounts.size();
                    if(i > 0) {
                        UIUtil.createAppendAccountsDialog(activity, 100 - accounts.size());
                    }
                    else {
                        Toast.makeText(activity, "Max accounts reached", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        generateSeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String generateText = getResources().getString(R.string.generate_seed);
                Intent intent;

                if (generateSeed.getLabelText().equals(generateText)) {
                    menu.setTag("OFF");
                    menu.close(true);
                    intent = new Intent(activity, GenerateSeedActivity.class);
                    startActivity(intent);
                }
                else {
                    FileUtil.backup(activity, wallet, walletFilePath);
                }
            }
        });

        loadBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileUtil.loadBackup(activity, walletFilePath, WALLET_FILE_NAME);
                Intent intent = new Intent(activity, ColdWalletActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        return view;
    }

    @Override
    public void onPause() {
        activity.unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        // Register receiver for account number
        activity.registerReceiver(receiver, new IntentFilter("SELECT_APPEND_ACCOUNT_NUMBER"));
        if (!menu.isOpened()) {
            displayAccounts(true);
            accountCards.setLayoutFrozen(false);
            menu.setTag("OFF");
        }
        super.onResume();
    }

    public void displayAccounts(Boolean flag){
        if (!flag) { adapter.setFlag(false); }
        else { adapter.setFlag(true); }
        adapter.notifyDataSetChanged();
    }

    public void refreshAccounts(ArrayList<VEEAccount> accounts){
        adapter = new AccountAdapter(accounts);

        if (accounts != null){
            linearLayout.setVisibility(View.GONE);

            UIUtil.setAccountCardsAdapter(activity, accountCards, adapter, accounts);
            displayAccounts(true);

            loadBackup.setVisibility(View.GONE);
            importSeed.setLabelText(getResources().getString(R.string.append_accounts));
            importSeed.setImageDrawable(getResources().getDrawable(R.drawable.ic_append));

            if (FileUtil.sdCardMountedExists()) {
                generateSeed.setLabelText(getResources().getString(R.string.backup_wallet));
                generateSeed.setImageDrawable(getResources().getDrawable(R.drawable.ic_backup));
            }
            else {
                generateSeed.setVisibility(View.GONE);
            }

        }
        else {
            linearLayout.setVisibility(View.VISIBLE);
            if (!FileUtil.sdCardMountedExists() ||
                    !FileUtil.backupExists(activity, WALLET_FILE_NAME)) {
                loadBackup.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Used to hold the contents of an account
     */
    public class AccountHolder extends RecyclerView.ViewHolder {
        public TextView accountName;
        public TextView mutatedAddress;
        public ImageView qrCode;

        public AccountHolder(View v) {
            super(v);
            accountName = (TextView) v.findViewById(R.id.account_name);
            mutatedAddress = (TextView) v.findViewById(R.id.account_mutated_address);
            qrCode = (ImageView) v.findViewById(R.id.account_qr_code);
            qrCode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        }
    }

    /**
     * Used to display all existing accounts in a scrollable recycler view
     */
    public class AccountAdapter extends RecyclerView.Adapter<AccountHolder> {
        private ArrayList<VEEAccount> accounts;
        private boolean flag = true;

        public AccountAdapter(ArrayList<VEEAccount> accounts) {
            this.accounts = accounts;
        }

        public Boolean getFlag() { return flag; }
        public void setFlag(Boolean flag) { this.flag = flag; }

        @Override
        public AccountHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.cardview_account, parent, false);
            AccountHolder holder = new AccountHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(final AccountHolder holder, int position) {
            final VEEAccount account = accounts.get(position);
            holder.accountName.setText(account.getAccountName());
            holder.mutatedAddress.setText(account.getMutatedAddress());

            if(flag) {
                holder.qrCode.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        UIUtil.createExportAddressDialog(activity, account);
                    }
                });
            }
            else {
                holder.qrCode.setOnClickListener(null);
            }
        }

        @Override
        public int getItemCount() {
            return accounts.size();
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == "SELECT_APPEND_ACCOUNT_NUMBER") {
                int accountNum = intent.getIntExtra("ACCOUNT_NUMBER", 1);

                //Toast.makeText(activity, "Seed: " + seed
                //        + "\nAccount Number " + accountNum, Toast.LENGTH_LONG).show();

                wallet.append(accountNum);

                FileUtil.save(wallet.getJson(), walletFilePath);
                Log.d(TAG, wallet.getJson());
                FileUtil.backup(activity, wallet, WALLET_FILE_NAME);
                intent = new Intent(activity, ColdWalletActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        }
    };
}
