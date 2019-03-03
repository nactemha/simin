package ahmetcan.simin

import ahmetcan.simin.Discovery.Model.persistent.YoutubeSubscriptionResult
import android.app.Application
import android.util.Log
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.firebase.FirebaseApp
import io.realm.Realm
import io.realm.RealmConfiguration


open class ACApplication : Application() {

    init {
        instance=this

    }

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        Fresco.initialize(this);

        Realm.init(this);
        val config = RealmConfiguration.Builder()
                .name("ahmetcan3.realm")
                .schemaVersion(1)
                .deleteRealmIfMigrationNeeded()
                .build()
        Realm.setDefaultConfiguration(config)

//        MobileAds.initialize(this, "ca-app-pub-3353784488411814~2328933720");


//        Thread.setDefaultUncaughtExceptionHandler(object: Thread.UncaughtExceptionHandler{
//            override fun uncaughtException(p0: Thread?, p1: Throwable?) {
//                Log.e("Simin App Uncaught:",p1.toString())
//            }
//
//        })
    }


    companion object {
        lateinit var instance:ACApplication
    }
}