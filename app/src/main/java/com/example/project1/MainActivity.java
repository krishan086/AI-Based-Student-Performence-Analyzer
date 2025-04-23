package com.example.project1;

import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.textfield.TextInputEditText;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText studentNameInput, studentIdInput, mathScoreInput, scienceScoreInput,
            englishScoreInput, attendanceInput, studyHoursInput;
    private Button analyzeButton;
    private CardView resultCard, progressCard;
    private TextView performanceScoreText, performanceCategoryText, recommendationsList, predictionText;
    private RadarChart radarChart;

    private Interpreter tflite;
    private static final String MODEL_PATH = "student_performance_model.tflite";

    // Performance categories
    private static final String[] CATEGORIES = {"Needs Improvement", "Average", "Good", "Excellent"};

    // Recommendations based on performance
    private static final Map<String, String[]> RECOMMENDATIONS = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize views
        initViews();

        // Initialize recommendations
        initRecommendations();

        // Load TensorFlow Lite model
        try {
            tflite = new Interpreter(loadModelFile());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading AI model, using fallback analysis", Toast.LENGTH_SHORT).show();
        }

        // Set click listener for analyze button
        analyzeButton.setOnClickListener(v -> analyzePerformance());
    }

    private void initViews() {
        studentNameInput = findViewById(R.id.studentNameInput);
        studentIdInput = findViewById(R.id.studentIdInput);
        mathScoreInput = findViewById(R.id.mathScoreInput);
        scienceScoreInput = findViewById(R.id.scienceScoreInput);
        englishScoreInput = findViewById(R.id.englishScoreInput);
        attendanceInput = findViewById(R.id.attendanceInput);
        studyHoursInput = findViewById(R.id.studyHoursInput);
        analyzeButton = findViewById(R.id.analyzeButton);
        resultCard = findViewById(R.id.resultCard);
        progressCard = findViewById(R.id.progressCard);
        performanceScoreText = findViewById(R.id.performanceScoreText);
        performanceCategoryText = findViewById(R.id.performanceCategoryText);
        recommendationsList = findViewById(R.id.recommendationsList);
        predictionText = findViewById(R.id.predictionText);
        radarChart = findViewById(R.id.radarChart);
    }

    private void initRecommendations() {
        RECOMMENDATIONS.put("Needs Improvement", new String[]{
                "• Establish a regular study schedule with short, focused sessions",
                "• Seek additional help from teachers or tutors",
                "• Focus on improving attendance and participation",
                "• Use visual aids and practice problems to reinforce concepts",
                "• Join a study group for peer support"
        });

        RECOMMENDATIONS.put("Average", new String[]{
                "• Increase study hours gradually",
                "• Identify and focus on weaker subjects",
                "• Practice active learning techniques like summarizing and teaching concepts",
                "• Improve note-taking skills",
                "• Set specific, achievable goals for each subject"
        });

        RECOMMENDATIONS.put("Good", new String[]{
                "• Challenge yourself with advanced problems",
                "• Develop deeper understanding of concepts through research",
                "• Help peers to reinforce your own knowledge",
                "• Maintain consistent study habits",
                "• Explore related topics of interest"
        });

        RECOMMENDATIONS.put("Excellent", new String[]{
                "• Consider mentoring other students",
                "• Explore advanced topics beyond the curriculum",
                "• Participate in academic competitions",
                "• Maintain your balanced approach to studies",
                "• Consider research projects or internships in areas of interest"
        });
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void analyzePerformance() {
        // Validate inputs
        if (!validateInputs()) {
            Toast.makeText(this, "Please fill all fields with valid values", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get input values
        float mathScore = Float.parseFloat(mathScoreInput.getText().toString());
        float scienceScore = Float.parseFloat(scienceScoreInput.getText().toString());
        float englishScore = Float.parseFloat(englishScoreInput.getText().toString());
        float attendance = Float.parseFloat(attendanceInput.getText().toString());
        float studyHours = Float.parseFloat(studyHoursInput.getText().toString());

        // Normalize inputs (assuming scores are 0-100, attendance 0-100%, study hours 0-40)
        float[] normalizedInputs = {
                mathScore / 100f,
                scienceScore / 100f,
                englishScore / 100f,
                attendance / 100f,
                studyHours / 40f  // Assuming max study hours is 40 per week
        };

        // If TensorFlow model is available, use it for prediction
        float performanceScore;
        if (tflite != null) {
            performanceScore = runInference(normalizedInputs);
        } else {
            // Fallback to simple weighted average if model not available
            performanceScore = calculatePerformanceScore(normalizedInputs);
        }

        // Scale performance score to 0-100
        performanceScore *= 100;

        // Determine performance category
        String category = determineCategory(performanceScore);

        // Display results
        displayResults(performanceScore, category);

        // Create radar chart
        createRadarChart(mathScore, scienceScore, englishScore, attendance, studyHours);

        // Show result cards
        resultCard.setVisibility(View.VISIBLE);
        progressCard.setVisibility(View.VISIBLE);
    }

    private boolean validateInputs() {
        if (studentNameInput.getText().toString().trim().isEmpty() ||
                studentIdInput.getText().toString().trim().isEmpty() ||
                mathScoreInput.getText().toString().trim().isEmpty() ||
                scienceScoreInput.getText().toString().trim().isEmpty() ||
                englishScoreInput.getText().toString().trim().isEmpty() ||
                attendanceInput.getText().toString().trim().isEmpty() ||
                studyHoursInput.getText().toString().trim().isEmpty()) {
            return false;
        }

        try {
            float mathScore = Float.parseFloat(mathScoreInput.getText().toString());
            float scienceScore = Float.parseFloat(scienceScoreInput.getText().toString());
            float englishScore = Float.parseFloat(englishScoreInput.getText().toString());
            float attendance = Float.parseFloat(attendanceInput.getText().toString());
            float studyHours = Float.parseFloat(studyHoursInput.getText().toString());

            return mathScore >= 0 && mathScore <= 100 &&
                    scienceScore >= 0 && scienceScore <= 100 &&
                    englishScore >= 0 && englishScore <= 100 &&
                    attendance >= 0 && attendance <= 100 &&
                    studyHours >= 0 && studyHours <= 168; // Max hours in a week
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private float runInference(float[] inputs) {
        // Prepare input and output tensors
        float[][] inputArray = new float[1][inputs.length];
        float[][] outputArray = new float[1][1];

        // Copy inputs to input tensor
        System.arraycopy(inputs, 0, inputArray[0], 0, inputs.length);

        // Run inference
        tflite.run(inputArray, outputArray);

        // Return predicted performance score
        return outputArray[0][0];
    }

    private float calculatePerformanceScore(float[] normalizedInputs) {
        // Simple weighted average as fallback
        float weights[] = {0.25f, 0.25f, 0.2f, 0.15f, 0.15f};
        float score = 0;

        for (int i = 0; i < normalizedInputs.length; i++) {
            score += normalizedInputs[i] * weights[i];
        }

        return score;
    }

    private String determineCategory(float performanceScore) {
        if (performanceScore < 50) {
            return CATEGORIES[0]; // Needs Improvement
        } else if (performanceScore < 70) {
            return CATEGORIES[1]; // Average
        } else if (performanceScore < 85) {
            return CATEGORIES[2]; // Good
        } else {
            return CATEGORIES[3]; // Excellent
        }
    }

    private void displayResults(float performanceScore, String category) {
        // Display performance score
        performanceScoreText.setText(String.format("Overall Performance Score: %.1f/100", performanceScore));

        // Display performance category
        performanceCategoryText.setText(String.format("Performance Category: %s", category));

        // Display recommendations
        StringBuilder recommendations = new StringBuilder();
        for (String recommendation : RECOMMENDATIONS.get(category)) {
            recommendations.append(recommendation).append("\n");
        }
        recommendationsList.setText(recommendations.toString());

        // Display prediction
        String prediction;
        if (performanceScore >= 85) {
            prediction = "Student is likely to excel in future academic endeavors.";
        } else if (performanceScore >= 70) {
            prediction = "Student shows good potential for academic success with consistent effort.";
        } else if (performanceScore >= 50) {
            prediction = "Student may benefit from additional support in specific areas.";
        } else {
            prediction = "Student requires significant intervention to improve academic performance.";
        }
        predictionText.setText("AI Prediction: " + prediction);
    }

    private void createRadarChart(float mathScore, float scienceScore, float englishScore,
                                  float attendance, float studyHours) {
        // Normalize study hours to 0-100 scale for visualization
        float normalizedStudyHours = Math.min(studyHours * 2.5f, 100);

        // Create radar chart entries
        List<RadarEntry> entries = new ArrayList<>();
        entries.add(new RadarEntry(mathScore));
        entries.add(new RadarEntry(scienceScore));
        entries.add(new RadarEntry(englishScore));
        entries.add(new RadarEntry(attendance));
        entries.add(new RadarEntry(normalizedStudyHours));

        RadarDataSet dataSet = new RadarDataSet(entries, "Performance Metrics");
        dataSet.setColor(Color.rgb(103, 58, 183));
        dataSet.setFillColor(Color.rgb(103, 58, 183));
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(180);
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(false);

        RadarData data = new RadarData(dataSet);
        radarChart.setData(data);

        // Configure radar chart
        radarChart.getDescription().setEnabled(false);
        radarChart.setWebLineWidth(1f);
        radarChart.setWebColor(Color.LTGRAY);
        radarChart.setWebLineWidthInner(1f);
        radarChart.setWebColorInner(Color.LTGRAY);
        radarChart.setWebAlpha(100);

        // Configure X axis (categories)
        XAxis xAxis = radarChart.getXAxis();
        xAxis.setTextSize(12f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(
                new String[]{"Math", "Science", "English", "Attendance", "Study Hours"}));

        // Configure Y axis (values)
        radarChart.getYAxis().setAxisMinimum(0f);
        radarChart.getYAxis().setAxisMaximum(100f);
        radarChart.getYAxis().setDrawLabels(false);

        radarChart.getLegend().setEnabled(false);
        radarChart.invalidate(); // Refresh chart
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tflite != null) {
            tflite.close();
        }
    }
}