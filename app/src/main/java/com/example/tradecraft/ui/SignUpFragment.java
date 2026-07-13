package com.example.tradecraft.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.tradecraft.R;
import com.example.tradecraft.viewmodel.AuthViewModel;
import com.google.android.material.textfield.TextInputLayout;

/** Sign-up screen with a confirm-password check. Shares AuthViewModel with SignInFragment via the host activity. */
public class SignUpFragment extends Fragment {

    private AuthViewModel viewModel;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private TextInputLayout confirmPasswordLayout;
    private ProgressBar progressBar;
    private TextView errorText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        emailInput = view.findViewById(R.id.email_input);
        passwordInput = view.findViewById(R.id.password_input);
        confirmPasswordLayout = view.findViewById(R.id.confirm_password_layout);
        confirmPasswordInput = view.findViewById(R.id.confirm_password_input);
        progressBar = view.findViewById(R.id.progress_bar);
        errorText = view.findViewById(R.id.error_text);

        Button signUpButton = view.findViewById(R.id.sign_up_button);
        TextView footerText = view.findViewById(R.id.footer_text);

        signUpButton.setOnClickListener(v -> submit());
        setupFooter(footerText, getString(R.string.footer_sign_up_prefix), getString(R.string.footer_sign_up_action),
                () -> {
                    viewModel.clearError();
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.auth_fragment_container, new SignInFragment())
                            .addToBackStack(null)
                            .commit();
                });

        observeViewModel();
    }

    private void submit() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        confirmPasswordLayout.setError(null);

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            showError(getString(R.string.error_empty_fields));
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError(getString(R.string.error_password_mismatch));
            return;
        }

        errorText.setVisibility(View.GONE);
        viewModel.signUp(email, password);
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading ->
                progressBar.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE));

        viewModel.getError().observe(getViewLifecycleOwner(), this::showError);
    }

    private void showError(String message) {
        if (message == null) {
            return;
        }
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    /** Builds a "prefix + emphasized, tappable action" footer line, e.g. "Already have an account? Sign in". */
    private void setupFooter(TextView footerText, String prefix, String action, Runnable onClick) {
        String full = prefix + action;
        SpannableString spannable = new SpannableString(full);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                onClick.run();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setColor(ContextCompat.getColor(requireContext(), R.color.brand_primary));
                ds.setFakeBoldText(true);
                ds.setUnderlineText(false);
            }
        };
        spannable.setSpan(clickableSpan, prefix.length(), full.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        footerText.setText(spannable);
        footerText.setMovementMethod(LinkMovementMethod.getInstance());
        footerText.setHighlightColor(Color.TRANSPARENT);
    }
}
