package com.example.ninemenout;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.text.TextWatcher;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;

// USES INFORMATION FROM THE SEARCH BETS BUFFER ACTIVITY PASSED BY INTENT
public class SearchBetsActivity extends AppCompatActivity {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference betRef = db.collection("bets");
    private BetsAdapter adapter;
    String[] arr = new String[4];
    String[] readHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_bets);

        // SearchBetsActivity gets info from the buffer, passed by intent
        Bundle b = this.getIntent().getExtras();
        if(b != null)
            readHolder = b.getStringArray("terms");
        if(readHolder != null)
            setUpRecyclerView(readHolder);
        else {
            // default search values, mainly used for testing
            arr[0] = "any";
            arr[1] = "spread";
            arr[2] = "odds";
            setUpRecyclerView(arr);
        }

    }

    // display recyclerview based on the search terms
    private void setUpRecyclerView(String[] searchTerms) {
        // separate to reduce errors
//        String teamName = toCamelcase(searchTerms[0]);
        String teamName = updateStringToExactMatch(searchTerms[0]);
        String betType = searchTerms[1];
        String sortType = searchTerms[2];

        Query query;

        if(searchTerms[0].equals("any")){ // query when nothing specified, return all data
            query = betRef.whereEqualTo("active", 0).orderBy("type", Query.Direction.DESCENDING);
        } else { // query based on data supplied through intent
            query = betRef.whereEqualTo("active", 0)
                    .whereEqualTo("type", betType)
                    .whereEqualTo("home", teamName)
                    .orderBy(sortType, Query.Direction.DESCENDING);
        }
        // set view based on the query object
        FirestoreRecyclerOptions<Bets> options = new FirestoreRecyclerOptions.Builder<Bets>()
                .setQuery(query, Bets.class)
                .build();

        adapter = new BetsAdapter(options);

        // RECYCLER VIEW PROPERTY CONTROLS
        RecyclerView recyclerView = findViewById(R.id.recyclerSearch);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // set each item in the recycler view to have an on click
        // opens a new activity that lets users view + accept bets
        adapter.setOnItemClickListener(new BetsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(DocumentSnapshot documentSnapshot, int position) {
                String id = documentSnapshot.getId();
                Bundle viewBet = new Bundle();
                viewBet.putString("documentID", id);
                Intent intent = new Intent(getApplicationContext(), BetsViewerActivity.class);
                intent.putExtras(viewBet);
                startActivity(intent);
            }
        });

    }

    // sanitize input since firestore requires exact matches for queries
    // for example, changes "toronto raptors" to "Toronto Raptors"
    public static String updateStringToExactMatch(String team){
        String ret = "";
        boolean spaceFound = true;
        for(int i = 0; i < team.length(); i++){
            char x = team.charAt(i);
            if(x == ' ') {
                spaceFound = true;
                ret += x;
            } else{
                if(spaceFound) {
                    ret += toUpperCase(x);
                    spaceFound = false;
                } else
                    ret += toLowerCase(x);
            }
        }
        return ret;
    }

    @Override
    protected void onStart() {
        db = FirebaseFirestore.getInstance();
        betRef = db.collection("bets");
        super.onStart();
        adapter.startListening(); // so the adapter will update live
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening(); // so the adapter knows it needs to stop updating live
    }

}
