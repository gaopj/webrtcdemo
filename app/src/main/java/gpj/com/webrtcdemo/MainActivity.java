package gpj.com.webrtcdemo;

import android.Manifest;
import android.media.AsyncPlayer;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import gpj.com.webrtcdemo.webrtc.WebRTCManager;

import static gpj.com.webrtcdemo.webrtc.WebRTCManager.ANSWER_PHONE_TYPE;
import static gpj.com.webrtcdemo.webrtc.WebRTCManager.OFFER_PHONE_TYPE;


public class MainActivity extends AppCompatActivity {

    Button mButton;
    Button mButton2;
    Button mButton3;
    Button mButton4;
    Button mButton5;
    Button mButton6;

    EditText localEdt;
    EditText otherEdt;
    EditText roomEdt;
    String localName;
    String otherName;
    String roomName;

    AsyncPlayer asyncPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 0);
        }
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 0);
        }


        localEdt = findViewById(R.id.localEdt);
        otherEdt = findViewById(R.id.otherEdt);
        roomEdt = findViewById(R.id.roomEdt);

        WebRTCManager.getInstance().init(this);
        mButton = findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(localEdt.getText() ==null||localEdt.getText().toString().equals("")){
                    localName = "aa1";
                }else{
                    localName = localEdt.getText().toString();
                }

                if(otherEdt.getText() ==null||otherEdt.getText().toString().equals("")){
                    otherName = "bb1";
                }else{
                    otherName = otherEdt.getText().toString();
                }
                WebRTCManager.webRTCCall(MainActivity.this, OFFER_PHONE_TYPE, localName, otherName, "hahah");
            }
        });

        mButton2 = findViewById(R.id.button2);
        mButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(localEdt.getText() ==null||localEdt.getText().toString().equals("")){
                    localName = "bb1";
                }else{
                    localName = localEdt.getText().toString();
                }

                if(otherEdt.getText() ==null||otherEdt.getText().toString().equals("")){
                    otherName = "aa1";
                }else{
                    otherName = otherEdt.getText().toString();
                }
                WebRTCManager.webRTCCall(MainActivity.this, ANSWER_PHONE_TYPE, localName, otherName, "lalala");
            }
        });

        final String name = "a " + (int) (1 + Math.random() * (1000));
        mButton3 = findViewById(R.id.button3);
        mButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(roomEdt.getText() ==null||roomEdt.getText().toString().equals("")){
                    roomName = "rommmm16";
                }else{
                    roomName = roomEdt.getText().toString();
                }
                WebRTCManager.webRTCMultiplayerCall(MainActivity.this, OFFER_PHONE_TYPE, name, roomName);
            }
        });

        mButton4 = findViewById(R.id.button4);
        mButton4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(roomEdt.getText() ==null||roomEdt.getText().toString().equals("")){
                    roomName = "rommmm16";
                }else{
                    roomName = roomEdt.getText().toString();
                }
                WebRTCManager.webRTCMultiplayerCall(MainActivity.this, ANSWER_PHONE_TYPE, name, roomName);
            }
        });

        mButton5 = findViewById(R.id.button5);
        mButton5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.avchat_ring);
                asyncPlayer = new AsyncPlayer("my");
                asyncPlayer.play(MainActivity.this, uri, true, AudioManager.STREAM_VOICE_CALL);


            }
        });
        mButton6 = findViewById(R.id.button6);
        mButton6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                asyncPlayer.stop();

            }
        });


    }


}
