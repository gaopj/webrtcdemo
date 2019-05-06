package gpj.com.webrtcdemo.webrtc;


import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.LifecycleRegistryOwner;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RendererCommon;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import fi.vtt.nubomedia.webrtcpeerandroid.NBMPeerConnection;
import fi.vtt.nubomedia.webrtcpeerandroid.NBMWebRTCPeer;
import gpj.com.webrtcdemo.R;

import static gpj.com.webrtcdemo.webrtc.WebRTCManager.ANSWER_PHONE_TYPE;
import static gpj.com.webrtcdemo.webrtc.WebRTCManager.OFFER_PHONE_TYPE;
import static gpj.com.webrtcdemo.webrtc.WebRTCManager.SEND_ON_ICACANDIDATE_ID;
import static gpj.com.webrtcdemo.webrtc.WebRTCManager.SEND_PUBLISH_VIDEO_ID;


public class AVChatWebRTCActivity extends AppCompatActivity implements NBMWebRTCPeer.Observer, LifecycleRegistryOwner {
    private static final String TAG = WebRTCManager.TAG;
    private static final String KEY_OTHER_NAME = "KEY_OTHER_NAME";

    private LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(this);
    private WebRTCManager mWebRTCManager;
    private volatile NBMWebRTCPeer mNbmWebRTCPeer;

    private SurfaceViewRenderer mMasterView;
    private SurfaceViewRenderer mLocalView;
    private TextView callStatusTxt;
    private TextView callNameTxt;
    private ImageButton mCallButton;
    private ImageButton mHangupButton;


    private String mOtherName; // 对方昵称
    private Observer<WebRTCManager.CallState> statusObserver;

    protected static void outgoingCall(Context context, String otherName) {
        Intent intent = new Intent();
        intent.setClass(context, AVChatWebRTCActivity.class);
        intent.putExtra(KEY_OTHER_NAME, otherName);

        context.startActivity(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mMasterView = findViewById(R.id.gl_surface);
        mLocalView = findViewById(R.id.gl_surface_local);
        callStatusTxt = findViewById(R.id.call_status);
        callNameTxt = findViewById(R.id.call_name);
        mCallButton = findViewById(R.id.answerCallButton);
        mCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                joinRoom(v);
                mWebRTCManager.stopCall();
            }
        });

        mHangupButton = findViewById(R.id.hangUpButton);
        mHangupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
        mWebRTCManager = WebRTCManager.getInstance();
        statusObserver = new Observer<WebRTCManager.CallState>() {
            @Override
            public void onChanged(@Nullable final WebRTCManager.CallState state) {
                if (state != null) {
                    Log.i(TAG, "切换状态：" + state);
                    switch (state) {
                        case INIT:
                            callStatusTxt.setText("初始化...");
                        case IDLE:
                            callStatusTxt.setText("");
                            break;
                        case JOIN_ROOM:
                            if (mNbmWebRTCPeer != null)
                                mNbmWebRTCPeer.generateOffer("local", true);
                            break;
                        case FINISH:
                            mMasterView.setVisibility(View.INVISIBLE);
                            mLocalView.setVisibility(View.INVISIBLE);
                            callStatusTxt.setText("通话结束");
                            break;
                    }
                }
            }
        };
        mWebRTCManager.getCallState().observe(this, statusObserver);

        this.mOtherName = getIntent().getStringExtra(KEY_OTHER_NAME);
        Log.i(TAG, "mOtherName:" + mOtherName);
        callNameTxt.setText(mOtherName);

        EglBase rootEglBase = EglBase.create();
        try {
            mMasterView.init(rootEglBase.getEglBaseContext(), null);
            mMasterView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            mLocalView.init(rootEglBase.getEglBaseContext(), null);
            mLocalView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        } catch (IllegalStateException e) {
            Log.e(TAG, e.getMessage());
        }


        mNbmWebRTCPeer = new NBMWebRTCPeer(mWebRTCManager.mPeerConnectionParameters, this, mLocalView, this);
        mWebRTCManager.setNbmWebRTCPeer(mNbmWebRTCPeer);
        mNbmWebRTCPeer.registerMasterRenderer(mMasterView);
        Log.i(TAG, "Initializing nbmWebRTCPeer...");
        mNbmWebRTCPeer.initialize();
        switch (mWebRTCManager.getCallType()) {
            case OFFER_PHONE_TYPE:
                callStatusView();
                break;
            case ANSWER_PHONE_TYPE:
                answerCallStatusView();
                break;
        }

        mWebRTCManager.setCallState(WebRTCManager.CallState.INIT);


    }

    @Override
    protected void onStop() {
        mWebRTCManager.hungUp();
        endCall();
        super.onStop();
    }

    @Override
    protected void onPause() {
        if (mNbmWebRTCPeer != null)
            mNbmWebRTCPeer.stopLocalMedia();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    protected void joinRoom(View view) {
        String roomId = "";
        switch (mWebRTCManager.getCallType()) {
            case OFFER_PHONE_TYPE:
                roomId = mWebRTCManager.getLocalAccount() + mWebRTCManager.getOtherAccount().get(0);
                break;
            case ANSWER_PHONE_TYPE:
                roomId = mWebRTCManager.getOtherAccount().get(0) + mWebRTCManager.getLocalAccount();
                break;
        }
        Log.i(TAG, "joinRoom() :" + roomId);
        mWebRTCManager.joinRoom(roomId);
        if (view != null)
            view.setVisibility(View.GONE);

    }

    /**
     * Terminates the current call and ends activity
     */
    private void endCall() {
        Log.i(TAG, "endCall() ");
        mWebRTCManager.stopCall();
        mWebRTCManager.setCallState(WebRTCManager.CallState.FINISH);
        try {

            mLocalView.release();
            mMasterView.release();
            mNbmWebRTCPeer.closeConnection("local");
            mNbmWebRTCPeer.closeConnection(mWebRTCManager.getOtherAccount().get(0));
//            if (mNbmWebRTCPeer != null) {
//                mNbmWebRTCPeer.close();
//            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void onInitialize() {
        Log.i(TAG, "onInitialize()");
        Log.i(TAG, "nbmWebRTCPeer.startLocalMedia()");
        boolean mediaSuccess = false;
        if (mNbmWebRTCPeer != null)
            try {
                mediaSuccess = mNbmWebRTCPeer.startLocalMedia();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        if (mWebRTCManager.getCallType() == OFFER_PHONE_TYPE) {
            joinRoom(null);
        }
        Log.i(TAG, "本地媒体准备：" + mediaSuccess);

    }

    @Override
    public void onLocalSdpOfferGenerated(final SessionDescription sessionDescription, final NBMPeerConnection nbmPeerConnection) {
        Log.i(TAG, "onLocalSdpOfferGenerated(): " + nbmPeerConnection.getConnectionId());

        String connectionId = nbmPeerConnection.getConnectionId();
        if (connectionId.equals("local")) {
            Log.d(TAG, "Sending " + sessionDescription.type + " " + mWebRTCManager.getCallState().getValue());

            mWebRTCManager.getKurentoRoomAPI().sendPublishVideo(sessionDescription.description, false, SEND_PUBLISH_VIDEO_ID);
            Log.i(TAG, "roomreq:" + "sendPublishVideo,id:" + SEND_PUBLISH_VIDEO_ID);
        } else { // Asking for remote user video
            Log.d(TAG, "Sending " + sessionDescription.type + " " + mWebRTCManager.getCallState().getValue());

            mWebRTCManager.getKurentoRoomAPI().sendReceiveVideoFrom(connectionId, connectionId + "webcam", sessionDescription.description, ++mWebRTCManager.mSendReceiveVideoID);
            mWebRTCManager.getVideoRequestUserMapping().put(mWebRTCManager.mSendReceiveVideoID, connectionId);
            Log.i(TAG, "roomreq:" + "sendReceiveVideoFrom,id" + mWebRTCManager.mSendReceiveVideoID);
        }
    }

    @Override
    public void onLocalSdpAnswerGenerated(SessionDescription sessionDescription, NBMPeerConnection nbmPeerConnection) {
        Log.i(TAG, "onLocalSdpAnswerGenerated");
    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate, NBMPeerConnection nbmPeerConnection) {
        Log.i(TAG, "onIceCandidate");
        Log.i(TAG, "onIceCandidate:callState= " + mWebRTCManager.getCallState());
        Log.i(TAG, "onIceCandidate:callState= " + iceCandidate.sdpMid);
        Log.i(TAG, "onIceCandidate:callState= " + iceCandidate.sdpMLineIndex);
        Log.i(TAG, "onIceCandidate:callState= " + iceCandidate.sdp);

        String connectionId = nbmPeerConnection.getConnectionId();

        if (connectionId.equals("local")) {
            mWebRTCManager.getKurentoRoomAPI().sendOnIceCandidate(mWebRTCManager.getLocalAccount(), iceCandidate.sdp,
                    iceCandidate.sdpMid, Integer.toString(iceCandidate.sdpMLineIndex), SEND_ON_ICACANDIDATE_ID);
            Log.i(TAG, "roomreq:" + "sendOnIceCandidate,id" + SEND_ON_ICACANDIDATE_ID);
        } else {
            mWebRTCManager.getKurentoRoomAPI().sendOnIceCandidate(nbmPeerConnection.getConnectionId(), iceCandidate.sdp,
                    iceCandidate.sdpMid, Integer.toString(iceCandidate.sdpMLineIndex), SEND_ON_ICACANDIDATE_ID);
            Log.i(TAG, "roomreq:" + "sendOnIceCandidate,id" + SEND_ON_ICACANDIDATE_ID);
        }
    }

    @Override
    public void onIceStatusChanged(PeerConnection.IceConnectionState iceConnectionState, NBMPeerConnection nbmPeerConnection) {
        Log.i(TAG, "onIceStatusChanged");
    }

    @Override
    public void onRemoteStreamAdded(MediaStream mediaStream, NBMPeerConnection nbmPeerConnection) {
        Log.i(TAG, "  :" + nbmPeerConnection.getConnectionId()
                + ",mediaStream:" + mediaStream.videoTracks.size());
        final String id = nbmPeerConnection.getConnectionId();
        if (id.equals("local"))
            return;
        mNbmWebRTCPeer.setActiveMasterStream(mediaStream);
        mWebRTCManager.setCallState(WebRTCManager.CallState.IDLE);
        //mNbmWebRTCPeer.attachRendererToRemoteStream(mMasterView, mediaStream);
    }

    @Override
    public void onRemoteStreamRemoved(MediaStream mediaStream, NBMPeerConnection nbmPeerConnection) {
        Log.i(TAG, "onRemoteStreamRemoved");
    }

    @Override
    public void onPeerConnectionError(String s) {
        Log.e(TAG, "onPeerConnectionError:" + s);
    }

    @Override
    public void onDataChannel(DataChannel dataChannel, NBMPeerConnection connection) {
        Log.i(TAG, "[datachannel] Peer opened data channel");
    }

    @Override
    public void onBufferedAmountChange(long l, NBMPeerConnection connection, DataChannel channel) {
        Log.i(TAG, "onBufferedAmountChange");
    }

    protected void sendHelloMessage(DataChannel channel) {
        byte[] rawMessage = "Hello Peer!".getBytes(Charset.forName("UTF-8"));
        ByteBuffer directData = ByteBuffer.allocateDirect(rawMessage.length);
        directData.put(rawMessage);
        directData.flip();
        DataChannel.Buffer data = new DataChannel.Buffer(directData, false);
        channel.send(data);
    }

    @Override
    public void onStateChange(NBMPeerConnection connection, DataChannel channel) {
        Log.i(TAG, "[datachannel] DataChannel onStateChange: " + channel.state());
        if (channel.state() == DataChannel.State.OPEN) {
            sendHelloMessage(channel);
            Log.i(TAG, "[datachannel] Datachannel open, sending first hello");
        }
    }

    @Override
    public void onMessage(DataChannel.Buffer buffer, NBMPeerConnection connection, DataChannel channel) {
        Log.i(TAG, "[datachannel] Message received: " + buffer.toString());
        sendHelloMessage(channel);
    }

    private void callStatusView() {
        mMasterView.setVisibility(View.VISIBLE);
        mLocalView.setVisibility(View.VISIBLE);
        mCallButton.setVisibility(View.GONE);
        mHangupButton.setVisibility(View.VISIBLE);

    }

    private void answerCallStatusView() {
        mWebRTCManager.playCall(this);
        mMasterView.setVisibility(View.VISIBLE);
        mLocalView.setVisibility(View.VISIBLE);
        mCallButton.setVisibility(View.VISIBLE);
        mHangupButton.setVisibility(View.VISIBLE);
    }


    @NonNull
    @Override
    public LifecycleRegistry getLifecycle() {
        return lifecycleRegistry;
    }
}

