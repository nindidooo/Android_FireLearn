package com.example.android.firelearn;

import android.app.ProgressDialog;
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
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private Button mRecordBtn;
    private TextView mRecordLabel;
    private MediaRecorder mRecorder;
    private String mFileName = null;
    private static final String LOG_TAG = "Record_log";

    private StorageReference mStorage;
    private ProgressDialog mProgress;

    // Create a storage reference from our app
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecordBtn = (Button) findViewById(R.id.RecordBtn);
        mRecordLabel = (TextView) findViewById(R.id.RecordLabel);
        mProgress = new ProgressDialog(this);

        // upload
        mStorage = FirebaseStorage.getInstance().getReference();
        // download
        // Set location here
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        // Set file type here and THREE_GPP below
        mFileName += "/myrecording.mp3"; // that's the format we'll use, can be changed

        mRecordBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                    // user has pressed down on the button
                    startRecording();
                    mRecordLabel.setText("Recording Started ...");
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP){
                    // user has let go of the button
                    stopRecording();
                    mRecordLabel.setText("Recording Stopped ...");
                }
                return false;
            }
        });
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // change this for file
        mRecorder.setOutputFile(mFileName);
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
        uploadAudio();
        downloadMidi();
    }

    private void uploadAudio() {

        mProgress.setMessage("Uploading Audio ...");
        mProgress.show();
        StorageReference filepath = mStorage.child("new_audio.aac");
//        StorageReference filepath = mStorage.child("Audio").child("new_audio.aac");
        // create a uri file form filename string
        Uri uri = Uri.fromFile(new File(mFileName));

        // THIS PART AUTO-GENERATES
        filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                mProgress.dismiss();
                mRecordLabel.setText("Uploading Finished");

            }
        });
    }

    private void downloadMidi(){
        mProgress.setMessage("Downloading MIDI ...");
        mProgress.show();

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl("gs://firelearn-122c1.appspot.com");
        StorageReference  midiRef = storageRef.child("major-scale.mid");

        File rootPath = new File(Environment.getExternalStorageDirectory(), "major-scale.mid");
        if(!rootPath.exists()) {
            rootPath.mkdirs();
        }


        final File localFile = new File(rootPath,"major-scale.mid");

        midiRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.e("firebase ",";local tem file created  created " +localFile.toString());
                //  updateDb(timestamp,localFile.toString(),position);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.e("firebase ",";local tem file not created  created " +exception.toString());
            }
        });



        // Create a reference to a file from a Google Cloud Storage URI
//        StorageReference gsReference = storageRef.child("gs://firelearn-122c1.appspot.com/major-scale.mid");




    }

}