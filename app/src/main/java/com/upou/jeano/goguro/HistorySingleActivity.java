package com.upou.jeano.goguro;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class HistorySingleActivity extends AppCompatActivity {
    private String rideId, currentUserId, tuteeId, tutorId, userTutorOrTutee;
    private TextView mDate, userName, userPhone, mTopic;
    private ImageView userImage;
    private DatabaseReference historyRideInfoDb;
    private RatingBar mRatingBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);
        rideId = getIntent().getExtras().getString("rideId");
        mDate = findViewById(R.id.date);
        userName = findViewById(R.id.userName);
        userPhone = findViewById(R.id.userPhone);
        mTopic = findViewById(R.id.topic);
        userImage = findViewById(R.id.userImage);
        mRatingBar = findViewById(R.id.ratingBar);
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        historyRideInfoDb = FirebaseDatabase.getInstance().getReference().child("History").child(rideId);
        getRideInformation();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void getRideInformation() {
        historyRideInfoDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        switch (child.getKey().toString()) {
                            case "tutee":
                                tuteeId = child.getValue().toString();
                                if (!tuteeId.equals(currentUserId)) {
                                    userTutorOrTutee = "Tutors";
                                    getUserInformation("Tutees", tuteeId);
                                }
                                break;
                            case "tutor":
                                tutorId = child.getValue().toString();
                                if (!tutorId.equals(currentUserId)) {
                                    userTutorOrTutee = "Tutees";
                                    getUserInformation("Tutors", tutorId);
                                    displayTuteeRelatedObject();
                                }
                                break;
                            case "timestamp":
                                mDate.setText(getDate(Long.valueOf(child.getValue().toString())));
                                break;
                            case "rating":
                                mRatingBar.setRating(Integer.valueOf(child.getValue().toString()));
                                break;
                            case "topic":
                                mTopic.setText(child.getValue().toString());
                                break;
                        }
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    private void displayTuteeRelatedObject() {
        mRatingBar.setVisibility(View.VISIBLE);
        mRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                historyRideInfoDb.child("rating").setValue(rating);
                DatabaseReference mTutorRatingDbRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Tutors").child(tutorId).child("rating");
                mTutorRatingDbRef.child(rideId).setValue(rating);
            }
        });
    }
    private void getUserInformation(String otherUserTutorOrTutee, String otherUserId) {
        DatabaseReference mOtherUserDb = FirebaseDatabase.getInstance().getReference().child("Users").child(otherUserTutorOrTutee).child(otherUserId);
        mOtherUserDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null) {
                        userName.setText(map.get("name").toString());
                    }
                    if (map.get("phone") != null) {
                        userPhone.setText(map.get("phone").toString());
                    }
                    if (map.get("profileImageUrl") != null) {
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(userImage);
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    private String getDate(Long timestamp) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timestamp * 1000);
        String date = DateFormat.format("MMMM dd, yyyy h:mmaa", cal).toString();
        return date;
    }
}