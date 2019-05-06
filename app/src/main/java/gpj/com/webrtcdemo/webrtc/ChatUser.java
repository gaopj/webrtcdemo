package gpj.com.webrtcdemo.webrtc;

import org.webrtc.MediaStream;

public class ChatUser {
    private String mId;
    private MediaStream mMediaStream;

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public MediaStream getMediaStream() {
        return mMediaStream;
    }

    public void setMediaStream(MediaStream mediaStream) {
        mMediaStream = mediaStream;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ChatUser) {
            ChatUser person = (ChatUser) o;
            return mId.equals(person.mId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mId.hashCode();
    }
}
