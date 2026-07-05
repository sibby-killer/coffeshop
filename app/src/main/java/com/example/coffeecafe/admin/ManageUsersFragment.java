package com.example.coffeecafe.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coffeecafe.R;
import com.example.coffeecafe.auth.AuthManager;
import com.example.coffeecafe.config.SupabaseApi;
import com.example.coffeecafe.models.Profile;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ManageUsersFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private UserAdapter adapter;
    private List<Profile> userList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.orders_recycler);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);

        userList = new ArrayList<>();
        adapter = new UserAdapter(userList);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadUsers();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUsers();
    }

    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        String token = AuthManager.getInstance(getContext()).getAccessToken();

        new Thread(() -> {
            try {
                String query = "select=*&order=created_at.desc";
                String response = SupabaseApi.getInstance().get("profiles", query, token);

                Gson gson = new Gson();
                Profile[] users = gson.fromJson(response, Profile[].class);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (users != null && users.length > 0) {
                            userList.clear();
                            for (Profile u : users) {
                                userList.add(u);
                            }
                            adapter.notifyDataSetChanged();
                        } else {
                            emptyView.setVisibility(View.VISIBLE);
                        }
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        emptyView.setText("Failed to load: " + e.getMessage());
                        emptyView.setVisibility(View.VISIBLE);
                    });
                }
            }
        }).start();
    }

    private void updateUser(Profile user, String newName, String newPhone, String newRole) {
        progressBar.setVisibility(View.VISIBLE);
        String token = AuthManager.getInstance(getContext()).getAccessToken();

        new Thread(() -> {
            try {
                String json = "{\"full_name\":\"" + escapeJson(newName) +
                        "\",\"phone\":\"" + escapeJson(newPhone) +
                        "\",\"role\":\"" + escapeJson(newRole) + "\"}";

                SupabaseApi.getInstance().patch("profiles", "id=eq." + user.getId(), json, token);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "User updated", Toast.LENGTH_SHORT).show();
                        loadUsers();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        }).start();
    }

    private void deleteUser(Profile user) {
        progressBar.setVisibility(View.VISIBLE);
        String token = AuthManager.getInstance(getContext()).getAccessToken();

        new Thread(() -> {
            try {
                SupabaseApi.getInstance().delete("profiles", "id=eq." + user.getId(), token);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "User deleted", Toast.LENGTH_SHORT).show();
                        loadUsers();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        }).start();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private final List<Profile> users;

        UserAdapter(List<Profile> users) {
            this.users = users;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            Profile user = users.get(position);

            holder.userName.setText(user.getFullName() != null ? user.getFullName() : "No Name");
            holder.userEmail.setText(getEmailForUser(user));
            holder.userPhone.setText("Phone: " + (user.getPhone() != null ? user.getPhone() : "N/A"));
            holder.userRole.setText(user.getRole() != null ? user.getRole().toUpperCase() : "UNKNOWN");

            // Role spinner setup
            String[] roles = {"customer", "shop_owner", "admin"};
            ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(
                    holder.itemView.getContext(),
                    android.R.layout.simple_spinner_item,
                    roles
            );
            roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            holder.editRoleSpinner.setAdapter(roleAdapter);

            // Set current role in spinner
            if (user.getRole() != null) {
                int roleIndex = Arrays.asList(roles).indexOf(user.getRole());
                if (roleIndex >= 0) {
                    holder.editRoleSpinner.setSelection(roleIndex);
                }
            }

            // Pre-fill edit fields
            holder.editNameInput.setText(user.getFullName());
            holder.editPhoneInput.setText(user.getPhone());

            // Edit button
            holder.editUserBtn.setOnClickListener(v -> {
                holder.editSection.setVisibility(View.VISIBLE);
                holder.actionButtons.setVisibility(View.GONE);
            });

            // Cancel button
            holder.cancelEditBtn.setOnClickListener(v -> {
                holder.editSection.setVisibility(View.GONE);
                holder.actionButtons.setVisibility(View.VISIBLE);
                holder.editNameInput.setText(user.getFullName());
                holder.editPhoneInput.setText(user.getPhone());
            });

            // Save button
            holder.saveUserBtn.setOnClickListener(v -> {
                String newName = holder.editNameInput.getText().toString().trim();
                String newPhone = holder.editPhoneInput.getText().toString().trim();
                String newRole = holder.editRoleSpinner.getSelectedItem().toString();

                if (newName.isEmpty()) {
                    Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                holder.editSection.setVisibility(View.GONE);
                holder.actionButtons.setVisibility(View.VISIBLE);
                updateUser(user, newName, newPhone, newRole);
            });

            // Delete button
            holder.deleteUserBtn.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(getContext())
                        .setTitle("Delete User")
                        .setMessage("Are you sure you want to delete " + user.getFullName() + "?")
                        .setPositiveButton("Delete", (dialog, which) -> deleteUser(user))
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        private String getEmailForUser(Profile user) {
            // Email is not in profiles table, show user ID instead
            return "ID: " + (user.getId() != null ? user.getId().substring(0, Math.min(8, user.getId().length())) : "N/A");
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView userName, userEmail, userPhone, userRole;
            EditText editNameInput, editPhoneInput;
            Spinner editRoleSpinner;
            Button editUserBtn, deleteUserBtn, saveUserBtn, cancelEditBtn;
            View editSection, actionButtons;

            UserViewHolder(@NonNull View itemView) {
                super(itemView);
                userName = itemView.findViewById(R.id.user_name);
                userEmail = itemView.findViewById(R.id.user_email);
                userPhone = itemView.findViewById(R.id.user_phone);
                userRole = itemView.findViewById(R.id.user_role);
                editNameInput = itemView.findViewById(R.id.edit_name_input);
                editPhoneInput = itemView.findViewById(R.id.edit_phone_input);
                editRoleSpinner = itemView.findViewById(R.id.edit_role_spinner);
                editUserBtn = itemView.findViewById(R.id.edit_user_btn);
                deleteUserBtn = itemView.findViewById(R.id.delete_user_btn);
                saveUserBtn = itemView.findViewById(R.id.save_user_btn);
                cancelEditBtn = itemView.findViewById(R.id.cancel_edit_btn);
                editSection = itemView.findViewById(R.id.edit_section);
                actionButtons = itemView.findViewById(R.id.action_buttons);
            }
        }
    }
}
