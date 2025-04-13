package com.maze.myapplication;

import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;



public class NetworkInterfaceViewer {



    public static void NetworkInterfaceView() {




        Enumeration<NetworkInterface> networkInterfaces = null ;


        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) networkInterfaces.nextElement();
                showNetworkInterface(ni);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public static void showNetworkInterface(NetworkInterface ni) throws SocketException {
        if (ni.getDisplayName().contains("swlan")) {
            Log.d("TrashView", "===================================");
            Log.d("TrashView", "   Network Interface Information   ");
            Log.d("TrashView", "===================================");
            Log.d("TrashView", "Display name: " + ni.getDisplayName());
            Log.d("TrashView", "Name: " + ni.getName());
            Log.d("TrashView", "MTU: " + ni.getMTU());
            Log.d("TrashView", "Is loop back?: " + ni.isLoopback());
            Log.d("TrashView", "Is point to point?: " + ni.isPointToPoint());
            Log.d("TrashView", "Is up?: " + ni.isUp());
            Log.d("TrashView", "Is virtual?: " + ni.isVirtual());
            Log.d("TrashView", "Support multicast?: " + ni.supportsMulticast());


            Enumeration<InetAddress> addresses = ni.getInetAddresses();

            while (addresses.hasMoreElements()) {
                InetAddress address = (InetAddress) addresses.nextElement();
                if(!address.isLinkLocalAddress())
                    showIpAddressInfo(address);
            }

        }
    }

    public static void showIpAddressInfo(InetAddress ipAddress ) {
        Log.d("TrashView","IP address info:" );
        //Log.d("TrashView"," - Canonical host name: " +ipAddress.getCanonicalHostName());
        Log.d("TrashView"," - Host address: " + ipAddress .getHostAddress());
        //Log.d("TrashView"," - Host name: " + ipAddress .getHostName());
        Log.d("TrashView"," - Is any local address?: " + ipAddress.isAnyLocalAddress());
        Log.d("TrashView"," - Is link local address?: " +ipAddress.isLinkLocalAddress());
        Log.d("TrashView"," - Is loop back address?: " + ipAddress.isLoopbackAddress());
        Log.d("TrashView"," - Is MC global?: " + ipAddress .isMCGlobal());
        Log.d("TrashView"," - Is MC link local?: " + ipAddress .isMCLinkLocal());
        Log.d("TrashView"," - Is MC node global?: " + ipAddress .isMCNodeLocal());
        Log.d("TrashView"," - Is MC org local?: " + ipAddress .isMCOrgLocal());
        Log.d("TrashView"," - Is MC site local?: " + ipAddress .isMCSiteLocal());
        Log.d("TrashView"," - Is multicast address?: " +ipAddress.isMulticastAddress());
        Log.d("TrashView"," - Is site local address?: " +ipAddress.isSiteLocalAddress());

    }

}
