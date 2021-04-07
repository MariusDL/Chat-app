package com.marius.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.service.autofill.Dataset;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.marius.chatapp.Adapter.MessageAdapter;
import com.marius.chatapp.Fragments.APIService;
import com.marius.chatapp.Model.Message;
import com.marius.chatapp.Model.User;
import com.marius.chatapp.Notifications.Client;
import com.marius.chatapp.Notifications.Data;
import com.marius.chatapp.Notifications.MyResponse;
import com.marius.chatapp.Notifications.Sender;
import com.marius.chatapp.Notifications.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageActivity extends AppCompatActivity {

    private CircleImageView profileImage;
    private TextView username;
    private ImageButton sendMessage;
    private EditText messageToSend;

    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;

    private Intent intent;

    private MessageAdapter messageAdapter;
    private List<Message> messages;
    private RecyclerView recyclerView;

    private ValueEventListener seenMessageListener;

    private String userID;

    private APIService apiService;

    private boolean notify = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Toolbar toolbar = findViewById(R.id.MessageActivityToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });

        apiService = Client.getClient("https://fcm.googleapis.com").create(APIService.class);

        intent = getIntent();
        userID = intent.getStringExtra("userId");

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(userID);

        profileImage = findViewById(R.id.MessageActivityProfileImage);
        username = findViewById(R.id.MessageActivityUsername);
        sendMessage = findViewById(R.id.MessageActivitySendButton);
        messageToSend = findViewById(R.id.MessageActivityMessageEditText);
        recyclerView = findViewById(R.id.MessageActivityRecyclerView);
        recyclerView.setHasFixedSize(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);


        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notify = true;
                String message = messageToSend.getText().toString().trim();
                if(!TextUtils.isEmpty(message)){
                    sendMessage(currentUser.getUid(), userID, message);
                } else {
                    Toast.makeText(MessageActivity.this, "You can not send an empty message", Toast.LENGTH_SHORT).show();
                }
                messageToSend.setText("");
            }
        });



        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                username.setText(user.getUsername());
                if(user.getImageURL().equals("default")){
                    profileImage.setImageResource(R.mipmap.ic_launcher);
                } else {

                    Glide.with(getApplicationContext()).load(user.getImageURL()).into(profileImage);
                }

                readMessages(currentUser.getUid(), userID, user.getImageURL());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        seenMessage(userID);
    }

    private void seenMessage(final String userId){
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Messages");
        seenMessageListener = databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot dataSnapshot:snapshot.getChildren()){
                    Message message = dataSnapshot.getValue(Message.class);
                    if(message.getReceiver().equals(currentUser.getUid()) &&
                            message.getSender().equals(userId)){
                        dataSnapshot.getRef().child("seen").setValue(true);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendMessage(String sender, final String receiver, final String message){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", message);
        hashMap.put("seen", false);

        reference.child("Messages").push().setValue(hashMap);

        final DatabaseReference messagesRefSender = FirebaseDatabase.getInstance().getReference().child("connectedusers")
                .child(currentUser.getUid()).child(userID);

        final DatabaseReference messagesRefReceiver = FirebaseDatabase.getInstance().getReference().child("connectedusers")
                .child(userID).child(currentUser.getUid());

        messagesRefSender.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(!snapshot.exists()){
                    messagesRefSender.child("id").setValue(userID);
                    messagesRefReceiver.child("id").setValue(currentUser.getUid());
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        final String msg = message;

        reference = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if(notify){

                    sendNotification(receiver, user.getUsername(), msg);
                }
                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    private void sendNotification(String receiver, final String username, final String message){
        final DatabaseReference tokens = FirebaseDatabase.getInstance().getReference().child("Tokens");
        Query query = tokens.orderByKey().equalTo(receiver);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot dataSnapshot : snapshot.getChildren()){
                    Token token = dataSnapshot.getValue(Token.class);

                    Data data = new Data(currentUser.getUid(), R.mipmap.ic_launcher, username+": "+message,
                            "New message", userID);

                    Sender sender = new Sender(data, token.getToken());

                    apiService.sendNotification(sender).enqueue(new Callback<MyResponse>() {
                        @Override
                        public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                            if(response.code() == 200){
                                if(response.body().success != 1){
                                    Toast.makeText(MessageActivity.this, "Failed", Toast.LENGTH_SHORT).show();

                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<MyResponse> call, Throwable t) {

                        }
                    });

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void readMessages(final String myId, final String userId, final String imageUrl){
        messages = new ArrayList<>();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Messages");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messages.clear();
                for(DataSnapshot dataSnapshot:snapshot.getChildren()){
                    Message msg = dataSnapshot.getValue(Message.class);

                    if(msg.getReceiver().equals(myId) && msg.getSender().equals(userId) ||
                            msg.getReceiver().equals(userId) && msg.getSender().equals(myId) ){

                        messages.add(msg);
                    }
                    messageAdapter = new MessageAdapter(MessageActivity.this, messages, imageUrl);
                    recyclerView.setAdapter(messageAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void setStatus(String status){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUser.getUid());
        reference.child("status").setValue(status);
    }


    @Override
    protected void onResume() {
        super.onResume();
        setStatus("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        databaseReference.removeEventListener(seenMessageListener);
        setStatus("offline");
    }

}