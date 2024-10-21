package ca.gbc.comp3074.paystubcalculator;

import android.content.SharedPreferences;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EmployeesActivity extends AppCompatActivity {

    private EditText nameInput, lastNameInput, roleInput;
    private TextView employeeIdTextView, salaryTextView;
    private Spinner employeesSpinner;
    private Button resetBtn, updateBtn, addBtn, deleteBtn;
    private RecyclerView paystubsRecyclerView;

    private List<Employee> employeeList = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private PaystubAdapter paystubAdapter;
    private Employee selectedEmployee;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employees);

        nameInput = findViewById(R.id.nameInput);
        lastNameInput = findViewById(R.id.lastNameInput);
        roleInput = findViewById(R.id.roleInput);
        employeeIdTextView = findViewById(R.id.employeeIdTextView);
        salaryTextView = findViewById(R.id.salaryTextView);
        employeesSpinner = findViewById(R.id.employeesSpinner);
        resetBtn = findViewById(R.id.resetBtn);
        updateBtn = findViewById(R.id.updateBtn);
        addBtn = findViewById(R.id.addBtn);
        deleteBtn = findViewById(R.id.deleteBtn);
        paystubsRecyclerView = findViewById(R.id.paystubsRecyclerView);

        sharedPreferences = getSharedPreferences("PaystubCalculator", MODE_PRIVATE);
        loadEmployees();

        ArrayAdapter<Employee> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, employeeList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        employeesSpinner.setAdapter(adapter);


        employeesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedEmployee = (Employee) parent.getSelectedItem();
                fillEmployeeData(selectedEmployee);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        paystubAdapter = new PaystubAdapter();
        paystubsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        paystubsRecyclerView.setAdapter(paystubAdapter);

        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewEmployee(adapter);
            }
        });

        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedEmployee != null) {
                    updateEmployeeData();
                }
            }
        });

        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetInputs();
            }
        });

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedEmployee != null) {
                    deleteEmployee(adapter);
                } else {
                    Toast.makeText(EmployeesActivity.this, "No employee selected", Toast.LENGTH_SHORT).show();
                }
            }
        });
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

    private void loadEmployees() {
        String employeeJson = sharedPreferences.getString("employeeList", null);
        if (employeeJson != null) {
            try {
                JSONArray jsonArray = new JSONArray(employeeJson);
                employeeList.clear(); // Clear the list before loading from SharedPreferences
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    int id = jsonObject.getInt("id");
                    String name = jsonObject.getString("name");
                    String lastName = jsonObject.getString("lastName");
                    String role = jsonObject.getString("role");

                    Employee employee = new Employee(id, name, lastName, role);

                    JSONArray paystubsArray = jsonObject.getJSONArray("paystubs");
                    for (int j = 0; j < paystubsArray.length(); j++) {
                        JSONObject paystubObject = paystubsArray.getJSONObject(j);
                        double netPay = paystubObject.getDouble("netPay");
                        String creationTime = paystubObject.getString("creationTime"); // Load the time
                        employee.addPaystub(new Paystub(netPay, creationTime));
                    }

                    employeeList.add(employee);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

            employeeList.add(new Employee(1, "Mehmet Ali", "KABA", "Developer"));
            employeeList.add(new Employee(2, "Mehmet", "KABA", "Designer"));
            employeeList.add(new Employee(3, "Ali", "KABA", "Programmer"));


        }
    }

    private void fillEmployeeData(Employee employee) {
        if (employee != null) {
            employeeIdTextView.setText(String.format("Employee ID: %d", employee.getId()));
            nameInput.setText(employee.getName());
            lastNameInput.setText(employee.getLastName());
            roleInput.setText(employee.getRole());

            salaryTextView.setText(String.format("Total Salary Earned: $%.2f", calculateTotalSalary(employee)));

            paystubAdapter.setPaystubs(employee.getPaystubs());

            if (employee.getPaystubs().isEmpty()) {
                findViewById(R.id.noPaystubsTextView).setVisibility(View.VISIBLE);
                paystubsRecyclerView.setVisibility(View.GONE);
            } else {
                findViewById(R.id.noPaystubsTextView).setVisibility(View.GONE);
                paystubsRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void resetInputs() {
        nameInput.setText("");
        lastNameInput.setText("");
        roleInput.setText("");
        employeeIdTextView.setText("Employee ID: -");
        salaryTextView.setText("Total Salary Earned: -");
    }

    private void addNewEmployee(ArrayAdapter<Employee> adapter) {
        String name = nameInput.getText().toString().trim();
        String lastName = lastNameInput.getText().toString().trim();
        String role = roleInput.getText().toString().trim();

        if (name.isEmpty() || lastName.isEmpty() || role.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int newId = employeeList.size() + 1;
        Employee newEmployee = new Employee(newId, name, lastName, role);
        employeeList.add(newEmployee);
        adapter.notifyDataSetChanged();
        employeesSpinner.setSelection(employeeList.size() - 1);
        saveEmployees();
        Toast.makeText(this, "Employee added", Toast.LENGTH_SHORT).show();
    }

    private void updateEmployeeData() {
        String name = nameInput.getText().toString().trim();
        String lastName = lastNameInput.getText().toString().trim();
        String role = roleInput.getText().toString().trim();

        selectedEmployee.setName(name);
        selectedEmployee.setLastName(lastName);
        selectedEmployee.setRole(role);

        saveEmployees();
        Toast.makeText(this, "Employee updated", Toast.LENGTH_SHORT).show();
    }


    private void deleteEmployee(ArrayAdapter<Employee> adapter) {
        if (selectedEmployee != null) {
            employeeList.remove(selectedEmployee);
            adapter.notifyDataSetChanged();
            resetInputs();
            saveEmployees();

            if (employeeList.isEmpty()) {
                employeeList.add(new Employee(0, "No employee exists", "", ""));
                adapter.notifyDataSetChanged();

                updateBtn.setEnabled(false);
                deleteBtn.setEnabled(false);
            }

            Toast.makeText(this, "Employee deleted", Toast.LENGTH_SHORT).show();
        }
    }

    private double calculateTotalSalary(Employee employee) {
        double totalSalary = 0;
        for (Paystub paystub : employee.getPaystubs()) {
            totalSalary += paystub.getNetPay();
        }
        return totalSalary;
    }

    public class PaystubAdapter extends RecyclerView.Adapter<PaystubAdapter.ViewHolder> {

        private List<Paystub> paystubs = new ArrayList<>();

        public void setPaystubs(List<Paystub> paystubs) {
            this.paystubs = paystubs;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Paystub paystub = paystubs.get(position);
            holder.paystubTextView.setText(String.format(Locale.getDefault(),
                    "Net Pay: $%.2f\nCreated on: %s",
                    paystub.getNetPay(), paystub.getCreationTime()));

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int clickedPosition = holder.getAdapterPosition();
                    if (clickedPosition != RecyclerView.NO_POSITION && clickedPosition < paystubs.size()) {
                        Paystub clickedPaystub = paystubs.get(clickedPosition);

                        v.animate().alpha(0).setDuration(500).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                paystubs.remove(clickedPaystub);
                                notifyItemRemoved(clickedPosition);
                                notifyItemRangeChanged(clickedPosition, paystubs.size());

                                if (selectedEmployee != null) {
                                    selectedEmployee.getPaystubs().remove(clickedPaystub);
                                    saveEmployees();
                                }

                                Toast.makeText(EmployeesActivity.this, "Paystub deleted", Toast.LENGTH_SHORT).show();
                            }
                        }).start();
                    } else {
                        Toast.makeText(EmployeesActivity.this, "Error deleting paystub. Try again.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
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

        public void setName(String name) {
            this.name = name;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public void addPaystub(Paystub paystub) {
            this.paystubs.add(paystub);
        }

        public void removePaystub(int index) {
            this.paystubs.remove(index);
        }

        @Override
        public String toString() {
            return name + " " + lastName;
        }
    }

    public class Paystub {
        private double netPay;
        private String creationTime; // New field for storing the creation time

        public Paystub(double netPay, String creationTime) {
            this.netPay = netPay;
            this.creationTime = creationTime; // Assign the creation time
        }

        public double getNetPay() {
            return netPay;
        }

        public void setNetPay(double netPay) {
            this.netPay = netPay;
        }

        public String getCreationTime() { // Getter for creation time
            return creationTime;
        }

        public void setCreationTime(String creationTime) { // Setter for creation time
            this.creationTime = creationTime;
        }
    }
}