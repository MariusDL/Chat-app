package com.marius.chatapp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.marius.chatapp.MessageActivity;
import com.marius.chatapp.Model.Message;
import com.marius.chatapp.Model.User;
import com.marius.chatapp.R;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    public static final int RECEIVED_MESSAGE = 0;
    public static final int SENT_MESSAGE = 1;

    private Context mContext;
    private List<Message> mMessages;
    private String imageUrl;

    private FirebaseUser mUser;

    public MessageAdapter(Context mContext, List<Message> mMessages, String imageUrl){
        this.mMessages = mMessages;
        this.mContext = mContext;
        this.imageUrl = imageUrl;
    }

    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == RECEIVED_MESSAGE){
            View view = LayoutInflater.from(mContext).inflate(R.layout.received_message_layout, parent, false);
            return new MessageAdapter.ViewHolder(view);
        } else{
            View view = LayoutInflater.from(mContext).inflate(R.layout.sent_message_layout, parent, false);
            return new MessageAdapter.ViewHolder(view);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, int position) {
        Message message = mMessages.get(position);

        holder.messageTextView.setText(message.getMessage());

        if(imageUrl.equals("default")){
            holder.profileImage.setImageResource(R.mipmap.ic_launcher);
        } else {
            Glide.with(mContext).load(imageUrl).into(holder.profileImage);
        }

        if(position == mMessages.size()-1){
            if(message.isSeen()){
                holder.seenMessage.setText("Seen");
            } else{
                holder.seenMessage.setText("Delivered");
            }
        } else {
            holder.seenMessage.setVisibility(View.GONE);
        }


    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        public TextView messageTextView;
        public CircleImageView profileImage;

        public TextView seenMessage;

        public ViewHolder(View itemView){
            super(itemView);

            messageTextView = itemView.findViewById(R.id.MessageLayoutMessageTextView);
            profileImage = itemView.findViewById(R.id.MessageLayoutImageView);
            seenMessage = itemView.findViewById(R.id.SeenMessage);

        }
    }

    @Override
    public int getItemViewType(int position) {
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        if(mMessages.get(position).getSender().equals(mUser.getUid())){
            return SENT_MESSAGE;
        } else {
            return RECEIVED_MESSAGE;
        }
    }
}
