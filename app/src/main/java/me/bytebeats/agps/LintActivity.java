package me.bytebeats.agps;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import me.bytebeats.agp.base.R;

/**
 * Created by bytebeats on 2021/6/28 : 19:55
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
public class LintActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_main);
        Toast.makeText(this, R.string.previous, Toast.LENGTH_SHORT);
        Log.i("AAA", "onCreate");
        int color = Color.parseColor("#09");
        new aClass().Hello("John");
    }

    private static final class aClass {
        public boolean Hello(String name) {
            System.out.println("hello, " + name);
            return false;
        }
    }
}
