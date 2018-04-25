package com.upou.jeano.goguro;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button mTutor, mTutee;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mTutor = findViewById(R.id.tutor);
        mTutee = findViewById(R.id.tutee);

        startService(new Intent(MainActivity.this, OnAppKilled.class));
        mTutor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TutorLoginActivity.class);
                startActivity(intent);
                //finish();
                return;
            }
        });

        mTutee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TuteeLoginActivity.class);
                startActivity(intent);
                //finish();
                return;
            }
        });

    }
}
