package com.example.codestreak;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Initialize views
        ImageButton backButton = findViewById(R.id.backButton);
        TextView versionText = findViewById(R.id.versionText);
        
        // Set version info
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            versionText.setText("Version " + version);
        } catch (PackageManager.NameNotFoundException e) {
            versionText.setText("Version 1.0.0");
        }

        // Back button
        backButton.setOnClickListener(v -> finish());

        // Setup feature cards
        setupFeatureCards();
        
        // Setup social links
        setupSocialLinks();
        
        // Setup developer info
        setupDeveloperInfo();
    }

    private void setupFeatureCards() {
        // Feature cards are defined in layout
        // You can add click listeners if needed
    }

    private void setupSocialLinks() {
        MaterialCardView githubCard = findViewById(R.id.githubCard);
        MaterialCardView linkedinCard = findViewById(R.id.linkedinCard);
        MaterialCardView emailCard = findViewById(R.id.emailCard);

        githubCard.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ADITYA-SHAKYA04/CODEStreak"));
            startActivity(browserIntent);
        });

        linkedinCard.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/aditya-shakya-86b517285/"));
            startActivity(browserIntent);
        });

        emailCard.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:adityashak04@gmail.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback for CodeStreak App");
            startActivity(Intent.createChooser(emailIntent, "Send feedback via email"));
        });
    }

    private void setupDeveloperInfo() {
        MaterialCardView rateCard = findViewById(R.id.rateCard);
        MaterialCardView shareCard = findViewById(R.id.shareCard);

        rateCard.setOnClickListener(v -> {
            // Open app in Play Store for rating
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName()));
                startActivity(intent);
            }
        });

        shareCard.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out CodeStreak!");
            shareIntent.putExtra(Intent.EXTRA_TEXT, 
                "Track your LeetCode progress with CodeStreak! 🚀\n\nDownload: https://play.google.com/store/apps/details?id=" + getPackageName());
            startActivity(Intent.createChooser(shareIntent, "Share CodeStreak"));
        });
    }
}
