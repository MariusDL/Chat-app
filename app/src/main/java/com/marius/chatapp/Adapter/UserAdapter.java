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
import com.marius.chatapp.MessageActivity;
import com.marius.chatapp.Model.User;
import com.marius.chatapp.R;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private Context mContext;
    private List<User> mUsers;
    private boolean usersAlreadyChat;

    public UserAdapter(Context mContext, List<User> mUsers, boolean usersAlreadyChat){
        this.mUsers = mUsers;
        this.mContext = mContext;
        this.usersAlreadyChat = usersAlreadyChat;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.users_list_item, parent, false);
        return new UserAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final User user = mUsers.get(position);
        holder.username.setText(user.getUsername());
        if(user.getImageURL().equals("default")){
            holder.profileImage.setImageResource(R.mipmap.ic_launcher);
        } else {
            Glide.with(mContext).load(user.getImageURL()).into(holder.profileImage);
        }

        if(usersAlreadyChat){
            if(user.getStatus().equals("online")){
                holder.userOn.setVisibility(View.VISIBLE);
                holder.userOff.setVisibility(View.INVISIBLE);
            } else {
                holder.userOn.setVisibility(View.INVISIBLE);
                holder.userOff.setVisibility(View.VISIBLE);
            }
        } else {
            holder.userOn.setVisibility(View.GONE);
            holder.userOff.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, MessageActivity.class);
                intent.putExtra("userId", user.getId());
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        public TextView username;
        public CircleImageView profileImage, userOn, userOff;

        public ViewHolder(View itemView){
            super(itemView);

            username = itemView.findViewById(R.id.UsersListUsername);
            profileImage = itemView.findViewById(R.id.UsersListProfileImage);
            userOn = itemView.findViewById(R.id.UsersListItemOnBubble);
            userOff = itemView.findViewById(R.id.UsersListItemOffBubble);


        }
    }
}
