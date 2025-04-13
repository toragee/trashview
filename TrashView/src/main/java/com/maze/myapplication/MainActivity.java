package com.maze.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URLDecoder;

public class MainActivity extends AppCompatActivity {
    private String TAG = "TrashView";
    private final int STORAGE_PERMISSION_CODE = 1;

    private int CLIENT_PORT = 18081;
    //    private String SERVER_IP = "192.168.213.125";
    private String SERVER_IP = "192.168.0.4";
    private int SERVER_PORT = 18080;
    private WebView mWebView;

    private NetworkInterfaceViewer niv;
    private ConnectivityManager.NetworkCallback networkCallback;
    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed to download files")
                    .setPositiveButton("ok", (dialog, which) -> ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE))
                    .setNegativeButton("cancel", (dialog, which) -> dialog.dismiss())
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }



    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        requestStoragePermission();
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mWebView = findViewById(R.id.activity_main_webview);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new HelloWebViewClient());
        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {

            try {
                Uri source = Uri.parse(url);

                // 파일명 잘라내기
                contentDisposition = URLDecoder.decode(contentDisposition, "UTF-8");
                String fileName = contentDisposition.replace("attachment; filename=", "");
                if (fileName != null && fileName.length() > 0) {
                    int idxFileName = fileName.indexOf("filename =");
                    if (idxFileName > -1) {
                        fileName = fileName.substring(idxFileName + 9).trim();
                    }

                    if (fileName.endsWith(";")) {
                        fileName = fileName.substring(0, fileName.length() - 1);
                    }

                    if (fileName.startsWith("\"") && fileName.startsWith("\"")) {
                        fileName = fileName.substring(1, fileName.length() - 1);
                    }
                }else {
                    fileName = URLUtil.guessFileName(url, contentDisposition, "mp4");
                }

                DownloadManager.Request request = new DownloadManager.Request(source);
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription("Downloading File...");
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, "mp4"));
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);


                //request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                dm.enqueue(request);
                Toast.makeText(getApplicationContext(), "Downloading File", Toast.LENGTH_LONG).show();
            }
            catch (Exception e) {
                // 권한ID를 가져옵니다


            }
        });
        mWebView.loadUrl(SERVER_IP); //Replace The Link Here


        if (isNetworkAvailable()) {
            mWebView.loadUrl(SERVER_IP);
            //mWebView.loadUrl("trashview.iptime.org");


        } else {
            mWebView.loadUrl("file:///android_asset/offline.html");
        }

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
//                runOnUiThread(() -> {
//                    if (!mWebView.getUrl().startsWith("file:///android_asset")) {
//                        Log.i(TAG, "mWebView loadUrl: " + SERVER_IP);
//                        //mWebView.loadUrl(SERVER_IP);
//                        //mWebView.loadUrl("trashview.iptime.org");
//
//                        syncTime();
//                    }
//                });
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


        BroadcastReceive();

    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = connectivityManager.getActiveNetwork();
        if (nw == null) return false;
        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
        return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
    }
    public void BroadcastReceive() {
        // 안드로이드에서 네트워킹은 thread 이용이 필수 이다.
        new Thread(new Runnable() {
            @Override
            public void run() {


                try {
                    DatagramSocket socket = new DatagramSocket(9050, InetAddress.getByName("192.168.158.255"));
                    socket.setBroadcast(true);

                    while (true) {
                        Log.i(TAG, "Ready to receive broadcast packets!");

                        byte[] recvBuf = new byte[15000];
                        DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                        socket.receive(packet);

                        Log.i(TAG, "Packet received from: " + packet.getAddress().getHostAddress());
                        String data = new String(packet.getData()).trim();
                        Log.i(TAG, "Packet received; data: " + data);

                        SERVER_IP = packet.getAddress().toString();
                        SERVER_IP = SERVER_IP.replace("/","");

                        runOnUiThread(() -> {
                            if (!mWebView.getUrl().startsWith("file:///android_asset")) {
                                Log.i(TAG, "mWebView loadUrl: " + SERVER_IP);
                                mWebView.loadUrl(SERVER_IP);
                                //mWebView.loadUrl("trashview.iptime.org");

                                syncTime();
                            }
                        });

                        break;
                    }
                } catch (IOException ex) {
                    Log.i(TAG, "Oops" + ex.getMessage());
                }
            }
        }).start();
    }
    public void syncTime() {
        // 안드로이드에서 네트워킹은 thread 이용이 필수 이다.
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 1. Client Socket 생성
                    DatagramSocket clientSocket = new DatagramSocket(CLIENT_PORT);
                    Log.d("TrashView", "Client Socket Created");
                    long now = System.currentTimeMillis()/1000 + 32400; //UTC - KST


                    String currentTime ="time:"+ now+"\r\n"; // 서버에 보낼 메세지
                    InetAddress inetAddress = InetAddress.getByName(SERVER_IP); // InetAddress 객체에 입력받은 ip 담기

                    // 2. datagram 생성 - server IP 와 server 포트넘버 포함
                    DatagramPacket clientPacket = new DatagramPacket(currentTime.getBytes(), currentTime.length(), inetAddress, SERVER_PORT);
                    // UDPSocket 을 통해서 datagram 을 내보낸다.
                    clientSocket.send(clientPacket);
                    Log.d("TrashView", "Packet Send");


                    // 10.0.2.15
                } catch (SocketException e) {
                    Log.e("TrashView", "Sender SocketException");
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }).start();

    }
    private static class HelloWebViewClient extends WebViewClient
    {
        @Override
        public boolean shouldOverrideUrlLoading(final WebView view, final String url)
        {
            view.loadUrl(url);
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