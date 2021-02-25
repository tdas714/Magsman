package applied.ai.magsman;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.e("onReceive: BOOT", "Completed");
//        Intent i = new Intent(context,MyService.class);
//        context.startService(i);
        ComponentName comp = new ComponentName(context.getPackageName(),
                MyService.class.getName());
        MyService.enqueueWork(context, (intent.setComponent(comp)));
    }
}
