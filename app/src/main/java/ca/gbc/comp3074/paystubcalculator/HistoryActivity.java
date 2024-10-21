package ca.gbc.comp3074.paystubcalculator;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView paystubsRecyclerView;
    private TextView totalAmountTextView;
    private TextView noPaystubTextView;
    private EditText numberOfPayrollsEditText;
    private Button applyFilterButton;
    private SharedPreferences sharedPreferences;
    private PaystubAdapter paystubAdapter;
    private List<Paystub> allPaystubs = new ArrayList<>();
    private List<Paystub> displayedPaystubs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        totalAmountTextView = findViewById(R.id.totalAmountTextView);
        noPaystubTextView = findViewById(R.id.noPaystubTextView); // New "No paystub" message
        paystubsRecyclerView = findViewById(R.id.paystubsRecyclerView);
        numberOfPayrollsEditText = findViewById(R.id.numberOfPayrollsEditText);
        applyFilterButton = findViewById(R.id.applyFilterButton);

        sharedPreferences = getSharedPreferences("PaystubCalculator", MODE_PRIVATE);

        loadAllPaystubs();

        applyDefaultFilter();

        updateTotalAmount();

        paystubAdapter = new PaystubAdapter(displayedPaystubs);
        paystubsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        paystubsRecyclerView.setAdapter(paystubAdapter);

        applyFilterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyFilter();
            }
        });
    }

    private void loadAllPaystubs() {
        allPaystubs = new ArrayList<>();
        String employeeJson = sharedPreferences.getString("employeeList", null);
        if (employeeJson != null) {
            try {
                JSONArray jsonArray = new JSONArray(employeeJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject employeeObject = jsonArray.getJSONObject(i);

                    JSONArray paystubsArray = employeeObject.getJSONArray("paystubs");
                    for (int j = 0; j < paystubsArray.length(); j++) {
                        JSONObject paystubObject = paystubsArray.getJSONObject(j);
                        double netPay = paystubObject.getDouble("netPay");
                        String creationTime = paystubObject.optString("creationTime", "");
                        Paystub paystub = new Paystub(netPay, creationTime);

                        String employeeName = employeeObject.getString("name") + " " + employeeObject.getString("lastName");
                        paystub.setEmployeeName(employeeName);

                        allPaystubs.add(paystub);
                    }
                }

                Collections.sort(allPaystubs, (p1, p2) -> p2.getCreationTime().compareTo(p1.getCreationTime()));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void applyDefaultFilter() {
        if (allPaystubs.isEmpty()) {
            paystubsRecyclerView.setVisibility(View.GONE);
            noPaystubTextView.setVisibility(View.VISIBLE);
        } else {
            paystubsRecyclerView.setVisibility(View.VISIBLE);
            noPaystubTextView.setVisibility(View.GONE);

            if (allPaystubs.size() > 5) {
                displayedPaystubs = allPaystubs.subList(0, 5); // Show only the first 5 (most recent)
            } else {
                displayedPaystubs = new ArrayList<>(allPaystubs); // Show all if fewer than 5
            }
        }
    }

    private void updateTotalAmount() {
        double totalAmount = calculateTotalAmount();
        totalAmountTextView.setText(String.format(Locale.getDefault(), "Total Amount: $%.2f", totalAmount));
    }

    private double calculateTotalAmount() {
        double total = 0;
        for (Paystub paystub : displayedPaystubs) {
            total += paystub.getNetPay();
        }
        return total;
    }

    private void applyFilter() {
        String numberStr = numberOfPayrollsEditText.getText().toString().trim();
        if (numberStr.isEmpty()) {
            displayedPaystubs = new ArrayList<>(allPaystubs);
        } else {
            int number = Integer.parseInt(numberStr);
            if (number <= 0) {
                Toast.makeText(this, "Please enter a positive number", Toast.LENGTH_SHORT).show();
                return;
            }
            displayedPaystubs = new ArrayList<>();
            for (int i = 0; i < Math.min(number, allPaystubs.size()); i++) {
                displayedPaystubs.add(allPaystubs.get(i));
            }
        }

        if (displayedPaystubs.isEmpty()) {
            paystubsRecyclerView.setVisibility(View.GONE);
            noPaystubTextView.setVisibility(View.VISIBLE);
        } else {
            paystubsRecyclerView.setVisibility(View.VISIBLE);
            noPaystubTextView.setVisibility(View.GONE);
        }

        paystubAdapter.setPaystubs(displayedPaystubs);
        updateTotalAmount();
    }

    public class PaystubAdapter extends RecyclerView.Adapter<PaystubAdapter.ViewHolder> {

        private List<Paystub> paystubs;

        public PaystubAdapter(List<Paystub> paystubs) {
            this.paystubs = paystubs;
        }

        public void setPaystubs(List<Paystub> paystubs) {
            this.paystubs = paystubs;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Paystub paystub = paystubs.get(position);
            holder.paystubTextView.setText(String.format(Locale.getDefault(),
                    "Employee: %s\nNet Pay: $%.2f\nDate: %s",
                    paystub.getEmployeeName(),
                    paystub.getNetPay(),
                    paystub.getCreationTime()));
        }

        @Override
        public int getItemCount() {
            return paystubs.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView paystubTextView;

            public ViewHolder(View itemView) {
                super(itemView);
                paystubTextView = itemView.findViewById(android.R.id.text1);
            }
        }
    }

    public class Paystub {
        private double netPay;
        private String creationTime;
        private String employeeName; // Added to store employee name

        public Paystub(double netPay, String creationTime) {
            this.netPay = netPay;
            this.creationTime = creationTime;
        }

        public double getNetPay() {
            return netPay;
        }

        public void setNetPay(double netPay) {
            this.netPay = netPay;
        }

        public String getCreationTime() {
            return creationTime;
        }

        public void setCreationTime(String creationTime) {
            this.creationTime = creationTime;
        }

        public String getEmployeeName() {
            return employeeName;
        }

        public void setEmployeeName(String employeeName) {
            this.employeeName = employeeName;
        }
    }
}
