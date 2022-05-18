package mts.build.firebasedatabase;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Random;

import mts.build.firebasedatabase.R;

public class Main extends AppCompatActivity {

    private TextInputEditText key, conf;
    private Button btn_export, btn_read;
    private TextView results;

    private String getInstanceDb = "https://mtsdatabase-2b32c-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private ConfigParser parser;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private Prefs prefs;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new Prefs(this);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sp.edit();
        key = findViewById(R.id.edTxt_key);
        conf  = findViewById(R.id.edTxt_config);
        btn_export = findViewById(R.id.btn_export);
        btn_read = findViewById(R.id.btn_read);
        results = findViewById(R.id.tv_results);

        //database = FirebaseDatabase.getInstance();
       // myRef = database.getReference("cloudConfig");

        btn_export.setOnClickListener(view -> {
            if(conf.getText().toString().isEmpty()){
                showSnack("Unique key cannot be empty!");
            } else {
                String token = getSaltString();
                String config = conf.getText().toString();
                parser = new ConfigParser(config);
                database = FirebaseDatabase.getInstance(getInstanceDb);
                myRef = database.getReference("cloudConfig");
                ProgressDialog pd = new ProgressDialog(this);
                pd.setMessage("Please wait...");
                pd.setCancelable(false);
                pd.show();
                Handler mHandler = new Handler();
                mHandler.postDelayed(() -> {
                    myRef.child(token).setValue(parser).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            showSnack("Successfully Updated");
                            results.setText("Successfully Updated");
                            pd.dismiss();
                            success(token);
                        }
                    });
                }, 1200);
            }
        });

        btn_read.setOnClickListener(view -> {
            read();
        });
    }

    protected String getSaltString() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 18) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

    }

    private void read()
    {
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_read, null);
        ab.setView(dialogView);
        ab.setCancelable(false);
        ab.setTitle("Read Data");
        ab.setPositiveButton("CHECK", null);
        ab.setNegativeButton("Cancel", null);

        EditText token = dialogView.findViewById(R.id.edtxt_token);

        AlertDialog ad = ab.create();
        ad.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {

                Button p = ad.getButton(AlertDialog.BUTTON_POSITIVE);
                p.setOnClickListener(view -> {
                    // TODO Do something
                    ProgressDialog pd = new ProgressDialog(Main.this);
                    pd.setMessage("Checking Data...");
                    pd.setCancelable(false);
                    pd.show();
                    Handler mHandler = new Handler();
                    mHandler.postDelayed(() ->  readUser(token.getText().toString(), pd), 1200);
                    ad.dismiss();
                });

                Button nega = ad.getButton(AlertDialog.BUTTON_NEGATIVE);
                nega.setOnClickListener(view -> {
                    // TODO Do something
                    ad.dismiss();
                });
            }
        });
        ad.show();
    }

    private void success(String str)
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_success, null);
        alertDialogBuilder.setView(dialogView);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setTitle("Success");

        TextView token = dialogView.findViewById(R.id.tv_token);
        token.setText(str);

        alertDialogBuilder.setPositiveButton("COPY TOKEN", (dialog, id) -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("token", str);
            clipboard.setPrimaryClip(clip);
            showSnack("Copied: " + str);
            dialog.cancel();
        });

        alertDialogBuilder.setNegativeButton("CANCEL", (dialog, id) -> dialog.cancel());

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void readUser(String username, ProgressDialog pd) {
        database = FirebaseDatabase.getInstance(getInstanceDb);
        myRef = database.getReference("cloudConfig");
        myRef.child(username).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {

                if (task.isSuccessful()){

                    if (task.getResult().exists()){
                        pd.dismiss();
                        Toast.makeText(Main.this,"Successfully Read",Toast.LENGTH_SHORT).show();
                        DataSnapshot dataSnapshot = task.getResult();
                        String conf = String.valueOf(dataSnapshot.child("config").getValue());
                        //boolean bl_premium = (boolean) dataSnapshot.child("premiumAccess").getValue();
                        results.setText(Prefs.decrypt(conf));
                        editor.putString("configs", conf);
                        editor.apply();
                    }else {
                        results.setText("User Doesn't Exist");
                        showSnack("User Doesn't Exist");
                    }

                }else {
                    Toast.makeText(Main.this,"Failed to read",Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    private void showToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void showSnack(String msg){
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show();
    }
}
