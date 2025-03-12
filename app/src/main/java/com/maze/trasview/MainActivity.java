package com.maze.trasview;

import android.annotation.SuppressLint;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;


public class MainActivity extends AppCompatActivity {
    private WebView mWebView;
    private ConnectivityManager.NetworkCallback networkCallback;
    private int CLIENT_PORT = 18081;
    private String SERVER_IP = "192.168.0.4";
    private int SERVER_PORT = 18080;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mWebView = findViewById(R.id.activity_main_webview);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new HelloWebViewClient());

        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            String fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimetype);

            if(fileExtension == null || fileExtension.isEmpty() || fileExtension.contains("bin")){
                fileExtension = "mp4";
            }
            request.setMimeType(mimetype);
            request.addRequestHeader("cookie", CookieManager.getInstance().getCookie(url));
            request.addRequestHeader("User-Agent", userAgent);
            request.setDescription("Downloading file...");
            String fileName = URLUtil.guessFileName(url, contentDisposition, fileExtension);
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, fileName));
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, fileExtension));

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            dm.enqueue(request);
            Toast.makeText(getApplicationContext(), "Downloading File " + fileName, Toast.LENGTH_LONG).show();

        });

        if (isNetworkAvailable()) {
            mWebView.loadUrl("192.168.0.4");
            //mWebView.loadUrl("trashview.iptime.org");
        } else {
            mWebView.loadUrl("file:///android_asset/offline.html");
        }

        networkCallback = new NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> {
                    if (!mWebView.getUrl().startsWith("file:///android_asset")) {
                        mWebView.loadUrl("192.168.0.4");
                        //mWebView.loadUrl("trashview.iptime.org");

                        syncTime();
                    }
                });
            }
            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> {
                    if (mWebView.getUrl() != null) {
                        mWebView.loadUrl("file:///android_asset/offline.html");
                    }
                });
            }
        };
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.registerDefaultNetworkCallback(networkCallback);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    public void syncTime() {
        // 안드로이드에서 네트워킹은 thread 이용이 필수 이다.
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 1. Client Socket 생성
                    DatagramSocket clientSocket = new DatagramSocket(CLIENT_PORT);
                    Log.d("VR", "Client Socket Created");
                    long now = System.currentTimeMillis()/1000 + 32400; //UTC - KST


                    String currentTime ="time:"+ now+"\r\n"; // 서버에 보낼 메세지
                    InetAddress inetAddress = InetAddress.getByName(SERVER_IP); // InetAddress 객체에 입력받은 ip 담기

                    // 2. datagram 생성 - server IP 와 server 포트넘버 포함
                    DatagramPacket clientPacket = new DatagramPacket(currentTime.getBytes(), currentTime.length(), inetAddress, SERVER_PORT);
                    // UDPSocket 을 통해서 datagram 을 내보낸다.
                    clientSocket.send(clientPacket);
                    Log.d("VR", "Packet Send");


                    // 10.0.2.15
                } catch (SocketException e) {
                    Log.e("VR", "Sender SocketException");
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = connectivityManager.getActiveNetwork();
        if (nw == null) return false;
        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
        return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
    }

    private static class HelloWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            view.loadUrl(request.getUrl().toString());
            return true;
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (networkCallback != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }
}