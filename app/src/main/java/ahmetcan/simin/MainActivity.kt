package ahmetcan.simin

import ahmetcan.echo.ACPremium
import ahmetcan.simin.Discovery.DiscoveryFragment
import ahmetcan.simin.Discovery.Real.DiscoveryRepository
import ahmetcan.simin.Discovery.SearchActivity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import com.google.android.material.tabs.TabLayout
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.telephony.TelephonyManager
import android.util.Log
import android.view.*
import android.view.Window.FEATURE_NO_TITLE
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import com.google.firebase.analytics.FirebaseAnalytics
import com.tooltip.Tooltip
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity() : AppCompatActivity(){
    lateinit var billing:ACPremium
    var isSubscripted: Boolean = false
    var  popupWindow:PopupWindow?=null
    var backgroundPop:PopupWindow?=null


    fun saveSubscriptionState(has: Boolean) {
        val subscription = getSharedPreferences("subscription", Context.MODE_PRIVATE)
        val edit = subscription.edit()
        edit.putBoolean("has", has)
        edit.commit()
    }
    fun checkCache() {
        val subscription = getSharedPreferences("cache", Context.MODE_PRIVATE)
        var introTimeMs = subscription.getLong("time", 0)

        if (introTimeMs > 0) {
            var asDay = TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().timeInMillis - introTimeMs)
            if (asDay > 1) {

                val edit = subscription.edit()
                edit.putLong("time", Calendar.getInstance().timeInMillis)
                edit.commit()

                DiscoveryRepository.invalidateChannelLists()
                DiscoveryRepository.invalidateLists()

            }
        }
        else{
            val edit = subscription.edit()
            edit.putLong("time", Calendar.getInstance().timeInMillis)
            edit.commit()
        }

    }
    fun fetchSubscriptionState(): Boolean {
        val subscription = getSharedPreferences("subscription", Context.MODE_PRIVATE)
        val has: Boolean = subscription.getBoolean("has", false)
        return has;
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        billing=ACPremium(this,object :ACPremium.IState{
            override fun onError() {
                FirebaseAnalytics.getInstance(this@MainActivity).logEvent("BillingError", null)
            }

            override fun onUserCancelFlow() {
                try{
                    var firebaseAnalytics = FirebaseAnalytics.getInstance(this@MainActivity)
                    val params = Bundle()
                    params.putString("BillingUserCancelStr","BillingUserCancelStrValu")
                    firebaseAnalytics.logEvent("BillingUserCancelEvent", params)

                }catch (ex:Exception){}
            }

            override fun onPremiumChanged(isPremium: Boolean) {
                if (isPremium) {
                    saveSubscriptionState(true)
                    HideShowPremiumDialog()
                } else {
                    saveSubscriptionState(false)
                    ShowGetPremimumDialog()
                }
            }

        })


        requestWindowFeature(FEATURE_NO_TITLE)

        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(false);

        checkCache();


        tab_layout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.icon?.clearColorFilter()
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    tab.icon?.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
                    when (it.position) {
                        0 -> changeFragment(DiscoveryFragment())
                        1 -> changeFragment(ChannelFragment())
                        2 -> changeFragment(FavoriesFragment())
                    }

                }

            }

        })
        tab_layout.getTabAt(0)?.icon?.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
        changeFragment(DiscoveryFragment())


        fab.setOnClickListener {
             var intent: Intent = Intent(this, SearchActivity::class.java)
             startActivity(intent)


        }


        isSubscripted = fetchSubscriptionState()
        if (isSubscripted) {
            HideShowPremiumDialog()
        }

        button_privacypolicy.setOnClickListener {
            AlertDialog.Builder(this)
                    .setMessage("would you like to see the privacy policy ?")
                    .setNegativeButton("Close",object:DialogInterface.OnClickListener{
                        override fun onClick(p0: DialogInterface?, p1: Int) {

                        }


                    })
                    .setPositiveButton("Go",object :DialogInterface.OnClickListener{
                        override fun onClick(p0: DialogInterface?, p1: Int) {
                            try {
                                val myIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.freeprivacypolicy.com/privacy/view/cf6bb63fe3af021f517bc52e9b40c4f4"))
                                startActivity(myIntent)
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(this@MainActivity, "No application can handle this request." + " Please install a webbrowser", Toast.LENGTH_LONG).show()
                                e.printStackTrace()
                            }

                        }


                    }).show()

        }
    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.main, menu)

        return true
    }


    private fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.contentFragment, fragment)
                .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        try{
            billing.destroy()
        }
        catch (ex:Exception){
            Log.e("Simin",ex.toString())
        }

    }

    fun ShowGetPremimumDialog(){
         var  inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
         var popupView = inflater.inflate(R.layout.view_getpremium, null);
         popupView.findViewById<Button>(R.id.button_getpremium).setOnClickListener {
             billing.buyPremium()
         }


        var backgroundView=View(this)
        backgroundView.setBackgroundColor(Color.TRANSPARENT)
        backgroundView.setOnClickListener {
            billing.buyPremium()

        }
        backgroundPop=PopupWindow(backgroundView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, false)
        backgroundPop!!.showAtLocation(tab_layout, Gravity.CENTER, 0, 0);


        var width = LinearLayout.LayoutParams.WRAP_CONTENT;
         var  height = LinearLayout.LayoutParams.WRAP_CONTENT;
         var focusable = false; // lets taps outside the popup also dismiss it
         popupWindow = PopupWindow(popupView, width, height, focusable);
         popupWindow!!.showAtLocation(tab_layout, Gravity.CENTER, 0, 0);


    }
    fun HideShowPremiumDialog(){
        if(popupWindow!=null) popupWindow!!.dismiss()
        if(backgroundPop!=null) backgroundPop!!.dismiss()

    }

}
