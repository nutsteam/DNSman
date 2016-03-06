package io.github.otakuchiyan.dnsman;

import android.net.NetworkInfo;

import java.util.HashMap;
import java.lang.Integer;

/**
 * Created by 西行寺幽玄 on 3/2/2016.
 */
public interface ValueConstants {
    String[] DEFAULT_DNS_LIST = {
            "127.0.0.1",
            "192.168.0.1",
            "192.168.100.1",
            "8.8.8.8",
            "8.8.4.4",
            "208.67.222.222",
            "208.67.220.220"
    };

    String KEY_DNS_LIST = "dns_list";
    String KEY_PREF_FULL_KEYBOARD = "pref_full_keyboard";

    int REQUEST_DNS_CHANGE = 0x00;
}
