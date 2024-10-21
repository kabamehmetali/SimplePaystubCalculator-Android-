package ca.gbc.comp3074.paystubcalculator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText hoursInput;
    private EditText overtimeHoursInput;
    private EditText hourlyRateInput;
    private EditText overtimeHourlyRateInput;
    private EditText taxRateInput;
    private Button resetBtn;
    private Button calculateBtn;
    private Button addDataBtn;
    private RecyclerView calculationView;
    private Spinner employeesSpinner;

    private SharedPreferences sharedPreferences;
    private CalculationResult savedResult;
    private CalculationAdapter calculationAdapter;
    private Employee selectedEmployee;
    private List<Employee> employeeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hoursInput = findViewById(R.id.hoursInput);
        overtimeHoursInput = findViewById(R.id.overtimeHoursInput);
        hourlyRateInput = findViewById(R.id.hourlyRateInput);
        overtimeHourlyRateInput = findViewById(R.id.overtimeHourlyRateInput);
        taxRateInput = findViewById(R.id.taxRateInput);
        resetBtn = findViewById(R.id.resetBtn);
        calculateBtn = findViewById(R.id.calculateBtn);
        addDataBtn = findViewById(R.id.addDataBtn); // Initialize add button
        calculationView = findViewById(R.id.calculationView);
        employeesSpinner = findViewById(R.id.employeesSpinner);

        sharedPreferences = getSharedPreferences("PaystubCalculator", MODE_PRIVATE);

        calculationAdapter = new CalculationAdapter();
        calculationView.setLayoutManager(new LinearLayoutManager(this));
        calculationView.setAdapter(calculationAdapter);

        loadInputs();
        loadCalculationResult();

        loadEmployees();

        ArrayAdapter<Employee> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, employeeList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        employeesSpinner.setAdapter(adapter);

        employeesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedEmployee = (Employee) parent.getSelectedItem();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        calculateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculatePay();
            }
        });

        addDataBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addPaystubToEmployee();
            }
        });

        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetInputs();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveInputs();
        saveCalculationResult();
    }


    private void loadInputs() {
        hoursInput.setText(sharedPreferences.getString("hoursInput", ""));
        overtimeHoursInput.setText(sharedPreferences.getString("overtimeHoursInput", ""));
        hourlyRateInput.setText(sharedPreferences.getString("hourlyRateInput", ""));
        overtimeHourlyRateInput.setText(sharedPreferences.getString("overtimeHourlyRateInput", ""));
        taxRateInput.setText(sharedPreferences.getString("taxRateInput", ""));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInputs();
        loadCalculationResult();
    }


    private void saveInputs() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("hoursInput", hoursInput.getText().toString());
        editor.putString("overtimeHoursInput", overtimeHoursInput.getText().toString());
        editor.putString("hourlyRateInput", hourlyRateInput.getText().toString());
        editor.putString("overtimeHourlyRateInput", overtimeHourlyRateInput.getText().toString());
        editor.putString("taxRateInput", taxRateInput.getText().toString());
        editor.apply();
    }

    private void resetInputs() {
        hoursInput.setText("");
        overtimeHoursInput.setText("");
        hourlyRateInput.setText("");
        overtimeHourlyRateInput.setText("");
        taxRateInput.setText("");

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("hoursInput");
        editor.remove("overtimeHoursInput");
        editor.remove("hourlyRateInput");
        editor.remove("overtimeHourlyRateInput");
        editor.remove("taxRateInput");
        editor.apply();

        savedResult = null;
        calculationAdapter.setSavedResult(null);
        calculationAdapter.notifyDataSetChanged();

        Toast.makeText(this, "Inputs and calculation results reset. Paystub data preserved.", Toast.LENGTH_SHORT).show();
    }

    private void calculatePay() {
        String hoursStr = hoursInput.getText().toString().trim();
        String hourlyRateStr = hourlyRateInput.getText().toString().trim();
        String taxRateStr = taxRateInput.getText().toString().trim();

        if (hoursStr.isEmpty() || hourlyRateStr.isEmpty() || taxRateStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double hoursWorked = Double.parseDouble(hoursStr);
        double hourlyRate = Double.parseDouble(hourlyRateStr);
        double taxRate = Double.parseDouble(taxRateStr);
        double overtimeHoursWorked = overtimeHoursInput.getText().toString().isEmpty() ? 0 : Double.parseDouble(overtimeHoursInput.getText().toString());
        double overtimeHourlyRate = overtimeHourlyRateInput.getText().toString().isEmpty() ? 0 : Double.parseDouble(overtimeHourlyRateInput.getText().toString());

        double regularPay = hoursWorked * hourlyRate;
        double overtimePay = overtimeHoursWorked * overtimeHourlyRate;
        double totalPay = regularPay + overtimePay;
        double taxAmount = totalPay * (taxRate / 100);
        double netPay = totalPay - taxAmount;

        savedResult = new CalculationResult(hoursWorked, overtimeHoursWorked, hourlyRate, overtimeHourlyRate, taxRate, regularPay, overtimePay, totalPay, taxAmount, netPay);

        calculationAdapter.setSavedResult(savedResult);


    }


    private void addPaystubToEmployee() {
        if (savedResult == null) {
            Toast.makeText(this, "Please calculate pay first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedEmployee != null) {
            String newTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            Paystub newPaystub = new Paystub(savedResult.netPay, newTime);

            List<Paystub> currentPaystubs = selectedEmployee.getPaystubs();

            currentPaystubs.add(newPaystub);

            selectedEmployee.setPaystubs(currentPaystubs);

            saveEmployees();

            Toast.makeText(this, "Paystub added for " + selectedEmployee.getName() + " at " + newTime, Toast.LENGTH_SHORT).show();

            savedResult = null;

            calculatePay();
        } else {
            Toast.makeText(this, "No employee selected", Toast.LENGTH_SHORT).show();
        }
    }







    private void saveEmployees() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        JSONArray jsonArray = new JSONArray();

        for (Employee employee : employeeList) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("id", employee.getId());
                jsonObject.put("name", employee.getName());
                jsonObject.put("lastName", employee.getLastName());
                jsonObject.put("role", employee.getRole());

                JSONArray paystubsArray = new JSONArray();
                for (Paystub paystub : employee.getPaystubs()) {
                    JSONObject paystubObject = new JSONObject();
                    paystubObject.put("netPay", paystub.getNetPay());
                    paystubObject.put("creationTime", paystub.getCreationTime());
                    paystubsArray.put(paystubObject);
                }
                jsonObject.put("paystubs", paystubsArray);

            } catch (Exception e) {
                e.printStackTrace();
            }
            jsonArray.put(jsonObject);
        }

        editor.putString("employeeList", jsonArray.toString());
        editor.apply();
    }



    private void saveCalculationResult() {
        if (savedResult == null) {
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("saved_hoursWorked", (float) savedResult.hoursWorked);
        editor.putFloat("saved_overtimeHoursWorked", (float) savedResult.overtimeHoursWorked);
        editor.putFloat("saved_hourlyRate", (float) savedResult.hourlyRate);
        editor.putFloat("saved_overtimeHourlyRate", (float) savedResult.overtimeHourlyRate);
        editor.putFloat("saved_taxRate", (float) savedResult.taxRate);
        editor.putFloat("saved_regularPay", (float) savedResult.regularPay);
        editor.putFloat("saved_overtimePay", (float) savedResult.overtimePay);
        editor.putFloat("saved_totalPay", (float) savedResult.totalPay);
        editor.putFloat("saved_taxAmount", (float) savedResult.taxAmount);
        editor.putFloat("saved_netPay", (float) savedResult.netPay);
        editor.apply();
    }


    private void loadCalculationResult() {
        if (sharedPreferences.contains("saved_hoursWorked")) {
            double hoursWorked = sharedPreferences.getFloat("saved_hoursWorked", 0);
            double overtimeHoursWorked = sharedPreferences.getFloat("saved_overtimeHoursWorked", 0);
            double hourlyRate = sharedPreferences.getFloat("saved_hourlyRate", 0);
            double overtimeHourlyRate = sharedPreferences.getFloat("saved_overtimeHourlyRate", 0);
            double taxRate = sharedPreferences.getFloat("saved_taxRate", 0);
            double regularPay = sharedPreferences.getFloat("saved_regularPay", 0);
            double overtimePay = sharedPreferences.getFloat("saved_overtimePay", 0);
            double totalPay = sharedPreferences.getFloat("saved_totalPay", 0);
            double taxAmount = sharedPreferences.getFloat("saved_taxAmount", 0);
            double netPay = sharedPreferences.getFloat("saved_netPay", 0);

            savedResult = new CalculationResult(hoursWorked, overtimeHoursWorked, hourlyRate,
                    overtimeHourlyRate, taxRate, regularPay, overtimePay, totalPay, taxAmount, netPay);

            calculationAdapter.setSavedResult(savedResult);
        } else {
            savedResult = null;
        }
    }

    private void loadEmployees() {
        String employeeJson = sharedPreferences.getString("employeeList", null);

        if (employeeJson != null) {
            try {
                JSONArray jsonArray = new JSONArray(employeeJson);
                employeeList.clear();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    int id = jsonObject.getInt("id");
                    String name = jsonObject.getString("name");
                    String lastName = jsonObject.getString("lastName");
                    String role = jsonObject.getString("role");
                    Employee employee = new Employee(id, name, lastName, role);

                    JSONArray paystubsArray = jsonObject.getJSONArray("paystubs");
                    List<Paystub> paystubList = new ArrayList<>();
                    for (int j = 0; j < paystubsArray.length(); j++) {
                        JSONObject paystubObject = paystubsArray.getJSONObject(j);
                        double netPay = paystubObject.getDouble("netPay");
                        String timeAdded = paystubObject.optString("creationTime", "");
                        paystubList.add(new Paystub(netPay, timeAdded));
                    }
                    employee.setPaystubs(paystubList);

                    employeeList.add(employee);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

            employeeList.add(new Employee(1, "Mehmet Ali", "Kaba", "Developer"));
            employeeList.add(new Employee(2, "Mehmet", "KABA", "Designer"));
            employeeList.add(new Employee(3, "Ali", "KABA", "Designer"));

        }
    }


    public class CalculationResult {
        public double hoursWorked;
        public double overtimeHoursWorked;
        public double hourlyRate;
        public double overtimeHourlyRate;
        public double taxRate;
        public double regularPay;
        public double overtimePay;
        public double totalPay;
        public double taxAmount;
        public double netPay;

        public CalculationResult(double hoursWorked, double overtimeHoursWorked, double hourlyRate,
                                 double overtimeHourlyRate, double taxRate, double regularPay,
                                 double overtimePay, double totalPay, double taxAmount, double netPay) {

            this.hoursWorked = hoursWorked;
            this.overtimeHoursWorked = overtimeHoursWorked;
            this.hourlyRate = hourlyRate;
            this.overtimeHourlyRate = overtimeHourlyRate;
            this.taxRate = taxRate;
            this.regularPay = regularPay;
            this.overtimePay = overtimePay;
            this.totalPay = totalPay;
            this.taxAmount = taxAmount;
            this.netPay = netPay;
        }
    }


    public class CalculationAdapter extends RecyclerView.Adapter<CalculationAdapter.ViewHolder> {

        private CalculationResult savedResult;

        public void setSavedResult(CalculationResult savedResult) {
            this.savedResult = savedResult;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (savedResult != null) {
                String info = String.format(Locale.getDefault(),
                        "Hours Worked: %.2f\nOvertime Hours: %.2f\nHourly Rate: $%.2f\nOvertime Rate: $%.2f\nTax Rate: %.2f%%\n\n" +
                                "Regular Pay: $%.2f\nOvertime Pay: $%.2f\nTotal Pay: $%.2f\nTax Amount: $%.2f\nNet Pay: $%.2f",
                        savedResult.hoursWorked,
                        savedResult.overtimeHoursWorked,
                        savedResult.hourlyRate,
                        savedResult.overtimeHourlyRate,
                        savedResult.taxRate,
                        savedResult.regularPay,
                        savedResult.overtimePay,
                        savedResult.totalPay,
                        savedResult.taxAmount,
                        savedResult.netPay);
                holder.infoTextView.setText(info);
            } else {
                holder.infoTextView.setText("No calculation made.");
            }
        }

        @Override
        public int getItemCount() {
            return 1;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView infoTextView;

            public ViewHolder(View itemView) {
                super(itemView);
                infoTextView = itemView.findViewById(android.R.id.text1);
            }
        }
    }

    // Employee class
    public class Employee {
        private int id;
        private String name;
        private String lastName;
        private String role;
        private List<Paystub> paystubs;


        public Employee(int id, String name, String lastName, String role) {
            this.id = id;
            this.name = name;
            this.lastName = lastName;
            this.role = role;
            this.paystubs = new ArrayList<>();
        }
        public void setPaystubs(List<Paystub> paystubs) {
            this.paystubs = paystubs;
        }
        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getLastName() {
            return lastName;
        }

        public String getRole() {
            return role;
        }

        public List<Paystub> getPaystubs() {
            return paystubs;
        }

        public void addPaystub(Paystub paystub) {
            this.paystubs.add(paystub);
        }

        @Override
        public String toString() {
            return name + " " + lastName;
        }
    }


    public class Paystub {
        private double netPay;
        private String creationTime;

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
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
       if (id == R.id.menu_employees) {
            startActivity(new Intent(MainActivity.this, EmployeesActivity.class));
            return true;
        } else if (id == R.id.menu_history) {
            startActivity(new Intent(MainActivity.this, HistoryActivity.class));
            return true;
        } else if (id == R.id.menu_about) {
            startActivity(new Intent(MainActivity.this, AboutActivity.class));
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}
