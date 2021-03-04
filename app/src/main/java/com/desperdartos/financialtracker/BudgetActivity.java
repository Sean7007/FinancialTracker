package com.desperdartos.financialtracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.joda.time.DateTime;
import org.joda.time.Months;
import org.joda.time.MutableDateTime;
import org.joda.time.Weeks;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class BudgetActivity extends AppCompatActivity {
    private FloatingActionButton fab;

    //Inputting Item into Database
    private DatabaseReference budgetRef;
    private FirebaseAuth mAuth;
    private ProgressDialog loader;

    private TextView totalBudgetAmountTextView;
    private RecyclerView recyclerView;

    //Variables for updating item amount
    private String post_key= "";
    private String item = "";
    private int amount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        //Firebase Authentication
        mAuth = FirebaseAuth.getInstance();
        budgetRef = FirebaseDatabase.getInstance().getReference().child("budget").child(mAuth.getCurrentUser().getUid());
        loader = new ProgressDialog(this);

        //Text that is retrieved from Firebase and Displayed in screen
        totalBudgetAmountTextView = findViewById(R.id.totalBudgetAmountTextView);
        recyclerView = findViewById(R.id.recyclerView);

        //Layout manager to display retrieved data on screen
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.setReverseLayout(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        budgetRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalAmount = 0;

                for(DataSnapshot snap: snapshot.getChildren()){
                    Data data = snap.getValue(Data.class);
                    totalAmount += data.getAmount();
                    String sTotal = String.valueOf("Month Budget: P" + totalAmount);
                    totalBudgetAmountTextView.setText(sTotal);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addItem();
            }
        });
    }

    private void addItem() {
        AlertDialog.Builder myDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View myView = inflater.inflate(R.layout.input_layout, null);
        myDialog.setView(myView);

        final AlertDialog dialog = myDialog.create();
        dialog.setCancelable(false);

        final Spinner itemSpinner = myView.findViewById(R.id.itemsspinner);
        final EditText amount = myView.findViewById(R.id.amount);
        final Button cancel = myView.findViewById(R.id.cancel);
        final Button save = myView.findViewById(R.id.save);

        save.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                //Check for validations
                String budgetAmount = amount.getText().toString();
                String budgetItem = itemSpinner.getSelectedItem().toString();

                if (TextUtils.isEmpty(budgetAmount)){
                    amount.setError("Amount is required!");
                    return;
                }
                if (budgetItem.equals("Select Item")){
                    Toast.makeText(BudgetActivity.this,"Select a valid Item", Toast.LENGTH_SHORT).show();
                }
                else{
                    loader.setMessage("Adding a budget item");
                    loader.setCanceledOnTouchOutside(false);
                    loader.show();

                    String id = budgetRef.push().getKey();
                    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                    Calendar calendar = Calendar.getInstance();
                    String date = dateFormat.format(calendar.getTime());

                    MutableDateTime epoch = new MutableDateTime();
                    epoch.setDate(0);
                    DateTime now = new DateTime();
                    Weeks weeks = Weeks.weeksBetween(epoch, now);
                    Months months = Months.monthsBetween(epoch, now);

                    Data data = new Data(budgetItem, date, id, null, Integer.parseInt(budgetAmount), months.getMonths(), weeks.getWeeks());
                    budgetRef.child(id).setValue(data).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                Toast.makeText(BudgetActivity.this, "Budget Item added SUCCESSFULLY!", Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(BudgetActivity.this,task.getException().toString(), Toast.LENGTH_SHORT).show();
                            }
                            loader.dismiss();
                        }
                    });
                }
                dialog.dismiss();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseRecyclerOptions<Data> options = new FirebaseRecyclerOptions.Builder<Data>()
                .setQuery(budgetRef,Data.class)
                .build();
        FirebaseRecyclerAdapter<Data, MyViewHolder> adapter = new FirebaseRecyclerAdapter<Data, MyViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int i, @NonNull Data data) {
                myViewHolder.setItemAmount("Allocated amount: P" + data.getAmount());
                myViewHolder.setDate("On: " +data.getDate());
                myViewHolder.setItemName("BudgetItem: " +data.getItem());

               myViewHolder.notes.setVisibility(View.GONE);

                switch(data.getItem()){
                    case "Transport":
                        myViewHolder.imageView.setImageResource(R.drawable.ic_transport);
                        break;
                    case "Food":
                        myViewHolder.imageView.setImageResource(R.drawable.ic_food1);
                        break;
                    case "House":
                        myViewHolder.imageView.setImageResource(R.drawable.ic_house);
                        break;
                    case "Entertainment":
                        myViewHolder.imageView.setImageResource(R.drawable.ic_entertainment2);
                        break;
                    case "Education":
                        myViewHolder.imageView.setImageResource(R.drawable.ic_education);
                        break;
                    case "Charity":
                        myViewHolder.imageView.setImageResource(R.drawable.ic_charity);
                        break;
                    case "Apparel":
                        myViewHolder.imageView.setImageResource(R.drawable.ic_apparel);
                        break;
                    case "Health":
                        myViewHolder.imageView.setImageResource(R.drawable.ic_health);
                        break;
                    case "Personal":
                        myViewHolder.imageView.setImageResource(R.drawable.ic_personal);
                        break;
                    case "Other":
                        myViewHolder.imageView.setImageResource(R.drawable.ic_other);
                        break;
                }
                //Updating item amount
                myViewHolder.view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        post_key = getRef(i).getKey();
                        item = data.getItem();
                        amount = data.getAmount();
                        updateData();
                    }
                });
            }

            @NonNull
            @Override
            public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.retrieve_layout,parent,false);
                return new MyViewHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
        adapter.startListening();
        adapter.notifyDataSetChanged();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{
        //Declaration
        View view;
        public ImageView imageView;
        public TextView notes;

        public MyViewHolder(@NonNull View itemView){
            super(itemView);
            view = itemView;
            imageView = itemView.findViewById(R.id.imageView);
            notes = itemView.findViewById(R.id.note);
        }

        public void setItemName(String itemName){
            TextView item = view.findViewById(R.id.item);
            item.setText(itemName);
        }

        public void setItemAmount(String itemAmount){
            TextView amount = view.findViewById(R.id.amount);
            amount.setText(itemAmount);
        }
        public void setDate(String itemDate){
            TextView date = view.findViewById(R.id.date);
        }
    }

    //Method which updates data
    private void updateData(){
        AlertDialog.Builder myDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.update_layout,null);

        //display on screen
        myDialog.setView(view);
        final AlertDialog dialog = myDialog.create();
        final TextView mItem = view.findViewById(R.id.itemName);
        final EditText mAmount = view.findViewById(R.id.amount);
        final EditText mNotes = view.findViewById(R.id.note);

        mNotes.setVisibility(View.GONE);
        mItem.setText(item);

        mAmount.setText(String.valueOf(amount));
        mAmount.setSelection(String.valueOf(amount).length());

        Button delBut = view.findViewById(R.id.btnDelete);
        Button btnUpdate = view.findViewById(R.id.btnUpdate);
        //
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                amount = Integer.parseInt(mAmount.getText().toString());
                DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                Calendar calendar = Calendar.getInstance();
                String date = dateFormat.format(calendar.getTime());

                MutableDateTime epoch = new MutableDateTime();
                epoch.setDate(0);
                DateTime now = new DateTime();
                Weeks weeks = Weeks.weeksBetween(epoch, now);
                Months months = Months.monthsBetween(epoch, now);

                Data data = new Data(item, date, post_key, null, amount, months.getMonths(), weeks.getWeeks());
                budgetRef.child(post_key).setValue(data).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            Toast.makeText(BudgetActivity.this, "Updated SUCCESSFULLY!", Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(BudgetActivity.this,task.getException().toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                dialog.dismiss();
            }
        });
        delBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                budgetRef.child(post_key).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            Toast.makeText(BudgetActivity.this, "Deleted SUCCESSFULLY!", Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(BudgetActivity.this,task.getException().toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}