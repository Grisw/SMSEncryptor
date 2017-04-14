package pers.lxt.smsencryptor.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;

import pers.lxt.smsencryptor.R;
import pers.lxt.smsencryptor.adapter.ContactsAdapter;

public class MainActivity extends AppCompatActivity {

    private ContactsAdapter contactsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final RecyclerView contacts = (RecyclerView) findViewById(R.id.contacts);
        ImageButton createMsg = (ImageButton) findViewById(R.id.create_msg);

        contacts.setLayoutManager(new LinearLayoutManager(this));
        contactsAdapter = new ContactsAdapter(this);
        contacts.setAdapter(contactsAdapter);
        contacts.setItemAnimator(new DefaultItemAnimator());
        createMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,CreateContactActivity.class));
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        contactsAdapter.unload();
    }

    @Override
    protected void onResume() {
        super.onResume();

        contactsAdapter.load();
    }
}
