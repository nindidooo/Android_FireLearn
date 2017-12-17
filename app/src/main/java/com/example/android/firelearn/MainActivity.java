package com.example.android.firelearn;

import android.app.ProgressDialog;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

import static android.app.ProgressDialog.STYLE_HORIZONTAL;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class MainActivity extends AppCompatActivity {

    private Button mRecordBtn;
    private TextView mRecordLabel;
    private MediaRecorder mRecorder;

    public String mFilePath = null;
    public String mAudioFileName = null;
    public String mMidiFileName = null;
    public String mDatePrefix = null;

    // MIDI available?
    public boolean midiAvailable = FALSE;


    private static final String LOG_TAG = "Record_log";

    private Button mDownloadBtn;
    private TextView mDownloadLabel;
    private ProgressDialog mDownloadProgress;


    private StorageReference mStorage;
    private ProgressDialog mProgress;
    private UploadTask uploadTask;

    // Create a storage reference from our app
    private StorageReference storageRef;

    public String Md5Hash = "HELLO";


    private static final String TAG = "MyFirebaseIIDService";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    // [START refresh_token]
//    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
//        Log.d(TAG, "Refreshed token: " + refreshedToken);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(refreshedToken);
    }
    // [END refresh_token]

    /**
     * Persist token to third-party servers.
     * <p>
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        // TODO: Implement this method to send token to your app server.
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecordBtn = (Button) findViewById(R.id.RecordBtn);
        mRecordLabel = (TextView) findViewById(R.id.RecordLabel);
        mProgress = new ProgressDialog(this);
        mProgress.setProgressStyle(STYLE_HORIZONTAL);
        mDownloadProgress = new ProgressDialog(this);


        // upload
        mStorage = FirebaseStorage.getInstance().getReference();

        // download
        mDownloadBtn = (Button) findViewById(R.id.DownloadMidiBtn);
        mDownloadLabel = (TextView) findViewById(R.id.DownloadLabel);


        mRecordBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    // user has pressed down on the button
                    startRecording();
                    mRecordLabel.setText("Recording Started ...");
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    // user has let go of the button
                    stopRecording();
                    mRecordLabel.setText("Recording Stopped ...");
                }
                return false;
            }
        });
        mDownloadBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    // user has pressed down on the button
                    downloadMidi();
                    mDownloadLabel.setText("Downloading MIDI ...");
                }
                return false;
            }
        });
//        mDownloadBtn.setEnabled(false);
    }

    private void startRecording() {

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // change this for file


        // Set location here
        mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath();

        // get date/time
        mDatePrefix = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss").format(Calendar.getInstance().getTime());
        mAudioFileName = mDatePrefix + ".aac";
        mMidiFileName = mDatePrefix + ".mid";

        mFilePath += "/" + mAudioFileName;
        mRecorder.setOutputFile(mFilePath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);


        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        mProgress.dismiss();
        uploadAudio();

    }

    public static String getMD5EncryptedString(String encTarget) {
        MessageDigest mdEnc = null;
        try {
            mdEnc = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Exception while encrypting to md5");
            e.printStackTrace();
        } // Encryption algorithm
        mdEnc.update(encTarget.getBytes(), 0, encTarget.length());
        String md5 = new BigInteger(1, mdEnc.digest()).toString(16);
        while (md5.length() < 32) {
            md5 = "0" + md5;
        }
        return md5;
    }

    private void uploadAudio() {
//
        mProgress.setMessage("Uploading Audio ...");
        mProgress.show();

        Uri file = Uri.fromFile(new File(mFilePath)); // get full local device file path
        StorageReference recordingRef = mStorage.child(mAudioFileName);
        uploadTask = recordingRef.putFile(file);

        // Observe state change events such as progress, pause, and resume
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                System.out.println("Upload is " + progress + "% done");
                int currentprogress = (int) progress;
                mProgress.setProgress(currentprogress);
            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                System.out.println("Upload is paused");
            }
        });


        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                mProgress.dismiss();
                mRecordLabel.setText(mAudioFileName);

                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
            }
        });

    }


//    private void uploadAudio() {
//
////        mProgress.setMessage("Uploading Audio ...");
//        mProgress.show();
//        StorageReference filepath = mStorage.child(mAudioFileName); // give the name of what the file should be called
//
//
//        filepath.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
//            @Override
//            public void onSuccess(StorageMetadata storageMetadata) {
//                // storageMetadata now contains the metadata of input audio file
////                mRecordLabel.setText(storageMetadata.getMd5Hash());
//                Md5Hash = storageMetadata.getMd5Hash();
//            }
//        });
//
////        StorageReference filepath = mStorage.child("Audio").child("new_audio.aac");
//        // create a uri file form filename string
//
//        Uri uri = Uri.fromFile(new File(mFilePath)); // get full local device file path
//
//        // THIS PART AUTO-GENERATES
//        filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//            @Override
//            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                mProgress.dismiss();
////                mRecordLabel.setText(Md5Hash + ".mid");
//                mRecordLabel.setText(mAudioFileName);
//
//                mDownloadBtn.setEnabled(true);
//
//            }
//        });
//    }

    private void downloadMidi() {
        mDownloadProgress.setMessage("Downloading MIDI ...");
        mDownloadProgress.show();

//        String mMidiFileName = Md5Hash + ".mid";

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl("gs://firelearn-122c1.appspot.com");
        StorageReference midiRef = storageRef.child(mMidiFileName);
        midiRef.getDownloadUrl();


        // set up file directory on local device for downloading midi
        File rootPath = new File(Environment.getExternalStorageDirectory(), mMidiFileName);
        if (!rootPath.exists()) {
            rootPath.mkdirs();
        }
        final File localFile = new File(rootPath, mMidiFileName);


        // try and download midi
        midiRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {

                mDownloadProgress.dismiss();
                mDownloadLabel.setText("Downloading Finished");
                Log.e("firebase ", ";local tem file created  created " + localFile.toString());
                // display new screen
                startActivity(new Intent(getApplicationContext(), MIDI.class));
                midiAvailable = TRUE;

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                int errorCode = ((StorageException) exception).getErrorCode();
                String errorMessage = exception.getMessage();
                // test the errorCode and errorMessage, and handle accordingly


                mDownloadProgress.dismiss();
//                mDownloadLabel.setText("Could not download file");
                mDownloadLabel.setText(errorMessage);
                Log.e("firebase ", ";local tem file not created  created " + exception.toString());
                midiAvailable = FALSE;

            }
        });


    }

}