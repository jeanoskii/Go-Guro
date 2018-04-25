package com.upou.jeano.goguro;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.upou.jeano.goguro.HistoryRecyclerView.HistoryAdapter;
import com.upou.jeano.goguro.HistoryRecyclerView.HistoryObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView mHistoryRecyclerView;
    private RecyclerView.Adapter mHistoryAdapter;
    private RecyclerView.LayoutManager mHistoryLayoutManager;
    private String tuteeOrTutor, userId, userName, otherUserTuteeOrTutor, otherUserId, rideId;
    private Long timestamp;
    private TextView mGreetings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        mGreetings = findViewById(R.id.greetings);
        mHistoryRecyclerView = findViewById(R.id.historyRecyclerView);
        mHistoryRecyclerView.setNestedScrollingEnabled(false);
        mHistoryRecyclerView.setHasFixedSize(true);

        mHistoryLayoutManager = new LinearLayoutManager(HistoryActivity.this);
        mHistoryRecyclerView.setLayoutManager(mHistoryLayoutManager);
        mHistoryAdapter = new HistoryAdapter(getDataSetHistory(), HistoryActivity.this);
        mHistoryRecyclerView.setAdapter(mHistoryAdapter);

        tuteeOrTutor = getIntent().getExtras().getString("tuteeOrTutor");
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        getUserHistoryIds();
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

    private void setGreetingsText(boolean hasHistory) {
        if (hasHistory) {
            switch (tuteeOrTutor) {
                case "Tutors":
                    mGreetings.setText("These are the users you have helped in the past:");
                    otherUserTuteeOrTutor = "tutee";
                    break;
                case "Tutees":
                    mGreetings.setText("These are the users that helped you in the past:");
                    otherUserTuteeOrTutor = "tutor";
                    break;
            }
        } else {
            switch (tuteeOrTutor) {
                case "Tutors":
                    mGreetings.setText("Oops! You haven't helped anyone yet. Start helping others by going to the map page, toggle on the \"OFF\" button, and wait for tutees.");
                    otherUserTuteeOrTutor = "tutee";
                    break;
                case "Tutees":
                    mGreetings.setText("Oops! No one has helped you yet. Start looking for help by going to the map page, type in the topic, then press the \"CALL FOR HELP\" button.");
                    otherUserTuteeOrTutor = "tutor";
                    break;
            }
        }
    }
    private void getUserHistoryIds() {
        DatabaseReference userHistoryDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(tuteeOrTutor).child(userId).child("history");
        userHistoryDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    setGreetingsText(true);
                    for (DataSnapshot history : dataSnapshot.getChildren()) {
                        fetchRideInformation(history.getKey());
                    }
                } else {
                    setGreetingsText(false);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void fetchRideInformation(String rideKey) {
        DatabaseReference historyDatabase = FirebaseDatabase.getInstance().getReference().child("History").child(rideKey);
        historyDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    rideId = dataSnapshot.getKey();
                    timestamp = 0L;
                    for(DataSnapshot child : dataSnapshot.getChildren()) {
                        if (child.getKey().equals("timestamp")) {
                            timestamp = Long.valueOf(child.getValue().toString());
                        }
                        if (child.getKey().equals(otherUserTuteeOrTutor)) {
                            otherUserId = child.getValue().toString();
                        }
                    }
                    setHistoryObject(rideId, timestamp);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void setHistoryObject(final String mRideId, final Long mTimestamp) {
        String usersRecordDatabaseName = otherUserTuteeOrTutor.substring(0,1).toUpperCase() + otherUserTuteeOrTutor.substring(1).toLowerCase() + "s";
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(usersRecordDatabaseName).child(otherUserId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    userName = dataSnapshot.child("name").getValue().toString();
                    HistoryObject obj = new HistoryObject(mRideId, getDate(mTimestamp), userName);
                    resultsHistory.add(obj);
                    mHistoryAdapter.notifyDataSetChanged();
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

    private ArrayList resultsHistory = new ArrayList<HistoryObject>();
    private ArrayList<HistoryObject> getDataSetHistory() {
        return resultsHistory;
    }


}
