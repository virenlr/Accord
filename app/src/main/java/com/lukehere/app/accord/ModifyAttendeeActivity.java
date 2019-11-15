package com.lukehere.app.accord;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.alespero.expandablecardview.ExpandableCardView;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.xw.repo.BubbleSeekBar;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.widget.NestedScrollView;

public class ModifyAttendeeActivity extends AppCompatActivity {

    /*This constant is used in the app to determine which feedback questions are DEPARTMENT STALL
    related and which ones are NON-DEPARTMENT STALLS or TALKS. If you modify the list of stalls and
    talks in strings.xml, make sure to change the value of this constant as well. By just inputting
    the number of stalls here, the algorithm will take care of displaying the questions properly*/
    public static final int NUMBER_OF_STALLS = 55;

    @Nullable
    FirebaseUser user;

    private boolean openedByClickingOnItem = false;
    private boolean attendeeFound = false;
    private boolean attendeeSearched = false;

    private ExpandableCardView mBasicInformationExpandableCard;
    private ExpandableCardView mPriorityExpandableCard;

    private ProgressBar mBasicInformationProgressBar, mFeedbackProgressBar;

    private BubbleSeekBar mBubbleSeekBar1, mBubbleSeekBar2, mBubbleSeekBar3, mBubbleSeekBar4, mBubbleSeekBar5;
    private TextInputEditText mRegistrationNumberEditText, mNameEditText, mAgeEditText, mStreamEditText, mInstitutionEditText, mDesignationEditText, mEmailAddressEditText, mPhoneNumberEditText;
    private Spinner mGenderSpinner;
    private Spinner mAttendeeTypeSpinner;
    private RadioGroup mPriorityGroup;
    private SwitchCompat mBlacklistSwitch;
    @Nullable
    private Attendee mAttendee;
    @Nullable
    private Feedback mFeedback;
    private FirebaseFirestore mDb;

    private SwitchCompat mFeedbackSwitch;
    private LinearLayout mFeedbackPanel;
    private boolean mFeedbackModeOn = true;
    private boolean itemSelecedIsAStall = true;

    private Button mFeedbackSubmitButton;

    private Spinner mStallSpinner;

    private static final int SCANNER_RESULT_CODE = 21;

    @Override
    public void onBackPressed() {
        if (Objects.requireNonNull(mRegistrationNumberEditText.getText()).toString().trim().length() > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ModifyAttendeeActivity.this);
            builder.setMessage(getString(R.string.confirm_back_press))
                    .setTitle(getString(R.string.warning));
            builder.setPositiveButton(getString(R.string.okay), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent returnIntent = new Intent();
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                }
            });
            builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_attendee);

        setup();
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_modify, menu);

        if (openedByClickingOnItem) {
            MenuItem searchMenuItem = menu.findItem(R.id.search);
            MenuItem qrCodeMenuItem = menu.findItem(R.id.qr_scan);
            searchMenuItem.setVisible(false);
            qrCodeMenuItem.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.update:
                updateToServer();
                return true;
            case R.id.qr_scan:
                qrScanAndUpdateRegistrationNumber();
                return true;
            case R.id.search:
                searchDatabase();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setup() {
        user = FirebaseAuth.getInstance().getCurrentUser();

        NestedScrollView outerScrollView = findViewById(R.id.outer_scroll_view);

        mBasicInformationProgressBar = findViewById(R.id.basic_information_loading_progress_bar);
        mFeedbackProgressBar = findViewById(R.id.feedback_loading_progress_bar);

        mBasicInformationExpandableCard = findViewById(R.id.basic_information_card_view);
        mPriorityExpandableCard = findViewById(R.id.priority_card_view);

        mBubbleSeekBar1 = findViewById(R.id.seekbar_1);
        mBubbleSeekBar2 = findViewById(R.id.seekbar_2);
        mBubbleSeekBar3 = findViewById(R.id.seekbar_3);
        mBubbleSeekBar4 = findViewById(R.id.seekbar_4);
        mBubbleSeekBar5 = findViewById(R.id.seekbar_5);

        outerScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                mBubbleSeekBar1.correctOffsetWhenContainerOnScrolling();
                mBubbleSeekBar2.correctOffsetWhenContainerOnScrolling();
                mBubbleSeekBar3.correctOffsetWhenContainerOnScrolling();
                mBubbleSeekBar4.correctOffsetWhenContainerOnScrolling();
                mBubbleSeekBar5.correctOffsetWhenContainerOnScrolling();
            }
        });

        mRegistrationNumberEditText = findViewById(R.id.registration_number_edit_text);
        mNameEditText = findViewById(R.id.name_edit_text);
        mAgeEditText = findViewById(R.id.age_edit_text);
        mStreamEditText = findViewById(R.id.stream_edit_text);
        mInstitutionEditText = findViewById(R.id.institution_edit_text);
        mDesignationEditText = findViewById(R.id.designation_edit_text);
        mEmailAddressEditText = findViewById(R.id.email_edit_text);
        mPhoneNumberEditText = findViewById(R.id.phone_number_edit_text);

        mGenderSpinner = findViewById(R.id.gender_spinner);
        mAttendeeTypeSpinner = findViewById(R.id.attendee_type_spinner);

        mPriorityGroup = findViewById(R.id.priority_radio_group);

        mFeedbackSwitch = findViewById(R.id.disable_feedback_button);
        mFeedbackPanel = findViewById(R.id.feedback_panel_layout);

        mFeedbackSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && itemSelecedIsAStall) {//Checked,and a stall
                    mFeedbackModeOn = true;
                    mFeedbackPanel.setVisibility(View.VISIBLE);
                    mFeedbackSubmitButton.setVisibility(View.VISIBLE);
                } else if (isChecked) {//Checked, but not a stall
                    mFeedbackModeOn = true;
                    mFeedbackPanel.setVisibility(View.GONE);
                    mFeedbackSubmitButton.setVisibility(View.VISIBLE);
                } else if (itemSelecedIsAStall) {//Unchecked, and a stall
                    mFeedbackModeOn = false;
                    mFeedbackPanel.setVisibility(View.GONE);
                    mFeedbackSubmitButton.setVisibility(View.VISIBLE);
                } else {//Unchecked, but not a stall
                    mFeedbackModeOn = false;
                    mFeedbackPanel.setVisibility(View.GONE);
                    mFeedbackSubmitButton.setVisibility(View.GONE);
                }
            }
        });

        mFeedbackSubmitButton = findViewById(R.id.feedback_submit_button);

        mFeedbackSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitFeedback();
            }
        });

        mBlacklistSwitch = findViewById(R.id.blacklist_switch);

        mStallSpinner = findViewById(R.id.stall_spinner);

        mFeedbackPanel.setVisibility(View.GONE);

        mStallSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {//Option selected is NONE
                    mFeedbackSwitch.setVisibility(View.GONE);
                    mFeedbackPanel.setVisibility(View.GONE);
                    mFeedbackSubmitButton.setVisibility(View.GONE);
                } else if (position > 0 && position <= NUMBER_OF_STALLS) {//Option selected is a stall
                    itemSelecedIsAStall = true;
                    mFeedbackSwitch.setVisibility(View.VISIBLE);

                    if (mFeedbackSwitch.isChecked()) {
                        mFeedbackPanel.setVisibility(View.VISIBLE);
                    }

                    mFeedbackSwitch.setChecked(true);
                    mFeedbackSwitch.setText(getString(R.string.button_enable_disable_feedback));
                    mFeedbackSubmitButton.setVisibility(View.VISIBLE);

                    searchAndUpdateFeedbackViews();
                } else {//Option selected is not a stall
                    itemSelecedIsAStall = false;
                    mFeedbackSwitch.setVisibility(View.VISIBLE);

                    mFeedbackPanel.setVisibility(View.GONE);

                    mFeedbackSwitch.setChecked(true);
                    mFeedbackSwitch.setText(getString(R.string.button_enable_disable_attendance));
                    mFeedbackSubmitButton.setVisibility(View.VISIBLE);

                    searchAndUpdateFeedbackViews();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing
            }
        });

        mAttendee = new Attendee();
        mFeedback = new Feedback();

        mDb = FirebaseFirestore.getInstance();

        Intent intentThatCreatedThisActivity = getIntent();
        if (intentThatCreatedThisActivity.hasExtra("registration_number")) {
            openedByClickingOnItem = true;
            int registrationNumber = intentThatCreatedThisActivity.getIntExtra("registration_number", 0);
            mRegistrationNumberEditText.setText(String.valueOf(registrationNumber));
            mAttendee.setRegistrationNumber(registrationNumber);
            mRegistrationNumberEditText.setEnabled(false);
            searchDatabase();
        } else {
            mRegistrationNumberEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    attendeeSearched = false;
                    clearAllFields(false);
                }
            });

            mRegistrationNumberEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!attendeeSearched) {
                        searchDatabase();
                    }
                }
            });
        }
    }

    private void obtainDataFromFields() {
        if (Objects.requireNonNull(mRegistrationNumberEditText.getText()).length() > 0)
            if (mAttendee != null) {
                mAttendee.setRegistrationNumber(Integer.parseInt(mRegistrationNumberEditText.getText().toString().trim()));
            }
        mAttendee.setName(Objects.requireNonNull(mNameEditText.getText()).toString().trim());
        if (Objects.requireNonNull(mAgeEditText.getText()).length() > 0) {
            mAttendee.setAge(Integer.parseInt(mAgeEditText.getText().toString().trim()));
        } else {
            mAttendee.setAge(0);
        }
        mAttendee.setGender(mGenderSpinner.getSelectedItem().toString().trim());
        mAttendee.setStream(Objects.requireNonNull(mStreamEditText.getText()).toString().trim());
        mAttendee.setAttendeeType(mAttendeeTypeSpinner.getSelectedItem().toString().trim());
        mAttendee.setInstitution(Objects.requireNonNull(mInstitutionEditText.getText()).toString().trim());
        mAttendee.setDesignation(Objects.requireNonNull(mDesignationEditText.getText()).toString().trim());
        mAttendee.setEmailAddress(Objects.requireNonNull(mEmailAddressEditText.getText()).toString().trim());
        mAttendee.setPhoneNumber(Objects.requireNonNull(mPhoneNumberEditText.getText()).toString().trim());

        switch (mPriorityGroup.getCheckedRadioButtonId()) {
            case R.id.low_priority:
                mAttendee.setPriority(1);
                break;
            case R.id.medium_priority:
                mAttendee.setPriority(2);
                break;
            case R.id.high_priority:
                mAttendee.setPriority(3);
                break;
            default:
                mAttendee.setPriority(1);
        }

        if (mBlacklistSwitch.isChecked()) {
            mAttendee.setBlackList(1);
        } else {
            mAttendee.setBlackList(0);
        }
    }

    private void searchDatabase() {
        if (Objects.requireNonNull(mRegistrationNumberEditText.getText()).toString().trim().length() > 0) {
            Objects.requireNonNull(mAttendee).setRegistrationNumber(Integer.parseInt(mRegistrationNumberEditText.getText().toString().trim()));
        } else {
            Objects.requireNonNull(mAttendee).setRegistrationNumber(0);
        }

        if (!(mAttendee.getRegistrationNumber() > 999)) {

            attendeeSearched = false;
            Snackbar.make(findViewById(R.id.outer_scroll_view), getString(R.string.registration_field_error), Snackbar.LENGTH_SHORT)
                    .show();

        } else {
            mBasicInformationProgressBar.setVisibility(View.VISIBLE);
            final DocumentReference docRef = mDb.collection("daksh_attendees").document(mAttendee.getRegistrationNumber() + "");

            docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(@NonNull DocumentSnapshot documentSnapshot) {
                    attendeeSearched = true;

                    mBasicInformationProgressBar.setVisibility(View.GONE);

                    if (documentSnapshot.exists()) {
                        clearAllFields(false);

                        attendeeFound = true;
                        mAttendee = documentSnapshot.toObject(Attendee.class);

                        if (mAttendee != null) {
                            if (mAttendee.getName() != null) {
                                mNameEditText.setText(mAttendee.getName());
                            }
                            mAgeEditText.setText(String.valueOf(mAttendee.getAge()));

                            if (mAttendee.getGender() != null) {
                                if (mAttendee.getGender().equals(getResources().getStringArray(R.array.gender)[0])) {
                                    mGenderSpinner.setSelection(0);
                                } else if (mAttendee.getGender().equals(getResources().getStringArray(R.array.gender)[1])) {
                                    mGenderSpinner.setSelection(1);
                                } else {
                                    mGenderSpinner.setSelection(2);
                                }
                            }

                            if (mAttendee.getStream() != null) {
                                mStreamEditText.setText(mAttendee.getStream());
                            }

                            if (mAttendee.getAttendeeType() != null) {
                                if (mAttendee.getAttendeeType().equals(getResources().getStringArray(R.array.attendee_type)[0])) {
                                    mAttendeeTypeSpinner.setSelection(0);
                                } else {
                                    mAttendeeTypeSpinner.setSelection(1);
                                }
                            }

                            if (mAttendee.getInstitution() != null)
                                mInstitutionEditText.setText(mAttendee.getInstitution());

                            if (mAttendee.getDesignation() != null)
                                mDesignationEditText.setText(mAttendee.getDesignation());

                            if (mAttendee.getEmailAddress() != null)
                                mEmailAddressEditText.setText(mAttendee.getEmailAddress());

                            if (mAttendee.getPhoneNumber() != null)
                                mPhoneNumberEditText.setText(mAttendee.getPhoneNumber());

                            if (mAttendee.getPhoneNumber() != null)
                                switch (mAttendee.getPriority()) {
                                    case 1:
                                        mPriorityGroup.check(R.id.low_priority);
                                        break;
                                    case 2:
                                        mPriorityGroup.check(R.id.medium_priority);
                                        break;
                                    case 3:
                                        mPriorityGroup.check(R.id.high_priority);
                                        break;
                                    default:
                                }

                            if (mAttendee.getBlackList() == 1) {
                                mBlacklistSwitch.setChecked(true);
                            }
                        }

                        if (Objects.requireNonNull(mAttendee).getName().trim().length() == 0 || openedByClickingOnItem) {
                            if (!mBasicInformationExpandableCard.isExpanded())
                                mBasicInformationExpandableCard.expand();
                            if (!mPriorityExpandableCard.isExpanded())
                                mPriorityExpandableCard.expand();
                        }

                        searchAndUpdateFeedbackViews();

                    } else {
                        attendeeFound = false;

                        Snackbar.make(findViewById(R.id.outer_scroll_view), getString(R.string.attendee_not_found), Snackbar.LENGTH_SHORT)
                                .show();

                        if (!mBasicInformationExpandableCard.isExpanded())
                            mBasicInformationExpandableCard.expand();
                        if (!mPriorityExpandableCard.isExpanded())
                            mPriorityExpandableCard.expand();
                    }
                }
            });
        }
    }

    private void updateToServer() {
        obtainDataFromFields();
        if (!attendeeSearched) {
            Snackbar.make(findViewById(R.id.outer_scroll_view), getString(R.string.search_option_first), Snackbar.LENGTH_SHORT).show();
        } else {
            if (!(Objects.requireNonNull(mAttendee).getRegistrationNumber() > 999)) {
                Snackbar.make(findViewById(R.id.outer_scroll_view), getString(R.string.registration_field_error), Snackbar.LENGTH_SHORT).show();
            } else {
                DocumentReference docRef = mDb.collection("daksh_attendees").document(mAttendee.getRegistrationNumber() + "");
                docRef.set(mAttendee);

                if (openedByClickingOnItem) {
                    Toast.makeText(this, getString(R.string.updated), Toast.LENGTH_SHORT).show();
                    Intent returnIntent = new Intent();
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                } else {
                    clearAllFields(true);
                    Snackbar.make(findViewById(R.id.outer_scroll_view), getString(R.string.updated), Snackbar.LENGTH_SHORT)
                            .show();
                }
            }
        }
        attendeeSearched = false;
    }

    private void clearAllFields(boolean clearRegistrationNumber) {
        if (clearRegistrationNumber) {
            mRegistrationNumberEditText.setText("");
        }
        mNameEditText.setText("");
        mAgeEditText.setText("");
        mGenderSpinner.setSelection(0);
        mStreamEditText.setText("");
        mAttendeeTypeSpinner.setSelection(0);
        mInstitutionEditText.setText("");
        mDesignationEditText.setText("");
        mEmailAddressEditText.setText("");
        mPhoneNumberEditText.setText("");
        mPriorityGroup.clearCheck();
        mBlacklistSwitch.setChecked(false);

        mBubbleSeekBar1.setProgress(0);
        mBubbleSeekBar2.setProgress(0);
        mBubbleSeekBar3.setProgress(0);
        mBubbleSeekBar4.setProgress(0);
        mBubbleSeekBar5.setProgress(0);

        modifyFeedbackStatus(true);

        mAttendee = new Attendee();
        mFeedback = new Feedback();
    }

    private void searchAndUpdateFeedbackViews() {
        mFeedbackProgressBar.setVisibility(View.VISIBLE);
        mFeedbackSubmitButton.setEnabled(false);

        if (Objects.requireNonNull(mAttendee).getRegistrationNumber() > 999) {
            DocumentReference feedbackRef = mDb.collection("daksh_attendees").document(mAttendee.getRegistrationNumber() + "").collection("daksh_feedback").document(mStallSpinner.getSelectedItemPosition() + "");

            feedbackRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(@NonNull DocumentSnapshot documentSnapshot) {
                    mFeedbackProgressBar.setVisibility(View.GONE);
                    mFeedbackSubmitButton.setEnabled(true);

                    if (documentSnapshot.exists()) {
                        mFeedback = documentSnapshot.toObject(Feedback.class);
                        if (Objects.requireNonNull(mFeedback).getFeedback1() == -3) {
                            mBubbleSeekBar1.setProgress(0);
                            showPanel(false);
                        } else {
                            mBubbleSeekBar1.setProgress((int) mFeedback.getFeedback1());
                            showPanel(true);
                        }
                        if (mFeedback.getFeedback2() == -3) {
                            mBubbleSeekBar2.setProgress(0);
                            showPanel(false);
                        } else {
                            mBubbleSeekBar2.setProgress((int) mFeedback.getFeedback2());
                            showPanel(true);
                        }
                        if (mFeedback.getFeedback3() == -3) {
                            mBubbleSeekBar3.setProgress(0);
                            showPanel(false);
                        } else {
                            mBubbleSeekBar3.setProgress((int) mFeedback.getFeedback3());
                            showPanel(true);
                        }
                        if (mFeedback.getFeedback4() == -3) {
                            mBubbleSeekBar4.setProgress(0);
                            showPanel(false);
                        } else {
                            mBubbleSeekBar4.setProgress((int) mFeedback.getFeedback4());
                            showPanel(true);
                        }
                        if (mFeedback.getFeedback5() == -3) {
                            mBubbleSeekBar5.setProgress(0);
                            showPanel(false);
                        } else {
                            mBubbleSeekBar5.setProgress((int) mFeedback.getFeedback5());
                            showPanel(true);
                        }
                        modifyFeedbackStatus(false);

                    } else if (mStallSpinner.getSelectedItemPosition() == 0) {
                        mFeedbackSubmitButton.setVisibility(View.GONE);
                    } else {
                        mBubbleSeekBar1.setProgress(0);
                        mBubbleSeekBar2.setProgress(0);
                        mBubbleSeekBar3.setProgress(0);
                        mBubbleSeekBar4.setProgress(0);
                        mBubbleSeekBar5.setProgress(0);
                        showPanel(true);
                        modifyFeedbackStatus(true);
                    }
                }
            });
        } else {
            mFeedbackProgressBar.setVisibility(View.GONE);
            showPanel(true);
            modifyFeedbackStatus(true);
        }
    }

    private void submitFeedback() {
        if (!(attendeeFound) && Objects.requireNonNull(mAttendee).getRegistrationNumber() > 999) {
            obtainDataFromFields();
            DocumentReference docRef = mDb.collection("daksh_attendees").document(mAttendee.getRegistrationNumber() + "");
            docRef.set(mAttendee);
            saveFeedback();
        } else if (attendeeFound && Objects.requireNonNull(mAttendee).getRegistrationNumber() > 999) {
            saveFeedback();
        } else {
            Snackbar.make(findViewById(R.id.outer_scroll_view), getString(R.string.registration_field_error), Snackbar.LENGTH_SHORT)
                    .show();
        }
    }

    private void saveFeedback() {
        modifyFeedbackStatus(false);

        if (mFeedbackModeOn && itemSelecedIsAStall) {
            Snackbar.make(findViewById(R.id.outer_scroll_view), getString(R.string.thank_you_for_feedback), Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(findViewById(R.id.outer_scroll_view), getString(R.string.attendance_registered_successfully), Snackbar.LENGTH_LONG).show();
        }

        DocumentReference feedbackRef = mDb.collection("daksh_attendees").document(Objects.requireNonNull(mAttendee).getRegistrationNumber() + "").collection("daksh_feedback").document(mStallSpinner.getSelectedItemPosition() + "");

        DocumentReference redundantRef = mDb.collection("daksh_feedback").document("required_feedback_document").collection(mStallSpinner.getSelectedItemPosition() + "").document(mAttendee.getRegistrationNumber() + "");

        if (mStallSpinner.getSelectedItemPosition() != 0 && mFeedbackModeOn && itemSelecedIsAStall) {
            if (user != null) {
                Objects.requireNonNull(mFeedback).setUsername(user.getEmail());
            }

            Objects.requireNonNull(mFeedback).setAttendeeRegistrationNumber(mAttendee.getRegistrationNumber());

            mFeedback.setStallName(mStallSpinner.getSelectedItem().toString());
            mFeedback.setFeedback1(mBubbleSeekBar1.getProgress());
            mFeedback.setFeedback2(mBubbleSeekBar2.getProgress());
            mFeedback.setFeedback3(mBubbleSeekBar3.getProgress());
            mFeedback.setFeedback4(mBubbleSeekBar4.getProgress());
            mFeedback.setFeedback5(mBubbleSeekBar5.getProgress());
            feedbackRef.set(mFeedback);

            redundantRef.set(mFeedback);

        } else if (mStallSpinner.getSelectedItemPosition() != 0 && mFeedbackModeOn && !itemSelecedIsAStall) {
            if (user != null) {
                Objects.requireNonNull(mFeedback).setUsername(user.getEmail());
            }

            Objects.requireNonNull(mFeedback).setAttendeeRegistrationNumber(mAttendee.getRegistrationNumber());

            mFeedback.setStallName(mStallSpinner.getSelectedItem().toString());
            mFeedback.setFeedback1(-3);
            mFeedback.setFeedback2(-3);
            mFeedback.setFeedback3(-3);
            mFeedback.setFeedback4(-3);
            mFeedback.setFeedback5(-3);
            feedbackRef.set(mFeedback);

            redundantRef.set(mFeedback);
        } else if (mStallSpinner.getSelectedItemPosition() != 0 && !mFeedbackModeOn) {
            if (user != null) {
                Objects.requireNonNull(mFeedback).setUsername(user.getEmail());
            }

            Objects.requireNonNull(mFeedback).setAttendeeRegistrationNumber(mAttendee.getRegistrationNumber());

            mFeedback.setStallName(mStallSpinner.getSelectedItem().toString());
            mFeedback.setFeedback1(-3);
            mFeedback.setFeedback2(-3);
            mFeedback.setFeedback3(-3);
            mFeedback.setFeedback4(-3);
            mFeedback.setFeedback5(-3);
            feedbackRef.set(mFeedback);

            redundantRef.set(mFeedback);
        }
    }

    private void modifyFeedbackStatus(boolean enableAllFields) {
        if (mStallSpinner.getSelectedItemPosition() != 0) {
            mFeedbackSubmitButton.setVisibility(View.VISIBLE);
        } else {
            mFeedbackSubmitButton.setVisibility(View.GONE);
        }

        if (enableAllFields) {
            mFeedbackSwitch.setEnabled(true);
            mBubbleSeekBar1.setEnabled(true);
            mBubbleSeekBar2.setEnabled(true);
            mBubbleSeekBar3.setEnabled(true);
            mBubbleSeekBar4.setEnabled(true);
            mBubbleSeekBar5.setEnabled(true);
            mFeedbackSubmitButton.setEnabled(true);
        } else {
            mFeedbackSwitch.setEnabled(false);
            mBubbleSeekBar1.setEnabled(false);
            mBubbleSeekBar2.setEnabled(false);
            mBubbleSeekBar3.setEnabled(false);
            mBubbleSeekBar4.setEnabled(false);
            mBubbleSeekBar5.setEnabled(false);
            mFeedbackSubmitButton.setEnabled(false);
        }
    }

    private void showPanel(boolean show) {
        if (show && itemSelecedIsAStall) {
            mFeedbackModeOn = true;
            mFeedbackSwitch.setChecked(true);
            mFeedbackPanel.setVisibility(View.VISIBLE);
        } else if (!itemSelecedIsAStall) {
            mFeedbackModeOn = true;
            mFeedbackSwitch.setChecked(true);
            mFeedbackPanel.setVisibility(View.GONE);
        } else {
            mFeedbackModeOn = false;
            mFeedbackSwitch.setChecked(false);
            mFeedbackPanel.setVisibility(View.GONE);
        }
    }

    private void qrScanAndUpdateRegistrationNumber() {
        clearAllFields(true);
        Intent scanCodeIntent = new Intent(ModifyAttendeeActivity.this, QRScannerActivity.class);
        startActivityForResult(scanCodeIntent, SCANNER_RESULT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SCANNER_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            String scanResult = Objects.requireNonNull(data).getStringExtra("Result");
            int scanResultInteger;

            if (scanResult != null) {
                try {
                    scanResultInteger = Integer.parseInt(scanResult);

                    mRegistrationNumberEditText.setText(String.valueOf(scanResultInteger));
                    searchDatabase();

                } catch (NumberFormatException e) {
                    Snackbar.make(findViewById(R.id.outer_scroll_view), getString(R.string.invalid_registration_number), Snackbar.LENGTH_SHORT).show();
                }
            }
        }
    }
}
