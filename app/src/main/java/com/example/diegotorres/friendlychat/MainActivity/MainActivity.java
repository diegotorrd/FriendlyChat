package com.example.diegotorres.friendlychat.MainActivity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.example.diegotorres.friendlychat.R;
import com.example.diegotorres.friendlychat.adapter.MensajeAdapter;
import com.example.diegotorres.friendlychat.beans.FriendlyMensaje;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static String TAG = MainActivity.class.getName();

    public static String ANONYMUS="anonymus";
    public static int DEFAULT_LENGTH_MSG = 1000;

    private ListView mListView;
    private MensajeAdapter mMensajeAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mImageButton;
    private EditText mEditText;
    private Button mButtonEnviar;

    private FirebaseDatabase mDatabase;
    private DatabaseReference mReference;
    private ChildEventListener mChildEventListener;

    private String mUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDatabase = FirebaseDatabase.getInstance();
        mReference = mDatabase.getReference().child("mensajes");

        mUsername = ANONYMUS;

        mListView = (ListView) findViewById(R.id.messageListView);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mImageButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mEditText = (EditText) findViewById(R.id.messageEditText);
        mButtonEnviar = (Button) findViewById(R.id.sendButton);

        List<FriendlyMensaje> friendlyMensajes = new ArrayList<>();
        mMensajeAdapter = new MensajeAdapter(this, R.layout.item_message, friendlyMensajes);
        mListView.setAdapter(mMensajeAdapter);

        mProgressBar.setVisibility(View.INVISIBLE);

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.toString().trim().length()>0){
                    mButtonEnviar.setEnabled(true);
                }else{
                    mButtonEnviar.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        mEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_LENGTH_MSG)});

        mButtonEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FriendlyMensaje mensaje = new FriendlyMensaje();
                mensaje.setName(mUsername);
                mensaje.setPhotoUrl(null);
                mensaje.setText(mEditText.getText().toString().trim());
                mReference.push().setValue(mensaje);
                mEditText.setText("");
            }
        });

        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                FriendlyMensaje mensaje = dataSnapshot.getValue(FriendlyMensaje.class);
                mMensajeAdapter.add(mensaje);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };

        mReference.addChildEventListener(mChildEventListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
