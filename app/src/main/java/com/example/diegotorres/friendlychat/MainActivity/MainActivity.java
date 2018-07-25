package com.example.diegotorres.friendlychat.MainActivity;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
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
import android.widget.Toast;

import com.example.diegotorres.friendlychat.R;
import com.example.diegotorres.friendlychat.adapter.MensajeAdapter;
import com.example.diegotorres.friendlychat.beans.FriendlyMensaje;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
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

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mReference;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseStorage mStorage;
    private StorageReference mStorageReference;

    private String mUsername;

    private static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER =  2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mReference = mFirebaseDatabase.getReference().child("mensajes");
        mStorage = FirebaseStorage.getInstance();
        mStorageReference = mStorage.getReference().child("chat_fotos");

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
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.toString().trim().length()>0){
                    mButtonEnviar.setEnabled(true);
                }else{
                    mButtonEnviar.setEnabled(false);
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
        mEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_LENGTH_MSG)});

        mImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Selecciona la imagen"), RC_PHOTO_PICKER);
            }
        });

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

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    onSignInInitialize(user.getDisplayName());
                }else{
                    onSignOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                            .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                            .setAvailableProviders(Arrays.asList(
                                    new AuthUI.IdpConfig.GoogleBuilder().build(),
                                    new AuthUI.IdpConfig.EmailBuilder().build()
                            )).build()
                    , RC_SIGN_IN);
                }
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_SIGN_IN){
            if(resultCode == RESULT_OK){
                Toast.makeText(this, "Estás registrado! Bienvenido a FriendlyChat", Toast.LENGTH_LONG).show();
            }else if (resultCode == RESULT_CANCELED){
                Toast.makeText(this, "Registro cancelado", Toast.LENGTH_LONG).show();
                finish();
            }
        }else if(requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK){
            Uri imageUri = data.getData();
            StorageReference fotoRef = mStorageReference.child(imageUri.getLastPathSegment());
            fotoRef.putFile(imageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri dwlUrl = taskSnapshot.getDownloadUrl();
                    FriendlyMensaje mensaje = new FriendlyMensaje();
                    mensaje.setText(null);
                    mensaje.setName(mUsername);
                    mensaje.setPhotoUrl(dwlUrl.toString());
                    mReference.push().setValue(mensaje);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mAuthStateListener!=null){
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        quitListenerToDatabase();
        mMensajeAdapter.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    private void onSignInInitialize(String username){
        mUsername = username;
        addListenerToDatabase();
    }

    private void onSignOutCleanup(){
        mUsername = ANONYMUS;
        mMensajeAdapter.clear();

    }

    private void addListenerToDatabase(){
        if(mChildEventListener==null){
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    FriendlyMensaje mensaje = dataSnapshot.getValue(FriendlyMensaje.class);
                    mMensajeAdapter.add(mensaje);
                }
                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            };

            mReference.addChildEventListener(mChildEventListener);
        }
    }

    private void quitListenerToDatabase(){
        if(mChildEventListener!= null){
            mReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }
}
