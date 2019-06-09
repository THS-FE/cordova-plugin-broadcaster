package org.bsc.cordova;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

/**
 * This class echoes a string called from JavaScript.
 */
public class CDVBroadcaster extends CordovaPlugin {

    public static final String USERDATA = "userdata";
    public static final String MSGDATA = "msgData";
    private static String TAG =  CDVBroadcaster.class.getSimpleName();

    public static final String EVENTNAME_ERROR = "event name null or empty.";

    java.util.Map<String,BroadcastReceiver> receiverMap =
                    new java.util.HashMap<String,BroadcastReceiver>(10);

    /**
     *
     * @param eventName
     * @param jsonUserData
     * @throws JSONException
     */
    protected void fireEvent( final String eventName, final Object jsonUserData) throws JSONException {

        String method ;
        if( jsonUserData != null ) {

            if (!(jsonUserData instanceof JSONObject)) {
                final JSONObject json = new JSONObject(String.valueOf(jsonUserData)); // CHECK IF VALID
            }

            method = String.format("window.broadcaster.fireEvent( '%s', %s );", eventName, String.valueOf(jsonUserData));
        }
        else {
            method = String.format("window.broadcaster.fireEvent( '%s', {} );", eventName);
        }
        this.webView.sendJavascript(method);
        /*
        this.webView.evaluateJavascript(method, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.d(TAG, "fireEvent executed!");
            }
        });
        */
    }

    protected void registerReceiver(android.content.BroadcastReceiver receiver, android.content.IntentFilter filter) {
        //LocalBroadcastManager.getInstance(super.webView.getContext()).registerReceiver(receiver,filter);
        //MainActivity.this.registerReceiver(receiver, filter);
        this.cordova.getActivity().registerReceiver(receiver,filter);
    }

    protected void unregisterReceiver(android.content.BroadcastReceiver receiver) {
        //LocalBroadcastManager.getInstance(super.webView.getContext()).unregisterReceiver(receiver);
        this.cordova.getActivity().unregisterReceiver(receiver);
    }

    protected void sendBroadcast(android.content.Intent intent) {
        this.cordova.getActivity().sendBroadcast(intent);
    }

    @Override
    public Object onMessage(String id, Object data) {
        if ("exit".equals(id)) {
            this.cordova.getActivity().finish();
        }
        try {
            fireEvent( id, data );
        } catch (JSONException e) {
            Log.e(TAG, "'userdata' is not a valid json object!");
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private void fireNativeEvent( final String eventName, JSONObject userData ) {
        if( eventName == null ) {
            throw new IllegalArgumentException("eventName parameter is null!");
        }
        /*Intent intent = new Intent( "com.nqsky.im.user.login");
		intent.putExtra("userName", "wangyun");
		intent.putExtra("passWord", "12345678");*/

        final Intent intent = new Intent(eventName);
        if(eventName.equals("com.nqsky.im.user.login")){
        	try {
				intent.putExtra("userName", userData.getString("userName"));
				intent.putExtra("passWord", userData.getString("passWord"));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }else if(eventName.equals("com.nqsky.im.user.logout")){
        	try {
				intent.putExtra("userName", userData.getString("userName"));
				intent.putExtra("passWord", userData.getString("passWord"));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }else{
        	if( userData != null ) {
                Bundle b = new Bundle();
                b.putString(USERDATA, userData.toString());
                intent.putExtras(b);
            }
        }
        sendBroadcast( intent );
    }

    /**
     *
     * @param action          The action to execute.
     * @param args            The exec() arguments.
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return
     * @throws JSONException
     */
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if( action.equals("fireNativeEvent")) {

            final String eventName = args.getString(0);
            if( eventName==null || eventName.isEmpty() ) {
                callbackContext.error(EVENTNAME_ERROR);

            }
            final JSONObject userData = args.getJSONObject(1);

            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    fireNativeEvent(eventName, userData);
                }
            });

            return true;
        }
        else if (action.equals("addEventListener")) {

            final String eventName = args.getString(0);
            if (eventName == null || eventName.isEmpty()) {
                callbackContext.error(EVENTNAME_ERROR);
                return false;
            }
            if (!receiverMap.containsKey(eventName)) {

                final BroadcastReceiver r = new BroadcastReceiver(){

                        @Override
                        public void onReceive(Context context, final Intent intent)
                        {
                            if (Intent.ACTION_PACKAGE_ADDED.equals(eventName)
                                || Intent.ACTION_PACKAGE_REMOVED.equals(eventName)
                                || Intent.ACTION_PACKAGE_REPLACED.equals(eventName))
                            {
                                try
                                {
                                    String packageName = intent.getData().getSchemeSpecificPart();
                                    JSONObject jsonObject = new JSONObject();
                                    jsonObject.put("packageName", packageName);
                                    String userData = jsonObject.toString();
                                    fireEvent(eventName, userData);
                                }
                                catch (JSONException e)
                                {
                                    e.printStackTrace();
                                }

                            }else if(eventName.equals("fromGZHMsg")||eventName.equals("fromChatMsg")){//来自公众号消息\通知等聊天消息
                            	 Bundle b = intent.getExtras();
                            	try
                                {

                                    String userData = "{}";
                                    if (b != null)
                                    {// in some broadcast there might be no extra info
                                        userData = b.getString(MSGDATA, "{}");
                                    }
                                    else
                                    {
                                        Log.v(TAG, "No extra information in intent bundle");
                                    }
                                    fireEvent(eventName, userData);

                                }
                                catch (JSONException e)
                                {
                                    Log.e(TAG, "'userdata' is not a valid json object!");
                                }
                            }
                            else
                            {
                                final Bundle b = intent.getExtras();
                                // parse the JSON passed as a string.
                                try
                                {

                                    String userData = "{}";
                                    if (b != null)
                                    {// in some broadcast there might be no extra info
                                        userData = b.getString(USERDATA, "{}");
                                    }
                                    else
                                    {
                                        Log.v(TAG, "No extra information in intent bundle");
                                    }
                                    fireEvent(eventName, userData);

                                }
                                catch (JSONException e)
                                {
                                    Log.e(TAG, "'userdata' is not a valid json object!");
                                }
                            }
                        }
                    };
                    IntentFilter filter = new IntentFilter(eventName);
                    if (Intent.ACTION_PACKAGE_ADDED.equals(eventName) || Intent.ACTION_PACKAGE_REMOVED.equals(eventName)
                        || Intent.ACTION_PACKAGE_REPLACED.equals(eventName))
                    {
                        filter.addDataScheme("package");
                    }
                    registerReceiver(r, filter);
                    receiverMap.put(eventName, r);
                }
                callbackContext.success();

            return true;
        } else if (action.equals("removeEventListener")) {

            final String eventName = args.getString(0);
            if (eventName == null || eventName.isEmpty()) {
                callbackContext.error(EVENTNAME_ERROR);
                return false;
            }

            BroadcastReceiver r = receiverMap.remove(eventName);

            if (r != null) {

                unregisterReceiver(r);


            }
            callbackContext.success();
            return true;
        }
        return false;
    }

    /**
     *
     */
    @Override
    public void onDestroy() {
        // deregister receiver
        for( BroadcastReceiver r : receiverMap.values() ) {
                    unregisterReceiver(r);
        }

        receiverMap.clear();

        super.onDestroy();

    }

}
