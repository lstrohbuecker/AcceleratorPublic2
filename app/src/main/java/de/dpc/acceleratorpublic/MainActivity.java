package de.dpc.acceleratorpublic;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView mTextMessage;
    private PhoneAccelerometer phoneGyro;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextMessage = (TextView) findViewById(R.id.edMsg);
        phoneGyro = new PhoneAccelerometer(getApplicationContext(), mTextMessage);
    }

    public void bnStop_Click(View v) {
        phoneGyro.stop();
    }

    public void bnStart_Click(View v) {
        phoneGyro.start();
    }

}
