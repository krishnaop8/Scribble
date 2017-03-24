package com.example.ompra.scribble;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by ompra on 3/21/2017.
 */

public class Utils {
    public static String getUserId(){
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users").push();
        String[] url = databaseReference.toString().split("/");
        return url[url.length-1];
    }
}
