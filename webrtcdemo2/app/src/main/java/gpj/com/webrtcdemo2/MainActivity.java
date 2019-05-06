package gpj.com.webrtcdemo2;

import android.Manifest;
import android.media.AsyncPlayer;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import gpj.com.webrtcdemo2.webrtc.WebRTCManager;

import static gpj.com.webrtcdemo2.webrtc.WebRTCManager.ANSWER_PHONE_TYPE;
import static gpj.com.webrtcdemo2.webrtc.WebRTCManager.OFFER_PHONE_TYPE;

public class MainActivity extends AppCompatActivity {

    Button mButton;
    Button mButton2;
    Button mButton3;
    Button mButton4;
    Button mButton5;
    Button mButton6;

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

        WebRTCManager.getInstance().init(this);
        mButton = findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebRTCManager.webRTCCall(MainActivity.this, OFFER_PHONE_TYPE, "aaa1", "bbb1", "hahah");
            }
        });

        mButton2 = findViewById(R.id.button2);
        mButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebRTCManager.webRTCCall(MainActivity.this, ANSWER_PHONE_TYPE, "bbb1", "aaa1", "lalala");
            }
        });

        final String name = "a " + (int) (1 + Math.random() * (1000));
        mButton3 = findViewById(R.id.button3);
        mButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebRTCManager.webRTCMultiplayerCall(MainActivity.this, OFFER_PHONE_TYPE, name, "rommmm17");
            }
        });

        mButton4 = findViewById(R.id.button4);
        mButton4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebRTCManager.webRTCMultiplayerCall(MainActivity.this, ANSWER_PHONE_TYPE, name, "rommmm17");
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
