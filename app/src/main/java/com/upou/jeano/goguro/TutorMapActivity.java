package com.upou.jeano.goguro;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
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

public class TutorMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, RoutingListener {
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private Button mLogout, mSettings, mRideStatus, mHistory, mAccept, mDecline;
    private int status = 0;
    private String tuteeId = "", acceptedTuteeId = "", topic, userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private LatLng pickupLatLng;
    private FusedLocationProviderClient mFusedLocationClient;
    private boolean isLoggingOut = false, hasTutorCompleteInfo;
    private LinearLayout mTuteeInfo, mAcceptDecline, mButtons;
    private ImageView mTuteeProfileImage;
    private TextView mTuteeName, mTuteePhone, mTuteeTopic;
    private ToggleButton mWorkingToggle;
    private static Bundle bundle = new Bundle();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_map);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        polylines = new ArrayList<>();
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(TutorMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            mapFragment.getMapAsync(this);
        }
        mTuteeInfo = findViewById(R.id.tuteeInfo);
        mButtons = findViewById(R.id.buttons);
        mTuteeProfileImage = findViewById(R.id.tuteeProfileImage);
        mTuteeName = findViewById(R.id.tuteeName);
        mTuteePhone = findViewById(R.id.tuteePhone);
        mTuteeTopic = findViewById(R.id.tuteeTopic);
        mLogout = findViewById(R.id.logout);
        mSettings = findViewById(R.id.settings);
        mHistory = findViewById(R.id.history);
        mWorkingToggle = findViewById(R.id.workingSwitch);
        mAccept = findViewById(R.id.accept);
        mDecline = findViewById(R.id.decline);
        mAcceptDecline = findViewById(R.id.acceptDecline);
        checkTutorInfo();
        mWorkingToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkTutorInfo();
                if (hasTutorCompleteInfo) {
                    if (isChecked) {
                        connectTutor();
                        getAssignedTutee();
                    } else {
                        disconnectTutor();
                        deleteRequests();
                        mTuteeInfo.setVisibility(View.GONE);
                    }
                } else {
                    mWorkingToggle.setChecked(false);
                }
            }
        });

        mAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Users").child("Tutors").child(userId).child("requests").child(tuteeId);
                acceptedTuteeId = tuteeId;
                tuteeId = "";
                HashMap map = new HashMap();
                map.put("isAccepted", true);
                ref.updateChildren(map);
                mAcceptDecline.setVisibility(View.GONE);
                mRideStatus.setVisibility(View.VISIBLE);
            }
        });

        mDecline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Users").child("Tutors").child(userId).child("requests").child(tuteeId);
                HashMap map = new HashMap();
                map.put("isAccepted", false);
                ref.updateChildren(map);
                mTuteeInfo.setVisibility(View.GONE);
                enableActionButtons(true);
                erasePolylines();
                //mWorkingToggle.setEnabled(true);
            }
        });

        mRideStatus = findViewById(R.id.rideStatus);
        mRideStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (status) {
                    case 1:
                        status = 2;
                        erasePolylines();
                        mRideStatus.setText("End Study Session");
                        break;
                    case 2:
                        recordRide();
                        endRide();
                        break;
                }
            }
        });

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isLoggingOut = true;
                disconnectTutor();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(TutorMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mHistory.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(TutorMapActivity.this, HistoryActivity.class);
                intent.putExtra("tuteeOrTutor", "Tutors");
                startActivity(intent);
                return;
            }
        });

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(TutorMapActivity.this, TutorSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });
    }
    private void checkTutorInfo() {
            DatabaseReference mTutorDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Tutors").child(userId);
            mTutorDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                        Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                        if (map.get("name") != null && map.get("phone") != null) {
                            if (!map.get("name").equals("") && !map.get("phone").equals("")) {
                                hasTutorCompleteInfo = true;
                            } else {
                                Toast.makeText(getApplicationContext(), "Please complete your account details form using the Settings button at top.", Toast.LENGTH_LONG).show();
                                hasTutorCompleteInfo = false;

                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "Please complete your account details form using the Settings button at top.", Toast.LENGTH_LONG).show();
                            hasTutorCompleteInfo = false;
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Please complete your account details form using the Settings button at top.", Toast.LENGTH_LONG).show();
                        hasTutorCompleteInfo = false;
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
    }

    private void deleteRequests() {
        DatabaseReference requestsRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Tutors").child(userId).child("requests");
        requestsRef.removeValue();
    }

    private void recordRide() {
        DatabaseReference tutorRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Tutors").child(userId).child("history");
        DatabaseReference tuteeRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Tutees").child(acceptedTuteeId).child("history");
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("History");
        String requestId = historyRef.push().getKey();
        tutorRef.child(requestId).setValue(true);
        tuteeRef.child(requestId).setValue(true);

        HashMap map = new HashMap();
        map.put("tutor", userId);
        map.put("tutee", acceptedTuteeId);
        map.put("rating", 0);
        map.put("timestamp", getCurrentTimestamp());
        map.put("topic", topic);
        map.put("pickupLat", pickupLatLng.latitude);
        map.put("pickupLng", pickupLatLng.longitude);

        historyRef.child(requestId).updateChildren(map);

    }

    private Long getCurrentTimestamp() {
        Long timestamp = System.currentTimeMillis() / 1000;
        return timestamp;
    }

    private void getAssignedTutee() {
        DatabaseReference assignedTuteeRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Tutors").child(userId).child("requests");
        assignedTuteeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        if (child.child("isAccepted").getValue().equals("0")) {
                            mTuteeInfo.setVisibility(View.VISIBLE);
                            enableActionButtons(false);
                            //mWorkingToggle.setEnabled(false);

                            status = 1;
                            tuteeId = child.getKey();
                            getAssignedTuteePickupLocation();
                            getAssignedTuteeInfo();
                            getAssignedTuteeTopic();
                        }
                    }
                } else {
                    //endRide();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void getAssignedTuteeTopic() {
        DatabaseReference assignedTuteeRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Tutors").child(userId).child("requests").child(tuteeId).child("topic");
        assignedTuteeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    topic = dataSnapshot.getValue().toString();
                    mTuteeTopic.setText("Topic: " + topic);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    private void getAssignedTuteeInfo() {
        DatabaseReference mTuteeDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Tutees").child(tuteeId);
        mTuteeDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null) {
                        mTuteeName.setText(map.get("name").toString());
                    }
                    if (map.get("phone") != null) {
                        mTuteePhone.setText(map.get("phone").toString());
                    }
                    if (map.get("profileImageUrl") != null) {
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mTuteeProfileImage);
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private Marker pickupMarker;
    private DatabaseReference assignedTuteePickupLocationRef;
    private ValueEventListener assignedTuteePickupLocationRefListener;

    private void getAssignedTuteePickupLocation() {
        assignedTuteePickupLocationRef = FirebaseDatabase.getInstance().getReference().child("tuteeRequest").child(tuteeId).child("l");
        assignedTuteePickupLocationRefListener = assignedTuteePickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !tuteeId.equals("")) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    pickupLatLng = new LatLng(locationLat, locationLng);
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup Location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)));
                    getRouteToMarker(pickupLatLng);
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
        mWorkingToggle.setEnabled(value);
    }

    private void getRouteToMarker(LatLng pickupLatLng) {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), pickupLatLng)
                .build();
        routing.execute();
    }

    private void endRide() {
        mRideStatus.setText("Start Study Session");
        erasePolylines();

        DatabaseReference tutorRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Tutors").child(userId).child("requests").child(acceptedTuteeId);
        tutorRef.removeValue();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("tuteeRequest");
        GeoFire geoFire = new GeoFire(ref);
        if (acceptedTuteeId != "") {
            geoFire.removeLocation(acceptedTuteeId, new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {
                }
            });
            acceptedTuteeId = "";
        }
        if (pickupMarker != null) {
            pickupMarker.remove();
        }
        if (assignedTuteePickupLocationRefListener != null) {
            assignedTuteePickupLocationRef.removeEventListener(assignedTuteePickupLocationRefListener);
        }
        mTuteeInfo.setVisibility(View.GONE);
        enableActionButtons(true);
        mTuteeName.setText("");
        mTuteePhone.setText("");
        mTuteeProfileImage.setImageResource(R.mipmap.ic_default_user);
        mAcceptDecline.setVisibility(View.VISIBLE);
        mRideStatus.setVisibility(View.GONE);
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
            ActivityCompat.requestPermissions(TutorMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        buildGoogleApiClient();
        LatLng manila = new LatLng(14.5818, 120.9770);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(manila, 8f));
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

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {

                    mLastLocation = location;
                    LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

                    DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("tutorsAvailable");
                    DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("tutorsWorking");
                    GeoFire geoFireAvailable = new GeoFire(refAvailable);
                    GeoFire geoFireWorking = new GeoFire(refWorking);
                    switch (acceptedTuteeId) {
                        case "":
                            geoFireWorking.removeLocation(userId, new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                }
                            });
                            geoFireAvailable.setLocation(userId, new GeoLocation(latLng.latitude, latLng.longitude), new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                }
                            });
                            break;
                        default:
                            geoFireAvailable.removeLocation(userId, new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                }
                            });
                            geoFireWorking.setLocation(userId, new GeoLocation(latLng.latitude, latLng.longitude), new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                }
                            });
                            break;
                    }
                }
            }
        }
    };

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
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

        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(TutorMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                    }
                    mapFragment.getMapAsync(this);
                } else {
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    private void connectTutor() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(TutorMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        mMap.setMyLocationEnabled(true);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    private void disconnectTutor() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("tutorsAvailable");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
            }
        });
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(false);
    }

    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};
    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {
    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingCancelled() {
    }

    private void erasePolylines() {
        for(Polyline line : polylines) {
            line.remove();
        }
        polylines.clear();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

}
