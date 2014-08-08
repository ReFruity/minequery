package com.jdc.minequery;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import com.jdc.minequery.InetSocketAddress.InvalidHostException;
import com.jdc.minequery.InetSocketAddress.InvalidPortException;

import java.util.ArrayList;
import java.util.Map;

public class MainActivity extends Activity {

    private InetSocketAddress address;
    private EditText hostEdit;
    private EditText portEdit;
    private ListView listView;
    private ArrayList<String> list = new ArrayList<String>();
    private ArrayAdapter<String> adapter;
    private Map<String,String> serverInfo;
    SharedPreferences sharedPref;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.main);
        
        setSettings();
        
        hostEdit = (EditText)findViewById(R.id.editText);
        portEdit = (EditText)findViewById(R.id.editText1);
        listView = (ListView)findViewById(R.id.listView);

        // Fill fields with previously used values
        String lastHost = sharedPref.getString("lastHost", "");
        int lastPort = sharedPref.getInt("lastPort", 25565);
        hostEdit.setText(lastHost);
        portEdit.setText(Integer.toString(lastPort));
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }
    
    public void onButtonClick(View view) {

        // Fields and variables pre-initialization
        int port = 0;
        serverInfo = null;
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_LONG;

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        
        // Fetch host address and port
        String host = hostEdit.getText().toString();
        try {
            port = Integer.parseInt(portEdit.getText().toString());
        }
        catch(NumberFormatException e) {
            // Incorrect port was specified, skipping
        }
        
        // Check Internet connection availability
        if (networkInfo != null && networkInfo.isConnected()) {
            try {
                address = new InetSocketAddress(host, port);
                new GetServerInfoTask().execute(address);
            }
            catch (InvalidHostException e) {
                Toast.makeText(context, "Please, enter host address", duration).show();
            }
            catch (InvalidPortException e) {
                Toast.makeText(context, "Please, enter correct port", duration).show();
            }
        } else {
            Toast.makeText(context, "No Internet connection", duration).show();
        }

        // Remember input values
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("lastHost", host);
        editor.putInt("lastPort", port);
        editor.apply();
    }

    private class GetServerInfoTask extends AsyncTask<InetSocketAddress, Void, String[]> {
        private Exception e = null;
        
        @Override
        protected String[] doInBackground(InetSocketAddress... addresses) {
            // displays a loading spinner near the title
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.setProgressBarIndeterminateVisibility(true);
                }
            });
            // params comes from the execute() call: params[0] is the server address.
            try {
                Query query = new Query(addresses[0].getHostName(), addresses[0].getPort());
                query.sendQuery();
                serverInfo = query.getValues();
                String[] players = query.getOnlineUsernames();
                return players;
            } catch (Exception e) {
                this.e = e;
                return new String[0];
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String[] result) {
            list.clear();
            if (e != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), 
                                "Unable to retrieve info. Server offline or incorrect address.", 
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
            else {
                if (result.length > 0) {
                    for (String player : result) {
                        list.add(player);
                    }
                } else {
                    list.add("No players");
                }
            }
            adapter.notifyDataSetChanged();
            // hide loading spinner
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.setProgressBarIndeterminateVisibility(false);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_info:
                showInfo();
                return true;
            case R.id.action_settings:
                startActivityForResult(new Intent(getApplicationContext(), SettingsActivity.class), 0);
            return true;
            case R.id.action_about:
                showAbout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    public void showInfo() {
        Context context = getApplicationContext();
        
        if(serverInfo == null) {
            CharSequence text = "Press check button first";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            
            return;
        }
        
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        // Setting Dialog Title
        alertDialog.setTitle("Additional information");

        String message = serverInfo.toString();
        
        // Setting Dialog Message
        alertDialog.setMessage(message);

        // Setting OK Button
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Executes after dialog is closed
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }
    
    public void showAbout() {
        Context context = getApplicationContext();
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        // Setting Dialog Title
        alertDialog.setTitle("About");

        String program = "Minecraft Query";
        String version = "";
        String author = "Anton Istomin";
        String email = "istomanton@gmail.com";
        
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(
                    context.getPackageName(), 0);
            version = info.versionName;
        } catch (Exception e) {
            Log.e("YourActivity", "Error getting version");
        }
        
        String message = program + " " 
                         + version + "\n"
                         + author + "\n" 
                         + email + "\n";

        // Setting Dialog Message
        alertDialog.setMessage(message);

        // Setting OK Button
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Write your code here to execute after dialog closed
            }
        });
        
        // Showing Alert Message
        alertDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        setSettings();
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    private void setSettings() {
        if (sharedPref.getBoolean("pref_keepScreenOn", false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}
