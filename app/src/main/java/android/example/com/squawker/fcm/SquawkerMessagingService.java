package android.example.com.squawker.fcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.example.com.squawker.MainActivity;
import android.example.com.squawker.R;
import android.example.com.squawker.provider.SquawkContract;
import android.example.com.squawker.provider.SquawkProvider;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class SquawkerMessagingService extends FirebaseMessagingService {
    private static final String TAG = SquawkerMessagingService.class.getSimpleName();

    private static final String JSON_KEY_AUTHOR = SquawkContract.COLUMN_AUTHOR;
    private static final String JSON_KEY_AUTHOR_KEY = SquawkContract.COLUMN_AUTHOR_KEY;
    private static final String JSON_KEY_MESSAGE = SquawkContract.COLUMN_MESSAGE;
    private static final String JSON_KEY_DATE = SquawkContract.COLUMN_DATE;

    private static final int NOTIFICATION_MAX_CHARACTERS = 30;
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String,String> data =  remoteMessage.getData();
        if (data.size() > 0) {
            String author = data.get(JSON_KEY_AUTHOR);
            String authorKey = data.get(JSON_KEY_AUTHOR_KEY);
            String message = data.get(JSON_KEY_MESSAGE);
            String date = data.get(JSON_KEY_DATE);

            sendNotification(author, message);
            insertSquawk(author, authorKey, message, date);
        }
    }

    private void insertSquawk(final String author, final String authorKey,
                              final String message, final String date) {

        // Database operations should not be done on the main thread
        AsyncTask<Void, Void, Void> insertSquawkTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                ContentValues newMessage = new ContentValues();
                newMessage.put(SquawkContract.COLUMN_AUTHOR, author);
                newMessage.put(SquawkContract.COLUMN_MESSAGE, message);
                newMessage.put(SquawkContract.COLUMN_DATE, date);
                newMessage.put(SquawkContract.COLUMN_AUTHOR_KEY, authorKey);
                getContentResolver().insert(SquawkProvider.SquawkMessages.CONTENT_URI, newMessage);
                return null;
            }
        };

        insertSquawkTask.execute();
    }

    private void sendNotification(final String author, final String pMessage) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Create the pending intent to launch the activity
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        // If the message is longer than the max number of characters we want in our
        // notification, truncate it and add the unicode character for ellipsis
        String message = pMessage;
        if (message.length() > NOTIFICATION_MAX_CHARACTERS) {
            message = message.substring(0, NOTIFICATION_MAX_CHARACTERS) + "\u2026";
        }

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_duck)
                .setContentTitle(String.format(getString(R.string.notification_message), author))
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
