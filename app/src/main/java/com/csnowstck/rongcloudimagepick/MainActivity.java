package com.csnowstck.rongcloudimagepick;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextView go,all;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        go=(TextView) findViewById(R.id.go);
        all=(TextView) findViewById(R.id.all);
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(MainActivity.this,RCSelectImageActivity.class),10001);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK){
            List<String> urls=data.getStringArrayListExtra("android.intent.extra.RETURN_RESULT");
            StringBuilder sb=new StringBuilder();
            for(int i=0;i<urls.size();i++){
                sb.append(urls.get(i));
                sb.append("\n");
            }
            all.setText(sb.toString());
        }

    }
}
