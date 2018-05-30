package projet_e3.esiee.com.projet_e3;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import java.net.InetAddress;

public class GuestClass extends Thread {
    private String HostAdd;
    private Context context;

    public GuestClass(InetAddress hostAdd, Context app) {
        HostAdd = hostAdd.getHostAddress();
        context = app;
    }

    @SuppressLint("ResourceType")
    @Override
    public void run() {

        Intent serviceIntent = new Intent(context, FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        if (!TextUtils.isEmpty(HostAdd) && HostAdd.length() > 0) {
            FileTransferService.PORT = 8988;
            int sub_port = FileTransferService.PORT;
            serviceIntent.putExtra(
                    FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                    HostAdd);

            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, FileTransferService.PORT);

            if (HostAdd != null && sub_port != -1) {
                context.startService(serviceIntent);
            }
        }
    }
}