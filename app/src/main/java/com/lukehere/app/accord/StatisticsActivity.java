package com.lukehere.app.accord;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.xw.repo.BubbleSeekBar;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static com.lukehere.app.accord.ModifyAttendeeActivity.NUMBER_OF_STALLS;

public class StatisticsActivity extends AppCompatActivity {
    private FirebaseFirestore firestoreDB;
    private ProgressBar mProgressBar;
    private Spinner mStallSpinner;
    private LinearLayout mFeedbackPanel;
    private BubbleSeekBar mBubbleSeekBar1, mBubbleSeekBar2, mBubbleSeekBar3, mBubbleSeekBar4, mBubbleSeekBar5;
    private TextView mTotalNumberOfAttendeesTextView;
    private TextView mNumberOfChristitesTextView, mNumberOfNonChristitesTextView;
    private TextView mNumberOfRegisteredAttendeesHeaderTextView, mNumberOfFeedbackEntriesHeaderTextView;
    private TextView mRegisteredNumberOfAttendeesTextView, mFeedbackNumberOfAttendeesTextView;
    private Button mDownloadReportButton;

    private boolean mTotalCountDoneLoading = false, mFeedbackCountDoneLoading = false;
    private ArrayList<Feedback> mFeedbackList;

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 13;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        setup();
        refreshDataset();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDataset();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_statistics, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.statistics_refresh:
                refreshDataset();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setup() {
        firestoreDB = FirebaseFirestore.getInstance();

        mProgressBar = findViewById(R.id.statistics_loading_progress_bar);

        mTotalNumberOfAttendeesTextView = findViewById(R.id.total_number_of_attendees_text_view);
        mNumberOfChristitesTextView = findViewById(R.id.number_of_christites_text_view);
        mNumberOfNonChristitesTextView = findViewById(R.id.number_of_non_christites_text_view);
        mRegisteredNumberOfAttendeesTextView = findViewById(R.id.registered_number_of_attendees_text_view);
        mFeedbackNumberOfAttendeesTextView = findViewById(R.id.feedback_number_of_attendees_text_view);

        mNumberOfRegisteredAttendeesHeaderTextView = findViewById(R.id.header_registered_number_of_attendees_text_view);
        mNumberOfFeedbackEntriesHeaderTextView = findViewById(R.id.header_feedback_number_of_attendees_text_view);

        mStallSpinner = findViewById(R.id.statistics_stall_spinner);

        mFeedbackPanel = findViewById(R.id.statistics_feedback_panel_layout);
        TextView mQuestion1 = findViewById(R.id.statistics_feedback_question_one);
        TextView mQuestion2 = findViewById(R.id.statistics_feedback_question_two);
        TextView mQuestion3 = findViewById(R.id.statistics_feedback_question_three);
        TextView mQuestion4 = findViewById(R.id.statistics_feedback_question_four);
        TextView mQuestion5 = findViewById(R.id.statistics_feedback_question_five);

        mBubbleSeekBar1 = findViewById(R.id.statistics_seekbar_1);
        mBubbleSeekBar2 = findViewById(R.id.statistics_seekbar_2);
        mBubbleSeekBar3 = findViewById(R.id.statistics_seekbar_3);
        mBubbleSeekBar4 = findViewById(R.id.statistics_seekbar_4);
        mBubbleSeekBar5 = findViewById(R.id.statistics_seekbar_5);

        mDownloadReportButton = findViewById(R.id.download_report_button);

        mStallSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    mFeedbackPanel.setVisibility(View.GONE);
                    mNumberOfRegisteredAttendeesHeaderTextView.setVisibility(View.GONE);
                    mNumberOfFeedbackEntriesHeaderTextView.setVisibility(View.GONE);
                    mRegisteredNumberOfAttendeesTextView.setVisibility(View.GONE);
                    mFeedbackNumberOfAttendeesTextView.setVisibility(View.GONE);
                    mDownloadReportButton.setVisibility(View.GONE);
                } else {
                    refreshDataset();
                }

                mDownloadReportButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkForStoragePermission();
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Do nothing
            }
        });
    }

    private void refreshDataset() {
        mProgressBar.setVisibility(View.VISIBLE);
        mTotalCountDoneLoading = false;
        mFeedbackCountDoneLoading = false;

        firestoreDB.collection("daksh_attendees")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ArrayList<Attendee> attendeeList = new ArrayList<>();

                            for (DocumentSnapshot doc : Objects.requireNonNull(task.getResult())) {
                                Attendee attendee = doc.toObject(Attendee.class);
                                attendeeList.add(attendee);
                            }

                            int totalNumberOfAttendees = attendeeList.size();

                            if (totalNumberOfAttendees < 1000) {
                                mTotalNumberOfAttendeesTextView.setTextColor(Color.BLACK);
                            } else {
                                mTotalNumberOfAttendeesTextView.setTextColor(Color.BLUE);
                            }

                            mTotalNumberOfAttendeesTextView.setText(String.valueOf(totalNumberOfAttendees));

                            int numberOfChristites = 0;
                            int numberOfNonChristites = 0;

                            for (Attendee attendee : attendeeList) {
                                if (attendee.getAttendeeType() != null) {
                                    if (attendee.getAttendeeType().equals(getResources().getStringArray(R.array.attendee_type)[0])) {
                                        numberOfChristites++;
                                    } else {
                                        numberOfNonChristites++;
                                    }
                                } else {
                                    numberOfChristites++;
                                }
                            }

                            mNumberOfChristitesTextView.setText(String.valueOf(numberOfChristites));
                            mNumberOfNonChristitesTextView.setText(String.valueOf(numberOfNonChristites));

                            mTotalCountDoneLoading = true;
                            checkAndDisableProgressBarIfDoneLoading();
                        }
                    }
                });

        if (mStallSpinner.getSelectedItemPosition() != 0) {
            firestoreDB.collection("daksh_feedback").document("required_feedback_document").collection(mStallSpinner.getSelectedItemPosition() + "")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                mFeedbackList = new ArrayList<>();

                                for (DocumentSnapshot doc : Objects.requireNonNull(task.getResult())) {
                                    Feedback feedback = doc.toObject(Feedback.class);
                                    mFeedbackList.add(feedback);
                                }

                                int registeredNumberOfAttendees = 0;
                                int feedbackNumberOfAttendees = 0;
                                float averageOfQuestionOne = 0.0f;
                                float averageOfQuestionTwo = 0.0f;
                                float averageOfQuestionThree = 0.0f;
                                float averageOfQuestionFour = 0.0f;
                                float averageOfQuestionFive = 0.0f;

                                for (Feedback currentFeedback : mFeedbackList) {
                                    registeredNumberOfAttendees++;

                                    if (currentFeedback.getFeedback1() != -3) {
                                        feedbackNumberOfAttendees++;

                                        averageOfQuestionOne += currentFeedback.getFeedback1();
                                        averageOfQuestionTwo += currentFeedback.getFeedback2();
                                        averageOfQuestionThree += currentFeedback.getFeedback3();
                                        averageOfQuestionFour += currentFeedback.getFeedback4();
                                        averageOfQuestionFive += currentFeedback.getFeedback5();
                                    }
                                }

                                if (feedbackNumberOfAttendees != 0) {
                                    averageOfQuestionOne /= feedbackNumberOfAttendees;
                                    averageOfQuestionTwo /= feedbackNumberOfAttendees;
                                    averageOfQuestionThree /= feedbackNumberOfAttendees;
                                    averageOfQuestionFour /= feedbackNumberOfAttendees;
                                    averageOfQuestionFive /= feedbackNumberOfAttendees;

                                    mNumberOfRegisteredAttendeesHeaderTextView.setVisibility(View.VISIBLE);
                                    mRegisteredNumberOfAttendeesTextView.setVisibility(View.VISIBLE);

                                    if (mStallSpinner.getSelectedItemPosition() <= NUMBER_OF_STALLS) {
                                        mNumberOfFeedbackEntriesHeaderTextView.setVisibility(View.VISIBLE);
                                        mFeedbackNumberOfAttendeesTextView.setVisibility(View.VISIBLE);
                                    } else {
                                        mNumberOfFeedbackEntriesHeaderTextView.setVisibility(View.GONE);
                                        mFeedbackNumberOfAttendeesTextView.setVisibility(View.GONE);
                                    }

                                    mFeedbackPanel.setVisibility(View.VISIBLE);

                                } else {
                                    averageOfQuestionOne = 0.0f;
                                    averageOfQuestionTwo = 0.0f;
                                    averageOfQuestionThree = 0.0f;
                                    averageOfQuestionFour = 0.0f;
                                    averageOfQuestionFive = 0.0f;

                                    mNumberOfRegisteredAttendeesHeaderTextView.setVisibility(View.VISIBLE);
                                    mRegisteredNumberOfAttendeesTextView.setVisibility(View.VISIBLE);

                                    if (mStallSpinner.getSelectedItemPosition() <= NUMBER_OF_STALLS) {
                                        mNumberOfFeedbackEntriesHeaderTextView.setVisibility(View.VISIBLE);
                                        mFeedbackNumberOfAttendeesTextView.setVisibility(View.VISIBLE);
                                    } else {
                                        mNumberOfFeedbackEntriesHeaderTextView.setVisibility(View.GONE);
                                        mFeedbackNumberOfAttendeesTextView.setVisibility(View.GONE);
                                    }

                                    mFeedbackPanel.setVisibility(View.GONE);
                                }

                                mRegisteredNumberOfAttendeesTextView.setText(String.valueOf(registeredNumberOfAttendees));
                                mFeedbackNumberOfAttendeesTextView.setText(String.valueOf(feedbackNumberOfAttendees));

                                mBubbleSeekBar1.setProgress(averageOfQuestionOne);
                                mBubbleSeekBar2.setProgress(averageOfQuestionTwo);
                                mBubbleSeekBar3.setProgress(averageOfQuestionThree);
                                mBubbleSeekBar4.setProgress(averageOfQuestionFour);
                                mBubbleSeekBar5.setProgress(averageOfQuestionFive);

                                mDownloadReportButton.setVisibility(View.VISIBLE);

                                mFeedbackCountDoneLoading = true;
                                checkAndDisableProgressBarIfDoneLoading();
                            }
                        }
                    });
        } else {
            mFeedbackCountDoneLoading = true;
            checkAndDisableProgressBarIfDoneLoading();
        }
    }

    private void checkAndDisableProgressBarIfDoneLoading() {
        if (mTotalCountDoneLoading && mFeedbackCountDoneLoading) {
            mProgressBar.setVisibility(View.GONE);
        }
    }

    private void checkForStoragePermission() {
        if (ContextCompat.checkSelfPermission(StatisticsActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(StatisticsActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            downloadReport();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    downloadReport();
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.permission_denied), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void downloadReport() {
        Snackbar.make(findViewById(R.id.statistics_outer_view), getString(R.string.statistics_data_being_exported), Snackbar.LENGTH_LONG).show();

        String[] columns;
        if (mStallSpinner.getSelectedItemPosition() <= NUMBER_OF_STALLS) {
            columns = new String[]{"Registration Number", getString(R.string.regular_question_one), getString(R.string.regular_question_two), getString(R.string.regular_question_three), getString(R.string.regular_question_four), getString(R.string.regular_question_five)};
        } else {
            columns = new String[]{"Registration Number"};
        }

        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("Report");

        Row headerRow = sheet.createRow(0);

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
        }

        int rowNum = 1;
        for (Feedback feedback : mFeedbackList) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(feedback.getAttendeeRegistrationNumber());

            if (mStallSpinner.getSelectedItemPosition() <= NUMBER_OF_STALLS && feedback.getFeedback1() != -3.0) {
                row.createCell(1).setCellValue(feedback.getFeedback1());
                row.createCell(2).setCellValue(feedback.getFeedback2());
                row.createCell(3).setCellValue(feedback.getFeedback3());
                row.createCell(4).setCellValue(feedback.getFeedback4());
                row.createCell(5).setCellValue(feedback.getFeedback5());
            } else if (mStallSpinner.getSelectedItemPosition() <= NUMBER_OF_STALLS && feedback.getFeedback1() == -3.0) {
                row.createCell(1).setCellValue(getString(R.string.feedback_not_provided));
                row.createCell(2).setCellValue(getString(R.string.feedback_not_provided));
                row.createCell(3).setCellValue(getString(R.string.feedback_not_provided));
                row.createCell(4).setCellValue(getString(R.string.feedback_not_provided));
                row.createCell(5).setCellValue(getString(R.string.feedback_not_provided));
            }
        }

        String filePath = Environment.getExternalStorageDirectory().toString();
        File file = new File(filePath + File.separator, "Feedback Report (" + mStallSpinner.getSelectedItem() + ") -" + new Date().getTime() + ".xls");
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            workbook.write(fos);
            Objects.requireNonNull(fos).flush();
            fos.close();
            workbook.close();
            Snackbar.make(findViewById(R.id.statistics_outer_view), getString(R.string.statistics_data_exported_successfully) + " " + filePath, Snackbar.LENGTH_LONG).show();
        } catch (IOException e) {
            Snackbar.make(findViewById(R.id.statistics_outer_view), getString(R.string.statistics_data_export_failed), Snackbar.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}