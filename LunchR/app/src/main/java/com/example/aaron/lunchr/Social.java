package com.example.aaron.lunchr;

import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.Iterator;

public class Social extends AppCompatActivity {

    DatabaseReference myRef;
    FirebaseDatabase database;
    SQLiteDatabase myDB;
    String chatMessage = "";
    String chatName = "anonymous";
    String tableName = "myTable";
    TextView t;

    /**
     * setup database connection, TextViews, and kick off the initial database read
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // connect to Firebase and retrieve the database. The contents of the database
        // will then be displayed inside a TextView widget
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();
        t = (TextView) findViewById(R.id.databaseEntries);
        readAndDisplayFirebaseDatabase();
    }

    /**
     * writes the current contents of the database to the screen, and
     * then rewrites it once a new entry has appeared in the database
     */
    protected void readAndDisplayFirebaseDatabase() {
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                DataSnapshot curr;
                Iterator<DataSnapshot> i = dataSnapshot.getChildren().iterator();
                t.setText("");
                while (i.hasNext()) {
                    curr = i.next();
                    System.out.println(curr.getKey());
                    writeToTextView(curr.getKey(), curr.getValue().toString());
                    storeChatHistory(curr.getKey(), curr.getValue().toString());
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                // do nothing on database error
            }
        });
    }

    /**
     * get a chat message from the user, using a dialog
     * @param view
     */
    protected void setChatMessage(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter New chat message");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                chatMessage = input.getText().toString();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    /**
     * prepare the chat message for sending - get a simple date and time from the
     * phone and then send that to the writeToDatabase() function
     * @param view
     */
    protected void sendChatMessage(View view) {
        String format = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
        format = format.replace(".", "");
        format = format.replace("am", "AM");
        format = format.replace("pm", "PM");
        writeToDatabase(format, chatName + ": " + chatMessage);
    }

    /**
     * get a username from the user using a dialog
     * @param view
     */
    protected void setChatName(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter your name");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                chatName = input.getText().toString();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    /**
     * copy the chat history into a local SQL database
     * additional help provided by http://sarangasl.blogspot.ie/2009/09/create-android-database.html
     * @param key
     * @param value
     */
    protected void storeChatHistory(String key, String value) {
        try {
            myDB = this.openOrCreateDatabase("chatHistory", MODE_PRIVATE, null);

            /* Create a Table in the Database. */
            myDB.execSQL("CREATE TABLE IF NOT EXISTS "
                    + tableName
                    + " (Field1 VARCHAR, Field2 VARCHAR);");

            // scrub the text for any escape characters
            key = key.replace("'", "");
            value = value.replace("'", "");

            /* Insert data to a Table*/
            myDB.execSQL("INSERT INTO "
                    + tableName
                    + " (Field1, Field2)"
                    + " VALUES ('" + key + "', '" + value + "');");
        } catch (Exception e) {
            Log.e("exception", e.toString());
        } finally {
            if (myDB != null) {
                myDB.close();
            }
        }
    }

    /**
     * currently unused. function available only if the internal database needs to be cleared
     */
    protected void clearDatabase() {
        getApplicationContext().deleteDatabase("chatHistory");
    }

    /**
     * write a key and value to the activity's text view
     * @param key
     * @param value
     */
    protected void writeToTextView(String key, String value) {
        String prev = t.getText().toString();
        t.setText(key + " " + value + "\n" + prev);
    }

    /**
     * write a key and value to the firebase database
     * @param key
     * @param value
     */
    protected void writeToDatabase(String key, String value) {
        myRef = database.getReference(key);
        myRef.setValue(value);
        // writeToTextView(key, value);
    }
}