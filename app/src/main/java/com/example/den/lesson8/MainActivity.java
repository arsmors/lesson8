package com.example.den.lesson8;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuItem;

import com.example.den.lesson8.DataSources.Giphy.NetworkingManagerGiphy;
import com.example.den.lesson8.DataSources.Giphy.PhotoItemsGiphy;
import com.example.den.lesson8.DataSources.Local.NetworkingManagerLocal;
import com.example.den.lesson8.DataSources.Unsplash.NetworkingManagerUnsplash;
import com.example.den.lesson8.DataSources.Unsplash.PhotoItemUnsplash;
import com.example.den.lesson8.Interfaces.NetworkingManager;
import com.example.den.lesson8.Interfaces.PhotoItem;
import com.example.den.lesson8.Interfaces.PhotoItemsPresenter;
import com.example.den.lesson8.Interfaces.PhotoItemsPresenterCallbacks;
import com.example.den.lesson8.Presenters.PhotoItemPresenterGridView;
import com.onesignal.OneSignal;
import com.orm.SugarRecord;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements PhotoItemsPresenterCallbacks {

    public enum ImgServices {
        UNSPLASH,
        GIPHY,
        FAVORITE
    }

    private NetworkingManager networkingManager;
    private PhotoItemsPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showImgService(ImgServices.GIPHY);

        showNotificationAfterDelay(getBasicNotification());

        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();

       // OneSignal.setEmail("arsens.morins@chi.lv");
    }


    private void showImgService(ImgServices service) {

        switch (service) {
            case GIPHY:
                networkingManager = new NetworkingManagerGiphy();
                break;
            case UNSPLASH:
                networkingManager = new NetworkingManagerUnsplash();
                break;
            case FAVORITE:
                networkingManager = new NetworkingManagerLocal();
        }

        this.presenter = new PhotoItemPresenterGridView();
        this.networkingManager.getPhotoItems(photoItems ->
                runOnUiThread(()-> {
                    presenter.setupWithPhotoItems(photoItems,this, this);
                })
        );
    }


    private NotificationCompat.Builder getBasicNotification() {

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "default")
                .setSmallIcon(R.drawable.white_notification_icon)
                .setContentTitle("This is my test notification")
                .setContentText("Notification visible when app starts")
//                .setStyle(new NotificationCompat.BigTextStyle()
//                        .bigText("Much longer text that cannot fit one line..."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        return mBuilder;
    }

    private void showNotificationAfterDelay(final NotificationCompat.Builder notification) {

        final NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 26) {
            String id = "my_channel_01";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(id, "Channel name",importance);
            mChannel.enableLights(true);
            mNotificationManager.createNotificationChannel(mChannel);
            notification.setChannelId(id);
        }

        final Handler handler = new Handler();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mNotificationManager.notify(0, notification.build());
            }
        }, 5000);
    }


    @Override
    public void onItemSelected(PhotoItem item) {
        Intent shareIntent = new Intent(this, ShareActivityWithFragments.class);
        shareIntent.putExtra(ShareActivity.PHOTO_ITEM_KEY,item);
        startActivity(shareIntent);
    }

    @Override
    public void onItemToggleFavorite(PhotoItem item) {
        testFavoriteORM(item);
    }

    @Override
    public void onLastItemReach(int position) {
        networkingManager.fetchNewItemsFromPosition(position, photoItems -> {
            runOnUiThread(()-> {
                presenter.updateWithItems(photoItems);
            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu, menu);

        final MenuItem favoriteMenuItem = menu.findItem(R.id.action_show_favotites);
        favoriteMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                showImgService(ImgServices.FAVORITE);
                return true;
            }
        });

        final MenuItem showUnsplashMenuItem = menu.findItem(R.id.action_show_unslash);
        showUnsplashMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                showImgService(ImgServices.UNSPLASH);
                return true;
            }
        });

        final MenuItem showUnsplashGiphyItem = menu.findItem(R.id.action_show_giphy);
        showUnsplashGiphyItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                showImgService(ImgServices.GIPHY);
                return true;
            }
        });

        return true;
    }

    // *****************************************************
    // *****************************************************
    // ************************ ORM ************************
    // *****************************************************
    // *****************************************************

    private void testFavoriteORM(PhotoItem item) {

        if(item.isSavedToDatabase()) {
            item.deleteFromDatabase();

            // Remove favorite from screen if unfavorite from favorite screen
            if (networkingManager.getClass() == NetworkingManagerLocal.class) {
                showImgService(ImgServices.FAVORITE);
            }
        } else {
            item.saveToDatabase();
        }
    }
}
