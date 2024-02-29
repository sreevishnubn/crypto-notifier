package com.example.stoploss;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static String API_URL;
    private static final String CHANNEL_ID = "StockPriceChannel";
    private static final long CHECK_INTERVAL_MS = 60000; // 1 minute
    private static String cname;
    private static Boolean greater = true;
    private EditText targetPriceEditText;
    private Button checkPriceButton;
    private Button stopPriceButton;
    private Button highButton;
    private Button lowButton;
    private TextView price;

    private Spinner coins;

    private TextView coinName;
    private Handler handler;
    private Runnable runnable;
    private boolean notificationSent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        targetPriceEditText = findViewById(R.id.target_price_edittext);
        checkPriceButton = findViewById(R.id.check_price_button);
        stopPriceButton = findViewById(R.id.stop);
        highButton = findViewById(R.id.high);
        lowButton = findViewById(R.id.low);
        coinName = findViewById(R.id.coinName);
        price = findViewById(R.id.pricevalue);
        coins = findViewById(R.id.spinner);

        coins.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedValue = parent.getItemAtPosition(position).toString();
                cname = selectedValue;
                API_URL = "https://api.coincap.io/v2/assets/"+selectedValue;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                API_URL = "https://api.coincap.io/v2/assets/bitcoin";
            }
        });


        //checkPriceButton.setEnabled(false);

        checkPriceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                coinName.setText(cname);
                String targetPriceText = targetPriceEditText.getText().toString().trim();
                if (!targetPriceText.isEmpty()) {
                    double targetPrice = Double.parseDouble(targetPriceText);
                    startStockPriceCheck(targetPrice);
                }
            }

        });

        stopPriceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopStockPriceCheck();
                targetPriceEditText.setText("");
                price.setText("₹0");
                coinName.setText("--");
                highButton.setEnabled(true);
                lowButton.setEnabled(true);
            }
        });

        highButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lowButton.setEnabled(false);
                greater = true;
            }
        });

        lowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                highButton.setEnabled(false);
                greater = false;
            }
        });

    }

    private void startStockPriceCheck(final double targetPrice) {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                new CheckStockPriceTask().execute(targetPrice);
            }
        };

        handler.post(runnable);
    }

    private class CheckStockPriceTask extends AsyncTask<Double, Void, Double> {
        private double targetPrice;
        @Override
        protected Double doInBackground(Double... params) {
            targetPrice = params[0];
            double currentPrice = fetchStockPrice();


            if(!greater){
                if (currentPrice < targetPrice) {
                    sendNotification("Crypto Price Alert", "Current price " + currentPrice + " The Crypto price has gone above the target price" + targetPrice + "!");
                    notificationSent = true;
                    System.out.println(targetPrice);
                    stopStockPriceCheck();
                }
            }else{
                if (currentPrice > targetPrice) {
                    sendNotification("Crypto Price Alert", "Current price " + currentPrice + " The Crypto price has fallen below the target price" + targetPrice + "!");
                    notificationSent = true;
                    System.out.println(targetPrice);
                    stopStockPriceCheck();
                }
            }

            return currentPrice;
        }

        protected void onPostExecute(Double result) {
            super.onPostExecute(result);
            price.setText("₹"+String.valueOf(result));
            if(!greater){
                if (result >= targetPrice) {
                    handler.postDelayed(runnable, CHECK_INTERVAL_MS);
                }
            }else{
                if (result <= targetPrice) {
                    handler.postDelayed(runnable, CHECK_INTERVAL_MS);
                }
            }
        }
    }

    private  double fetchStockPrice() {
        double currentPrice = 0;
        double rateUsd = 1;

        try
        {
            URL rateurl = new URL("https://api.coincap.io/v2/rates");
            HttpURLConnection rateconnection = (HttpURLConnection) rateurl.openConnection();
            rateconnection.setRequestMethod("GET");
            rateconnection.connect();

            int rateres = rateconnection.getResponseCode();
            if(rateres == HttpURLConnection.HTTP_OK){
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(rateconnection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                JSONArray dataArray = json.getJSONArray("data");
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject currency = dataArray.getJSONObject(i);
                    if (currency.getString("symbol").equals("INR")) {
                        rateUsd = currency.getDouble("rateUsd");
                        break;
                    }
                }
                System.out.println("-------------rate--------below----------");
                System.out.println(rateUsd);

            }

            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                JSONObject data = json.getJSONObject("data");
                currentPrice = data.getDouble("priceUsd");
                System.out.println(currentPrice);
            }

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (currentPrice/rateUsd);
    }

    private void sendNotification(String title, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Stock Price Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notification channel for stock price alerts");
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(0, builder.build());
    }

    private void stopStockPriceCheck() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
            notificationSent = false;
            System.out.println("stopped ");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopStockPriceCheck();
    }
}
