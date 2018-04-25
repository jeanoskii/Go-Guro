package com.upou.jeano.goguro;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TuteeMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private Button mLogout, mRequest, mSettings, mHistory, mSendMessage;
    private LatLng pickupLocation;
    private boolean isTutorRequested = false, hasTuteeCompleteInfo;
    private Marker pickupMarker;
    private String topic, userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private LinearLayout mTutorInfo;
    private ImageView mTutorProfileImage;
    private TextView mTutorName, mTutorPhone;
    private RatingBar mRatingBar;
    private AutoCompleteTextView mTopic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutee_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(TuteeMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            mapFragment.getMapAsync(this);
        }
        mTutorInfo = findViewById(R.id.tutorInfo);
        mTutorProfileImage = findViewById(R.id.tutorProfileImage);
        mTutorName = findViewById(R.id.tutorName);
        mTutorPhone = findViewById(R.id.tutorPhone);
        mRatingBar = findViewById(R.id.ratingBar);
        mLogout = findViewById(R.id.logout);
        mSendMessage = findViewById(R.id.sendMessage);
        mTopic = findViewById(R.id.topic);
        mTopic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference topicsRef = FirebaseDatabase.getInstance().getReference("Topics");
                topicsRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        final List<String> topics = new ArrayList<String>();
                        for (DataSnapshot topicSnapshot : dataSnapshot.getChildren()) {
                            String topic = topicSnapshot.getKey();
                            topics.add(topic);
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(TuteeMapActivity.this, android.R.layout.simple_dropdown_item_1line, topics);
                        mTopic.setAdapter(adapter);
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
            }
        });
        mRequest = findViewById(R.id.request);
        mSettings = findViewById(R.id.settings);
        mHistory = findViewById(R.id.history);
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(TuteeMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        mSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("smsto:" + mTutorPhone.getText()));
                intent.putExtra("sms_body", "Hi " + mTutorName.getText() + "! Can you help me with my lesson in " + topic + "? ");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });
        checkUserInfo();
        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (hasTuteeCompleteInfo) {
                    if (isTutorRequested) {
                        getHasRideEnded();
                        //endRide();
                    } else {
                        isTutorRequested = true;
                        topic = mTopic.getText().toString();
                        if (topic.equals("")) {
                            Toast.makeText(getApplicationContext(), "Please select a topic.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("tuteeRequest");
                        GeoFire geoFire = new GeoFire(ref);
                        geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                            }
                        });
                        pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                        pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)));
                        mRequest.setText("Getting your Tutor...");
                        enableActionButtons(false);
                        getClosestTutor();
                    }
                }
            }
        });
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TuteeMapActivity.this, TuteeSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });
        mHistory.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(TuteeMapActivity.this, HistoryActivity.class);
                intent.putExtra("tuteeOrTutor", "Tutees");
                startActivity(intent);
                return;
            }
        });
    }
    private void checkUserInfo() {
        DatabaseReference mTuteeDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Tutees").child(userId);
        mTuteeDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null && map.get("phone") != null) {
                        if (!map.get("name").equals("") && !map.get("phone").equals("")) {
                            mRequest.setEnabled(true);
                            mRequest.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                            mRequest.setTextColor(Color.parseColor("#ffffff"));
                            hasTuteeCompleteInfo = true;
                        } else {
                            Toast.makeText(getApplicationContext(), "Please complete your account details form using the Settings button at top.", Toast.LENGTH_LONG).show();
                            mRequest.setEnabled(false);
                            hasTuteeCompleteInfo = false;
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Please complete your account details form using the Settings button at top.", Toast.LENGTH_LONG).show();
                        mRequest.setEnabled(false);
                        hasTuteeCompleteInfo = false;
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please complete your account details form using the Settings button at top.", Toast.LENGTH_LONG).show();
                    mRequest.setEnabled(false);
                    hasTuteeCompleteInfo = false;
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    private int radius = 1;
    private boolean isTutorFound = false;
    //private String hasTutorAccepted; // 0 = not yet accepted/declined, 1 = accepted, 2 = declined
    private List<String> declinedTutorIds = new ArrayList<>();
    private String tutorFoundId;
    private DatabaseReference tutorRef;
    GeoQuery geoQuery;
    private void getClosestTutor() {
        DatabaseReference tutorLocation = FirebaseDatabase.getInstance().getReference().child("tutorsAvailable");
        GeoFire geoFire = new GeoFire(tutorLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!isTutorFound && isTutorRequested) {
                    if (declinedTutorIds.contains(key))
                    {
                        return;
                    }
                    tutorRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Tutors").child(key);
                    tutorRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                                tutorFoundId = dataSnapshot.getKey();
                                if (isTutorFound) {
                                    return;
                                }
                                isTutorFound = true;
                                tutorRef = tutorRef.child("requests").child(userId);
                                HashMap map = new HashMap();
                                map.put("topic", topic);
                                map.put("isAccepted", "0");
                                tutorRef.updateChildren(map);
                                getTutorResponse();
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
                }
            }
            @Override
            public void onKeyExited(String key) {
            }
            @Override
            public void onKeyMoved(String key, GeoLocation location) {
            }
            @Override
            public void onGeoQueryReady() {
                if (!isTutorFound) {
                    radius++;
                    getClosestTutor();
                }
            }
            @Override
            public void onGeoQueryError(DatabaseError error) {
            }
        });
    }
    private void getTutorResponse() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Users").child("Tutors").child(tutorFoundId).child("requests").child(userId).child("isAccepted");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String tutorResponse = dataSnapshot.getValue().toString();
                    if (tutorResponse.equals("0")) {
                        //not yet accepted/declined or record deleted
                        return;
                    }
                    if (tutorResponse.equals("true")) {
                        //tutor has accepted
                        mRequest.setEnabled(false);
                        getTutorLocation();
                        getTutorInfo();
                        getHasRideEnded();
                        mRequest.setEnabled(true);
                    } else {
                        //tutor has declined
                        isTutorFound = false;
                        declinedTutorIds.add(tutorFoundId);
                        tutorFoundId = null;
                        radius++;
                        getClosestTutor();
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    private void getTutorInfo() {
        mTutorInfo.setVisibility(View.VISIBLE);
        DatabaseReference mTutorDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Tutors").child(tutorFoundId);
        mTutorDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name") != null) {
                        mTutorName.setText(map.get("name").toString());
                    }
                    if(map.get("phone") != null) {
                        mTutorPhone.setText(map.get("phone").toString());
                    }
                    if(map.get("profileImageUrl") != null) {
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mTutorProfileImage);
                    }
                    int ratingSum = 0;
                    float ratingCount = 0;
                    float ratingAvg = 0;
                    for (DataSnapshot child : dataSnapshot.child("rating").getChildren()) {
                        ratingSum = ratingSum + Integer.valueOf(child.getValue().toString());
                        ratingCount++;
                    }
                    if (ratingCount != 0) {
                        ratingAvg = ratingSum/ratingCount;
                        mRatingBar.setRating(ratingAvg);
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    Marker mTutorMarker;
    private DatabaseReference tutorLocationRef;
    private ValueEventListener tutorLocationRefListener;
    private void getTutorLocation() {
        tutorLocationRef = FirebaseDatabase.getInstance().getReference().child("tutorsWorking").child(tutorFoundId).child("l");
        tutorLocationRefListener = tutorLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && isTutorRequested) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    mRequest.setText("Tutor found!");
                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng tutorLatLng = new LatLng(locationLat, locationLng);
                    if (mTutorMarker != null) {
                        mTutorMarker.remove();
                    }
                    Location loc1 = new Location("");
                    loc1.setLongitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);
                    Location loc2 = new Location("");
                    loc2.setLongitude(tutorLatLng.latitude);
                    loc2.setLongitude(tutorLatLng.longitude);
                    float distance = loc1.distanceTo(loc2);
                    if (distance < 100) {
                        mRequest.setText("Tutor's Here!");
                    } else {
                        mRequest.setText("Tutor Found: " + String.valueOf(distance));
                    }
                    mTutorMarker = mMap.addMarker(new MarkerOptions().position(tutorLatLng).title("Your Tutor").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_tutor)));
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    private DatabaseReference tutorHasEndedRef;
    private ValueEventListener tutorHasEndedRefListener;
    private void getHasRideEnded() {
        if (tutorFoundId == null) {
            endRide();
            return;
        }
        tutorHasEndedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Tutors").child(tutorFoundId).child("requests").child(userId);
        tutorHasEndedRefListener = tutorHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                } else {
                    endRide();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    private void enableActionButtons(boolean value) {
        mLogout.setEnabled(value);
        mHistory.setEnabled(value);
        mSettings.setEnabled(value);
        mTopic.setEnabled(value);
    }
    private void endRide() {
        if (geoQuery != null) {
            geoQuery.removeAllListeners();
        }
        if (tutorLocationRef != null) {
            tutorLocationRef.removeEventListener(tutorLocationRefListener);
        }
        if (tutorHasEndedRef != null) {
            tutorHasEndedRef.removeEventListener(tutorHasEndedRefListener);
            tutorHasEndedRef.removeValue();
        }
        if (!declinedTutorIds.isEmpty()) {
            for (int i = 0; i < declinedTutorIds.size(); i++) {
                DatabaseReference declinedTutorsRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Tutors").child(declinedTutorIds.get(i)).child("requests").child(userId);
                declinedTutorsRef.removeValue();
            }
            declinedTutorIds.clear();
        }
        if (tutorFoundId != null) {
            DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("tutorsWorking").child(tutorFoundId);
            GeoFire geoFire = new GeoFire(refWorking);
            geoFire.removeLocation(tutorFoundId, new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {
                }
            });
            tutorFoundId = null;
        }
        isTutorRequested = false;
        isTutorFound = false;
        radius = 1;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("tuteeRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
            }
        });
        if (pickupMarker != null) {
            pickupMarker.remove();
        }
        if (mTutorMarker != null) {
            mTutorMarker.remove();
        }
        mRequest.setText("Call For Help");
        enableActionButtons(true);
        mTutorInfo.setVisibility(View.GONE);
        mTutorName.setText("");
        mTutorPhone.setText("");
        mTutorProfileImage.setImageResource(R.mipmap.ic_default_user);
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(TuteeMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }
    @Override
    public void onLocationChanged(Location location) {
        if (getApplicationContext() != null) {
            mLastLocation = location;
            LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        }
    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(TuteeMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }
    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }
    final int LOCATION_REQUEST_CODE = 1;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode) {
            case LOCATION_REQUEST_CODE: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mapFragment.getMapAsync(this);
                } else {
                    Toast.makeText(getApplicationContext(), "Please provide the permission.", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
    }
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
