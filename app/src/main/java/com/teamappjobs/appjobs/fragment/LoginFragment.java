package com.teamappjobs.appjobs.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.iid.FirebaseInstanceId;
import com.squareup.picasso.Picasso;
import com.teamappjobs.appjobs.R;
import com.teamappjobs.appjobs.activity.CadastroUsuarioActivity;
import com.teamappjobs.appjobs.activity.LoginActivity;
import com.teamappjobs.appjobs.asyncTask.FirebaseLoginTask;
import com.teamappjobs.appjobs.asyncTask.LoginTask;
import com.teamappjobs.appjobs.modelo.Usuario;
import com.teamappjobs.appjobs.web.FacebookJson;
import com.teamappjobs.appjobs.web.LoginJson;
import com.teamappjobs.appjobs.web.UsuarioJson;

import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Lucas on 28/04/2016.
 */
public class LoginFragment extends Fragment implements View.OnClickListener {

    private EditText etEmail;
    private EditText etSenha;
    private Button btnEntrar;
    private Button btnCadastrese;
    private TextView esqueciSenha;

    //Firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private String TAG = "FirebaseAuth";
    ProgressDialog progress;

    //Facebook
    private LoginButton loginButton;
    private CallbackManager callbackManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //FirebaseAuth
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View cardLogin = inflater.inflate(R.layout.fragment_login, container, false);
        onRegistrar();

        //Facebook
        callbackManager = CallbackManager.Factory.create();
        loginButton = (LoginButton) cardLogin.findViewById(R.id.login_button);
        loginButton.setReadPermissions("email", "public_profile");
        // If using in a fragment
        loginButton.setFragment(this);
        // Other app specific specialization

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // Enviando ao Firebase
                handleFacebookAccessToken(loginResult.getAccessToken());

                String userId = loginResult.getAccessToken().getUserId();
                String accessToken = loginResult.getAccessToken().getToken();

                // save accessToken to SharedPreference
                //preferencias.saveFacebookAccessToken(accessToken);
                //preferencias.saveFacebookUserId(userId);
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
            }
        });

        //Login Normal
        this.etEmail = (EditText) cardLogin.findViewById(R.id.etEmail);
        this.etSenha = (EditText) cardLogin.findViewById(R.id.etSenha);
        this.btnEntrar = (Button) cardLogin.findViewById(R.id.btnEntrar);
        this.esqueciSenha = (TextView) cardLogin.findViewById(R.id.esqueciSenha);
        this.btnCadastrese = (Button) cardLogin.findViewById(R.id.btnCadastrese);

        esqueciSenha.setOnClickListener(this);

        btnEntrar.setOnClickListener(this);

        btnCadastrese.setOnClickListener(this);

        return cardLogin;
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btnEntrar:

                if (validarCampos()) {
                    SharedPreferences prefs = getActivity().getSharedPreferences("Configuracoes", Context.MODE_PRIVATE);


                    LoginJson loginJson = new LoginJson();
                    String data = loginJson.loginToJson(etEmail.getText().toString().toLowerCase(),
                            etSenha.getText().toString(), prefs.getString("gcmId", ""));
                    LoginTask task = new LoginTask(data, (LoginActivity) getActivity());
                    task.execute();
                }
                break;
            case R.id.esqueciSenha:
                ((LoginActivity) getActivity()).RecuperarSenha();
                break;


            case R.id.btnCadastrese:
                Intent intent = new Intent(getActivity(), CadastroUsuarioActivity.class);
                startActivity(intent);
        }

    }

    // Faz o registro no FCM
    public void onRegistrar() {
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.i("FIREBASE TOKEN: ",token);
        SharedPreferences prefs = getActivity().getSharedPreferences("Configuracoes", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("gcmId", token);
        editor.commit();
    }

    public Boolean validarCampos() {

        Boolean aux = true;

        if (etEmail.getText().toString().equals("")) {
            aux = false;
            etEmail.setError(getResources().getString(R.string.preenchacampo));
        } else {
            String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

            Pattern pattern = Pattern.compile(EMAIL_PATTERN);
            Matcher matcher = pattern.matcher(etEmail.getText().toString());
            aux = matcher.matches();
            if (!matcher.matches()) {
                etEmail.setError(getResources().getString(R.string.emailInvalido));
            }
        }

        if (etSenha.getText().toString().equals("")) {
            aux = false;
            etEmail.setError(getResources().getString(R.string.preenchacampo));
        } else {
            if (etSenha.getText().toString().length() < 5) {
                etSenha.setError(getResources().getString(R.string.senhacurta));
                aux = false;
            }
        }

        return aux;
    }

    public void createFireBaseAccount(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Toast.makeText(getActivity(), "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void signInFirebase(String email, String password) {

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithEmail", task.getException());
                            Toast.makeText(getActivity(), "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }

    public void getUser(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Name, email address, and profile photo Url
            String name = user.getDisplayName();
            String email = user.getEmail();
            Uri photoUrl = user.getPhotoUrl();

            // The user's ID, unique to the Firebase project. Do NOT use this value to
            // authenticate with your backend server, if you have one. Use
            // FirebaseUser.getToken() instead.
            String uid = user.getUid();
        }
    }

    //Tratando login do facebook com Firebase
    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        progress.dismiss();

                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(getActivity(), "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        } else {

                            //Tudo certo... lidar com o server aqui

                            SharedPreferences prefs = getActivity().getSharedPreferences("Configuracoes", Context.MODE_PRIVATE);
                            Profile faceProfile = Profile.getCurrentProfile();
                            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                            String email = firebaseUser.getEmail().toLowerCase();
                            String nome = firebaseUser.getDisplayName();
                            String uid = firebaseUser.getUid();

                            Log.i("FaceFireLogin", email+"--"+nome+"--"+uid);

                            Usuario usuario = new Usuario();
                            usuario.setNome(faceProfile.getFirstName());
                            usuario.setSobreNome(faceProfile.getLastName());
                            usuario.setEmail(firebaseUser.getEmail());
                            usuario.setGcmIdAtual(prefs.getString("gcmId", ""));
                            usuario.setFirebaseUID(firebaseUser.getUid());
                            usuario.setDataCadastro(Calendar.getInstance().getTime());
                           // Bitmap profilePic = convertFacebookProfileImage(faceProfile.getProfilePictureUri(1,1));

                            FirebaseLoginTask fireLoginTask = new FirebaseLoginTask(usuario, (LoginActivity) getActivity(),
                                    faceProfile.getProfilePictureUri(1000,1000));
                            fireLoginTask.execute();
                        }
                    }
                });
        progress = ProgressDialog.show(getActivity(), "Aguarde...", "Conectando", true, true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    //Utiliza o GraphRequest do Facebook pra pegar dados adicionais
    private void getFacebookData() {
        GraphRequest request = GraphRequest.newMeRequest(
                AccessToken.getCurrentAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        Log.v("LoginActivity", response.toString());
                        //     Log.v("LoginActivityRaw", response.getRawResponse());
                        Log.v("LoginActJsontoString", response.getJSONObject().toString());

                        FacebookJson faceJson = new FacebookJson();
                      //  usuario = faceJson.jsonToUsuario(response.getRawResponse());
                      //  coverUrl = faceJson.jsonToCoverPic(response.getRawResponse());

                        //Mostra a coverPic
                        // if(!coverUrl.equals("") && !prefs.getToken().equals(null))
                        //  Picasso.with(MainActivity.this).load(coverUrl).into(coverProfile);

                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "age_range,birthday,bio,cover,first_name,gender,last_name,education,likes,name");
        request.setParameters(parameters);
        request.executeAsync();

    }

    //Transforma a foto do facebook em Bitmap
    public Bitmap convertFacebookProfileImage(Uri uri){
        InputStream inputStream;
        Bitmap bitmap;
        try {
            //inputStream = getActivity().getContentResolver().openInputStream(uri);
            //Imagem original
            bitmap = Picasso.with(getActivity()).load(uri).get();

            Log.i("tamanho original", String.valueOf(bitmap.getHeight()) + String.valueOf(bitmap.getWidth()));

            //Reduz o tamanho do foto
            if (bitmap.getHeight() > 3000 || bitmap.getWidth() > 3000) {
                bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / 2,
                        bitmap.getHeight() / 2, true);

                //Reduz again (no caso de cameras muuuuito sensacionais)
                if (bitmap.getHeight() > 3000 || bitmap.getWidth() > 3000) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / 2,
                            bitmap.getHeight() / 2, true);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Erro ao carregar a imagem do face", Toast.LENGTH_LONG).show();

            return null;
        }
        return bitmap;
    }

    //LogOut = FirebaseAuth.getInstance().signOut();
}
