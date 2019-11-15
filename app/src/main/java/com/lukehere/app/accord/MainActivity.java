package com.lukehere.app.accord;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {
    private FirebaseFirestore firestoreDB;
    private RecyclerView mAttendeeListRecyclerView;

    private SwipeRefreshLayout pullToRefresh;
    private AttendeeAdapter mAttendeeAdapter;

    private static final int OPEN_EDITOR_ACTIVITY = 7;

    private int mBackCount = 0;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 13;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firestoreDB = FirebaseFirestore.getInstance();
        mAttendeeListRecyclerView = findViewById(R.id.attendee_list_recycler_view);
        mAttendeeListRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));

        pullToRefresh = findViewById(R.id.pull_to_refresh);
        pullToRefresh.setColorSchemeColors(Color.BLUE);

        loadAttendeesList();

        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadAttendeesList();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        mAttendeeAdapter.startListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBackCount = 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.editor:
                Intent openEditorActivity = new Intent(MainActivity.this, ModifyAttendeeActivity.class);
                startActivityForResult(openEditorActivity, OPEN_EDITOR_ACTIVITY);
                return true;
            case R.id.statistics:
                Intent openStatisticsActivity = new Intent(MainActivity.this, StatisticsActivity.class);
                startActivity(openStatisticsActivity);
                return true;
            case R.id.export:
                exportToFileStorage();
                return true;
            case R.id.about:
                displayAboutPopup();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void loadAttendeesList() {
        CollectionReference mAttendeesCollection = firestoreDB.collection("daksh_attendees");
        Query baseQuery = mAttendeesCollection.orderBy("name", Query.Direction.ASCENDING);
        PagedList.Config config = new PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPrefetchDistance(5)
                .setPageSize(20)
                .build();
        FirestorePagingOptions<Attendee> options = new FirestorePagingOptions.Builder<Attendee>()
                .setLifecycleOwner(this)
                .setQuery(baseQuery, config, Attendee.class)
                .build();

        mAttendeeAdapter = new AttendeeAdapter(MainActivity.this, pullToRefresh, options, new AttendeeAdapter.ListItemClickListener() {
            @Override
            public void onListItemClick(int clickedItemRegistrationNumber) {
                Intent i = new Intent(MainActivity.this, ModifyAttendeeActivity.class);
                i.putExtra("registration_number", clickedItemRegistrationNumber);
                startActivityForResult(i, OPEN_EDITOR_ACTIVITY);
            }
        });

        mAttendeeListRecyclerView.setAdapter(mAttendeeAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == OPEN_EDITOR_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK) {
                loadAttendeesList();
            }
        }
    }

    private void displayAboutPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(getString(R.string.developed_by) + "\n\n" + getString(R.string.inspired_by_paper))
                .setTitle(getString(R.string.about_menu_title));
        builder.setPositiveButton(getString(R.string.okay), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void exportToFileStorage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(getString(R.string.export_information))
                .setTitle(getString(R.string.export_menu_title));
        builder.setPositiveButton(R.string.proceed, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                checkForStoragePermission();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void checkForStoragePermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            export();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    export();
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.permission_denied), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void export() {
        Snackbar.make(findViewById(R.id.outer_linear_layout), getString(R.string.database_being_exported), Snackbar.LENGTH_LONG).show();

        firestoreDB.collection("daksh_attendees")
                .orderBy("name")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            String[] columns = {"Registration Number", "Name", "Age", "Gender", "Stream", "Attendee Type", "Institution", "Designation", "Email Address", "Phone Number", "Priority", "Blacklist"};

                            Workbook workbook = new HSSFWorkbook();
                            Sheet sheet = workbook.createSheet("Attendee List");

                            Row headerRow = sheet.createRow(0);

                            for (int i = 0; i < columns.length; i++) {
                                Cell cell = headerRow.createCell(i);
                                cell.setCellValue(columns[i]);
                            }

                            int rowNum = 1;

                            for (DocumentSnapshot doc : Objects.requireNonNull(task.getResult())) {
                                Attendee attendee = doc.toObject(Attendee.class);

                                Row row = sheet.createRow(rowNum++);
                                if (attendee != null) {
                                    row.createCell(0).setCellValue(attendee.getRegistrationNumber());
                                    row.createCell(1).setCellValue(attendee.getName());
                                    row.createCell(2).setCellValue(attendee.getAge());
                                    row.createCell(3).setCellValue(attendee.getGender());
                                    row.createCell(4).setCellValue(attendee.getStream());
                                    row.createCell(5).setCellValue(attendee.getAttendeeType());
                                    row.createCell(6).setCellValue(attendee.getInstitution());
                                    row.createCell(7).setCellValue(attendee.getDesignation());
                                    row.createCell(8).setCellValue(attendee.getEmailAddress());
                                    row.createCell(9).setCellValue(attendee.getPhoneNumber());
                                    row.createCell(10).setCellValue(attendee.getPriority());
                                    row.createCell(11).setCellValue(attendee.getBlackList());
                                }
                            }

                            String filePath = Environment.getExternalStorageDirectory().toString();
                            File file = new File(filePath + File.separator, "Attendee List -" + new Date().getTime() + ".xls");
                            FileOutputStream fos = null;

                            try {
                                fos = new FileOutputStream(file);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                            try {
                                workbook.write(fos);
                                if (fos != null) {
                                    fos.flush();
                                    fos.close();
                                }
                                workbook.close();
                                Snackbar.make(findViewById(R.id.outer_linear_layout), getString(R.string.database_exported_successfully) + " " + filePath, Snackbar.LENGTH_LONG).show();
                            } catch (IOException e) {
                                Snackbar.make(findViewById(R.id.outer_linear_layout), getString(R.string.database_export_failed), Snackbar.LENGTH_LONG).show();
                                e.printStackTrace();
                            }

                        } else {
                            Snackbar.make(findViewById(R.id.outer_linear_layout), getString(R.string.database_export_failed), Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
    }

    @Override
    public void onBackPressed() {
        mBackCount++;

        if (mBackCount == 1) {
            Toast.makeText(this, getString(R.string.press_back_to_exit), Toast.LENGTH_SHORT).show();
        } else if (mBackCount == 2) {
            finishAffinity();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAttendeeAdapter.stopListening();
    }
}
