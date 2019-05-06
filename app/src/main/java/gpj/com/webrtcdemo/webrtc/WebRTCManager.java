package gpj.com.webrtcdemo.webrtc;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.graphics.PixelFormat;
import android.media.AsyncPlayer;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fi.vtt.nubomedia.kurentoroomclientandroid.KurentoRoomAPI;
import fi.vtt.nubomedia.kurentoroomclientandroid.RoomError;
import fi.vtt.nubomedia.kurentoroomclientandroid.RoomListener;
import fi.vtt.nubomedia.kurentoroomclientandroid.RoomNotification;
import fi.vtt.nubomedia.kurentoroomclientandroid.RoomResponse;
import fi.vtt.nubomedia.utilitiesandroid.LooperExecutor;
import fi.vtt.nubomedia.webrtcpeerandroid.NBMMediaConfiguration;
import fi.vtt.nubomedia.webrtcpeerandroid.NBMWebRTCPeer;
import gpj.com.webrtcdemo.R;


public class WebRTCManager implements RoomListener {
    protected static final String TAG = "WebRTClog";

    //private static final String websocketUri = "wss://leisurely.org.cn:8443/room";
    //private static final String websocketUri = "wss://47.93.229.41:8443/room";
    //private static final String websocketUri = "ws://59.108.71.109:8443/room";
    //private static final String websocketUri = "ws://vc.xinshenshixun.com:28443/room";
    private static final String websocketUri = "wss://vc.xinshenshixun.com:28443/room";
    protected static final int JOIN_ROOM_ID = 120;
    protected static final int SEND_MESSAGE_ID = 121;
    protected static final int SEND_ON_ICACANDIDATE_ID = 122;
    protected static final int SEND_PUBLISH_VIDEO_ID = 123;
    protected static final int SEND_UN_PUBLISH_VIDEO_ID = 124;
    //public static final int SEND_RECEIVE_VIDEO_ID = 125;
    protected static final int SEND_UNSUBSCRIBE_VIDEO_ID = 126;
    protected static final int SEND_LEAVE_ROOM_ID = 127;
    protected static final int ERROR_SEND_LEAVE_ROOM_ID = 128;

    protected static final int ONE_TO_ONE_CHAT = 301;
    protected static final int MULTI_TO_MULTI_CHAT = 302;

    protected static final int REMOTE_SURFACE_NUMBER = 3;


    private WebRTCManager() {
    }

    private static class WebRTCManagerInstance {
        private static final WebRTCManager INSTANCE = new WebRTCManager();
    }

    public static WebRTCManager getInstance() {
        return WebRTCManagerInstance.INSTANCE;
    }

    protected enum CallState {
        INIT, JOIN_ROOM, IDLE, FINISH, SURFACE1_CLOSE, SURFACE2_CLOSE, SURFACE3_CLOSE
    }


    private Context mContext;
    private LooperExecutor mExecutor;// 用于处理和房间服务器交互的线程
    private KurentoRoomAPI mKurentoRoomAPI; // 用于和房间服务器交互的核心类
    protected volatile int mSendReceiveVideoID;// 向房间服务器发送请求流时的标识位，用于回调时确认返回流和连接id
    private String mLocalAccount;// 本地用户名
    private List<String> mOtherAccount;// 对方的用户名（连接id）  一对一时候取第0项

    private String mRoomId;// 房间号
    private int mChatMode; //ONE_TO_ONE_CHAT（一对一）；ONE_TO_ONE_CHAT（多对多）
    private int mCallType; //打电话或者接电话（ANSWER_PHONE_TYPE，OFFER_PHONE_TYPE）

    private volatile NBMWebRTCPeer mNbmWebRTCPeer;// 用于webrtc通信的核心类，所有点对点连接由它控制

    NBMMediaConfiguration mPeerConnectionParameters;// webrtc通信时音视频配置

    private MutableLiveData<CallState> mCallState;// 用于和Activity传递状态，用于和activity通信
    private Map<Integer, String> mVideoRequestUserMapping;// mSendReceiveVideoID 和连接id 映射关系
    private volatile ChatUser[] mSurfaceUserArray;//Surface 和连接用户对于关系，数组下标对应 Surface，例如0 对应Surface1

    private Handler mHandler;// 用于回调主线程函数
    private Map<String, Boolean> userPublishList;// 是否请求过用户流的映射关系

    private AsyncPlayer asyncPlayer; // 用于播放铃声等音乐


    public WebRTCManager init(Context context) {
        if (mContext == null)
            mContext = context.getApplicationContext();
        if (mExecutor == null)
            mExecutor = new LooperExecutor();
        mExecutor.requestStart();
        if (mKurentoRoomAPI == null)
            mKurentoRoomAPI = new KurentoRoomAPI(mExecutor, websocketUri, this);
        if (!mKurentoRoomAPI.isWebSocketConnected())
            mKurentoRoomAPI.connectWebSocket();
        if (mPeerConnectionParameters == null) {
            mPeerConnectionParameters = new NBMMediaConfiguration(
                    NBMMediaConfiguration.NBMRendererType.OPENGLES,
                    NBMMediaConfiguration.NBMAudioCodec.OPUS, 0,
                    NBMMediaConfiguration.NBMVideoCodec.VP9, 0,
                    new NBMMediaConfiguration.NBMVideoFormat(640, 480, PixelFormat.RGBX_8888, 15),
                    NBMMediaConfiguration.NBMCameraPosition.FRONT);
        }
        if (asyncPlayer == null) {
            asyncPlayer = new AsyncPlayer("webrtc");
        }
        mSendReceiveVideoID = 0;
        mOtherAccount = new LinkedList<>();
        mLocalAccount = "";
        mRoomId = "";
        mCallState = new MutableLiveData<>();
        mCallState.postValue(CallState.IDLE);
        mNbmWebRTCPeer = null;
        mVideoRequestUserMapping = new HashMap<>();
        userPublishList = new ConcurrentHashMap<>();
        mHandler = new Handler();
        mSurfaceUserArray = new ChatUser[REMOTE_SURFACE_NUMBER];
        return this;
    }

    protected WebRTCManager setLocalAccount(String localAccount) {
        mLocalAccount = localAccount;
        return this;
    }

    public String getLocalAccount() {
        return mLocalAccount;
    }

    protected WebRTCManager addOtherAccount(String localAccount) {
        mOtherAccount.add(localAccount);
        return this;
    }

    protected WebRTCManager setCallType(int callType) {
        mCallType = callType;
        return this;
    }

    protected WebRTCManager setRoom(String room) {
        this.mRoomId = room;
        return this;
    }

    public String getRoomId() {
        return mRoomId;
    }

    protected WebRTCManager setChatMode(int chatMode) {
        mChatMode = chatMode;
        return this;
    }

    protected int getCallType() {
        return mCallType;
    }

    protected NBMMediaConfiguration getPeerConnectionParameters() {
        return mPeerConnectionParameters;
    }

    protected Handler getHandler() {
        return mHandler;
    }

    protected List<String> getOtherAccount() {
        return mOtherAccount;
    }

    protected LiveData<CallState> getCallState() {
        return mCallState;
    }

    protected void setCallState(CallState callState) {
        mCallState.postValue(callState);
    }


    protected Map<Integer, String> getVideoRequestUserMapping() {
        return mVideoRequestUserMapping;
    }

    protected ChatUser[] getSurfaceUserArray() {
        return mSurfaceUserArray;
    }

    @Override
    public void onRoomResponse(RoomResponse response) {

        if (response.getMethod() != null) {
            switch (response.getMethod()) {
                case JOIN_ROOM:
                    Log.i(TAG, "onRoomResponse: 加入房间反馈");
                    userPublishList.putAll(response.getUsers());
                    mCallState.postValue(CallState.JOIN_ROOM);
                    return;

                case PUBLISH_VIDEO:
                    Log.i(TAG, "onRoomResponse: 开始推流反馈");
                    break;
                case UNPUBLISH_VIDEO:
                    Log.i(TAG, "onRoomResponse: 停止推流反馈");
                    break;
                case RECEIVE_VIDEO:
                    Log.i(TAG, "onRoomResponse: 拉流反馈");
                    break;
                case STOP_RECEIVE_VIDEO:
                    Log.i(TAG, "onRoomResponse: 停止拉流反馈");
                    break;

            }
        }

        switch (response.getId()) {
            case JOIN_ROOM_ID:
                Log.i(TAG, "onRoomResponse: JOIN_ROOM_ID");
                return;

            case SEND_ON_ICACANDIDATE_ID:
                Log.i(TAG, "onRoomResponse: SEND_ON_ICACANDIDATE_ID");
                break;
            case SEND_PUBLISH_VIDEO_ID:
                Log.i(TAG, "onRoomResponse: SEND_PUBLISH_VIDEO_ID");
                SessionDescription sd = new SessionDescription(SessionDescription.Type.ANSWER,
                        response.getValue("sdpAnswer").get(0));
                mNbmWebRTCPeer.processAnswer(sd, "local");
                mHandler.postDelayed(offerWhenReady, 1000);
                break;
            case ERROR_SEND_LEAVE_ROOM_ID:
                Log.i(TAG, "onRoomResponse: ERROR_SEND_LEAVE_ROOM_ID");
                joinRoom(mRoomId);
                break;
        }

        for (Map.Entry<Integer, String> e : mVideoRequestUserMapping.entrySet()) {
            if (e.getKey().equals(response.getId())) {
                Log.i(TAG, "onRoomResponse: mSendReceiveVideoID  " + mSendReceiveVideoID);
                SessionDescription sd = new SessionDescription(SessionDescription.Type.ANSWER,
                        response.getValue("sdpAnswer").get(0));

                mNbmWebRTCPeer.processAnswer(sd, e.getValue());
            }
        }


    }

    @Override
    public void onRoomError(RoomError error) {
        Log.e(TAG, "Room error ：" + error);
        switch (error.getCode()) {
            case 104:
                Log.i(TAG, "Room error case：" + 104);
                mKurentoRoomAPI.sendLeaveRoom(ERROR_SEND_LEAVE_ROOM_ID);
                break;
        }
    }

    @Override
    public void onRoomNotification(RoomNotification notification) {
        Map<String, Object> map = notification.getParams();

        final String user = map.get("user") != null ? map.get("user").toString() : "";
        final String id = map.get("id") != null ? map.get("id").toString() : "";
        final String name = map.get("name") != null ? map.get("name").toString() : "";
        switch (notification.getMethod()) {
            case RoomListener.METHOD_PARTICIPANT_JOINED:
                Log.i(TAG, "onRoomNotification: " + id + "加入房间");
                userPublishList.put(id, false);
                break;
            case RoomListener.METHOD_PARTICIPANT_PUBLISHED:
                Log.i(TAG, "onRoomNotification: " + id + "推流");
                userPublishList.put(id, true);
                mHandler.postDelayed(offerWhenReady, 1000);
                break;
            case RoomListener.METHOD_PARTICIPANT_UNPUBLISHED:
                Log.i(TAG, "onRoomNotification: 有人停止推流");
                userPublishList.put(name, false);
                closeSurface(name);
                break;
            case RoomListener.METHOD_ICE_CANDIDATE:
                Log.i(TAG, "onRoomNotification: 有新的ICE候选");

                String sdpMid = map.get("sdpMid").toString();
                int sdpMLineIndex = Integer.valueOf(map.get("sdpMLineIndex").toString());
                String sdp = map.get("candidate").toString();
                IceCandidate ic = new IceCandidate(sdpMid, sdpMLineIndex, sdp);
                String endpointName = notification.getParam("endpointName").toString();
                if (endpointName.equals(mLocalAccount)) {
                    mNbmWebRTCPeer.addRemoteIceCandidate(ic, "local");
                } else {
                    mNbmWebRTCPeer.addRemoteIceCandidate(ic, notification.getParam("endpointName").toString());
                }

                break;
            case RoomListener.METHOD_PARTICIPANT_LEFT:
                Log.i(TAG, "onRoomNotification: " + name + "离开房间");
                userPublishList.remove(name);
                closeSurface(name);
                if (mChatMode == ONE_TO_ONE_CHAT)
                    hungUp();
                else {
                    mNbmWebRTCPeer.closeConnection(name);
                }
                break;
            case RoomListener.METHOD_SEND_MESSAGE:
                final String message = map.get("message").toString();
                Log.i(TAG, "onRoomNotification: 有消息。" + user + "->" + message);
                break;
            case RoomListener.METHOD_MEDIA_ERROR:
                Log.e(TAG, "onRoomNotification: 多媒体错误");
                break;
            default:
                break;
        }
    }

    @Override
    public void onRoomConnected() {
        Log.d(TAG, "onRoomConnected()");

    }

    protected void joinRoom(String roomid) {
        mRoomId = roomid;
        Log.i(TAG, "Joinroom: User: " + this.mLocalAccount + ", Room: " + mRoomId + " id:" + JOIN_ROOM_ID);
        if (mKurentoRoomAPI.isWebSocketConnected()) {
            Log.i(TAG, "——————————");

            mKurentoRoomAPI.sendJoinRoom(this.mLocalAccount, mRoomId, true, JOIN_ROOM_ID);
        }
    }

    @Override
    public void onRoomDisconnected() {
        Log.d(TAG, "onRoomDisconnected()");
//        if (!mKurentoRoomAPI.isWebSocketConnected())
//            mKurentoRoomAPI.connectWebSocket();

    }

    protected KurentoRoomAPI getKurentoRoomAPI() {
        return mKurentoRoomAPI;
    }

    protected Map<String, Boolean> getUserPublishList() {
        return userPublishList;
    }

    protected WebRTCManager setNbmWebRTCPeer(NBMWebRTCPeer nbmWebRTCPeer) {
        mNbmWebRTCPeer = nbmWebRTCPeer;
        return this;
    }

    protected void playCall(Context context) {
        Uri uri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.avchat_ring);
        asyncPlayer = new AsyncPlayer("my");
        asyncPlayer.play(context, uri, true, AudioManager.STREAM_VOICE_CALL);

    }

    protected void stopCall() {
        asyncPlayer.stop();

    }

    private Runnable offerWhenReady = new Runnable() {
        @Override
        public void run() {
            for (Map.Entry<String, Boolean> entry : userPublishList.entrySet()) {
                if (entry.getValue()) {
                    Log.i(TAG, "offerWhenReady");
                    Log.i(TAG, "I'm " + entry.getKey() + " DERP: Generating offer for peer " + entry.getKey());
                    mNbmWebRTCPeer.generateOffer(entry.getKey(), false);

                    entry.setValue(false);
                }
            }
        }
    };

    protected void hungUp() {
        mKurentoRoomAPI.sendUnpublishVideo(SEND_UN_PUBLISH_VIDEO_ID);
        mKurentoRoomAPI.sendLeaveRoom(SEND_LEAVE_ROOM_ID);
        try {
            if (mNbmWebRTCPeer != null) {
                mNbmWebRTCPeer.stopLocalMedia();
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }

        mCallState.postValue(CallState.FINISH);
    }

    protected void closeSurface(String id) {
        for (int i = 0; i < REMOTE_SURFACE_NUMBER; i++) {
            if (mSurfaceUserArray[i] != null && mSurfaceUserArray[i].getId().equals(id)) {
                switch (i) {
                    case 0:
                        mCallState.postValue(CallState.SURFACE1_CLOSE);
                        break;
                    case 1:
                        mCallState.postValue(CallState.SURFACE2_CLOSE);
                        break;
                    case 2:
                        mCallState.postValue(CallState.SURFACE3_CLOSE);
                        break;
                }
                mSurfaceUserArray[i] = null;
                return;
            }
        }

    }


    // 外部调用webRTCCall()时所传callType参数
    public static final int ANSWER_PHONE_TYPE = 201; //接电话
    public static final int OFFER_PHONE_TYPE = 202; // 打电话

    /**
     * 发起一对一音视频通话呼叫
     *
     * @param context      上下文
     * @param callType     打或接电话 （ANSWER_PHONE_TYPE，OFFER_PHONE_TYPE）
     * @param localAccount 本地账号
     * @param otherAccount 对方账号
     * @param otherName    对方昵称
     */
    public static void webRTCCall(Context context, Integer callType, String localAccount, String otherAccount, String otherName) {
        getInstance().init(context)
                .setLocalAccount(localAccount)
                .addOtherAccount(otherAccount)
                .setCallType(callType)
                .setChatMode(ONE_TO_ONE_CHAT);

        AVChatWebRTCActivity.outgoingCall(context, otherName);
    }

    /**
     * 发起多对多音视频通话呼叫
     *
     * @param context      上下文
     * @param callType     打或接电话 （ANSWER_PHONE_TYPE，OFFER_PHONE_TYPE）
     * @param localAccount 本地账号
     * @param roomName     房间名
     */
    public static void webRTCMultiplayerCall(Context context, Integer callType, String localAccount, String roomName) {
        getInstance().init(context)
                .setLocalAccount(localAccount)
                .setCallType(callType)
                .setRoom(roomName)
                .setChatMode(MULTI_TO_MULTI_CHAT);
        AVChatWebRTCMultiplayerActivity.outgoingMultiplayerCall(context);
    }


}

