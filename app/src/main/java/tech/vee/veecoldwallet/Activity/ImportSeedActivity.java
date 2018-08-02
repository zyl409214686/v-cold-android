package tech.vee.veecoldwallet.Activity;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.HashMap;

import tech.vee.veecoldwallet.R;
import tech.vee.veecoldwallet.Util.JsonUtil;
import tech.vee.veecoldwallet.Util.QRCodeUtil;
import tech.vee.veecoldwallet.Util.UIUtil;
import tech.vee.veecoldwallet.Wallet.VEEAccount;
import tech.vee.veecoldwallet.Wallet.VEETransaction;
import tech.vee.veecoldwallet.Wallet.VEEWallet;

public class ImportSeedActivity extends AppCompatActivity {
    private static final String TAG = "Winston";
    private static final String WALLET_FILE_NAME = "wallet.dat";

    private ActionBar actionBar;
    private ImportSeedActivity activity;
    private TextView paste;
    private EditText input;
    private ImageView scan;
    private Button confirm;

    private String qrContents;
    private String pasteContents;
    private String walletFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_seed);

        Toolbar toolbar = (Toolbar) findViewById(R.id.custom_toolbar);
        setSupportActionBar(toolbar);

        Drawable icon = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_import);
        icon.mutate();
        icon.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_IN);

        walletFilePath = getFilesDir().getPath() + "/" + WALLET_FILE_NAME;

        activity = this;
        actionBar = getSupportActionBar();
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setLogo(icon);
        actionBar.setTitle(R.string.title_import_seed);

        paste = findViewById(R.id.import_seed_paste);
        input = findViewById(R.id.import_seed_input);
        scan = findViewById(R.id.import_seed_scan);
        confirm = findViewById(R.id.import_seed_confirm);

        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pasteContents != null) { input.setText(pasteContents); }
            }
        });

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QRCodeUtil.scan(activity);
            }
        });

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String seed = input.getText().toString();
                if (VEEAccount.validateSeedPhrase(activity, seed)) {
                    UIUtil.createAccountNumberDialog(activity, seed);
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        qrContents = result.getContents();

        if(result != null) {
            switch (QRCodeUtil.processQrContents(qrContents)) {
                case 0:
                    Toast.makeText(activity, "Cancelled", Toast.LENGTH_LONG).show();
                    break;

                case 2:
                    String seed = QRCodeUtil.parseSeed(qrContents);
                    if (VEEAccount.validateSeedPhrase(activity, seed)) {
                        UIUtil.createAccountNumberDialog(activity, seed);
                    }
                    break;

                case 3:
                    Toast.makeText(activity, "Incorrect QR code format", Toast.LENGTH_LONG).show();
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == "SELECT_ACCOUNT_NUMBER") {
                int accountNum = intent.getIntExtra("ACCOUNT_NUMBER", 1);
                String seed = intent.getStringExtra("SEED");

                //Toast.makeText(activity, "Seed: " + seed
                //        + "\nAccount Number " + accountNum, Toast.LENGTH_LONG).show();

                VEEWallet wallet = VEEWallet.recover(seed, accountNum);
                JsonUtil.save(wallet.getJson(), walletFilePath);
                Log.d(TAG, wallet.getJson());
                intent = new Intent(activity, ColdWalletActivity.class);
                startActivity(intent);
            }
        }
    };

    @Override
    protected void onPause() {
        unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Retrieve paste content from clipboard if possible
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        if (!(clipboard.hasPrimaryClip())) {
            paste.setEnabled(false);
            paste.setTextColor(getResources().getColor(R.color.textLight));
        }
        else if (!(clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))) {
            paste.setEnabled(false);
            paste.setTextColor(getResources().getColor(R.color.textLight));
        } else {
            paste.setEnabled(true);
            paste.setTextColor(getResources().getColor(R.color.orange));
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            pasteContents = item.getText().toString();
        }

        // Register receiver for account number
        registerReceiver(receiver, new IntentFilter("SELECT_ACCOUNT_NUMBER"));
    }
}
