package unipi.exercise.trackmemore;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    EditText emailText, passwordText;
    FirebaseAuth auth;
    FirebaseDatabase database;
    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        emailText = findViewById(R.id.editTextText);
        passwordText = findViewById(R.id.editTextTextPassword);
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        reference = database.getReference("message");
    }

    public void signup(View view){
        if (checkEmptyInput()) return;
        auth.createUserWithEmailAndPassword(emailText.getText().toString(),
                passwordText.getText().toString()).addOnCompleteListener(
                new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            showMessage("Success","User has been created!");
                        } else {
                            showMessage("Error", Objects.requireNonNull(task.getException()).getLocalizedMessage());
                        }
                    }
                }
        );
    }
    public void signin(View view){
        if (checkEmptyInput()) return;
        auth.signInWithEmailAndPassword(emailText.getText().toString(),
                passwordText.getText().toString()).addOnCompleteListener(
                new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                checkAuthorizedAndRedirect(user);
                            } else {
                                showMessage("Error", "Unauthorized user");
                            }

                        } else {
                            showMessage("Error",task.getException().getLocalizedMessage());
                        }
                    }
                }
        );
    }

    private void checkAuthorizedAndRedirect(FirebaseUser user) {
        System.out.println("User ID: " + user.getUid());
        System.out.println("User Email: " + user.getEmail());
        System.out.println("User Name: " + user.getDisplayName());
        if (user != null ) {
            Log.d("INSIDE_REDIRECT_METHOD", "Passed the check");
            Intent intent = new Intent(MainActivity.this, HomePage.class);
            intent.putExtra("userId", user.getUid());
            intent.putExtra("email", user.getEmail());
            startActivity(intent);
        }
    }

    private boolean checkEmptyInput() {
        if (TextUtils.isEmpty(emailText.getText().toString())) {
            showMessage("Error", "Please enter email");
            return true;
        }

        if (TextUtils.isEmpty(passwordText.getText().toString())) {
            showMessage("Error", "Please enter password");
            return true;
        }
        return false;
    }

    void showMessage(String title, String message){
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setCancelable(true).show();
    }
}