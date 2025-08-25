package com.example.codestreak;

import android.content.Context;
import android.widget.TextView;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import java.text.DecimalFormat;

public class CustomMarkerView extends MarkerView {
    
    private TextView markerTitle;
    private TextView markerValue;
    private TextView markerPercentage;
    private String[] labels = {"Easy", "Medium", "Hard"};
    private DecimalFormat decimalFormat;

    public CustomMarkerView(Context context) {
        super(context, R.layout.marker_view);
        
        markerTitle = findViewById(R.id.markerTitle);
        markerValue = findViewById(R.id.markerValue);
        markerPercentage = findViewById(R.id.markerPercentage);
        decimalFormat = new DecimalFormat("#.#");
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = (int) highlight.getX();
        
        if (index < labels.length) {
            markerTitle.setText(labels[index]);
            markerValue.setText((int) e.getY() + " problems");
            
            // Calculate percentage (assuming we have access to total)
            float percentage = (e.getY() / getTotalProblems()) * 100;
            markerPercentage.setText(decimalFormat.format(percentage) + "%");
        }
        
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2), -getHeight());
    }
    
    private float getTotalProblems() {
        // This should be updated to get actual total from MainActivity
        return 95f; // Default value
    }
}
