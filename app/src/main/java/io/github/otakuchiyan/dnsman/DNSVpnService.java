package io.github.otakuchiyan.dnsman;

import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Enumeration;

public class DnsVpnService extends VpnService {
    private static ParcelFileDescriptor fd;
    private static Thread vpnThread;
    private static String vdns1;
    private static String vdns2;

    public DnsVpnService() {
    }

    public static void perform(Context c, String dns1, String dns2){
        Intent i = new Intent(c, DnsVpnService.class);
        vdns1 = dns1;
        vdns2 = dns2;
        c.startService(i);
    }

    //FIXME: cannot stop vpn
    public int disconnect(){
        if(vpnThread != null) {
            try {
                vpnThread.join(1000);
                stopSelf();
                return ValueConstants.RESTORE_SUCCEED;
            } catch (InterruptedException e) {
                Log.e("VpnService", "vpnThread did not exit");
            }
        }
        return ValueConstants.ERROR_NULL_VPN;
    }

    private String getAddress(){
        try {
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
                 ifaces.hasMoreElements(); ) {
                Enumeration<InetAddress> addresses = ifaces.nextElement().getInetAddresses();
                while (addresses.hasMoreElements()) {
                    String addr = addresses.nextElement().getHostAddress();
                    if (!addr.equals("127.0.0.1") &&
                            !addr.equals("0.0.0.0") &&
                            !addr.equals("::1%1") &&
                            //Escaping IPv6
                            addr.charAt(5) != ':') {
                        return addr;
                    }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId){
        vpnThread = new Thread(new Runnable() {
            @Override
            public void run() {
            try {
                String addr = getAddress();
                String real_addr = "";

                //Escaping IPv6 address suffix - "<Real address>%wlan0"
                for(int i = addr.length() - 1; i != 0; i--){
                    if(addr.charAt(i) == '%'){
                        real_addr = addr.substring(0, i);
                    }
                }

                //If no suffix
                if(real_addr.equals("")){
                    real_addr = addr;
                }

                Log.d("DnsVpn", "addr = " + real_addr);
                DatagramChannel tunnel = DatagramChannel.open();

                if(!protect(tunnel.socket())) {
                    throw new IllegalStateException("Cannot protect the tunnel");
                }
                tunnel.connect(new InetSocketAddress(addr, 8087));
                tunnel.configureBlocking(false);

                Builder vpn = new Builder();
                vpn.setSession("DnsVpnService")
                        .addAddress(real_addr, 24);
                if(!vdns1.equals("")) {
                    vpn.addDnsServer(vdns1);
                }
                if(!vdns2.equals("")) {
                    vpn.addDnsServer(vdns2);
                }
                fd = vpn.establish();

                while(true){
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if(fd != null) {
                        fd.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            }
        });

        vpnThread.start();
        return START_STICKY;
    }
}
