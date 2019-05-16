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
import static gpj.com.webrtcdemo.webrtc.WebRTCManager.REMOTE_SURFACE_NUMBER;
import static gpj.com.webrtcdemo.webrtc.WebRTCManager.SEND_ON_ICACANDIDATE_ID;
import static gpj.com.webrtcdemo.webrtc.WebRTCManager.SEND_PUBLISH_VIDEO_ID;

public class AVChatWebRTCMultiplayerActivity extends AppCompatActivity implements NBMWebRTCPeer.Observer, LifecycleRegistryOwner {
    private static final String TAG = WebRTCManager.TAG;


    private LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(this);
    private WebRTCManager mWebRTCManager;
    private volatile NBMWebRTCPeer mNbmWebRTCPeer;


    private SurfaceViewRenderer mLocalView;
    private SurfaceViewRenderer mRemoteView1;
    private SurfaceViewRenderer mRemoteView2;
    private SurfaceViewRenderer mRemoteView3;
    private SurfaceViewRenderer mRemoteView4;
    private SurfaceViewRenderer mRemoteView5;
    private SurfaceViewRenderer mRemoteView6;
    private SurfaceViewRenderer mRemoteView7;
    private SurfaceViewRenderer mRemoteView8;
    private TextView callStatusTxt;
    private ImageButton mCallButton;
    private ImageButton mHangupButton;

    private Observer<WebRTCManager.CallState> statusObserver;

    protected static void outgoingMultiplayerCall(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, AVChatWebRTCMultiplayerActivity.class);

        context.startActivity(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer_video_chat);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mLocalView = findViewById(R.id.gl_surface_local);

        mRemoteView1 = findViewById(R.id.gl_surface_1);
        mRemoteView2 = findViewById(R.id.gl_surface_2);
        mRemoteView3 = findViewById(R.id.gl_surface_3);
        mRemoteView4 = findViewById(R.id.gl_surface_4);
        mRemoteView5 = findViewById(R.id.gl_surface_5);
        mRemoteView6 = findViewById(R.id.gl_surface_6);
        mRemoteView7 = findViewById(R.id.gl_surface_7);
        mRemoteView8 = findViewById(R.id.gl_surface_8);

        callStatusTxt = findViewById(R.id.call_status);
        mCallButton = findViewById(R.id.answerCallButton);
        mCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                joinRoom(v);
            }
        });

        mHangupButton = findViewById(R.id.hangUpButton);
        mHangupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hangup();
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
                            break;
                        case JOIN_ROOM:
                            if (mNbmWebRTCPeer != null)
                                mNbmWebRTCPeer.generateOffer("local", true);
                            break;
                        case IDLE:
                            callStatusTxt.setText("等待对方连接");
                            break;
                        case CONNECT:
                            callStatusTxt.setText("");
                            break;
                        case FINISH:
                            callStatusTxt.setText("通话结束");
                            break;
                        case SURFACE1_CLOSE:
                            mRemoteView1.setVisibility(View.INVISIBLE);
                            break;
                        case SURFACE2_CLOSE:
                            mRemoteView2.setVisibility(View.INVISIBLE);
                            break;
                        case SURFACE3_CLOSE:
                            mRemoteView3.setVisibility(View.INVISIBLE);
                            break;
                        case SURFACE4_CLOSE:
                            mRemoteView4.setVisibility(View.INVISIBLE);
                            break;
                        case SURFACE5_CLOSE:
                            mRemoteView5.setVisibility(View.INVISIBLE);
                            break;
                        case SURFACE6_CLOSE:
                            mRemoteView6.setVisibility(View.INVISIBLE);
                            break;
                        case SURFACE7_CLOSE:
                            mRemoteView7.setVisibility(View.INVISIBLE);
                            break;
                        case SURFACE8_CLOSE:
                            mRemoteView8.setVisibility(View.INVISIBLE);
                            break;
                    }
                }
            }
        };
        mWebRTCManager.getCallState().observe(this, statusObserver);
        EglBase rootEglBase = EglBase.create();
        try {
            mLocalView.init(rootEglBase.getEglBaseContext(), null);
            mLocalView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            mRemoteView1.init(rootEglBase.getEglBaseContext(), null);
            mRemoteView1.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            mRemoteView2.init(rootEglBase.getEglBaseContext(), null);
            mRemoteView2.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            mRemoteView3.init(rootEglBase.getEglBaseContext(), null);
            mRemoteView3.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            mRemoteView4.init(rootEglBase.getEglBaseContext(), null);
            mRemoteView4.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            mRemoteView5.init(rootEglBase.getEglBaseContext(), null);
            mRemoteView5.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            mRemoteView6.init(rootEglBase.getEglBaseContext(), null);
            mRemoteView6.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            mRemoteView7.init(rootEglBase.getEglBaseContext(), null);
            mRemoteView7.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            mRemoteView8.init(rootEglBase.getEglBaseContext(), null);
            mRemoteView8.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        } catch (IllegalStateException e) {
            Log.e(TAG, e.getMessage());
        }
        mNbmWebRTCPeer = new NBMWebRTCPeer(mWebRTCManager.getPeerConnectionParameters(), this, mLocalView, this);
        mWebRTCManager.setNbmWebRTCPeer(mNbmWebRTCPeer);

        mWebRTCManager.initPeer = false;
        if (mWebRTCManager.getKurentoRoomAPI().isWebSocketConnected()) {
            Log.i(TAG, "Initializing nbmWebRTCPeer...");
            mNbmWebRTCPeer.initialize();
            mWebRTCManager.initPeer = true;
        }

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

    protected void hangup() {
        mWebRTCManager.hungUp();
        endCall();
        finish();
    }

    protected void joinRoom(View view) {
        mWebRTCManager.joinRoom(mWebRTCManager.getRoomId());
        if (view != null)
            view.setVisibility(View.GONE);
    }

    /**
     * Terminates the current call and ends activity
     */
    private void endCall() {
        Log.i(TAG, "endCall() ");
        mWebRTCManager.setCallState(WebRTCManager.CallState.FINISH);
        try {
            if (mNbmWebRTCPeer != null) {
                mNbmWebRTCPeer.close();
                mNbmWebRTCPeer = null;
                mWebRTCManager.setNbmWebRTCPeer(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        // Log.i(TAG, "onLocalSdpOfferGenerated(): "+sessionDescription.description);

        String connectionId = nbmPeerConnection.getConnectionId();
        if (connectionId.equals("local")) {
            Log.d(TAG, "Sending " + sessionDescription.type + " " + mWebRTCManager.getCallState().getValue());
            mWebRTCManager.getKurentoRoomAPI().sendPublishVideo(sessionDescription.description, false, SEND_PUBLISH_VIDEO_ID);
            Log.i(TAG, "roomreq:" + "sendPublishVideo,id:" + SEND_PUBLISH_VIDEO_ID);
        } else {
            Log.d(TAG, "Sending " + sessionDescription.type + " " + mWebRTCManager.getCallState().getValue());
            mWebRTCManager.getKurentoRoomAPI().sendReceiveVideoFrom(connectionId, connectionId + "webcam", sessionDescription.description, ++mWebRTCManager.mSendReceiveVideoID);
            // mWebRTCManager.getLocalOfferMap().put(connectionId,sessionDescription.description);
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
        Log.i(TAG, "onIceCandidate:"+iceCandidate.toString());

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
    public void onRemoteStreamAdded(final MediaStream mediaStream, NBMPeerConnection nbmPeerConnection) {
        Log.i(TAG, "onRemoteStreamAdded:" + nbmPeerConnection.getConnectionId()
                + ",mediaStream:" + mediaStream.videoTracks.size());

        final String id = nbmPeerConnection.getConnectionId();
        if (id.equals("local")) {
            mWebRTCManager.setCallState(WebRTCManager.CallState.IDLE);
            return;
        }
        if (mWebRTCManager.getCallState().getValue() != WebRTCManager.CallState.CONNECT)
            mWebRTCManager.setCallState(WebRTCManager.CallState.CONNECT);
        mWebRTCManager.getHandler().post(new Runnable() {
            @Override
            public void run() {
                bindSurface(mediaStream, id);
            }
        });


    }

    @Override
    public void onRemoteStreamRemoved(MediaStream mediaStream, final NBMPeerConnection nbmPeerConnection) {
        Log.i(TAG, "onRemoteStreamRemoved");
        mWebRTCManager.getHandler().post(new Runnable() {
            @Override
            public void run() {
                unbindSurface(nbmPeerConnection.getConnectionId());
            }
        });

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
        mRemoteView1.setVisibility(View.VISIBLE);
        mRemoteView2.setVisibility(View.VISIBLE);
        mRemoteView3.setVisibility(View.VISIBLE);
        mRemoteView4.setVisibility(View.VISIBLE);
        mRemoteView5.setVisibility(View.VISIBLE);
        mRemoteView6.setVisibility(View.VISIBLE);
        mRemoteView7.setVisibility(View.VISIBLE);
        mRemoteView8.setVisibility(View.VISIBLE);

        mLocalView.setVisibility(View.VISIBLE);
        mCallButton.setVisibility(View.GONE);
        mHangupButton.setVisibility(View.VISIBLE);
    }

    private void answerCallStatusView() {
        mRemoteView1.setVisibility(View.VISIBLE);
        mRemoteView2.setVisibility(View.VISIBLE);
        mRemoteView3.setVisibility(View.VISIBLE);
        mRemoteView4.setVisibility(View.VISIBLE);
        mRemoteView5.setVisibility(View.VISIBLE);
        mRemoteView6.setVisibility(View.VISIBLE);
        mRemoteView7.setVisibility(View.VISIBLE);
        mRemoteView8.setVisibility(View.VISIBLE);
        mLocalView.setVisibility(View.VISIBLE);
        mCallButton.setVisibility(View.VISIBLE);
        mHangupButton.setVisibility(View.VISIBLE);
    }

    @NonNull
    @Override
    public LifecycleRegistry getLifecycle() {
        return lifecycleRegistry;
    }

    private synchronized void bindSurface(MediaStream mediaStream, String id) {

        for (int i = 0; i < REMOTE_SURFACE_NUMBER; i++) {
            if (mWebRTCManager.getSurfaceUserArray()[i] != null && mWebRTCManager.getSurfaceUserArray()[i].getId().equals(id)) {
                switch (i) {
                    case 0:
                        mRemoteView1.setVisibility(View.VISIBLE);
                        mNbmWebRTCPeer.attachRendererToRemoteStream(mRemoteView1, mediaStream);
                        break;
                    case 1:
                        mRemoteView2.setVisibility(View.VISIBLE);
                        mNbmWebRTCPeer.attachRendererToRemoteStream(mRemoteView2, mediaStream);
                        break;
                    case 2:
                        mRemoteView3.setVisibility(View.VISIBLE);
                        mNbmWebRTCPeer.attachRendererToRemoteStream(mRemoteView3, mediaStream);
                        break;
                    case 3:
                        mRemoteView4.setVisibility(View.VISIBLE);
                        mNbmWebRTCPeer.attachRendererToRemoteStream(mRemoteView4, mediaStream);
                        break;
                    case 4:
                        mRemoteView5.setVisibility(View.VISIBLE);
                        mNbmWebRTCPeer.attachRendererToRemoteStream(mRemoteView5, mediaStream);
                        break;
                    case 5:
                        mRemoteView6.setVisibility(View.VISIBLE);
                        mNbmWebRTCPeer.attachRendererToRemoteStream(mRemoteView6, mediaStream);
                        break;
                    case 6:
                        mRemoteView7.setVisibility(View.VISIBLE);
                        mNbmWebRTCPeer.attachRendererToRemoteStream(mRemoteView7, mediaStream);
                        break;
                    case 7:
                        mRemoteView8.setVisibility(View.VISIBLE);
                        mNbmWebRTCPeer.attachRendererToRemoteStream(mRemoteView8, mediaStream);
                        break;

                }

                mWebRTCManager.getSurfaceUserArray()[i].setMediaStream(mediaStream);
                return;
            }
        }

        ChatUser chatUser = new ChatUser();
        chatUser.setId(id);
        chatUser.setMediaStream(mediaStream);

        for (int i = 0; i < REMOTE_SURFACE_NUMBER; i++) {
            if (mWebRTCManager.getSurfaceUserArray()[i] == null) {
                switch (i) {
                    case 0:
                        mRemoteView1.setVisibility(View.VISIBLE);
                        mNbmWebRTCPeer.attachRendererToRemoteStream(mRemoteView1, mediaStream);
                        break;
                    case 1:
                        mRemoteView2.setVisibility(View.VISIBLE);
                        mNbmWebRTCPeer.attachRendererToRemoteStream(mRemoteView2, mediaStream);
                        break;
                    case 2:
                        mRemoteView3.setVisibility(View.VISIBLE);
                        mNbmWebRTCPeer.attachRendererToRemoteStream(mRemoteView3, mediaStream);
                        break;
                    case 3:
                        mRemoteView4.setVisibility(View.VISIBLE);
                        mNbmWebRTCPeer.attachRendererToRemoteStream(mRemoteView4, mediaStream);
                        break;
                    case 4:
                        mRemoteView5.setVisibility(View.VISIBLE);
                        mNbmWebRTCPeer.attachRendererToRemoteStream(mRemoteView5, mediaStream);
                        break;
                    case 5:
                        mRemoteView6.setVisibility(View.VISIBLE);
                        mNbmWebRTCPeer.attachRendererToRemoteStream(mRemoteView6, mediaStream);
                        break;
                    case 6:
                        mRemoteView7.setVisibility(View.VISIBLE);
                        mNbmWebRTCPeer.attachRendererToRemoteStream(mRemoteView7, mediaStream);
                        break;
                    case 7:
                        mRemoteView8.setVisibility(View.VISIBLE);
                        mNbmWebRTCPeer.attachRendererToRemoteStream(mRemoteView8, mediaStream);
                        break;

                }
                mWebRTCManager.getSurfaceUserArray()[i] = chatUser;
                return;
            }
        }
    }

    private synchronized void unbindSurface(String id) {
        for (int i = 0; i < REMOTE_SURFACE_NUMBER; i++) {
            if (mWebRTCManager.getSurfaceUserArray()[i] != null && mWebRTCManager.getSurfaceUserArray()[i].getId().equals(id)) {
                mWebRTCManager.getSurfaceUserArray()[i] = null;
                switch (i) {
                    case 0:
                        mRemoteView1.setVisibility(View.INVISIBLE);
                        break;
                    case 1:
                        mRemoteView2.setVisibility(View.INVISIBLE);
                        break;
                    case 2:
                        mRemoteView3.setVisibility(View.INVISIBLE);
                        break;
                    case 3:
                        mRemoteView4.setVisibility(View.INVISIBLE);
                        break;
                    case 4:
                        mRemoteView5.setVisibility(View.INVISIBLE);
                        break;
                    case 5:
                        mRemoteView6.setVisibility(View.INVISIBLE);
                        break;
                    case 6:
                        mRemoteView7.setVisibility(View.INVISIBLE);
                        break;
                    case 7:
                        mRemoteView8.setVisibility(View.INVISIBLE);
                        break;
                }

                return;
            }
        }
    }
}