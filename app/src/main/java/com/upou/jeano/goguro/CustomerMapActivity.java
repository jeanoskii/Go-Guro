package com.upou.jeano.goguro;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
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

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private Button mLogout, mRequest, mSettings, mHistory, mSendMessage;
    private LatLng pickupLocation;
    private boolean isDriverRequested = false;
    private Marker pickupMarker;
    private String destination, requestService, topic, userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private LinearLayout mDriverInfo;
    private ImageView mDriverProfileImage;
    private TextView mDriverName, mDriverPhone, mDriverCar;
    private RadioGroup mRadioGroup;
    private LatLng destinationLatLng;
    private RatingBar mRatingBar;
    private AutoCompleteTextView mTopic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            mapFragment.getMapAsync(this);
        }

        destinationLatLng = new LatLng(0.0,0.0);

        mDriverInfo = (LinearLayout) findViewById(R.id.driverInfo);
        mDriverProfileImage = (ImageView) findViewById(R.id.driverProfileImage);
        mDriverCar = (TextView) findViewById(R.id.driverCar);
        mDriverName = (TextView) findViewById(R.id.driverName);
        mDriverPhone = (TextView) findViewById(R.id.driverPhone);
        mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        mRadioGroup.check(R.id.uberX);
        mRatingBar = (RatingBar) findViewById(R.id.ratingBar);
        mLogout = (Button) findViewById(R.id.logout);
        mSendMessage = (Button) findViewById(R.id.sendMessage);

        mTopic = (AutoCompleteTextView) findViewById(R.id.topic);

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
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(CustomerMapActivity.this, android.R.layout.simple_dropdown_item_1line, topics);
                        mTopic.setAdapter(adapter);
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
            }
        });

        mRequest = (Button) findViewById(R.id.request);
        mSettings = (Button) findViewById(R.id.settings);
        mHistory = (Button) findViewById(R.id.history);
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        mSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("smsto:" + mDriverPhone.getText()));
                intent.putExtra("sms_body", "Hi " + mDriverName.getText() + "! Can you help me with my lesson in " + topic + "? ");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });

        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isDriverRequested) {
                    getHasRideEnded();
                    endRide();
                } else {
                    isDriverRequested = true;
                    int selectId = mRadioGroup.getCheckedRadioButtonId();
                    final RadioButton radioButton = (RadioButton) findViewById(selectId);
                    requestService = radioButton.getText().toString();
                    topic = mTopic.getText().toString();
                    if(requestService.equals("") || topic.equals("")) {
                        Toast.makeText(getApplicationContext(), "Please select service/topic.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                        }
                    });

                    pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)));

                    mRequest.setText("Getting your Tutor...");

                    getClosestDriver();
                }
            }
        });
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerMapActivity.this, CustomerSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });

        mHistory.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(CustomerMapActivity.this, HistoryActivity.class);
                intent.putExtra("customerOrDriver", "Customers");
                startActivity(intent);
                return;
            }
        });

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                destination = place.getName().toString();
                destinationLatLng = place.getLatLng();
            }
            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
            }
        });

    }

    private int radius = 1;
    private boolean isDriverFound = false;
    //private String hasDriverAccepted; // 0 = not yet accepted/declined, 1 = accepted, 2 = declined
    private List<String> declinedDriverIds = new ArrayList<>();
    private String driverFoundId;
    private DatabaseReference driverRef;
    GeoQuery geoQuery;

    private void getClosestDriver() {
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");
        GeoFire geoFire = new GeoFire(driverLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!isDriverFound && isDriverRequested) {
                    if (declinedDriverIds.contains(key))
                    {
                        return;
                    }
                    driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);
                    driverRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                                driverFoundId = dataSnapshot.getKey();
                                if (isDriverFound) {
                                    return;
                                } /*
                                if (declinedDriverIds.contains(driverFoundId)) {
                                    driverFoundId = null;
                                    radius++;
                                    getClosestDriver();
                                    return;
                                } */
                                isDriverFound = true;
                                driverRef = driverRef.child("requests").child(userId);
                                HashMap map = new HashMap();
                                map.put("destination", destination);
                                map.put("destinationLat", destinationLatLng.latitude);
                                map.put("destinationLng", destinationLatLng.longitude);
                                map.put("topic", topic);
                                map.put("isAccepted", "0");
                                driverRef.updateChildren(map);
                                getDriverResponse();
                                /*
                                driverFoundId = dataSnapshot.getKey();
                                final DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("requests").child(userId);
                                driverRef.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            hasDriverAccepted = dataSnapshot.child("isAccepted").getValue().toString();
                                            if (hasDriverAccepted.equals("0")) {

                                            } else if (hasDriverAccepted.equals("false")) {

                                            } else { //driver accepted
                                                getDriverLocation();
                                                getDriverInfo();
                                                getHasRideEnded();
                                                mRequest.setText("Looking for Driver Location...");
                                            }
                                        } else {
                                            HashMap map = new HashMap();
                                            map.put("destination", destination);
                                            map.put("destinationLat", destinationLatLng.latitude);
                                            map.put("destinationLng", destinationLatLng.longitude);
                                            map.put("topic", topic);
                                            map.put("isAccepted", "0");
                                            driverRef.updateChildren(map);
                                            isDriverFound = true;
                                            //getAcceptedDriver();
                                        }
                                    }
                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                    }
                                });
                                */
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
                if (!isDriverFound /*|| hasDriverAccepted.equals("false")*/) {
                    radius++;
                    getClosestDriver();
                }
            }
            @Override
            public void onGeoQueryError(DatabaseError error) {
            }
        });
    }


    private void getDriverResponse() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("requests").child(userId).child("isAccepted");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String driverResponse = dataSnapshot.getValue().toString();
                    if (driverResponse.equals("0")) {
                        //not yet accepted/declined or record deleted
                        //getDriverResponse();
                        return;
                    }
                    if (driverResponse.equals("true")) {
                        //driver has accepted
                        mRequest.setEnabled(false);
                        getDriverLocation();
                        getDriverInfo();
                        getHasRideEnded();
                        mRequest.setEnabled(true);
                    } else {
                        //driver has declined
                        isDriverFound = false;
                        declinedDriverIds.add(driverFoundId);
                        driverFoundId = null;
                        radius++;
                        getClosestDriver();
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }


    private void getDriverInfo() {
        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference mDriverDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
        mDriverDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name") != null) {
                        mDriverName.setText(map.get("name").toString());
                    }
                    if(map.get("phone") != null) {
                        mDriverPhone.setText(map.get("phone").toString());
                    }
                    if(map.get("car") != null) {
                        mDriverCar.setText(map.get("car").toString());
                    }
                    if(map.get("profileImageUrl") != null) {
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mDriverProfileImage);
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

    Marker mDriverMarker;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;

    private void getDriverLocation() {
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("driversWorking").child(driverFoundId).child("l");
        driverLocationRefListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && isDriverRequested) {
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

                    LatLng driverLatLng = new LatLng(locationLat, locationLng);

                    if (mDriverMarker != null) {
                        mDriverMarker.remove();
                    }

                    Location loc1 = new Location("");
                    loc1.setLongitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLongitude(driverLatLng.latitude);
                    loc2.setLongitude(driverLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if (distance < 100) {
                        mRequest.setText("Tutor's Here!");
                    } else {
                        mRequest.setText("Tutor Found: " + String.valueOf(distance));
                    }

                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your Driver").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)));

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /*
    private DatabaseReference driverRequestEndedRef;
    private ValueEventListener driverRequestEndedRefListener;
    private DatabaseReference customerRequestEndedRef;
    private ValueEventListener customerRequestEndedRefListener;
    private void getHasDriverRequestEnded() {
        driverRequestEndedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("requests").child(userId);
        driverRequestEndedRefListener = driverRequestEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    return;
                } else {
                    getHadCustomerRequestEnded();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    private void getHadCustomerRequestEnded() {
        customerRequestEndedRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(userId);
        customerRequestEndedRefListener = customerRequestEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    getClosestDriver();
                } else {
                    isDriverFound = false;
                    endRide();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    */
    private DatabaseReference driverHasEndedRef;
    private ValueEventListener driverHasEndedRefListener;
    private void getHasRideEnded() {
        driverHasEndedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("requests").child(userId);
        driverHasEndedRefListener = driverHasEndedRef.addValueEventListener(new ValueEventListener() {
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

    private void endRide() {
        //if (geoQuery != null) {
            geoQuery.removeAllListeners();
        //}
        if (driverLocationRef != null) {
            driverLocationRef.removeEventListener(driverLocationRefListener);
        }
        if (driverHasEndedRef != null) {
            driverHasEndedRef.removeEventListener(driverHasEndedRefListener);
            driverHasEndedRef.removeValue();
        }
        if (!declinedDriverIds.isEmpty()) {
            for (int i = 0; i < declinedDriverIds.size(); i++) {
                DatabaseReference declinedDriversRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(declinedDriverIds.get(i)).child("requests").child(userId);
                declinedDriversRef.removeValue();
            }
        }
        /*
        if (driverRequestEndedRef != null) {
            driverRequestEndedRef.removeEventListener(driverRequestEndedRefListener);
            driverRequestEndedRef.removeValue();
        }
        if (customerRequestEndedRef != null) {
            customerRequestEndedRef.removeEventListener(customerRequestEndedRefListener);
            customerRequestEndedRef.removeValue();
        }
        */
        if (driverFoundId != null) {
            DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversWorking").child(driverFoundId);
            GeoFire geoFire = new GeoFire(refWorking);
            geoFire.removeLocation(driverFoundId, new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {
                }
            });
            driverFoundId = null;
        }
        isDriverRequested = false;
        isDriverFound = false;
        radius = 1;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
            }
        });
        if (pickupMarker != null) {
            pickupMarker.remove();
        }
        if (mDriverMarker != null) {
            mDriverMarker.remove();
        }
        mRequest.setText("Call Uber");
        mDriverInfo.setVisibility(View.GONE);
        mDriverName.setText("");
        mDriverPhone.setText("");
        mDriverCar.setText("");
        mDriverProfileImage.setImageResource(R.mipmap.ic_default_user);
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
            ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
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
            ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
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